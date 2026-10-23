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

import port_dungeons

ROOT = Path(__file__).resolve().parents[2]
LEGACY = ROOT / "legacy/src/main/resources/assets/techguns"
LEGACY_TGITEMS = ROOT / "legacy/src/main/java/techguns/TGItems.java"
OUT = ROOT / "src/generated/resources"
ASSETS = OUT / "assets/techguns"
DATA = OUT / "data"

ROWS = list(csv.DictReader(open(Path(__file__).with_name("flattening.csv"))))
BY_LEGACY = {(r["legacy_id"], int(r["meta"])): r for r in ROWS}
LEGACY_META_IDS = {r["legacy_id"] for r in ROWS}
BLOCKS = [r for r in ROWS if r["kind"] in ("cube", "stairs", "ladder", "lamp", "camonet", "copy", "crate", "door")]
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
# 3D armor: Java renderer except in the GUI, which shows a flat icon baked from the 3D model (src/main/resources textures/item)
ICON_ARMOR = [f"{s}_{p}" for s in ("steam", "t3_power", "t4_power") for p in ("helmet", "chestplate", "leggings", "boots")]
# Shields: the vanilla shield transforms, drawn by a Java renderer, with the blocking pose
SHIELDS = ["riot_shield", "ballistic_shield", "advanced_shield"]
# 1.12 lang keys that 1.21 looks up under another name
LANG_RENAMES = {
    "techguns.radiation": "effect.techguns.radiation", "techguns.radresistance": "effect.techguns.radresistance",
    "techguns.radregeneration": "effect.techguns.radregeneration",
    "attribute.name.techguns.radiationResistance": "attribute.name.techguns.radiation_resistance",
    "item.techguns.item_bunkerdoor.name": "block.techguns.bunkerdoor", "item.techguns.item_bunkerdoor": "block.techguns.bunkerdoor",
}
# en_us keys 1.12 didn't have, for what the port adds
EXTRA_LANG = {
    "techguns.tgguitooltip.togglejump": "Toggle Jump Boost",
    "techguns.tgguitooltip.togglejump.tooltip1": "Lets your armor make",
    "techguns.tgguitooltip.togglejump.tooltip2": " you jump higher",
    "techguns.msg.jumpboost": "Jump Boost",
    "death.attack.techguns.radiation": "%s died of radiation",
    "death.attack.techguns.radiation.player": "%s died of radiation whilst fighting %s",
    "death.attack.techguns.radiation_poisoning": "%s died of radiation poisoning",
    "death.attack.techguns.radiation_poisoning.player": "%s died of radiation poisoning whilst fighting %s",
    "techguns.extendedinventory": "Techguns Inventory",
    "techguns.key.openTGInventory": "Open Techguns Inventory",
    "item.techguns.creeper_acid_bucket": "Creeper Acid Bucket",
    "block.techguns.door3x3": "Techdoor",
    "techguns.container.explosivecharge": "Mining Charge",
}
# Block items whose 1.12 item model wasn't the block's
ITEM_MODELS = {"ammo_press": "techguns:item/basicmachine", "metal_press": "techguns:item/basicmachine",
               "chem_lab": "techguns:item/basicmachine", "grinder": "techguns:item/simplemachine2_grinder_inv",
               "door3x3": "techguns:item/item_door3x3", "turret": "techguns:item/basicmachine"}
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
# Unlike 1.12, plates don't smelt back into ingots.
SMELTING = [
    ("ore_tin", "ingottin", 1, 0.5), ("ore_lead", "ingotlead", 1, 1.0),
    ("turretarmoriron", "minecraft:iron_ingot", 5, 0), ("turretarmorsteel", "ingotsteel", 5, 0),
    ("turretarmorobsidiansteel", "ingotobsidiansteel", 5, 0), ("ironbarrel", "minecraft:iron_ingot", 6, 0),
    ("obsidiansteelbarrel", "ingotobsidiansteel", 6, 0), ("rawrubber", "rubberbar", 1, 0),
    ("rawplastic", "plasticsheet", 1, 0), ("oretitanium", "ingottitanium", 1, 0),
]
# Crafting results changed from 1.12, by recipe name: circuit boards cost twice as much (1.12 made 4).
RESULT_COUNT = {"circuitboard": 2}
# Tech ladder (not 1.12): elite circuits and coils only come from the fabricator, so its own parts take basic circuits;
# the housing's cybernetic parts (fabricator-only, which made it uncraftable in 1.12) become obsidian steel parts.
REMOVED_RECIPES = {"circuitboardelite", "coil"}
KEY_SWAP = {
    "multiblockmachine_0_fabricator_housing": {"c": {"tag": "c:circuits/basic"}, "p": {"item": "techguns:mechanicalpartsobsidiansteel"}},
    "multiblockmachine_2_fabricator_controller": {"c": {"tag": "c:circuits/basic"}},
    "electricengine": {"i": {"tag": "c:plates/steel"}},
    "tacticalnukewarhead": {"c": {"tag": "c:circuits/elite"}},
}
# TGMachineRecipes as machine recipe json (MachineRecipe.java). Item specs: "id*count", "#tag", "a|b" for either;
# ids without a namespace are ours. Fluids: (key of FLUIDS, mB).
FLUIDS = {"water": {"fluid": "minecraft:water"}, "lava": {"fluid": "minecraft:lava"}, "milk": {"tag": "c:milk"},
          "acid": {"fluid": "techguns:creeper_acid"}}


