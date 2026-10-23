package techguns.machines;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import techguns.TGBlockEntities;
import techguns.blocks.Door3x3Block;

/**
 * 1.12 Door3x3TileEntity, on the center block of a 3x3 door: its mode (0 right click, 1 redstone, 2 player sensor), auto
 * close, and when its state last changed, for the animation and the timers in {@link Door3x3Block#tick}.
 */
public class Door3x3BlockEntity extends TGBlockEntity {
	public static final int BUTTON_MODE = 3, BUTTON_AUTOCLOSE = 4;

	public byte mode;
	public boolean autoClose = true;
	/** Game time of the last state change, on each side. */
	public long changeTime;

	public Door3x3BlockEntity(BlockPos pos, BlockState state) {
		super(TGBlockEntities.DOOR3X3.get(), pos, state, "door3x3", new int[0][]);
	}

	@Override
	public boolean playerInventory() {
		return false;
	}

	@Override
	public void buttonClicked(int id, Player player) {
		if (id == BUTTON_MODE) mode = (byte) ((mode + 1) % 3);
		else if (id == BUTTON_AUTOCLOSE) autoClose = !autoClose;
		else {
			super.buttonClicked(id, player);
			return;
		}
		level.scheduleTick(worldPosition, getBlockState().getBlock(), Door3x3Block.UPDATE_DELAY);
		needUpdate();
	}

	/** Notes state changes on both sides; the client learns them from block updates. */
	@SuppressWarnings("deprecation")
	@Override
	public void setBlockState(BlockState state) {
		if (level != null && state.getValue(Door3x3Block.STATE) != getBlockState().getValue(Door3x3Block.STATE)) changeTime = level.getGameTime();
		super.setBlockState(state);
	}

	/** 0 to 1 while opening or closing: how far the wings have moved apart. */
	public float openness(float partialTick) {
		float progress = Math.min((level.getGameTime() - changeTime + partialTick) / Door3x3Block.OPEN_TICKS, 1);
		return switch (getBlockState().getValue(Door3x3Block.STATE)) {
			case OPENING -> progress;
			case CLOSING -> 1 - progress;
			case OPENED -> 1;
			case CLOSED -> 0;
		};
	}

	@Override
	protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.saveAdditional(tag, registries);
		tag.putByte("doormode", mode);
		tag.putBoolean("autoClose", autoClose);
	}

	@Override
	protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.loadAdditional(tag, registries);
		mode = tag.getByte("doormode");
		autoClose = tag.getBoolean("autoClose");
	}
}
