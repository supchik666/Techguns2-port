package techguns;

import net.neoforged.neoforge.registries.DeferredItem;
import techguns.entities.projectiles.*;
import techguns.items.guns.GenericGrenade;
import techguns.items.guns.GenericGun;
import techguns.items.guns.GenericGun.Crosshair;
import techguns.items.guns.GenericGun.GunHandType;
import techguns.items.guns.GenericGun.RangeTooltipType;
import techguns.items.guns.GenericGunCharge;
import techguns.items.guns.GenericGunMeleeCharge;
import techguns.items.guns.GenericGunMeleeCharge.RightClick;
import techguns.items.guns.GuidedMissileLauncher;
import techguns.items.guns.SonicShotgun;
import techguns.items.guns.ammo.AmmoTypes;

/**
 * Gun definitions, numbers as in 1.12 TGuns. ponytail: turret offsets, muzzle lights and the M4 Infiltrator
 * silencer are dropped until NPCs, turrets and dynamic lights exist; the minigun fires without its spin-up check.
 */
public class TGuns {
	public static final int MAX_RANGE_PISTOL = 40;
	public static final int MAX_RANGE_RIFLE = 60;
	public static final int MAX_RANGE_RIFLE_LONG = 75;
	public static final int MAX_RANGE_SNIPER = 90;
	public static final float PENETRATION_LOW = 0.5f;
	public static final float PENETRATION_MED = 1.0f;
	public static final float PENETRATION_MED_HIGH = 2.0f;
	/** NPC attack ranges ({@link GenericGun#setAIStats}). */
	public static final float RANGE_MELEE = 3.0f;
	public static final float RANGE_CLOSE = 12.0f;
	public static final float RANGE_SHORT = 18.0f;
	public static final float RANGE_MEDIUM = 24.0f;
	public static final float RANGE_FAR = 36.0f;

