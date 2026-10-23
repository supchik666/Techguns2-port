package techguns.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;
import techguns.TGBlocks;
import techguns.TGItems;
import techguns.Techguns;
import techguns.machines.multiblock.FabricatorBlockEntity;
import techguns.machines.multiblock.MultiBlockPart;
import techguns.machines.multiblock.OreDrillBlockEntity;
import techguns.machines.multiblock.ReactionChamberBlockEntity;

/** Forming, unforming and running the multiblock machines; {@code empty8} is an 8x8x8 air template. */
@GameTestHolder(Techguns.MODID)
@PrefixGameTestTemplate(false)
public class MultiBlockTests {
	private static final BlockPos FABRICATOR = new BlockPos(1, 1, 1);

	/** A fabricator at {@link #FABRICATOR}, clicked on its north side: housings behind and to the east, glass on top. */
	private static FabricatorBlockEntity fabricator(GameTestHelper helper, Player player) {
		helper.setBlock(FABRICATOR, TGBlocks.FABRICATOR_CONTROLLER.get());
		for (BlockPos p : new BlockPos[] {new BlockPos(1, 1, 2), new BlockPos(2, 1, 1), new BlockPos(2, 1, 2)}) {
			helper.setBlock(p, TGBlocks.FABRICATOR_HOUSING.get());
		}
		for (int x = 1; x <= 2; x++) for (int z = 1; z <= 2; z++) helper.setBlock(new BlockPos(x, 2, z), TGBlocks.FABRICATOR_GLASS.get());
		FabricatorBlockEntity be = helper.getBlockEntity(FABRICATOR);
		be.tryForm(player, Direction.NORTH);
		return be;
	}

