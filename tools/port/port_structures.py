#!/usr/bin/env python3
"""Converts the 1.12 scanned structures (legacy structures/<name> + the blockList of its WorldgenStructure class) into NBT templates,
worldgen structures, structure sets and biome tags under src/generated/resources. Called by port_assets.py.

A blockList entry is evaluated as Python after a few regex rewrites (MBlock helpers below). Random 1.12 choices become data structure
blocks whose metadata is a JSON node read by techguns.world.TemplateStructure.Piece#resolve."""
import gzip
import json
import re
import struct
from types import SimpleNamespace

from port_assets import BY_LEGACY, DATA, ROOT, write_json

LEGACY_JAVA = ROOT / "legacy/src/main/java/techguns/world/structures"
LEGACY_TEMPLATES = ROOT / "legacy/src/main/resources/assets/techguns/structures"
OUT = DATA / "techguns"

# template, class, structure set, weight, placement, y_offset, cleanUpwards range, foundation, biome tag
STRUCTURES = [
    ("factory_building_small", "FactoryHouseSmall", "overworld_small", 10, "surface", 0, 7, 3, "overworld"),
    ("small_trainstation", "SmallTrainstation", "overworld_small", 10, "surface", 0, 7, 1, "overworld"),
    ("small_mine", "SmallMine", "overworld_small", 10, "surface", -5, 4, 0, "overworld"),
    ("gasstation", "GasStation", "overworld_small", 10, "surface", 0, 7, 3, "overworld"),
    ("policestation", "PoliceStation", "overworld_medium", 10, "surface", 0, 7, 3, "overworld"),
    ("survivor_hideout", "SurvivorHideout", "overworld_medium", 10, "surface", 0, 7, 3, "overworld"),
    ("orecluster_spike", "OreClusterSpike", "overworld_medium", 10, "surface", 0, 6, 0, "overworld"),
    ("orecluster_meteorbase", "OreClusterMeteorBasis", "overworld_medium", 5, "surface", -5, 30, 0, "overworld"),
    ("desert_oil_cluster", "DesertOilCluster", "overworld_medium", 15, "surface", -4, 6, 0, "desert"),
    ("aircraft_carrier", "AircraftCarrier", "overworld_big", 1, "water", 0, 0, 0, "ocean"),
    # nether: y_offset is the 1.12 hoffset - 1, as the cave floor is the block below the 1.12 posY
    ("nether_altar_small", "NetherAltarSmall", "nether_small", 10, "nether", -3, 0, 16, "nether"),
    ("nether_soul_platform", "NetherSoulPlatform", "nether_small", 10, "nether", -3, 0, 16, "nether"),
    ("nether_loot_01", "NetherLoot01", "nether_small", 10, "nether", 0, 0, 16, "nether"),
    ("nether_acid_hole", "NetherAcidHole", "nether_small", 10, "nether", -2, 0, 16, "nether"),
    ("nether_orecluster_small", "NetherOreClusterSmall", "nether_small", 10, "nether", 0, 0, 2, "nether"),
    ("nether_altar_medium", "NetherAltarMedium", "nether_medium", 10, "nether", -2, 0, 16, "nether"),
    ("nether_ghast_spawner", "NetherGhastSpawner", "nether_medium", 10, "nether", -3, 0, 16, "nether"),
    ("nether_orecluster_castle", "NetherOreClusterCastle", "nether_medium", 1000, "nether", -2, 0, 16, "nether"),
]
# 1.12 TGConfig StructureSpawnWeightSmall/Medium/Big: one try every N chunks
SETS = {"overworld_small": 16, "overworld_medium": 32, "overworld_big": 64, "nether_small": 16, "nether_medium": 32}
BIOMES = {"overworld": "#minecraft:is_overworld", "desert": "#c:is_desert", "ocean": "#minecraft:is_ocean", "nether": "#minecraft:is_nether"}

# --- 1.12 block + metadata -> 1.21 block state -------------------------------------------------------------------------------
F6 = ["down", "up", "north", "south", "west", "east"]  # EnumFacing.getFront
H4 = ["south", "west", "north", "east"]  # EnumFacing.getHorizontal
COLORS = ["white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray", "light_gray", "cyan", "purple", "blue", "brown",
          "green", "red", "black"]
