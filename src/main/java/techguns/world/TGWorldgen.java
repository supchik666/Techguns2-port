package techguns.world;

import java.util.function.Supplier;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import techguns.TGBlocks;
import techguns.Techguns;

/**
 * World generation (M7.1): the TG spawner and the structure type of the 1.12 scanned structures. The structures, structure sets
 * and ore features themselves are data: tools/port/port_structures.py and src/main/resources/data/techguns/worldgen.
 */
public class TGWorldgen {
	static final Logger LOGGER = LogUtils.getLogger();
	private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Techguns.MODID);
	private static final DeferredRegister<StructureType<?>> STRUCTURE_TYPES = DeferredRegister.create(Registries.STRUCTURE_TYPE, Techguns.MODID);
	private static final DeferredRegister<StructurePieceType> PIECES = DeferredRegister.create(Registries.STRUCTURE_PIECE, Techguns.MODID);
	private static final DeferredRegister<AttachmentType<?>> ATTACHMENTS = DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, Techguns.MODID);

	@SuppressWarnings("DataFlowIssue")
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TGSpawnerBlockEntity>> TG_SPAWNER = BLOCK_ENTITIES.register("tg_spawner",
			() -> BlockEntityType.Builder.of(TGSpawnerBlockEntity::new, TGBlocks.HOLE.get(), TGBlocks.SOLDIER_SPAWN.get()).build(null));
	public static final DeferredHolder<StructureType<?>, StructureType<TemplateStructure>> TEMPLATE = STRUCTURE_TYPES.register("template",
			() -> () -> TemplateStructure.CODEC);
	public static final DeferredHolder<StructurePieceType, StructurePieceType> TEMPLATE_PIECE = PIECES.register("template",
			() -> TemplateStructure.Piece::new);
	/** Position of the spawner a mob came from (1.12 TGSpawnerNPCData). */
	public static final Supplier<AttachmentType<BlockPos>> SPAWNER = ATTACHMENTS.register("spawner",
			() -> AttachmentType.builder(() -> BlockPos.ZERO).serialize(BlockPos.CODEC).build());

	public static void register(IEventBus modBus) {
		BLOCK_ENTITIES.register(modBus);
		STRUCTURE_TYPES.register(modBus);
		PIECES.register(modBus);
		ATTACHMENTS.register(modBus);
		NeoForge.EVENT_BUS.addListener(TGWorldgen::onDeath);
	}

	private static void onDeath(LivingDeathEvent event) {
		LivingEntity mob = event.getEntity();
		if (mob.level().isClientSide || !mob.hasData(SPAWNER)) return;
		BlockPos pos = mob.getData(SPAWNER);
		if (mob.level().isLoaded(pos) && mob.level().getBlockEntity(pos) instanceof TGSpawnerBlockEntity spawner) spawner.killed(mob);
	}
}
