#!/usr/bin/env python3
"""Regenerates src/generated/resources/ from the 1.12 resources in legacy/ and flattening.csv.

Run from the repo root: python3 tools/port/port_assets.py
The output directory is wiped and rebuilt on every run, so never hand-edit it.
Hand-written resources belong in src/main/resources/.

Recipes are converted for everything, gated by neoforge:item_exists conditions,
so they switch on by themselves as items get ported.
"""
import csv
import json
import re
import shutil
from collections import defaultdict
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
LEGACY = ROOT / "legacy/src/main/resources/assets/techguns"
OUT = ROOT / "src/generated/resources"
ASSETS = OUT / "assets/techguns"
DATA = OUT / "data"

ROWS = list(csv.DictReader(open(Path(__file__).with_name("flattening.csv"))))
BY_LEGACY = {(r["legacy_id"], int(r["meta"])): r for r in ROWS}
LEGACY_META_IDS = {r["legacy_id"] for r in ROWS}
BLOCKS = [r for r in ROWS if r["kind"] in ("cube", "stairs", "ladder", "lamp", "camonet", "copy")]
ITEMS = [r for r in ROWS if r["kind"] == "item"]

# 1.12 ore dictionary name -> 1.21 tag
ORE_TAGS = {
    "blockGlass": "c:glass_blocks", "blockIron": "c:storage_blocks/iron", "blockWool": "minecraft:wool",
    "circuitBasic": "c:circuits/basic", "circuitElite": "c:circuits/elite", "cobblestone": "c:cobblestones",
    "dirt": "minecraft:dirt", "dustGlowstone": "c:dusts/glowstone", "dustRedstone": "c:dusts/redstone",
    "dye": "c:dyes", "dyeGreen": "c:dyes/green", "dyeYellow": "c:dyes/yellow", "fiberCarbon": "c:fibers/carbon",
    "gemDiamond": "c:gems/diamond", "gemEmerald": "c:gems/emerald", "gemLapis": "c:gems/lapis",
    "gemQuartz": "c:gems/quartz", "ingotCopper": "c:ingots/copper", "ingotGold": "c:ingots/gold",
    "ingotIron": "c:ingots/iron", "ingotLead": "c:ingots/lead", "ingotObsidianSteel": "c:ingots/obsidian_steel",
    "ingotSteel": "c:ingots/steel", "ingotTitanium": "c:ingots/titanium", "ingotTin": "c:ingots/tin",
    "ingotBronze": "c:ingots/bronze", "ingotUraniumEnriched": "c:ingots/enriched_uranium",
    "itemRubber": "c:rubbers", "logWood": "minecraft:logs", "netherrack": "c:netherracks",
    "nuggetCopper": "c:nuggets/copper", "nuggetIron": "c:nuggets/iron", "nuggetLead": "c:nuggets/lead",
    "nuggetSteel": "c:nuggets/steel", "obsidian": "c:obsidians", "paneGlass": "c:glass_panes",
    "plankWood": "minecraft:planks", "plateCarbon": "c:plates/carbon", "plateIron": "c:plates/iron",
    "plateLead": "c:plates/lead", "plateObsidianSteel": "c:plates/obsidian_steel", "plateSteel": "c:plates/steel",
    "plateTin": "c:plates/tin", "plateTitanium": "c:plates/titanium", "plateCopper": "c:plates/copper",
    "plateBronze": "c:plates/bronze", "sand": "c:sands", "sheetPlastic": "c:plates/plastic",
    "slimeball": "c:slime_balls", "stickWood": "c:rods/wooden", "stone": "c:stones", "string": "c:strings",
    "wireCopper": "c:wires/copper", "wireGold": "c:wires/gold",
    "workbench": "c:player_workstations/crafting_tables", "oreTitanium": "c:raw_materials/titanium",
    "dustUranium": "c:dusts/uranium", "itemRawRubber": "c:raw_materials/rubber",
    "rawPlastic": "c:raw_materials/plastic",
}
# Custom 1.12 ingredient types used as recipe constants
SPECIAL_CONSTANTS = {
    "electrumOrGold": [{"tag": "c:ingots/electrum"}, {"tag": "c:ingots/gold"}],
    "hardenedGlassOrGlass": {"tag": "c:glass_blocks"},
}
# Our items that belong in common tags (from TGItems.registerItemsToOreDict)
ITEM_ORE_DICT = {
    "ingotObsidianSteel": "ingotobsidiansteel", "plateIron": "plateiron", "plateTin": "platetin",
    "plateCopper": "platecopper", "plateBronze": "platebronze", "plateSteel": "platesteel", "plateLead": "platelead",
    "plateCarbon": "platecarbon", "fiberCarbon": "carbonfibers", "plateObsidianSteel": "plateobsidiansteel",
    "circuitBasic": "circuitboard", "wireCopper": "copperwire", "itemRubber": "rubberbar", "wireGold": "goldwire",
    "circuitElite": "circuitboardelite", "ingotTitanium": "ingottitanium", "oreTitanium": "oretitanium",
    "plateTitanium": "platetitanium", "sheetPlastic": "plasticsheet", "dustUranium": "yellowcake",
    "ingotUraniumEnriched": "enricheduranium", "ingotBronze": "ingotbronze", "ingotLead": "ingotlead",
    "ingotTin": "ingottin", "ingotSteel": "ingotsteel", "nuggetCopper": "nuggetcopper", "nuggetLead": "nuggetlead",
    "nuggetSteel": "nuggetsteel", "itemRawRubber": "rawrubber", "rawPlastic": "rawplastic",
}
ORE_BLOCKS = {"ore_tin": ("tin", "stone"), "ore_lead": ("lead", "iron"),
              "ore_titanium": ("titanium", "diamond"), "ore_uranium": ("uranium", "iron")}
