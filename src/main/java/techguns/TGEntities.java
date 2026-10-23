package techguns;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import techguns.entities.projectiles.AlienBlasterProjectile;
import techguns.entities.projectiles.BioGunProjectile;
import techguns.entities.projectiles.BlasterProjectile;
import techguns.entities.projectiles.CyberdemonBlasterProjectile;
import techguns.entities.projectiles.DeatomizerProjectile;
import techguns.entities.projectiles.FlamethrowerProjectile;
import techguns.entities.projectiles.FragGrenadeProjectile;
import techguns.entities.projectiles.GaussProjectile;
import techguns.entities.projectiles.GenericProjectile;
import techguns.entities.projectiles.Grenade40mmProjectile;
import techguns.entities.projectiles.GrenadeProjectile;
import techguns.entities.projectiles.GuidedMissileProjectile;
import techguns.entities.projectiles.LaserProjectile;
import techguns.entities.projectiles.NDRProjectile;
import techguns.entities.projectiles.PowerHammerProjectile;
import techguns.entities.projectiles.RocketProjectile;
import techguns.entities.projectiles.SonicShotgunProjectile;
import techguns.entities.projectiles.TFGProjectile;
import techguns.entities.projectiles.TeslaProjectile;
import techguns.entities.special.RadiationEntity;

