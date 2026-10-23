package techguns.world.dungeon;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;

import com.mojang.serialization.MapCodec;

import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.phys.Vec3;
import techguns.TGBlocks;
import techguns.TGEntities;
import techguns.world.TGSpawnerBlockEntity;

/**
 * 1.12 AlienBugNest(Structure): a shaft from the desert surface down to a main chamber, linked by tunnels to 5-10 side
 * chambers whose floors are covered in eggs around an alien bug spawner. Carved into the terrain, so the whole layout
 * is drawn from the seed and replayed chunk by chunk.
 */
public class AlienBugNest extends Structure {
	public static final MapCodec<AlienBugNest> CODEC = simpleCodec(AlienBugNest::new);

	public AlienBugNest(StructureSettings settings) {
		super(settings);
	}

	/** 1.12 AlienBugNestStructure: centered in a random 16-31 block area from the chunk corner, 3 below the surface. */
	@Override
	protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
		int x = context.chunkPos().getMinBlockX(), z = context.chunkPos().getMinBlockZ();
		int surface = DungeonStructure.landHeight(context, x, z, 4, 4, 256);
		if (surface < 0) return Optional.empty();
		RandomSource random = context.random();
		BlockPos center = new BlockPos(x + (16 + random.nextInt(16)) / 2, surface - 3, z + (16 + random.nextInt(16)) / 2);
		Piece piece = new Piece(random.nextLong(), center, surface);
		return Optional.of(new GenerationStub(center, b -> b.addPiece(piece)));
	}

	@Override
	public StructureType<?> type() {
		return TGDungeons.ALIEN_BUG_NEST.get();
	}

	public static class Piece extends StructurePiece {
		final long seed;
		final BlockPos center;
		final int surface;
		private final Layout layout;

		public Piece(long seed, BlockPos center, int surface) {
			this(seed, center, surface, new Layout(seed, center, surface));
		}

		private Piece(long seed, BlockPos center, int surface, Layout layout) {
			super(TGDungeons.ALIEN_BUG_NEST_PIECE.get(), 0, layout.bounds());
			this.seed = seed;
			this.center = center;
			this.surface = surface;
			this.layout = layout;
		}

		public Piece(CompoundTag tag) {
			this(tag.getLong("seed"), BlockPos.of(tag.getLong("center")), tag.getInt("surface"));
		}

		@Override
		protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
			tag.putLong("seed", seed);
			tag.putLong("center", center.asLong());
			tag.putInt("surface", surface);
		}

		@Override
		public void postProcess(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator, RandomSource random, BoundingBox box, ChunkPos chunkPos, BlockPos pivot) {
			BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
			for (var e : layout.blocks.long2ObjectEntrySet()) {
				if (!DungeonPiece.set(level, box, pos.set(e.getLongKey()), e.getValue())) continue;
				if (layout.spawners.contains(e.getLongKey()) && level.getBlockEntity(pos) instanceof TGSpawnerBlockEntity spawner) {
					spawner.configure(7, 3, 150, 1).addMob(TGEntities.ALIENBUG.get(), 1);
				}
			}
		}
	}

	/** The nest's blocks in 1.12 drawing order; positions it never wrote are terrain: solid below the surface, air above. */
	public static final class Layout {
		private static final int SEGMENTS = 5, MIN_RADIUS = 3, START_RADIUS = Math.min(10, MIN_RADIUS + SEGMENTS / 2);
		private static final Direction[] LADDER_FACINGS = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};
		public final Long2ObjectLinkedOpenHashMap<BlockState> blocks = new Long2ObjectLinkedOpenHashMap<>();
		public final LongSet spawners = new LongOpenHashSet();
		private final Random rand;
		private final int surface;
		private final Segment main;

		private static final class Segment {
			final int x, y, z, radius, type; // type 0 main, 1 regular, 2 entrance
			int status; // 1 sphere, 2 connections, 3 interior done
			final List<Segment> neighbors = new ArrayList<>();

			Segment(int x, int y, int z, int radius, int type) {
				this.x = x;
				this.y = y;
				this.z = z;
				this.radius = radius;
				this.type = type;
			}
		}

		public Layout(long seed, BlockPos center, int surface) {
			this.rand = new Random(seed);
			this.surface = surface;
			int x = center.getX(), y = center.getY(), z = center.getZ();
			Segment entrance = new Segment(x, y, z, 2, 2);
			int mainY = y - (START_RADIUS + 5 + rand.nextInt(7));
			main = new Segment(x + rand.nextInt(3), mainY, z + rand.nextInt(3), START_RADIUS, 0);
			main.neighbors.add(entrance);
			double angle = Math.PI * 2.0 / SEGMENTS;
			Segment prev = main;
			for (int i = 0; i < SEGMENTS; i++) {
				int radius = Math.max(MIN_RADIUS, START_RADIUS - rand.nextInt(i + 1));
				double dist = START_RADIUS + radius + 5.0 + rand.nextDouble() * 8.0;
				double sx = x + dist * Math.cos(angle * i), sz = z + dist * Math.sin(angle * i);
				Segment seg = new Segment((int) sx, mainY + rand.nextInt(START_RADIUS * 3) - START_RADIUS * 2, (int) sz, radius, 1);
				if (prev == main || !rand.nextBoolean()) {
					main.neighbors.add(seg);
				} else {
					prev.neighbors.add(seg);
					if (rand.nextBoolean()) main.neighbors.add(seg);
				}
				if (rand.nextBoolean()) {
					radius = Math.max(MIN_RADIUS, START_RADIUS - rand.nextInt(i + 1));
					dist = START_RADIUS + radius + 5.0 + rand.nextDouble() * 8.0;
					double a = angle * i + Math.PI * 0.5 * (rand.nextDouble() - 0.5);
					seg.neighbors.add(new Segment((int) (sx + dist * Math.cos(a)), mainY + rand.nextInt(START_RADIUS * 3) - START_RADIUS * 2,
							(int) (sz + dist * Math.sin(a)), radius, 1));
				}
				prev = seg;
			}
			placeSegment(main);
			placeConnections(main);
			placeInterior(main);
		}

		BoundingBox bounds() {
			int[] b = {Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE};
			for (long key : blocks.keySet()) {
				int x = BlockPos.getX(key), y = BlockPos.getY(key), z = BlockPos.getZ(key);
				b[0] = Math.min(b[0], x); b[1] = Math.min(b[1], y); b[2] = Math.min(b[2], z);
				b[3] = Math.max(b[3], x); b[4] = Math.max(b[4], y); b[5] = Math.max(b[5], z);
			}
			return new BoundingBox(b[0], b[1], b[2], b[3], b[4], b[5]);
		}

		private void set(int x, int y, int z, BlockState state) {
			long key = BlockPos.asLong(x, y, z);
			blocks.put(key, state);
			spawners.remove(key);
		}

		private boolean isAir(int x, int y, int z) {
			BlockState state = blocks.get(BlockPos.asLong(x, y, z));
			return state == null ? y >= surface : state.isAir();
		}

		private boolean isSolid(int x, int y, int z) {
			BlockState state = blocks.get(BlockPos.asLong(x, y, z));
			return state == null ? y < surface : state.isSolid();
		}

		/** 1.12 wallBlock: bugnest sand 16, eggs 1, sandstone 2. */
		private BlockState wall() {
			int r = rand.nextInt(19);
			return (r < 16 ? TGBlocks.BUGNEST_SAND.get() : r < 17 ? TGBlocks.BUGNEST_EGGS.get() : Blocks.SANDSTONE).defaultBlockState();
		}

		/** 1.12 wallBlock2: bugnest sand 12, sandstone 1. */
		private BlockState wall2() {
			return (rand.nextInt(13) < 12 ? TGBlocks.BUGNEST_SAND.get() : Blocks.SANDSTONE).defaultBlockState();
		}

		private void placeSegment(Segment segment) {
			if (segment.status >= 1 || segment.type == 2) return;
			// 1.12 BlockUtils.fillSphere2: air inside, a one block wall shell
			int r = segment.radius;
			for (int i = -r; i <= r; i++) {
				for (int j = -r; j <= r; j++) {
					for (int k = -r; k <= r; k++) {
						int d = i * i + j * j + k * k;
						if (d < (r - 1) * (r - 1)) set(segment.x + i, segment.y + j, segment.z + k, Blocks.AIR.defaultBlockState());
						else if (d < r * r) set(segment.x + i, segment.y + j, segment.z + k, wall());
					}
				}
			}
			segment.status = 1;
			for (Segment seg : segment.neighbors) placeSegment(seg);
		}

		private void placeConnections(Segment segment) {
			if (segment.status >= 2) return;
			segment.status = 2;
			for (Segment seg : segment.neighbors) {
				Vec3 dir = new Vec3(seg.x - segment.x, seg.y - segment.y, seg.z - segment.z);
				double d2 = dir.length() - seg.radius + (seg.type == 2 ? 5.0 : 0);
				dir = dir.normalize();
				int x1 = (int) (segment.x + dir.x * segment.radius), y1 = (int) (segment.y + dir.y * segment.radius), z1 = (int) (segment.z + dir.z * segment.radius);
				int x2 = (int) (segment.x + dir.x * d2), y2 = (int) (segment.y + dir.y * d2), z2 = (int) (segment.z + dir.z * d2);
				int r = Math.min(segment.radius, Math.min(2 + rand.nextInt(2), seg.radius));
				fillCylinder(x1, y1, z1, x2, y2, z2, r);
				if (seg.type == 2) slimeLadder(x1, y1, z1, x2, y2, z2, r - 1);
				placeConnections(seg);
			}
		}

		/** 1.12 BlockUtils.fillCylinder: air within radius - 1 of the line, wallBlock2 out to radius. */
		private void fillCylinder(int x1, int y1, int z1, int x2, int y2, int z2, int radius) {
			Vec3 v1 = new Vec3(x1, y1, z1), v2v1 = v1.subtract(x2, y2, z2);
			double l = v2v1.length();
			for (int x = Math.min(x1, x2) - radius; x < Math.max(x1, x2) + radius; x++) {
				for (int y = Math.min(y1, y2) - radius; y < Math.max(y1, y2) + radius; y++) {
					for (int z = Math.min(z1, z2) - radius; z < Math.max(z1, z2) + radius; z++) {
						double distance = v2v1.cross(v1.subtract(x, y, z)).length() / l;
						if (distance < radius - 1) set(x, y, z, Blocks.AIR.defaultBlockState());
						else if (distance < radius) set(x, y, z, wall2());
					}
				}
			}
		}

		/** 1.12 createSlimeLadder; its distance check always passed, so it fills every free spot in the shaft's box. */
		private void slimeLadder(int x1, int y1, int z1, int x2, int y2, int z2, int r) {
			for (int x = Math.min(x1, x2) - r; x < Math.max(x1, x2) + r; x++) {
				for (int y = Math.min(y1, y2) - r; y < Math.max(y1, y2) + r; y++) {
					for (int z = Math.min(z1, z2) - r; z < Math.max(z1, z2) + r; z++) {
						ladder(x, y, z);
					}
				}
			}
		}

		/** A slimy ladder if there's air and a solid block to hang on, facing as 1.12 BlockLadder.getStateForPlacement. */
		private void ladder(int x, int y, int z) {
			if (!isAir(x, y, z)) return;
			for (Direction facing : LADDER_FACINGS) {
				if (isSolid(x - facing.getStepX(), y, z - facing.getStepZ())) {
					set(x, y, z, TGBlocks.SLIMYLADDER.get().defaultBlockState().setValue(LadderBlock.FACING, facing));
					return;
				}
			}
		}

		private void placeInterior(Segment segment) {
			if (segment.status >= 3 || segment.type == 2) return;
			int r = segment.radius - 1;
			for (int i = -r; i <= r; i++) {
				for (int j = -r; j <= r; j++) {
					for (int k = -r; k <= r; k++) {
						if (i * i + j * j + k * k < r * r) set(segment.x + i, segment.y + j, segment.z + k, Blocks.AIR.defaultBlockState());
					}
				}
			}
			segment.status = 3;
			if (segment.type == 1) placeEggs(segment);
			for (Segment seg : segment.neighbors) {
				if (segment.type == 0 && seg.type == 2) {
					// a sand pillar with ladders from the chamber floor up towards the shaft
					double angle = rand.nextDouble() * 2.0 * Math.PI, offset = 1.0 + rand.nextDouble() * 0.66;
					int xo = (int) (offset * Math.cos(angle)), zo = (int) (offset * Math.sin(angle));
					Vec3 dir = new Vec3(seg.x - segment.x, seg.y - segment.y, seg.z - segment.z).normalize();
					double d1 = -(segment.radius + 1), d2 = (segment.radius - 1) * 2;
					drawLine((int) (segment.x + xo + dir.x * d1), (int) (segment.y + dir.y * d1), (int) (segment.z + zo + dir.z * d1),
							(int) (segment.x + xo + dir.x * d2), (int) (segment.y + dir.y * d2), (int) (segment.z + zo + dir.z * d2));
				}
				placeInterior(seg);
			}
		}

		/** 1.12 MBlockSlime: bugnest sand with slimy ladders on its four sides. */
		private void slimy(int x, int y, int z) {
			set(x, y, z, TGBlocks.BUGNEST_SAND.get().defaultBlockState());
			ladder(x, y, z - 1);
			ladder(x + 1, y, z);
			ladder(x, y, z + 1);
			ladder(x - 1, y, z);
		}

		/** 1.12 BlockUtils.drawLine (3D Bresenham), keeping the line 4-connected while it climbs. */
		private void drawLine(int x1, int y1, int z1, int x2, int y2, int z2) {
			int ax = Math.abs(x2 - x1) << 1, ay = Math.abs(y2 - y1) << 1, az = Math.abs(z2 - z1) << 1;
			int sx = Integer.signum(x2 - x1), sy = Integer.signum(y2 - y1), sz = Integer.signum(z2 - z1);
			int x = x1, y = y1, z = z1;
			if (ax >= Math.max(ay, az)) {
				for (int dy = ay - (ax >> 1), dz = az - (ax >> 1); ; x += sx, dy += ay, dz += az) {
					slimy(x, y, z);
					if (x == x2) return;
					if (dy >= 0) { y += sy; dy -= ax; }
					if (dz >= 0) { z += sz; dz -= ax; }
				}
			} else if (ay >= Math.max(ax, az)) {
				for (int dx = ax - (ay >> 1), dz = az - (ay >> 1); ; y += sy, dx += ax, dz += az) {
					slimy(x, y, z);
					if (y == y2) return;
					if (dx >= 0) { slimy(x + sx, y, z); x += sx; dx -= ay; }
					if (dz >= 0) { slimy(x, y, z + sz); z += sz; dz -= ay; }
				}
			} else {
				for (int dx = ax - (az >> 1), dy = ay - (az >> 1); ; z += sz, dx += ax, dy += ay) {
					slimy(x, y, z);
					if (z == z2) return;
					if (dx >= 0) { x += sx; dx -= az; }
					if (dy >= 0) { y += sy; dy -= az; }
				}
			}
		}

		/** 1.12 placeEggs: an alien bug spawner on the floor and egg piles, thicker towards the middle. */
		private void placeEggs(Segment segment) {
			int r = segment.radius - 2, ri = r > 1 ? r - 1 : r;
			int spawnerX = rand.nextInt(2 * ri) - ri, spawnerZ = rand.nextInt(2 * ri) - ri;
			for (int i = -r; i < r; i++) {
				for (int j = -r; j < r; j++) {
					int x = segment.x + i, z = segment.z + j;
					if (i == spawnerX && j == spawnerZ) {
						set(x, segment.y - r, z, TGBlocks.HOLE.get().defaultBlockState());
						spawners.add(BlockPos.asLong(x, segment.y - r, z));
						continue;
					}
					float f = ((float) (r - Math.abs(i)) / r + (float) (r - Math.abs(j)) / r) / 2.0f;
					if (rand.nextDouble() >= f) continue;
					for (int k = 0; k < segment.radius; k++) {
						int y = segment.y - (segment.radius - 1) + k;
						if (isAir(x, y, z)) set(x, y, z, TGBlocks.BUGNEST_EGGS.get().defaultBlockState());
						if (rand.nextDouble() > f * 2.0f / 3.0f) break;
					}
				}
			}
		}
	}
}
