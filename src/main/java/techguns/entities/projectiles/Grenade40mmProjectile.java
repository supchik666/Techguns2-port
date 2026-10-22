package techguns.entities.projectiles;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import techguns.TGEntities;

/** Grenade launcher grenade: a {@link GrenadeProjectile} fired from a gun, bouncing twice, falling with the gun's gravity. */
public class Grenade40mmProjectile extends GrenadeProjectile {
	public Grenade40mmProjectile(EntityType<? extends Grenade40mmProjectile> type, Level level) {
		super(type, level);
	}

	/** {@link techguns.items.guns.ProjectileFactory}. */
	@Override
	protected float speedFactor() {
		return SPEED_FACTOR; // fired from a gun, unlike the thrown grenades
	}

	public Grenade40mmProjectile(Level level, LivingEntity shooter, Kind kind, float damage, float speed, int ticksToLive, float spread,
			float damageDropStart, float damageDropEnd, float damageMin, float side, double gravity) {
		super(TGEntities.GRENADE_40MM_PROJECTILE.get(), level, shooter, kind, damage, speed, ticksToLive, spread, damageDropStart, damageDropEnd, damageMin,
				side, gravity, 2);
	}
}