def stacks(*pairs):
    return [f"{s}*{n}" for s, n in pairs if n]


# TGMachineRecipes' grinder helpers; "nuggets" of steelAndPlastic were steel ingots there too
def iron_wood(iron, wood, nuggets):
    return stacks(("minecraft:iron_ingot", iron), ("minecraft:oak_log", wood), ("minecraft:iron_nugget", nuggets))


def steel_plastic(steel, plastic, nuggets):
    return stacks(("ingotsteel", steel), ("plasticsheet", plastic), ("ingotsteel", nuggets))


def obsi_plastic(obsi, plastic):
    return stacks(("ingotobsidiansteel", obsi), ("plasticsheet", plastic))


def carbon_plastic_redstone(carbon, plastic, redstone):
    return stacks(("carbonfibers", carbon), ("plasticsheet", plastic), ("minecraft:redstone", redstone))


OBSI_BARREL = "ingotobsidiansteel*3"
GRINDER_GUNS = {
    "handcannon": ["minecraft:cobblestone*3", *iron_wood(0, 1, 4)], "sawedoff": [*iron_wood(4, 1, 0), "minecraft:flint"],
    "revolver": iron_wood(3, 1, 0), "goldenrevolver": [*iron_wood(3, 1, 0), "minecraft:gold_ingot*4"],
    "thompson": iron_wood(4, 1, 0), "ak47": [OBSI_BARREL, *iron_wood(4, 1, 0)], "boltaction": [OBSI_BARREL, *iron_wood(4, 1, 0)],
    "m4": [OBSI_BARREL, *steel_plastic(2, 2, 0)], "m4_infiltrator": [OBSI_BARREL, *steel_plastic(3, 2, 0), "minecraft:redstone"],
    "pistol": [OBSI_BARREL, *steel_plastic(0, 1, 0), *iron_wood(2, 0, 0)], "combatshotgun": [OBSI_BARREL, *steel_plastic(2, 2, 0)],
    "mac10": [OBSI_BARREL, *steel_plastic(1, 0, 5), *iron_wood(1, 0, 0)], "flamethrower": [*steel_plastic(0, 2, 0), *iron_wood(2, 0, 0)],
    "rocketlauncher": ["ingotobsidiansteel*6", *steel_plastic(1, 0, 5)],
    "grimreaper": ["ingotobsidiansteel*32", "circuitboardelite", "carbonfibers"],
    "grenadelauncher": [OBSI_BARREL, *steel_plastic(2, 2, 0)], "aug": [OBSI_BARREL, *steel_plastic(2, 2, 0)],
    "netherblaster": ["ingotobsidiansteel*4", "cyberneticparts*2"], "biogun": [*steel_plastic(2, 2, 0), "minecraft:glass*2"],
    "teslagun": carbon_plastic_redstone(3, 2, 2), "lmg": ["ingotobsidiansteel*6", "ingotsteel*4", "plasticsheet*2"],
    "minigun": ["ingotobsidiansteel*20", "ingotsteel*2", "electricengine"],
    "as50": ["ingotobsidiansteel*9", "plasticsheet*2", "minecraft:diamond"], "vector": obsi_plastic(5, 2),
    "scar": obsi_plastic(5, 2), "lasergun": [*obsi_plastic(2, 2), "minecraft:redstone*20", "minecraft:gold_ingot*3"],
    "blasterrifle": [*carbon_plastic_redstone(3, 1, 20), "minecraft:gold_ingot*3"],
    "sonicshotgun": [*carbon_plastic_redstone(3, 2, 2), "ingottitanium"],
    "pdw": [*carbon_plastic_redstone(6, 1, 0), *obsi_plastic(1, 0)],
    "pulserifle": [*carbon_plastic_redstone(6, 1, 0), *obsi_plastic(1, 0)],
    "mibgun": ["minecraft:gold_ingot*3", "ingottitanium", "minecraft:redstone*20"],
    "powerhammer": [*iron_wood(4, 0, 0), "minecraft:flint"], "chainsaw": [*iron_wood(5, 0, 0), "plasticsheet"],
    "nucleardeathray": ["carbonfibers*6", "ingotlead*3", "mechanicalpartscarbon", "circuitboardelite"],
    "gaussrifle": ["carbonfibers*9", "ingottitanium", "plasticsheet", "circuitboardelite"],
    "guidedmissilelauncher": ["ingotobsidiansteel*8", "copperwire"], "miningdrill": obsi_plastic(6, 2),
    "tfg": ["ingottitanium*5", "ingotlead*4", "carbonfibers*4"],
}
# ammo -> lead nuggets, copper nuggets, chances of the lead, copper and gunpowder
AMMO_CRUSH = {"riflerounds": (1, 2, [1, 1, 0.125]), "pistolrounds": (1, 1, [0.75, 1.5, 1 / 12]),
              "sniperrounds": (2, 4, [1, 1, 0.25]), "shotgunrounds": (1, 1, [0.5, 1, 0.0625])}
