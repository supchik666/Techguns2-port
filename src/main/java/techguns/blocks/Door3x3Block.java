package techguns.blocks;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import techguns.TGSounds;
import techguns.machines.Door3x3BlockEntity;

/**
 * 1.12 BlockTGDoor3x3, the metal type: a 3x3 door of nine blocks around a master in the middle, which holds the
 * {@link Door3x3BlockEntity}. Its wings slide apart sideways over {@link #OPEN_TICKS}, drawn by the client's block entity
 * renderer; right click, redstone or nearby players open it, by its mode. Sneak-use with an empty hand opens its GUI.
 * ponytail: the hangar door types (OBJ parts, linked neighbours) and the nether door aren't ported.
 */
public class Door3x3Block extends BaseEntityBlock {
	public static final BooleanProperty MASTER = BooleanProperty.create("master");
	/** The door spans the Z axis (placed looking east or west). */
	public static final BooleanProperty ZPLANE = BooleanProperty.create("zplane");
	public static final EnumProperty<State> STATE = EnumProperty.create("state", State.class);
	public static final MapCodec<Door3x3Block> CODEC = simpleCodec(Door3x3Block::new);

	/** 1.12 BLOCK_UPDATE_DELAY, DOOR_OPEN_TIME (1 s) and DOOR_AUTOCLOSE_DELAY (5 s), in ticks. */
	public static final int UPDATE_DELAY = 5, OPEN_TICKS = 20, AUTOCLOSE_TICKS = 100;

	private static final VoxelShape CLOSED_X = box(0, 0, 6, 16, 16, 10), CLOSED_Z = box(6, 0, 0, 10, 16, 16);
	private static final VoxelShape TOP_X = box(0, 12, 6, 16, 16, 10), TOP_Z = box(6, 12, 0, 10, 16, 16);
	private static final VoxelShape LEFT_X = box(0, 0, 6, 4, 16, 10), RIGHT_X = box(12, 0, 6, 16, 16, 10);
	private static final VoxelShape LEFT_Z = box(6, 0, 0, 10, 16, 4), RIGHT_Z = box(6, 0, 12, 10, 16, 16);

	public enum State implements StringRepresentable {
		CLOSED, OPENED, OPENING, CLOSING;

		@Override
		public String getSerializedName() {
			return name().toLowerCase();
		}

		State next() {
			return switch (this) {
				case CLOSED -> OPENING;
				case OPENED -> CLOSING;
				case OPENING -> OPENED;
				case CLOSING -> CLOSED;
			};
		}

		public boolean open() {
			return this == OPENED || this == OPENING;
		}
	}

