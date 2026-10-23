# Porting Techguns2: 1.12.2 Forge → 1.21.1 NeoForge

Branch: `port/1.21.1`. The 1.12.2 code lives in `legacy/` (not compiled) as a reference; port one package at a time into `src/`.

**License:** `LICENSE.txt` forbids releasing modified versions without the authors' permission. A private port or an upstream PR is fine; publishing needs pWn3d1337's approval.

**Out of scope:** migrating 1.12 worlds, Albedo lighting, and the OpenComputers, Thermal and Chisel integrations (none have 1.21.1 versions). Lost Cities was a dependency but is unused in code, so it's dropped.

## Status

- [x] M0. Project setup
- [x] M1. Registration and data
- [x] M2. One gun working end to end
- [x] M3. Remaining guns and their models
  - [x] M3.1 Bullet guns (20), ammo variants, zoom and scopes, crosshairs, magazine/rocket renderers
  - [x] M3.2 Energy guns and TGFX particles
  - [x] M3.3 Explosives, charge guns, grenades
  - [x] M3.4 Melee and mining tools
- [x] M4. Player data, armor and damage
- [x] M5. Machines and GUIs
  - [x] M5.1 Machine framework and the six single-block machines
  - [x] M5.2 Multiblocks: fabricator, reaction chamber (also a radiation source, M4), ore drill; keep the 1.12 master/slave design
  - [x] M5.3 Benches and the rest: repair bench (repairs broken armor, M4), camo bench (shield and gun camos), upgrade bench, explosive charges, 3x3 door. Brings back the turret/camo bench/repair bench/armor bench crafting recipes once their blocks have CSV rows. The turret waits for M6 (NPC AI).
- [x] M6. NPCs
  - [x] M6.1 NPC framework and the 17 GenericNPC mobs (soldiers, zombies, bandits, cyberdemon, super mutants), natural spawning
  - [x] M6.2 Special mobs: alien bug, ghastling, attack helicopter (eggs only; their 1.12 spawners come with M7)
  - [x] M6.3 NPC turret (block, GUI, targeting) on `GenericGun.fireWeaponFromNPC` and `TGNpcFaction.TURRET`
- [ ] M7. World generation
  - [x] M7.1 Ores, the 18 scanned structures (buildings, ore clusters, aircraft carrier, nether ruins), TG spawners, military crates, bunker door, structure loot
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
- **Recipe balance changed from 1.12** (`port_assets.py`): circuit boards craft 2 instead of 4 (`RESULT_COUNT`), and plates no longer smelt back into ingots. Tech ladder: elite circuits and coils are fabricator-only (`REMOVED_RECIPES`, `MACHINE_RECIPES["fabricator"]`), so the fabricator housing and controller take basic circuits, and the housing's cybernetic parts (fabricator-only, which left it uncraftable in 1.12) become obsidian steel mechanical parts (`KEY_SWAP`). The electric engine takes steel plates instead of iron ingots, the nuke warhead elite circuits, and the metal press makes 1 carbon mechanical part instead of 2.
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
- **Deferred:** turret offsets (M6), the M4 Infiltrator silencer, penetration (stored, used in M4), minigun spin-up, camo change recipe, `miningtool_upgrade`/`copy_nbt`/`ammo_sum_recipe` recipes.

**M3.2 done:**
- TGFX (`client/particle/`) reads `assets/techguns/particles/*.txt` on resource reload and draws after vanilla particles. `TGEffects` is the common entry point: `createFX`/`createFXOnEntity` on the client, `sendFX` from the server (`techguns:spawn_fx`). Bullet impacts use the 1.12 per-material impact effects.
- 12 energy guns (laser gun and pistol, tesla, alien blaster, netherblaster, blaster rifle, scatterbeam, sonic shotgun, NDR, deatomizer, gauss rifle, flamethrower), picked per gun with `GenericGun.setProjectile`. Beams (laser, tesla, NDR) are hitscan `AbstractBeamProjectile`s; tesla chains. Gauss rifle reload takes a slug plus an energy cell (`AmmoType.extra`). The gauss rifle is an OBJ model drawn by `ModelObj`.
- Looping fire sounds (flamethrower, NDR), the flamethrower pilot flame.
- **Deferred:** TGFX death effects (M6), radiation from the NDR (M4), fire projectiles igniting blocks (M4 block damage config), armor penetration (M4). The gauss rifle HUD shows only slugs.

