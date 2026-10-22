package techguns.entities.projectiles;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import techguns.TGEntities;

/** Thrown frag grenade: a {@link GrenadeProjectile} that bounces twice. */
public class FragGrenadeProjectile extends GrenadeProjectile {
	public FragGrenadeProjectile(EntityType<? extends FragGrenadeProjectile> type, Level level) {
		super(type, level);
	}

	/** {@link techguns.items.guns.ChargedProjectileFactory}, as {@link GrenadeProjectile}. */
	public FragGrenadeProjectile(Level level, LivingEntity shooter, Kind kind, float damage, float speed, int ticksToLive, float spread,
			float damageDropStart, float damageDropEnd, float damageMin, float side, double gravity, float charge, int ammoConsumed) {
		super(TGEntities.FRAG_GRENADE_PROJECTILE.get(), level, shooter, kind, damage, speed, ticksToLive, spread, damageDropStart, damageDropEnd, damageMin,
				side, 0.015 / Math.max(charge, 0.01f), 2);
	}
}
