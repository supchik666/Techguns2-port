package techguns.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.registries.DeferredBlock;
import techguns.TGBlocks;
import techguns.TGFluids;
import techguns.TGItems;
import techguns.Techguns;
import techguns.machines.ChemLabBlockEntity;
import techguns.machines.MachineBlockEntity;
import techguns.recipes.MachineRecipe;

@GameTestHolder(Techguns.MODID)
@PrefixGameTestTemplate(false)
public class MachineTests {
	private static final BlockPos POS = new BlockPos(1, 1, 1);

	/** Places {@code block} with full power and {@code inputs} in its first slots. */
	private static MachineBlockEntity machine(GameTestHelper helper, DeferredBlock<?> block, ItemStack... inputs) {
		helper.setBlock(POS, block.get());
		MachineBlockEntity be = helper.getBlockEntity(POS);
		be.energy.set(be.energy.getMaxEnergyStored());
		for (int i = 0; i < inputs.length; i++) be.inventory.setStackInSlot(i, inputs[i]);
		return be;
	}

	private static void succeedWhen(GameTestHelper helper, MachineBlockEntity be, int slot, Item item, int count) {
		helper.succeedWhen(() -> {
			ItemStack out = be.inventory.getStackInSlot(slot);
			helper.assertTrue(out.is(item) && out.getCount() == count, "slot " + slot + " holds " + out);
		});
	}

	private static Item item(String name) {
		return BuiltInRegistries.ITEM.get(Techguns.id(name));
	}

	/** metal_press/plateiron.json: two iron ingots make two iron plates in 100 ticks at 20 FE/t. */
	@GameTest(template = "empty", timeoutTicks = 200)
	public static void metalPressMakesPlates(GameTestHelper helper) {
		MachineBlockEntity be = machine(helper, TGBlocks.METAL_PRESS, new ItemStack(Items.IRON_INGOT), new ItemStack(Items.IRON_INGOT));
		succeedWhen(helper, be, 2, item("plateiron"), 2);
	}

	/** chem_lab/radaway.json: nether wart, glistering melon, an infusion bag and 250 mB of acid. */
	@GameTest(template = "empty", timeoutTicks = 200)
	public static void chemLabMakesRadAway(GameTestHelper helper) {
		// the recipe allows swapped inputs
		ChemLabBlockEntity be = (ChemLabBlockEntity) machine(helper, TGBlocks.CHEM_LAB, new ItemStack(Items.GLISTERING_MELON_SLICE),
				new ItemStack(Items.NETHER_WART), new ItemStack(TGItems.INFUSION_BAG.get()));
		be.inputTank.fill(new FluidStack(TGFluids.CREEPER_ACID.get(), 1000), IFluidHandler.FluidAction.EXECUTE);
		helper.succeedWhen(() -> {
			helper.assertTrue(be.inventory.getStackInSlot(3).is(TGItems.RAD_AWAY.get()), "no RadAway: " + be.inventory.getStackInSlot(3));
			helper.assertTrue(be.inputTank.getFluidAmount() == 750, "acid left: " + be.inputTank.getFluidAmount());
		});
	}

	/** chem_lab/radpills.json needs milk; water doesn't start it. */
	@GameTest(template = "empty", timeoutTicks = 200)
	public static void chemLabNeedsTheRightFluid(GameTestHelper helper) {
		ChemLabBlockEntity be = (ChemLabBlockEntity) machine(helper, TGBlocks.CHEM_LAB, new ItemStack(Items.SUGAR, 4),
				new ItemStack(Items.GLISTERING_MELON_SLICE), new ItemStack(Items.GLASS_BOTTLE));
		be.inputTank.fill(new FluidStack(Fluids.WATER, 1000), IFluidHandler.FluidAction.EXECUTE);
		helper.runAfterDelay(5, () -> {
			helper.assertTrue(be.operation == null, "started with water");
			be.inputTank.drain(1000, IFluidHandler.FluidAction.EXECUTE);
			be.inputTank.fill(new FluidStack(NeoForgeMod.MILK.get(), 1000), IFluidHandler.FluidAction.EXECUTE);
			succeedWhen(helper, be, 3, TGItems.RAD_PILLS.get(), 4);
		});
	}

	/** charging_station/energycell.json: 62 ticks at 800 FE/t. */
	@GameTest(template = "empty", timeoutTicks = 200)
	public static void chargingStationChargesCells(GameTestHelper helper) {
		MachineBlockEntity be = machine(helper, TGBlocks.CHARGING_STATION, new ItemStack(TGItems.ENERGY_CELL_EMPTY.get()));
		succeedWhen(helper, be, 1, TGItems.ENERGY_CELL.get(), 1);
	}

	/** Without power nothing happens. */
	@GameTest(template = "empty")
	public static void machinesNeedPower(GameTestHelper helper) {
		MachineBlockEntity be = machine(helper, TGBlocks.METAL_PRESS, new ItemStack(Items.IRON_INGOT), new ItemStack(Items.IRON_INGOT));
		be.energy.set(0);
		helper.runAfterDelay(20, () -> {
			helper.assertTrue(be.progress == 0 && be.inventory.getStackInSlot(2).isEmpty(), "worked without power");
			helper.succeed();
		});
	}

	/** The json recipes load as their machine's recipe type, with their fields. */
	@GameTest(template = "empty")
	public static void machineRecipesLoadFromJson(GameTestHelper helper) {
		var manager = helper.getLevel().getRecipeManager();
		var radpills = manager.byKey(Techguns.id("chem_lab/radpills")).orElseThrow().value();
		helper.assertTrue(radpills instanceof MachineRecipe r && r.kind() == MachineRecipe.CHEM_LAB && r.swap() && r.power() == 20
				&& r.fluid().orElseThrow().amount() == 1000 && r.bottle().isPresent(), "chem_lab/radpills: " + radpills);
		for (var kind : new MachineRecipe.Kind[] {MachineRecipe.AMMO_PRESS, MachineRecipe.METAL_PRESS, MachineRecipe.GRINDER,
				MachineRecipe.BLAST_FURNACE, MachineRecipe.CHARGING_STATION, MachineRecipe.CHEM_LAB}) {
			helper.assertFalse(manager.getAllRecipesFor(kind.type.get()).isEmpty(), "no recipes for " + kind.type.getId());
		}
		helper.succeed();
	}
}
