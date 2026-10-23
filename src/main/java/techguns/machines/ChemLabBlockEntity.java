package techguns.machines;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import techguns.TGBlockEntities;
import techguns.TGSounds;
import techguns.recipes.MachineRecipe;

/**
 * 1.12 ChemLabTileEnt: two inputs, a container slot and an input tank, an item output and an output tank. Pipes and
 * buckets fill the input tank and drain the output tank, or the input tank in drain-input mode.
 */
public class ChemLabBlockEntity extends MachineBlockEntity {
	public static final int BUTTON_DUMP_INPUT = 3;
	public static final int BUTTON_DUMP_OUTPUT = 4;
	public static final int BUTTON_TOGGLE_DRAIN = 5;

	public final FluidTank inputTank = tank(8000);
	public final FluidTank outputTank = tank(16000);
	public boolean drainInput;

	public ChemLabBlockEntity(BlockPos pos, BlockState state) {
		super(TGBlockEntities.CHEM_LAB.get(), pos, state, "chemlab", new int[][] {{35, 17}, {57, 17}, {35, 40}, {135, 17}, {135, 60}},
				MachineRecipe.CHEM_LAB, 20000, 1, 2, true);
	}

	private FluidTank tank(int capacity) {
		return new FluidTank(capacity) {
			@Override
			protected void onContentsChanged() {
				contentsChanged = true;
				needUpdate();
			}
		};
	}

	/** Fluid handler capability. */
	public final IFluidHandler fluids = new IFluidHandler() {
		@Override
		public int getTanks() {
			return 2;
		}

		private FluidTank tank(int tank) {
			return tank == 0 ? inputTank : outputTank;
		}

		@Override
		public FluidStack getFluidInTank(int tank) {
			return tank(tank).getFluid();
		}

		@Override
		public int getTankCapacity(int tank) {
			return tank(tank).getCapacity();
		}

		@Override
		public boolean isFluidValid(int tank, FluidStack stack) {
			return tank == 0;
		}

		@Override
		public int fill(FluidStack resource, FluidAction action) {
			return inputTank.fill(resource, action);
		}

		@Override
		public FluidStack drain(FluidStack resource, FluidAction action) {
			return (drainInput ? inputTank : outputTank).drain(resource, action);
		}

		@Override
		public FluidStack drain(int maxDrain, FluidAction action) {
			return (drainInput ? inputTank : outputTank).drain(maxDrain, action);
		}
	};

	@Override
	protected FluidStack inputFluid() {
		return inputTank.getFluid();
	}

	@Override
	protected void drainInput(int amount) {
		inputTank.drain(amount, IFluidHandler.FluidAction.EXECUTE);
	}

	@Override
	protected boolean fillOutput(FluidStack fluid, boolean simulate) {
		return fluid.isEmpty() || outputTank.fill(fluid, simulate ? IFluidHandler.FluidAction.SIMULATE : IFluidHandler.FluidAction.EXECUTE) == fluid.getAmount();
	}

	/** 1.12 multiplied the power by the stack multiplier twice. */
	@Override
	protected int operationPower(MachineRecipe recipe, int mult) {
		return super.operationPower(recipe, mult) * mult;
	}

	@Override
	public void buttonClicked(int id, Player player) {
		switch (id) {
			case BUTTON_DUMP_INPUT -> inputTank.setFluid(FluidStack.EMPTY);
			case BUTTON_DUMP_OUTPUT -> outputTank.setFluid(FluidStack.EMPTY);
			case BUTTON_TOGGLE_DRAIN -> drainInput = !drainInput;
			default -> {
				super.buttonClicked(id, player);
				return;
			}
		}
		contentsChanged = true;
		needUpdate();
	}

	@Override
	protected void ambient() {
		if (progress == 1 || progress == 1 + Math.round(totaltime * 0.5f)) playSound(TGSounds.CHEM_LAB_WORK.get(), 0.65f, 1);
		if (progress % 10 == 0) {
			level.addParticle(ParticleTypes.EFFECT, worldPosition.getX() + 0.5 + level.random.nextFloat(), worldPosition.getY() + 0.5 + level.random.nextFloat(),
					worldPosition.getZ() + 0.5 + level.random.nextFloat(), 0, 1, 0);
		}
	}

	@Override
	protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.saveAdditional(tag, registries);
		tag.put("inputTank", inputTank.writeToNBT(registries, new CompoundTag()));
		tag.put("outputTank", outputTank.writeToNBT(registries, new CompoundTag()));
		tag.putBoolean("drainInput", drainInput);
	}

	@Override
	protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.loadAdditional(tag, registries);
		inputTank.readFromNBT(registries, tag.getCompound("inputTank"));
		outputTank.readFromNBT(registries, tag.getCompound("outputTank"));
		drainInput = tag.getBoolean("drainInput");
	}
}
