package techguns.items.guns;

import java.util.Optional;
import java.util.function.Supplier;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import techguns.TGSounds;
import techguns.entities.projectiles.GenericProjectile;
import techguns.entities.projectiles.GuidedMissileProjectile;
import techguns.items.guns.ammo.AmmoType;
import techguns.plugins.sable.SableCompat;

/**
 * Hold right click to lock on to what the crosshair stays on; left click then fires a missile that homes in on it
 * (1.12 GuidedMissileLauncher). Releasing right click fires nothing. The lock is tracked on the client only (one
 * local player, so statics) and the target is sent with the shot.
 */
public class GuidedMissileLauncher extends GenericGunCharge {
	public static final float LOCK_RANGE = 150.0f;
	public static final float LOCK_ERROR_THRESHOLD = 0.5f;

	int lockOnTicks;
	int lockOnPersistTicks;

	/** Client only: the entity being locked on to and how far along the lock is (past {@code lockOnTicks} when locked). */
	private static Entity lockTarget;
	private static int lockTicks;

	public GuidedMissileLauncher(AmmoType ammoType, boolean semiAuto, int minFiretime, int clipsize, int reloadtime, float damage,
			Supplier<SoundEvent> fireSound, Supplier<SoundEvent> reloadSound, int ttl, float accuracy, float fullChargeTime,
			int ammoConsumedOnFullCharge) {
		super(ammoType, semiAuto, minFiretime, clipsize, reloadtime, damage, fireSound, reloadSound, ttl, accuracy, fullChargeTime,
				ammoConsumedOnFullCharge);
	}

	/** Ticks on target to lock, and how many more ticks the lock survives looking away. */
	public GuidedMissileLauncher setLockOn(int ticks, int persistTicks) {
		this.lockOnTicks = ticks;
		this.lockOnPersistTicks = persistTicks;
		return this;
	}

	public int getLockOnTicks() {
		return lockOnTicks;
	}

	/** Client: the entity being locked on to, or null. */
	public static Entity lockTarget() {
		return lockTarget;
	}

	/** Client: lock progress, 0..1 while locking, above 1 once locked. */
	public float lockProgress() {
		return (float) lockTicks / lockOnTicks;
	}

	/** Client: the target to send with a shot, once the lock is complete. */
	public Entity lockedTarget() {
		return lockTarget != null && lockTicks > lockOnTicks ? lockTarget : null;
	}

	public boolean isLockedOn() {
		return lockTarget != null && lockTicks >= lockOnTicks;
	}

	private static void resetLock() {
		lockTarget = null;
		lockTicks = 0;
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		if (level.isClientSide) resetLock();
		return super.use(level, player, hand);
	}

	@Override
	public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
		if (level.isClientSide) resetLock();
	}

	@Override
	public void onUseTick(Level level, LivingEntity shooter, ItemStack stack, int count) {
		if (!level.isClientSide) return;
		if (getAmmo(stack) <= 0) {
			resetLock();
			return;
		}
		traceTarget(shooter);
		Player player = shooter instanceof Player p ? p : null;
		if (lockTicks >= lockOnTicks) {
			if (count % 4 == 0) level.playSound(player, shooter, TGSounds.LOCKED_BEEP.get(), SoundSource.PLAYERS, 1.0f, 1.0f);
		} else if (lockTicks > 0) {
			if (count % 4 == 0) level.playSound(player, shooter, TGSounds.LOCKON_BEEP.get(), SoundSource.PLAYERS, 1.0f, 1.0f);
		} else if (count % 12 == 0) {
			level.playSound(player, shooter, TGSounds.LOCKON_BEEP.get(), SoundSource.PLAYERS, 0.5f, 1.0f);
		}
	}

	/** The 1.12 lock state machine: build up on the same target, decay when looking away or at something else. */
	private void traceTarget(LivingEntity shooter) {
		Vec3 start = shooter.getEyePosition();
		Vec3 end = start.add(shooter.getLookAngle().scale(LOCK_RANGE));
		end = SableCompat.toWorld(shooter.level(), shooter.level().clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, shooter)).getLocation());
		Entity hit = findEntityOnPath(shooter, start, end);
		if (hit instanceof Player target && shooter instanceof Player player && !player.canHarmPlayer(target)) hit = null;
		if (hit != null) {
			if (lockTarget != null && lockTarget.isAlive()) {
				if (lockTarget == hit) {
					if (++lockTicks >= lockOnTicks) lockTicks = lockOnTicks + lockOnPersistTicks;
				} else if (lockTicks > 0) {
					lockTicks--;
				} else {
					resetLock();
				}
			} else {
				lockTarget = hit;
				lockTicks = 1;
			}
		} else if (lockTarget != null && !lockTarget.isAlive()) {
			resetLock();
		} else if (lockTicks > 0) {
			lockTicks--;
		} else {
			lockTarget = null;
		}
	}

	/** The closest living entity whose slightly grown box the ray passes through; the current target wins over closer ones. */
	private static Entity findEntityOnPath(LivingEntity shooter, Vec3 start, Vec3 end) {
		Entity found = null;
		double best = Double.MAX_VALUE;
		for (Entity e : shooter.level().getEntities(shooter, shooter.getBoundingBox().expandTowards(end.subtract(start)).inflate(1.0),
				e -> e instanceof LivingEntity && e.isPickable() && !e.isSpectator())) {
			Optional<Vec3> hit = e.getBoundingBox().inflate(LOCK_ERROR_THRESHOLD).clip(start, end);
			if (hit.isEmpty()) continue;
			if (e == lockTarget) return e;
			double d = start.distanceToSqr(hit.get());
			if (d < best) {
				found = e;
				best = d;
			}
		}
		return found;
	}

	@Override
	protected void spawnProjectile(Level level, Player player, GenericProjectile.Kind kind, float spread, float offset, float side, Entity lockedTarget) {
		GenericProjectile missile = createProjectile(level, player, kind, spread, offset, side);
		if (missile instanceof GuidedMissileProjectile guided) guided.setTarget(lockedTarget);
		level.addFreshEntity(missile);
	}
}
