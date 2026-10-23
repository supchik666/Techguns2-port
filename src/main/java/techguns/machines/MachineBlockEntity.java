package techguns.machines;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.energy.EnergyStorage;
import net.neoforged.neoforge.fluids.FluidStack;
import techguns.TGItems;
import techguns.recipes.MachineRecipe;

/**
 * A powered machine running {@link MachineRecipe}s (1.12 BasicPoweredTileEnt + BasicMachineTileEnt). The inventory is laid
 * out as input slots, the bottle slot if any, output slots, then the upgrade slot. An operation takes its inputs when it
 * starts and gives its outputs after {@code totaltime} powered ticks. The client runs the progress itself while
 * {@link #powered}, for the renderers and sounds.
 */
public abstract class MachineBlockEntity extends TGBlockEntity {
	public final Energy energy;
	public final MachineRecipe.Kind kind;
	public final int inputs;
	public final boolean bottle;
	public final int outputStart, outputs;
	/** FE/t of recipes that set none. */
	public final int defaultPower;
	@Nullable
	public Operation operation;
	public int progress, totaltime;
	/** Working this tick: redstone allows it and the power lasted. */
	public boolean powered;
	/** Look for a recipe on the next idle tick. */
	protected boolean contentsChanged = true;

	/** A running recipe: what it took, what it will give, and its FE/t. */
	public record Operation(List<ItemStack> inputs, List<ItemStack> outputs, FluidStack fluidOutput, int power) {
		static final Codec<Operation> CODEC = RecordCodecBuilder.create(i -> i.group(
				ItemStack.OPTIONAL_CODEC.listOf().fieldOf("inputs").forGetter(Operation::inputs),
				ItemStack.OPTIONAL_CODEC.listOf().fieldOf("outputs").forGetter(Operation::outputs),
				FluidStack.OPTIONAL_CODEC.fieldOf("fluid").forGetter(Operation::fluidOutput),
				Codec.INT.fieldOf("power").forGetter(Operation::power)).apply(i, Operation::new));
	}

	/** Machine power: receives from anything, gives nothing back. */
	public class Energy extends EnergyStorage {
		Energy(int capacity) {
			super(capacity, capacity, 0);
		}

		@Override
		public int receiveEnergy(int toReceive, boolean simulate) {
			int received = super.receiveEnergy(toReceive, simulate);
			if (received > 0 && !simulate) setChanged();
			return received;
		}

		/** Takes all of {@code amount} or nothing. */
		public boolean consume(int amount) {
			if (energy < amount) return false;
			take(amount);
			return true;
		}

		/** Takes up to {@code max}; returns how much. */
		public int take(int max) {
			int taken = Math.min(energy, max);
			energy -= taken;
			if (taken > 0) setChanged();
			return taken;
		}

		public void set(int energy) {
			this.energy = energy;
		}
	}

