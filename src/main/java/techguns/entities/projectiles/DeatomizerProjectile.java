package techguns.entities.projectiles;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import techguns.TGEntities;

/** MIB gun (deatomizer) bolt. */
public class DeatomizerProjectile extends BlasterProjectile {
	public DeatomizerProjectile(EntityType<? extends DeatomizerProjectile> type, Level level) {
		super(type, level);
	}

	public DeatomizerProjectile(Level level, LivingEntity shooter, Kind kind, float damage, float speed, int ticksToLive, float spread,
			float damageDropStart, float damageDropEnd, float damageMin, float side, double gravity) {
		super(TGEntities.DEATOMIZER_PROJECTILE.get(), level, shooter, kind, damage, speed, ticksToLive, spread, damageDropStart, damageDropEnd, damageMin,
				side, gravity);
	}

	@Override
	protected String trail() {
		return "BlueBlasterTrail";
	}

	@Override
	protected String impact() {
		return "BlueBlasterExplosion";
	}
}
