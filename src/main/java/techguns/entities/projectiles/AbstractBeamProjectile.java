package techguns.entities.projectiles;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.EventHooks;
import techguns.plugins.sable.SableCompat;

/**
 * A hitscan beam: {@link #trace} hits along the whole range (the motion vector) at once, then the entity stays put for
 * {@link #maxTicks} so the renderer can draw it. Length and exact direction go to the client in the spawn data.
 */
public abstract class AbstractBeamProjectile extends GenericProjectile {
	/** Beam length: to what it hit, or the full range. */
	public double distance;
	public int maxTicks;

	protected AbstractBeamProjectile(EntityType<? extends AbstractBeamProjectile> type, Level level) {
		super(type, level);
	}

	protected AbstractBeamProjectile(EntityType<? extends AbstractBeamProjectile> type, Level level, LivingEntity shooter, Kind kind, float damage,
			float range, int ticksToLive, float spread, float damageDropStart, float damageDropEnd, float damageMin, float side) {
		super(type, level, shooter, kind, damage, range, ticksToLive, spread, damageDropStart, damageDropEnd, damageMin, side, 0);
		maxTicks = ticksToLive;
	}

	@Override
	protected float speedFactor() {
		return 1; // speed is the beam's range
	}

	/** Hits the first block or entity along the motion vector and sets {@link #distance} to it. */
	protected void trace() {
		Vec3 from = position(), to = from.add(getDeltaMovement());
		HitResult hit = SableCompat.toWorld(level(), ProjectileUtil.getHitResultOnMoveVector(this, this::canHitEntity));
		Vec3 end = hit instanceof EntityHitResult e ? e.getEntity().getBoundingBox().inflate(0.3).clip(from, to).orElse(to) : hit.getLocation();
		distance = from.distanceTo(end);
		if (hit.getType() != HitResult.Type.MISS) {
			start = end; // getDamage() drops over position→start, which for a beam is its length
			if (!EventHooks.onProjectileImpact(this, hit)) onHit(hit);
		}
	}

	/** Unit vector along the beam, from the projectile rotation (yaw = atan2(x, z), pitch up positive). */
	public Vec3 direction() {
		return Vec3.directionFromRotation(-getXRot(), -getYRot());
	}

	@Override
	public void tick() {
		if (!level().isClientSide && tickCount >= maxTicks) discard();
	}

	@Override
	public AABB getBoundingBoxForCulling() {
		return getBoundingBox().expandTowards(direction().scale(distance));
	}

	@Override
	public boolean shouldRenderAtSqrDistance(double distance) {
		return true; // a long beam from a tiny entity; the tracking range limits it
	}

	@Override
	public void writeSpawnData(RegistryFriendlyByteBuf buffer) {
		super.writeSpawnData(buffer);
		buffer.writeDouble(distance);
		buffer.writeFloat(getYRot()); // the add-entity packet only has 1.4° steps, too coarse for a 100 block beam
		buffer.writeFloat(getXRot());
		buffer.writeVarInt(maxTicks);
	}

	@Override
	public void readSpawnData(RegistryFriendlyByteBuf buffer) {
		super.readSpawnData(buffer);
		distance = buffer.readDouble();
		setYRot(buffer.readFloat());
		setXRot(buffer.readFloat());
		maxTicks = buffer.readVarInt();
	}
}
