package techguns.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import techguns.TGEntities;
import techguns.TGItems;
import techguns.Techguns;
import techguns.entities.npcs.AlienBug;
import techguns.entities.npcs.AttackHelicopter;
import techguns.entities.npcs.Ghastling;

@GameTestHolder(Techguns.MODID)
@PrefixGameTestTemplate(false)
public class SpecialMobTests {
	/** 1.12 health and armor, the bug is an arthropod, the eggs spawn their mob, and the loot tables load. */
	@GameTest(template = "empty8")
	public static void specialMobStats(GameTestHelper helper) {
		AlienBug bug = helper.spawnWithNoFreeWill(TGEntities.ALIENBUG.get(), new Vec3(1.5, 1, 1.5));
		Ghastling ghastling = helper.spawnWithNoFreeWill(TGEntities.GHASTLING.get(), new Vec3(6.5, 1, 1.5));
		AttackHelicopter heli = helper.spawnWithNoFreeWill(TGEntities.ATTACK_HELICOPTER.get(), new Vec3(4, 1, 5));
		helper.assertValueEqual(bug.getMaxHealth(), 20.0f, "bug health");
		helper.assertValueEqual(bug.getAttributeValue(Attributes.ATTACK_DAMAGE), 4.0, "bug damage");
		helper.assertTrue(bug.getType().is(EntityTypeTags.ARTHROPOD), "bug not an arthropod");
		helper.assertValueEqual(ghastling.getMaxHealth(), 20.0f, "ghastling health");
		helper.assertValueEqual(ghastling.getArmorValue(), 10, "ghastling armor");
		helper.assertTrue(ghastling.fireImmune(), "ghastling burns");
		helper.assertValueEqual(heli.getMaxHealth(), 100.0f, "helicopter health");
		helper.assertValueEqual(heli.getArmorValue(), 16, "helicopter armor");
		assertEgg(helper, new ItemStack(TGItems.ALIENBUG_SPAWN_EGG.get()), TGEntities.ALIENBUG.get());
		assertEgg(helper, new ItemStack(TGItems.GHASTLING_SPAWN_EGG.get()), TGEntities.GHASTLING.get());
		assertEgg(helper, new ItemStack(TGItems.ATTACK_HELICOPTER_SPAWN_EGG.get()), TGEntities.ATTACK_HELICOPTER.get());
		for (String name : new String[] {"ghastling", "attackhelicopter"}) {
			LootTable table = helper.getLevel().getServer().reloadableRegistries()
					.getLootTable(ResourceKey.create(Registries.LOOT_TABLE, Techguns.id("entities/" + name)));
			helper.assertTrue(table != LootTable.EMPTY, name + " loot table missing");
		}
		// a downed helicopter drops its loot right away; 2-4 cybernetic parts always
		heli.kill();
		helper.assertItemEntityPresent(TGItems.CYBERNETIC_PARTS.get(), new BlockPos(4, 1, 5), 4);
		helper.succeed();
	}

	private static void assertEgg(GameTestHelper helper, ItemStack egg, EntityType<?> type) {
		helper.assertTrue(((net.minecraft.world.item.SpawnEggItem) egg.getItem()).getType(egg) == type, "egg spawns the wrong mob");
	}

	/** The bug bites a pig (re-targeted each tick: in daylight it may give up). */
	@GameTest(template = "empty8")
	public static void alienBugBites(GameTestHelper helper) {
		AlienBug bug = helper.spawn(TGEntities.ALIENBUG.get(), new Vec3(1.5, 1, 1.5));
		Pig pig = helper.spawnWithNoFreeWill(EntityType.PIG, new Vec3(4.5, 1, 4.5));
		helper.succeedWhen(() -> {
			bug.setTarget(pig);
			helper.assertTrue(pig.getHealth() < pig.getMaxHealth(), "pig not bitten");
		});
	}

	/** The ghastling winds up and hits a pig 6 blocks away with its blaster volley. */
	@GameTest(template = "empty8", timeoutTicks = 200)
	public static void ghastlingShoots(GameTestHelper helper) {
		Ghastling ghastling = helper.spawn(TGEntities.GHASTLING.get(), new Vec3(1.5, 1, 1.5));
		Pig pig = helper.spawnWithNoFreeWill(EntityType.PIG, new Vec3(6.5, 1, 5.5));
		helper.succeedWhen(() -> {
			ghastling.setTarget(pig);
			helper.assertTrue(pig.getHealth() < pig.getMaxHealth(), "pig not hit");
		});
	}

	/**
	 * The helicopter's opening burst hits a pig next to it. Times out before the rocket (tick 35), whose 30 block blast would
	 * reach other tests; on success the helicopter is removed.
	 */
	@GameTest(template = "empty8", timeoutTicks = 34)
	public static void helicopterShoots(GameTestHelper helper) {
		AttackHelicopter heli = helper.spawn(TGEntities.ATTACK_HELICOPTER.get(), new Vec3(4, 1, 4));
		Pig pig = helper.spawnWithNoFreeWill(EntityType.PIG, new Vec3(7.5, 1, 4));
		helper.succeedWhen(() -> {
			heli.setTarget(pig);
			helper.assertTrue(pig.getHealth() < pig.getMaxHealth(), "pig not hit");
			heli.discard();
		});
	}
}
