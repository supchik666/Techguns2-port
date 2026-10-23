package techguns.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import techguns.TGBlocks;
import techguns.TGComponents;
import techguns.TGItems;
import techguns.TGuns;
import techguns.Techguns;
import techguns.blocks.Door3x3Block;
import techguns.blocks.ExplosiveChargeBlock;
import techguns.items.armors.GenericArmor;
import techguns.machines.CamoBenchBlockEntity;
import techguns.machines.Door3x3BlockEntity;
import techguns.machines.ExplosiveChargeBlockEntity;
import techguns.machines.RepairBenchBlockEntity;
import techguns.machines.UpgradeBenchBlockEntity;

@GameTestHolder(Techguns.MODID)
@PrefixGameTestTemplate(false)
public class BenchTests {
	private static final BlockPos POS = new BlockPos(1, 1, 1);

	private static Item item(String name) {
		return BuiltInRegistries.ITEM.get(Techguns.id(name));
	}

	private static boolean hasArmor(ItemStack stack) {
		return stack.getAttributeModifiers().modifiers().stream().anyMatch(e -> e.attribute().is(Attributes.ARMOR));
	}

	/** A broken t1 combat helmet gives no armor; an iron ingot and heavy cloth repair it fully. */
	@GameTest(template = "empty")
	public static void repairBenchFixesBrokenArmor(GameTestHelper helper) {
		helper.setBlock(POS, TGBlocks.REPAIR_BENCH.get());
		RepairBenchBlockEntity be = helper.getBlockEntity(POS);
		ItemStack helmet = new ItemStack(item("t1_combat_helmet"));
		helper.assertTrue(hasArmor(helmet), "new helmet has no armor");
		helmet.setDamageValue(helmet.getMaxDamage() - 1);
		helper.assertTrue(GenericArmor.isBroken(helmet) && !hasArmor(helmet), "broken helmet still counts as armor");
		be.inventory.setStackInSlot(RepairBenchBlockEntity.SLOT_ITEM, helmet);
		be.inventory.setStackInSlot(0, new ItemStack(Items.IRON_INGOT, 2));
		helper.assertTrue(!be.repair(be.inventory.getStackInSlot(RepairBenchBlockEntity.SLOT_ITEM)), "repaired without heavy cloth");
		be.inventory.setStackInSlot(1, new ItemStack(TGItems.HEAVY_CLOTH.get()));
		helper.assertTrue(be.repair(be.inventory.getStackInSlot(RepairBenchBlockEntity.SLOT_ITEM)), "didn't repair");
		ItemStack repaired = be.inventory.getStackInSlot(RepairBenchBlockEntity.SLOT_ITEM);
		helper.assertTrue(repaired.getDamageValue() == 0 && hasArmor(repaired), "still damaged: " + repaired.getDamageValue());
		helper.assertTrue(be.inventory.getStackInSlot(0).getCount() == 1 && be.inventory.getStackInSlot(1).isEmpty(), "wrong materials used");
		helper.succeed();
	}

	/** The M4's four camos cycle both ways; a wool block becomes the next color. */
	@GameTest(template = "empty")
	public static void camoBenchChangesGunCamo(GameTestHelper helper) {
		helper.setBlock(POS, TGBlocks.CAMO_BENCH.get());
		CamoBenchBlockEntity be = helper.getBlockEntity(POS);
		be.inventory.setStackInSlot(0, new ItemStack(TGuns.M4.get()));
		be.buttonClicked(CamoBenchBlockEntity.BUTTON_NEXT, null);
		helper.assertTrue(be.inventory.getStackInSlot(0).getOrDefault(TGComponents.CAMO, 0) == 1, "next camo isn't 1");
		be.buttonClicked(CamoBenchBlockEntity.BUTTON_PREV, null);
		be.buttonClicked(CamoBenchBlockEntity.BUTTON_PREV, null);
		helper.assertTrue(be.inventory.getStackInSlot(0).getOrDefault(TGComponents.CAMO, 0) == 3, "camo doesn't wrap to the last");
		ItemStack wool = CamoBenchBlockEntity.cycle(new ItemStack(Items.WHITE_WOOL, 5), false);
		helper.assertTrue(wool.is(Items.ORANGE_WOOL) && wool.getCount() == 5, "white wool became " + wool);
		helper.succeed();
	}

	/** upgrade_bench/upgrade_protection_2.json: Protection II on a bare helmet for 40 XP. */
	@GameTest(template = "empty")
	public static void upgradeBenchEnchantsArmor(GameTestHelper helper) {
		helper.setBlock(POS, TGBlocks.UPGRADE_BENCH.get());
		UpgradeBenchBlockEntity be = helper.getBlockEntity(POS);
		be.inventory.setStackInSlot(UpgradeBenchBlockEntity.SLOT_ARMOR, new ItemStack(item("t1_combat_helmet")));
		be.inventory.setStackInSlot(UpgradeBenchBlockEntity.SLOT_UPGRADE, new ItemStack(TGItems.UPGRADE_PROTECTION_2.get()));
		var protection = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.PROTECTION);
		ItemStack out = be.output.getItem(0);
		helper.assertTrue(EnchantmentHelper.getItemEnchantmentLevel(protection, out) == 2, "no Protection II: " + out);
		helper.assertTrue(be.cost == 40, "cost " + be.cost);
		helper.succeed();
	}

	/** A mining charge on a 3x3x3 stone cube, facing into it, breaks all of it once its fuse runs out. */
	@GameTest(template = "empty", timeoutTicks = 100)
	public static void chargeBreaksBlocks(GameTestHelper helper) {
		for (BlockPos p : BlockPos.betweenClosed(1, 1, 0, 3, 3, 2)) helper.setBlock(p, Blocks.STONE);
		BlockPos at = new BlockPos(0, 2, 1);
		helper.setBlock(at, TGBlocks.TNT_CHARGE.get().defaultBlockState().setValue(ExplosiveChargeBlock.FACING, Direction.EAST));
		ExplosiveChargeBlockEntity be = helper.getBlockEntity(at);
		be.fuse = 20;
		be.arm();
		helper.runAfterDelay(10, () -> helper.assertBlockPresent(Blocks.STONE, 1, 2, 1));
		helper.succeedWhen(() -> {
			helper.assertBlockNotPresent(TGBlocks.TNT_CHARGE.get(), at);
			for (BlockPos p : BlockPos.betweenClosed(1, 1, 0, 3, 3, 2)) helper.assertBlockPresent(Blocks.AIR, p);
		});
	}

	/** A redstone block next to an outer block opens a door in redstone mode. */
	@GameTest(template = "empty", timeoutTicks = 100)
	public static void doorOpensOnRedstone(GameTestHelper helper) {
		BlockPos master = new BlockPos(1, 2, 1);
		for (BlockPos p : Door3x3Block.parts(master, false)) {
			helper.setBlock(p, TGBlocks.DOOR3X3.get().defaultBlockState().setValue(Door3x3Block.MASTER, p.equals(master)));
		}
		Door3x3BlockEntity door = helper.getBlockEntity(master);
		door.mode = 1;
		door.redstoneMode = 1;
		helper.setBlock(0, 1, 0, Blocks.REDSTONE_BLOCK);
		helper.succeedWhen(() -> {
			helper.assertBlockProperty(master, Door3x3Block.STATE, Door3x3Block.State.OPENED);
			helper.assertBlockProperty(new BlockPos(2, 3, 1), Door3x3Block.STATE, Door3x3Block.State.OPENED);
		});
	}
}