WOODS = ["oak", "spruce", "birch", "jungle", "acacia", "dark_oak"]
BY_META = {
    "stone": ["stone", "granite", "polished_granite", "diorite", "polished_diorite", "andesite", "polished_andesite"],
    "stonebrick": ["stone_bricks", "mossy_stone_bricks", "cracked_stone_bricks", "chiseled_stone_bricks"],
    "planks": [w + "_planks" for w in WOODS],
    "dirt": ["dirt", "coarse_dirt", "podzol"],
    "quartz_block": ["quartz_block", "chiseled_quartz_block", "quartz_pillar"],
    "sandstone": ["sandstone", "chiseled_sandstone", "cut_sandstone"],
    "concrete": [c + "_concrete" for c in COLORS],
}
RENAMES = {"brick_block": "bricks", "nether_brick": "nether_bricks", "magma": "magma_block", "quartz_ore": "nether_quartz_ore",
           "grass": "grass_block", "snow": "snow_block", "hardened_clay": "terracotta", "fence": "oak_fence", "oak_fence": "oak_fence",
           "wooden_pressure_plate": "oak_pressure_plate", "skull": "skeleton_skull", "double_stone_slab": "smooth_stone_slab[type=double]"}
STAIRS = {"oak_stairs": "oak_stairs", "stone_stairs": "cobblestone_stairs", "stone_brick_stairs": "stone_brick_stairs",
          "nether_brick_stairs": "nether_brick_stairs", "quartz_stairs": "quartz_stairs"}
STONE_SLABS = ["smooth_stone", "sandstone", "petrified_oak", "cobblestone", "brick", "stone_brick", "nether_brick", "quartz"]
RAILS = ["north_south", "east_west", "ascending_east", "ascending_west", "ascending_north", "ascending_south", "south_east",
         "south_west", "north_west", "north_east"]
TG_LEGACY = {"CONCRETE": "concrete", "METAL_PANEL": "metalpanel", "NETHER_METAL": "nethermetal", "LAMP_0": "lamp0", "LADDER_0": "ladder0",
             "SANDBAGS": "sandbags", "SLIMY_BLOCK": "slimy", "SLIMY_LADDER": "slimyladder", "ORE_CLUSTER": "orecluster",
             "BUNKER_DOOR": "bunkerdoor", "TG_ORE": "basicore", "MILITARY_CRATE": "military_crate"}


def door(name, m):
    if m & 8:
        return f"{name}[half=upper,hinge={'right' if m & 1 else 'left'}]"
    return f"{name}[half=lower,facing={['east', 'south', 'west', 'north'][m & 3]},open={str(bool(m & 4)).lower()}]"


