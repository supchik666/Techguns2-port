package techguns.entities.projectiles;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import techguns.TGEntities;

/** Alien blaster bolt: fire damage, sets the target alight. ponytail: no block burning until the M4 block damage config. */
public class AlienBlasterProjectile extends BlasterProjectile {
	public AlienBlasterProjectile(EntityType<? extends AlienBlasterProjectile> type, Level level) {
		super(type, level);
	}

	public AlienBlasterProjectile(Level level, LivingEntity shooter, Kind kind, float damage, float speed, int ticksToLive, float spread,
			float damageDropStart, float damageDropEnd, float damageMin, float side, double gravity) {
		super(TGEntities.ALIEN_BLASTER_PROJECTILE.get(), level, shooter, kind, damage, speed, ticksToLive, spread, damageDropStart, damageDropEnd, damageMin,
				side, gravity);
	}

	@Override
	protected String trail() {
		return "AlienBlasterTrail";
	}

	@Override
	protected String impact() {
		return "AlienExplosion";
	}

	@Override
	protected void onHitEntity(EntityHitResult result) {
		fireHit(result, 3);
	}
}
