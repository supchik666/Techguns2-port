package techguns.gametest;

import java.io.InputStream;
import java.util.List;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import techguns.TGBlocks;
import techguns.Techguns;
import techguns.world.TGSpawnerBlockEntity;
import techguns.world.TGWorldgen;
import techguns.world.TemplateStructure;

@GameTestHolder(Techguns.MODID)
@PrefixGameTestTemplate(false)
public class WorldgenTests {
	private static final List<String> STRUCTURES = List.of("factory_building_small", "small_trainstation", "small_mine", "gasstation",
			"policestation", "survivor_hideout", "orecluster_spike", "orecluster_meteorbase", "desert_oil_cluster", "aircraft_carrier",
			"nether_altar_small", "nether_soul_platform", "nether_loot_01", "nether_acid_hole", "nether_orecluster_small", "nether_altar_medium",
			"nether_ghast_spawner", "nether_orecluster_castle");

	/** The spawner spawns a linked mob, counts its death, and removes itself after the last one. */
	@GameTest(template = "empty8", timeoutTicks = 100)
	public static void spawnerSpawnsAndLinks(GameTestHelper helper) {
		BlockPos rel = new BlockPos(4, 1, 4);
		helper.setBlock(rel, TGBlocks.SOLDIER_SPAWN.get());
		TGSpawnerBlockEntity spawner = helper.getBlockEntity(rel);
		spawner.configure(2, 1, 1, 0).addMob(EntityType.PIG, 1);
		helper.startSequence()
				.thenWaitUntil(() -> helper.assertValueEqual(spawner.activeCount(), 1, "active mobs"))
				.thenExecute(() -> {
					Pig pig = helper.getLevel().getEntitiesOfClass(Pig.class, helper.getBounds()).getFirst();
					helper.assertValueEqual(pig.getData(TGWorldgen.SPAWNER), helper.absolutePos(rel), "linked spawner");
					pig.kill();
					helper.assertValueEqual(spawner.mobsLeft, 1, "mobs left");
				})
				.thenWaitUntil(() -> helper.assertValueEqual(spawner.activeCount(), 1, "second mob"))
				.thenExecute(() -> helper.getLevel().getEntitiesOfClass(Pig.class, helper.getBounds()).forEach(Pig::kill))
				.thenExecute(() -> helper.assertBlockPresent(Blocks.AIR, rel))
				.thenSucceed();
	}

	/** Every converted template only names existing blocks and properties, and every data marker resolves to a valid state. */
	@GameTest(template = "empty")
	public static void templatesAreValid(GameTestHelper helper) throws Exception {
		ServerLevel level = helper.getLevel();
		StructureTemplateManager manager = level.getStructureManager();
		for (String name : STRUCTURES) {
			ResourceLocation id = Techguns.id(name);
			helper.assertTrue(manager.get(id).isPresent(), name + " template missing");
			CompoundTag nbt;
			try (InputStream in = level.getServer().getResourceManager().open(Techguns.id("structure/" + name + ".nbt"))) {
				nbt = NbtIo.readCompressed(in, NbtAccounter.unlimitedHeap());
			}
			for (Tag t : nbt.getList("palette", Tag.TAG_COMPOUND)) {
				CompoundTag state = (CompoundTag) t;
				StringBuilder s = new StringBuilder(state.getString("Name")).append('[');
				state.getCompound("Properties").getAllKeys().forEach(k -> s.append(k).append('=').append(state.getCompound("Properties").getString(k)).append(','));
				parse(helper, s.append(']').toString().replace(",]", "]").replace("[]", ""), name);
			}
			TemplateStructure.Piece piece = new TemplateStructure.Piece(manager, id, BlockPos.ZERO, Rotation.NONE, 0);
			for (Tag t : nbt.getList("blocks", Tag.TAG_COMPOUND)) {
				String marker = ((CompoundTag) t).getCompound("nbt").getString("metadata");
				if (!marker.isEmpty()) checkMarker(helper, piece, JsonParser.parseString(marker), name);
			}
		}
		helper.succeed();
	}

	private static void checkMarker(GameTestHelper helper, TemplateStructure.Piece piece, JsonElement node, String name) {
		if (node.isJsonPrimitive()) {
			parse(helper, node.getAsString(), name);
		} else if (node.isJsonArray()) {
			node.getAsJsonArray().forEach(e -> checkMarker(helper, piece, e, name));
		} else {
			node.getAsJsonObject().entrySet().stream().filter(e -> !e.getValue().isJsonPrimitive() || e.getKey().equals("then") || e.getKey().equals("else"))
					.forEach(e -> checkMarker(helper, piece, e.getValue(), name));
			// and it resolves
			parse(helper, piece.resolve(node, helper.absolutePos(BlockPos.ZERO), helper.getLevel()), name);
		}
	}

	private static void parse(GameTestHelper helper, String state, String name) {
		if (state.matches("\\d+")) return; // a weight
		try {
			BlockState parsed = BlockStateParser.parseForBlock(helper.getLevel().holderLookup(Registries.BLOCK), state, false).blockState();
			helper.assertTrue(!parsed.isAir() || state.equals("minecraft:air"), name + ": " + state);
		} catch (Exception e) {
			helper.fail(name + ": bad state " + state + ": " + e.getMessage());
		}
	}

