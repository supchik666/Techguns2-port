package techguns.entities.projectiles;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import techguns.TGEffects;
import techguns.TGEntities;
import techguns.damagesystem.TGDamageSource;
import techguns.damagesystem.TGDamageSource.DeathType;
import techguns.damagesystem.TGDamageType;

/** Laser gun/pistol beam; lives 7 ticks whatever the gun's TTL. */
public class LaserProjectile extends AbstractBeamProjectile {
	public LaserProjectile(EntityType<? extends LaserProjectile> type, Level level) {
		super(type, level);
	}

	public LaserProjectile(Level level, LivingEntity shooter, Kind kind, float damage, float speed, int ticksToLive, float spread, float damageDropStart,
			float damageDropEnd, float damageMin, float side, double gravity) {
		super(TGEntities.LASER_PROJECTILE.get(), level, shooter, kind, damage, speed, 7, spread, damageDropStart, damageDropEnd, damageMin, side);
		trace();
	}

	@Override
	protected void onHitBlock(BlockHitResult result) {
		Vec3 pos = result.getLocation();
		if (level() instanceof ServerLevel server) TGEffects.sendFX(server, "LaserGunImpact", pos.x, pos.y, pos.z, 1.0f, 35);
	}

	@Override
	protected TGDamageSource damageSource() {
		return source(TGDamageType.ENERGY, DeathType.LASER);
	}
}