def legacy_state(lid, m):
    """A 1.12 block id + metadata as a 1.21 state string."""
    ns, name = lid.split(":")
    if ns == "techguns":
        if name == "lamp0":
            return f"techguns:lamp_yellow[facing={F6[m]}]" if m < 6 else f"techguns:lamp_white[facing={F6[m - 6]}]" if m < 12 \
                else ["techguns:lantern_yellow", "techguns:lantern_white"][m - 12]
        if name == "ladder0":
            return f"techguns:ladder_{['metal', 'shiny', 'rusty', 'carbon'][m & 3]}[facing={H4[m >> 2]}]"
        if name == "slimyladder":
            return f"techguns:slimyladder[facing={F6[m]}]"
        if name == "bunkerdoor":
            return door("techguns:bunkerdoor", m)
        if name == "military_crate":
            m = min(m, 5)  # ponytail: 1.12 metas 6-8 (generic crate wood variants) are all the generic crate
        return BY_LEGACY[(lid, m)]["new_id"]
    if name in BY_META:
        return "minecraft:" + BY_META[name][m]
    if name in STAIRS:
        return f"minecraft:{STAIRS[name]}[facing={['east', 'west', 'south', 'north'][m & 3]},half={'top' if m & 4 else 'bottom'}]"
    if name in ("furnace", "chest", "ladder"):
        return f"minecraft:{name}[facing={F6[m]}]"
    if name == "torch":
        return "minecraft:torch" if m == 5 else f"minecraft:wall_torch[facing={['', 'east', 'west', 'south', 'north'][m]}]"
    if name == "lever":
        return f"minecraft:lever[face=wall,facing={['', 'east', 'west', 'south', 'north'][m & 7]}]"  # only wall levers are used
    if name == "bed":
        return f"minecraft:red_bed[facing={H4[m & 3]},part={'head' if m & 8 else 'foot'}]"
    if name == "trapdoor":
        return (f"minecraft:oak_trapdoor[facing={['north', 'south', 'west', 'east'][m & 3]},open={str(bool(m & 4)).lower()},"
                f"half={'top' if m & 8 else 'bottom'}]")
    if name == "wooden_door":
        return door("minecraft:oak_door", m)
    if name == "rail":
        return f"minecraft:rail[shape={RAILS[m]}]"
    if name == "log":
        return f"minecraft:{WOODS[m & 3]}_log[axis={['y', 'x', 'z', 'y'][m >> 2]}]"
    if name in ("fence_gate", "oak_fence_gate"):
        return f"minecraft:oak_fence_gate[facing={H4[m & 3]},open={str(bool(m & 4)).lower()}]"
    if name == "stone_slab":
        return f"minecraft:{STONE_SLABS[m & 7]}_slab[type={'top' if m & 8 else 'bottom'}]"
    if name == "wooden_slab":
        return f"minecraft:{WOODS[m & 7]}_slab[type={'top' if m & 8 else 'bottom'}]"
    return "minecraft:" + RENAMES.get(name, name)


# --- the MBlock classes, evaluated from the Java source ------------------------------------------------------------------------
class Id(str):
    """A 1.12 block id (Blocks.X / TGBlocks.X / "mod:name"), as opposed to an already converted state string."""


def MBlock(block, meta=0):
    return legacy_state(block, meta) if isinstance(block, Id) else block


def MultiMBlock(blocks, metas, weights):
    return {"random": [[MBlock(b, m), w] for b, m, w in zip(blocks, metas, weights)]}


def MultiMMBlock(nodes, weights):
    return {"random": [[n, w] for n, w in zip(nodes, weights)]}


def roll(nodes, weights):
    """1.12 MultiMMBlockIndexRoll: one index rolled per structure (with the structure's cluster weights) for all of them."""
    return {"roll": "r", "of": [[n, w] for n, w in zip(nodes, weights)]}


MultiMMBlockIndexRoll = roll


def MBlockOreclusterType(types, weights, chance, ores):
    clusters = roll(types, weights)
    return {"chance": chance, "then": roll(ores, weights), "else": clusters} if chance > 0 and ores else clusters


class BE(SimpleNamespace):
    """A block with block entity data."""


class MBlockTGSpawner(BE):
    def __init__(self, block, mobs_left, max_active, delay, spawn_range):
        super().__init__(state=block, nbt={"id": "techguns:tg_spawner", "mobsLeft": mobs_left, "maxActive": max_active, "spawnDelay": delay,
                                           "spawnRange": spawn_range, "delay": delay, "mobtypes": []})

    def addMobType(self, entity, weight):
        self.nbt["mobtypes"].append({"id": entity, "weight": weight})
        return self

    def setWeaponOverride(self, stack):
        self.nbt["weapon"] = stack
        return self


def MBlockChestLoottable(block, facing, loot):
    return BE(state=f"minecraft:chest[facing={F6[facing]}]", nbt={"id": "minecraft:chest", "LootTable": loot})


def MBlockVanillaSpawner(entity):
    return BE(state="minecraft:spawner", nbt={"id": "minecraft:mob_spawner", "SpawnData": {"entity": {"id": entity}}})


def MBlockBiomeColorType(blocks, metas):
    return {"biome": [MBlock(b, m) for b, m in zip(blocks, metas)]}


def ItemStack(item):
    return {"id": item, "count": 1}


def enum(*names):
    return SimpleNamespace(**{n: SimpleNamespace(ordinal=i) for i, n in enumerate(names)})