METAL1, METAL2 = "#c:ingots/steel|#c:ingots/lead", "#c:ingots/copper|#c:ingots/tin|#c:ingots/iron|#c:ingots/bronze"
# machine -> name -> MachineRecipe fields (inputs, bottle, fluid, results, chances, fluid_result, power, time, swap)
MACHINE_RECIPES = {
    "ammo_press": {name: {"inputs": [METAL1, METAL2 + "*2", "#c:gunpowders"], "results": [f"{name}*{n}"]}
                   for name, n in (("pistolrounds", 12), ("shotgunrounds", 16), ("riflerounds", 8), ("sniperrounds", 4))},
    "metal_press": {result.partition("*")[0]: {"inputs": inputs, "results": [result], "swap": True} for inputs, result in (
        (["#c:ingots/tin", "#c:ingots/tin"], "platetin*2"), (["#c:ingots/copper", "#c:ingots/copper"], "platecopper*2"),
        (["#c:ingots/bronze", "#c:ingots/bronze"], "platebronze*2"), (["#c:ingots/iron", "#c:ingots/iron"], "plateiron*2"),
        (["#c:ingots/steel", "#c:ingots/steel"], "platesteel*2"), (["#c:ingots/lead", "#c:ingots/lead"], "platelead*2"),
        (["#c:plates/iron", "minecraft:flint"], "mechanicalpartsiron"),
        (["#c:plates/obsidian_steel", "#c:gems/quartz"], "mechanicalpartsobsidiansteel"),
        (["#c:plates/carbon", "minecraft:blaze_rod"], "mechanicalpartscarbon"),  # 1.12 made 2
        (["#c:plates/copper", "#c:plates/copper"], "copperwire*8"), (["#c:fibers/carbon", "#c:fibers/carbon"], "platecarbon*2"),
        (["#c:plates/obsidian_steel", "tgx"], "advancedrounds*16"),
        (["#c:ingots/obsidian_steel", "#c:ingots/obsidian_steel"], "plateobsidiansteel*2"),
        (["#c:plates/steel", "#c:plates/bronze"], "steamarmorplate"), (["#c:ingots/gold", "#c:ingots/gold"], "goldwire*2"),
        (["#c:plates/iron", "minecraft:tnt"], "40mmgrenade*16"), (["#c:ingots/titanium", "#c:ingots/titanium"], "platetitanium*2"),
        (["#c:plates/obsidian_steel", "#c:plates/titanium"], "gaussrifleslugs*4"),
        (["sniperrounds_incendiary", "tgx"], "sniperrounds_explosive"))},
    # 1.12 branches taken: no coal dust, oil or fuel fluids, so the coal and lava variants
    "chem_lab": {
        "gunpowder": {"inputs": ["#c:dusts/redstone", "minecraft:coal"], "fluid": ("water", 250),
                      "results": ["minecraft:gunpowder"], "swap": True, "power": 5},
        "rawplastic": {"inputs": ["#c:raw_materials/rubber", "minecraft:coal"], "fluid": ("water", 1000),
                       "results": ["rawplastic"], "swap": True, "power": 25},
        "creeper_acid": {"inputs": ["biomass", "minecraft:gunpowder"], "fluid": ("water", 1000),
                         "fluid_result": ("techguns:creeper_acid", 1000), "swap": True, "power": 20},
        "tgx": {"inputs": ["#c:gunpowders", "#c:gems/lapis"], "fluid": ("lava", 500), "results": ["tgx"], "swap": True, "power": 20},
        "fueltank": {"inputs": ["fueltankempty"], "fluid": ("lava", 500), "results": ["fueltank"], "power": 1},
        "rocket_high_velocity": {"inputs": ["rocket"], "fluid": ("lava", 250), "results": ["rocket_high_velocity"], "power": 5},
        "carbonfibers": {"inputs": ["#c:gems/diamond", "minecraft:blaze_powder"], "fluid": ("lava", 1000),
                         "results": ["carbonfibers*2"], "swap": True, "power": 25},
        "rawrubber": {"inputs": ["#minecraft:logs"], "fluid": ("water", 1000), "results": ["rawrubber"], "power": 20},
        "biotank": {"inputs": ["biomass"], "bottle": "biotankempty", "fluid": ("water", 500), "results": ["biotank"], "power": 1},
        "compressedairtank": {"inputs": ["minecraft:coal"], "bottle": "compressedairtankempty", "fluid": ("water", 250),
                              "results": ["compressedairtank"], "swap": True, "power": 5},
        "nethercharge": {"inputs": ["minecraft:netherrack", "minecraft:soul_sand"], "fluid": ("lava", 1000),
                         "results": ["nethercharge*4"], "swap": True, "power": 20},
        "yellowcake": {"inputs": ["#c:ores/uranium"], "fluid": ("acid", 250), "results": ["yellowcake*3"], "power": 20},
        "nuclearpowercell": {"inputs": ["enricheduranium"], "bottle": "nuclearpowercelldepleted", "fluid": ("water", 1000),
                             "results": ["nuclearpowercell"], "swap": True, "power": 40},
        "slime_ball": {"inputs": ["minecraft:green_dye", "#c:raw_materials/rubber"], "fluid": ("milk", 500),
                       "results": ["minecraft:slime_ball"], "swap": True, "power": 25},
        "treatedleather": {"inputs": ["minecraft:leather*2", "#c:slime_balls"], "fluid": ("acid", 500),
                           "results": ["treatedleather*2"], "swap": True, "power": 20},
        "leather": {"inputs": ["minecraft:rotten_flesh"], "fluid": ("water", 500), "results": ["minecraft:leather"], "power": 15},
        "light_gray_concrete": {"inputs": ["minecraft:gravel", "minecraft:sand"], "fluid": ("water", 250),
                                "results": ["minecraft:light_gray_concrete*2"], "power": 5},
        "clay": {"inputs": ["minecraft:gravel", "minecraft:dirt"], "fluid": ("water", 250), "results": ["minecraft:clay*2"], "power": 5},
        **{ammo + "_incendiary": {"inputs": [f"{ammo}*{n}", "minecraft:blaze_powder"], "fluid": ("lava", 250),
                                  "results": [f"{ammo}_incendiary*{n}"], "power": 25}
           for ammo, n in (("riflerounds", 1), ("pistolrounds", 2), ("sniperrounds", 1), ("shotgunrounds", 8))},
        "radpills": {"inputs": ["minecraft:sugar*4", "minecraft:glistering_melon_slice"], "bottle": "minecraft:glass_bottle",
                     "fluid": ("milk", 1000), "results": ["radpills*4"], "swap": True, "power": 20},
        "radaway": {"inputs": ["minecraft:nether_wart", "minecraft:glistering_melon_slice"], "bottle": "infusionbag",
                    "fluid": ("acid", 250), "results": ["radaway"], "swap": True, "power": 25},
    },
    "blast_furnace": {
        "ingotsteel": {"inputs": ["minecraft:iron_ingot*4", "minecraft:coal"], "results": ["ingotsteel*4"], "power": 10, "time": 800},
        "ingotsteel_charcoal": {"inputs": ["minecraft:iron_ingot*4", "minecraft:charcoal"], "results": ["ingotsteel*4"],
                                "power": 10, "time": 800},
        "ingotobsidiansteel": {"inputs": ["#c:ingots/steel", "minecraft:obsidian"], "results": ["ingotobsidiansteel"],
                               "power": 10, "time": 200},
        "ingotbronze": {"inputs": ["#c:ingots/copper*3", "#c:ingots/tin"], "results": ["ingotbronze*4"], "power": 10, "time": 100},
    },
    # time: 1.12's charge amount / 800 FE/t
    "charging_station": {"energycell": {"inputs": ["energycellempty"], "results": ["energycell"], "time": 62},
                         "redstone_battery": {"inputs": ["redstone_battery_empty"], "results": ["redstone_battery"], "time": 25}},
    # ponytail: 1.12's generic armor recipes (its crafting ingredients back) aren't ported
    "grinder": {
        **{gun: {"inputs": [gun], "results": results} for gun, results in GRINDER_GUNS.items()},
        "platecarbon": {"inputs": ["platecarbon"], "results": ["carbonfibers"]},
        **{ammo + v: {"inputs": [ammo + v], "results": [f"nuggetlead*{lead}", f"nuggetcopper*{copper}", "minecraft:gunpowder",
                                                        *(["minecraft:blaze_powder"] if v else [])],
                      "chances": chances + ([0.125] if v else [])}
           for ammo, (lead, copper, chances) in AMMO_CRUSH.items() for v in ("", "_incendiary")},
        "sniperrounds_explosive": {"inputs": ["sniperrounds_explosive"], "chances": [1, 1, 0.25, 0.125, 0.5], "results": [
            "nuggetlead*2", "nuggetcopper*4", "minecraft:gunpowder", "minecraft:blaze_powder", "tgx"]},
    },
}


