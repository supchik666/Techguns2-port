package techguns;

import java.util.List;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * config/techguns-client.toml, and the per-world serverconfig/techguns-server.toml (synced to clients); NeoForge reloads
 * both when the file changes. 1.12 TGConfig options that 1.21 makes data-driven (ore and structure generation, the
 * overall NPC spawn weight) are datapack JSON now; see PORTING.md M8.1.
 */
public final class TGConfig {
	private static final ModConfigSpec.Builder CLIENT = new ModConfigSpec.Builder();

	public static final ModConfigSpec.BooleanValue BLOOM = CLIENT
			.comment("Glow around muzzle flashes, energy shots, explosions and lit parts of guns and armor. Off while an Iris/Oculus shader pack is on.")
			.define("bloom", true);

	public static final ModConfigSpec.DoubleValue BLOOM_INTENSITY = CLIENT
			.comment("Strength of the bloom glow.")
			.defineInRange("bloomIntensity", 1.25, 0.0, 5.0);

	public static final ModConfigSpec.DoubleValue LIGHT_TINT = CLIENT
			.comment("How strongly muzzle flashes and explosions tint their surroundings with their color. 0 turns it off.")
			.defineInRange("lightTint", 1.5, 0.0, 5.0);

	public static final ModConfigSpec.BooleanValue SCOPE_LENS = CLIENT
			.comment("Lens look through scopes: the view bends and splits into colors toward the rim, which is shaded. Off while an Iris/Oculus shader pack is on.")
			.define("scopeLens", true);

	public static final ModConfigSpec.BooleanValue SCREEN_SHAKE = CLIENT
			.comment("Camera shake from your own shots, and the view kick through scopes.")
			.define("screenShake", true);

	public static final ModConfigSpec.BooleanValue DEATH_EFFECTS = CLIENT
			.comment("Mobs killed by some Techguns weapons burst into gore, melt away or burn to ash.")
			.define("deathEffects", true);

	public static final ModConfigSpec CLIENT_SPEC = CLIENT.build();

	private static final ModConfigSpec.Builder SERVER = new ModConfigSpec.Builder();

	public static final ModConfigSpec.DoubleValue DAMAGE_PVP = SERVER
			.comment("Damage factor Techguns weapons deal when fired from players against other players (no damage at all when PvP is off).")
			.defineInRange("damagePvP", 0.5, 0.0, 100.0);

	public static final ModConfigSpec.DoubleValue DAMAGE_TURRET_TO_PLAYER = SERVER
			.comment("Damage factor Techguns turrets deal when hitting players.")
			.defineInRange("damageTurretToPlayer", 0.5, 0.0, 100.0);

	public static final ModConfigSpec.DoubleValue DAMAGE_FACTOR_NPC = SERVER
			.comment("Damage factor for Techguns weapons fired by mobs other than turrets.")
			.defineInRange("damageFactorNPC", 1.0, 0.0, 100.0);

	public static final ModConfigSpec.DoubleValue GORE_CHANCE = SERVER
			.comment("Chance that a kill by a Techguns weapon bursts the body into gibs (1.12 rolled a chance per weapon, 0.25 to 1).")
			.defineInRange("goreChance", 1.0, 0.0, 1.0);

	public static final ModConfigSpec.BooleanValue HELICOPTER_SELF_DAMAGE = SERVER
			.comment("Attack helicopters are hurt by their own rockets, as in 1.12. They fly well inside the blast, so this downs them after a few volleys.")
			.define("helicopterSelfDamage", false);

	public static final ModConfigSpec.BooleanValue EXPLOSION_BLOCK_DAMAGE = SERVER
			.comment("Explosive and incendiary shots from players outside safe mode break blocks and light fires.")
			.define("explosionBlockDamage", true);

	public static final ModConfigSpec.BooleanValue RESTRICT_UNSAFE_MODE_TO_OP = SERVER
			.comment("Only opped players can turn off safe mode. Overrides the permission 'techguns.allowunsafemode'.")
			.define("restrictUnsafeModeToOP", false);

	public static final ModConfigSpec.BooleanValue DISABLE_AUTOFEEDER = SERVER
			.comment("Don't eat automatically from the food slots of the Techguns inventory.")
			.define("disableAutofeeder", false);

	// 1.12 WIP_disableRadiationSystem defaulted to off; the port's radiation is finished, so it's on by default
	public static final ModConfigSpec.BooleanValue RADIATION = SERVER
			.comment("Players build up radiation from radiation effects and get sick from it.")
			.define("radiation", true);

	public static final ModConfigSpec.BooleanValue MACHINES_NEED_NO_POWER = SERVER
			.comment("Machines, turrets and the ore drill run without power or fuel, for playing without a mod that makes power.")
			.define("machinesNeedNoPower", false);

