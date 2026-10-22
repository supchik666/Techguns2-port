package techguns.items.guns;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import techguns.entities.projectiles.GenericProjectile;

/** What a gun fires (1.12 IProjectileFactory); the constructor of a {@link GenericProjectile} subclass fits. */
@FunctionalInterface
public interface ProjectileFactory {
	GenericProjectile create(Level level, LivingEntity shooter, GenericProjectile.Kind kind, float damage, float speed, int ticksToLive,
			float spread, float damageDropStart, float damageDropEnd, float damageMin, float side, double gravity);
}