	public static final DeferredItem<GenericGun> HANDCANNON = TGItems.ITEMS.register("handcannon",
			() -> new GenericGun(AmmoTypes.STONE_BULLETS, true, 12, 1, 30, 8.0f, TGSounds.HANDGUN_FIRE, TGSounds.HANDGUN_RELOAD, 25, 0.035f)
					.setBulletSpeed(1f)
					.setGravity(0.015d)
					.setDamageDrop(10, 25, 5.0f)
					.setTexture("textures/guns/handgun")
					.setRecoiltime(12)
					.setCrossHair(Crosshair.QUAD_NO_CORNERS)
					.setAIStats(RANGE_CLOSE, 60, 0, 0));
	public static final DeferredItem<GenericGun> SAWEDOFF = TGItems.ITEMS.register("sawedoff",
			() -> new GenericGun(AmmoTypes.SHOTGUN_ROUNDS, true, 4, 2, 28, 4.0f, TGSounds.SAWEDOFF_FIRE, TGSounds.SAWEDOFF_RELOAD, 10, 0.01f)
					.setAmmoCount(2)
					.setShotgunSpread(7, 0.2f, false)
					.setDamageDrop(1, 4, 1.5f)
					.setTexture("textures/guns/sawedoff")
					.setBulletSpeed(1.5f)
					.setCrossHair(Crosshair.FOUR_PARTS)
					.setAIStats(RANGE_CLOSE, 60, 2, 20));
	public static final DeferredItem<GenericGun> REVOLVER = TGItems.ITEMS.register("revolver",
			() -> new GenericGun(AmmoTypes.PISTOL_ROUNDS, true, 6, 6, 45, 8.0f, TGSounds.REVOLVER_FIRE, TGSounds.REVOLVER_RELOAD, MAX_RANGE_PISTOL, 0.025f)
					.setDamageDrop(12, 20, 6.0f)
					.setBulletSpeed(2f)
					.setRecoiltime(6)
					.setTexture("textures/guns/revolver")
					.setHandType(GunHandType.ONE_HANDED)
					.setAIStats(RANGE_SHORT, 90, 6, 20));
	public static final DeferredItem<GenericGun> GOLDENREVOLVER = TGItems.ITEMS.register("goldenrevolver",
			() -> new GenericGun(AmmoTypes.PISTOL_ROUNDS, true, 8, 6, 45, 14.0f, TGSounds.REVOLVER_GOLDEN_FIRE, TGSounds.REVOLVER_RELOAD, MAX_RANGE_PISTOL, 0.015f)
					.setDamageDrop(12, 30, 6.0f)
					.setRecoiltime(8)
					.setBulletSpeed(1.75f)
					.setTexture("textures/guns/goldenrevolver")
					.setHandType(GunHandType.ONE_HANDED)
					.setAIStats(RANGE_MEDIUM, 60, 6, 15));
	public static final DeferredItem<GenericGun> THOMPSON = TGItems.ITEMS.register("thompson",
			() -> new GenericGun(AmmoTypes.SMG_MAGAZINE, false, 3, 20, 40, 5.0f, TGSounds.THOMPSON_FIRE, TGSounds.THOMPSON_RELOAD, MAX_RANGE_PISTOL, 0.05f)
					.setBulletSpeed(1.75f)
					.setDamageDrop(15, 24, 3.0f)
					.setTexture("textures/guns/thompson")
					.setMuzzleFlashTime(4)
					.setAIStats(RANGE_SHORT, 45, 3, 3));
	public static final DeferredItem<GenericGun> AK47 = TGItems.ITEMS.register("ak47",
			() -> new GenericGun(AmmoTypes.ASSAULT_RIFLE_MAGAZINE, false, 3, 30, 45, 9.0f, TGSounds.AK_FIRE, TGSounds.AK_RELOAD, MAX_RANGE_RIFLE, 0.030f)
					.setDamageDrop(20, 30, 5.0f)
					.setPenetration(PENETRATION_LOW)
					.setBulletSpeed(2.5f)
					.setTextures("textures/guns/ak47texture", 2)
					.setMuzzleFlashTime(4)
					.setAIStats(RANGE_MEDIUM, 30, 3, 3));
	public static final DeferredItem<GenericGun> BOLTACTION = TGItems.ITEMS.register("boltaction",
			() -> new GenericGun(AmmoTypes.RIFLE_ROUNDS, true, 25, 6, 50, 16.0f, TGSounds.BOLT_ACTION_FIRE, TGSounds.BOLT_ACTION_RELOAD, MAX_RANGE_SNIPER, 0.05f)
					.setZoom(0.35f, true, 0.125f, true)
					.setBulletSpeed(3.5f)
					.setRecoiltime(15)
					.setRechamberSound(TGSounds.BOLT_ACTION_RECHAMBER)
					.setDamageDrop(40, 60, 10.0f)
					.setPenetration(PENETRATION_LOW)
					.setTextures("textures/guns/boltactionrifle", 3)
					.setAIStats(RANGE_FAR, 60, 0, 0));
	public static final DeferredItem<GenericGun> M4 = TGItems.ITEMS.register("m4",
			() -> new GenericGun(AmmoTypes.ASSAULT_RIFLE_MAGAZINE, false, 3, 30, 45, 8.0f, TGSounds.M4_FIRE, TGSounds.M4_RELOAD, MAX_RANGE_RIFLE, 0.015f)
					.setBulletSpeed(3.0f)
					.setZoom(0.75f, true, 0.75f, false)
					.setDamageDrop(25, 40, 6.0f)
					.setTextures("textures/guns/m4texture", 4)
					.setPenetration(PENETRATION_LOW)
					.setMuzzleFlashTime(4)
					.setAIStats(RANGE_MEDIUM, 30, 3, 3));
	public static final DeferredItem<GenericGun> M4_INFILTRATOR = TGItems.ITEMS.register("m4_infiltrator",
			() -> new GenericGun(AmmoTypes.ASSAULT_RIFLE_MAGAZINE, false, 3, 30, 45, 8.0f, TGSounds.M4_SILENCED_FIRE, TGSounds.M4_RELOAD, MAX_RANGE_RIFLE, 0.010f)
					.setZoom(0.50f, true, 0.5f, true)
					.setDamageDrop(25, 35, 6.0f)
					.setBulletSpeed(2.0f)
					.setTexture("textures/guns/m4_uq_texture")
					.setPenetration(PENETRATION_LOW)
					.setNoMuzzleLight()
					.setAIStats(RANGE_MEDIUM, 30, 3, 3));
	public static final DeferredItem<GenericGun> COMBATSHOTGUN = TGItems.ITEMS.register("combatshotgun",
			() -> new GenericGun(AmmoTypes.SHOTGUN_ROUNDS, true, 14, 8, 50, 4.0f, TGSounds.COMBATSHOTGUN_FIRE, TGSounds.COMBATSHOTGUN_RELOAD, 15, 0.01f)
					.setAmmoCount(8)
					.setShotgunSpread(7, 0.15f, false)
					.setRecoiltime(12)
					.setBulletSpeed(1.5f)
					.setRechamberSound(TGSounds.COMBATSHOTGUN_RECHAMBER)
					.setDamageDrop(2, 5, 1.5f)
					.setPenetration(PENETRATION_LOW)
					.setTexture("textures/guns/combatshotgun")
					.setCrossHair(Crosshair.FOUR_PARTS)
					.setAIStats(RANGE_CLOSE, 30, 0, 0));
	public static final DeferredItem<GenericGun> MAC10 = TGItems.ITEMS.register("mac10",
			() -> new GenericGun(AmmoTypes.SMG_MAGAZINE, false, 2, 32, 40, 5.0f, TGSounds.MAC10_FIRE, TGSounds.M4_RELOAD, MAX_RANGE_PISTOL, 0.05f)
					.setDamageDrop(15, 24, 3.0f)
					.setTexture("textures/guns/mac10texture")
					.setRecoiltime(2)
					.setMuzzleFlashTime(3)
					.setBulletSpeed(1.75f)
					.setHandType(GunHandType.ONE_POINT_FIVE_HANDED)
					.setAIStats(RANGE_SHORT, 35, 3, 2));
	public static final DeferredItem<GenericGun> LMG = TGItems.ITEMS.register("lmg",
			() -> new GenericGun(AmmoTypes.LMG_MAGAZINE, false, 2, 100, 100, 8.0f, TGSounds.LMG_FIRE, TGSounds.LMG_RELOAD, MAX_RANGE_RIFLE_LONG, 0.020f)
					.setZoom(0.75f, true, 0.75f, false)
					.setDamageDrop(40, 60, 6.0f)
					.setPenetration(PENETRATION_LOW)
					.setBulletSpeed(3.0f)
					.setTexture("textures/guns/mg2_texture")
					.setMuzzleFlashTime(2)
					.setRecoiltime(3)
					.setAIStats(RANGE_MEDIUM, 40, 6, 3));
	public static final DeferredItem<GenericGun> MINIGUN = TGItems.ITEMS.register("minigun",
			() -> new GenericGun(AmmoTypes.MINIGUN_AMMO_DRUM, false, 0, 200, 100, 5.0f, TGSounds.MINIGUN_FIRE, TGSounds.MINIGUN_RELOAD, MAX_RANGE_RIFLE_LONG, 0.025f)
					.setNoZoom()
					.setDamageDrop(30, 50, 3.0f)
					.setPenetration(PENETRATION_LOW)
					.setBulletSpeed(3.0f)
					.setTexture("textures/guns/minigun")
					.setCheckRecoil()
					.setMuzzleFlashTime(4)
					.setCheckMuzzleFlash()
					.setAIStats(RANGE_MEDIUM, 40, 10, 1));
	public static final DeferredItem<GenericGun> AS50 = TGItems.ITEMS.register("as50",
			() -> new GenericGun(AmmoTypes.AS50_MAGAZINE, true, 10, 10, 80, 32.0f, TGSounds.AS50_FIRE, TGSounds.AS50_RELOAD, MAX_RANGE_SNIPER, 0.0625f)
					.setDamageDrop(40, 60, 24.0f)
					.setZoom(0.35f, true, 0.125f, true)
					.setBulletSpeed(3.5f)
					.setRecoiltime(10)
					.setPenetration(PENETRATION_MED_HIGH)
					.setTexture("textures/guns/as50texture")
					.setAIStats(RANGE_FAR, 30, 0, 0));
	public static final DeferredItem<GenericGun> VECTOR = TGItems.ITEMS.register("vector",
			() -> new GenericGun(AmmoTypes.SMG_MAGAZINE, false, 2, 25, 40, 6.0f, TGSounds.VECTOR_FIRE, TGSounds.VECTOR_RELOAD, MAX_RANGE_PISTOL, 0.05f)
					.setZoom(0.75f, true, 0.35f, false)
					.setDamageDrop(17, 25, 4.0f)
					.setBulletSpeed(2.0f)
					.setTextures("textures/guns/vector_texture", 2)
					.setRecoiltime(2)
					.setMuzzleFlashTime(3)
					.setPenetration(PENETRATION_LOW)
					.setAIStats(RANGE_SHORT, 35, 3, 2));
	public static final DeferredItem<GenericGun> SCAR = TGItems.ITEMS.register("scar",
			() -> new GenericGun(AmmoTypes.ASSAULT_RIFLE_MAGAZINE, false, 4, 20, 45, 12.0f, TGSounds.SCAR_FIRE, TGSounds.SCAR_RELOAD, MAX_RANGE_RIFLE_LONG, 0.015f)
					.setZoom(0.65f, true, 0.5f, true)
					.setDamageDrop(35, 60, 10.0f)
					.setTextures("textures/guns/scar_texture", 2)
					.setPenetration(PENETRATION_MED)
					.setBulletSpeed(3.0f)
					.setMuzzleFlashTime(5)
					.setAIStats(RANGE_MEDIUM, 30, 5, 2));
	public static final DeferredItem<GenericGun> AUG = TGItems.ITEMS.register("aug",
			() -> new GenericGun(AmmoTypes.ASSAULT_RIFLE_MAGAZINE, false, 3, 30, 45, 8.0f, TGSounds.AUG_FIRE, TGSounds.AUG_RELOAD, MAX_RANGE_RIFLE, 0.010f)
					.setZoom(0.50f, true, 0.5f, true)
					.setDamageDrop(30, 45, 7.0f)
					.setTextures("textures/guns/augtexture", 5)
					.setPenetration(PENETRATION_LOW)
					.setMuzzleFlashTime(4)
					.setAIStats(RANGE_MEDIUM, 30, 3, 3));
	public static final DeferredItem<GenericGun> PDW = TGItems.ITEMS.register("pdw",
			() -> new GenericGun(AmmoTypes.ADVANCED_MAGAZINE, false, 1, 40, 40, 5.0f, TGSounds.PDW_FIRE, TGSounds.PDW_RELOAD, MAX_RANGE_PISTOL, 0.03f)
					.setDamageDrop(18, 25, 3.0f)
					.setPenetration(PENETRATION_MED)
					.setTextures("textures/guns/pdw", 3)
					.setHandType(GunHandType.ONE_POINT_FIVE_HANDED)
					.setMuzzleFlashTime(2)
					.setCrossHair(Crosshair.HORIZONTAL_TWO_PART)
					.setMuzzleLight(0f, 0.8f, 1.0f)
					.setAIStats(RANGE_SHORT, 30, 4, 2));
	public static final DeferredItem<GenericGun> PULSERIFLE = TGItems.ITEMS.register("pulserifle",
			() -> new GenericGun(AmmoTypes.ADVANCED_MAGAZINE, false, 7, 12, 45, 10.0f, TGSounds.PULSE_RIFLE_FIRE, TGSounds.PULSE_RIFLE_RELOAD, MAX_RANGE_RIFLE_LONG, 0.024f)
					.setDamageDrop(30, 45, 8f)
					.setZoom(0.35f, true, 0.5f, true)
					.setBulletSpeed(3.25f)
					.setRecoiltime(8)
					.setPenetration(PENETRATION_MED)
					.setShotgunSpread(2, 0.015f, true)
					.setTextures("textures/guns/pulserifle", 3)
					.setMuzzleFlashTime(4)
					.setCrossHair(Crosshair.HORIZONTAL_TWO_PART)
					.setMuzzleLight(0f, 0.8f, 1.0f)
					.setAIStats(RANGE_MEDIUM, 30, 0, 0));
	public static final DeferredItem<GenericGun> PISTOL = TGItems.ITEMS.register("pistol",
			() -> new GenericGun(AmmoTypes.PISTOL_MAGAZINE, true, 4, 18, 35, 8.0f, TGSounds.PISTOL_FIRE, TGSounds.PISTOL_RELOAD, MAX_RANGE_PISTOL, 0.025f)
					.setBulletSpeed(2.0f)
					.setDamageDrop(15, 22, 6.0f)
					.setTexture("textures/guns/pistol3")
					.setDamageDrop(18, 25, 5.0f)
					.setHandType(GunHandType.ONE_HANDED)
					.setRecoiltime(3)
					.setCrossHair(Crosshair.GUN_DYNAMIC)
					.setAIStats(RANGE_MEDIUM, 30, 3, 10));

