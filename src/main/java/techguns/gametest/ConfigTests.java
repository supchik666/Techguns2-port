package techguns.gametest;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import techguns.TGConfig;
import techguns.TGEntities;
import techguns.Techguns;
import techguns.damagesystem.DamageSystem;
import techguns.damagesystem.TGDamageSource;
import techguns.damagesystem.TGDamageSource.DeathType;
import techguns.damagesystem.TGDamageType;

@GameTestHolder(Techguns.MODID)
@PrefixGameTestTemplate(false)
public class ConfigTests {
	/** 1.12 getDamageFactor: PvP 0.5, turret to player 0.5, players on mobs 1, other mobs the NPC factor (set to 0.25 here). */
	@GameTest(template = "empty")
	public static void damageFactors(GameTestHelper helper) {
		double npc = TGConfig.DAMAGE_FACTOR_NPC.get();
		TGConfig.DAMAGE_FACTOR_NPC.set(0.25);
		try {
			Player a = helper.makeMockPlayer(GameType.SURVIVAL), b = helper.makeMockPlayer(GameType.SURVIVAL);
			Zombie attacker = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new Vec3(0.5, 1, 0.5));
			Zombie target = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new Vec3(2.5, 1, 0.5));
			helper.assertValueEqual(DamageSystem.damageFactor(a, b), 0.5f, "PvP");
			helper.assertValueEqual(DamageSystem.damageFactor(TGEntities.TURRET.get().create(helper.getLevel()), a), 0.5f, "turret to player");
			helper.assertValueEqual(DamageSystem.damageFactor(a, target), 1.0f, "player to mob");
			helper.assertValueEqual(DamageSystem.damageFactor(null, target), 1.0f, "no attacker");

			// end to end: a TG hit from one zombie on another (no armor) takes a quarter
			target.getAttribute(Attributes.ARMOR).setBaseValue(0);
			target.hurt(new TGDamageSource(TGDamageType.PROJECTILE, DeathType.GORE, attacker, attacker), 8);
			helper.assertValueEqual(target.getMaxHealth() - target.getHealth(), 2.0f, "NPC hit");
		} finally {
			TGConfig.DAMAGE_FACTOR_NPC.set(npc);
		}
		helper.succeed();
	}
}