**M3.3 done:**
- Rocket launcher (rocket, nuke and high-velocity variants; the variant multipliers are applied in `RocketProjectile.create`), grenade launcher, guided missile launcher and grim reaper (hold right click to lock on, fire with left click), TFG and biogun (`GenericGunCharge`: hold to charge, ammo used by charge), stielgranate and frag grenade (`GenericGrenade`: hold to throw further). Charge guns and grenades fire through `ChargedProjectileFactory`.
- `damagesystem/TGExplosion`: damage falls from full at the inner radius to the minimum at the outer one. 1.12 had this inverted past the inner radius; fixed on purpose.
- Bloom (not in 1.12): `client/render/Bloom` draws glowing render types a second time into a mask, blurs it and adds it on top before the GUI. `ScreenEffect.additive(texture[, bloom])` glows, explosions in TGFX glow the most. Turns itself off with Iris/Oculus or on any GL error.
- **Deferred:** radiation (nuke, biogun; M4), bioblob blocks, explosion, bloom and light config (M8), the layered TFG/biogun muzzle flashes with fades.
- Since then: glowing model parts and scope lenses bloom (`LegacyGl` glowing render types, `GunRenderer.setScopeLenses`). Light pulses: `client/LightPulses` raises block light around muzzle flashes and explosions (mixins into `LevelRenderer.getLightColor` and entity light), and `client/render/LightTint` adds their color as a screen pass over the depth buffer. Only bullet guns and explosions light up; the flamethrower and minigun hold one steady light. Projectiles fly twice as fast with the same range (`GenericProjectile.SPEED_FACTOR`; beams and thrown grenades excluded), and the spawn packet carries the exact velocity so fast shots stay straight. `runClient` also loads JEI and some sound mods (`clientMods` in `build.gradle`).

**M3.4 done:**
- Power hammer, chainsaw, mining drill and shishkebap (`GenericGunMeleeCharge`): melee damage, tool types and dig speed by fitted head, 3x3 drill mining (not while sneaking), ammo used by mining and hits, charged hammer blow (`PowerHammerProjectile`), shishkebap flame. `techguns:miningtool_upgrade` fits obsidian then carbon heads. No muzzle light.
- Combat knife and crowbar on the 1.12 steel tier (`items/tools`); they're in the vanilla `swords`/`pickaxes` tags for enchanting.
- Shots leave from the gun muzzle (`GenericProjectile.aim`): beside, below and in front of the eyes, aimed at the crosshair point; from the eyes when a block is in the way.
- **Deferred:** damage types, gore, penetration (M4), drill sneak silk touch, per-tool-type harvest levels, the shishkebap flame on the held model.
**M4 radiation done:**
- `TGRadiation` (1.12 `TGRadiationSystem`): the player's radiation level is the `techguns:radiation` attachment (saved, kept through death less 200, synced to its own player). Effects `radiation`, `radregeneration`, `radresistance` with the 1.12 icons drawn from `tgplayerinventory.png`; four potions with the 1.12 brewing mixes. Poisoning at 500/750/1000 as in 1.12, lethal damage is `techguns:radiation_poisoning` (bypasses armor); the radiation effect hurts mobs with `techguns:radiation`.
- `TGAttributes.RADIATION_RESISTANCE` (`techguns:radiation_resistance`) on every living entity; the radresistance effect adds to it.
- Radioactive items are the `techguns:radiation` item data map (`data/techguns/data_maps/item/radiation.json`, 1.12 numbers, uranium tags included); carrying one gives the effect every 3 s, the tooltip shows its strength.
- Sources: the nuke leaves a `RadiationEntity` zone for 60 s (server only); the NDR beam gives radiation V for 2 s. The biogun never irradiated in 1.12. The zone falloff runs from full at the inner radius to the outer strength at the edge; 1.12 had it backwards, fixed on purpose.
- RadAway and Anti-RAD Pills (`RadConsumable`); in 1.12 they come from the chem lab, so their recipes come with M5.
- HUD: "N RAD" and "+[n RAD/s]" at the right edge (`client/RadiationHud`).
- **Deferred:** the 1.12 config switch (off by default there, WIP), mutants' radiation immunity (M6).

### M4. Player data, armor and damage
- Player capabilities (extra slots, shooter values, death type) → Data Attachments, synced with payloads.
- Keep `TGDamageSource` as a `DamageSource` subclass, add data-driven `DamageType`s; armor penetration in `LivingIncomingDamageEvent`.
- Armor materials are a registry; stats via the attribute-modifier component; custom armor models via `IClientItemExtensions`.
- Radiation → `MobEffect` + `Potion`.