	public static final DeferredItem<GenericGun> FLAMETHROWER = TGItems.ITEMS.register("flamethrower",
			() -> new GenericGun(AmmoTypes.FUEL_TANK, false, 2, 100, 45, 5.0f, TGSounds.FLAMETHROWER_FIRE, TGSounds.FLAMETHROWER_RELOAD, 16, 0.05f)
					.setProjectile(FlamethrowerProjectile::new)
					.setBulletSpeed(0.5f)
					.setGravity(0.01d)
					.setFiresoundStart(TGSounds.FLAMETHROWER_START)
					.setMaxLoopDelay(10)
					.setDamageDrop(4, 16, 2.0f)
					.setTexture("textures/guns/flamethrower")
					.setCheckRecoil()
					.setRecoiltime(10)
					.setCheckMuzzleFlash()
					.setMuzzleFlashTime(10)
					.setForwardOffset(0.35f)
					.setCrossHair(Crosshair.QUAD_NO_CORNERS)
					.setAIStats(RANGE_CLOSE, 20, 5, 5));
	public static final DeferredItem<GenericGun> TESLAGUN = TGItems.ITEMS.register("teslagun",
			() -> new GenericGun(AmmoTypes.ENERGY_CELL, false, 8, 25, 45, 12.0f, TGSounds.TESLA_FIRE, TGSounds.TESLA_RELOAD, MAX_RANGE_RIFLE, 0.0f)
					.setProjectile(TeslaProjectile::new)
					.setZoom(0.75f, true, 1.0f, false)
					.setBulletSpeed(80.0f)
					.setMuzzleFlashTime(10)
					.setTexture("textures/guns/teslagun")
					.setCrossHair(Crosshair.HORIZONTAL_TWO_PART_E)
					.setMuzzleLight(0f, 0.8f, 1.0f)
					.setAIStats(RANGE_MEDIUM, 30, 0, 0));
	public static final DeferredItem<GenericGun> LASERGUN = TGItems.ITEMS.register("lasergun",
			() -> new GenericGun(AmmoTypes.ENERGY_CELL, false, 5, 45, 45, 12.0f, TGSounds.LASERGUN_FIRE, TGSounds.LASERGUN_RELOAD, MAX_RANGE_SNIPER, 0.0f)
					.setProjectile(LaserProjectile::new)
					.setZoom(0.75f, true, 0.75f, false)
					.setBulletSpeed(100.0f)
					.setTexture("textures/guns/lasergun")
					.setRangeTooltipType(RangeTooltipType.NO_DROP)
					.setCrossHair(Crosshair.HORIZONTAL_TWO_PART)
					.setMuzzleLight(1.0f, 0f, 0f)
					.setAIStats(RANGE_MEDIUM, 30, 0, 0));
	public static final DeferredItem<GenericGun> ALIENBLASTER = TGItems.ITEMS.register("alienblaster",
			() -> new GenericGun(AmmoTypes.ENERGY_CELL, false, 8, 10, 35, 16.0f, TGSounds.ALIENBLASTER_FIRE, TGSounds.ALIENBLASTER_RELOAD, MAX_RANGE_PISTOL, 0.0f)
					.setProjectile(AlienBlasterProjectile::new)
					.setBulletSpeed(1.0f)
					.setMuzzleFlashTime(10)
					.setPenetration(PENETRATION_MED)
					.setTexture("textures/guns/alien_blaster")
					.setHandType(GunHandType.ONE_HANDED)
					.setCrossHair(Crosshair.FOUR_PARTS_SPIKED)
					.setMuzzleLight(0.925f, 0.415f, 1f)
					.setAIStats(RANGE_MEDIUM, 40, 0, 0));
	public static final DeferredItem<GenericGun> NETHERBLASTER = TGItems.ITEMS.register("netherblaster",
			() -> new GenericGun(AmmoTypes.NETHER_CHARGE, false, 8, 10, 35, 14.0f, TGSounds.NETHERBLASTER_FIRE, TGSounds.NETHERBLASTER_RELOAD, MAX_RANGE_RIFLE, 0.0f)
					.setProjectile(CyberdemonBlasterProjectile::new)
					.setBulletSpeed(1.5f)
					.setMuzzleFlashTime(10)
					.setPenetration(PENETRATION_LOW)
					.setTexture("textures/guns/cyberdemonblaster")
					.setDamageDrop(15, 30, 8.0f)
					.setHandType(GunHandType.ONE_POINT_FIVE_HANDED)
					.setCrossHair(Crosshair.FOUR_PARTS_SPIKED)
					.setMuzzleLight(0.9f, 0.8f, 0.1f)
					.setAIStats(RANGE_MEDIUM, 40, 0, 0));
	public static final DeferredItem<GenericGun> BLASTERRIFLE = TGItems.ITEMS.register("blasterrifle",
			() -> new GenericGun(AmmoTypes.ENERGY_CELL, false, 5, 50, 45, 10.0f, TGSounds.BLASTER_RIFLE_FIRE, TGSounds.LASERGUN_RELOAD, MAX_RANGE_RIFLE, 0.025f)
					.setProjectile(BlasterProjectile::new)
					.setZoom(0.5f, true, 0.75f, true)
					.setTexture("textures/guns/blasterrifle")
					.setDamageDrop(25, 35, 8.0f)
					.setPenetration(PENETRATION_MED)
					.setCrossHair(Crosshair.HORIZONTAL_TWO_PART)
					.setMuzzleLight(1.0f, 0f, 0f)
					.setAIStats(RANGE_MEDIUM, 30, 5, 3));
	public static final DeferredItem<GenericGun> SONICSHOTGUN = TGItems.ITEMS.register("sonicshotgun",
			() -> new SonicShotgun(AmmoTypes.ENERGY_CELL, true, 12, 8, 40, 25.0f, TGSounds.SONIC_SHOTGUN_FIRE, TGSounds.SONIC_SHOTGUN_RELOAD, 20, 0.0f)
					.setProjectile(SonicShotgunProjectile::new)
					.setDamageDrop(5, 15, 5.0f)
					.setPenetration(PENETRATION_MED)
					.setTexture("textures/guns/sonicshotgun")
					.setCrossHair(Crosshair.QUAD_NO_CORNERS)
					.setMuzzleLight(0.6f, 0.8f, 1.0f)
					.setAIStats(RANGE_SHORT, 40, 0, 0));
	public static final DeferredItem<GenericGun> SCATTERBEAMRIFLE = TGItems.ITEMS.register("scatterbeamrifle",
			() -> new GenericGun(AmmoTypes.ENERGY_CELL, false, 7, 40, 45, 6.0f, TGSounds.LASERGUN_FIRE, TGSounds.LASERGUN_RELOAD, 30, 0.1f)
					.setProjectile(BlasterProjectile::new)
					.setShotgunSpread(4, 0.15f, false)
					.setBulletSpeed(1.5f)
					.setZoom(0.75f, true, 0.75f, false)
					.setBulletSpeed(2.0f)
					.setTexture("textures/guns/lasergunnew")
					.setCrossHair(Crosshair.FOUR_PARTS)
					.setMuzzleLight(1.0f, 0f, 0f)
					.setAIStats(RANGE_SHORT, 30, 0, 0));
	public static final DeferredItem<GenericGun> NUCLEARDEATHRAY = TGItems.ITEMS.register("nucleardeathray",
			() -> new GenericGun(AmmoTypes.NUCLEAR_POWER_CELL, false, 5, 40, 50, 6.0f, TGSounds.BEAMGUN_FIRE, TGSounds.LASERGUN_RELOAD, MAX_RANGE_SNIPER, 0.0f)
					.setProjectile(NDRProjectile::new)
					.setFiresoundStart(TGSounds.BEAMGUN_START)
					.setMaxLoopDelay(10)
					.setRecoiltime(10)
					.setCheckRecoil()
					.setBulletSpeed(100.0f)
					.setTexture("textures/guns/ndr")
					.setPenetration(PENETRATION_MED)
					.setDamageDrop(20, 40, 1.0f)
					.setHandType(GunHandType.TWO_HANDED)
					.setCrossHair(Crosshair.TRI)
					.setMuzzleLight(0.2f, 1.0f, 0.2f)
					.setAIStats(RANGE_MEDIUM, 40, 5, 5));
	public static final DeferredItem<GenericGun> MIBGUN = TGItems.ITEMS.register("mibgun",
			() -> new GenericGun(AmmoTypes.ENERGY_CELL, true, 8, 20, 45, 16.0f, TGSounds.MIBGUN_FIRE, TGSounds.MIBGUN_RELOAD, MAX_RANGE_PISTOL, 0.035f)
					.setProjectile(DeatomizerProjectile::new)
					.setTexture("textures/guns/mibgun")
					.setDamageDrop(20, 30, 8.0f)
					.setBulletSpeed(1.5f)
					.setPenetration(PENETRATION_MED)
					.setHandType(GunHandType.ONE_HANDED)
					.setCrossHair(Crosshair.HORIZONTAL_TWO_PART)
					.setMuzzleLight(1.0f, 0.5f, 0.05f)
					.setAIStats(RANGE_MEDIUM, 60, 0, 0));
	public static final DeferredItem<GenericGun> GAUSSRIFLE = TGItems.ITEMS.register("gaussrifle",
			() -> new GenericGun(AmmoTypes.AMMO_GAUSS_RIFLE, true, 30, 8, 60, 40.0f, TGSounds.GAUSS_RIFLE_FIRE, TGSounds.GAUSS_RIFLE_RELOAD, MAX_RANGE_SNIPER, 0.025f)
					.setProjectile(GaussProjectile::new)
					.setZoom(0.35f, true, 0.0f, true)
					.setBulletSpeed(5.0f)
					.setRechamberSound(TGSounds.GAUSS_RIFLE_RECHAMBER)
					.setRecoiltime(8)
					.setTexture("textures/guns/gaussrifle")
					.setForwardOffset(0.45f)
					.setPenetration(PENETRATION_MED_HIGH)
					.setCrossHair(Crosshair.HORIZONTAL_TWO_PART_LARGE)
					.setMuzzleLight(0f, 0.8f, 1.0f)
					.setAIStats(RANGE_FAR, 30, 0, 0));
	public static final DeferredItem<GenericGun> LASERPISTOL = TGItems.ITEMS.register("laserpistol",
			() -> new GenericGun(AmmoTypes.REDSTONE_BATTERY, false, 6, 20, 40, 9.0f, TGSounds.LASER_PISTOL_FIRE, TGSounds.LASER_PISTOL_RELOAD, MAX_RANGE_SNIPER, 0.025f)
					.setProjectile(LaserProjectile::new)
					.setBulletSpeed(100.0f)
					.setTexture("textures/guns/laser_pistol")
					.setRangeTooltipType(RangeTooltipType.NO_DROP)
					.setHandType(GunHandType.ONE_HANDED)
					.setCrossHair(Crosshair.HORIZONTAL_TWO_PART)
					.setMuzzleLight(1.0f, 0f, 0f)
					.setAIStats(RANGE_MEDIUM, 30, 0, 0));

