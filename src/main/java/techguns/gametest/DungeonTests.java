package techguns.gametest;

import java.util.Arrays;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import techguns.Techguns;
import techguns.world.dungeon.DungeonPiece;
import techguns.world.dungeon.DungeonPreset;
import techguns.world.dungeon.DungeonTemplate;
import techguns.world.dungeon.MazeDungeonPath;
import techguns.world.dungeon.ProceduralStructure;

/** M7.2: converted templates, maze layout, and the procedural structures drawn into {@code empty48}. */
@GameTestHolder(Techguns.MODID)
@PrefixGameTestTemplate(false)
public class DungeonTests {
	@GameTest(template = "empty8")
	public static void templatesLoad(GameTestHelper helper) {
		for (String name : DungeonTemplate.NAMES) {
			DungeonTemplate t = DungeonTemplate.get(name);
			boolean any = Arrays.stream(DungeonTemplate.Type.values()).anyMatch(type -> t.grid(type) != null);
			helper.assertTrue(any, name + " has no segments");
		}
		helper.assertTrue(DungeonTemplate.get("ncdung1").grid(DungeonTemplate.Type.STRAIGHT) != null, "castle has no straight segment");
		helper.assertTrue(DungeonTemplate.get("netherdungeon_entrance").grid(DungeonTemplate.Type.STRUCTURE) != null, "no nether entrance");
		helper.succeed();
	}

	@GameTest(template = "empty8")
	public static void mazeIsDeterministic(GameTestHelper helper) {
		for (DungeonPreset preset : DungeonPreset.values()) {
			MazeDungeonPath a = MazeDungeonPath.generate(preset, 40, 30, 40, RandomSource.create(42));
			MazeDungeonPath b = MazeDungeonPath.generate(preset, 40, 30, 40, RandomSource.create(42));
			helper.assertTrue(a.getNumSegments() > 3, preset + " maze too small: " + a.getNumSegments());
			helper.assertTrue(a.getNumSegments() == b.getNumSegments(), preset + " maze not deterministic");
		}
		helper.succeed();
	}

	/** A small castle placed through its piece: walls go down, a spawner or a chest shows up. */
	@GameTest(template = "empty48")
	public static void castlePlaces(GameTestHelper helper) {
		BlockPos origin = helper.absolutePos(new BlockPos(2, 1, 2));
		DungeonPiece piece = new DungeonPiece(DungeonPreset.CASTLE, origin, MazeDungeonPath.generate(DungeonPreset.CASTLE, 30, 20, 30, RandomSource.create(7)), false);
		piece.postProcess(helper.getLevel(), null, null, RandomSource.create(7), testBox(helper), new ChunkPos(origin), origin);
		helper.assertTrue(count(helper) > 500, "castle placed too few blocks");
		helper.succeed();
	}

	@GameTest(template = "empty48")
	public static void militaryBasePlaces(GameTestHelper helper) {
		procedural(helper, ProceduralStructure.Kind.MILITARY_BASE);
	}

	@GameTest(template = "empty48")
	public static void cityPlaces(GameTestHelper helper) {
		procedural(helper, ProceduralStructure.Kind.CITY);
	}

	private static void procedural(GameTestHelper helper, ProceduralStructure.Kind kind) {
		BlockPos origin = helper.absolutePos(new BlockPos(2, 17, 2));
		ProceduralStructure.Piece piece = new ProceduralStructure.Piece(kind, 1234L, origin, 40, 40, techguns.world.dungeon.Canvas.Camo.WOOD);
		piece.postProcess(helper.getLevel(), null, null, RandomSource.create(1), testBox(helper), new ChunkPos(origin), origin);
		int above = 0;
		for (BlockPos p : BlockPos.betweenClosed(origin.offset(0, 1, 0), origin.offset(39, 20, 39))) {
			if (!helper.getLevel().getBlockState(p).isAir()) above++;
		}
		helper.assertTrue(above > 200, kind + " placed only " + above + " blocks above ground");
		helper.assertTrue(helper.getLevel().getBlockState(origin.offset(20, 0, 20)).isSolid(), kind + " ground not filled");
		helper.succeed();
	}

	@GameTest(template = "empty8")
	public static void structuresRegistered(GameTestHelper helper) {
		var structures = helper.getLevel().registryAccess().registryOrThrow(Registries.STRUCTURE);
		for (String id : new String[]{"castle", "tech_fortress", "nether_dungeon", "military_base", "city"}) {
			helper.assertTrue(structures.containsKey(ResourceLocation.fromNamespaceAndPath(Techguns.MODID, id)), "missing structure " + id);
		}
		helper.assertTrue(helper.getLevel().registryAccess().registryOrThrow(Registries.STRUCTURE_SET)
				.containsKey(ResourceLocation.fromNamespaceAndPath(Techguns.MODID, "big_structures")), "missing structure set");
		helper.succeed();
	}

	private static BoundingBox testBox(GameTestHelper helper) {
		BlockPos a = helper.absolutePos(BlockPos.ZERO), b = helper.absolutePos(new BlockPos(47, 47, 47));
		return BoundingBox.fromCorners(a, b);
	}

	private static int count(GameTestHelper helper) {
		int n = 0;
		for (BlockPos p : BlockPos.betweenClosed(helper.absolutePos(BlockPos.ZERO), helper.absolutePos(new BlockPos(47, 47, 47)))) {
			if (!helper.getLevel().getBlockState(p).is(Blocks.AIR)) n++;
		}
		return n;
	}
}
