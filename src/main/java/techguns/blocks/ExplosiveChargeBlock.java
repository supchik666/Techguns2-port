package techguns.blocks;

import javax.annotation.Nullable;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import techguns.TGBlockEntities;
import techguns.machines.ExplosiveChargeBlockEntity;
import techguns.machines.TGBlockEntity;

/**
 * 1.12 BlockExplosiveCharge: a mining charge stuck to the block it was placed on, facing into it. Its GUI sets radius,
 * length and fuse and arms it; it falls off when that block goes.
 */
public class ExplosiveChargeBlock extends BaseEntityBlock {
	public static final DirectionProperty FACING = DirectionalBlock.FACING;
	public static final BooleanProperty ARMED = BooleanProperty.create("armed");
	/** By facing (down, up, north, south, west, east): 5 px thick against the block it's on. */
	private static final VoxelShape[] SHAPES = {box(2, 0, 2, 14, 5, 14), box(2, 11, 2, 14, 16, 14), box(2, 2, 0, 14, 14, 5),
			box(2, 2, 11, 14, 14, 16), box(0, 2, 2, 5, 14, 14), box(11, 2, 2, 16, 14, 14)};

	public final boolean advanced;
	private final MapCodec<ExplosiveChargeBlock> codec;

	public ExplosiveChargeBlock(boolean advanced, Properties properties) {
		super(properties);
		this.advanced = advanced;
		this.codec = simpleCodec(p -> new ExplosiveChargeBlock(advanced, p));
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.DOWN).setValue(ARMED, false));
	}

	@Override
	protected MapCodec<? extends BaseEntityBlock> codec() {
		return codec;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, ARMED);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return defaultBlockState().setValue(FACING, context.getClickedFace().getOpposite());
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPES[state.getValue(FACING).get3DDataValue()];
	}

	@Override
	protected RenderShape getRenderShape(BlockState state) {
		return RenderShape.MODEL;
	}

	@Nullable
	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new ExplosiveChargeBlockEntity(pos, state);
	}

	@Nullable
	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		return level.isClientSide ? null : createTickerHelper(type, TGBlockEntities.EXPLOSIVE_CHARGE.get(), (l, p, s, be) -> be.tick());
	}

	@Override
	public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
		if (placer instanceof Player player && level.getBlockEntity(pos) instanceof TGBlockEntity be) be.owner = player.getUUID();
	}

	@Override
	protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighbor, BlockPos neighborPos, boolean movedByPiston) {
		if (!level.isClientSide && level.isEmptyBlock(pos.relative(state.getValue(FACING)))) level.destroyBlock(pos, true);
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!state.getValue(ARMED) && level.getBlockEntity(pos) instanceof TGBlockEntity be) open(be, player);
		return InteractionResult.sidedSuccess(level.isClientSide);
	}

	/** Opens {@code be}'s menu for players who may use it, as {@link techguns.machines.MachineBlock} does. */
	static void open(TGBlockEntity be, Player player) {
		if (player.level().isClientSide) return;
		if (be.canUse(player)) {
			player.openMenu(be, be.getBlockPos());
		} else {
			player.displayClientMessage(Component.translatable("techguns.container.security.denied").append(" ").append(be.getDisplayName()), true);
		}
	}
}