public class TGEntities {
	public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, Techguns.MODID);

	public static final DeferredHolder<EntityType<?>, EntityType<GenericProjectile>> GENERIC_PROJECTILE = projectile("generic_projectile", GenericProjectile::new);
	public static final DeferredHolder<EntityType<?>, EntityType<LaserProjectile>> LASER_PROJECTILE = projectile("laser_projectile", LaserProjectile::new);
	public static final DeferredHolder<EntityType<?>, EntityType<TeslaProjectile>> TESLA_PROJECTILE = projectile("tesla_projectile", TeslaProjectile::new);
	public static final DeferredHolder<EntityType<?>, EntityType<NDRProjectile>> NDR_PROJECTILE = projectile("ndr_projectile", NDRProjectile::new);
	public static final DeferredHolder<EntityType<?>, EntityType<BlasterProjectile>> BLASTER_PROJECTILE = projectile("blaster_projectile", BlasterProjectile::new);
	public static final DeferredHolder<EntityType<?>, EntityType<AlienBlasterProjectile>> ALIEN_BLASTER_PROJECTILE = projectile("alien_blaster_projectile",
			AlienBlasterProjectile::new);
	public static final DeferredHolder<EntityType<?>, EntityType<DeatomizerProjectile>> DEATOMIZER_PROJECTILE = projectile("deatomizer_projectile",
			DeatomizerProjectile::new);
	public static final DeferredHolder<EntityType<?>, EntityType<CyberdemonBlasterProjectile>> CYBERDEMON_BLASTER_PROJECTILE = projectile(
			"cyberdemon_blaster_projectile", CyberdemonBlasterProjectile::new);
	public static final DeferredHolder<EntityType<?>, EntityType<GaussProjectile>> GAUSS_PROJECTILE = projectile("gauss_projectile", GaussProjectile::new);
	public static final DeferredHolder<EntityType<?>, EntityType<SonicShotgunProjectile>> SONIC_SHOTGUN_PROJECTILE = projectile("sonic_shotgun_projectile",
			SonicShotgunProjectile::new);
	public static final DeferredHolder<EntityType<?>, EntityType<FlamethrowerProjectile>> FLAMETHROWER_PROJECTILE = projectile("flamethrower_projectile",
			FlamethrowerProjectile::new);
	public static final DeferredHolder<EntityType<?>, EntityType<PowerHammerProjectile>> POWERHAMMER_PROJECTILE = projectile("powerhammer_projectile",
			PowerHammerProjectile::new);
	public static final DeferredHolder<EntityType<?>, EntityType<RocketProjectile>> ROCKET_PROJECTILE = projectile("rocket_projectile", RocketProjectile::new);
	public static final DeferredHolder<EntityType<?>, EntityType<GuidedMissileProjectile>> GUIDED_MISSILE_PROJECTILE = projectile("guided_missile_projectile",
			GuidedMissileProjectile::new);
	public static final DeferredHolder<EntityType<?>, EntityType<GrenadeProjectile>> GRENADE_PROJECTILE = projectile("grenade_projectile", GrenadeProjectile::new);
	public static final DeferredHolder<EntityType<?>, EntityType<FragGrenadeProjectile>> FRAG_GRENADE_PROJECTILE = projectile("frag_grenade_projectile",
			FragGrenadeProjectile::new);
	public static final DeferredHolder<EntityType<?>, EntityType<Grenade40mmProjectile>> GRENADE_40MM_PROJECTILE = projectile("grenade_40mm_projectile",
			Grenade40mmProjectile::new);
	public static final DeferredHolder<EntityType<?>, EntityType<TFGProjectile>> TFG_PROJECTILE = projectile("tfg_projectile", TFGProjectile::new);
	public static final DeferredHolder<EntityType<?>, EntityType<BioGunProjectile>> BIOGUN_PROJECTILE = projectile("biogun_projectile", BioGunProjectile::new);
	/** Tracking range 0: never sent to clients. */
	public static final DeferredHolder<EntityType<?>, EntityType<RadiationEntity>> RADIATION = ENTITIES.register("radiation",
			() -> EntityType.Builder.<RadiationEntity>of(RadiationEntity::new, MobCategory.MISC).sized(0.5f, 0.5f).clientTrackingRange(0).build("radiation"));

	// Special mobs (M6.2)
	public static final DeferredHolder<EntityType<?>, EntityType<techguns.entities.npcs.AlienBug>> ALIENBUG = ENTITIES.register("alienbug",
			() -> EntityType.Builder.of(techguns.entities.npcs.AlienBug::new, MobCategory.MONSTER).sized(1.1f, 1.2f).eyeHeight(0.65f).build("alienbug"));
	public static final DeferredHolder<EntityType<?>, EntityType<techguns.entities.npcs.Ghastling>> GHASTLING = ENTITIES.register("ghastling",
			() -> EntityType.Builder.of(techguns.entities.npcs.Ghastling::new, MobCategory.MONSTER).sized(1.0f, 2.1f).eyeHeight(1.5f).fireImmune()
					.build("ghastling"));
	public static final DeferredHolder<EntityType<?>, EntityType<techguns.entities.npcs.AttackHelicopter>> ATTACK_HELICOPTER = ENTITIES.register(
			"attackhelicopter", () -> EntityType.Builder.of(techguns.entities.npcs.AttackHelicopter::new, MobCategory.MONSTER).sized(4.0f, 4.0f)
					.eyeHeight(0.5f).fireImmune().clientTrackingRange(13).build("attackhelicopter"));
	// NPC turret (M6.3)
	public static final DeferredHolder<EntityType<?>, EntityType<techguns.entities.npcs.NPCTurret>> TURRET = ENTITIES.register("turret",
			() -> EntityType.Builder.of(techguns.entities.npcs.NPCTurret::new, MobCategory.MISC).sized(0.6f, 0.9f).eyeHeight(0.75f).build("turret"));

	/** Mod bus listener (see {@link Techguns}). */
	public static void registerAttributes(net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent event) {
		event.put(ALIENBUG.get(), techguns.entities.npcs.AlienBug.createAttributes().build());
		event.put(GHASTLING.get(), techguns.entities.npcs.Ghastling.createAttributes().build());
		event.put(ATTACK_HELICOPTER.get(), techguns.entities.npcs.AttackHelicopter.createAttributes().build());
		event.put(TURRET.get(), techguns.entities.npcs.NPCTurret.createAttributes().build());
	}

	private static <T extends Entity> DeferredHolder<EntityType<?>, EntityType<T>> projectile(String name, EntityType.EntityFactory<T> factory) {
		return ENTITIES.register(name, () -> EntityType.Builder.of(factory, MobCategory.MISC).sized(0.25f, 0.25f)
				.clientTrackingRange(4).updateInterval(20).build(name));
	}
}
