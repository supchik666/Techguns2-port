package techguns.world.dungeon;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.storage.loot.LootTable;
import net.neoforged.neoforge.common.Tags;
import techguns.TGBlocks;

/**
 * The blocks a 1.12 procedural structure (military camp, city) wrote straight into the World, collected so a
 * structure piece can replay them chunk by chunk. The structure stands on flat ground at {@code groundY}: unwritten
 * blocks read as ground up to it and air above.
 */
public final class Canvas {
	private static final int CLEAR_HEIGHT = 24, FILL_DEPTH = 16;

	public final Camo camo;
	public final int groundY;
	/** Footprint that gets flattened to groundY. */
	public final BoundingBox area;
	private final Long2ObjectOpenHashMap<BlockState> blocks = new Long2ObjectOpenHashMap<>();
	private final Long2ObjectOpenHashMap<ResourceKey<LootTable>> chests = new Long2ObjectOpenHashMap<>();
	private final Long2ObjectOpenHashMap<Spawner> spawners = new Long2ObjectOpenHashMap<>();

	private record Spawner(int mobsLeft, int maxActive, EntityType<?>[] mobs) {
	}

	public Canvas(Camo camo, int groundY, BoundingBox area) {
		this.camo = camo;
		this.groundY = groundY;
		this.area = area;
	}

	public void set(int x, int y, int z, BlockState state) {
		long key = BlockPos.asLong(x, y, z);
		blocks.put(key, state);
		chests.remove(key);
		spawners.remove(key);
	}

	public void air(int x, int y, int z) {
		set(x, y, z, Blocks.AIR.defaultBlockState());
	}

	public BlockState get(int x, int y, int z) {
		BlockState state = blocks.get(BlockPos.asLong(x, y, z));
		return state != null ? state : y <= groundY ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState();
	}

	public boolean isAir(int x, int y, int z) {
		return get(x, y, z).isAir();
	}

	/** 1.12 BlockUtils.fillBlocks. */
	public void fill(int x, int y, int z, int sizeX, int sizeY, int sizeZ, BlockState state) {
		for (int i = 0; i < sizeX; i++) {
			for (int j = 0; j < sizeZ; j++) {
				for (int k = 0; k < sizeY; k++) {
					set(x + i, y + k, z + j, state);
				}
			}
		}
	}

	public void chest(int x, int y, int z, BlockState state, ResourceKey<LootTable> loot) {
		set(x, y, z, state);
		chests.put(BlockPos.asLong(x, y, z), loot);
	}

	/** See {@link DungeonSpawners#place}. */
	public void spawner(int x, int y, int z, int mobsLeft, int maxActive, EntityType<?>... mobs) {
		set(x, y, z, Blocks.SPAWNER.defaultBlockState());
		spawners.put(BlockPos.asLong(x, y, z), new Spawner(mobsLeft, maxActive, mobs));
	}

	/** 1.12 ItemDoor.placeDoor. */
	public void door(int x, int y, int z, Block door, Direction facing, boolean rightHinge) {
		BlockState lower = door.defaultBlockState().setValue(DoorBlock.FACING, facing)
				.setValue(DoorBlock.HINGE, rightHinge ? DoorHingeSide.RIGHT : DoorHingeSide.LEFT);
		set(x, y, z, lower);
		set(x, y + 1, z, lower.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
	}

	/** A red bed with its foot at (x, y, z) and its head one block towards {@code facing}. */
	public void bed(int x, int y, int z, Direction facing) {
		BlockState foot = Blocks.RED_BED.defaultBlockState().setValue(BedBlock.FACING, facing);
		set(x, y, z, foot);
		set(x + facing.getStepX(), y, z + facing.getStepZ(), foot.setValue(BedBlock.PART, BedPart.HEAD));
	}

	/** Writes the part of the canvas inside {@code box}: flattens the footprint to groundY, then sets the blocks. */
	public void place(WorldGenLevel level, BoundingBox box, RandomSource random) {
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		int minX = Math.max(box.minX(), area.minX()), maxX = Math.min(box.maxX(), area.maxX());
		int minZ = Math.max(box.minZ(), area.minZ()), maxZ = Math.min(box.maxZ(), area.maxZ());
		for (int x = minX; x <= maxX; x++) {
			for (int z = minZ; z <= maxZ; z++) {
				for (int y = groundY + 1; y <= groundY + CLEAR_HEIGHT; y++) {
					if (!level.getBlockState(pos.set(x, y, z)).isAir()) level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
				}
				for (int y = groundY; y > groundY - FILL_DEPTH && level.getBlockState(pos.set(x, y, z)).canBeReplaced(); y--) {
					level.setBlock(pos, (y == groundY ? Blocks.GRASS_BLOCK : Blocks.DIRT).defaultBlockState(), 2);
				}
			}
		}
		for (Long2ObjectMap.Entry<BlockState> e : blocks.long2ObjectEntrySet()) {
			pos.set(e.getLongKey());
			if (!DungeonPiece.set(level, box, pos, e.getValue())) continue;
			ResourceKey<LootTable> loot = chests.get(e.getLongKey());
			if (loot != null) RandomizableContainer.setBlockEntityLootTable(level, random, pos, loot);
			Spawner s = spawners.get(e.getLongKey());
			if (s != null) DungeonSpawners.place(level, pos.immutable(), random, s.mobsLeft, s.maxActive, s.mobs);
		}
	}

	/** 1.12 BiomeColorType without NETHER: which camo net a structure uses. */
	public enum Camo {
		WOOD, DESERT, SNOW;

		/** 1.12 BlockUtils.getBiomeType. */
		public static Camo of(Holder<Biome> biome) {
			if (biome.is(Tags.Biomes.IS_SANDY) || biome.is(Tags.Biomes.IS_SAVANNA) || biome.is(Tags.Biomes.IS_WASTELAND)) return DESERT;
			return biome.is(Tags.Biomes.IS_SNOWY) ? SNOW : WOOD;
		}

		public BlockState net(boolean top) {
			return (switch (this) {
				case WOOD -> top ? TGBlocks.CAMONET_TOP_WOOD : TGBlocks.CAMONET_WOOD;
				case DESERT -> top ? TGBlocks.CAMONET_TOP_DESERT : TGBlocks.CAMONET_DESERT;
				case SNOW -> top ? TGBlocks.CAMONET_TOP_SNOW : TGBlocks.CAMONET_SNOW;
			}).get().defaultBlockState();
		}
	}
}
