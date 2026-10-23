package techguns.machines.multiblock;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import techguns.TGBlocks;
import techguns.Techguns;
import techguns.recipes.ReactionChamberRecipe;

/**
 * Registrations of the multiblock machines (1.12 MultiBlockRegister): their block entity types, the reaction chamber recipe
 * type, and capabilities. Parts forward a capability to their controller by {@link MultiBlockMachine#partCapability}.
 * Kept apart from TGBlockEntities, whose capability loop expects every type to be a single-block machine.
 */
public final class MultiBlocks {
	public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Techguns.MODID);
	public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES = DeferredRegister.create(Registries.RECIPE_TYPE, Techguns.MODID);
	public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS = DeferredRegister.create(Registries.RECIPE_SERIALIZER, Techguns.MODID);

	@SuppressWarnings("DataFlowIssue")
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FabricatorBlockEntity>> FABRICATOR = BLOCK_ENTITIES.register("fabricator",
			() -> BlockEntityType.Builder.of(FabricatorBlockEntity::new, TGBlocks.FABRICATOR_CONTROLLER.get()).build(null));
	@SuppressWarnings("DataFlowIssue")
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ReactionChamberBlockEntity>> REACTION_CHAMBER = BLOCK_ENTITIES.register(
			"reaction_chamber", () -> BlockEntityType.Builder.of(ReactionChamberBlockEntity::new, TGBlocks.REACTION_CHAMBER_CONTROLLER.get()).build(null));
	@SuppressWarnings("DataFlowIssue")
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<OreDrillBlockEntity>> ORE_DRILL = BLOCK_ENTITIES.register("ore_drill",
			() -> BlockEntityType.Builder.of(OreDrillBlockEntity::new, TGBlocks.ORE_DRILL_CONTROLLER.get()).build(null));
	@SuppressWarnings("DataFlowIssue")
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MultiBlockSlave>> PART = BLOCK_ENTITIES.register("multiblock_part",
			() -> BlockEntityType.Builder.of(MultiBlockSlave::new, TGBlocks.FABRICATOR_HOUSING.get(), TGBlocks.FABRICATOR_GLASS.get(),
					TGBlocks.REACTION_CHAMBER_HOUSING.get(), TGBlocks.REACTION_CHAMBER_GLASS.get(), TGBlocks.ORE_DRILL_FRAME.get(),
					TGBlocks.ORE_DRILL_SCAFFOLD.get(), TGBlocks.ORE_DRILL_ROD.get(), TGBlocks.ORE_DRILL_ENGINE.get()).build(null));

	public static final DeferredHolder<RecipeType<?>, RecipeType<ReactionChamberRecipe>> REACTION_CHAMBER_RECIPE = RECIPE_TYPES.register(
			"reaction_chamber", () -> RecipeType.simple(Techguns.id("reaction_chamber")));
	public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<ReactionChamberRecipe>> REACTION_CHAMBER_SERIALIZER = SERIALIZERS.register(
			"reaction_chamber", () -> new RecipeSerializer<>() {
				@Override
				public MapCodec<ReactionChamberRecipe> codec() {
					return ReactionChamberRecipe.CODEC;
				}

				@Override
				public StreamCodec<RegistryFriendlyByteBuf, ReactionChamberRecipe> streamCodec() {
					return ReactionChamberRecipe.STREAM_CODEC;
				}
			});

	private MultiBlocks() {}

	public static void register(IEventBus bus) {
		BLOCK_ENTITIES.register(bus);
		RECIPE_TYPES.register(bus);
		SERIALIZERS.register(bus);
		bus.addListener(MultiBlocks::registerCapabilities);
	}

	private static void registerCapabilities(RegisterCapabilitiesEvent event) {
		for (var type : new BlockEntityType[] {FABRICATOR.get(), REACTION_CHAMBER.get(), ORE_DRILL.get()}) {
			@SuppressWarnings("unchecked")
			BlockEntityType<MultiBlockMachine> machine = type;
			event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, machine, (be, side) -> be.automation);
			event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, machine, (be, side) -> be.energy);
		}
		event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, REACTION_CHAMBER.get(), (be, side) -> be.tank);
		event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, ORE_DRILL.get(), (be, side) -> be.tank);
		forward(event, Capabilities.ItemHandler.BLOCK);
		forward(event, Capabilities.EnergyStorage.BLOCK);
		forward(event, Capabilities.FluidHandler.BLOCK);
	}

	private static <T> void forward(RegisterCapabilitiesEvent event, BlockCapability<T, net.minecraft.core.Direction> cap) {
		event.registerBlockEntity(cap, PART.get(), (part, side) -> {
			MultiBlockMachine master = part.master();
			return master != null ? master.partCapability(cap, part.type) : null;
		});
	}
}