SHOVEL_BLOCKS = {"bugnest_sand", "sandbags"}
HAND_BLOCKS = {"slimyladder"}  # plus all camo nets
CLIMBABLE = {"slimyladder"}  # plus kind "ladder"
# Items drawn by a Java renderer (1.12 ItemRenderHack); they get a builtin/entity model
BEWLR_ITEMS = [
    "pistol", "handcannon", "sawedoff", "revolver", "goldenrevolver", "thompson", "ak47", "boltaction", "m4",
    "m4_infiltrator", "combatshotgun", "mac10", "lmg", "minigun", "as50", "vector", "scar", "aug", "pdw", "pulserifle",
    "assaultriflemagazine", "assaultriflemagazineempty", "assaultriflemagazine_incendiary", "lmgmagazine",
    "lmgmagazineempty", "lmgmagazine_incendiary", "as50magazine", "as50magazineempty", "as50magazine_incendiary",
    "as50magazine_explosive", "rocket", "rocket_nuke", "rocket_high_velocity",
    "lasergun", "laserpistol", "teslagun", "alienblaster", "mibgun", "netherblaster", "blasterrifle", "scatterbeamrifle",
    "nucleardeathray", "gaussrifle", "sonicshotgun", "flamethrower",
    "rocketlauncher", "grenadelauncher", "guidedmissilelauncher", "grimreaper", "tfg", "biogun", "stielgranate", "fraggrenade",
    "powerhammer", "chainsaw", "miningdrill", "shishkebap",
]
# 1.12 lang keys that 1.21 looks up under another name
LANG_RENAMES = {
    "techguns.radiation": "effect.techguns.radiation", "techguns.radresistance": "effect.techguns.radresistance",
    "techguns.radregeneration": "effect.techguns.radregeneration",
    "attribute.name.techguns.radiationResistance": "attribute.name.techguns.radiation_resistance",
}
# en_us keys 1.12 didn't have, for what the port adds
EXTRA_LANG = {
    "death.attack.techguns.radiation": "%s died of radiation",
    "death.attack.techguns.radiation.player": "%s died of radiation whilst fighting %s",
    "death.attack.techguns.radiation_poisoning": "%s died of radiation poisoning",
    "death.attack.techguns.radiation_poisoning.player": "%s died of radiation poisoning whilst fighting %s",
}
# Models whose 1.12 block had getBlockLayer() == CUTOUT
CUTOUT_MODELS = ["ladder_base", "slimyladder"]
# 1.12 vanilla (id, meta) -> 1.21 ingredient, for the few with metadata used in recipes
VANILLA = {
    ("minecraft:coal", 0): {"item": "minecraft:coal"},
    ("minecraft:concrete", 32767): {"tag": "c:concretes"},
    ("minecraft:stonebrick", 0): {"item": "minecraft:stone_bricks"},
    ("minecraft:wool", 0): {"item": "minecraft:white_wool"},
}
# GameRegistry.addSmelting calls from TGMachineRecipes: (input, output, count, xp)
SMELTING = [
    ("ore_tin", "ingottin", 1, 0.5), ("ore_lead", "ingotlead", 1, 1.0),
    ("turretarmoriron", "minecraft:iron_ingot", 5, 0), ("turretarmorsteel", "ingotsteel", 5, 0),
    ("turretarmorobsidiansteel", "ingotobsidiansteel", 5, 0), ("ironbarrel", "minecraft:iron_ingot", 6, 0),
    ("obsidiansteelbarrel", "ingotobsidiansteel", 6, 0), ("platebronze", "ingotbronze", 1, 0),
    ("platecopper", "minecraft:copper_ingot", 1, 0), ("platetin", "ingottin", 1, 0), ("platelead", "ingotlead", 1, 0),
    ("plateiron", "minecraft:iron_ingot", 1, 0), ("plateobsidiansteel", "ingotobsidiansteel", 1, 0),
    ("platetitanium", "ingottitanium", 1, 0), ("platesteel", "ingotsteel", 1, 0), ("rawrubber", "rubberbar", 1, 0),
    ("rawplastic", "plasticsheet", 1, 0), ("oretitanium", "ingottitanium", 1, 0),
]


