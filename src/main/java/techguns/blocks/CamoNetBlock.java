package techguns.blocks;

import java.util.Locale;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Camo net (a post with nets towards neighbors) or camo net top (a flat roof). Connects to its own kind. */
public class CamoNetBlock extends Block {
	public static final EnumProperty<Connection> CONNECTION = EnumProperty.create("connection", Connection.class);

	private static final VoxelShape[] NET_SHAPES = new VoxelShape[16];
	private static final VoxelShape[] TOP_SHAPES = new VoxelShape[16];
	static {
		for (int i = 0; i < 16; i++) {
			boolean n = (i & 8) != 0, e = (i & 4) != 0, s = (i & 2) != 0, w = (i & 1) != 0;
			VoxelShape net = box(7, 0, 7, 9, 16, 9);
			if (n) net = Shapes.or(net, box(7, 0, 0, 9, 16, 7));
			if (e) net = Shapes.or(net, box(9, 0, 7, 16, 16, 9));
			if (s) net = Shapes.or(net, box(7, 0, 9, 9, 16, 16));
			if (w) net = Shapes.or(net, box(0, 0, 7, 7, 16, 9));
			NET_SHAPES[i] = net;
			TOP_SHAPES[i] = box(w ? 0 : e ? 7 : 4, 0, n ? 0 : s ? 7 : 4, e ? 16 : w ? 9 : 12, 1, s ? 16 : n ? 9 : 12);
		}
	}

	private final boolean top;

	public CamoNetBlock(boolean top, Properties properties) {
		super(properties);
		this.top = top;
		registerDefaultState(defaultBlockState().setValue(CONNECTION, Connection.NONE));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(CONNECTION);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return connect(defaultBlockState(), context.getLevel(), context.getClickedPos());
	}

	@Override
	protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
		return direction.getAxis().isHorizontal() ? connect(state, level, pos) : state;
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return (top ? TOP_SHAPES : NET_SHAPES)[state.getValue(CONNECTION).ordinal()];
	}

	private BlockState connect(BlockState state, BlockGetter level, BlockPos pos) {
		return state.setValue(CONNECTION, Connection.get(connects(level, pos, Direction.NORTH), connects(level, pos, Direction.EAST),
				connects(level, pos, Direction.SOUTH), connects(level, pos, Direction.WEST)));
	}

	private boolean connects(BlockGetter level, BlockPos pos, Direction direction) {
		return level.getBlockState(pos.relative(direction)).getBlock() instanceof CamoNetBlock other && other.top == top;
	}

	/** Connected sides; ordinal is the bitmask n=8, e=4, s=2, w=1. */
	public enum Connection implements StringRepresentable {
		NONE, W, S, SW, E, EW, ES, ESW, N, NW, NS, NSW, NE, NEW, NES, NESW;

		static Connection get(boolean n, boolean e, boolean s, boolean w) {
			return values()[(n ? 8 : 0) | (e ? 4 : 0) | (s ? 2 : 0) | (w ? 1 : 0)];
		}

		@Override
		public String getSerializedName() {
			return name().toLowerCase(Locale.ROOT);
		}
	}
}