# Multiblock machines (M5.2). Fabricator: input, wire, powder, plate (TGMachineRecipes.addFabricatorRecipes)
MACHINE_RECIPES["fabricator"] = {result.partition("*")[0]: {"inputs": inputs, "results": [result]} for inputs, result in (
    (["#c:ingots/gold", "#c:wires/copper", "#c:dusts/redstone*3", "#c:plates/plastic"], "energycellempty"),
    (["minecraft:soul_sand", "#c:wires/gold", "#c:dusts/redstone", "#c:plates/plastic"], "cyberneticparts"),
    (["#c:ingots/titanium*2", "#c:circuits/basic*4", "mechanicalpartscarbon", "#c:plates/carbon*4"], "powerplating*2"),
    (["coil", "#c:circuits/elite*2", "mechanicalpartscarbon", "#c:plates/titanium"], "sonicemitter"),
    (["enricheduranium", "#c:circuits/elite*2", "mechanicalpartscarbon*2", "#c:plates/lead*2"], "rademitter"),
    (["#c:ingots/steel", "#c:circuits/basic", "#c:dusts/redstone*4", "#c:plates/lead*2"], "nuclearpowercelldepleted"),
    # not in 1.12: crafted there (see REMOVED_RECIPES)
    (["#c:circuits/basic*2", "#c:wires/gold*2", "#c:dusts/glowstone*2", "#c:plates/plastic"], "circuitboardelite"),
    (["#c:ingots/iron*2", "#c:wires/copper*4", "#c:dusts/redstone", "#c:plates/iron"], "coil"))}
