#!/usr/bin/env python3
"""Converts the 1.12 Patchouli book (techguns_manual) for Patchouli 1.21. Called by port_assets.py.

book.json moves to data/techguns/patchouli_books/ with use_resource_pack, the contents stay in assets/. 1.12 item ids
("techguns:itemshared:57") and multiblock states ("techguns:oredrill[machinetype=rod]") go through flattening.csv,
entry categories get their namespace, armor stand NBT uses 1.21's lowercase "count". The book's crafting recipes
only load with Patchouli.
"""
import json
import re

from port_assets import ASSETS, BY_LEGACY, DATA, LEGACY, LEGACY_META_IDS, ROOT, ROWS, write_json

SRC = LEGACY / "patchouli_books/techguns_manual"
BOOK = "techguns:techguns_manual"
ITEM_ID = re.compile(r"^(\w+:\w+)(?::(\d+))?$")
STATE = re.compile(r"^(\w+:\w+)\[(.+)\]$")


def known_ids():
    """Item ids the port has, judged by their item models and blockstates."""
    ids = set()
    for root in (ASSETS, ROOT / "src/main/resources/assets/techguns"):
        for sub in ("models/item", "blockstates"):
            ids |= {"techguns:" + p.stem for p in (root / sub).glob("*.json")}
    return ids


def item(s, notes):
    m = ITEM_ID.match(s)
    if not m:
        return s
    if not m[1].startswith("techguns:"):
        return m[1]  # 1.21 has no metadata
    legacy, meta = m[1], int(m[2] or 0)
    if legacy not in LEGACY_META_IDS:
        return legacy
    row = BY_LEGACY.get((legacy, meta))
    if not row:  # the hangar door (item_door3x3:1) isn't ported; show the metal door
        notes.append(f"{s} unmapped, using meta 0")
        row = BY_LEGACY[(legacy, 0)]
    return row["new_id"]


def state(s):
    m = STATE.match(s)
    if not m:
        return s
    return next(r["new_id"] for r in ROWS if r["legacy_id"] == m[1] and r["legacy_variant"] == m[2])


def convert_page(page, notes):
    for key in ("item", "icon"):
        if key in page:
            page[key] = item(page[key], notes)
    if "entity" in page:
        page["entity"] = page["entity"].replace("Count:", "count:")
    if "multiblock" in page:
        mapping = page["multiblock"]["mapping"]
        for k, v in mapping.items():
            mapping[k] = state(v)
    return page


def write_book_recipes():
    for name in ("techguns_manual", "techguns_manual_alt"):
        r = json.loads((LEGACY / f"recipes/{name}.json").read_text())
        ings = [{"item": item(f"{i['item']}:{i.get('data', 0)}", [])} for i in r["ingredients"]]
        tg = sorted(i["item"] for i in ings if i["item"].startswith("techguns:"))
        write_json(DATA / f"techguns/recipe/{name}.json", {
            "neoforge:conditions": [{"type": "neoforge:mod_loaded", "modid": "patchouli"}]
            + [{"type": "neoforge:item_exists", "item": i} for i in tg],
            "type": "minecraft:crafting_shapeless", "ingredients": ings,
            "result": {"id": "patchouli:guide_book", "components": {"patchouli:book": BOOK}}})


def main():
    notes = []
    book = json.loads((SRC / "book.json").read_text())
    book |= {"creative_tab": "techguns:techguns", "use_resource_pack": True}
    write_json(DATA / "techguns/patchouli_books/techguns_manual/book.json", book)
    out = ASSETS / "patchouli_books/techguns_manual"
    for src in sorted(SRC.rglob("*.json")):
        if src.name == "book.json":
            continue
        j = json.loads(src.read_text())
        if "icon" in j:
            j["icon"] = item(j["icon"], notes)
        if "category" in j:
            j["category"] = "techguns:" + j["category"]
        j["pages"] = [convert_page(p, notes) for p in j.get("pages", [])]
        if not j["pages"]:
            del j["pages"]
        write_json(out / src.relative_to(SRC), j)
    write_book_recipes()
    check(out, notes)


def check(out, notes):
    """Every converted id must exist in the port: items, multiblock blocks, recipes and images."""
    ids, missing = known_ids(), []
    for p in out.rglob("*.json"):
        j = json.loads(p.read_text())
        refs = [j.get("icon")] + [pg.get(k) for pg in j.get("pages", []) for k in ("item", "icon")]
        refs += [v for pg in j.get("pages", []) for v in pg.get("multiblock", {}).get("mapping", {}).values()]
        missing += [f"{p.name}: {r}" for r in refs if r and r.startswith("techguns:") and r not in ids]
        for pg in j.get("pages", []):
            for k in ("recipe", "recipe2"):
                if k in pg and not (DATA / "techguns/recipe" / (pg[k].split(":")[1] + ".json")).exists():
                    missing.append(f"{p.name}: recipe {pg[k]}")
            for img in pg.get("images", []):
                if not (ASSETS / img.split(":")[1]).exists():
                    missing.append(f"{p.name}: image {img}")
            assert "Count:" not in pg.get("entity", ""), p
    assert not missing, "Patchouli book refers to missing ids:\n  " + "\n  ".join(missing)
    print(f"Patchouli book: {len(list(out.rglob('*.json')))} files" + "".join(f"\n  note: {n}" for n in sorted(set(notes))))


if __name__ == "__main__":
    main()
