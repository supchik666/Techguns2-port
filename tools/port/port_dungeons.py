#!/usr/bin/env python3
"""Converts the 1.12 dungeon templates (legacy .../assets/techguns/dungeons/*.ser) into readable JSON.

The .ser files are Java-serialized 1.12 classes. tools/port/dungeons/ holds read-only stubs of those
classes plus DumpDungeons, which javac compiles and runs here (needs a JDK: JAVA_HOME or javac on PATH).
Output: src/generated/resources/data/techguns/dungeon/<template>.json, one entry per segment type with
a palette (char -> block state) and layers[y][z] strings indexed by x. '.' is air (the segment carves it).

Called from port_assets.py; runnable alone: python3 tools/port/port_dungeons.py
"""
import csv
import json
import os
import shutil
import subprocess
import tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
STUBS = Path(__file__).with_name("dungeons")
SER = ROOT / "legacy/src/main/resources/assets/techguns/dungeons"
OUT = ROOT / "src/generated/resources/data/techguns/dungeon"
CSV = {(r["legacy_id"], int(r["meta"])): r["new_id"] for r in csv.DictReader(open(Path(__file__).with_name("flattening.csv")))}

STAIRS = {"brick_stairs": "brick_stairs", "nether_brick_stairs": "nether_brick_stairs", "oak_stairs": "oak_stairs",
          "spruce_stairs": "spruce_stairs", "stone_brick_stairs": "stone_brick_stairs", "stone_stairs": "cobblestone_stairs"}
SLABS = ["smooth_stone_slab", "sandstone_slab", "petrified_oak_slab", "cobblestone_slab", "brick_slab",
         "stone_brick_slab", "nether_brick_slab", "quartz_slab"]
WOODS = ["oak", "spruce", "birch", "jungle", "acacia", "dark_oak"]
HORIZONTAL = ["south", "west", "north", "east"]  # 1.12 EnumFacing.getHorizontal
FRONT = ["down", "up", "north", "south", "west", "east"]  # 1.12 EnumFacing.getFront
CCW = {"south": "east", "west": "south", "north": "west", "east": "north"}
PLAIN = {"air": "air", "bookshelf": "bookshelf", "cobblestone": "cobblestone", "crafting_table": "crafting_table",
         "glass": "glass", "glowstone": "glowstone", "gravel": "gravel", "iron_bars": "iron_bars",
         "nether_brick": "nether_bricks", "netherrack": "netherrack", "stone": "stone"}


def state(name, meta, below, above):
    """1.21 block state string for a 1.12 block name + meta. below/above = (name, meta) of the neighbours."""
    ns, n = name.split(":")
    if (name, meta) in CSV:
        return CSV[(name, meta)]
    if ns == "chisel" and n == "netherbrick":
        return "minecraft:nether_bricks"  # MBlock's replacement when chisel is missing
    if ns == "techguns":
        if n == "debugblock":
            return "minecraft:air"  # scanner interior markers
        if n == "door3x3":
            st = ["closed", "opened", "opening", "closing"][meta & 3]
            return f"techguns:door3x3[master={str(meta & 8 > 0).lower()},state={st},zplane={str(meta & 4 > 0).lower()}]"
        if n == "lamp0" and meta < 6:
            return f"techguns:lamp_yellow[facing={FRONT[meta]}]"
        if n == "stairs_metal":
            half = "top" if meta & 4 == 0 else "bottom"  # BlockTGStairs: EnumHalf.values() is {TOP, BOTTOM}
            sid = "stairs_metal_carbon" if meta & 8 else "stairs_metal"
            return f"techguns:{sid}[facing={HORIZONTAL[meta & 3]},half={half}]"
        raise KeyError(f"{name} {meta}")
    m = "minecraft:"
    if n in PLAIN:
        return m + PLAIN[n]
    if n in STAIRS:
        return f"{m}{STAIRS[n]}[facing={FRONT[5 - (meta & 3)]},half={'top' if meta & 4 else 'bottom'}]"
    if n == "stone_slab":
        return f"{m}{SLABS[meta & 7]}[type={'top' if meta & 8 else 'bottom'}]"
    if n == "wooden_slab":
        return f"{m}{WOODS[meta & 7]}_slab[type={'top' if meta & 8 else 'bottom'}]"
    if n == "double_stone_slab":
        return f"{m}{SLABS[meta & 7]}[type=double]"
    if n == "double_wooden_slab":
        return f"{m}{WOODS[meta & 7]}_slab[type=double]"
    if n == "planks":
        return f"{m}{WOODS[meta]}_planks"
    if n == "log":
        return f"{m}{WOODS[meta & 3]}_log[axis={['y', 'x', 'z', 'y'][meta >> 2]}]"
    if n == "stonebrick":
        return m + ["stone_bricks", "mossy_stone_bricks", "cracked_stone_bricks", "chiseled_stone_bricks"][meta]
    if n == "quartz_block":
        return m + ["quartz_block", "chiseled_quartz_block", "quartz_pillar[axis=y]", "quartz_pillar[axis=x]", "quartz_pillar[axis=z]"][meta]
    if n == "carpet":
        return m + ["white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray", "light_gray",
                    "cyan", "purple", "blue", "brown", "green", "red", "black"][meta] + "_carpet"
    if n in ("chest", "furnace"):
        return f"{m}{n}[facing={FRONT[max(meta, 2)]}]"
    if n == "anvil":
        return f"{m}{['anvil', 'chipped_anvil', 'damaged_anvil'][meta >> 2]}[facing={HORIZONTAL[meta & 3]}]"
    if n == "torch":
        return m + "torch" if meta == 5 else f"{m}wall_torch[facing={FRONT[[0, 5, 4, 3, 2][meta]]}]"
    if n == "lava":
        return m + "lava" + (f"[level={meta}]" if meta else "")
    if n == "skull":
        return m + ("skeleton_skull" if meta == 1 else f"skeleton_wall_skull[facing={FRONT[meta]}]")
    if n.endswith("_door"):
        lower = meta if not meta & 8 else (below[1] if below and below[0] == name else 0)
        facing, is_open = CCW[HORIZONTAL[lower & 3]], str(lower & 4 > 0).lower()
        if meta & 8:
            return f"{m}{n}[facing={facing},half=upper,hinge={'right' if meta & 1 else 'left'},open={is_open}]"
        hinge = "right" if above and above[0] == name and above[1] & 9 == 9 else "left"
        return f"{m}{n}[facing={facing},half=lower,hinge={hinge},open={is_open}]"
    raise KeyError(f"{name} {meta}")


