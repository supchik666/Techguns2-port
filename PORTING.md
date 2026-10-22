# Porting Techguns2: 1.12.2 Forge → 1.21.1 NeoForge

Branch: `port/1.21.1`. The 1.12.2 code lives in `legacy/` (not compiled) as a reference; port one package at a time into `src/`.

**License:** `LICENSE.txt` forbids releasing modified versions without the authors' permission. A private port or an upstream PR is fine; publishing needs pWn3d1337's approval.

**Out of scope:** migrating 1.12 worlds, Albedo lighting, and the OpenComputers, Thermal and Chisel integrations (none have 1.21.1 versions). Lost Cities was a dependency but is unused in code, so it's dropped.

## Status

- [x] M0. Project setup
- [x] M1. Registration and data
- [x] M2. One gun working end to end
- [ ] M3. Remaining guns and their models
  - [x] M3.1 Bullet guns (20), ammo variants, zoom and scopes, crosshairs, magazine/rocket renderers
  - [x] M3.2 Energy guns and TGFX particles
  - [ ] M3.3 Explosives, charge guns, grenades
  - [ ] M3.4 Melee and mining tools
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

### M1. Registration and data ✅
Done:
- [x] `DeferredRegister` for items and blocks, creative tab (`Techguns`, `TGItems`, `TGBlocks`).
- [x] All 155 shared-item variants as plain items (legacy stack sizes kept).
- [x] 43 simple blocks: ores, metal panels, nether metal, concrete, neon lights, bug nest blocks, ladders, stairs.
- [x] 12 shaped blocks in `techguns.blocks`: lamps, wall lanterns, camo nets and camo net tops (3 camos each), sandbags, slimy ladder. Forge v1 blockstates are converted to vanilla multipart, with one child model per camo.
- [x] `tools/port/flattening.csv`: `(legacy id, meta) → new id`, verified against the 109 recipe files that embed metadata in their names.
- [x] `tools/port/port_assets.py` regenerates `src/generated/resources/`: textures (folders renamed to `item/`, `block/`), models (paths rewritten), blockstates, lang JSON, tags, block loot tables, recipes.
- [x] 321 of 372 recipes converted. Each is gated by `neoforge:item_exists` conditions, so it switches on once its items are registered (151 active now).

Left for later milestones:
- The other meta blocks (machines, doors, spawner, crate, explosive charges, bioblob, ore clusters, ore drill). Add their rows to `flattening.csv` when they're ported. Door models use 1.12 vanilla parents (`block/door_bottom` etc.), which were renamed in 1.21.

Decisions:
- **Ids keep their 1.12 names** (`plateiron`, `ingotsteel`), so textures, models and lang keys line up without a rename table. Flattened blocks are named after their variant (`container_red`, `nethermetal_grate1`).
- **Copper uses vanilla** (`minecraft:copper_ingot`, `minecraft:copper_ore`). The 1.12 config toggles for optional ingots are gone; recipes use `c:` tags so other mods' metals still work.
- **Deferred to M2/M3:**
  - 13 magazines and rockets have no flat texture (they were 3D-rendered in 1.12), so they show as missing textures until their renderers exist.
  - 51 recipes use custom types (`ammo_change_crafting`, `miningtool_upgrade`, `copy_nbt`, `ammo_sum_recipe`) or NBT results, and need `RecipeSerializer`s.
- Tag names for things without a NeoForge convention are guesses: `c:plates/*`, `c:wires/*`, `c:circuits/*`, `c:rubbers`, `c:fibers/carbon`, `c:ingots/obsidian_steel`.

### M2. One gun end to end (pistol) ✅
Validates the architecture; everything after is repetition.
- Gun and magazine NBT (ammo count, ammo variant, camo, upgrades) → Data Components with codecs.
- `GenericGun`, `ProjectileSelector`, `AmmoTypes` and one projectile entity with its renderer.
- All 26 packets → `CustomPacketPayload` + `StreamCodec`.
- Drop the ASM coremod. Render guns through `IClientItemExtensions` with a `BlockEntityWithoutLevelRenderer`; `ItemDisplayContext` replaces `TransformType`.

