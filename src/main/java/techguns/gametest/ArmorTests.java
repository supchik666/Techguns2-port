package techguns.gametest;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import techguns.Techguns;
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
}
