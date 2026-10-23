package techguns.machines;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import techguns.TGBlockEntities;
import techguns.TGSounds;
import techguns.recipes.MachineRecipe;

/** 1.12 MetalPressTileEnt; with auto split on, a stack alone in one input is split over both if that makes a recipe. */
public class MetalPressBlockEntity extends MachineBlockEntity {
	public static final int BUTTON_AUTOSPLIT = 3;

	public boolean autoSplit;

	public MetalPressBlockEntity(BlockPos pos, BlockState state) {
		super(TGBlockEntities.METAL_PRESS.get(), pos, state, "metalpress", new int[][] {{110, 17}, {130, 17}, {120, 60}, {152, 60}},
				MachineRecipe.METAL_PRESS, 20000, 20, 2, false);
	}

	@Override
	protected void start() {
		if (autoSplit) {
			ItemStack a = inventory.getStackInSlot(0), b = inventory.getStackInSlot(1);
			if (a.getCount() > 1 && b.isEmpty()) split(0, 1);
			else if (b.getCount() > 1 && a.isEmpty()) split(1, 0);
		}
		super.start();
	}

	private void split(int from, int to) {
		ItemStack stack = inventory.getStackInSlot(from);
		ItemStack half = stack.copyWithCount(stack.getCount() / 2);
		MachineRecipe.Input in = new MachineRecipe.Input(List.of(stack.copyWithCount(1), half.copyWithCount(1)), ItemStack.EMPTY,
				FluidStack.EMPTY);
		if (recipes().stream().noneMatch(r -> r.value().match(in, 1) >= 0)) return;
		inventory.setStackInSlot(from, stack.copyWithCount(stack.getCount() - half.getCount()));
		inventory.setStackInSlot(to, half);
	}

	/** 1.12 multiplied the power by the stack multiplier twice. */
	@Override
	protected int operationPower(MachineRecipe recipe, int mult) {
		return super.operationPower(recipe, mult) * mult;
	}

	@Override
	public void buttonClicked(int id, Player player) {
		if (id == BUTTON_AUTOSPLIT) {
			autoSplit = !autoSplit;
			contentsChanged = true;
			needUpdate();
		}
		super.buttonClicked(id, player);
	}

	@Override
	protected void ambient() {
		if (atFraction(0.075f)) playSound(TGSounds.METAL_PRESS_WORK.get(), 0.5f, 1);
	}

	@Override
	protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.saveAdditional(tag, registries);
		tag.putBoolean("autoSplit", autoSplit);
	}

	@Override
	protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.loadAdditional(tag, registries);
		autoSplit = tag.getBoolean("autoSplit");
	}
}