	public Door3x3Block(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(MASTER, false).setValue(ZPLANE, false).setValue(STATE, State.CLOSED));
	}

	@Override
	protected MapCodec<? extends BaseEntityBlock> codec() {
		return CODEC;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(MASTER, ZPLANE, STATE);
	}

	@Override
	protected RenderShape getRenderShape(BlockState state) {
		return RenderShape.MODEL;
	}

	@Nullable
	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return state.getValue(MASTER) ? new Door3x3BlockEntity(pos, state) : null;
	}

	/** The nine blocks of the door around {@code master}, master last. */
	public static List<BlockPos> parts(BlockPos master, boolean zplane) {
		List<BlockPos> parts = new ArrayList<>();
		for (int y = -1; y <= 1; y++) {
			for (int side = -1; side <= 1; side++) {
				if (y != 0 || side != 0) parts.add(zplane ? master.offset(0, y, side) : master.offset(side, y, 0));
			}
		}
		parts.add(master);
		return parts;
	}

	public BlockPos findMaster(BlockGetter level, BlockPos pos, BlockState state) {
		if (state.getValue(MASTER)) return pos;
		for (BlockPos p : parts(pos, state.getValue(ZPLANE))) {
			BlockState s = level.getBlockState(p);
			if (s.is(this) && s.getValue(MASTER)) return p;
		}
		return pos;
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		boolean z = state.getValue(ZPLANE);
		if (!state.getValue(STATE).open()) return z ? CLOSED_Z : CLOSED_X;
		// open: the frame's top and sides
		BlockPos master = findMaster(level, pos, state);
		if (pos.getY() > master.getY()) return z ? TOP_Z : TOP_X;
		int side = z ? pos.getZ() - master.getZ() : pos.getX() - master.getX();
		if (side == 0) return Shapes.empty();
		return side < 0 ? (z ? LEFT_Z : LEFT_X) : (z ? RIGHT_Z : RIGHT_X);
	}

	@Override
	protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		if (state.getValue(STATE).open()) return Shapes.empty();
		return state.getValue(ZPLANE) ? CLOSED_Z : CLOSED_X;
	}

	@Override
	protected boolean isPathfindable(BlockState state, PathComputationType type) {
		return state.getValue(STATE).open();
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		BlockPos master = findMaster(level, pos, state);
		if (level.getBlockEntity(master) instanceof Door3x3BlockEntity door && door.canUse(player)) {
			// ponytail: 1.12 wanted a wrench in hand; the port has none, so it's sneak-use with an empty hand
			if (player.isShiftKeyDown()) ExplosiveChargeBlock.open(door, player);
			else if (door.mode == 0 && !level.isClientSide) toggle(level, master);
		}
		return InteractionResult.sidedSuccess(level.isClientSide);
	}

	/** Moves the whole door on to its next state. */
	public void toggle(Level level, BlockPos master) {
		BlockState masterState = level.getBlockState(master);
		State next = masterState.getValue(STATE).next();
		for (BlockPos p : parts(master, masterState.getValue(ZPLANE))) {
			BlockState s = level.getBlockState(p);
			if (s.is(this)) level.setBlock(p, s.setValue(STATE, next), 3);
		}
		SoundEvent sound = switch (next) {
			case OPENING -> TGSounds.TECHDOOR_OPEN.get();
			case CLOSING -> TGSounds.TECHDOOR_CLOSE.get();
			default -> TGSounds.TECHDOOR_STATE_FINISHED.get();
		};
		level.playSound(null, master, sound, SoundSource.BLOCKS, 0.9f + level.random.nextFloat() * 0.2f, 0.9f + level.random.nextFloat() * 0.2f);
		level.gameEvent(null, next.open() ? GameEvent.BLOCK_OPEN : GameEvent.BLOCK_CLOSE, master);
		if (next == State.OPENING || next == State.CLOSING) level.scheduleTick(master, this, 1);
		else if (next == State.OPENED && level.getBlockEntity(master) instanceof Door3x3BlockEntity door && door.autoClose) level.scheduleTick(master, this, UPDATE_DELAY);
	}

	/** 1.12 updateTick on the master: finishes moving, auto closes, and polls the player sensor. */
	@Override
	protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		if (!state.is(this) || !state.getValue(MASTER) || !(level.getBlockEntity(pos) instanceof Door3x3BlockEntity door)) return;
		State current = state.getValue(STATE);
		long since = level.getGameTime() - door.changeTime;
		if (current == State.OPENING || current == State.CLOSING) {
			level.scheduleTick(pos, this, 1);
			if (since >= OPEN_TICKS) toggle(level, pos);
		} else if (door.mode == 0) {
			if (door.autoClose && current == State.OPENED) {
				if (since > AUTOCLOSE_TICKS) toggle(level, pos);
				level.scheduleTick(pos, this, UPDATE_DELAY);
			}
		} else if (door.mode == 2) {
			boolean near = level.getEntitiesOfClass(Player.class, new AABB(pos).inflate(2, 1, 2), door::canUse).stream().anyMatch(p -> !p.isSpectator());
			if (near && current == State.CLOSED) toggle(level, pos);
			else if (!near && current == State.OPENED && since > OPEN_TICKS) toggle(level, pos);
			level.scheduleTick(pos, this, UPDATE_DELAY);
		}
	}

	/** Redstone mode: any signal at the eight outer blocks opens the door (redstone setting 1) or closes it (2). */
	@Override
	protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighbor, BlockPos neighborPos, boolean movedByPiston) {
		if (level.isClientSide || state.getValue(MASTER)) return;
		BlockPos master = findMaster(level, pos, state);
		if (!(level.getBlockEntity(master) instanceof Door3x3BlockEntity door)) return;
		boolean powered = parts(master, state.getValue(ZPLANE)).stream().filter(p -> !p.equals(master)).anyMatch(level::hasNeighborSignal);
		if (powered != door.hasSignal) {
			door.hasSignal = powered;
			door.needUpdate();
		}
		if (door.mode != 1 || door.redstoneMode == 0) return;
		State current = level.getBlockState(master).getValue(STATE);
		boolean open = powered == (door.redstoneMode == 1);
		if (open && current == State.CLOSED || !open && current == State.OPENED) toggle(level, master);
	}

	/** Breaking any block takes the whole door; the broken one drops it. */
	@Override
	protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
		if (!state.is(newState.getBlock())) {
			for (BlockPos p : parts(pos, state.getValue(ZPLANE))) {
				BlockState s = level.getBlockState(p);
				// the master takes its parts, a part takes the master (which takes the rest)
				if (s.is(this) && (state.getValue(MASTER) || s.getValue(MASTER))) level.removeBlock(p, false);
			}
		}
		super.onRemove(state, level, pos, newState, movedByPiston);
	}

	/** 1.12 ItemTGDoor3x3: places the door on top of the clicked block, across the player's view, the master in the middle. */
	public static class Placer extends BlockItem {
		public Placer(Block block, Properties properties) {
			super(block, properties);
		}

		@Nullable
		@Override
		protected BlockState getPlacementState(BlockPlaceContext context) {
			return context.getClickedFace() == Direction.UP ? getBlock().defaultBlockState() : null;
		}

		@Override
		protected boolean placeBlock(BlockPlaceContext context, BlockState state) {
			Level level = context.getLevel();
			boolean zplane = context.getHorizontalDirection().getAxis() == Direction.Axis.X;
			BlockPos master = context.getClickedPos().above();
			List<BlockPos> parts = parts(master, zplane);
			for (BlockPos p : parts) {
				if (!level.getBlockState(p).canBeReplaced(context) || !level.isInWorldBounds(p)) return false;
			}
			for (BlockPos p : parts) {
				level.setBlock(p, state.setValue(ZPLANE, zplane).setValue(MASTER, p.equals(master)), 3);
			}
			if (context.getPlayer() != null && level.getBlockEntity(master) instanceof Door3x3BlockEntity door) door.owner = context.getPlayer().getUUID();
			return true;
		}
	}
}
