package techguns.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Lantern with a bracket towards every face it can hang from. Survives while at least one face holds it. */
public class WallLanternBlock extends Block {
	private static final VoxelShape SHAPE = box(4, 2, 4, 12, 14, 12);

	public WallLanternBlock(Properties properties) {
		super(properties);
		BlockState state = defaultBlockState();
		for (Direction d : Direction.values()) {
			state = state.setValue(PipeBlock.PROPERTY_BY_DIRECTION.get(d), false);
		}
		registerDefaultState(state);
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		PipeBlock.PROPERTY_BY_DIRECTION.values().forEach(builder::add);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		BlockState state = defaultBlockState();
		for (Direction d : Direction.values()) {
			state = state.setValue(PipeBlock.PROPERTY_BY_DIRECTION.get(d), attaches(context.getLevel(), context.getClickedPos(), d));
		}
		return state.canSurvive(context.getLevel(), context.getClickedPos()) ? state : null;
	}

	@Override
	protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
		for (Direction d : Direction.values()) {
			if (attaches(level, pos, d)) return true;
		}
		return false;
	}

	@Override
	protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
		if (!state.canSurvive(level, pos)) return Blocks.AIR.defaultBlockState();
		return state.setValue(PipeBlock.PROPERTY_BY_DIRECTION.get(direction), attaches(level, pos, direction));
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}

	private static boolean attaches(LevelReader level, BlockPos pos, Direction direction) {
		return canSupportCenter(level, pos.relative(direction), direction.getOpposite());
	}
}