**M4a done (damage system and armor):**
- `damagesystem/`: `TGDamageSource` (type, death type, penetration) over the `data/techguns/damage_type` JSON and tags; `DamageSystem` replaces the vanilla armor reduction in `LivingIncomingDamageEvent` with the 1.12 formula (4% a point, cap 24, penetration ×4 minus toughness removes points) for TG damage and for anyone in TG armor. `ShieldStats` lets TG damage through shields by type. Guns pass their penetration to projectiles and explosions; each projectile sets its 1.12 damage type and knockback.
- `TGArmors`: all 1.12 armor sets with their materials (`TGArmorMaterial`, registered `ArmorMaterial`s plus per-type values), bonuses (`TGArmorBonus`, applied in `ArmorBonuses`), steam/RF powered armor (`PoweredArmor`, charged from batteries in the inventory), camo cycling on sneak-use, the beret, and riot, ballistic and advanced shields.
- 3D armor models and shields are ported with `port_models.py` over a `ModelBiped` shim, drawn by `client/ArmorRenderers` (worn model, render-hack items, shield BEWLR with the blocking model).
- **Deferred:** PvP/NPC damage factors (M8); NPC penetration resistance only on the helicopter so far; gore (M6); `ModelHeavyShield` has no item; no baby scaling on armor models.
**M4 player data and extra slots done:**
- `capabilities/TGExtendedPlayer` is the `techguns:extended_player` attachment (15 slots, toggles, autofeed leftovers), copied on death and synced to everyone by `TGExtendedPlayer.SYNC` (item or toggle changes at once, component-only changes once a second). Transient state (jump key, gliding) syncs to other players only.
- Techguns inventory tab (`gui/TGPlayerInventoryMenu`, `client/TGPlayerInventoryScreen`): face/back/hand, 3 food, heal and 8 ammo slots, the five toggle buttons. Tabs above the vanilla and TG inventory screens. Keys: I (open), N, B, V, J (night vision, safe mode, step assist, jetpack).
- Slot items (`items/additionalslots/`): gas mask, oxygen mask, tactical mask, night vision goggles, glider, jetpack, jump pack, scuba tanks, anti grav pack, working gloves. Effects live in `events/TGPlayerEvents` (night vision, creative flight, jump boost, fall damage, break speed, autofeed, drops on death); the client part (keys, HUD icons, worn models with glowing parts, jetpack flames, loop sounds, fly speed) is in `client/TGPlayerClient`.
- Guns reload from the ammo slots first and empties go back there (`TGExtendedPlayer.consumeAmmo/giveAmmo/countAmmo`); `techguns:ammo_slot` item tag says what fits. Safe mode stops TG explosions breaking blocks.
- GameTests in `PlayerSlotTests` (save/load, gas mask, autofeed, reload from ammo slots).
- **Deferred:** heal slot items (none accept anything yet), the safe mode permission, hover mode, sending food/ammo slots only to their owner.
**M4 wiring done:** armor and gas mask radiation resistance (`ArmorBonuses`, an attribute modifier), armor night vision and step assist follow the toggles, armor oxygen gear counts for the scuba tanks, helmet/chest hide flags hide the face/back/hand slot models, HUD lines for power armor, step assist and radiation, and the TG inventory's armor values and radiation bar tooltips. GameTest `hazmatAndGasMaskResistRadiation`.

**Gun feel (not in 1.12):** camera shake per unscoped shot by gun class (`client/render/ShotShake.SHAKE`: 0.75° pistols/SMGs/energy guns, 1.5° ARs/LMG/hand cannon, 1.75° minigun, 2.5° shotguns/snipers/gauss, 4.5° launchers/TFG). Spread doubles unless sneaking, aiming while sneaking cuts it to a tenth (`GenericGun.stanceSpread`; the crosshair follows). Every gun aims like the LMG by default; `setNoZoom()` on the rocket launcher, grenade launcher, minigun and all charge guns.
**Dev client:** `runClient` also loads Sodium (synced into `run/mods`, since its wrapper jar only loads from a mods folder), Iris, OpenComputers: Rebooted, Markdown Manual. `runServer`/`runGameTestServer` empty `run/mods` first. Bloom, light tint and scope lens step aside only while an Iris shader pack is on. 3D armor (steam, T3/T4 power) shows icons baked from the 3D models in the GUI (`ICON_ARMOR` in `port_assets.py`).

### M5. Machines and GUIs
- `TileEntity` → `BlockEntity`, `ITickable` → `BlockEntityTicker`.
- Item/energy/fluid handlers via `RegisterCapabilitiesEvent`.
- `Container` → `AbstractContainerMenu` + `MenuType`; `GuiContainer` → `AbstractContainerScreen` with `GuiGraphics` (72 GUI files).
- Keep the master/slave multiblock design.

**M5.1 done (framework, simple machines):**
- `machines/`: `TGBlockEntity` (inventory laid out by `slots`, automation handler that only extracts outputs, owner + security, redstone mode, `buttonClicked`, `menuData`, whole-BE sync via `needUpdate`), `MachineBlockEntity` (energy, an `Operation` with progress/totaltime, upgrade-slot stack multiplier, recipe matching; hooks `recipes`, `recipeInput`, `inputFluid`/`drainInput`/`fillOutput`, `operationPower`, `start`, `finish`, `ambient`), `MachineBlock` (facing, ticker, owner on place, menu, buckets into fluid handlers, drops), `MachineMenu` (one `MenuType` for every TG block entity; buttons go through `clickMenuButton`). `client/machines/MachineScreen` draws the 1.12 GUI textures: redstone/security buttons, power bar, upgrade slot, per-machine progress; `MachineRenderers` has the BERs and item renderers. Capabilities (items, energy, chem lab fluids, RF powered armor energy) in `TGBlockEntities.registerCapabilities`.
- Machines: ammo press, metal press, grinder, blast furnace, charging station (also charges anything with an energy capability, e.g. RF power armor), chem lab. Creeper acid fluid (`TGFluids`), milk via NeoForge.
- For the bench/multiblock steps: a new GUI block entity extends `TGBlockEntity` (or `MachineBlockEntity` for recipe machines), passes its slot positions, adds button ids from 3 up and a `MachineScreen` subclass in `MachineScreen.create`.
- Recipes are data: types `techguns:ammo_press`, `metal_press`, `grinder`, `blast_furnace`, `charging_station`, `chem_lab`, all one format (`recipes/MachineRecipe`), generated into `data/techguns/recipe/<machine>/` by `port_assets.py` from `MACHINE_RECIPES`:
  ```json
  {"type": "techguns:chem_lab",
   "inputs": [{"item": "minecraft:nether_wart"}, {"tag": "c:gems/diamond", "count": 2}],
   "bottle": {"item": "techguns:infusionbag"},
   "fluid": {"fluid": "techguns:creeper_acid", "amount": 250},
   "results": [{"id": "techguns:radaway"}],
   "chances": [1.0],
   "fluid_result": {"id": "techguns:creeper_acid", "amount": 1000},
   "power": 25, "time": 100, "swap": true}
  ```
  `inputs` fill the input slots in order (sized ingredients; `neoforge:compound` for either-of; empty slots past them must stay empty); `swap` also accepts the first two swapped. `bottle` (chem lab container slot), `fluid` (sized fluid ingredient, `{"tag": "c:milk", ...}` works) and `fluid_result` are optional. `chances` (grinder) multiply each result's count, a fraction is a random extra item. `power` is FE/t, 0 or missing means the machine's default (ammo press and grinder 5, metal press 20, blast furnace 10, charging station 800, chem lab 1), times the upgrade stack multiplier (squared for metal press, blast furnace and chem lab, as in 1.12); `time` defaults to 100 ticks. The ammo press's plan buttons cycle through its recipes sorted by id.
