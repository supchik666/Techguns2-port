package techguns.machines;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import techguns.TGBlockEntities;

/** The turret base (1.12 BasicMachine turret): placed against a block's underside, its turret hangs below it. */
public class TurretBlock extends MachineBlock {
	public static final BooleanProperty HANGING = BooleanProperty.create("hanging");
	public static final MapCodec<TurretBlock> CODEC = simpleCodec(TurretBlock::new);

	public TurretBlock(Properties properties) {
		super(() -> TGBlockEntities.TURRET.get(), properties);
		registerDefaultState(defaultBlockState().setValue(HANGING, false));
	}

	@Override
	protected MapCodec<? extends BaseEntityBlock> codec() {
		return CODEC;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder);
		builder.add(HANGING);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return super.getStateForPlacement(context).setValue(HANGING, context.getClickedFace() == Direction.DOWN);
	}

	@Override
	protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
		if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof TurretBlockEntity be) be.removeTurret();
		super.onRemove(state, level, pos, newState, movedByPiston);
	}
}
