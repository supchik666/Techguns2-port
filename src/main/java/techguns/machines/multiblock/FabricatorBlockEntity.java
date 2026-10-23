package techguns.machines.multiblock;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.Capabilities;
import techguns.TGBlocks;
import techguns.TGSounds;
import techguns.recipes.MachineRecipe;

/**
 * 1.12 FabricatorTileEntMaster and FabricatorDefinition: a 2x2x2 machine, the controller and three housings at the bottom,
 * four glass blocks on top, extending away from the clicked side and to its left. Runs {@code techguns:fabricator}
 * machine recipes: input, wire, powder and plate slots. The housings take items and power.
 */
public class FabricatorBlockEntity extends MultiBlockMachine {
	public static final int GLASS = 1, HOUSING = 2;

	public FabricatorBlockEntity(BlockPos pos, BlockState state) {
		super(MultiBlocks.FABRICATOR.get(), pos, state, "fabricator", new int[][] {{19, 17}, {47, 17}, {68, 17}, {89, 17}, {116, 50}, {150, 50}},
				MachineRecipe.FABRICATOR, 100000, 80, 4, true);
	}

	/** The wire, powder and plate slots take what some recipe wants there; the input slot takes anything. */
	@Override
	public boolean canInsert(int slot, ItemStack stack) {
		if (slot < 1 || slot >= inputs || level == null) return super.canInsert(slot, stack);
		return recipes().stream().anyMatch(r -> r.value().inputs().size() > slot && r.value().inputs().get(slot).ingredient().test(stack));
	}

	@Nullable
	@Override
	protected Map<BlockPos, Integer> check(Player player, Direction face) {
		if (face.getAxis().isVertical()) return invalid(player, "techguns.msg.error.multiblockinvalid");
		BlockPos behind = worldPosition.relative(face.getOpposite());
		Direction left = face.getClockWise();
		List<BlockPos> housings = List.of(behind, worldPosition.relative(left), behind.relative(left));
		List<BlockPos> glass = List.of(worldPosition.above(), behind.above(), worldPosition.relative(left).above(), behind.relative(left).above());
		boolean housingsOk = allParts(housings, TGBlocks.FABRICATOR_HOUSING.get());
		if (!(allParts(glass, TGBlocks.FABRICATOR_GLASS.get()) & housingsOk)) return invalid(player, "techguns.msg.error.multiblockinvalid");
		direction = face;
		Map<BlockPos, Integer> parts = new HashMap<>();
		housings.forEach(p -> parts.put(p, HOUSING));
		glass.forEach(p -> parts.put(p, GLASS));
		return parts;
	}

	@Nullable
	@Override
	@SuppressWarnings("unchecked")
	public <T> T partCapability(BlockCapability<T, Direction> cap, int type) {
		if (type != HOUSING) return null;
		if (cap == Capabilities.ItemHandler.BLOCK) return (T) automation;
		if (cap == Capabilities.EnergyStorage.BLOCK) return (T) energy;
		return null;
	}

	@Override
	protected void ambient() {
		if (progress == 1) playSound(TGSounds.FABRICATOR_WORK.get(), 0.5f, 1);
	}
}
