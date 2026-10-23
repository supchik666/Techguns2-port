package techguns.blocks;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import techguns.TGBlockEntities;

/** Creative energy source (not in 1.12): pushes Integer.MAX_VALUE FE into every side each tick, and gives that much to whatever pulls. */
public class CreativeEnergyBlock extends Block implements EntityBlock {
	/** An endless source: extracts anything, accepts nothing. */
	public static final IEnergyStorage INFINITE = new IEnergyStorage() {
		public int receiveEnergy(int toReceive, boolean simulate) { return 0; }
		public int extractEnergy(int toExtract, boolean simulate) { return toExtract; }
		public int getEnergyStored() { return Integer.MAX_VALUE; }
		public int getMaxEnergyStored() { return Integer.MAX_VALUE; }
		public boolean canExtract() { return true; }
		public boolean canReceive() { return false; }
	};

	public CreativeEnergyBlock(Properties properties) {
		super(properties);
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new BlockEntity(TGBlockEntities.CREATIVE_ENERGY.get(), pos, state) {};
	}

	@Nullable
	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		return level.isClientSide ? null : (l, pos, s, be) -> {
			for (Direction d : Direction.values()) {
				IEnergyStorage target = l.getCapability(Capabilities.EnergyStorage.BLOCK, pos.relative(d), d.getOpposite());
				if (target != null) target.receiveEnergy(Integer.MAX_VALUE, false);
			}
		};
	}
}
