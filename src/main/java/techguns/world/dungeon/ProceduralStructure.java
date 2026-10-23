package techguns.world.dungeon;

import java.util.Optional;
import java.util.Random;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.QuartPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import techguns.world.camp.MilitaryCamp;
import techguns.world.city.GenericCity;

/**
 * 1.12 MilitaryBaseStructure and the GenericCity test structure: a layout rolled from a seed, drawn into a
 * {@link Canvas} again for every chunk it covers.
 */
public class ProceduralStructure extends Structure {
	public enum Kind { MILITARY_BASE, CITY }

	public static final MapCodec<ProceduralStructure> MILITARY_BASE_CODEC = simpleCodec(s -> new ProceduralStructure(s, Kind.MILITARY_BASE));
	public static final MapCodec<ProceduralStructure> CITY_CODEC = simpleCodec(s -> new ProceduralStructure(s, Kind.CITY));
	static final int MIN_SIZE = 32, HEIGHT = 24;

	private final Kind kind;

	public ProceduralStructure(StructureSettings settings, Kind kind) {
		super(settings);
		this.kind = kind;
	}

	/** 1.12 MilitaryBaseStructure sizes (32-80) and height check (5), shrinking by 16 until the ground is flat enough. */
	@Override
	protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
		WorldgenRandom random = context.random();
		int x = context.chunkPos().getMinBlockX(), z = context.chunkPos().getMinBlockZ();
		for (int sizeX = MIN_SIZE + random.nextInt(48), sizeZ = MIN_SIZE + random.nextInt(48); sizeX >= MIN_SIZE && sizeZ >= MIN_SIZE; sizeX -= 16, sizeZ -= 16) {
			int y = DungeonStructure.landHeight(context, x, z, sizeX, sizeZ, 5);
			if (y < 0) continue;
			Canvas.Camo camo = Canvas.Camo.of(context.biomeSource().getNoiseBiome(QuartPos.fromBlock(x + sizeX / 2), QuartPos.fromBlock(y),
					QuartPos.fromBlock(z + sizeZ / 2), context.randomState().sampler()));
			Piece piece = new Piece(kind, random.nextLong(), new BlockPos(x, y - 1, z), sizeX, sizeZ, camo);
			return Optional.of(new GenerationStub(new BlockPos(x, y, z), b -> b.addPiece(piece)));
		}
		return Optional.empty();
	}

	@Override
	public StructureType<?> type() {
		return kind == Kind.CITY ? TGDungeons.CITY.get() : TGDungeons.MILITARY_BASE.get();
	}

	/** The whole structure as one piece; origin is its corner on the ground block layer. */
	public static class Piece extends StructurePiece {
		final Kind kind;
		final long seed;
		final BlockPos origin;
		final int sizeX, sizeZ;
		final Canvas.Camo camo;

		public Piece(Kind kind, long seed, BlockPos origin, int sizeX, int sizeZ, Canvas.Camo camo) {
			//box from the ground layer up so beard_box levels the terrain to it; the fence/roads stick out by one
			super(TGDungeons.PROCEDURAL_PIECE.get(), 0, new BoundingBox(origin.getX() - 1, origin.getY() + 1, origin.getZ() - 1,
					origin.getX() + sizeX, origin.getY() + HEIGHT, origin.getZ() + sizeZ));
			this.kind = kind;
			this.seed = seed;
			this.origin = origin;
			this.sizeX = sizeX;
			this.sizeZ = sizeZ;
			this.camo = camo;
		}

		public Piece(CompoundTag tag) {
			this(Kind.values()[tag.getInt("kind")], tag.getLong("seed"), BlockPos.of(tag.getLong("origin")), tag.getInt("sizeX"), tag.getInt("sizeZ"),
					Canvas.Camo.values()[tag.getInt("camo")]);
		}

		@Override
		protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
			tag.putInt("kind", kind.ordinal());
			tag.putLong("seed", seed);
			tag.putLong("origin", origin.asLong());
			tag.putInt("sizeX", sizeX);
			tag.putInt("sizeZ", sizeZ);
			tag.putInt("camo", camo.ordinal());
		}

		/** Draws the whole layout; same seed, same blocks. */
		public Canvas draw() {
			int x = origin.getX(), y = origin.getY(), z = origin.getZ();
			Canvas c = new Canvas(camo, y, new BoundingBox(x - 1, y, z - 1, x + sizeX, y, z + sizeZ));
			if (kind == Kind.CITY) new GenericCity(new Random(seed), x, y, z, sizeX, sizeZ).draw(c);
			else new MilitaryCamp(new Random(seed), x, y, z, sizeX, sizeZ).draw(c);
			return c;
		}

		/** ponytail: redraws the layout for each of its ~25 chunks, a cache keyed by seed if worldgen time shows it */
		@Override
		public void postProcess(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator, RandomSource random, BoundingBox box, ChunkPos chunkPos, BlockPos pivot) {
			draw().place(level, box, random);
		}
	}
}
