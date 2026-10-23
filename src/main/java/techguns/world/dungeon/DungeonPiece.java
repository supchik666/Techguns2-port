package techguns.world.dungeon;

import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CrossCollisionBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.storage.loot.LootTable;
import techguns.blocks.CamoNetBlock;
import techguns.blocks.Door3x3Block;
import techguns.blocks.SandbagsBlock;
import techguns.blocks.WallLanternBlock;
import techguns.world.dungeon.DungeonTemplate.Grid;
import techguns.world.dungeon.DungeonTemplate.Type;

/**
 * A whole maze dungeon as one piece: the segment list from {@link MazeDungeonPath} is stored as ints, and
 * postProcess places only the blocks inside the chunk being generated.
 */
public class DungeonPiece extends StructurePiece {
	/** legacy rotation (counterclockwise quarter turns) to vanilla. */
	private static final Rotation[] ROTATIONS = {Rotation.NONE, Rotation.COUNTERCLOCKWISE_90, Rotation.CLOCKWISE_180, Rotation.CLOCKWISE_90};

	private final DungeonPreset preset;
	private final BlockPos origin;
	/** 6 ints per segment, see {@link MazeDungeonPath#placements}. */
	private final int[] segments;
	/** 3 grid ints per spawner. */
	private final int[] spawners;
	/** Nether entrance: start cell x, y, z and 1.12 rotation, or empty. */
	private final int[] entrance;

	public DungeonPiece(DungeonPreset preset, BlockPos origin, MazeDungeonPath path, boolean withEntrance) {
		super(TGDungeons.DUNGEON_PIECE.get(), 0, box(origin, path.placements, withEntrance));
		this.preset = preset;
		this.origin = origin;
		this.segments = path.placements.stream().flatMapToInt(java.util.Arrays::stream).toArray();
		this.spawners = path.spawners.stream().flatMapToInt(java.util.Arrays::stream).toArray();
		this.entrance = withEntrance ? new int[]{path.startPos.getX(), path.startPos.getY(), path.startPos.getZ(), facingToRotation(path.startFacing)} : new int[0];
	}

	public DungeonPiece(CompoundTag tag) {
		super(TGDungeons.DUNGEON_PIECE.get(), tag);
		preset = DungeonPreset.values()[tag.getInt("preset")];
		origin = BlockPos.of(tag.getLong("origin"));
		segments = tag.getIntArray("segments");
		spawners = tag.getIntArray("spawners");
		entrance = tag.getIntArray("entrance");
	}

