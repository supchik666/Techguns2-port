package techguns.machines.multiblock;

import javax.annotation.Nullable;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.fluids.FluidUtil;
import techguns.machines.MachineBlock;
import techguns.machines.TGBlockEntity;

/**
 * A building block of a multiblock machine (1.12 MultiBlockMachine/BlockOreDrill housings, glass, frames...). Formed, it
 * opens its controller's GUI and passes fluid containers on to it; breaking it unforms the machine.
 */
public class MultiBlockPart extends BaseEntityBlock {
	public static final BooleanProperty FORMED = BooleanProperty.create("formed");
	private static final MapCodec<MultiBlockPart> CODEC = simpleCodec(MultiBlockPart::new);

	public MultiBlockPart(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(MachineBlock.FACING, Direction.NORTH).setValue(FORMED, false));
	}

	@Override
	protected MapCodec<? extends BaseEntityBlock> codec() {
		return CODEC;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(MachineBlock.FACING, FORMED);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return defaultBlockState().setValue(MachineBlock.FACING, context.getHorizontalDirection().getOpposite());
	}

	@Override
	protected RenderShape getRenderShape(BlockState state) {
		return RenderShape.MODEL;
	}

	@Nullable
	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new MultiBlockSlave(pos, state);
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!state.getValue(FORMED)) return InteractionResult.PASS;
		if (!level.isClientSide && level.getBlockEntity(pos) instanceof MultiBlockSlave part && part.master() != null) open(part.master(), player);
		return InteractionResult.sidedSuccess(level.isClientSide);
	}

	/** MachineBlock's GUI opening, for the controller. */
	static void open(TGBlockEntity be, Player player) {
		if (be.canUse(player)) {
			player.openMenu(be, be.getBlockPos());
		} else {
			player.displayClientMessage(Component.translatable("techguns.container.security.denied").append(" ").append(be.getDisplayName()), true);
		}
	}

	@Override
	protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
			BlockHitResult hit) {
		if (state.getValue(FORMED) && FluidUtil.interactWithFluidHandler(player, hand, level, pos, hit.getDirection())) {
			return ItemInteractionResult.sidedSuccess(level.isClientSide);
		}
		return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
	}

	@Override
	protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
		if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof MultiBlockSlave part && part.master() != null) part.master().unform();
		super.onRemove(state, level, pos, newState, movedByPiston);
	}
}
