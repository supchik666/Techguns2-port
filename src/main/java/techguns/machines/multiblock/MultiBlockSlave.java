package techguns.machines.multiblock;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A part of a formed multiblock (1.12 MultiBlockMachineTileEntSlave): knows its controller and its role there,
 * {@link #type} (0 when unformed, else per machine, e.g. the reaction chamber's connectors).
 */
public class MultiBlockSlave extends BlockEntity {
	@Nullable
	private BlockPos master;
	public int type;

	public MultiBlockSlave(BlockPos pos, BlockState state) {
		super(MultiBlocks.PART.get(), pos, state);
	}

	/** The formed controller this is part of. */
	@Nullable
	public MultiBlockMachine master() {
		return master != null && level != null && level.getBlockEntity(master) instanceof MultiBlockMachine m && m.formed ? m : null;
	}

	public boolean isPartOf(BlockPos controller) {
		return controller.equals(master);
	}

	void link(BlockPos master, int type) {
		this.master = master;
		this.type = type;
		setFormed(true);
	}

	void unlink() {
		master = null;
		type = 0;
		setFormed(false);
	}

	private void setFormed(boolean formed) {
		setChanged();
		BlockState state = level.getBlockState(worldPosition);
		if (state.getBlock() instanceof MultiBlockPart) level.setBlock(worldPosition, state.setValue(MultiBlockPart.FORMED, formed), 3);
		level.invalidateCapabilities(worldPosition);
	}

	@Override
	protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.saveAdditional(tag, registries);
		if (master != null) tag.put("master", NbtUtils.writeBlockPos(master));
		tag.putInt("type", type);
	}

	@Override
	protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.loadAdditional(tag, registries);
		master = NbtUtils.readBlockPos(tag, "master").orElse(null);
		type = tag.getInt("type");
	}
}