	@Override
	protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
		tag.putInt("preset", preset.ordinal());
		tag.putLong("origin", origin.asLong());
		tag.putIntArray("segments", segments);
		tag.putIntArray("spawners", spawners);
		tag.putIntArray("entrance", entrance);
	}

	private static BoundingBox box(BlockPos o, List<int[]> placements, boolean withEntrance) {
		int maxX = 0, maxY = 0, maxZ = 0;
		for (int[] p : placements) {
			maxX = Math.max(maxX, p[3]);
			maxY = Math.max(maxY, p[4] + (p[1] == Type.RAMP.ordinal() ? 1 : 0));
			maxZ = Math.max(maxZ, p[5]);
		}
		int s = DungeonPreset.SIZE_XZ, h = DungeonPreset.SIZE_Y;
		//the nether entrance pokes out of the start cell by up to 3 blocks sideways and 1 up
		int pad = withEntrance ? 3 : 0;
		return new BoundingBox(o.getX() - pad, o.getY(), o.getZ() - pad, o.getX() + (maxX + 1) * s - 1 + pad, o.getY() + (maxY + 1) * h + pad, o.getZ() + (maxZ + 1) * s - 1 + pad);
	}

	/** 1.12 BlockUtils.facingToRotation. */
	private static int facingToRotation(net.minecraft.core.Direction facing) {
		return switch (facing) {
			case WEST -> 0;
			case SOUTH -> 1;
			case EAST -> 2;
			default -> 3;
		};
	}

	@Override
	public void postProcess(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator, RandomSource random, BoundingBox box, ChunkPos chunkPos, BlockPos pivot) {
		int s = DungeonPreset.SIZE_XZ, h = DungeonPreset.SIZE_Y;
		for (int i = 0; i < segments.length; i += 6) {
			Grid grid = DungeonTemplate.get(DungeonTemplate.NAMES.get(segments[i])).grid(Type.values()[segments[i + 1]]);
			int x = origin.getX() + segments[i + 3] * s, y = origin.getY() + segments[i + 4] * h, z = origin.getZ() + segments[i + 5] * s;
			if (box.intersects(x, z, x + s - 1, z + s - 1)) {
				placeGrid(level, box, random, grid, x, y, z, segments[i + 2], s - 1, s - 1, preset.lootTable);
			}
		}
		if (entrance.length > 0) {
			//1.12 WorldGenTestTool: entrance origin one block outside the start cell, turned around the cell's center
			Grid grid = DungeonTemplate.get("netherdungeon_entrance").grid(Type.STRUCTURE);
			placeGrid(level, box, random, grid, origin.getX() + entrance[0] * s - 1, origin.getY() + entrance[1] * h, origin.getZ() + entrance[2] * s - 1, entrance[3], 6, 6, null);
		}
		for (int i = 0; i < spawners.length; i += 3) {
			BlockPos pos = origin.offset(spawners[i] * s + s / 2, spawners[i + 1] * h + 1, spawners[i + 2] * s + s / 2);
			if (box.isInside(pos)) {
				DungeonSpawners.place(level, pos, random, 2, 2, preset.mobs.stream().map(java.util.function.Supplier::get).toArray(EntityType[]::new));
			}
		}
	}

	/**
	 * Places a template grid at (x, y, z) turned {@code rot} legacy steps around the doubled center (c2x/2, c2z/2) in
	 * grid coordinates, clipped to {@code box}. Chests get {@code loot}.
	 */
	static void placeGrid(WorldGenLevel level, BoundingBox box, RandomSource random, Grid grid, int x, int y, int z, int rot, int c2x, int c2z, @Nullable ResourceKey<LootTable> loot) {
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		for (int ly = 0; ly < grid.sizeY(); ly++) {
			for (int lz = 0; lz < grid.sizeZ(); lz++) {
				for (int lx = 0; lx < grid.sizeX(); lx++) {
					BlockState state = grid.get(lx, ly, lz);
					if (state == null) continue;
					int dx = 2 * lx - c2x, dz = 2 * lz - c2z;
					for (int r = 0; r < rot; r++) {
						int t = dx;
						dx = dz;
						dz = -t;
					}
					pos.set(x + (c2x + dx) / 2, y + ly, z + (c2z + dz) / 2);
					if (set(level, box, pos, rotate(state, rot)) && loot != null && state.is(net.minecraft.world.level.block.Blocks.CHEST)) {
						RandomizableContainer.setBlockEntityLootTable(level, random, pos, loot);
					}
				}
			}
		}
	}

	/** 1.12 BlockRotator.getRotatedHorizontal: vanilla rotation plus TG blocks that don't rotate themselves. */
	public static BlockState rotate(BlockState state, int legacyRot) {
		//1.12 turned negative counts zero times
		Rotation rotation = ROTATIONS[Math.max(0, legacyRot) & 3];
		BlockState rotated = state.rotate(rotation);
		if (rotated != state) return rotated;
		if (state.hasProperty(BlockStateProperties.FACING)) {
			return state.setValue(BlockStateProperties.FACING, rotation.rotate(state.getValue(BlockStateProperties.FACING)));
		}
		if (state.hasProperty(Door3x3Block.ZPLANE) && (legacyRot & 1) == 1) return state.cycle(Door3x3Block.ZPLANE);
		return state;
	}

	/** StructurePiece.placeBlock without the piece transform: set inside {@code box}, tick fluids, fix connecting shapes later. */
	public static boolean set(WorldGenLevel level, BoundingBox box, BlockPos pos, BlockState state) {
		if (!box.isInside(pos)) return false;
		level.setBlock(pos, state, 2);
		if (!state.getFluidState().isEmpty()) level.scheduleTick(pos, state.getFluidState().getType(), 0);
		Block block = state.getBlock();
		if (block instanceof StairBlock || block instanceof CrossCollisionBlock || block instanceof WallBlock
				|| block instanceof CamoNetBlock || block instanceof SandbagsBlock || block instanceof WallLanternBlock) {
			level.getChunk(pos).markPosForPostprocessing(pos);
		}
		return true;
	}
}