	/** The gas station placed as a world gen piece: markers replaced, spawner and chest data loaded, doors whole. */
	@GameTest(template = "gasstation")
	public static void gasStationPiece(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos origin = helper.absolutePos(BlockPos.ZERO);
		TemplateStructure.Piece piece = new TemplateStructure.Piece(level.getStructureManager(), Techguns.id("gasstation"), origin, Rotation.NONE, 0);
		piece.postProcess(level, level.structureManager(), level.getChunkSource().getGenerator(), level.random, piece.getBoundingBox(),
				new ChunkPos(origin), origin);
		int spawners = 0, chests = 0, doors = 0;
		for (BlockPos pos : BlockPos.betweenClosed(piece.getBoundingBox().minX(), piece.getBoundingBox().minY(), piece.getBoundingBox().minZ(),
				piece.getBoundingBox().maxX(), piece.getBoundingBox().maxY(), piece.getBoundingBox().maxZ())) {
			BlockState state = level.getBlockState(pos);
			helper.assertFalse(state.is(Blocks.STRUCTURE_BLOCK), "marker left at " + pos);
			if (level.getBlockEntity(pos) instanceof TGSpawnerBlockEntity s) {
				spawners++;
				helper.assertValueEqual(s.mobs.size(), 3, "spawner mob types");
				helper.assertValueEqual(s.mobsLeft, 3, "spawner mobs left");
			}
			if (level.getBlockEntity(pos) instanceof ChestBlockEntity chest) {
				chests++;
				helper.assertTrue(chest.getLootTable() != null, "chest without loot table");
			}
			if (state.getBlock() instanceof DoorBlock && state.getValue(DoorBlock.HALF) == DoubleBlockHalf.LOWER) {
				doors++;
				BlockState upper = level.getBlockState(pos.above());
				helper.assertTrue(upper.is(state.getBlock()) && upper.getValue(DoorBlock.FACING) == state.getValue(DoorBlock.FACING), "door halves differ");
			}
		}
		helper.assertValueEqual(spawners, 1, "spawners");
		helper.assertValueEqual(chests, 6, "chests");
		helper.assertTrue(doors > 0, "no doors");
		helper.succeed();
	}

	/** The gas station finds a spot on the flat test world's surface, its floor on the top block. */
	@GameTest(template = "empty")
	public static void structureFindsSurface(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ChunkGenerator generator = level.getChunkSource().getGenerator();
		ChunkPos chunk = new ChunkPos(helper.absolutePos(BlockPos.ZERO));
		StructureStart start = level.registryAccess().registryOrThrow(Registries.STRUCTURE).getOrThrow(ResourceKey.create(Registries.STRUCTURE,
				Techguns.id("gasstation"))).generate(level.registryAccess(), generator, generator.getBiomeSource(), level.getChunkSource().randomState(),
				level.getStructureManager(), level.getSeed(), chunk, 0, level, biome -> true);
		helper.assertTrue(start.isValid(), "no place found");
		int top = generator.getFirstOccupiedHeight(chunk.getMinBlockX(), chunk.getMinBlockZ(), Heightmap.Types.WORLD_SURFACE_WG, level,
				level.getChunkSource().randomState());
		helper.assertValueEqual(start.getBoundingBox().minY(), top, "floor height");
		helper.succeed();
	}

	/** The structure loot tables roll items; a military crate drops its contents. */
	@GameTest(template = "empty")
	public static void lootTablesRoll(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		for (String name : new String[] {"gasstation", "policestation", "factory_building", "small_trainstation", "survivor_hideout", "aircraftcarrier"}) {
			LootTable table = level.getServer().reloadableRegistries().getLootTable(ResourceKey.create(Registries.LOOT_TABLE, Techguns.id("chests/" + name)));
			helper.assertTrue(table != LootTable.EMPTY, name + " loot table missing");
			LootParams params = new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN, Vec3.ZERO).create(LootContextParamSets.CHEST);
			helper.assertFalse(table.getRandomItems(params).isEmpty(), name + " rolled nothing");
		}
		BlockPos crate = helper.absolutePos(new BlockPos(1, 1, 1));
		helper.assertFalse(Block.getDrops(TGBlocks.MILITARY_CRATE_AMMO.get().defaultBlockState(), level, crate, null).isEmpty(), "crate dropped nothing");
		helper.succeed();
	}

	/** Structures, structure sets and the ore features are registered, and the ores reach the overworld biomes. */
	@GameTest(template = "empty")
	public static void worldgenRegistered(GameTestHelper helper) {
		var access = helper.getLevel().registryAccess();
		Registry<?> structures = access.registryOrThrow(Registries.STRUCTURE);
		STRUCTURES.forEach(s -> helper.assertTrue(structures.containsKey(Techguns.id(s)), "structure " + s));
		for (String set : new String[] {"overworld_small", "overworld_medium", "overworld_big", "nether_small", "nether_medium"}) {
			helper.assertTrue(access.registryOrThrow(Registries.STRUCTURE_SET).containsKey(Techguns.id(set)), "structure set " + set);
		}
		helper.assertTrue(access.registryOrThrow(NeoForgeRegistries.Keys.BIOME_MODIFIERS).containsKey(Techguns.id("ores")), "ore biome modifier");
		var ores = access.registryOrThrow(Registries.BIOME).getHolderOrThrow(Biomes.PLAINS).value().getGenerationSettings().features()
				.get(GenerationStep.Decoration.UNDERGROUND_ORES.ordinal());
		for (String ore : new String[] {"ore_tin", "ore_lead", "ore_titanium", "ore_uranium"}) {
			helper.assertTrue(ores.stream().anyMatch(f -> f.is(Techguns.id(ore))), ore + " not in plains");
		}
		helper.assertTrue(BuiltInRegistries.BLOCK_ENTITY_TYPE.containsKey(Techguns.id("tg_spawner")), "spawner block entity");
		helper.succeed();
	}
}