# Reaction chamber (ReactionChamberRecipe.java): input, focus, fluid, results, then ticks, required_completion, intensity,
# margin, liquid_level, liquid_consumption, instability, risk, power. ponytail: no liquid redstone/ender yet, lava instead
RC_RECIPES = {
    "uv_wheat": ("minecraft:wheat_seeds", "rcuvemitter", "water", ["minecraft:wheat", "minecraft:wheat_seeds*2"],
                 10, 5, 3, 1, 1, 1000, 0.5, "explosion_low", 50000),
    "laser_focus": ("#c:gems/diamond", "rcheatray", "lava", ["laserfocus"], 10, 5, 3, 1, 4, 4000, 0.5, "break_item", 100000),
    "titanium": ("ore_titanium", "rcheatray", "acid", ["oretitanium*2", "minecraft:iron_ore"], 2, 1, 5, 0, 3, 100, 0, "break_item", 25000),
    "blazerod": ("quartzrod", "rcheatray", "lava", ["minecraft:blaze_rod"], 5, 3, 7, 2, 4, 1000, 0.5, "break_item", 250000),
    "glowstone": ("#c:storage_blocks/redstone", "rcheatray", "lava", ["minecraft:glowstone"], 5, 3, 7, 2, 4, 1000, 0.5,
                  "explosion_medium", 250000),
    "antigrav": ("minecraft:nether_star", "rcheatray", "lava", ["antigravcore"], 10, 7, 8, 2, 4, 4000, 1, "explosion_low", 500000),
    "uranium": ("#c:dusts/uranium", "rcheatray", "water", ["enricheduranium"], 5, 4, 7, 0, 3, 1000, 0, "break_item", 250000),
}
RC_FIELDS = ("ticks", "required_completion", "intensity", "margin", "liquid_level", "liquid_consumption", "instability", "risk", "power")
CUTOUT_MODELS += ["fabricator_glass", "fabricator_glass_formed", "reactionchamber_glass", "reactionchamber_glass_formed",
                  "reactionchamber_controller", "oredrill_scaffold", "oredrill_scaffold_c",
                  *(f"ore_cluster_{t}" for t in ("coal", "common_metal", "rare_metal", "shiny_metal", "uranium", "common_gem",
                                                  "shiny_gem", "nether_crystal", "oil"))]
EXTRA_LANG |= {"block.techguns.ore_cluster_oil": "Ore Cluster (Oil)"}
EXTRA_LANG |= {"block.techguns.creative_energy": "Creative Energy Source"}  # not in 1.12; its models are hand-written
# tooltips show percents (DamageSystem.protectionPercent/penetrationPercent) instead of 1.12's raw points
EXTRA_LANG |= {"techguns.armorTooltip.resistances": "Protection", "techguns.gun.tooltip.armorPen": "Ignores armor"}
# TGConfig entries in NeoForge's config screen (techguns.configuration.<name>); the spec comments are the tooltips
EXTRA_LANG |= {"techguns.configuration.title": "Techguns Configuration"} | {f"techguns.configuration.{k}": v for k, v in {
    "bloom": "Bloom", "bloomIntensity": "Bloom Intensity", "lightTint": "Light Tint", "scopeLens": "Scope Lens Effect",
    "screenShake": "Screen Shake", "damagePvP": "PvP Damage Factor", "damageTurretToPlayer": "Turret to Player Damage Factor",
    "damageFactorNPC": "NPC Damage Factor", "helicopterSelfDamage": "Helicopters Hurt by Own Rockets",
    "explosionBlockDamage": "Guns Break Blocks", "restrictUnsafeModeToOP": "Unsafe Mode Only for OPs",
    "disableAutofeeder": "Disable Autofeeder", "radiation": "Radiation", "machinesNeedNoPower": "Machines Need No Power",
    "upgradeXPCost": "Upgrade Bench XP per Level", "explosiveChargeMaxHardness": "Mining Charge Max Hardness",
    "explosiveChargeAdvancedMaxHardness": "Advanced Mining Charge Max Hardness", "oreDrillMultiplierOre": "Ore Drill Ore Multiplier",
    "oreDrillMultiplierPower": "Ore Drill Power Multiplier", "oreDrillFuelMultiplier": "Ore Drill FE per Fuel",
    "distanceSpawnLevel0": "Danger Level 0 Distance", "distanceSpawnLevel1": "Danger Level 1 Distance",
    "distanceSpawnLevel2": "Danger Level 2 Distance", "spawnWeightZombieSoldier": "Zombie Soldier Spawn Weight",
    "spawnWeightZombieFarmer": "Zombie Farmer Spawn Weight", "spawnWeightZombieMiner": "Zombie Miner Spawn Weight",
    "spawnWeightSkeletonSoldier": "Skeleton Soldier Spawn Weight", "spawnWeightPsychoSteve": "Psycho Steve Spawn Weight",
    "spawnWeightBandit": "Bandit Spawn Weight", "spawnWeightZombiePigmanSoldier": "Zombie Pigman Soldier Spawn Weight",
    "spawnWeightCyberDemon": "Cyber Demon Spawn Weight", "npcBiomeBlacklist": "NPC Biome Blacklist"}.items()}


