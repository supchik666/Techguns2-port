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
- `./gradlew runData` — datagen into `src/generated/resources/`
- `./gradlew runGameTestServer` — runs NeoForge GameTests for the `techguns` namespace (crashes if none are registered)

## Legacy architecture (what you're porting from)

**Init flow.** `legacy/.../Techguns.java` holds an array of `ITGInitializer` instances (`TGItems`, `TGArmors`, `AmmoTypes`, `TGuns`, `TGFluids`, `TGBlocks`, `TGEntities`, `TGPackets`, `TGRadiationSystem`, `TGOreClusters`, `TGPermissions`) called in order for `preInit/init/postInit`. Objects are created in `preInit` into static fields; Forge registry registration happens in `CommonProxy`'s `RegistryEvent.Register` handlers. Client-only code is in `client.ClientProxy`.

**Guns.** Defined in `TGuns.java` as `GenericGun` (or subclasses in `items/guns/`) with a fluent setter chain. A gun fires via a `ProjectileSelector`/`ChargedProjectileSelector` mapping an `AmmoType` (`items/guns/ammo/AmmoTypes`) and its variants (default/incendiary/explosive) to projectile factories in `entities/projectiles/`. Magazines and most crafting items are metadata variants of one item, `GenericItemShared` (`TGItems.SHARED_ITEM`).

**Rendering.** A coremod (`core/TechgunsFMLPlugin` → `TechgunsASMTransformer`) patches vanilla item rendering to call `client/render/ItemRenderHack`, which dispatches to `IItemTGRenderer` items. Models are mostly Techne-style `ModelBase` Java classes in `client/models/`, plus OBJ files in `assets/techguns/models/`.

**Networking.** 26 packets registered in `TGPackets` with sequential IDs.

**Other systems:** damage system (`damagesystem/`), player capabilities incl. extra equipment slots (`capabilities/TGExtendedPlayer`), radiation, multiblock machines (`tileentities/*Master`/`*Slave`, recipes in `TGMachineRecipes`), NPC factions, structure worldgen (`world/`; structures in a custom text format in `assets/techguns/structures/`, dungeon segments as Java-serialized `.ser` files). Integrations in `plugins/` gated by `Loader.isModLoaded`.

**Dev switches.** `TGItems.WRITE_ITEM_JSON` / `WRITE_RECIPES` generated model and recipe JSON in the old build; datagen replaces them in the port.
