package techguns.gametest;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import techguns.TGItems;
import techguns.TGRadiation;
import techguns.Techguns;
import techguns.entities.special.RadiationEntity;

@GameTestHolder(Techguns.MODID)
@PrefixGameTestTemplate(false)
public class RadiationTests {
	/** A second of radiation V with rad resistance II (+2) adds 3; the level stays within 0 to 1000. */
	@GameTest(template = "empty")
	public static void addRadiationRespectsResistance(GameTestHelper helper) {
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		player.addEffect(new MobEffectInstance(TGRadiation.RAD_RESISTANCE, 100, 1));
		player.addEffect(new MobEffectInstance(TGRadiation.RADIATION, 100, 4));
		player.getEffect(TGRadiation.RADIATION).tick(player, () -> {});
		helper.assertValueEqual(TGRadiation.getRadiation(player), 3, "radiation level");
		TGRadiation.addRadiation(player, 5000);
		helper.assertValueEqual(TGRadiation.getRadiation(player), TGRadiation.MAX_RADIATION, "radiation level");
		TGRadiation.addRadiation(player, -5000);
		helper.assertValueEqual(TGRadiation.getRadiation(player), 0, "radiation level");
		helper.succeed();
	}

	/** RadAway's rad regeneration XV takes 15 off a second, and the empty bag comes back. */
	@GameTest(template = "empty")
	public static void radAwayLowersRadiation(GameTestHelper helper) {
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		TGRadiation.addRadiation(player, 500);
		ItemStack left = TGItems.RAD_AWAY.get().finishUsingItem(new ItemStack(TGItems.RAD_AWAY.get()), helper.getLevel(), player);
		helper.assertTrue(left.is(TGItems.INFUSION_BAG.get()), "no infusion bag back");
		player.getEffect(TGRadiation.RAD_REGENERATION).tick(player, () -> {});
		helper.assertValueEqual(TGRadiation.getRadiation(player), 485, "radiation level");
		helper.succeed();
	}

	/** Nothing below 500, hunger from 500, weakness and mining fatigue from 750, nausea, slowness and damage at 1000. */
	@GameTest(template = "empty")
	public static void poisoningThresholds(GameTestHelper helper) {
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		TGRadiation.addRadiation(player, 499);
		TGRadiation.poison(player);
		helper.assertTrue(player.getActiveEffects().isEmpty(), "poisoned below 500");
		TGRadiation.addRadiation(player, 1);
		TGRadiation.poison(player);
		helper.assertTrue(player.hasEffect(MobEffects.HUNGER) && !player.hasEffect(MobEffects.WEAKNESS), "minor poisoning");
		TGRadiation.addRadiation(player, 250);
		TGRadiation.poison(player);
		helper.assertTrue(player.getEffect(MobEffects.HUNGER).getAmplifier() == 1 && player.hasEffect(MobEffects.WEAKNESS)
				&& player.hasEffect(MobEffects.DIG_SLOWDOWN) && !player.hasEffect(MobEffects.CONFUSION), "severe poisoning");
		float health = player.getHealth();
		TGRadiation.addRadiation(player, 250);
		TGRadiation.poison(player);
		helper.assertTrue(player.hasEffect(MobEffects.CONFUSION) && player.hasEffect(MobEffects.MOVEMENT_SLOWDOWN), "lethal poisoning effects");
		helper.assertTrue(player.getHealth() < health, "lethal poisoning didn't hurt");
		helper.succeed();
	}

	/** A nuke's radiation zone: full strength at its center, weaker toward its edge. */
	@GameTest(template = "empty", timeoutTicks = 60)
	public static void radiationZoneIrradiates(GameTestHelper helper) {
		Zombie near = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new Vec3(1.5, 1, 0.5));
		Zombie far = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new Vec3(1.5, 1, 4.5));
		helper.getLevel().addFreshEntity(new RadiationEntity(helper.getLevel(), helper.absoluteVec(new Vec3(1.5, 1, 0.5)), 1200, 1, 9, 6, 2));
		helper.succeedWhen(() -> {
			helper.assertTrue(near.hasEffect(TGRadiation.RADIATION) && near.getEffect(TGRadiation.RADIATION).getAmplifier() == 9, "center not at 9");
			helper.assertTrue(far.hasEffect(TGRadiation.RADIATION) && far.getEffect(TGRadiation.RADIATION).getAmplifier() == 5, "4 blocks out not at 5");
		});
	}
}