def write_reaction_chamber_recipes():
    for name, (inp, focus, fluid, results, *values) in RC_RECIPES.items():
        used = {ns(focus)}
        out = {"type": "techguns:reaction_chamber", "input": machine_ingredient(inp, used), "focus": ns(focus), "fluid": FLUIDS[fluid],
               "results": [convert_result({"item": ns(i), "count": int(n or 1)}, used)
                           for i, _, n in (res.partition("*") for res in results)]}
        out |= dict(zip(RC_FIELDS, values))
        out["neoforge:conditions"] = conditions(used)
        write_json(DATA / "techguns/recipe/reaction_chamber" / (name + ".json"), out)
    return len(RC_RECIPES)


# Special mobs (M6.2): entity names, spawn eggs, entity loot tables.
SPECIAL_MOBS = {"AlienBug": "Alien Bug", "Ghastling": "Ghastling", "AttackHelicopter": "Attack Helicopter"}
EXTRA_LANG |= {f"item.techguns.{m.lower()}_spawn_egg": f"{name} Spawn Egg" for m, name in SPECIAL_MOBS.items()}
# 1.12 entity loot tables of the special mobs, converted by write_npcs (the alien bug had none).
ENTITY_LOOT = ["attackhelicopter", "ghastling"]


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
    text = re.sub(r'"((?:minecraft:)?block/door_(?:top|bottom))(_rh)?"', lambda m: f'"{m[1]}_{"right" if m[2] else "left"}"', text)
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
            if src.suffix == ".json":
                dst.write_text(fix_paths(src.read_text(encoding="utf-8")), encoding="utf-8")
            elif src.suffix == ".mtl":
                dst.write_text(src.read_text(encoding="utf-8").replace("techguns:blocks/", "techguns:block/"), encoding="utf-8")
            else:
                shutil.copy2(src, dst)
    shutil.copytree(LEGACY / "sounds", ASSETS / "sounds")
    # 1.12's extra "category" key is ignored; its shishkebab swings point at misspelled files
    (ASSETS / "sounds.json").write_text((LEGACY / "sounds.json").read_text().replace("shishkebap_swing", "shishkebab_swing"))
    shutil.copytree(LEGACY / "particles", ASSETS / "particles")  # TGFX definitions, read by client.particle.TGFX
    for name in BEWLR_ITEMS:
        write_json(ASSETS / "models/item" / (name + ".json"),
                   {"parent": "minecraft:builtin/entity", "textures": {"particle": "techguns:item/plateiron"}})
    for name in ICON_ARMOR:
        write_json(ASSETS / "models/item" / (name + ".json"), {
            "loader": "neoforge:separate_transforms", "gui_light": "front",
            "base": {"parent": "minecraft:builtin/entity"},
            "perspectives": {"gui": {"loader": "neoforge:item_layers", "gui_light": "front", "textures": {"layer0": f"techguns:item/{name}"}}},
            "textures": {"particle": f"techguns:item/{name}"}})
    for name in SHIELDS:
        write_json(ASSETS / "models/item" / (name + ".json"), {"parent": "minecraft:item/shield", "overrides": [
            {"predicate": {"blocking": 1}, "model": f"techguns:item/{name}_blocking"}]})
        write_json(ASSETS / "models/item" / (name + "_blocking.json"), {"parent": "minecraft:item/shield_blocking"})
    for name in CUTOUT_MODELS:
        path = ASSETS / "models/block" / (name + ".json")
        write_json(path, json.loads(path.read_text()) | {"render_type": "minecraft:cutout"})
    # creeper acid (TGFluids): the fluid block only needs a particle, the bucket shows the fluid
    write_json(ASSETS / "blockstates/block_creeper_acid.json", {"variants": {"": {"model": "techguns:block/block_creeper_acid"}}})
    write_json(ASSETS / "models/block/block_creeper_acid.json", {"textures": {"particle": "techguns:block/acid_still"}})
    write_json(ASSETS / "models/item/creeper_acid_bucket.json",
               {"parent": "neoforge:item/bucket", "loader": "neoforge:fluid_container", "fluid": "techguns:creeper_acid"})


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
        elif r["kind"] == "crate":  # Forge v1: one model, per-type textures
            model = f"techguns:block/{name}"
            write_json(ASSETS / "models" / (short(model) + ".json"),
                       {"parent": legacy["defaults"]["model"], "textures": legacy["variants"][prop][value]["textures"]})
            state = {"variants": {"": {"model": model}}}
            item_model = {"parent": model}
        elif r["kind"] == "door":  # 1.21 doors have the same states plus powered, which never changes the model
            state = {"variants": {k.replace(",powered=false", ""): v for k, v in legacy["variants"].items() if "powered=true" not in k}}
            item_model = {"parent": f"techguns:item/item_{name}"}
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
        for variant in variants.values():
            if variant["model"].endswith(".obj"):  # Forge v1 OBJ variant -> a NeoForge obj loader model
                obj = short(variant.pop("model")).removeprefix("block/")
                variant.pop("transform", None)
                variant["model"] = f"techguns:block/{name}"
                write_json(ASSETS / "models/block" / (name + ".json"), {
                    "parent": "minecraft:block/block", "loader": "neoforge:obj", "model": f"techguns:models/block/{obj}",
                    "flip_v": True, "textures": {"particle": "techguns:block/" + (name if (ASSETS / f"textures/block/{name}.png").exists() else r["legacy_variant"].split("=")[1])}})
        write_json(ASSETS / "blockstates" / (short(r["new_id"]) + ".json"), {"variants": variants})
        model = next(iter(variants.values()))["model"]
        if name in ITEM_MODELS:
            item_model = {"parent": ITEM_MODELS[name]}
        elif r["kind"] == "ladder":
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
            elif m := re.fullmatch(r"(?:item\.techguns|techguns\.item)\.(.+)\.name", key):  # armor used the latter
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
        if name in REMOVED_RECIPES:
            continue
        for key, ing in KEY_SWAP.get(name, {}).items():
            out["key"][key] = ing
            used |= {ing["item"]} if "item" in ing else set()
        if name in RESULT_COUNT:
            out["result"]["count"] = RESULT_COUNT[name]
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