If this can't reproduce the gun animations, find out here, not after 35 guns.

**Done:**
- Ammo count is the `techguns:ammo` component (absent = full clip); fire cooldown per hand is an unsaved attachment.
- Firing runs on both sides like 1.12: the client predicts ammo, sound and recoil; the server spawns the bullet and sends the animation to players tracking the shooter. Two payloads for now (`gun_action`, `gun_animation`); the other 1.12 packets come with their features.
- `GunRenderer` (a BEWLR) ports RenderItemBase/RenderGunBase: per-context transforms, recoil and reload animations, additive muzzle flash in first and third person. Models extend `ModelMultipart`, a thin port of the Techne box format.
- Gun crosshair, ammo HUD, force-reload key (R), bullet tracer renderer, block impact sounds and particles.
- `techguns:bullet` damage type, tagged to skip the hurt cooldown and knockback, as 1.12 did.
- `./gradlew runGameTestServer` runs `GunTests` (shoot, hit, reload).
- **Deferred:**
  - M3: other guns, ammo variants (incendiary/explosive) and `ProjectileSelector`, TGFX impact particles, zoom and scopes, the magazine/rocket renderers (the 13 missing textures from M1).
  - M4: armor penetration and the TG damage system.
  - M6: NPCs using guns. Later: off-hand firing, gun item particles.

### M3. Remaining guns and models
- Gun definitions in `TGuns` are mostly data; cheap once M2 works.
- Write a script converting the Techne-style `ModelRenderer` classes to `ModelPart`/`LayerDefinition` instead of porting 89 models by hand.
- OBJ models via the `neoforge:obj` loader.
- OpenGL calls → `PoseStack`, `VertexConsumer`, `RenderType`.
- Custom particles → `ParticleRenderType`.

**M3.1 done:**
- `tools/port/port_models.py` copies 1.12 model classes almost verbatim; `ModelRenderer`/`ModelBase`/`LegacyGl` are shims over `ModelPart.Cube` and the current `PoseStack`, so models keep their 1.12 code. This replaced the planned `LayerDefinition` conversion.
- The 20 bullet guns, numbers and renderer settings copied from 1.12 `TGuns`/`ClientProxy`. `ItemModelRenderer` (1.12 RenderItemBase) also draws the AR/LMG/AS50 magazines and rockets.
- Ammo variants (`techguns:ammo_variant` component) with incendiary and explosive bullets; `techguns:ammo_change` recipe (gun + other ammo). Shotgun pellets and bursts, loose-round reloads, rechamber sounds, gravity, camo textures (`techguns:camo`, no way to change it yet).
- Right-click zoom with FOV, sniper/tech scope overlays, the 1.12 crosshair styles.
- GameTests for the shotgun and the ammo change recipe.
- **Deferred:** AI stats and turret offsets (M6), muzzle lights, the M4 Infiltrator silencer, penetration (stored, used in M4), minigun spin-up, camo change recipe, `miningtool_upgrade`/`copy_nbt`/`ammo_sum_recipe` recipes.

**M3.2 done:**
- TGFX (`client/particle/`) reads `assets/techguns/particles/*.txt` on resource reload and draws after vanilla particles. `TGEffects` is the common entry point: `createFX`/`createFXOnEntity` on the client, `sendFX` from the server (`techguns:spawn_fx`). Bullet impacts use the 1.12 per-material impact effects.
- 12 energy guns (laser gun and pistol, tesla, alien blaster, netherblaster, blaster rifle, scatterbeam, sonic shotgun, NDR, deatomizer, gauss rifle, flamethrower), picked per gun with `GenericGun.setProjectile`. Beams (laser, tesla, NDR) are hitscan `AbstractBeamProjectile`s; tesla chains. Gauss rifle reload takes a slug plus an energy cell (`AmmoType.extra`). The gauss rifle is an OBJ model drawn by `ModelObj`.
- Looping fire sounds (flamethrower, NDR), the flamethrower pilot flame.
- **Deferred:** light pulses (no dynamic lights), TGFX death effects (M6), radiation from the NDR (M4), fire projectiles igniting blocks (M4 block damage config), armor penetration (M4). The gauss rifle HUD shows only slugs.

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
