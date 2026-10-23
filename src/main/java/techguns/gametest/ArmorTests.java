package techguns.gametest;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.GameType;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import techguns.TGAttributes;
import techguns.TGComponents;
import techguns.TGItems;
import techguns.TGuns;
import techguns.Techguns;
import techguns.capabilities.TGExtendedPlayer;
import techguns.damagesystem.DeathEffects;
import techguns.damagesystem.TGDamageSource;
import techguns.damagesystem.TGDamageSource.DeathType;
import techguns.damagesystem.TGDamageType;

@GameTestHolder(Techguns.MODID)
@PrefixGameTestTemplate(false)
public class ArmorTests {
	/**
	 * A zombie in full t3_combat (21 projectile and physical armor, 18 fire, toughness 2 a piece) takes 10 damage of {@code type};
	 * the 1.12 formula leaves {@code expected}. Equipment attributes apply on the next tick, so the hit waits one.
	 */
	private static void hit(GameTestHelper helper, TGDamageType type, float penetration, float expected) {
		Zombie zombie = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new Vec3(1.5, 1, 1.5));
		zombie.getAttribute(Attributes.ARMOR).setBaseValue(0);
		String[] pieces = {"helmet", "chestplate", "leggings", "boots"};
		EquipmentSlot[] slots = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
		for (int i = 0; i < 4; i++) zombie.setItemSlot(slots[i], new ItemStack(BuiltInRegistries.ITEM.get(Techguns.id("t3_combat_" + pieces[i]))));
		helper.runAfterDelay(1, () -> {
			TGDamageSource source = new TGDamageSource(type, DeathType.GORE, zombie, null);
			source.penetration = penetration;
			zombie.hurt(source, 10);
			float taken = zombie.getMaxHealth() - zombie.getHealth();
			helper.assertTrue(Math.abs(taken - expected) < 0.01f, type + " pen " + penetration + ": took " + taken + ", expected " + expected);
			helper.succeed();
		});
	}

	/** 21 armor stops 84%. */
	@GameTest(template = "empty")
	public static void armorReducesBullets(GameTestHelper helper) {
		hit(helper, TGDamageType.PROJECTILE, 0, 1.6f);
	}

	/** Penetration 4 (x4 = 16) minus toughness 8 removes 8 points: 13 armor stops 52%. */
	@GameTest(template = "empty")
	public static void penetrationPiercesArmor(GameTestHelper helper) {
		hit(helper, TGDamageType.PROJECTILE, 4, 4.8f);
	}

	/** Fire is resisted by the fire value, 18: 72%. */
	@GameTest(template = "empty")
	public static void armorResistsPerType(GameTestHelper helper) {
		hit(helper, TGDamageType.FIRE, 0, 2.8f);
	}

	/** A full hazmat suit (1 a piece) plus a gas mask in the face slot (1) give 5 radiation resistance. */
	@GameTest(template = "empty")
	public static void hazmatAndGasMaskResistRadiation(GameTestHelper helper) {
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		String[] pieces = {"helmet", "chestplate", "leggings", "boots"};
		EquipmentSlot[] slots = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
		for (int i = 0; i < 4; i++) player.setItemSlot(slots[i], new ItemStack(BuiltInRegistries.ITEM.get(Techguns.id("hazmat_" + pieces[i]))));
		TGExtendedPlayer.get(player).inventory.setItem(TGExtendedPlayer.SLOT_FACE, new ItemStack(TGItems.GAS_MASK.get()));
		NeoForge.EVENT_BUS.post(new PlayerTickEvent.Post(player));
		double resistance = player.getAttributeValue(TGAttributes.RADIATION_RESISTANCE);
		helper.assertTrue(resistance == 5, "resistance " + resistance);
		helper.succeed();
	}

	/** The tooltip percents match the real damage math: 10 armor stops 40%, a penetration-1 gun ignores 16% of it. */
	@GameTest(template = "empty")
	public static void tooltipPercentsMatchDamage(GameTestHelper helper) {
		float plain = techguns.damagesystem.DamageSystem.damageAfterArmor(100, 10, 0, 0);
		float pierced = techguns.damagesystem.DamageSystem.damageAfterArmor(100, 10, 0, techguns.damagesystem.DamageSystem.PENETRATION_POINTS);
		helper.assertTrue(Math.abs(plain - (100 - techguns.damagesystem.DamageSystem.protectionPercent(10))) < 0.01f, "protection % off: " + plain);
		helper.assertTrue(Math.abs(pierced - (plain + techguns.damagesystem.DamageSystem.penetrationPercent(1))) < 0.01f, "armor pen % off: " + pierced);
		helper.succeed();
	}

	/**
	 * Death effects (DeathEffects): gore only on gore-tagged mobs, BIO on any; a powered chainsaw hit gores, an empty one
	 * doesn't. Then a gore kill goes through the death event.
	 */
	@GameTest(template = "empty")
	public static void deathEffectTypes(GameTestHelper helper) {
		Zombie zombie = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new Vec3(1.5, 1, 1.5));
		IronGolem golem = helper.spawnWithNoFreeWill(EntityType.IRON_GOLEM, new Vec3(1.5, 1, 1.5));
		TGDamageSource gore = new TGDamageSource(TGDamageType.PROJECTILE, DeathType.GORE, zombie, null);
		TGDamageSource bio = new TGDamageSource(TGDamageType.POISON, DeathType.BIO, zombie, null);
		gore.goreChance = bio.goreChance = 1;
		helper.assertTrue(DeathEffects.deathType(gore, zombie) == DeathType.GORE, "zombie should gore");
		helper.assertTrue(DeathEffects.deathType(gore, golem) == DeathType.DEFAULT, "iron golem isn't in #techguns:gore");
		helper.assertTrue(DeathEffects.deathType(bio, golem) == DeathType.BIO, "bio death works on any mob");
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		ItemStack chainsaw = new ItemStack(TGuns.CHAINSAW.get());
		player.setItemInHand(InteractionHand.MAIN_HAND, chainsaw);
		helper.assertTrue(DeathEffects.deathType(player.damageSources().playerAttack(player), zombie) == DeathType.GORE, "powered chainsaw should gore");
		chainsaw.set(TGComponents.AMMO, 0);
		helper.assertTrue(DeathEffects.deathType(player.damageSources().playerAttack(player), zombie) == DeathType.DEFAULT, "empty chainsaw gored");
		gore.goreChance = 0;
		helper.assertTrue(DeathEffects.deathType(gore, zombie) == DeathType.DEFAULT, "chance 0 gored");
		zombie.hurt(new TGDamageSource(TGDamageType.PROJECTILE, DeathType.GORE, zombie, null), 1000);
		helper.assertTrue(zombie.isDeadOrDying(), "zombie survived");
		helper.succeed();
	}
}