def machine_ingredient(spec, used):
    spec, _, count = spec.partition("*")
    options = []
    for option in spec.split("|"):
        if option.startswith("#"):
            options.append({"tag": option[1:]})
        else:
            if ":" not in option:
                used.add(ns(option))
            options.append({"item": ns(option)})
    ing = options[0] if len(options) == 1 else {"type": "neoforge:compound", "children": options}
    return ing | ({"count": int(count)} if count else {})


def write_machine_recipes():
    count = 0
    for machine, recipes in MACHINE_RECIPES.items():
        for name, r in recipes.items():
            used = set()
            out = {"type": "techguns:" + machine, "inputs": [machine_ingredient(i, used) for i in r["inputs"]]}
            if "bottle" in r:
                out["bottle"] = machine_ingredient(r["bottle"], used)
            if "fluid" in r:
                out["fluid"] = FLUIDS[r["fluid"][0]] | {"amount": r["fluid"][1]}
            if "results" in r:
                out["results"] = [convert_result({"item": ns(i), "count": int(n or 1)}, used)
                                  for i, _, n in (res.partition("*") for res in r["results"])]
            if "fluid_result" in r:
                out["fluid_result"] = {"id": r["fluid_result"][0], "amount": r["fluid_result"][1]}
            out |= {k: r[k] for k in ("chances", "power", "time", "swap") if k in r}
            if used:
                out["neoforge:conditions"] = conditions(used)
            write_json(DATA / "techguns/recipe" / machine / (name + ".json"), out)
            count += 1
    return count


def write_tags_and_loot():
    item_tags, block_tags = defaultdict(list), defaultdict(list)
    for ore, item in ITEM_ORE_DICT.items():
        item_tags[ORE_TAGS[ore]].append(ns(item))
    for r in BLOCKS:
        rid, name = r["new_id"], short(r["new_id"])
        if name in SHOVEL_BLOCKS:
            block_tags["minecraft:mineable/shovel"].append(rid)
        elif r["kind"] == "crate":
            block_tags["minecraft:mineable/axe"].append(rid)
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
        if r["kind"] == "crate":  # drops its 1.12 loot table. ponytail: silk touch too, not the crate
            write_json(DATA / "techguns/loot_table/blocks" / (name + ".json"),
                       convert_chest_loot(json.loads((LEGACY / "loot_tables/blocks" / (name + ".json")).read_text()), "blocks/" + name, "block"))
            continue
        if r["kind"] == "door":
            block_tags["minecraft:doors"].append(rid)
            item_tags["minecraft:doors"].append(rid)
        lower = [{"condition": "minecraft:block_state_property", "block": rid, "properties": {"half": "lower"}}] if r["kind"] == "door" else []
        write_json(DATA / "techguns/loot_table/blocks" / (name + ".json"), {
            "type": "minecraft:block", "random_sequence": "techguns:blocks/" + name,
            "pools": [{"rolls": 1, "bonus_rolls": 0, "entries": [{"type": "minecraft:item", "name": rid}],
                       "conditions": [{"condition": "minecraft:survives_explosion"}] + lower}]})
    # Shared items 1.12 let into the ammo slots of the TG inventory.
    item_tags["techguns:ammo_slot"] = sorted(ns(m) for m in re.findall(
        r'addsharedVariant\("(\w+)"[^;]*TGSlotType\.AMMOSLOT', LEGACY_TGITEMS.read_text()))
    for kind, tags in (("item", item_tags), ("block", block_tags)):
        for tag, values in tags.items():
            tag_ns, path = tag.split(":")
            write_json(DATA / tag_ns / "tags" / kind / (path + ".json"), {"values": values})


# NPCs (entities.npcs.TGNpcs) with a spawn egg and a 1.12 loot table
NPCS = ["armysoldier", "bandit", "commando", "cyberdemon", "dictatordave", "outcast", "psychosteve", "skeletonsoldier", "stormtrooper",
        "supermutantbasic", "supermutantelite", "supermutantheavy", "zombiefarmer", "zombieminer", "zombiepigmansoldier",
        "zombiepoliceman", "zombiesoldier"]