	protected MachineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, String name, int[][] slots, MachineRecipe.Kind kind,
			int capacity, int defaultPower, int inputs, boolean bottle) {
		super(type, pos, state, name, slots);
		this.kind = kind;
		this.energy = new Energy(capacity);
		this.defaultPower = defaultPower;
		this.inputs = inputs;
		this.bottle = bottle;
		this.outputStart = inputs + (bottle ? 1 : 0);
		this.outputs = slots.length - 1 - outputStart;
	}

	public int upgradeSlot() {
		return inventory.getSlots() - 1;
	}

	@Override
	public boolean isOutput(int slot) {
		return slot >= outputStart && slot < outputStart + outputs;
	}

	@Override
	public boolean canInsert(int slot, ItemStack stack) {
		return slot == upgradeSlot() ? stack.is(TGItems.MACHINE_UPGRADE_STACK) : !isOutput(slot);
	}

	@Override
	protected void contentsChanged(int slot) {
		super.contentsChanged(slot);
		contentsChanged = true;
	}

	/** 1 + the stack upgrades in the upgrade slot. */
	public int multiplier() {
		ItemStack upgrade = inventory.getStackInSlot(upgradeSlot());
		return upgrade.is(TGItems.MACHINE_UPGRADE_STACK) ? 1 + upgrade.getCount() : 1;
	}

	public float getProgress() {
		return totaltime > 0 ? (float) progress / totaltime : 0;
	}

	public boolean isWorking() {
		return operation != null;
	}

	@Override
	public void tick() {
		if (level.isClientSide) {
			if (operation != null && powered && progress < totaltime) {
				progress++;
				ambient();
			}
			return;
		}
		boolean wasPowered = powered;
		powered = false;
		if (redstoneEnabled()) {
			if (operation != null) {
				if (energy.consume(operation.power)) {
					powered = true;
					if (++progress >= totaltime) {
						finish(operation);
						operation = null;
						progress = totaltime = 0;
						start();
						needUpdate();
					}
				}
			} else if (contentsChanged) {
				start();
			}
		}
		if (powered != wasPowered) needUpdate();
	}

	/** Client side, every working tick: sounds and particles (1.12 playAmbientSound). */
	protected void ambient() {}

	/** Whether {@code progress} is at {@code fraction} of the run or half a run later. */
	protected boolean atFraction(float fraction) {
		int tick = Math.round(totaltime * fraction);
		int half = Math.round(totaltime * 0.5f);
		return progress == tick || progress == tick + half;
	}

	protected void playSound(SoundEvent sound, float volume, float pitch) {
		level.playLocalSound(worldPosition, sound, SoundSource.BLOCKS, volume, pitch, false);
	}

	/** The recipes to try, in order. */
	protected List<RecipeHolder<MachineRecipe>> recipes() {
		return level.getRecipeManager().getAllRecipesFor(kind.type.get());
	}

	protected MachineRecipe.Input recipeInput() {
		List<ItemStack> items = new ArrayList<>();
		for (int i = 0; i < inputs; i++) items.add(inventory.getStackInSlot(i));
		return new MachineRecipe.Input(items, bottle ? inventory.getStackInSlot(inputs) : ItemStack.EMPTY, inputFluid());
	}

	protected FluidStack inputFluid() {
		return FluidStack.EMPTY;
	}

	/** Takes {@code amount} from the input tank. */
	protected void drainInput(int amount) {}

	/** Whether the output tank takes {@code fluid} (simulate) or takes it. */
	protected boolean fillOutput(FluidStack fluid, boolean simulate) {
		return fluid.isEmpty();
	}

	/** FE/t of {@code mult} runs of {@code recipe} at once. */
	protected int operationPower(MachineRecipe recipe, int mult) {
		return (recipe.power() > 0 ? recipe.power() : defaultPower) * mult;
	}

	/** Starts the first recipe that matches, as many runs at once as upgrades, inputs and output space allow. */
	protected void start() {
		contentsChanged = false;
		MachineRecipe.Input in = recipeInput();
		for (RecipeHolder<MachineRecipe> holder : recipes()) {
			MachineRecipe recipe = holder.value();
			for (int mult = multiplier(); mult >= 1; mult--) {
				int swapped = recipe.match(in, mult);
				if (swapped < 0) continue;
				List<ItemStack> results = recipe.rollResults(mult, level.random);
				int m = mult;
				FluidStack fluid = recipe.fluidResult().map(f -> f.copyWithAmount(f.getAmount() * m)).orElse(FluidStack.EMPTY);
				if (!output(results, true) || !fillOutput(fluid, true)) continue;
				List<ItemStack> taken = new ArrayList<>();
				for (int i = 0; i < recipe.inputs().size(); i++) {
					int slot = swapped == 1 && i < 2 ? 1 - i : i;
					taken.add(inventory.extractItem(slot, recipe.inputs().get(i).count() * mult, false));
				}
				if (recipe.bottle().isPresent()) taken.add(inventory.extractItem(inputs, recipe.bottle().get().count() * mult, false));
				recipe.fluid().ifPresent(f -> drainInput(f.amount() * m));
				operation = new Operation(taken, results, fluid, operationPower(recipe, mult));
				totaltime = recipe.time();
				progress = 0;
				needUpdate();
				return;
			}
		}
	}

	protected void finish(Operation operation) {
		output(operation.outputs, false);
		fillOutput(operation.fluidOutput, false);
	}

	/** Merges {@code stacks} into the output slots; false (and no change) if they don't all fit. */
	protected boolean output(List<ItemStack> stacks, boolean simulate) {
		ItemStack[] out = new ItemStack[outputs];
		for (int i = 0; i < outputs; i++) out[i] = inventory.getStackInSlot(outputStart + i).copy();
		for (ItemStack stack : stacks) {
			ItemStack left = stack.copy();
			for (int i = 0; i < outputs && !left.isEmpty(); i++) {
				if (out[i].isEmpty()) {
					out[i] = left.split(left.getMaxStackSize());
				} else if (ItemStack.isSameItemSameComponents(out[i], left)) {
					int n = Math.min(left.getCount(), out[i].getMaxStackSize() - out[i].getCount());
					out[i].grow(n);
					left.shrink(n);
				}
			}
			if (!left.isEmpty()) return false;
		}
		if (!simulate) {
			for (int i = 0; i < outputs; i++) inventory.setStackInSlot(outputStart + i, out[i]);
		}
		return true;
	}

	@Override
	public List<ItemStack> drops() {
		List<ItemStack> drops = super.drops();
		if (operation != null) drops.addAll(operation.inputs);
		return drops;
	}

	/** Energy (in two shorts), progress, total time. */
	@Override
	public ContainerData menuData() {
		return new ContainerData() {
			@Override
			public int get(int index) {
				return switch (index) {
					case 0 -> energy.getEnergyStored() & 0xFFFF;
					case 1 -> energy.getEnergyStored() >>> 16;
					case 2 -> progress;
					default -> totaltime;
				};
			}

			@Override
			public void set(int index, int value) {}

			@Override
			public int getCount() {
				return 4;
			}
		};
	}

	@Override
	protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.saveAdditional(tag, registries);
		tag.putInt("energy", energy.getEnergyStored());
		tag.putInt("progress", progress);
		tag.putInt("totaltime", totaltime);
		tag.putBoolean("powered", powered);
		if (operation != null) tag.put("operation", Operation.CODEC.encodeStart(registries.createSerializationContext(NbtOps.INSTANCE), operation).getOrThrow());
	}

	@Override
	protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.loadAdditional(tag, registries);
		energy.set(tag.getInt("energy"));
		progress = tag.getInt("progress");
		totaltime = tag.getInt("totaltime");
		powered = tag.getBoolean("powered");
		operation = tag.contains("operation")
				? Operation.CODEC.parse(registries.createSerializationContext(NbtOps.INSTANCE), tag.get("operation")).result().orElse(null) : null;
	}
}
