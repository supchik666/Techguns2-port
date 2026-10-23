package techguns.world.dungeon;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import techguns.world.TGSpawnerBlockEntity;

/** The one place M7.2 structures put mob spawners. */
public final class DungeonSpawners {
	private DungeonSpawners() {
	}

	/** A TG spawner ({@code block}: hole or soldier spawn) with 1.12 TGSpawner's params: {@code mobsLeft} mobs in total, {@code maxActive} at once. */
	public static void place(WorldGenLevel level, BlockPos pos, RandomSource random, Block block, int mobsLeft, int maxActive, EntityType<?>... mobs) {
		level.setBlock(pos, block.defaultBlockState(), 2);
		if (level.getBlockEntity(pos) instanceof TGSpawnerBlockEntity spawner) {
			spawner.configure(mobsLeft, maxActive, 200, 2);
			for (EntityType<?> mob : mobs) spawner.addMob(mob, 1);
		}
	}
}
