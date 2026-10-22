package techguns.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Wall-like block. Corner properties fill the gap where two connected sides also connect to each other. */
public class SandbagsBlock extends Block {
	private static final Direction[] SIDES = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};
	/** Corner i lies between SIDES[i] and SIDES[i + 1]. */
	public static final BooleanProperty[] CORNERS = {BooleanProperty.create("corner_ne"), BooleanProperty.create("corner_es"),
			BooleanProperty.create("corner_sw"), BooleanProperty.create("corner_wn")};

	private static final VoxelShape[] ARMS = {box(4, 0, 0, 12, 16, 4), box(12, 0, 4, 16, 16, 12), box(4, 0, 12, 12, 16, 16), box(0, 0, 4, 4, 16, 12)};
	private static final VoxelShape[] SHAPES = new VoxelShape[16];
	static {
		for (int mask = 0; mask < 16; mask++) {
			VoxelShape shape = box(4, 0, 4, 12, 16, 12);
			for (int i = 0; i < 4; i++) {
				if ((mask & 1 << i) != 0) shape = Shapes.or(shape, ARMS[i]);
			}
			SHAPES[mask] = shape;
		}
	}

	public SandbagsBlock(Properties properties) {
		super(properties);
		BlockState state = stateDefinition.any();
		for (int i = 0; i < 4; i++) {
			state = state.setValue(side(i), false).setValue(CORNERS[i], false);
		}
		registerDefaultState(state);
	}

	private static BooleanProperty side(int i) {
		return PipeBlock.PROPERTY_BY_DIRECTION.get(SIDES[i]);
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		for (int i = 0; i < 4; i++) {
			builder.add(side(i), CORNERS[i]);
		}
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return connect(defaultBlockState(), context.getLevel(), context.getClickedPos());
	}

	// ponytail: corners read diagonal blocks but only refresh on direct neighbor updates, so a corner can go stale
	// when only the diagonal block changes next to a non-sandbags neighbor. Schedule a tick on neighbors if it shows.
	@Override
	protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
		return direction.getAxis().isHorizontal() ? connect(state, level, pos) : state;
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		int mask = 0;
		for (int i = 0; i < 4; i++) {
			if (state.getValue(side(i))) mask |= 1 << i;
		}
		return SHAPES[mask];
	}

	private BlockState connect(BlockState state, BlockGetter level, BlockPos pos) {
		for (int i = 0; i < 4; i++) {
			Direction a = SIDES[i], b = SIDES[(i + 1) % 4];
			boolean corner = connects(level, pos, a) && connects(level, pos, b)
					&& (connects(level, pos.relative(a), b) || connects(level, pos.relative(b), a));
			state = state.setValue(side(i), connects(level, pos, a)).setValue(CORNERS[i], corner);
		}
		return state;
	}

	private static boolean connects(BlockGetter level, BlockPos pos, Direction direction) {
		BlockPos other = pos.relative(direction);
		BlockState state = level.getBlockState(other);
		return state.getBlock() instanceof SandbagsBlock || state.getBlock() instanceof FenceBlock
				|| !isExceptionForConnection(state) && state.isFaceSturdy(level, other, direction.getOpposite());
	}
}
