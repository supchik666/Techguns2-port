package techguns.machines;

import java.util.function.Supplier;

import javax.annotation.Nullable;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.fluids.FluidUtil;

/**
 * A block holding a {@link TGBlockEntity} (1.12 BasicMachine/SimpleMachine): faces away from the placer, opens the GUI for
 * players {@link TGBlockEntity#canUse may use it}, fills and empties fluid containers held against it, and drops its contents.
 */
public class MachineBlock extends BaseEntityBlock {
	public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

	private final Supplier<? extends BlockEntityType<? extends TGBlockEntity>> type;
	private final MapCodec<MachineBlock> codec;

	public MachineBlock(Supplier<? extends BlockEntityType<? extends TGBlockEntity>> type, Properties properties) {
		super(properties);
		this.type = type;
		this.codec = simpleCodec(p -> new MachineBlock(type, p));
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
	}

	@Override
	protected MapCodec<? extends BaseEntityBlock> codec() {
		return codec;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
	}

	@Override
	protected RenderShape getRenderShape(BlockState state) {
		return RenderShape.MODEL;
	}

	@Nullable
	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return type.get().create(pos, state);
	}

	@Nullable
	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
		return blockEntityType == type.get() ? (l, p, s, be) -> ((TGBlockEntity) be).tick() : null;
	}

	@Override
	public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
		if (level.getBlockEntity(pos) instanceof TGBlockEntity be) {
			if (placer instanceof Player player) be.owner = player.getUUID();
			if (!level.isClientSide) be.updateSignal();
		}
	}

	@Override
	protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighbor, BlockPos neighborPos, boolean movedByPiston) {
		if (!level.isClientSide && level.getBlockEntity(pos) instanceof TGBlockEntity be) be.updateSignal();
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!level.isClientSide && level.getBlockEntity(pos) instanceof TGBlockEntity be) {
			if (be.canUse(player)) {
				player.openMenu(be, pos);
			} else {
				player.displayClientMessage(Component.translatable("techguns.container.security.denied").append(" ").append(be.getDisplayName()), true);
			}
		}
		return InteractionResult.sidedSuccess(level.isClientSide);
	}

	@Override
	protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
			BlockHitResult hit) {
		if (FluidUtil.interactWithFluidHandler(player, hand, level, pos, hit.getDirection())) return ItemInteractionResult.sidedSuccess(level.isClientSide);
		return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
	}

	@Override
	protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
		if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof TGBlockEntity be) {
			for (ItemStack drop : be.drops()) Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), drop);
		}
		super.onRemove(state, level, pos, newState, movedByPiston);
	}
}
