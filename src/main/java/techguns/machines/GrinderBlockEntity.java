package techguns.machines;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import techguns.TGBlockEntities;
import techguns.TGItems;
import techguns.TGSounds;
import techguns.items.guns.GenericGun;
import techguns.recipes.MachineRecipe;

/** 1.12 GrinderTileEnt: grinds an item back into (some of) its materials, into a 3x3 output grid. */
public class GrinderBlockEntity extends MachineBlockEntity {
	public GrinderBlockEntity(BlockPos pos, BlockState state) {
		super(TGBlockEntities.GRINDER.get(), pos, state, "grinder", slots(), MachineRecipe.GRINDER, 20000, 5, 1, false);
	}

	private static int[][] slots() {
		int[][] slots = new int[11][];
		slots[0] = new int[] {18, 17};
		for (int i = 0; i < 9; i++) slots[1 + i] = new int[] {80 + i % 3 * 18, 17 + i / 3 * 18};
		slots[10] = new int[] {152, 60};
		return slots;
	}

	/** What's being ground, for the renderer. */
	public ItemStack grinding() {
		return operation == null || operation.inputs().isEmpty() ? ItemStack.EMPTY : operation.inputs().getFirst();
	}

	@Override
	protected void ambient() {
		if (progress % 2 == 0 && !grinding().isEmpty()) {
			ItemStack chips = grinding().getItem() instanceof GenericGun ? new ItemStack(TGItems.PLATE_STEEL.get()) : grinding();
			level.addParticle(new ItemParticleOption(ParticleTypes.ITEM, chips), worldPosition.getX() + 0.5, worldPosition.getY() + 0.6,
					worldPosition.getZ() + 0.5, Math.random() * 0.1 - 0.05, Math.random() * 0.1 + 0.1, Math.random() * 0.1 - 0.05);
		}
		if (progress == 1) playSound(TGSounds.GRINDER_START.get(), 1, 1);
		else if (progress == Math.round(totaltime * 0.2f) + 1 || progress == Math.round(totaltime * 0.6f) - 1) playSound(TGSounds.GRINDER_WORK.get(), 1, 1);
	}
}
