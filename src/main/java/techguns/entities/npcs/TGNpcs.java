package techguns.entities.npcs;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import techguns.TGEntities;
import techguns.TGItems;
import techguns.Techguns;

/**
 * NPC entity types with their spawn eggs (1.12 colors), attributes and spawn rules (the NPC part of 1.12 TGEntities).
 * Ids are the lowercased 1.12 names. Natural spawning goes through {@link TGSpawnManager}.
 */
@EventBusSubscriber(modid = Techguns.MODID, bus = EventBusSubscriber.Bus.MOD)
public class TGNpcs {
	/** Built in the event: attributes aren't bound while the mod constructs. */
	private static final Map<Supplier<? extends EntityType<? extends Monster>>, Supplier<AttributeSupplier.Builder>> ATTRIBUTES = new LinkedHashMap<>();

	public static final DeferredHolder<EntityType<?>, EntityType<ArmySoldier>> ARMY_SOLDIER = npc("armysoldier", humanoid(ArmySoldier::new),
			0x74806e, 0x191512, () -> GenericNPC.attributes(0.30, 25, 4, 75));
	public static final DeferredHolder<EntityType<?>, EntityType<Bandit>> BANDIT = npc("bandit", humanoid(Bandit::new),
			0x8f9d59, 0x2c3117, () -> GenericNPC.attributes(0.30, 20, 5, 40));
	public static final DeferredHolder<EntityType<?>, EntityType<Commando>> COMMANDO = npc("commando", humanoid(Commando::new),
			0x191512, 0x74806e, () -> GenericNPC.attributes(0.35, 30, 5, 75));
	public static final DeferredHolder<EntityType<?>, EntityType<CyberDemon>> CYBER_DEMON = npc("cyberdemon", humanoid(CyberDemon::new).fireImmune(),
			0xFF1111, 0x777777, () -> GenericNPC.attributes(0.35, 30, 7, 50).add(Attributes.ARMOR_TOUGHNESS, 1));
	public static final DeferredHolder<EntityType<?>, EntityType<DictatorDave>> DICTATOR_DAVE = npc("dictatordave", humanoid(DictatorDave::new),
			0x8f9d59, 0xA0A000, () -> GenericNPC.attributes(0.40, 100, 7, 60).add(Attributes.ARMOR_TOUGHNESS, 2));
	public static final DeferredHolder<EntityType<?>, EntityType<Outcast>> OUTCAST = npc("outcast", humanoid(Outcast::new),
			0x101010, 0xFF0000, () -> GenericNPC.attributes(0.2, 30, 8, 60));
	public static final DeferredHolder<EntityType<?>, EntityType<PsychoSteve>> PSYCHO_STEVE = npc("psychosteve", humanoid(PsychoSteve::new),
			0x757468, 0xF0F0F0, () -> GenericNPC.attributes(0.60, 75, 7, 60).add(Attributes.ARMOR_TOUGHNESS, 1));
	public static final DeferredHolder<EntityType<?>, EntityType<SkeletonSoldier>> SKELETON_SOLDIER = npc("skeletonsoldier",
			humanoid(SkeletonSoldier::new), 0x404040, 0xF0F0F0, () -> GenericNPC.attributes(0.25, 25, 4, 50));
	public static final DeferredHolder<EntityType<?>, EntityType<StormTrooper>> STORM_TROOPER = npc("stormtrooper", humanoid(StormTrooper::new),
			0xF0F0F0, 0x404040, () -> GenericNPC.attributes(0.30, 35, 4, 75));
	public static final DeferredHolder<EntityType<?>, EntityType<SuperMutant>> SUPER_MUTANT_BASIC = npc("supermutantbasic",
			EntityType.Builder.<SuperMutant>of((type, level) -> new SuperMutant(type, level, 0), MobCategory.MONSTER).sized(1.0f, 2.7f),
			0xc6a96b, 0x71552e, () -> GenericNPC.attributes(0.30, 35, 7, 50).add(Attributes.ARMOR_TOUGHNESS, 1));
	public static final DeferredHolder<EntityType<?>, EntityType<SuperMutant>> SUPER_MUTANT_HEAVY = npc("supermutantheavy",
			EntityType.Builder.<SuperMutant>of((type, level) -> new SuperMutant(type, level, 1), MobCategory.MONSTER).sized(1.2f, 3.0f),
			0xc6a96b, 0x51350e, () -> GenericNPC.attributes(0.25, 45, 9, 50).add(Attributes.ARMOR_TOUGHNESS, 2));
	public static final DeferredHolder<EntityType<?>, EntityType<SuperMutant>> SUPER_MUTANT_ELITE = npc("supermutantelite",
			EntityType.Builder.<SuperMutant>of((type, level) -> new SuperMutant(type, level, 2), MobCategory.MONSTER).sized(1.4f, 3.3f),
			0xc6a96b, 0x61451e, () -> GenericNPC.attributes(0.20, 60, 11, 50).add(Attributes.ARMOR_TOUGHNESS, 2.5));
	public static final DeferredHolder<EntityType<?>, EntityType<ZombieFarmer>> ZOMBIE_FARMER = npc("zombiefarmer", humanoid(ZombieFarmer::new),
			0x757468, 0x38B038, () -> GenericNPC.attributes(0.20, 18, 3, 50));
	public static final DeferredHolder<EntityType<?>, EntityType<ZombieMiner>> ZOMBIE_MINER = npc("zombieminer", humanoid(ZombieMiner::new),
			0x757468, 0x38B038, () -> GenericNPC.attributes(0.20, 20, 4, 50));
	public static final DeferredHolder<EntityType<?>, EntityType<ZombiePigmanSoldier>> ZOMBIE_PIGMAN_SOLDIER = npc("zombiepigmansoldier",
			humanoid(ZombiePigmanSoldier::new).fireImmune(), 0xFF1111, 0x770000, () -> GenericNPC.attributes(0.30, 25, 5, 50));
	public static final DeferredHolder<EntityType<?>, EntityType<ZombiePoliceman>> ZOMBIE_POLICEMAN = npc("zombiepoliceman",
			humanoid(ZombiePoliceman::new), 0x303030, 0x0000FF, () -> GenericNPC.attributes(0.25, 25, 4, 50));
	public static final DeferredHolder<EntityType<?>, EntityType<ZombieSoldier>> ZOMBIE_SOLDIER = npc("zombiesoldier", humanoid(ZombieSoldier::new),
			0x757468, 0x38B038, () -> GenericNPC.attributes(0.25, 25, 4, 50));
	/** Spawned naturally, then swapped for an NPC by {@link TGSpawnManager}; never in the world. */
	public static final DeferredHolder<EntityType<?>, EntityType<TGSpawnManager.Dummy>> DUMMY_SPAWN = register("tgdummyspawn",
			humanoid(TGSpawnManager.Dummy::new), Monster::createMonsterAttributes);

