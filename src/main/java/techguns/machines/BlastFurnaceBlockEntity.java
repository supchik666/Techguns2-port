package techguns.machines;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.block.state.BlockState;
import techguns.TGBlockEntities;
import techguns.recipes.MachineRecipe;

/** 1.12 BlastFurnaceTileEnt. */
public class BlastFurnaceBlockEntity extends MachineBlockEntity {
	public BlastFurnaceBlockEntity(BlockPos pos, BlockState state) {
		super(TGBlockEntities.BLAST_FURNACE.get(), pos, state, "blast_furnace", new int[][] {{19, 17}, {47, 17}, {116, 50}, {150, 50}},
				MachineRecipe.BLAST_FURNACE, 40000, 10, 2, false);
	}

	/** 1.12 multiplied the power by the stack multiplier twice. */
	@Override
	protected int operationPower(MachineRecipe recipe, int mult) {
		return super.operationPower(recipe, mult) * mult;
	}

	@Override
	protected void ambient() {
		if (progress % 35 == 0) playSound(SoundEvents.FURNACE_FIRE_CRACKLE, 0.75f, 1);
		if (progress % 20 != 0) return;
		playSound(SoundEvents.FIRE_AMBIENT, 0.75f, 0.5f + level.random.nextFloat() * 0.15f);
		Direction facing = getBlockState().getValue(MachineBlock.FACING);
		double side = level.random.nextDouble() * 0.6 - 0.3;
		double x = worldPosition.getX() + 0.5 + facing.getStepX() * 0.52 + (facing.getAxis() == Direction.Axis.Z ? side : 0);
		double y = worldPosition.getY() + level.random.nextDouble() * 6 / 16 + 3.0 / 16;
		double z = worldPosition.getZ() + 0.5 + facing.getStepZ() * 0.52 + (facing.getAxis() == Direction.Axis.X ? side : 0);
		level.addParticle(ParticleTypes.SMOKE, x, y, z, 0, 0, 0);
		level.addParticle(ParticleTypes.FLAME, x, y, z, 0, 0, 0);
	}
}