class Namespace:
    def __init__(self, fn):
        self.fn = fn

    def __getattr__(self, name):
        return self.fn(name)


A = "minecraft:air"
CRATES = [MBlock(Id("techguns:military_crate"), m) for m in range(9)]
REGISTER = {
    "AIR": A, "SUPPLY_CRATES": {"random": [[c, 1] for c in CRATES]}, "SUPPLY_CRATES_CHANCE": {"random": [[c, 1] for c in CRATES] + [[A, 9]]},
    "FACTORY_PLATE_DOTTED": "minecraft:terracotta", "FACTORY_PLATE": "minecraft:bricks", "FACTORY_CRATE": "techguns:container_red",
    "FACTORY_PLATE_DOTTED_GREY": "minecraft:light_gray_concrete", "FACTORY_PLATE_VERY_RUSTY": "minecraft:gray_concrete",
    "FACTORY_PLATE_WORN_COLUMN": "minecraft:terracotta", "FACTORY_WIREFRAME": "techguns:concrete_grey_dark",
    "FACTORY_SHINY_METAL": "techguns:panel_large_border", "TECHNICAL_CONCRETE": "techguns:concrete_brown_light",
    "TECHNICAL_BLOCK_SCAFFOLD": "techguns:steelframe_scaffold", "TECHNICAL_PIPES": "techguns:concrete_brown_pipes",
    "TECHNICAL_GRATE": "techguns:steelframe_dark", "TECHNICAL2_SCAFFOLD_TRANSPARENT": "techguns:steelframe_scaffold",
    "TECHNICAL2_FAN": "techguns:steelframe_dark", "IRON_BLOCK_VENTS": "minecraft:iron_block", "IRON_BLOCK_SMALL_INGOTS": "minecraft:iron_block",
    "ALUMINIUM_STAIRS_WEST": "techguns:stairs_metal[facing=west,half=bottom]", "ALUMINIUM_STAIRS_EAST": "techguns:stairs_metal[facing=east,half=bottom]",
    "NETHERRACK_ROCKY": "minecraft:netherrack", "NETHERRACK_C1": "minecraft:netherrack", "NETHERRACK_C2": "minecraft:netherrack",
    "NETHERRACK_C12": "minecraft:netherrack", "NETHERRACK_C8": "minecraft:netherrack", "NETHERBRICKS_C13": "minecraft:nether_bricks",
    "HELIPAD_HAZARDBLOCK": "minecraft:yellow_concrete",
    "COBBLESTONE_MOSSY_RND": {"random": [["minecraft:cobblestone", 2], ["minecraft:mossy_cobblestone", 1]]},
    "COBBLESTONE_FLOOR": "minecraft:stone", "COBBLESTONE_7": "minecraft:polished_andesite",
    "STONEBRICK_STAIRS_NORTH_INV": "minecraft:stone_brick_stairs[facing=north,half=top]",
    "STONEBRICK_STAIRS_SOUTH_INV": "minecraft:stone_brick_stairs[facing=south,half=top]",
    **{f"STONEBRICK_STAIRS_{f.upper()}_CRACKED{inv}_RND": {"random": [[f"minecraft:stone_brick_stairs[facing={f},half={h}]", 4], [A, 1]]}
       for f in ("east", "west") for inv, h in (("", "bottom"), ("_INV", "top"))},
    "STONEBRICK_CRACKED_RND": {"random": [["minecraft:stone_bricks", 3], ["minecraft:mossy_stone_bricks", 3],
                                          ["minecraft:cracked_stone_bricks", 3], [A, 2]]},
    "BRICKS_CRACKED_RND": "minecraft:bricks", "IRON_PANE_MODERN_FENCE": "minecraft:iron_bars", "OAK_PLANKS_1": "minecraft:oak_planks",
    "BLUE_CONCRETE_SMALL_BRICKS": "minecraft:blue_concrete", "GAS_STATION_CONSOLE": "minecraft:quartz_stairs[facing=west,half=bottom]",
    "SURIVIVOR_HIDEOUT_IRON_BOTTOM": "minecraft:iron_block", "SURIVIVOR_HIDEOUT_IRON_TOP": "minecraft:iron_block",
    "CAMO_NET_TOP": {"biome": ["techguns:camonet_top_wood", "techguns:camonet_top_snow", "techguns:camonet_top_desert", "techguns:camonet_top_wood"]},
}
ORE_TYPES = ["ORE_COPPER", "ORE_TIN", "ORE_LEAD", "ORE_TITANIUM", "ORE_URANIUM"]
ENV = {
    "MBlock": MBlock, "MultiMBlock": MultiMBlock, "MultiMMBlock": MultiMMBlock, "MultiMMBlockIndexRoll": MultiMMBlockIndexRoll,
    "MBlockOreclusterType": MBlockOreclusterType, "MBlockTGSpawner": MBlockTGSpawner, "MBlockChestLoottable": MBlockChestLoottable,
    "MBlockVanillaSpawner": MBlockVanillaSpawner, "MBlockBiomeColorType": MBlockBiomeColorType, "ItemStack": ItemStack,
    "Blocks": Namespace(lambda n: Id("minecraft:" + n.lower())), "TGBlocks": Namespace(lambda n: Id("techguns:" + TG_LEGACY[n])),
    "TGFluids": SimpleNamespace(BLOCK_FLUID_ACID="techguns:block_creeper_acid"), "null": None, "MBlockRegister": Namespace(REGISTER.__getitem__),
    "EnumFacing": enum(*[f.upper() for f in F6]), "EnumOreType": enum(*ORE_TYPES),
    "TGMetalPanelType": enum("CONTAINER_RED", "CONTAINER_GREEN", "CONTAINER_BLUE", "CONTAINER_ORANGE", "PANEL_LARGE_BORDER",
                             "STEELFRAME_BLUE", "STEELFRAME_DARK", "STEELFRAME_SCAFFOLD"),
    "EnumConcreteType": enum("CONCRETE_BROWN", "CONCRETE_BROWN_LIGHT", "CONCRETE_GREY", "CONCRETE_GREY_DARK", "CONCRETE_BROWN_PIPES",
                             "CONCRETE_BROWN_LIGHT_SCAFF"),
    "EnumOreClusterType": Namespace(lambda n: "techguns:ore_cluster_" + n.lower()),
    "EnumMonsterSpawnerType": SimpleNamespace(HOLE="techguns:hole", SOLDIER_SPAWN="techguns:soldier_spawn"),
    "TGuns": Namespace(lambda n: "techguns:" + n),
}
for mob in ("ArmySoldier", "Commando", "Bandit", "CyberDemon", "ZombieMiner", "ZombieFarmer", "ZombieSoldier", "ZombiePoliceman",
            "ZombiePigmanSoldier", "AlienBug", "Ghastling", "AttackHelicopter"):
    ENV[mob] = "techguns:" + mob.lower()