	public static void init() {}

	/** Zombie sized. */
	private static <T extends Monster> EntityType.Builder<T> humanoid(EntityType.EntityFactory<T> factory) {
		return EntityType.Builder.of(factory, MobCategory.MONSTER).sized(0.6f, 1.95f).eyeHeight(1.74f);
	}

	private static <T extends Monster> DeferredHolder<EntityType<?>, EntityType<T>> npc(String name, EntityType.Builder<T> builder, int eggColor,
			int eggSpots, Supplier<AttributeSupplier.Builder> attributes) {
		DeferredHolder<EntityType<?>, EntityType<T>> type = register(name, builder, attributes);
		TGItems.ITEMS.register(name + "_spawn_egg", () -> new DeferredSpawnEggItem(type, eggColor, eggSpots, new Item.Properties()));
		return type;
	}

	private static <T extends Monster> DeferredHolder<EntityType<?>, EntityType<T>> register(String name, EntityType.Builder<T> builder,
			Supplier<AttributeSupplier.Builder> attributes) {
		DeferredHolder<EntityType<?>, EntityType<T>> type = TGEntities.ENTITIES.register(name,
				() -> builder.clientTrackingRange(5).updateInterval(3).build(name));
		ATTRIBUTES.put(type, attributes);
		return type;
	}

	@SubscribeEvent
	static void attributes(EntityAttributeCreationEvent event) {
		ATTRIBUTES.forEach((type, attributes) -> event.put(type.get(), attributes.get().build()));
	}

	@SubscribeEvent
	static void spawnPlacements(RegisterSpawnPlacementsEvent event) {
		ATTRIBUTES.keySet().forEach(type -> placeOnGround(event, type.get()));
	}

	private static <T extends Monster> void placeOnGround(RegisterSpawnPlacementsEvent event, EntityType<T> type) {
		event.register(type, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Monster::checkMonsterSpawnRules,
				RegisterSpawnPlacementsEvent.Operation.REPLACE);
	}
}
