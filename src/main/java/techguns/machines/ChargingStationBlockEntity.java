package techguns.machines;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import techguns.TGBlockEntities;
import techguns.TGEffects;
import techguns.TGSounds;
import techguns.recipes.MachineRecipe;

/**
 * 1.12 ChargingStationTileEnt: charges items with an energy capability (RF powered armor) at 1600 FE/t, moving them to the
 * output when full, and runs charging_station recipes (empty cell to cell) at 800 FE/t.
 */
public class ChargingStationBlockEntity extends MachineBlockEntity {
	public static final int ITEM_CHARGE_RATE = 1600;

	/** Charging an item in the input slot. */
	public boolean charging;

	public ChargingStationBlockEntity(BlockPos pos, BlockState state) {
		super(TGBlockEntities.CHARGING_STATION.get(), pos, state, "chargingstation", new int[][] {{19, 17}, {68, 17}, {152, 60}},
				MachineRecipe.CHARGING_STATION, 100000, 800, 1, false);
	}

	/** What's being charged, for the renderer. */
	public ItemStack charged() {
		if (charging) return inventory.getStackInSlot(0);
		return operation == null || operation.inputs().isEmpty() ? ItemStack.EMPTY : operation.inputs().getFirst();
	}

	@Override
	public void tick() {
		if (level.isClientSide) {
			if (charging) ambient();
			else super.tick();
			return;
		}
		ItemStack stack = inventory.getStackInSlot(0);
		IEnergyStorage item = operation == null && redstoneEnabled() ? stack.getCapability(Capabilities.EnergyStorage.ITEM) : null;
		if (item == null) {
			setCharging(false);
			super.tick();
		} else if (item.receiveEnergy(ITEM_CHARGE_RATE, true) > 0) {
			energy.take(item.receiveEnergy(Math.min(energy.getEnergyStored(), ITEM_CHARGE_RATE), false));
			setChanged();
			setCharging(true);
		} else {
			if (inventory.getStackInSlot(1).isEmpty()) {
				inventory.setStackInSlot(1, stack);
				inventory.setStackInSlot(0, ItemStack.EMPTY);
			}
			setCharging(false);
		}
	}

	private void setCharging(boolean charging) {
		if (charging == this.charging) return;
		this.charging = charging;
		needUpdate();
	}

	@Override
	protected void ambient() {
		int rotation = getBlockState().getValue(MachineBlock.FACING).get2DDataValue();
		double x = worldPosition.getX(), y = worldPosition.getY() + 0.5, z = worldPosition.getZ();
		if (rotation != 2) TGEffects.createFX(level, "ChargeFlare2", x + 0.5, y, z + 0.3, 0, 0, 0);
		if (rotation != 0) TGEffects.createFX(level, "ChargeFlare2", x + 0.5, y, z + 0.7, 0, 0, 0);
		if (rotation != 1) TGEffects.createFX(level, "ChargeFlare2", x + 0.3, y, z + 0.5, 0, 0, 0);
		if (rotation != 3) TGEffects.createFX(level, "ChargeFlare2", x + 0.7, y, z + 0.5, 0, 0, 0);
		if (level.getGameTime() % 20 == 0) playSound(TGSounds.CHARGING_STATION_WORK.get(), 0.35f, 1);
	}

	@Override
	protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.saveAdditional(tag, registries);
		tag.putBoolean("charging", charging);
	}

	@Override
	protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.loadAdditional(tag, registries);
		charging = tag.getBoolean("charging");
	}
}
