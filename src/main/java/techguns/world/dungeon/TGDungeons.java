package techguns.world.dungeon;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import techguns.Techguns;

/**
 * Structure and piece types of the procedural structures (M7.2). The structures themselves, their structure sets and
 * biome tags are data: data/techguns/worldgen/structure*, data/techguns/tags/worldgen/biome/has_structure.
 */
public final class TGDungeons {
	public static final DeferredRegister<StructureType<?>> STRUCTURE_TYPES = DeferredRegister.create(Registries.STRUCTURE_TYPE, Techguns.MODID);
	public static final DeferredRegister<StructurePieceType> PIECE_TYPES = DeferredRegister.create(Registries.STRUCTURE_PIECE, Techguns.MODID);

	public static final DeferredHolder<StructureType<?>, StructureType<DungeonStructure>> DUNGEON = STRUCTURE_TYPES.register("dungeon", () -> () -> DungeonStructure.CODEC);
	public static final DeferredHolder<StructurePieceType, StructurePieceType> DUNGEON_PIECE = PIECE_TYPES.register("dungeon",
			() -> (StructurePieceType.ContextlessType) DungeonPiece::new);
	public static final DeferredHolder<StructureType<?>, StructureType<ProceduralStructure>> MILITARY_BASE = STRUCTURE_TYPES.register("military_base", () -> () -> ProceduralStructure.MILITARY_BASE_CODEC);
	public static final DeferredHolder<StructureType<?>, StructureType<ProceduralStructure>> CITY = STRUCTURE_TYPES.register("city", () -> () -> ProceduralStructure.CITY_CODEC);
	public static final DeferredHolder<StructurePieceType, StructurePieceType> PROCEDURAL_PIECE = PIECE_TYPES.register("procedural",
			() -> (StructurePieceType.ContextlessType) ProceduralStructure.Piece::new);
	public static final DeferredHolder<StructureType<?>, StructureType<AlienBugNest>> ALIEN_BUG_NEST = STRUCTURE_TYPES.register("alien_bug_nest", () -> () -> AlienBugNest.CODEC);
	public static final DeferredHolder<StructurePieceType, StructurePieceType> ALIEN_BUG_NEST_PIECE = PIECE_TYPES.register("alien_bug_nest",
			() -> (StructurePieceType.ContextlessType) AlienBugNest.Piece::new);

	private TGDungeons() {
	}

	public static void register(IEventBus modEventBus) {
		STRUCTURE_TYPES.register(modEventBus);
		PIECE_TYPES.register(modEventBus);
	}
}