# Blocks.X / TGBlocks.X with a state builder instead of a meta
STATE_BUILDERS = {
    r"TGBlocks\.LADDER_0\.getDefaultState\(\)\.withProperty\(TGBlocks\.LADDER_0\.FACING, EnumFacing\.SOUTH\)": '"techguns:ladder_metal[facing=south]"',
    r"TGBlocks\.BUNKER_DOOR\.getDefaultState\(\)\.withProperty\(TGBlocks\.BUNKER_DOOR\.FACING, EnumFacing\.SOUTH\)\.withProperty\(TGBlocks\.BUNKER_DOOR\.HINGE, EnumHingePosition\.LEFT\)\.withProperty\(TGBlocks\.BUNKER_DOOR\.HALF, EnumDoorHalf\.LOWER\)":
        '"techguns:bunkerdoor[facing=south,hinge=left,half=lower]"',
    r"TGBlocks\.BUNKER_DOOR\.getDefaultState\(\)\.withProperty\(TGBlocks\.BUNKER_DOOR\.FACING, EnumFacing\.SOUTH\)\.withProperty\(TGBlocks\.BUNKER_DOOR\.HINGE, EnumHingePosition\.LEFT\)\.withProperty\(TGBlocks\.BUNKER_DOOR\.HALF, EnumDoorHalf\.UPPER\)":
        '"techguns:bunkerdoor[hinge=left,half=upper]"',
}
# the ore arrays of SmallMine, OreClusterSpike and OreClusterMeteorBasis (the last one has the 8th, nether entry)
ORES = ("[MBlock(Blocks.COAL_ORE,0), MultiMBlock([Blocks.IRON_ORE, Id('techguns:basicore'), Id('techguns:basicore')], [0,0,1], [1,1,1]),"
        " MultiMBlock([Blocks.REDSTONE_ORE, Blocks.LAPIS_ORE], [0,0], [1,1]), MBlock(Id('techguns:basicore'), 2),"
        " MultiMBlock([Blocks.GOLD_ORE, Id('techguns:basicore')], [0,3], [1,1]), MultiMBlock([Blocks.DIAMOND_ORE, Blocks.EMERALD_ORE, Blocks.STONE], [0,0,0], [2,1,3]),"
        " MultiMBlock([Id('techguns:basicore'), Blocks.STONE], [4,0], [1,1]), MultiMBlock([Blocks.NETHERRACK, Blocks.QUARTZ_ORE, Blocks.GLOWSTONE], [0,0,0], [2,1,1])]")