CHARS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789#$%&*+-=?@!~^"


def convert(template):
    out = {"size_xz": template["size_xz"], "size_y": template["size_y"], "segments": {}}
    for seg_type, seg in sorted(template["segments"].items()):
        sx, sy, sz = seg["size"]
        raw = [seg["palette"][i].rsplit(" ", 1) for i in range(len(seg["palette"]))]
        idx = seg["blocks"]
        palette, chars = {}, {}
        layers = []
        for y in range(sy):
            layer = []
            for z in range(sz):
                row = ""
                for x in range(sx):
                    i = idx[(y * sz + z) * sx + x]
                    if i < 0:
                        raise ValueError(f"{seg_type}: unscanned block at {x} {y} {z}")
                    near = [idx[((y + d) * sz + z) * sx + x] if 0 <= y + d < sy else -1 for d in (-1, 1)]
                    below, above = [(raw[j][0], int(raw[j][1])) if j >= 0 else None for j in near]
                    s = state(raw[i][0], int(raw[i][1]), below, above)
                    if s == "minecraft:air":
                        row += "."
                        continue
                    if s not in chars:
                        chars[s] = CHARS[len(chars)]
                        palette[chars[s]] = s
                    row += chars[s]
                layer.append(row)
            layers.append(layer)
        out["segments"][seg_type] = {"palette": palette, "layers": layers}
    return out


# NetherDungeonEntrance: its 1.12 text structure (x,y,z,blockList index per line) and blockList
ENTRANCE = ROOT / "legacy/src/main/resources/assets/techguns/structures/netherdungeon_entrance"
ENTRANCE_BLOCKS = [("techguns:nethermetal", 0), ("techguns:nethermetal", 8), ("techguns:nethermetal", 9),
                   ("techguns:nethermetal", 7), ("techguns:door3x3", 4), ("techguns:door3x3", 12),
                   ("minecraft:nether_brick_stairs", 1)]


def convert_entrance():
    """The entrance as a one-grid template ("STRUCTURE"); ' ' marks blocks the 1.12 structure leaves alone."""
    cells = [tuple(map(int, line.split(","))) for line in ENTRANCE.read_text().split()[1:]]
    sx, sy, sz = (max(c[i] for c in cells) + 1 for i in range(3))
    grid = [[[" "] * sx for _ in range(sz)] for _ in range(sy)]
    palette = {}
    for x, y, z, i in cells:
        s = state(*ENTRANCE_BLOCKS[i], None, None)
        ch = palette.setdefault(s, CHARS[len(palette)])
        grid[y][z][x] = ch
    return {"size_xz": sx, "size_y": sy, "segments": {"STRUCTURE": {
        "palette": {c: s for s, c in palette.items()}, "layers": [["".join(r) for r in layer] for layer in grid]}}}


def javac():
    home = os.environ.get("JAVA_HOME")
    for tool in ([Path(home) / "bin/javac"] if home else []) + [shutil.which("javac")]:
        if tool and Path(tool).exists():
            return Path(tool)
    raise SystemExit("port_dungeons: needs a JDK (set JAVA_HOME or put javac on PATH)")


def main():
    jc = javac()
    files = sorted(SER.glob("*.ser"))
    with tempfile.TemporaryDirectory() as tmp:
        sources = [str(p) for p in STUBS.rglob("*.java")]
        subprocess.run([str(jc), "-d", tmp, *sources], check=True)
        dump = subprocess.run([str(jc.with_name("java")), "-cp", tmp, "DumpDungeons", *map(str, files)],
                              check=True, capture_output=True, text=True).stdout
    shutil.rmtree(OUT, ignore_errors=True)
    OUT.mkdir(parents=True)
    for name, template in json.loads(dump).items():
        (OUT / f"{name}.json").write_text(json.dumps(convert(template), indent=1) + "\n", encoding="utf-8")
    (OUT / "netherdungeon_entrance.json").write_text(json.dumps(convert_entrance(), indent=1) + "\n", encoding="utf-8")
    print(f"{len(files) + 1} dungeon templates written")


if __name__ == "__main__":
    main()