def loot_count(count):
    return {"type": "minecraft:uniform", "min": count["min"], "max": count["max"]}


def convert_npc_loot(table, name):
    """1.12 entity loot table: set_data picks the flattened item, looting_enchant and random_chance_with_looting become their 1.21 forms."""
    pools = []
    for pool in table["pools"]:
        entries = []
        for e in pool["entries"]:
            item, functions = e["name"], []
            for f in e.get("functions", []):
                if f["function"] == "set_data":
                    item = BY_LEGACY[(item, f["data"])]["new_id"]
                elif f["function"] == "set_count":
                    functions.append({"function": "minecraft:set_count", "count": loot_count(f["count"])})
                else:  # looting_enchant
                    functions.append({"function": "minecraft:enchanted_count_increase", "enchantment": "minecraft:looting",
                                      "count": loot_count(f["count"])})
            conditions = [{"condition": "minecraft:random_chance_with_enchanted_bonus", "enchantment": "minecraft:looting",
                           "unenchanted_chance": c["chance"], "enchanted_chance": {
                               "type": "minecraft:linear", "base": c["chance"] + c["looting_multiplier"],
                               "per_level_above_first": c["looting_multiplier"]}} for c in e.get("conditions", [])]
            entries.append({"type": "minecraft:item", "name": item, "weight": e.get("weight", 1), "functions": functions,
                            "conditions": conditions})
        pools.append({"rolls": pool["rolls"], "bonus_rolls": 0, "entries": entries})
    return {"type": "minecraft:entity", "random_sequence": "techguns:entities/" + name, "pools": pools}


def convert_chest_loot(table, name, kind="chest"):
    """1.12 chest or crate loot table: item and loot_table entries; set_data picks the flattened item."""
    pools = []
    for pool in table["pools"]:
        entries = []
        for e in pool["entries"]:
            if e["type"] == "loot_table":
                entries.append({"type": "minecraft:loot_table", "value": e["name"], "weight": e.get("weight", 1)})
                continue
            item, functions = e["name"], []
            for f in e.get("functions", []):
                if f["function"] == "set_data":
                    item = BY_LEGACY[(item, f["data"])]["new_id"]
                else:  # set_count
                    c = f["count"]
                    functions.append({"function": "minecraft:set_count", "count": loot_count(c) if isinstance(c, dict) else c})
            entries.append({"type": "minecraft:item", "name": item, "weight": e.get("weight", 1)} | ({"functions": functions} if functions else {}))
        rolls = pool["rolls"]
        pools.append({"rolls": loot_count(rolls) if isinstance(rolls, dict) else rolls, "bonus_rolls": 0, "entries": entries})
    return {"type": "minecraft:" + kind, "random_sequence": "techguns:" + name, "pools": pools}


def write_chest_loot():
    for src in (LEGACY / "loot_tables/chests").glob("*.json"):
        write_json(DATA / "techguns/loot_table/chests" / src.name, convert_chest_loot(json.loads(src.read_text()), "chests/" + src.stem))


def write_npcs():
    """Entity names under their 1.21 keys, spawn egg models and names, NPC loot tables."""
    for path in (ASSETS / "lang").glob("*.json"):
        lang = {}
        for key, value in json.loads(path.read_text(encoding="utf-8")).items():
            m = re.fullmatch(r"entity\.techguns\.(\w+)\.name", key)
            lang["entity.techguns." + m[1].lower() if m else key] = value
        if path.stem == "en_us":
            for npc in NPCS:
                lang[f"item.techguns.{npc}_spawn_egg"] = lang["entity.techguns." + npc] + " Spawn Egg"
        write_json(path, lang)
    for npc in NPCS:
        write_json(ASSETS / "models/item" / f"{npc}_spawn_egg.json", {"parent": "minecraft:item/template_spawn_egg"})
        src = LEGACY / "loot_tables/entities" / f"{npc}.json"
        if src.exists():  # heavy and elite mutants use the basic one
            write_json(DATA / "techguns/loot_table/entities" / src.name, convert_npc_loot(json.loads(src.read_text()), npc))
    for name in ENTITY_LOOT:
        src = LEGACY / "loot_tables/entities" / f"{name}.json"
        write_json(DATA / "techguns/loot_table/entities" / src.name, convert_npc_loot(json.loads(src.read_text()), name))


def main():
    shutil.rmtree(OUT, ignore_errors=True)
    copy_textures_and_models()
    write_blockstates()
    convert_lang()
    count, skipped = convert_recipes()
    count += write_machine_recipes()
    count += write_reaction_chamber_recipes()
    write_tags_and_loot()
    write_chest_loot()
    write_npcs()
    import port_structures
    port_structures.main()
    port_dungeons.main()
    import port_patchouli
    port_patchouli.main()
    print(f"{len(ITEMS)} items, {len(BLOCKS)} blocks, {count} recipes written")
    for reason, files in sorted(skipped.items(), key=lambda kv: -len(kv[1])):
        print(f"  skipped {len(files):3} recipes: {reason}")


if __name__ == "__main__":
    main()
