package techguns.items.guns;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import techguns.entities.projectiles.GenericProjectile;

/**
 * What a charge gun or a thrown grenade fires (1.12 IChargedProjectileFactory / IGrenadeProjectileFactory): a
 * {@link ProjectileFactory} plus the charge (0..1) and the ammo used. Explosives read damageDropStart/End as their
 * radii, as in 1.12.
 */
@FunctionalInterface
public interface ChargedProjectileFactory {
	GenericProjectile create(Level level, LivingEntity shooter, GenericProjectile.Kind kind, float damage, float speed, int ticksToLive,
			float spread, float damageDropStart, float damageDropEnd, float damageMin, float side, double gravity, float charge, int ammoConsumed);
}