	public static final ModConfigSpec.DoubleValue MACHINE_POWER_MULTIPLIER = SERVER
			.comment("Multiplies the FE/t that machine recipes use (1 = 1.12's values). Not the charging station, reaction chamber, ore drill or turrets.")
			.defineInRange("machinePowerMultiplier", 10.0, 0.0, 1000.0);

	public static final ModConfigSpec.IntValue UPGRADE_XP_COST = SERVER
			.comment("XP per enchantment level the upgrade bench charges.")
			.defineInRange("upgradeXPCost", 20, 0, 10000);

	public static final ModConfigSpec.DoubleValue CHARGE_MAX_HARDNESS = SERVER
			.comment("Hardest block (exclusive) mining charges can break; obsidian is 50.")
			.defineInRange("explosiveChargeMaxHardness", 30.0, 0.0, Float.MAX_VALUE);

	public static final ModConfigSpec.DoubleValue CHARGE_ADVANCED_MAX_HARDNESS = SERVER
			.comment("Hardest block (inclusive) advanced mining charges can break; obsidian is 50.")
			.defineInRange("explosiveChargeAdvancedMaxHardness", 100.0, 0.0, Float.MAX_VALUE);

	public static final ModConfigSpec.DoubleValue ORE_DRILL_ORES = SERVER
			.comment("Multiplier on how many ores an ore drill produces.")
			.defineInRange("oreDrillMultiplierOre", 1.0, 0.001, 1000.0);

	public static final ModConfigSpec.DoubleValue ORE_DRILL_POWER = SERVER
			.comment("Multiplier on how much power an ore drill needs.")
			.defineInRange("oreDrillMultiplierPower", 1.0, 0.0, 1000.0);

	public static final ModConfigSpec.DoubleValue ORE_DRILL_FUEL = SERVER
			.comment("FE an ore drill gets per tick of furnace burn time (or per 1/20 mB of lava).")
			.defineInRange("oreDrillFuelMultiplier", 1000.0, 1.0, 100000.0);

	public static final ModConfigSpec.IntValue SPAWN_DISTANCE_0 = SERVER
			.comment("Up to this distance from world spawn only NPCs of danger level 0 spawn (half that in the nether).")
			.defineInRange("distanceSpawnLevel0", 500, 0, Integer.MAX_VALUE);

	public static final ModConfigSpec.IntValue SPAWN_DISTANCE_1 = SERVER
			.comment("Up to this distance from world spawn only NPCs of danger level up to 1 spawn.")
			.defineInRange("distanceSpawnLevel1", 1000, 0, Integer.MAX_VALUE);

	public static final ModConfigSpec.IntValue SPAWN_DISTANCE_2 = SERVER
			.comment("Up to this distance from world spawn only NPCs of danger level up to 2 spawn.")
			.defineInRange("distanceSpawnLevel2", 2500, 0, Integer.MAX_VALUE);

	// weights as tuned for the port (1.12: soldiers and skeletons 100, bandits 50)
	public static final ModConfigSpec.IntValue SPAWN_ZOMBIE_SOLDIER = spawnWeight("spawnWeightZombieSoldier", 200);
	public static final ModConfigSpec.IntValue SPAWN_ZOMBIE_FARMER = spawnWeight("spawnWeightZombieFarmer", 200);
	public static final ModConfigSpec.IntValue SPAWN_ZOMBIE_MINER = spawnWeight("spawnWeightZombieMiner", 200);
	public static final ModConfigSpec.IntValue SPAWN_SKELETON_SOLDIER = spawnWeight("spawnWeightSkeletonSoldier", 200);
	public static final ModConfigSpec.IntValue SPAWN_PSYCHO_STEVE = spawnWeight("spawnWeightPsychoSteve", 3);
	public static final ModConfigSpec.IntValue SPAWN_BANDIT = spawnWeight("spawnWeightBandit", 100);
	public static final ModConfigSpec.IntValue SPAWN_ZOMBIE_PIGMAN_SOLDIER = spawnWeight("spawnWeightZombiePigmanSoldier", 100);
	public static final ModConfigSpec.IntValue SPAWN_CYBER_DEMON = spawnWeight("spawnWeightCyberDemon", 30);

	public static final ModConfigSpec.ConfigValue<List<? extends String>> NPC_BIOME_BLACKLIST = SERVER
			.comment("Biomes (e.g. minecraft:mushroom_fields) where Techguns NPCs don't spawn naturally.")
			.defineListAllowEmpty("npcBiomeBlacklist", List.of(), () -> "minecraft:", o -> o instanceof String);

	public static final ModConfigSpec SERVER_SPEC = SERVER.build();

	private static ModConfigSpec.IntValue spawnWeight(String name, int weight) {
		return SERVER.comment("Natural spawn weight among Techguns NPCs; 0 stops it spawning naturally.").defineInRange(name, weight, 0, 10000);
	}

	private TGConfig() {}
}