def class_env(java):
    """The per class statics a blockList refers to: loot table, ore cluster types and weights, ores, the oil blocks."""
    env = dict(ENV, Id=Id)
    loot = re.search(r'ResourceLocation\(Techguns\.MODID, *"(chests/\w+)"\)', java)
    env["CHEST_LOOT"] = env["LOOT_TABLE"] = loot and "techguns:" + loot[1]
    weights = re.search(r"clusterWeights = \{([\d,\s]+)\}", java)
    if weights:
        cw = [int(w) for w in weights[1].split(",")]
        env["clusterWeights"] = env["oreWeights"] = cw
        types = re.search(r"EnumOreClusterType\[\] types = \{([^}]+)\}", java)[1]
        env["types"] = ["techguns:ore_cluster_" + c.lower() for c in re.findall(r"EnumOreClusterType\.(\w+)", types)]
        env["ores"] = eval(ORES, env)[:len(cw)]
        # 1.12 MBlockOreClusterTypeOre: stone at that chance, else the rolled ore
        env["MBlockOreClusterTypeOre"] = lambda ores, w, stone, chance: {"chance": chance, "then": stone, "else": roll(ores, cw)}
    if "oilblock" in java:
        # ponytail: the 1.12 oil fluid isn't ported, so this is the 1.12 fallback without it (sandstone, lava, magma)
        env.update(ores=["minecraft:sandstone"], oilblock="minecraft:lava", oilblockSand="minecraft:magma_block")
    return env


def to_python(expr):
    expr = re.sub(r'MBlock\("([^"]+)"', r'MBlock(Id("\1")', expr)
    for pattern, repl in STATE_BUILDERS.items():
        expr = re.sub(pattern, repl, expr)
    expr = re.sub(r"new \w+\[\]\s*", "", expr).replace("{", "[").replace("}", "]")
    expr = re.sub(r"\bnew (\w+)\(", r"\1(", expr)
    return re.sub(r"(\d)f\b", r"\1", expr).replace(".class", "").replace(".ordinal()", ".ordinal")


def block_list(cls):
    java = (LEGACY_JAVA / f"{cls}.java").read_text()
    env = class_env(java)
    nodes = []
    for line in java.splitlines():
        line = line.strip()
        if not line.startswith("blockList.add("):
            continue
        depth, i = 0, len("blockList.add")
        for i in range(i, len(line)):
            depth += {"(": 1, ")": -1}.get(line[i], 0)
            if depth == 0:
                break
        nodes.append(eval(to_python(line[len("blockList.add") + 1:i]), env))
    return nodes


# --- NBT ----------------------------------------------------------------------------------------------------------------------
def payload(v):
    if isinstance(v, bool):
        return 1, struct.pack(">b", v)
    if isinstance(v, int):
        return 3, struct.pack(">i", v)
    if isinstance(v, str):
        b = v.encode()
        return 8, struct.pack(">H", len(b)) + b
    if isinstance(v, list):
        items = [payload(x) for x in v]
        return 9, struct.pack(">bi", items[0][0] if items else 0, len(items)) + b"".join(p for _, p in items)
    out = b""
    for k, x in v.items():
        t, p = payload(x)
        out += struct.pack(">bH", t, len(k)) + k.encode() + p
    return 10, out + b"\0"


