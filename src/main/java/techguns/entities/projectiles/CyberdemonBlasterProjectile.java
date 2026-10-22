package techguns.entities.projectiles;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import techguns.TGEntities;

/** Nether blaster bolt: fire damage, doesn't ignite. */
public class CyberdemonBlasterProjectile extends BlasterProjectile {
	public CyberdemonBlasterProjectile(EntityType<? extends CyberdemonBlasterProjectile> type, Level level) {
		super(type, level);
	}

	public CyberdemonBlasterProjectile(Level level, LivingEntity shooter, Kind kind, float damage, float speed, int ticksToLive, float spread,
			float damageDropStart, float damageDropEnd, float damageMin, float side, double gravity) {
		super(TGEntities.CYBERDEMON_BLASTER_PROJECTILE.get(), level, shooter, kind, damage, speed, ticksToLive, spread, damageDropStart, damageDropEnd,
				damageMin, side, gravity);
	}

	@Override
	protected String trail() {
		return "CyberDemonBlasterTrail";
	}

	@Override
	protected String impact() {
		return "CyberdemonBlasterImpact";
	}

	@Override
	protected void onHitEntity(EntityHitResult result) {
		fireHit(result, 0);
	}
}
