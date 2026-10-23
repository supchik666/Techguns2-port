package techguns.world.dungeon;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;

/** The one place M7.2 structures put mob spawners. */
public final class DungeonSpawners {
	private DungeonSpawners() {
	}

	/**
	 * A spawner for {@code mobs} with 1.12 TGSpawner's params: {@code mobsLeft} mobs in total, {@code maxActive} at once.
	 * ponytail: vanilla spawner until M7.1's TG spawner is merged: it spawns one random type of {@code mobs} forever and
	 * ignores mobsLeft. Swap the body for the TG spawner block (configure(mobsLeft, maxActive, 200, 2) + addMob) then.
	 */
	public static void place(WorldGenLevel level, BlockPos pos, RandomSource random, int mobsLeft, int maxActive, EntityType<?>... mobs) {
		level.setBlock(pos, Blocks.SPAWNER.defaultBlockState(), 2);
		if (level.getBlockEntity(pos) instanceof SpawnerBlockEntity spawner) {
			spawner.setEntityId(mobs[random.nextInt(mobs.length)], random);
			CompoundTag tag = spawner.getSpawner().save(new CompoundTag());
			tag.putShort("MaxNearbyEntities", (short) maxActive);
			tag.putShort("SpawnCount", (short) maxActive);
			spawner.getSpawner().load(null, pos, tag);
		}
	}
}