def write_nbt(path, root):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_bytes(gzip.compress(struct.pack(">bH", 10, 0) + payload(root)[1], mtime=0))


def parse_state(s):
    name, _, props = s.rstrip("]").partition("[")
    return name, dict(p.split("=") for p in props.split(",")) if props else {}


def template(name, cls, y_offset, cleanup):
    """The NBT template: the scanned blocks, plus air for the 1.12 cleanUpwards over every floor column."""
    nodes = block_list(cls)
    lines = (LEGACY_TEMPLATES / name).read_text().split()[1:]
    blocks = {}
    for line in lines:
        x, y, z, i = map(int, line.split(","))
        blocks[(x, y, z)] = nodes[i]
    for (x, y, z) in [p for p in blocks if p[1] == 0]:
        for o in range(1, cleanup + 1):
            blocks.setdefault((x, o - y_offset, z), A)
    # 1.12 door halves each hold half the state; the 1.21 ones both need it
    for (x, y, z), node in blocks.items():
        upper = blocks.get((x, y + 1, z))
        if isinstance(node, str) and isinstance(upper, str) and "half=lower" in node and "half=upper" in upper:
            name_, props = parse_state(node)
            props["hinge"] = parse_state(upper)[1]["hinge"]
            blocks[(x, y, z)] = f"{name_}[{','.join(f'{k}={v}' for k, v in props.items())}]"
            blocks[(x, y + 1, z)] = blocks[(x, y, z)].replace("half=lower", "half=upper")
    palette, entries = [], []
    for pos, node in sorted(blocks.items(), key=lambda kv: (kv[0][1], kv[0][2], kv[0][0])):
        nbt = None
        if isinstance(node, BE):
            state, nbt = node.state, node.nbt
        elif isinstance(node, dict):
            state, nbt = "minecraft:structure_block[mode=data]", {"id": "minecraft:structure_block", "mode": "DATA",
                                                                   "metadata": json.dumps(node, separators=(",", ":"))}
        else:
            state = node
        if state not in palette:
            palette.append(state)
        entry = {"pos": list(pos), "state": palette.index(state)}
        if nbt:
            entry["nbt"] = nbt
        entries.append(entry)
    size = [max(p[i] for p in blocks) + 1 for i in range(3)]
    pal = []
    for s in palette:
        n, props = parse_state(s)
        pal.append({"Name": n, "Properties": props} if props else {"Name": n})
    write_nbt(OUT / f"structure/{name}.nbt", {"DataVersion": 3955, "size": size, "palette": pal, "blocks": entries, "entities": []})
    return len(entries)


def main():
    for name, cls, structure_set, weight, placement, y_offset, cleanup, foundation, biome in STRUCTURES:
        template(name, cls, y_offset, cleanup)
        write_json(OUT / f"worldgen/structure/{name}.json", {
            "type": "techguns:template", "biomes": f"#techguns:has_structure/{biome}", "step": "surface_structures",
            "spawn_overrides": {}, "terrain_adaptation": "none", "template": f"techguns:{name}", "placement": placement,
            "y_offset": y_offset, "foundation": foundation})
    for salt, (structure_set, spacing) in enumerate(SETS.items()):
        write_json(OUT / f"worldgen/structure_set/{structure_set}.json", {
            "structures": [{"structure": f"techguns:{s[0]}", "weight": s[3]} for s in STRUCTURES if s[2] == structure_set],
            "placement": {"type": "minecraft:random_spread", "spacing": spacing, "separation": spacing // 4, "salt": 1645120 + salt}})
    for biome, tag in BIOMES.items():
        write_json(OUT / f"tags/worldgen/biome/has_structure/{biome}.json", {"values": [{"id": tag, "required": False}]})
    print(f"{len(STRUCTURES)} structures written")


if __name__ == "__main__":
    main()