	public static final DeferredItem<GenericGun> ROCKETLAUNCHER = TGItems.ITEMS.register("rocketlauncher",
			() -> new GenericGun(AmmoTypes.ROCKETS, true, 10, 1, 40, 50.0f, TGSounds.ROCKET_FIRE, TGSounds.ROCKET_RELOAD, 200, 0.05f)
					.setNoZoom()
					.setProjectile(RocketProjectile::create)
					.setGravity(0.01D)
					.setBulletSpeed(1.0f)
					.setRecoiltime(10)
					.setTextures("textures/guns/rocketlauncher", 2)
					.setDamageDrop(3.0f, 5.0f, 10f)
					.setRangeTooltipType(RangeTooltipType.RADIUS)
					.setForwardOffset(0.35f)
					.setCrossHair(Crosshair.QUAD_CORNERS_DOT)
					.setAIStats(RANGE_MEDIUM, 80, 0, 0));
	public static final DeferredItem<GenericGun> GRIMREAPER = TGItems.ITEMS.register("grimreaper",
			() -> new GuidedMissileLauncher(AmmoTypes.ROCKETS_NO_NUKES, false, 6, 4, 40, 50.0f, TGSounds.GUIDEDMISSILE_FIRE, TGSounds.ROCKET_RELOAD, 200, 0.05f, 100, 1)
					.setLockOn(20, 80)
					.setChargedProjectile(GuidedMissileProjectile::create)
					.setFireWhileCharging(true)
					.setChargeFireAnims(false)
					.setBulletSpeed(1.0f)
					.setRecoiltime(10)
					.setAmmoCount(4)
					.setTexture("textures/guns/grimreaper")
					.setDamageDrop(3f, 5f, 10f)
					.setRangeTooltipType(RangeTooltipType.RADIUS)
					.setCrossHair(Crosshair.VANILLA)
					.setAIStats(RANGE_MEDIUM, 120, 4, 30));
	public static final DeferredItem<GenericGun> GRENADELAUNCHER = TGItems.ITEMS.register("grenadelauncher",
			() -> new GenericGun(AmmoTypes.GRENADES_40MM, true, 5, 6, 100, 30.0f, TGSounds.GRENADE_LAUNCHER_FIRE, TGSounds.GRENADE_LAUNCHER_RELOAD, 160, 0.015f)
					.setNoZoom()
					.setProjectile(Grenade40mmProjectile::new)
					.setTexture("textures/guns/grenadelauncher")
					.setBulletSpeed(0.5f)
					.setAmmoCount(6)
					.setDamageDrop(4.0f, 8.0f, 12f)
					.setGravity(0.01d)
					.setRangeTooltipType(RangeTooltipType.RADIUS)
					.setCrossHair(Crosshair.QUAD_NO_CORNERS)
					.setAIStats(RANGE_MEDIUM, 40, 3, 20));
	public static final DeferredItem<GenericGun> BIOGUN = TGItems.ITEMS.register("biogun",
			() -> new GenericGunCharge(AmmoTypes.BIO_TANK, false, 6, 30, 45, 10.0f, TGSounds.BIOGUN_FIRE, TGSounds.BIOGUN_RELOAD, MAX_RANGE_PISTOL, 0.015f, 30.0f, 3)
					.setChargedProjectile(BioGunProjectile::new)
					.setChargeSound(TGSounds.BIOGUN_CHARGE)
					.setChargeFX("biogunCharge", -0.12f, -0.07f, 0.27f)
					.setBulletSpeed(0.75f)
					.setGravity(0.01d)
					.setPenetration(PENETRATION_LOW)
					.setTexture("textures/guns/biogun")
					.setDamageDrop(8, 15, 8.0f)
					.setForwardOffset(0.40f)
					.setCrossHair(Crosshair.QUAD_NO_CORNERS)
					.setMuzzleLight(0.2f, 0.9f, 0.5f)
					.setAIStats(RANGE_SHORT, 30, 0, 0));
	public static final DeferredItem<GenericGun> GUIDEDMISSILELAUNCHER = TGItems.ITEMS.register("guidedmissilelauncher",
			() -> new GuidedMissileLauncher(AmmoTypes.ROCKETS_NO_NUKES, true, 10, 1, 40, 50.0f, TGSounds.GUIDEDMISSILE_FIRE, TGSounds.ROCKET_RELOAD, 100, 0.05f, 200, 1)
					.setLockOn(20, 80)
					.setChargedProjectile(GuidedMissileProjectile::create)
					.setFireWhileCharging(true)
					.setChargeFireAnims(false)
					.setBulletSpeed(1.0f)
					.setRecoiltime(10)
					.setTexture("textures/guns/guidedmissilelauncher")
					.setDamageDrop(2f, 4f, 20f)
					.setRangeTooltipType(RangeTooltipType.RADIUS)
					.setCrossHair(Crosshair.QUAD_CORNERS_DOT)
					.setAIStats(RANGE_MEDIUM, 80, 0, 0));
	public static final DeferredItem<GenericGun> TFG = TGItems.ITEMS.register("tfg",
			() -> new GenericGunCharge(AmmoTypes.NUCLEAR_POWER_CELL, false, 5, 20, 45, 50.0f, TGSounds.TFG_FIRE, TGSounds.BIOGUN_RELOAD, 100, 0.015f, 60.0f, 10)
					.setChargedProjectile(TFGProjectile::new)
					.setChargeSound(TGSounds.TFG_CHARGE)
					.setChargeFX("TFGChargeStart", -0.14f, -0.10f, 0.42f)
					.setBulletSpeed(2.0f)
					.setRangeTooltipType(RangeTooltipType.RADIUS)
					.setPenetration(PENETRATION_MED_HIGH)
					.setTexture("textures/guns/tfg")
					.setDamageDrop(8, 15, 15.0f)
					.setMuzzleFlashTime(10)
					.setRecoiltime(10)
					.setForwardOffset(0.40f)
					.setCrossHair(Crosshair.TRI_INV)
					.setMuzzleLight(0.2f, 1.0f, 0.2f)
					.setAIStats(RANGE_SHORT, 30, 0, 0));