	/** fabricator/energycellempty.json, with the inputs and power going in through a housing. */
	@GameTest(template = "empty8", timeoutTicks = 200)
	public static void fabricatorFormsAndCrafts(GameTestHelper helper) {
		FabricatorBlockEntity be = fabricator(helper, helper.makeMockPlayer(GameType.SURVIVAL));
		helper.assertTrue(be.formed, "fabricator didn't form");
		helper.assertBlockProperty(new BlockPos(2, 2, 2), MultiBlockPart.FORMED, true);
		BlockPos housing = helper.absolutePos(new BlockPos(2, 1, 2));
		IItemHandler items = helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, housing, Direction.UP);
		helper.assertTrue(items != null, "housing has no item handler");
		helper.getLevel().getCapability(Capabilities.EnergyStorage.BLOCK, housing, Direction.UP).receiveEnergy(100000, false);
		ItemStack[] inputs = {new ItemStack(Items.GOLD_INGOT), new ItemStack(TGItems.WIRE_COPPER.get()), new ItemStack(Items.REDSTONE, 3),
				new ItemStack(TGItems.PLASTIC_SHEET.get())};
		for (int i = 0; i < inputs.length; i++) helper.assertTrue(items.insertItem(i, inputs[i], false).isEmpty(), "slot " + i + " refused " + inputs[i]);
		helper.succeedWhen(() -> helper.assertTrue(be.inventory.getStackInSlot(4).is(TGItems.ENERGY_CELL_EMPTY.get()), "no cell: " + be.inventory.getStackInSlot(4)));
	}

	@GameTest(template = "empty8")
	public static void breakingAPartUnforms(GameTestHelper helper) {
		FabricatorBlockEntity be = fabricator(helper, helper.makeMockPlayer(GameType.SURVIVAL));
		helper.assertTrue(be.formed, "fabricator didn't form");
		helper.destroyBlock(new BlockPos(1, 2, 2));
		helper.assertFalse(be.formed, "still formed");
		helper.assertBlockProperty(FABRICATOR, MultiBlockPart.FORMED, false);
		helper.assertBlockProperty(new BlockPos(2, 1, 2), MultiBlockPart.FORMED, false);
		// a part missing: it doesn't form again
		be.tryForm(helper.makeMockPlayer(GameType.SURVIVAL), Direction.NORTH);
		helper.assertFalse(be.formed, "formed without the glass");
		helper.succeed();
	}

	/**
	 * reaction_chamber/uv_wheat.json: wheat seeds under a UV emitter in a bucket of water at intensity 3, 5 good reaction
	 * ticks out of 10. The test follows the drifting intensity like a player would.
	 */
	@GameTest(template = "empty8", timeoutTicks = 500)
	public static void reactionChamberRunsAReaction(GameTestHelper helper) {
		BlockPos controller = new BlockPos(3, 1, 1);
		for (int x = 2; x <= 4; x++) for (int y = 1; y <= 4; y++) for (int z = 1; z <= 3; z++) {
			helper.setBlock(x, y, z, y == 1 || y == 4 ? TGBlocks.REACTION_CHAMBER_HOUSING.get() : TGBlocks.REACTION_CHAMBER_GLASS.get());
		}
		helper.setBlock(controller, TGBlocks.REACTION_CHAMBER_CONTROLLER.get());
		ReactionChamberBlockEntity be = helper.getBlockEntity(controller);
		be.tryForm(helper.makeMockPlayer(GameType.SURVIVAL), Direction.NORTH);
		helper.assertTrue(be.formed, "chamber didn't form");
		helper.assertTrue(helper.getLevel().getCapability(Capabilities.EnergyStorage.BLOCK, helper.absolutePos(new BlockPos(3, 4, 2)), null) != null,
				"top center doesn't take power");
		be.energy.set(be.energy.getMaxEnergyStored());
		be.liquidLevel = 1;
		be.intensity = 3;
		IFluidHandler fluids = helper.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, helper.absolutePos(new BlockPos(3, 1, 3)), null);
		helper.assertTrue(fluids.fill(new FluidStack(Fluids.WATER, 5000), IFluidHandler.FluidAction.EXECUTE) == 1000, "tank isn't capped at the level");
		be.inventory.setStackInSlot(ReactionChamberBlockEntity.SLOT_FOCUS, new ItemStack(TGItems.RC_UV_EMITTER.get()));
		be.inventory.setStackInSlot(ReactionChamberBlockEntity.SLOT_INPUT, new ItemStack(Items.WHEAT_SEEDS));
		helper.onEachTick(() -> {
			if (be.reaction != null) be.intensity = be.requiredIntensity;
		});
		helper.succeedWhen(() -> {
			helper.assertTrue(be.inventory.getStackInSlot(2).is(Items.WHEAT), "no wheat: " + be.inventory.getStackInSlot(2) + ", completion " + be.completion);
			helper.assertTrue(be.tank.isEmpty(), "water left: " + be.tank.getFluidAmount());
		});
	}

	/** The smallest drill: controller, rod, coal cluster. Runs on the coal in its fuel slot. */
	@GameTest(template = "empty8", timeoutTicks = 100)
	public static void miniOreDrillMines(GameTestHelper helper) {
		BlockPos controller = new BlockPos(1, 1, 1);
		helper.setBlock(controller, TGBlocks.ORE_DRILL_CONTROLLER.get());
		helper.setBlock(new BlockPos(1, 1, 2), TGBlocks.ORE_DRILL_ROD.get());
		helper.setBlock(new BlockPos(1, 1, 3), TGBlocks.ORE_CLUSTER_COAL.get());
		OreDrillBlockEntity be = helper.getBlockEntity(controller);
		be.inventory.setStackInSlot(OreDrillBlockEntity.SLOT_DRILL, new ItemStack(TGItems.OREDRILLHEAD_STEEL.get()));
		be.inventory.setStackInSlot(OreDrillBlockEntity.SLOT_FUEL, new ItemStack(Items.COAL));
		be.tryForm(helper.makeMockPlayer(GameType.SURVIVAL), Direction.UP);
		helper.assertTrue(be.formed && be.rods == 1 && be.engines == 0, "didn't form: rods " + be.rods);
		helper.runAfterDelay(2, () -> {
			// cluster size 1, head level 1: (3 + 0.5) * 10 ores an hour, 8 * 35 * 0.1 FE/t
			helper.assertTrue(be.operation != null && be.totaltime == 2057 && be.operation.power() == 28,
					"operation " + be.operation + " in " + be.totaltime);
			be.progress = be.totaltime - 3;
			helper.succeedWhen(() -> {
				ItemStack out = be.inventory.getStackInSlot(2);
				helper.assertTrue(out.is(Items.COAL_ORE) || out.is(Items.DIAMOND), "mined " + out);
				helper.assertTrue(be.inventory.getStackInSlot(OreDrillBlockEntity.SLOT_FUEL).isEmpty() && be.fuelBuffer > 0, "didn't burn the coal");
				helper.assertTrue(be.energy.getEnergyStored() == 0, "used FE");
			});
		});
	}

	/** One engine, three rods, a frame and scaffolds around them, drilling south into a cluster. */
	@GameTest(template = "empty8")
	public static void oreDrillFormsWithFrame(GameTestHelper helper) {
		BlockPos controller = new BlockPos(3, 2, 0);
		helper.setBlock(controller, TGBlocks.ORE_DRILL_CONTROLLER.get());
		for (int z = 1; z <= 4; z++) for (int x = 2; x <= 4; x++) for (int y = 1; y <= 3; y++) {
			Block block;
			if (x == 3 && y == 2) block = z == 1 ? TGBlocks.ORE_DRILL_ENGINE.get() : TGBlocks.ORE_DRILL_ROD.get();
			else if (z == 1 || z == 4 || (x != 3 && y != 2)) block = TGBlocks.ORE_DRILL_FRAME.get();
			else block = TGBlocks.ORE_DRILL_SCAFFOLD.get();
			helper.setBlock(x, y, z, block);
		}
		helper.setBlock(new BlockPos(3, 2, 5), TGBlocks.ORE_CLUSTER_COMMON_METAL.get());
		helper.setBlock(new BlockPos(3, 3, 2), TGBlocks.ORE_DRILL_FRAME.get());
		OreDrillBlockEntity be = helper.getBlockEntity(controller);
		be.tryForm(helper.makeMockPlayer(GameType.SURVIVAL), Direction.UP);
		helper.assertFalse(be.formed, "formed with a frame where a scaffold goes");
		helper.setBlock(new BlockPos(3, 3, 2), TGBlocks.ORE_DRILL_SCAFFOLD.get());
		be.tryForm(helper.makeMockPlayer(GameType.SURVIVAL), Direction.UP);
		helper.assertTrue(be.formed && be.engines == 1 && be.rods == 3 && be.radius == 0 && be.drillDirection == Direction.SOUTH,
				"shape " + be.engines + "/" + be.rods + "/" + be.radius + " " + be.drillDirection);
		helper.assertBlockProperty(new BlockPos(3, 3, 2), MultiBlockPart.FORMED, true);
		helper.assertBlockProperty(new BlockPos(2, 1, 4), MultiBlockPart.FORMED, true);
		helper.succeed();
	}
}
