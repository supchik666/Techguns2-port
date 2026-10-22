package techguns.entities.projectiles;

import javax.annotation.Nullable;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import techguns.TGEffects;
import techguns.TGEntities;
import techguns.damagesystem.TGExplosion;

/**
 * Guided missile: a rocket that turns up to 9 degrees a tick towards its lock-on target (set before spawning, sent to
 * clients so they steer the same way). Kind HIGH_VELOCITY is the 1.12 GuidedMissileProjectileHV.
 */
public class GuidedMissileProjectile extends RocketProjectile {
	private static final double MAX_TURN_ANGLE = 9.0 * Mth.DEG_TO_RAD;
	/** Unguided missiles drop a little, as in 1.12. */
	private static final double UNGUIDED_GRAVITY = 0.01;

	@Nullable
	private Entity target;

	public GuidedMissileProjectile(EntityType<? extends GuidedMissileProjectile> type, Level level) {
		super(type, level);
	}

	private GuidedMissileProjectile(Level level, LivingEntity shooter, Kind kind, float damage, float speed, int ticksToLive, float spread,
			float damageDropStart, float damageDropEnd, float damageMin, float side) {
		super(TGEntities.GUIDED_MISSILE_PROJECTILE.get(), level, shooter, kind, damage, speed, ticksToLive, spread, damageDropStart, damageDropEnd,
				damageMin, side, UNGUIDED_GRAVITY);
	}

	/**
	 * {@link techguns.items.guns.ChargedProjectileFactory}; charge and the gun's gravity are ignored (1.12). HIGH_VELOCITY
	 * applies the 1.12 DamageModifier. Call {@link #setTarget} before spawning it.
	 */
	public static GenericProjectile create(Level level, LivingEntity shooter, Kind kind, float damage, float speed, int ticksToLive, float spread,
			float damageDropStart, float damageDropEnd, float damageMin, float side, double gravity, float charge, int ammoConsumed) {
		if (kind == Kind.HIGH_VELOCITY) {
			return new GuidedMissileProjectile(level, shooter, kind, damage, speed * 2, Math.round(ticksToLive * 0.75f), spread, damageDropStart * 0.75f,
					damageDropEnd * 0.75f, damageMin, side);
		}
		return new GuidedMissileProjectile(level, shooter, kind, damage, speed, ticksToLive, spread, damageDropStart, damageDropEnd, damageMin, side);
	}

	/** The lock-on target to home in on, or null to fly unguided. Before the missile is spawned. */
	public void setTarget(@Nullable Entity target) {
		this.target = target;
		this.gravity = target != null ? 0 : UNGUIDED_GRAVITY;
	}

	@Override
	public void tick() {
		if (target != null) {
			Vec3 motion = getDeltaMovement();
			double speed = motion.length();
			Vec3 v1 = motion.normalize();
			Vec3 v2 = new Vec3(target.getX(), target.getY(0.5), target.getZ()).subtract(position()).normalize();
			double angle = Math.acos(Mth.clamp(v1.dot(v2), -1, 1));
			if (angle < MAX_TURN_ANGLE) {
				motion = v2.scale(speed);
			} else {
				// rotate v1 by MAX_TURN_ANGLE about the axis normal to v1 and v2
				Vec3 axis = v1.cross(v2).normalize();
				motion = v1.scale(Math.cos(MAX_TURN_ANGLE)).add(axis.cross(v1).scale(Math.sin(MAX_TURN_ANGLE))).scale(speed);
			}
			setDeltaMovement(motion);
		}
		super.tick();
	}

	@Override
	protected String trail() {
		return "GuidedMissileExhaust";
	}

	@Override
	protected void explode(ServerLevel server) {
		TGEffects.sendFX(server, "GuidedMissileExplosion", getX(), getY(), getZ(), 1.0f, 50);
		new TGExplosion(server, getOwner(), this, getX(), getY(), getZ(), damage, damageMin, damageDropStart, damageDropEnd,
				TGExplosion.blockDamage(getOwner()) ? 0.25 : 0.0).doExplosion(true);
	}

	@Override
	public void writeSpawnData(RegistryFriendlyByteBuf buffer) {
		super.writeSpawnData(buffer);
		buffer.writeInt(target != null ? target.getId() : -1);
	}

	@Override
	public void readSpawnData(RegistryFriendlyByteBuf buffer) {
		super.readSpawnData(buffer);
		target = level().getEntity(buffer.readInt());
	}
}
