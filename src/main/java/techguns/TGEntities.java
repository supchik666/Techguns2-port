package techguns;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import techguns.entities.projectiles.GenericProjectile;

public class TGEntities {
	public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, Techguns.MODID);

	public static final DeferredHolder<EntityType<?>, EntityType<GenericProjectile>> GENERIC_PROJECTILE = ENTITIES.register("generic_projectile",
			() -> EntityType.Builder.<GenericProjectile>of(GenericProjectile::new, MobCategory.MISC).sized(0.25f, 0.25f)
					.clientTrackingRange(4).updateInterval(20).build("generic_projectile"));
}
