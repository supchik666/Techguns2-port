package techguns;

import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Sound events, named as in 1.12 sounds.json (copied by the asset script). */
public class TGSounds {
	public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, Techguns.MODID);

	public static final DeferredHolder<SoundEvent, SoundEvent> PISTOL_FIRE = register("guns.pistolfire");
	public static final DeferredHolder<SoundEvent, SoundEvent> PISTOL_RELOAD = register("guns.pistolreload");
	public static final DeferredHolder<SoundEvent, SoundEvent> HANDGUN_FIRE = register("guns.handgunfire");
	public static final DeferredHolder<SoundEvent, SoundEvent> HANDGUN_RELOAD = register("guns.handgunreload");
	public static final DeferredHolder<SoundEvent, SoundEvent> SAWEDOFF_FIRE = register("guns.sawedofffire");
	public static final DeferredHolder<SoundEvent, SoundEvent> SAWEDOFF_RELOAD = register("guns.sawedoffreload");
	public static final DeferredHolder<SoundEvent, SoundEvent> REVOLVER_FIRE = register("guns.revolverfire");
	public static final DeferredHolder<SoundEvent, SoundEvent> REVOLVER_RELOAD = register("guns.revolverreload");
	public static final DeferredHolder<SoundEvent, SoundEvent> REVOLVER_GOLDEN_FIRE = register("guns.goldenrevolverfire");
	public static final DeferredHolder<SoundEvent, SoundEvent> THOMPSON_FIRE = register("guns.thompsonfire");
	public static final DeferredHolder<SoundEvent, SoundEvent> THOMPSON_RELOAD = register("guns.thompsonreload");
	public static final DeferredHolder<SoundEvent, SoundEvent> AK_FIRE = register("guns.ak47fire");
	public static final DeferredHolder<SoundEvent, SoundEvent> AK_RELOAD = register("guns.ak47reload");
	public static final DeferredHolder<SoundEvent, SoundEvent> BOLT_ACTION_FIRE = register("guns.boltactionfire");
	public static final DeferredHolder<SoundEvent, SoundEvent> BOLT_ACTION_RELOAD = register("guns.boltactionreload");
	public static final DeferredHolder<SoundEvent, SoundEvent> BOLT_ACTION_RECHAMBER = register("guns.boltactionrechamber");
	public static final DeferredHolder<SoundEvent, SoundEvent> M4_FIRE = register("guns.assaultriflefire");
	public static final DeferredHolder<SoundEvent, SoundEvent> M4_RELOAD = register("guns.assaultriflereload");
	public static final DeferredHolder<SoundEvent, SoundEvent> M4_SILENCED_FIRE = register("guns.silencedm4fire");
	public static final DeferredHolder<SoundEvent, SoundEvent> COMBATSHOTGUN_FIRE = register("guns.combatshotgunfire");
	public static final DeferredHolder<SoundEvent, SoundEvent> COMBATSHOTGUN_RELOAD = register("guns.combatshotgunreload");
	public static final DeferredHolder<SoundEvent, SoundEvent> COMBATSHOTGUN_RECHAMBER = register("guns.combatshotgunpump");
	public static final DeferredHolder<SoundEvent, SoundEvent> MAC10_FIRE = register("guns.mac10fire");
	public static final DeferredHolder<SoundEvent, SoundEvent> LMG_FIRE = register("guns.lmgfire");
	public static final DeferredHolder<SoundEvent, SoundEvent> LMG_RELOAD = register("guns.lmgreload");
	public static final DeferredHolder<SoundEvent, SoundEvent> MINIGUN_FIRE = register("guns.minigunfire");
	public static final DeferredHolder<SoundEvent, SoundEvent> MINIGUN_RELOAD = register("guns.minigunreload");
	public static final DeferredHolder<SoundEvent, SoundEvent> AS50_FIRE = register("guns.as50fire");
	public static final DeferredHolder<SoundEvent, SoundEvent> AS50_RELOAD = register("guns.as50reload");
	public static final DeferredHolder<SoundEvent, SoundEvent> VECTOR_FIRE = register("guns.vectorfire");
	public static final DeferredHolder<SoundEvent, SoundEvent> VECTOR_RELOAD = register("guns.vectorreload");
	public static final DeferredHolder<SoundEvent, SoundEvent> SCAR_FIRE = register("guns.scarfire");
	public static final DeferredHolder<SoundEvent, SoundEvent> SCAR_RELOAD = register("guns.scarreload");
	public static final DeferredHolder<SoundEvent, SoundEvent> AUG_FIRE = register("guns.augfire");
	public static final DeferredHolder<SoundEvent, SoundEvent> AUG_RELOAD = register("guns.augreload");
	public static final DeferredHolder<SoundEvent, SoundEvent> PDW_FIRE = register("guns.pdwfire");
	public static final DeferredHolder<SoundEvent, SoundEvent> PDW_RELOAD = register("guns.pdwreload");
	public static final DeferredHolder<SoundEvent, SoundEvent> PULSE_RIFLE_FIRE = register("guns.pulseriflefire");
	public static final DeferredHolder<SoundEvent, SoundEvent> PULSE_RIFLE_RELOAD = register("guns.pulseriflereload");

	public static final DeferredHolder<SoundEvent, SoundEvent> BULLET_IMPACT_STONE = register("guns.impact.rock");
	public static final DeferredHolder<SoundEvent, SoundEvent> BULLET_IMPACT_WOOD = register("guns.impact.wood");
	public static final DeferredHolder<SoundEvent, SoundEvent> BULLET_IMPACT_GLASS = register("guns.impact.glass");
	public static final DeferredHolder<SoundEvent, SoundEvent> BULLET_IMPACT_METAL = register("guns.impact.metal");
	public static final DeferredHolder<SoundEvent, SoundEvent> BULLET_IMPACT_DIRT = register("guns.impact.dirt");

	// Energy guns (M3.2)
	public static final DeferredHolder<SoundEvent, SoundEvent> FLAMETHROWER_FIRE = register("guns.flamethrowerfire");
	public static final DeferredHolder<SoundEvent, SoundEvent> FLAMETHROWER_RELOAD = register("guns.flamethrowerreload");
	public static final DeferredHolder<SoundEvent, SoundEvent> FLAMETHROWER_START = register("guns.flamethrowerstart");
	public static final DeferredHolder<SoundEvent, SoundEvent> TESLA_FIRE = register("guns.teslafire");
	public static final DeferredHolder<SoundEvent, SoundEvent> TESLA_RELOAD = register("guns.teslareload");
	public static final DeferredHolder<SoundEvent, SoundEvent> LASERGUN_FIRE = register("guns.lasergunfire");
	public static final DeferredHolder<SoundEvent, SoundEvent> LASERGUN_RELOAD = register("guns.lasergunreload");
	public static final DeferredHolder<SoundEvent, SoundEvent> ALIENBLASTER_FIRE = register("guns.alienblasterfire");
	public static final DeferredHolder<SoundEvent, SoundEvent> ALIENBLASTER_RELOAD = register("guns.alienblasterreload");
	public static final DeferredHolder<SoundEvent, SoundEvent> NETHERBLASTER_FIRE = register("guns.cyberdemonblasterfire");
	/** Not in sounds.json, silent as in 1.12. */
	public static final DeferredHolder<SoundEvent, SoundEvent> NETHERBLASTER_RELOAD = register("guns.cyberdemonblasterreload");
	public static final DeferredHolder<SoundEvent, SoundEvent> BLASTER_RIFLE_FIRE = register("guns.blasterriflefire");
	public static final DeferredHolder<SoundEvent, SoundEvent> SONIC_SHOTGUN_FIRE = register("guns.sonicshotgunfire");
	public static final DeferredHolder<SoundEvent, SoundEvent> SONIC_SHOTGUN_RELOAD = register("guns.sonicshotgunreload");
	public static final DeferredHolder<SoundEvent, SoundEvent> BEAMGUN_START = register("guns.beamgunstart");
	public static final DeferredHolder<SoundEvent, SoundEvent> BEAMGUN_FIRE = register("guns.beamgunfire");
	public static final DeferredHolder<SoundEvent, SoundEvent> MIBGUN_FIRE = register("guns.mibblasterfire");
	public static final DeferredHolder<SoundEvent, SoundEvent> MIBGUN_RELOAD = register("guns.mibblasterreload");
	public static final DeferredHolder<SoundEvent, SoundEvent> GAUSS_RIFLE_FIRE = register("guns.gaussriflefire");
	public static final DeferredHolder<SoundEvent, SoundEvent> GAUSS_RIFLE_RELOAD = register("guns.gaussriflereload");
	public static final DeferredHolder<SoundEvent, SoundEvent> GAUSS_RIFLE_RECHAMBER = register("guns.gaussriflerechamber");
	public static final DeferredHolder<SoundEvent, SoundEvent> LASER_PISTOL_FIRE = register("guns.laserpistolfire");
	public static final DeferredHolder<SoundEvent, SoundEvent> LASER_PISTOL_RELOAD = register("guns.laserpistolreload");
	public static final DeferredHolder<SoundEvent, SoundEvent> ROCKET_FIRE = register("guns.rocketfire");
	public static final DeferredHolder<SoundEvent, SoundEvent> ROCKET_RELOAD = register("guns.rocketreload");
	public static final DeferredHolder<SoundEvent, SoundEvent> BIOGUN_FIRE = register("guns.biogunfire");
	public static final DeferredHolder<SoundEvent, SoundEvent> BIOGUN_RELOAD = register("guns.biogunreload");
	public static final DeferredHolder<SoundEvent, SoundEvent> BIOGUN_CHARGE = register("guns.bioguncharge");
	public static final DeferredHolder<SoundEvent, SoundEvent> GRENADE_LAUNCHER_FIRE = register("guns.grenadelauncherfire");
	public static final DeferredHolder<SoundEvent, SoundEvent> GRENADE_LAUNCHER_RELOAD = register("guns.grenadelauncherreload");
	public static final DeferredHolder<SoundEvent, SoundEvent> LOCKON_BEEP = register("guns.lockon");
	public static final DeferredHolder<SoundEvent, SoundEvent> LOCKED_BEEP = register("guns.locked");
	public static final DeferredHolder<SoundEvent, SoundEvent> GUIDEDMISSILE_FIRE = register("guns.guidedmissilelauncherfire");
	public static final DeferredHolder<SoundEvent, SoundEvent> TFG_FIRE = register("guns.tfgfire");
	public static final DeferredHolder<SoundEvent, SoundEvent> TFG_CHARGE = register("guns.tfgcharge");
	public static final DeferredHolder<SoundEvent, SoundEvent> GRENADE_PIN = register("guns.grenade_pin");

	// Explosive projectiles (M3.3)
	public static final DeferredHolder<SoundEvent, SoundEvent> NUKE_EXPLOSION = register("effects.nukeexplosion");
	public static final DeferredHolder<SoundEvent, SoundEvent> TFG_EXPLOSION = register("effects.tfgexplosion");
	public static final DeferredHolder<SoundEvent, SoundEvent> TFG_EXPLOSION_ECHO = register("effects.tfgexplosionecho");
	public static final DeferredHolder<SoundEvent, SoundEvent> BIOGUN_IMPACT = register("effects.biogunimpact");
	public static final DeferredHolder<SoundEvent, SoundEvent> DEATH_GORE = register("effects.gore");
	public static final DeferredHolder<SoundEvent, SoundEvent> DEATH_BIO = register("effects.biodeath");
	public static final DeferredHolder<SoundEvent, SoundEvent> DEATH_LASER = register("effects.disintegrate");
	public static final DeferredHolder<SoundEvent, SoundEvent> STEAM_JUMP = register("effects.steamarmorjump");

	// Powered melee tools (M3.4)
	public static final DeferredHolder<SoundEvent, SoundEvent> POWERHAMMER_FIRE = register("guns.powerhammerfire");
	public static final DeferredHolder<SoundEvent, SoundEvent> POWERHAMMER_RELOAD = register("guns.powerhammerreload");
	public static final DeferredHolder<SoundEvent, SoundEvent> POWERHAMMER_SWING = register("guns.powerhammerswing");
	public static final DeferredHolder<SoundEvent, SoundEvent> POWERHAMMER_IMPACT = register("guns.powerhammerimpactground");
	public static final DeferredHolder<SoundEvent, SoundEvent> POWERHAMMER_CHARGE = register("guns.powerhammercharge");
	public static final DeferredHolder<SoundEvent, SoundEvent> CHAINSAW_LOOP = register("guns.chainsawloop");
	public static final DeferredHolder<SoundEvent, SoundEvent> CHAINSAW_LOOP_START = register("guns.chainsawloopstart");
	public static final DeferredHolder<SoundEvent, SoundEvent> CHAINSAW_HIT = register("guns.chainsawhit");
	public static final DeferredHolder<SoundEvent, SoundEvent> DRILLER_LOOP = register("guns.drillerloop");
	public static final DeferredHolder<SoundEvent, SoundEvent> DRILLER_HIT = register("guns.drillerhit");
	public static final DeferredHolder<SoundEvent, SoundEvent> DRILLER_SWING = register("guns.drillerswing");
	public static final DeferredHolder<SoundEvent, SoundEvent> DRILLER_BREAK = register("guns.drillerbreak");
	public static final DeferredHolder<SoundEvent, SoundEvent> SHISHKEBAP_SWING = register("guns.shishkebabfire");
	public static final DeferredHolder<SoundEvent, SoundEvent> SHISHKEBAP_RELOAD = register("guns.shishkebabreload");
	// Radiation (M4)
	public static final DeferredHolder<SoundEvent, SoundEvent> GEIGER_LOW = register("effects.geiger.low");
	public static final DeferredHolder<SoundEvent, SoundEvent> GEIGER_HIGH = register("effects.geiger.high");

	// Extra equipment slots (M4)
	public static final DeferredHolder<SoundEvent, SoundEvent> ANTI_GRAV_START = register("items.antigravstart");
	public static final DeferredHolder<SoundEvent, SoundEvent> NIGHTVISION_ON = register("nightvision.on");
	public static final DeferredHolder<SoundEvent, SoundEvent> NIGHTVISION_OFF = register("nightvision.off");
	public static final DeferredHolder<SoundEvent, SoundEvent> STEAM_JUMP_2 = register("effects.steamarmorjump2");
	public static final DeferredHolder<SoundEvent, SoundEvent> JETPACK_START = register("jetpack.start");
	/** 1.12 asked for "jetpack.end", which sounds.json doesn't have; this is the stop sound it meant. */
	public static final DeferredHolder<SoundEvent, SoundEvent> JETPACK_END = register("jetpack.stop");
	public static final DeferredHolder<SoundEvent, SoundEvent> JETPACK_LOOP = register("jetpack.loop");
	public static final DeferredHolder<SoundEvent, SoundEvent> GLIDER_LOOP = register("glider.loop");

	public static final DeferredHolder<SoundEvent, SoundEvent> AMMO_PRESS_WORK1 = register("machines.ammopresswork1");
	public static final DeferredHolder<SoundEvent, SoundEvent> AMMO_PRESS_WORK2 = register("machines.ammopresswork2");
	public static final DeferredHolder<SoundEvent, SoundEvent> METAL_PRESS_WORK = register("machines.metalpresswork");
	public static final DeferredHolder<SoundEvent, SoundEvent> CHEM_LAB_WORK = register("machines.chemlabwork");
	public static final DeferredHolder<SoundEvent, SoundEvent> CHARGING_STATION_WORK = register("machines.chargingstationwork");
	public static final DeferredHolder<SoundEvent, SoundEvent> GRINDER_START = register("machines.grinder.start");
	public static final DeferredHolder<SoundEvent, SoundEvent> GRINDER_WORK = register("machines.grinder.work");
	// Multiblock machines (M5.2)
	public static final DeferredHolder<SoundEvent, SoundEvent> FABRICATOR_WORK = register("machines.fabricatorwork");
	public static final DeferredHolder<SoundEvent, SoundEvent> RC_HEATRAY_WORK = register("machines.rc_heatraywork");
	public static final DeferredHolder<SoundEvent, SoundEvent> RC_WARNING = register("machines.rc_warning");
	public static final DeferredHolder<SoundEvent, SoundEvent> RC_BEEP = register("machines.rc_beep");
	public static final DeferredHolder<SoundEvent, SoundEvent> ORE_DRILL_SMALL = register("machines.oredrillsmallwork");
	public static final DeferredHolder<SoundEvent, SoundEvent> ORE_DRILL_MEDIUM = register("machines.oredrillmediumwork");
	public static final DeferredHolder<SoundEvent, SoundEvent> ORE_DRILL_LARGE = register("machines.oredrilllargework");

	public static final DeferredHolder<SoundEvent, SoundEvent> TNT_TICK = register("tntcharge.tick");
	public static final DeferredHolder<SoundEvent, SoundEvent> TNT_INIT = register("tntcharge.init");
	public static final DeferredHolder<SoundEvent, SoundEvent> C4_TICK = register("c4charge.tick");
	public static final DeferredHolder<SoundEvent, SoundEvent> C4_INIT = register("c4charge.init");
	public static final DeferredHolder<SoundEvent, SoundEvent> TECHDOOR_OPEN = register("blocks.techdoor.open");
	public static final DeferredHolder<SoundEvent, SoundEvent> TECHDOOR_CLOSE = register("blocks.techdoor.close");
	public static final DeferredHolder<SoundEvent, SoundEvent> TECHDOOR_STATE_FINISHED = register("blocks.techdoor.statefinished");
	public static final DeferredHolder<SoundEvent, SoundEvent> CYBERDEMON_IDLE = register("npcs.cyberdemonidle");
	public static final DeferredHolder<SoundEvent, SoundEvent> CYBERDEMON_HURT = register("npcs.cyberdemonhurt");
	public static final DeferredHolder<SoundEvent, SoundEvent> CYBERDEMON_DEATH = register("npcs.cyberdemondeath");
	public static final DeferredHolder<SoundEvent, SoundEvent> CYBERDEMON_STEP = register("npcs.cyberdemonstep");

	// Special mobs (M6.2)
	public static final DeferredHolder<SoundEvent, SoundEvent> ALIENBUG_IDLE = register("npcs.alienbugidle");
	public static final DeferredHolder<SoundEvent, SoundEvent> ALIENBUG_HURT = register("npcs.alienbughurt");
	public static final DeferredHolder<SoundEvent, SoundEvent> ALIENBUG_DEATH = register("npcs.alienbugdeath");
	public static final DeferredHolder<SoundEvent, SoundEvent> ALIENBUG_STEP = register("npcs.alienbugstep");
	public static final DeferredHolder<SoundEvent, SoundEvent> ALIENBUG_AGGRO = register("npcs.alienbugaggro");
	public static final DeferredHolder<SoundEvent, SoundEvent> ALIENBUG_BITE = register("npcs.alienbugbite");
	public static final DeferredHolder<SoundEvent, SoundEvent> HELICOPTER_HIT = register("npcs.apachehit");
	public static final DeferredHolder<SoundEvent, SoundEvent> HELICOPTER_ROTOR = register("npcs.apacherotor");
	public static final DeferredHolder<SoundEvent, SoundEvent> HELICOPTER_BURST = register("npcs.apacheburst");
	public static final DeferredHolder<SoundEvent, SoundEvent> HELICOPTER_DEATH = register("npcs.apachedie");
	public static final DeferredHolder<SoundEvent, SoundEvent> HELICOPTER_EXPLODE = register("npcs.apacheexplode");

	private static DeferredHolder<SoundEvent, SoundEvent> register(String name) {
		return SOUNDS.register(name, SoundEvent::createVariableRangeEvent);
	}
}