	// Powered melee tools (M3.4). Their upgrade heads live here as they only matter to them.

	public static final DeferredItem<GenericGun> POWERHAMMER = TGItems.ITEMS.register("powerhammer",
			() -> new GenericGunMeleeCharge(AmmoTypes.COMPRESSED_AIR_TANK, false, 4, 300, 45, 3.5f, TGSounds.POWERHAMMER_FIRE, TGSounds.POWERHAMMER_RELOAD, 3, 0.0f, 20f, 5)
					.setMeleeDmg(6, 2)
					.setTool("pickaxe", 2)
					.setTool("shovel", 2)
					.setDigSpeed(12)
					.setMiningHeads(2.0f, TGItems.POWERHAMMERHEAD_OBSIDIAN, TGItems.POWERHAMMERHEAD_CARBON)
					.setChargedDamagePerHead(0.5f)
					.setMeleeSounds(TGSounds.POWERHAMMER_SWING, TGSounds.POWERHAMMER_IMPACT, TGSounds.POWERHAMMER_IMPACT, true)
					.setChargedProjectile(PowerHammerProjectile::charged)
					.setChargeSound(TGSounds.POWERHAMMER_CHARGE)
					.setBulletSpeed(1.0f)
					.setTexture("textures/guns/powerhammer")
					.setRecoiltime(12)
					.setDamageDrop(3, 3, 2.5f)
					.setAIStats(RANGE_MELEE, 30, 0, 0));
	public static final DeferredItem<GenericGun> CHAINSAW = TGItems.ITEMS.register("chainsaw",
			() -> new GenericGunMeleeCharge(AmmoTypes.FUEL_TANK, false, 3, 300, 45, 10.0f, TGSounds.CHAINSAW_LOOP, TGSounds.POWERHAMMER_RELOAD, 2, 0.0f, 1f, 1)
					.setMeleeDmg(12, 2)
					.setTool("axe", 3)
					.setDigSpeed(14)
					.setMiningHeads(3.0f, TGItems.CHAINSAWBLADES_OBSIDIAN, TGItems.CHAINSAWBLADES_CARBON)
					.setRightClick(RightClick.FIRE)
					.setMeleeSounds(TGSounds.CHAINSAW_LOOP, TGSounds.CHAINSAW_HIT, TGSounds.CHAINSAW_HIT, false)
					.setChargedProjectile(PowerHammerProjectile::charged)
					.setTexture("textures/guns/chainsaw")
					.setRecoiltime(5)
					.setFiresoundStart(TGSounds.CHAINSAW_LOOP_START)
					.setMaxLoopDelay(10)
					.setPenetration(PENETRATION_MED)
					.setAIStats(RANGE_MELEE, 10, 0, 0));
	public static final DeferredItem<GenericGun> MININGDRILL = TGItems.ITEMS.register("miningdrill",
			() -> new GenericGunMeleeCharge(AmmoTypes.FUEL_TANK, false, 3, 300, 45, 10.0f, TGSounds.DRILLER_LOOP, TGSounds.POWERHAMMER_RELOAD, 2, 0.0f, 1f, 1)
					.setMeleeDmg(12, 2)
					.setTool("pickaxe", 3)
					.setTool("shovel", 3)
					.setDigSpeed(14)
					.setMiningRadius(1)
					.setSwingSoundDelay(10)
					.setMiningHeads(2.0f, TGItems.MININGDRILLHEAD_CARBON)
					.setRightClick(RightClick.FIRE)
					.setMeleeSounds(TGSounds.DRILLER_SWING, TGSounds.DRILLER_BREAK, TGSounds.DRILLER_HIT, true)
					.setChargedProjectile(PowerHammerProjectile::charged)
					.setTexture("textures/guns/miningdrill_obsidian")
					.setRecoiltime(5)
					.setFiresoundStart(TGSounds.DRILLER_SWING)
					.setMaxLoopDelay(10)
					.setPenetration(PENETRATION_MED)
					.setAIStats(RANGE_MELEE, 10, 0, 0));
	public static final DeferredItem<GenericGun> SHISHKEBAP = TGItems.ITEMS.register("shishkebap",
			() -> new GenericGunMeleeCharge(AmmoTypes.FUEL_TANK, false, 3, 300, 50, 12.0f, TGSounds.SHISHKEBAP_SWING, TGSounds.SHISHKEBAP_RELOAD, 2, 0.0f, 1f, 1)
					.setMeleeDmg(14, 2)
					.setTool("sword", 3)
					.setDigSpeed(4)
					.setHasCustomAnim(false)
					.setRightClick(RightClick.NONE)
					.setMeleeSounds(TGSounds.SHISHKEBAP_SWING, null, null, true)
					.setFireOnHit(3)
					.setChargedProjectile(PowerHammerProjectile::charged)
					.setTextures("textures/guns/shishkebab_texture", 3)
					.setRecoiltime(5)
					.setPenetration(PENETRATION_MED)
					.setHandType(GunHandType.ONE_HANDED)
					.setNoBowAnim()
					.setAmbientFX("ScreenTestFX")
					.setAIStats(RANGE_MELEE, 10, 0, 0));

	public static final DeferredItem<GenericGrenade> STIELGRANATE = TGItems.ITEMS.register("stielgranate",
			() -> new GenericGrenade()
					.setChargedProjectile(GrenadeProjectile::new)
					.setDamageAndRadius(10, 3.0f, 5, 5.0f));
	public static final DeferredItem<GenericGrenade> FRAGGRENADE = TGItems.ITEMS.register("fraggrenade",
			() -> new GenericGrenade()
					.setChargedProjectile(FragGrenadeProjectile::new)
					.setStartSound(TGSounds.GRENADE_PIN)
					.setDamageAndRadius(12, 3.5f, 6, 7.0f));

	/** Loads the class so the guns above get registered. */
	static void init() {}
}