- KubeJS + Rhino are `localRuntime` dev mods. Without a KubeJS plugin, scripts use `event.custom({type: 'techguns:metal_press', inputs: [...], results: [...]})` and `event.remove({id: 'techguns:metal_press/plateiron'})`; both checked by hand on the GameTest server.
- GameTests in `MachineTests` (metal press, chem lab RadAway and fluid check, charging station, no power, recipes load).
- **Deferred:** grinder recipes for armor (1.12 gave back their crafting ingredients); slot background icons; acid hurting what's in it; per-side slot config; the `machinesNeedNoPower` config (M8); team security levels 1/2; the KubeJS recipe schema plugin; a translucent render layer for creeper acid.

**JEI (`plugins/jei/`):**
- A category per machine type: the 1.12 GUI texture cut at (7, 15, 162, 63) as in 1.12, the progress drawn as `MachineScreen` draws it and looping over the recipe's time, the power bar (tooltip: FE/t and total FE) and a "FE/t ... Duration" line under it. Slots sit where the machine's block entity has them; grinder outputs show their chance, the chem lab shows its bottle slot and both tanks. The machine block is the catalyst; clicking the 1.12 click areas on a `MachineScreen` opens its category.
- The fabricator is a `MachineCategory`; the reaction chamber has `ReactionChamberCategory` (input, focus, fluid at the recipe's level with its consumption, results, wanted intensity, completion/progress bars, risk icon, 1.12's tooltips; FE/t is the per-reaction-tick power / 60). Category textures drop `_controller` from the block id.
- `techguns:ammo_change` and `techguns:miningtool_upgrade` show in the crafting category with their real result (variant and ammo set, head fitted); the upgrade's input tool has the previous head.
- Done since M5.1 (its "JEI categories" deferral). 1.12's plugin had no info pages, so there are none. JEI is `compileOnly`; only JEI loads `TGJeiPlugin`, so the mod runs without it.
- **Adding a category:** a machine running `MachineRecipe`s (a new `Kind`) is one line in `TGJeiPlugin.categories`: `new MachineCategory(gui, TGBlocks.X.get(), new Rect2i(clickX, clickY, w, h), (g, texture, p) -> g.blit(...))`, the lambda drawing its progress at `p` in GUI coordinates; subclass `MachineCategory` for extra slots (see `MachineCategory.ChemLab`). Another recipe class gets a small `TGCategory<T>` subclass (`recipes`, `power`, `time`, `setRecipe`, `drawGui`) and the same one line. The GUI texture must be `textures/gui/<block id>_gui.png`; the block's screen must be a `MachineScreen` for the click area.
- **Deferred:** the ammo press's "Building: X" plan text, the metal press autosplit hint, recipe transfer (+ button).