def write_json(path, obj):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(obj, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")


def ns(name):
    return name if ":" in name else "techguns:" + name


def short(rid):
    return rid.split(":", 1)[1]


def fix_paths(text):
    text = re.sub(r'"(techguns|minecraft):items/', r'"\1:item/', text)
    text = re.sub(r'"(techguns|minecraft):blocks/', r'"\1:block/', text)
    text = re.sub(r'"(blocks|items)/', lambda m: '"' + m[1][:-1] + '/', text)
    text = re.sub(r'"((?:minecraft:)?block/)planks_(\w+)"', r'"\1\2_planks"', text)
    return re.sub(r'"(parent|model)"(\s*:\s*)"techguns:([^"/]+)"', r'"\1"\2"techguns:block/\3"', text)


def copy_textures_and_models():
    renames = {"items": "item", "blocks": "block"}
    for src in (LEGACY / "textures").rglob("*"):
        if src.is_file():
            rel = src.relative_to(LEGACY / "textures")
            dst = ASSETS / "textures" / renames.get(rel.parts[0], rel.parts[0]) / Path(*rel.parts[1:])
            dst.parent.mkdir(parents=True, exist_ok=True)
            shutil.copy2(src, dst)
    for src in (LEGACY / "models").rglob("*"):
        if src.is_file():
            dst = ASSETS / "models" / src.relative_to(LEGACY / "models")
            dst.parent.mkdir(parents=True, exist_ok=True)
            if src.suffix in (".json", ".mtl"):
                dst.write_text(fix_paths(src.read_text(encoding="utf-8")), encoding="utf-8")
            else:
                shutil.copy2(src, dst)
    shutil.copytree(LEGACY / "sounds", ASSETS / "sounds")
    shutil.copy2(LEGACY / "sounds.json", ASSETS / "sounds.json")  # 1.12's extra "category" key is ignored
    shutil.copytree(LEGACY / "particles", ASSETS / "particles")  # TGFX definitions, read by client.particle.TGFX
    for name in BEWLR_ITEMS:
        write_json(ASSETS / "models/item" / (name + ".json"),
                   {"parent": "minecraft:builtin/entity", "textures": {"particle": "techguns:item/plateiron"}})
    for name in CUTOUT_MODELS:
        path = ASSETS / "models/block" / (name + ".json")
        write_json(path, json.loads(path.read_text()) | {"render_type": "minecraft:cutout"})


def rotate_y(y):
    return {"y": y % 360} if y % 360 else {}


def forge_to_multipart(legacy, new_name, prop, value):
    """Forge v1 blockstate (per-type textures + per-connection submodels) -> vanilla multipart.
    Each (type, submodel) pair becomes a child model carrying that type's textures."""
    textures = legacy["variants"][prop][value]["textures"]
    parts = []
    for conn, variant in legacy["variants"]["connection"].items():
        subs = variant["submodel"]
        for sub in ([{"model": subs}] if isinstance(subs, str) else subs.values()):
            base = short(sub["model"]).removeprefix("block/")
            child = f"block/{new_name}_{base}"
            write_json(ASSETS / "models" / (child + ".json"),
                       {"parent": "techguns:block/" + base, "render_type": "minecraft:cutout", "textures": textures})
            apply = {"model": "techguns:" + child} | rotate_y(sub.get("y", 0))
            if sub.get("uvlock"):
                apply["uvlock"] = True
            parts.append({"when": {"connection": conn}, "apply": apply})
    return {"multipart": parts}


def filter_multipart(legacy, prop, value):
    parts = []
    for part in legacy["multipart"]:
        when = dict(part.get("when", {}))
        if when.pop(prop, value) == value:
            parts.append(({"when": when} if when else {}) | {"apply": part["apply"]})
    return {"multipart": parts}


def write_blockstates():
    for r in BLOCKS:
        legacy_name, name = short(r["legacy_id"]), short(r["new_id"])
        legacy = json.loads(fix_paths((LEGACY / "blockstates" / (legacy_name + ".json")).read_text()))
        prop, _, value = r["legacy_variant"].partition("=")
        item_model = None
        if r["kind"] == "copy":
            state = legacy
            inventory = LEGACY / "models/item" / (legacy_name + "_inventory.json")
            if inventory.exists():
                item_model = {"parent": f"techguns:item/{legacy_name}_inventory"}
            else:
                item_model = {"parent": legacy["variants"][next(iter(legacy["variants"]))]["model"]}
        elif r["kind"] == "lamp":
            state = filter_multipart(legacy, prop, value)
            item_model = {"parent": f"techguns:item/lamp_inventory_{value}"}
        elif r["kind"] == "camonet":
            state = forge_to_multipart(legacy, name, prop, value)
            inventory = json.loads(fix_paths((LEGACY / "blockstates" / (legacy_name + "_inventory.json")).read_text()))
            item_model = {"parent": inventory["defaults"]["model"], "render_type": "minecraft:cutout",
                          "textures": inventory["variants"][prop][value]["textures"]}
        if item_model:
            write_json(ASSETS / "blockstates" / (name + ".json"), state)
            write_json(ASSETS / "models/item" / (name + ".json"), item_model)
            continue
        variants = {}
        for key, model in legacy["variants"].items():
            props = key.split(",")
            if r["legacy_variant"] in props:
                variants[",".join(p for p in props if p != r["legacy_variant"])] = model
        write_json(ASSETS / "blockstates" / (short(r["new_id"]) + ".json"), {"variants": variants})
        model = next(iter(variants.values()))["model"]
        if r["kind"] == "ladder":
            texture = json.loads((ASSETS / "models" / (short(model) + ".json")).read_text())["textures"]["texture"]
            item_model = {"parent": "minecraft:item/generated", "textures": {"layer0": texture}}
        else:
            item_model = {"parent": model}
        write_json(ASSETS / "models/item" / (short(r["new_id"]) + ".json"), item_model)


def convert_lang():
    for src in (LEGACY / "lang").glob("*.lang"):
        out = {}
        raw = src.read_bytes()
        try:
            text = raw.decode("utf-8")
        except UnicodeDecodeError:  # de_de.lang is Latin-1
            text = raw.decode("cp1252")
        for line in text.splitlines():
            if "=" not in line or line.lstrip().startswith("#"):
                continue
            key, value = line.split("=", 1)
            key = key.strip()
            if key == "techguns.itemGroup.techguns":
                key = "itemGroup.techguns"
            elif m := re.fullmatch(r"item\.techguns\.(.+)\.name", key):
                key = "item.techguns." + m[1]
            elif m := re.fullmatch(r"tile\.techguns\.(.+)\.(\d+)\.name", key):
                row = BY_LEGACY.get(("techguns:" + m[1], int(m[2])))
                if row:
                    if row["kind"] == "vanilla":
                        continue
                    key = "block." + row["new_id"].replace(":", ".")
            elif m := re.fullmatch(r"tile\.techguns\.(.+)\.name", key):
                key = "block.techguns." + m[1]
            elif re.fullmatch(r"(potion|splash_potion|lingering_potion|tipped_arrow)\.effect\..+", key):
                key = "item.minecraft." + key
            out[LANG_RENAMES.get(key, key)] = value
        if src.stem.lower() == "en_us":
            out |= EXTRA_LANG
        write_json(ASSETS / "lang" / (src.stem.lower() + ".json"), out)


class Skip(Exception):
    pass


def load_constants():
    consts = {}
    for c in json.loads((LEGACY / "recipes/_constants.json").read_text()):
        ing = c["ingredient"]
        if ing["type"] == "forge:ore_dict":
            consts["#" + c["name"]] = {"tag": ORE_TAGS[ing["ore"]]}
        else:
            consts["#" + c["name"]] = SPECIAL_CONSTANTS[ing["type"]]
    return consts


def convert_ingredient(ing, consts, used):
    if isinstance(ing, list):
        out = []
        for i in ing:
            c = convert_ingredient(i, consts, used)
            out.extend(c if isinstance(c, list) else [c])
        return out
    if ing.get("type") == "itemstackHasNBTInt":
        ing = {"item": ing["item"]}  # the tool; MiningToolUpgradeRecipe checks its head
    if "type" in ing:
        raise Skip("custom ingredient " + ing["type"])
    item, data = ing["item"], ing.get("data")
    if item.startswith("#"):
        return consts[item]
    if item.startswith("minecraft:"):
        return VANILLA.get((item, data), {"item": item})
    if item in LEGACY_META_IDS:
        if data == 32767:
            ids = [r["new_id"] for r in ROWS if r["legacy_id"] == item]
            used.update(i for i in ids if i.startswith("techguns:"))
            return [{"item": i} for i in ids]
        row = BY_LEGACY.get((item, data or 0))
        if not row:
            raise Skip(f"unmapped {item}@{data}")
        item = row["new_id"]
    if item.startswith("techguns:"):
        used.add(item)
    return {"item": item}


def convert_result(res, used):
    if "nbt" in res:
        raise Skip("result with nbt")
    item = res["item"]
    if item in LEGACY_META_IDS:
        row = BY_LEGACY.get((item, res.get("data", 0)))
        if not row:
            raise Skip(f"unmapped result {item}@{res.get('data', 0)}")
        item = row["new_id"]
    if item.startswith("techguns:"):
        used.add(item)
    out = {"id": item}
    if res.get("count", 1) != 1:
        out["count"] = res["count"]
    return out


def conditions(used):
    return [{"type": "neoforge:item_exists", "item": i} for i in sorted(used)]


def convert_recipes():
    consts = load_constants()
    skipped = defaultdict(list)
    names = set()
    count = 0
    for src in sorted((LEGACY / "recipes").glob("[!_]*.json")):
        r = json.loads(src.read_text())
        used = set()
        try:
            if r["type"] in ("forge:ore_shaped", "minecraft:crafting_shaped"):
                out = {"type": "minecraft:crafting_shaped", "pattern": r["pattern"],
                       "key": {k: convert_ingredient(v, consts, used) for k, v in r["key"].items()}}
            elif r["type"] in ("forge:ore_shapeless", "minecraft:crafting_shapeless", "techguns:ammo_change_crafting",
                               "techguns:miningtool_upgrade"):
                kind = {"techguns:ammo_change_crafting": "techguns:ammo_change",
                        "techguns:miningtool_upgrade": "techguns:miningtool_upgrade"}.get(r["type"], "minecraft:crafting_shapeless")
                out = {"type": kind,
                       "ingredients": [convert_ingredient(i, consts, used) for i in r["ingredients"]]}
            else:
                raise Skip(r["type"])
            out["result"] = convert_result(r["result"], used)
        except Skip as e:
            skipped[str(e)].append(src.stem)
            continue
        name = re.sub(r"^itemshared_\d+_", "", src.stem.lower()).replace(".techguns.itemshared", "")
        if name in names:
            name = src.stem.lower()
        names.add(name)
        if used:
            out["neoforge:conditions"] = conditions(used)
        write_json(DATA / "techguns/recipe" / (name + ".json"), out)
        count += 1
    for inp, outp, n, xp in SMELTING:
        used = {ns(i) for i in (inp, outp) if ":" not in i}
        result = {"id": ns(outp)} | ({"count": n} if n != 1 else {})
        write_json(DATA / "techguns/recipe" / f"smelting_{inp}.json", {
            "neoforge:conditions": conditions(used), "type": "minecraft:smelting",
            "ingredient": {"item": ns(inp)}, "result": result, "experience": xp, "cookingtime": 200})
        count += 1
    return count, skipped


def write_tags_and_loot():
    item_tags, block_tags = defaultdict(list), defaultdict(list)
    for ore, item in ITEM_ORE_DICT.items():
        item_tags[ORE_TAGS[ore]].append(ns(item))
    for r in BLOCKS:
        rid, name = r["new_id"], short(r["new_id"])
        if name in SHOVEL_BLOCKS:
            block_tags["minecraft:mineable/shovel"].append(rid)
        elif name not in HAND_BLOCKS and r["kind"] != "camonet":
            block_tags["minecraft:mineable/pickaxe"].append(rid)
        if name in ORE_BLOCKS:
            metal, tier = ORE_BLOCKS[name]
            for tags in (block_tags, item_tags):
                tags["c:ores/" + metal].append(rid)
            block_tags[f"minecraft:needs_{tier}_tool"].append(rid)
        if r["kind"] == "stairs":
            block_tags["minecraft:stairs"].append(rid)
            item_tags["minecraft:stairs"].append(rid)
        if r["kind"] == "ladder" or name in CLIMBABLE:
            block_tags["minecraft:climbable"].append(rid)
        write_json(DATA / "techguns/loot_table/blocks" / (name + ".json"), {
            "type": "minecraft:block", "random_sequence": "techguns:blocks/" + name,
            "pools": [{"rolls": 1, "bonus_rolls": 0, "entries": [{"type": "minecraft:item", "name": rid}],
                       "conditions": [{"condition": "minecraft:survives_explosion"}]}]})
    for kind, tags in (("item", item_tags), ("block", block_tags)):
        for tag, values in tags.items():
            tag_ns, path = tag.split(":")
            write_json(DATA / tag_ns / "tags" / kind / (path + ".json"), {"values": values})


def main():
    shutil.rmtree(OUT, ignore_errors=True)
    copy_textures_and_models()
    write_blockstates()
    convert_lang()
    count, skipped = convert_recipes()
    write_tags_and_loot()
    print(f"{len(ITEMS)} items, {len(BLOCKS)} blocks, {count} recipes written")
    for reason, files in sorted(skipped.items(), key=lambda kv: -len(kv[1])):
        print(f"  skipped {len(files):3} recipes: {reason}")


if __name__ == "__main__":
    main()
