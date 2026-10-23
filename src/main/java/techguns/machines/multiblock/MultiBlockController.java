package techguns.machines.multiblock;

import java.util.function.Supplier;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import techguns.machines.MachineBlock;

/**
 * The controller of a multiblock machine: clicked unformed (not sneaking), it tries to form the structure around it
 * (1.12 BasicMachine.onBlockActivated); formed, or sneaking, it opens the GUI. Breaking it unforms the machine.
 */
public class MultiBlockController extends MachineBlock {
	public MultiBlockController(Supplier<? extends BlockEntityType<? extends MultiBlockMachine>> type, Properties properties) {
		super(type, properties);
		registerDefaultState(defaultBlockState().setValue(MultiBlockPart.FORMED, false));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder);
		builder.add(MultiBlockPart.FORMED);
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (state.getValue(MultiBlockPart.FORMED) || player.isShiftKeyDown()) return super.useWithoutItem(state, level, pos, player, hit);
		if (!level.isClientSide && level.getBlockEntity(pos) instanceof MultiBlockMachine machine && machine.canUse(player)) {
			machine.tryForm(player, hit.getDirection());
		}
		return InteractionResult.sidedSuccess(level.isClientSide);
	}

	@Override
	protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
		if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof MultiBlockMachine machine) machine.unform();
		super.onRemove(state, level, pos, newState, movedByPiston);
	}
}