**M5.3 done (benches, charges, 3x3 door):**
- Framework hooks (existing behavior unchanged): `TGBlockEntity.playerInventory()` (charges and doors show none) and `addMenuSlots` (worn armor, offhand, TG face/back/hand slots, the upgrade bench's result). Their block entity types are in `TGBlockEntities.BENCHES`, without item/energy capabilities. Screens in `client/machines/BenchScreens` (`MachineScreen.create` asks it first).
- `RepairBenchBlockEntity`: materials in the 9 slots fully repair the worn armor, offhand item or the TG armor/shield in its slot, one button each; amounts from 1.12 `getRepairMats` (per-material metal/cloth table, scaled by damage; shields 3x their repair item). Guns have no durability, so there's nothing to repair on them. Broken TG armor (`GenericArmor.isBroken`) now drops its armor attribute too, so the armor bar no longer counts it.
- `CamoBenchBlockEntity`: arrows cycle the camo of the gun/armor/shield/face-back-hand item in the slot, or turn a block into the next of its group (vanilla colored blocks, TG panels, concrete, neon, camo nets, ladders, lamps); +/- cycle the worn armor. Shields have camos 4/4/3 as in 1.12.
- `UpgradeBenchBlockEntity` + `recipes/UpgradeBenchRecipe`: TG armor + an upgrade item → the armor with that enchantment level for 20 XP per level gained. Recipes are data in `data/techguns/recipe/upgrade_bench/`: `{"type": "techguns:upgrade_bench", "upgrade": <ingredient>, "enchantment": "minecraft:protection", "level": 1}`.
- `ExplosiveChargeBlock`/`ExplosiveChargeBlockEntity` (`tnt_charge`, `c4_charge`): stuck to a block facing into it; the GUI sets radius/length/delay and arms it; it then breaks a (radius/2) square of lines `length` deep, stopping each line at a block too hard (30, C4 100) or protected (`BlockEvent.BreakEvent`, as the owner or a fake player). 1.12 used no explosion here, so there's no TGExplosion and no safe mode to respect.
- `Door3x3Block`/`Door3x3BlockEntity`: placed by its item on top of a block, across the view; modes right click (with auto close after 5 s), redstone (signal at any outer block, per the redstone setting), player sensor; owner security; 1 s slide animation by `client/machines/DoorRenderer`. Sneak-use with an empty hand opens the GUI (1.12 wanted a wrench).
- CSV rows for the benches, charges, door and turret (item row only, so its recipe stays gated until M6).
- GameTests in `BenchTests` (repair fixes broken armor, camo bench cycles an M4, upgrade bench Protection II, charge breaks a cube, door opens on redstone).
- **Deferred:** hangar and nether 3x3 doors; upgrade bench inputs stay in the block instead of dropping back on close; upgrades check only for TG armor, not the enchantment's item tags (TG armor isn't in the vanilla enchantable tags, so it can't be enchanted at a table either); charge hardness limits aren't configurable (M8); JEI for the benches.

**M5.2 done (multiblocks):**
- `machines/multiblock/`: `MultiBlockMachine` (the 1.12 master: `check` finds the parts for a click on a face and returns them with their type, `tryForm` links them, `unform` unlinks exactly those; runs only while formed), `MultiBlockController` (click unformed to form, formed or sneaking opens the GUI), `MultiBlockPart` + `MultiBlockSlave` (the 1.12 slave: `formed`/`facing` state, a block entity holding the controller's position and its part type; opens the controller's GUI, buckets into its tank, unforms it when broken), `MultiBlocks` (block entity types, the RC recipe type, capabilities: parts forward to `MultiBlockMachine.partCapability(cap, type)`). Framework hooks: `MachineBlockEntity` constructor flag for machines without an upgrade slot, `consumePower`, `TGBlockEntity.menuRange`.
- **Fabricator** (`FabricatorBlockEntity`): click the controller's side. Controller and three housings at the bottom (behind the controller, to its left as the player sees it, and behind that), four fabricator glass on top. Housings take items and power. Recipe type `techguns:fabricator`, the M5.1 machine format (four inputs: item, wire, powder, plate; 80 FE/t default, upgrade slot). `MultiBlockRenderers` draws the 1.12 model on the controller.
- **Reaction chamber** (`ReactionChamberBlockEntity`): click a side of the controller. Behind it a 3x3 floor of housings (the controller is the middle of the front row), two layers of 3x3 glass, a 3x3 ceiling of housings. The bottom center of the back and the sides take items and fluid, the ceiling center takes power. The GUI sets the liquid level (buckets the tank fills to; it drains only what's above it) and the beam intensity; a reaction starts when input, focus, fluid, level, exact amount and intensity match a recipe. Every 60 ticks it takes `power` FE and counts a completion if the focus, intensity and level still match; the wanted intensity drifts (the marker beside the intensity bar). A missed reaction tick irradiates entities within 6 blocks (`TGRadiation.applyRadToEntities`, strength ceil(intensity / 2) - 1). Out of time: the input is lost, `explosion_low` blows out the middle of the chamber, `explosion_medium` the whole glass box, `break_item` stops it. Recipes `data/techguns/recipe/reaction_chamber/` from `RC_RECIPES`:
  ```json
  {"type": "techguns:reaction_chamber",
   "input": {"item": "minecraft:wheat_seeds"}, "focus": "techguns:rcuvemitter",
   "fluid": {"fluid": "minecraft:water"},
   "results": [{"id": "minecraft:wheat"}, {"id": "minecraft:wheat_seeds", "count": 2}],
   "ticks": 10, "required_completion": 5, "intensity": 3, "margin": 1,
   "liquid_level": 1, "liquid_consumption": 1000, "instability": 0.5,
   "risk": "explosion_low", "power": 50000}
  ```
  `ticks` is in reaction ticks (60 game ticks); `power` is FE per reaction tick; `margin` (default 0), `liquid_consumption` (0), `instability` (0, chance per reaction tick) and `risk` (`none`, `break_item`, `explosion_low`, `explosion_medium`) are optional. KubeJS: `event.custom({type: 'techguns:reaction_chamber', ...})`, `event.remove({type: 'techguns:reaction_chamber'})`; the same for `techguns:fabricator`.
- **Ore drill** (`OreDrillBlockEntity`): click the controller anywhere. Smallest: controller, one drill rod, an ore cluster behind it. Bigger: from the controller a line of engines then rods (at most 16) ending at a cluster, a square of engines `radius` (0-3) wide around the engine line, then a frame around everything: frame blocks on the edges and the first and last ring, scaffolds on the faces, air inside the rod part. Rods at least 2r+1 and at least the engines, more engines than r. Every part but the rods takes items, power and lava. The head must fit the size (small for the mini drill, medium for engine radius 0-1, large for 2-3); speed and power follow 1.12 (cluster size up to the drill length, head level + drill radius - cluster level). Fuel slot burns furnace fuel, the tank burns lava (20 per mB), at 1000 FE per fuel point, then FE. Clusters (`ore_cluster_*`, unbreakable, creative only for now) and their ores are the 1.12 defaults in `OreDrillBlockEntity.clusters()`.
- GameTests in `MultiBlockTests` (fabricator forms through and crafts through a housing, breaking a part unforms, an RC reaction run while following the intensity, the mini drill mines on coal, a drill with frame and scaffolds forms); template `empty8` (8x8x8 air).
- **Deferred:** RC beam/item renderer and the ore drill head renderer (1.12 hid the last scaffold row behind it), connected textures; the fabricator's item on the plate; ore cluster worldgen (M7); the oil cluster (1.12 pumped oil into an output tank; now it mines cobblestone) and fuel fluids for the drill; liquid redstone and liquid ender (RC recipes use lava instead); the ore drill GUI's fuel bar only syncs when a run starts or ends; a JEI category for the ore drill; `oreDrillMultiplier*` config (M8).

### M6. NPCs
- 27 mob types. `EntityAIBase` → `Goal`.
- Attributes via `EntityAttributeCreationEvent`; spawning via biome modifiers + `RegisterSpawnPlacementsEvent`; spawn eggs via `DeferredSpawnEggItem`.
- **M6.1 done** (`entities/npcs/`): `GenericNPC` (the 1.12 class: flat TG armor per damage type through `INpcTGDamageSystem`, gear picked in `addRandomArmor(difficulty)`, gun or melee attack goal chosen from the main hand, difficulty scaling of NPC damage and spread, burning in daylight), `RangedGunAttackGoal` (1.12 `EntityAIRangedAttack`: range, attack time and bursts from the gun's `setAIStats` in `TGuns`), `TGNpcFaction` + `FactionHurtByTargetGoal` (NPCs don't retaliate on their own faction), the mobs (ids are lowercased 1.12 names; `SuperMutant` covers basic/heavy/elite by tier), `TGNpcs` (types, attributes, eggs, spawn placements), `TGSpawnManager`. Guns fire for NPCs through `GenericGun.fireWeaponFromNPC(shooter, stack, damageScale, accuracyScale[, side])`: no ammo, sound on the hostile channel, third-person recoil sent to trackers. Renderer `client/render/NpcRenderer` (vanilla humanoid/skeleton/zombified piglin models, gun held like a bow, vanilla armor layer; the cyberdemon and super mutant models are ported 1.12 classes drawn in the vanilla pose). Loot tables, egg names/models and lang renames come from `NPCS` in `port_assets.py`. GameTests in `NpcTests`.
- Natural spawns: the biome modifiers `data/techguns/neoforge/biome_modifier/npcs_*.json` add a placeholder (`techguns:tgdummyspawn`) to the overworld (weight 600) and nether (300); on joining the world it becomes a mob picked by weight from `TGSpawnManager.OVERWORLD`/`NETHER` among the entries whose danger is at most the spot's (distance from world spawn, harsh biomes). Overworld: zombie soldier, farmer, miner, skeleton soldier, psycho steve (rare), bandits (danger 2); nether: zombie pigman soldier, cyberdemon. The others (army soldier, commando, stormtrooper, outcast, dictator dave, super mutants) come only from eggs until structures spawn them (M7).
- Adding a mob: a `GenericNPC` subclass (stats in its `TGNpcs` line, gear in `addRandomArmor`), its line in `TGNpcs`, its name in `NPCS` in `port_assets.py` (loot, egg, lang), a line in `NpcRenderer.register`. Adding a natural spawn: an entry in the `TGSpawnManager` tables, or another biome modifier JSON.
- **Deferred:** 1.12 spawner capability for structure spawners (M7); config for spawn weights, distances, the biome/dimension blacklist and NPC damage factor (M8); the NPC turret (`fireWeaponFromNPC` with side `CENTER`, `getAIAttack`, `TGNpcFaction.TURRET`); per-mob weapon offsets and the cyberdemon's gun scale; NPC gore and death effects; mutants' radiation immunity.

**M6.2 done (special mobs):**
- `entities/npcs/`: `AlienBug` (a `Spider`: 20 HP, 4 damage, speed 1.0, hunts players and iron golems day and night, screeches on aggro, jaw animation on a bite; arthropod via `data/minecraft/tags/entity_type/arthropod.json`; no loot, as in 1.12), `Ghastling` (a walking `Monster`, 20 HP, armor 10, fire immune; melee within 2 blocks, else a 1.5 s wind-up and three alien blaster bolts, 6 damage, then 2.5 s pause; ghast sounds and textures), `GenericFlyingMob` (hostile `FlyingMob`, gone in peaceful; the 1.12 look-around, random-fly at a height over the heightmap and ghast-style move control as `Goal`s/`MoveControl`) and `AttackHelicopter` (100 HP, armor 16, toughness 5; flies 24 blocks up, faces players within 96 and, within 64 and in sight, fires a 5 bullet burst then a rocket from a random pod, 12 damage each; rotor sound; a 5 s spinning crash, then `HelicopterDeathExplosion`).
- Entity ids `alienbug`, `ghastling`, `attackhelicopter`; attributes via `TGEntities.registerAttributes`; spawn eggs with the 1.12 colors; 1.12 names (all languages) and loot (`write_entity_loot` in `port_assets.py`, `ENTITY_LOOT` lists the tables to convert: set_count, set_data, looting_enchant, random_chance_with_looting).
- Renderers in `client/SpecialMobRenderers` (1.12 models through `port_models.py`; the helicopter's three OBJ parts with spinning rotor and pitching gun).
- GameTests in `SpecialMobTests` (stats, eggs and loot, the bug bites, the ghastling and the helicopter shoot a pig).
- **Deferred:** no natural spawns; 1.12 only spawned them from structure spawners (alien bug nest, nether soul platform, aircraft carrier, military camp), which come with M7 and the NPC spawn system. Spawner linking (`ITGSpawnerNPC`, M7) and gore deaths. Since the M6.1 merge the bug is HOSTILE, and the bug and helicopter take damage through `INpcTGDamageSystem` with their 1.12 per-type armor (the helicopter's penetration resistance too, via `getPenetrationResistance`). The helicopter is immune to its own rockets (1.12 let them hurt it) until a friendly-fire config (M8). Death smoke/fire particles (commented out in 1.12 too).

**M6.3 done (NPC turret):**
- Block `techguns:turret` (`machines/TurretBlock`, a `MachineBlock` with `hanging` set when placed against a block's underside; CSV row `basicmachine,3` is now `cube`, so its recipe is active) and `TurretBlockEntity` (1.12 `TurretTileEnt` on `MachineBlockEntity`: 5000 FE, 9 ammo inputs, 9 outputs for empty magazines, gun slot, armor plate slot; 5 FE/t while on, heals 1 HP per 3 s for 500 FE, rebuilds a dead turret in 10 s at 50 FE/t back at 10 HP; redstone off or no power turns the turret's AI off). Buttons: target animals (anyone who may use it) and PvP (owner only) cycling no PvP → everyone outside the owner's scoreboard team (1.12 setting 3, vanilla teams instead of FTB) → everyone but the owner.
- Entity `techguns:turret` (`entities/npcs/NPCTurret`, faction `TURRET`): pinned on (or under) its base, gun read from the base (`getItemBySlot`), attack goal from `GenericGun.getAIAttack` with the gun's AI range as follow range, shoots through `fireWeaponFromNPC(..., CENTER)` only after `TurretBlockEntity.consumeAmmo` takes a round (reloading from the inputs with the new `GenericGun.load(stack, AmmoSource, oldAmmo)`, which the player reload uses too). Targets `Enemy` mobs within 20 blocks up/down, animals when told (not tamed pets/horses), players and other turrets per PvP; retaliates on whoever hurts it except its owner and their team. Armor from the plate: 5/10/15/20 through `INpcTGDamageSystem`. The base tracks its turret by UUID; a turret whose base doesn't name it goes away, one without a base wastes away, breaking the base removes it and drops gun, ammo and plate.
- Client in `client/machines/TurretClient`: entity renderer (mount yaw, top pitch, gun in third person with the 1.12 per-gun turret offsets, tier texture from the plate), base BER and item renderer, the 1.12 GUI (`turret_base_gui.png`, repair bar and health tooltip). Rendering not checked in game yet.
- GameTests in `TurretTests` (kills a husk with base ammo and hands back the empty magazine; spares owner and tamed pets, targets strangers and animals per the buttons; breaking drops contents and removes the turret).
- Unlike 1.12, the turret has 60 max health (1.12: 30) and each armor plate tier adds 20 (`TurretBlockEntity.HEALTH_PER_TIER`, 60 → 80/100/120/140); the plate slot holds one plate (`TGBlockEntity.slotLimit`); fitting a plate heals by the gain.
- Unlike 1.12, the turret has no watch-closest goal: it only turns to what it targets (and looks around idly), never to a player it isn't shooting.
- On a Sable ship the turret entity is pinned to its base's world position (`SableCompat.toWorld`), and a base that moved (ship assembly/disassembly) relinks its turret; it doesn't tilt with the ship.
- **Deferred:** turret-to-player damage factor (1.12 config 0.5, M8); FTB PvP settings 1 and 2; shift-click into the gun and plate slots (lands in the ammo inputs); slot background icons; a turret lost for good (not just unloaded) counts as killed after 5 s and is rebuilt; it can drown.

**M7.1 done (ores, scanned structures, spawners):**
- Ores: `data/techguns/worldgen/{configured,placed}_feature/ore_{tin,lead,titanium,uranium}.json` with the 1.12 `OreGenerator` vein sizes, counts and heights (tin 5×10 at y 5-60, lead 5×8 at 5-50, titanium 6×5 at 4-32, uranium 5×4 at 4-24; stone only), added to `#minecraft:is_overworld` by `neoforge/biome_modifier/ores.json`. Copper is vanilla's.
- Structures: `tools/port/port_structures.py` (run by `port_assets.py`) evaluates each structure class's `blockList` (MBlock helpers in Python) and writes `data/techguns/structure/<name>.nbt`, `worldgen/structure/<name>.json` (type `techguns:template`), five structure sets and the biome tags `techguns:has_structure/{overworld,desert,ocean,nether}`. Sets follow 1.12's size classes and weights: `overworld_small` (spacing 16: factory, train station, small mine, gas station), `overworld_medium` (32: police station, survivor hideout, ore spike, meteor base, desert oil cluster in deserts only), `overworld_big` (64: aircraft carrier on deep ocean), `nether_small` (16: small altar, soul platform, loot 01, acid hole, small ore cluster), `nether_medium` (32: medium altar, ghast spawner, ore castle at weight 1000, as in 1.12).
- `world/TemplateStructure` (codec fields `template`, `placement` surface/water/nether, `y_offset`, `height_diff`, `foundation`): random rotation, placed in its chunk's corner; surface = dry land at most 3 blocks uneven with the template floor on the top block (1.12 `cleanUpwards` is baked in as air), water = sea level - 3 over 8+ deep water, nether = a cave floor with 10 air above (y 20-100), the bottom layer extended down by `foundation`. 1.12's random blocks are DATA structure blocks whose metadata is a JSON node (`random` per block, `roll` once per structure for the ore cluster type, `chance`, `biome` for woodland/snow/desert/nether variants), resolved by `TemplateStructure.Piece#resolve`.
- Spawners `techguns:hole` and `techguns:soldier_spawn` (block entity `techguns:tg_spawner`, `world/TGSpawnerBlockEntity`): NBT `mobsLeft`, `maxActive`, `spawnDelay`, `spawnRange`, `delay`, `spawnHeightOffset`, `mobtypes` [{id, weight}], `weapon` (item stack); code API `configure(mobsLeft, maxActive, spawnDelay, spawnRange)` + `addMob(type, weight)`. Spawned mobs get the attachment `TGWorldgen.SPAWNER` (spawner position), a 10 block leash and don't burn in daylight; their deaths count down `mobsLeft` and the block disappears at 0. Unbreakable, no item drops.
- Blocks: `military_crate_{ammo,gun,armor,medical,explosives,generic}` (CSV kind `crate`; break to get their 1.12 loot, `loot_table/blocks/military_crate_*`), `bunkerdoor` (CSV kind `door`, a vanilla `DoorBlock` with block set `techguns:bunker`, hand-openable). Chest loot tables: `techguns:chests/{gasstation,policestation,factory_building,small_trainstation,survivor_hideout,aircraftcarrier,militarybase_barracks,militarybase_bunker,castle}` (converted by `write_chest_loot`).
- GameTests in `WorldgenTests`: spawner spawn/link/countdown/removal, every template's palette and markers parse, the gas station placed as a piece (markers gone, spawner and chest data, whole doors), a structure start on the flat test world, chest and crate loot rolls, registries and the ores in plains.
- Not checked in game: none of it was generated in a real world yet (placement heights, frequency, how the buildings look).
- **Deferred:** the 1.12 oil fluid (the desert oil cluster uses 1.12's no-oil fallback: lava, magma, sandstone); tiered chest loot (`EnumLootType`, unused in 1.12 scanned structures); config for structure spacing and ore generation (M8); no exclusion between structure sets or vanilla structures; double bunker doors open independently; silk-touching a crate also drops its contents; the spawner leash isn't saved. Procedural structures (military camp/base, alien bug nest, nether dungeon, city) are M7.2.

### M7. World generation
- Ores → configured/placed feature JSON + `neoforge:add_features` biome modifier.
- Fixed structures: script converting the text format to vanilla `.nbt` templates using the M1 flattening table; register as single-piece or jigsaw structures with structure sets.
- Procedural maze dungeons (`DungeonTemplate`, `.ser` files) need a custom `Structure` and pieces. Do last.

### M8. Config and integrations
- `TGConfig` → `ModConfigSpec`; NeoForge's `ConfigurationScreen` provides the in-game screen.
- Keybinds via `RegisterKeyMappingsEvent`.
- Integrations in order: JEI, Patchouli (book moves to `data/`), CraftTweaker, permissions via NeoForge's permission API (replaces FTBLib).
- Done early: Sable (Create Aeronautics ships), `plugins/sable/SableCompat`. Sable's patched `Level.clip` returns ship hits in plot coordinates; projectile hits, gun aim and missile lock map them back to the world. `TGExplosion` rays also sample ship blocks, break them and push the ship (impulse 5 per block, as Sable gives vanilla explosions). Sable and its companion API are `compileOnly`; Sable types stay in `SableCompat.Ships`, which only loads with Sable (the verifier would otherwise load them).

## Effort split
Rendering (M2–M3) ~35–40%, machines and GUIs ~20%, world generation ~15%, the rest mostly mechanical.
