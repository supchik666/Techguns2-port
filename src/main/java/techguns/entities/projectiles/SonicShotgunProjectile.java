package techguns.entities.projectiles;

import java.util.HashSet;
import java.util.Set;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import techguns.TGEffects;
import techguns.TGEntities;

/**
 * One wave of a sonic shotgun shot (the gun fires a center one and two rings). Goes through entities, hitting each
 * one once per shot with double knockback; a block stops it.
 */
public class SonicShotgunProjectile extends GenericProjectile {
	/** Shared by all projectiles of one shot, so a target is hit only once. */
	public Set<Entity> entitiesHit = new HashSet<>();
	/** The center projectile: the only one drawn, with the trail and impact effects. */
	public boolean mainProjectile;

	public SonicShotgunProjectile(EntityType<? extends SonicShotgunProjectile> type, Level level) {
		super(type, level);
	}

	public SonicShotgunProjectile(Level level, LivingEntity shooter, Kind kind, float damage, float speed, int ticksToLive, float spread,
			float damageDropStart, float damageDropEnd, float damageMin, float side, double gravity) {
		super(TGEntities.SONIC_SHOTGUN_PROJECTILE.get(), level, shooter, kind, damage, speed, ticksToLive, spread, damageDropStart, damageDropEnd, damageMin,
				side, gravity);
	}

	@Override
	public void tick() {
		if (!level().isClientSide) {
			Vec3 motion = getDeltaMovement(), from = position(), to = from.add(motion);
			for (Entity target : level().getEntities(this, getBoundingBox().expandTowards(motion).inflate(1.0), super::canHitEntity)) {
				if (target.getBoundingBox().inflate(0.3).clip(from, to).isPresent() && entitiesHit.add(target)) hit(target, motion);
			}
		}
		super.tick();
	}

	private void hit(Entity target, Vec3 motion) {
		if (target.hurt(damageSources().source(BULLET, this, getOwner()), getDamage()) && target instanceof LivingEntity living) {
			living.knockback(0.8, -motion.x, -motion.z); // 1.12 knockback x2
		}
	}

	/** Entities are hit in {@link #tick} without stopping it; only blocks end it. */
	@Override
	protected boolean canHitEntity(Entity target) {
		return false;
	}

	@Override
	protected void onHitBlock(BlockHitResult result) {
		if (mainProjectile && level() instanceof ServerLevel server) TGEffects.sendFX(server, "SonicShotgunImpact", getX(), getY(), getZ(), 1.0f, 50);
	}

	@Override
	public void writeSpawnData(RegistryFriendlyByteBuf buffer) {
		super.writeSpawnData(buffer);
		buffer.writeBoolean(mainProjectile);
	}

	@Override
	public void readSpawnData(RegistryFriendlyByteBuf buffer) {
		super.readSpawnData(buffer);
		mainProjectile = buffer.readBoolean();
		if (mainProjectile) TGEffects.createFXOnEntity(this, "SonicShotgunTrail"); // 1.12 sent this from the gun
	}
}
