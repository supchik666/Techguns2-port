package techguns;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import techguns.entities.projectiles.AlienBlasterProjectile;
import techguns.entities.projectiles.BlasterProjectile;
import techguns.entities.projectiles.CyberdemonBlasterProjectile;
import techguns.entities.projectiles.DeatomizerProjectile;
import techguns.entities.projectiles.FlamethrowerProjectile;
import techguns.entities.projectiles.GaussProjectile;
import techguns.entities.projectiles.GenericProjectile;
import techguns.entities.projectiles.LaserProjectile;
import techguns.entities.projectiles.NDRProjectile;
import techguns.entities.projectiles.SonicShotgunProjectile;
import techguns.entities.projectiles.TeslaProjectile;

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

	private static <T extends Entity> DeferredHolder<EntityType<?>, EntityType<T>> projectile(String name, EntityType.EntityFactory<T> factory) {
		return ENTITIES.register(name, () -> EntityType.Builder.of(factory, MobCategory.MISC).sized(0.25f, 0.25f)
				.clientTrackingRange(4).updateInterval(20).build(name));
	}
}
