# Porting Techguns2: 1.12.2 Forge → 1.21.1 NeoForge

Branch: `port/1.21.1`. The 1.12.2 code lives in `legacy/` (not compiled) as a reference; port one package at a time into `src/`.

**License:** `LICENSE.txt` forbids releasing modified versions without the authors' permission. A private port or an upstream PR is fine; publishing needs pWn3d1337's approval.

**Out of scope:** migrating 1.12 worlds, Albedo lighting, and the OpenComputers, Thermal and Chisel integrations (none have 1.21.1 versions). Lost Cities was a dependency but is unused in code, so it's dropped.

## Status

- [x] M0. Project setup
- [ ] M1. Registration and data
- [ ] M2. One gun working end to end
- [ ] M3. Remaining guns and their models
- [ ] M4. Player data, armor and damage
- [ ] M5. Machines and GUIs
- [ ] M6. NPCs
- [ ] M7. World generation
- [ ] M8. Config and integrations

Each milestone ends with the game launching and something testable in game.

## What's being ported

- ~100k lines of Java in 758 files.
- Client code (32k lines) is the risk: 89 hand-coded `ModelBase` models, 73 files with raw OpenGL calls, item rendering via an ASM patch to vanilla classes.
- Metadata variants: one shared item (`GenericItemShared`) holds ~165 variants; 21 blocks use metadata the same way. These must be flattened into separate items/blocks.
- Data: 356 recipe JSONs, 32 loot tables, `.lang` files, a Patchouli book.
- Structures in a custom text format (`assets/techguns/structures/`) plus Java-serialized `.ser` dungeon segments.

## Milestones

### M0. Project setup ✅
Fresh NeoForge MDK (ModDevGradle, Java 21, Parchment, Gradle wrapper), `neoforge.mods.toml`, mod id `techguns`, CI workflow. An empty mod loads on a dev server.

### M1. Registration and data
- Replace the `ITGInitializer` classes with `DeferredRegister` for items, blocks, block entities, entities, menus, sounds, effects, data components and the creative tab.
- Write a flattening table `(old id, meta) → new id` for the shared item and the 21 meta blocks. M7 reuses it for structure conversion.
- Use datagen for item/block models, blockstates, lang JSON, recipes, loot tables and tags. Ore dictionary → `c:` tags.
- Custom recipe factories (`legacy/.../recipes/`) → `RecipeSerializer`s.
- Rename texture folders to 1.13+ names (`item/`, `block/`).

### M2. One gun end to end (pistol)
Validates the architecture; everything after is repetition.
- Gun and magazine NBT (ammo count, ammo variant, camo, upgrades) → Data Components with codecs.
- `GenericGun`, `ProjectileSelector`, `AmmoTypes` and one projectile entity with its renderer.
- All 26 packets → `CustomPacketPayload` + `StreamCodec`.
- Drop the ASM coremod. Render guns through `IClientItemExtensions` with a `BlockEntityWithoutLevelRenderer`; `ItemDisplayContext` replaces `TransformType`.

If this can't reproduce the gun animations, find out here, not after 35 guns.

### M3. Remaining guns and models
- Gun definitions in `TGuns` are mostly data; cheap once M2 works.
- Write a script converting the Techne-style `ModelRenderer` classes to `ModelPart`/`LayerDefinition` instead of porting 89 models by hand.
- OBJ models via the `neoforge:obj` loader.
- OpenGL calls → `PoseStack`, `VertexConsumer`, `RenderType`.
- Custom particles → `ParticleRenderType`.

### M4. Player data, armor and damage
- Player capabilities (extra slots, shooter values, death type) → Data Attachments, synced with payloads.
- Keep `TGDamageSource` as a `DamageSource` subclass, add data-driven `DamageType`s; armor penetration in `LivingIncomingDamageEvent`.
- Armor materials are a registry; stats via the attribute-modifier component; custom armor models via `IClientItemExtensions`.
- Radiation → `MobEffect` + `Potion`.

### M5. Machines and GUIs
- `TileEntity` → `BlockEntity`, `ITickable` → `BlockEntityTicker`.
- Item/energy/fluid handlers via `RegisterCapabilitiesEvent`.
- `Container` → `AbstractContainerMenu` + `MenuType`; `GuiContainer` → `AbstractContainerScreen` with `GuiGraphics` (72 GUI files).
- Keep the master/slave multiblock design.

### M6. NPCs
- 27 mob types. `EntityAIBase` → `Goal`.
- Attributes via `EntityAttributeCreationEvent`; spawning via biome modifiers + `RegisterSpawnPlacementsEvent`; spawn eggs via `DeferredSpawnEggItem`.

### M7. World generation
- Ores → configured/placed feature JSON + `neoforge:add_features` biome modifier.
- Fixed structures: script converting the text format to vanilla `.nbt` templates using the M1 flattening table; register as single-piece or jigsaw structures with structure sets.
- Procedural maze dungeons (`DungeonTemplate`, `.ser` files) need a custom `Structure` and pieces. Do last.

### M8. Config and integrations
- `TGConfig` → `ModConfigSpec`; NeoForge's `ConfigurationScreen` provides the in-game screen.
- Keybinds via `RegisterKeyMappingsEvent`.
- Integrations in order: JEI, Patchouli (book moves to `data/`), CraftTweaker, permissions via NeoForge's permission API (replaces FTBLib).

## Effort split
Rendering (M2–M3) ~35–40%, machines and GUIs ~20%, world generation ~15%, the rest mostly mechanical.
