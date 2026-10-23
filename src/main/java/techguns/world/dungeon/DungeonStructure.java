package techguns.world.dungeon;

import java.util.Optional;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

/** 1.12 CastleStructure, TechFortressStructure and NetherDungeonStructure (+ NetherDungeonEntrance): a maze dungeon from a preset. */
public class DungeonStructure extends Structure {
	public static final MapCodec<DungeonStructure> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(settingsCodec(i),
			DungeonPreset.CODEC.fieldOf("preset").forGetter(s -> s.preset)).apply(i, DungeonStructure::new));

	private final DungeonPreset preset;

	public DungeonStructure(StructureSettings settings, DungeonPreset preset) {
		super(settings);
		this.preset = preset;
	}

	@Override
	protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
		WorldgenRandom random = context.random();
		int x = context.chunkPos().getMinBlockX(), z = context.chunkPos().getMinBlockZ();
		if (preset == DungeonPreset.NETHER) {
			int sizeX = 24 + random.nextInt(16), sizeZ = 24 + random.nextInt(16);
			int height = 24 + random.nextInt(25);
			int y = caveFloor(context, x + sizeX / 2, z + sizeZ / 2);
			if (y < 0) return Optional.empty();
			//1.12 WorldGenTestTool: the dungeon hangs below y with its top level (and the entrance) at y
			BlockPos origin = new BlockPos(x, y + DungeonPreset.SIZE_Y + height % DungeonPreset.SIZE_Y - height, z);
			return Optional.of(new GenerationStub(new BlockPos(x, y, z),
					b -> b.addPiece(new DungeonPiece(preset, origin, MazeDungeonPath.generate(preset, sizeX, height, sizeZ, random), true))));
		}
		int sizeX = 32 + random.nextInt(16), sizeZ = 32 + random.nextInt(16), sizeY = 24 + random.nextInt(16);
		int y = landHeight(context, x, z, sizeX, sizeZ, 10);
		if (y < 0) return Optional.empty();
		//1.12 setBlocks(y-1) then generate at posY+1-sizeY: the bottom level is underground
		BlockPos origin = new BlockPos(x, y - DungeonPreset.SIZE_Y, z);
		return Optional.of(new GenerationStub(new BlockPos(x, y, z),
				b -> b.addPiece(new DungeonPiece(preset, origin, MazeDungeonPath.generate(preset, sizeX, sizeY, sizeZ, random), false))));
	}

	/**
	 * 1.12 BlockUtils.getValidSpawnYArea on worldgen noise: the mean surface height (first air block) over a 3x3 grid
	 * of the area, or -1 if it is under water or the heights differ by more than {@code heightDiffLimit}.
	 * ponytail: 9 samples instead of 1.12's every 4-8 blocks, enough for 32-80 block structures
	 */
	public static int landHeight(GenerationContext context, int x, int z, int sizeX, int sizeZ, int heightDiffLimit) {
		int min = Integer.MAX_VALUE, max = Integer.MIN_VALUE, sum = 0;
		for (int i = 0; i <= 2; i++) {
			for (int j = 0; j <= 2; j++) {
				int px = x + sizeX * i / 2, pz = z + sizeZ * j / 2;
				int h = context.chunkGenerator().getBaseHeight(px, pz, Heightmap.Types.WORLD_SURFACE_WG, context.heightAccessor(), context.randomState());
				int floor = context.chunkGenerator().getBaseHeight(px, pz, Heightmap.Types.OCEAN_FLOOR_WG, context.heightAccessor(), context.randomState());
				if (floor < h) return -1;
				min = Math.min(min, h);
				max = Math.max(max, h);
				sum += h;
			}
		}
		return max - min > heightDiffLimit ? -1 : sum / 9;
	}

	/** 1.12 getCaveHeight, simplified to one column: the lowest cave floor between y 32 and 100 with 3 blocks of air above. */
	private static int caveFloor(GenerationContext context, int x, int z) {
		NoiseColumn column = context.chunkGenerator().getBaseColumn(x, z, context.heightAccessor(), context.randomState());
		for (int y = 32; y < 100; y++) {
			if (column.getBlock(y - 1).isSolid() && column.getBlock(y).isAir() && column.getBlock(y + 1).isAir() && column.getBlock(y + 2).isAir()) {
				return y;
			}
		}
		return -1;
	}

	@Override
	public StructureType<?> type() {
		return TGDungeons.DUNGEON.get();
	}
}
