# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

Techguns 2: a Minecraft mod (guns, armor, machines, NPCs). This branch (`port/1.21.1`) is porting it from 1.12.2 Forge to **1.21.1 NeoForge**. `PORTING.md` holds the plan and milestone status; update its checkboxes as milestones complete.

- `src/` — the new NeoForge 1.21.1 mod (Java 21, ModDevGradle, Parchment mappings). Versions are in `gradle.properties`.
- `legacy/` — the original 1.12.2 source and build files. Not compiled; read it as the reference for behavior when porting a package. Don't edit it.

## Build

Requires JDK 21 (the system default `java` may be a different version; set `JAVA_HOME` to a JDK 21).

- `./gradlew build` — compile and build the jar into `build/libs/`
- `./gradlew runClient` / `./gradlew runServer` — dev runs in `run/` (`run/eula.txt` must say `eula=true` for the server)
- `python3 tools/port/port_assets.py` — regenerates all of `src/generated/resources/` from `legacy/` (see below). Don't run `./gradlew runData`; nothing uses Java datagen and it writes to the same folder.
- `./gradlew runGameTestServer` — runs the GameTests in `techguns.gametest`; `data/techguns/structure/empty.nbt` is their air template

## Resources pipeline

`src/generated/resources/` is script output, committed so the build doesn't need Python. Never edit it by hand; change `tools/port/port_assets.py` or `tools/port/flattening.csv` and rerun. Hand-written resources go in `src/main/resources/`.

- `flattening.csv` maps each 1.12 `(id, metadata)` to a 1.21 id. `kind` drives generation: `item`; block kinds `cube`/`stairs`/`ladder` (filter a variants blockstate), `lamp` (filter a multipart blockstate), `camonet` (Forge v1 blockstate → multipart) and `copy` (blockstate used as-is), which all get an item model, loot table and tags; `vanilla` (mapped to a minecraft id). `legacy_variant` is the property filter (e.g. `type=metal`) used to cut that block's variants out of the legacy blockstate.
- Porting a new metadata block: add rows to the CSV, register the block in `TGBlocks` with the same id, rerun the script.
- Recipes are converted for everything and gated by `neoforge:item_exists`, so they activate when their items get registered. The script prints the recipe types it skips.
- Item ids keep their 1.12 names (`plateiron`, `ingotsteel`).
- Sounds and `sounds.json` are copied as-is. Items drawn by a Java renderer go in the `BEWLR_ITEMS` list, which gives them a `builtin/entity` item model.
- `python3 tools/port/port_models.py guns/ModelM4 ...` copies 1.12 model classes from `legacy/` into `src/.../client/models/`, swapping imports for the `ModelRenderer`/`LegacyGl` shims. It overwrites the target, so fixes to ported models go in the script or the shims.

## Legacy architecture (what you're porting from)

**Init flow.** `legacy/.../Techguns.java` holds an array of `ITGInitializer` instances (`TGItems`, `TGArmors`, `AmmoTypes`, `TGuns`, `TGFluids`, `TGBlocks`, `TGEntities`, `TGPackets`, `TGRadiationSystem`, `TGOreClusters`, `TGPermissions`) called in order for `preInit/init/postInit`. Objects are created in `preInit` into static fields; Forge registry registration happens in `CommonProxy`'s `RegistryEvent.Register` handlers. Client-only code is in `client.ClientProxy`.

**Guns.** Defined in `TGuns.java` as `GenericGun` (or subclasses in `items/guns/`) with a fluent setter chain. A gun fires via a `ProjectileSelector`/`ChargedProjectileSelector` mapping an `AmmoType` (`items/guns/ammo/AmmoTypes`) and its variants (default/incendiary/explosive) to projectile factories in `entities/projectiles/`. Magazines and most crafting items are metadata variants of one item, `GenericItemShared` (`TGItems.SHARED_ITEM`).

**Rendering.** A coremod (`core/TechgunsFMLPlugin` → `TechgunsASMTransformer`) patches vanilla item rendering to call `client/render/ItemRenderHack`, which dispatches to `IItemTGRenderer` items. Models are mostly Techne-style `ModelBase` Java classes in `client/models/`, plus OBJ files in `assets/techguns/models/`.

**Networking.** 26 packets registered in `TGPackets` with sequential IDs.

**Other systems:** damage system (`damagesystem/`), player capabilities incl. extra equipment slots (`capabilities/TGExtendedPlayer`), radiation, multiblock machines (`tileentities/*Master`/`*Slave`, recipes in `TGMachineRecipes`), NPC factions, structure worldgen (`world/`; structures in a custom text format in `assets/techguns/structures/`, dungeon segments as Java-serialized `.ser` files). Integrations in `plugins/` gated by `Loader.isModLoaded`.

**Dev switches.** `TGItems.WRITE_ITEM_JSON` / `WRITE_RECIPES` generated model and recipe JSON in the old build; datagen replaces them in the port.
