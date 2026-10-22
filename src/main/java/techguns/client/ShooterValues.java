package techguns.client;

import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ThreadLocalRandom;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import techguns.items.guns.GenericGun;

/**
 * Client-side animation timers per shooter and hand (the 1.12 AttackTime). Wall-clock ms, so animations run smoothly
 * between ticks. Only touched from the client thread.
 */
public class ShooterValues {
	public static class AttackTime {
		public long recoilEnd, reloadEnd, muzzleFlashEnd;
		public int recoilTotal, reloadTotal, muzzleFlashTotal;
		public float jitterX, jitterY, jitterScale;
		/** How charged the last shot was; a charge gun's recoil eases back from it. */
		public float recoilCharge;
	}

	private static final Map<Entity, AttackTime[]> TIMES = new WeakHashMap<>();

	public static AttackTime get(Entity entity, InteractionHand hand) {
		return TIMES.computeIfAbsent(entity, e -> new AttackTime[] {new AttackTime(), new AttackTime()})[hand.ordinal()];
	}

	/** Restarts recoil and muzzle flash, unless the gun lets a running one finish (1.12 only checked the local first-person player). */
	public static void fired(Entity entity, InteractionHand hand, GenericGun gun) {
		AttackTime time = get(entity, hand);
		long now = System.currentTimeMillis();
		if (!gun.isCheckRecoil() || now >= time.recoilEnd) {
			time.recoilTotal = gun.getRecoiltime() * 50;
			time.recoilEnd = now + time.recoilTotal;
			time.recoilCharge = 0.0f;
		}
		float[] light = gun.getMuzzleLight();
		if (light != null) {
			// a gun that lets its flash run (flamethrower, minigun) holds one light instead of blinking with each flash
			if (gun.isCheckMuzzleFlash()) LightPulses.steadyMuzzleFlash(entity, light);
			else LightPulses.muzzleFlash(entity, light);
		}
		if (gun.isCheckMuzzleFlash() && now < time.muzzleFlashEnd) return;
		time.muzzleFlashTotal = gun.getMuzzleFlashtime() * 50;
		time.muzzleFlashEnd = now + time.muzzleFlashTotal;
		ThreadLocalRandom random = ThreadLocalRandom.current();
		time.jitterX = 1.0f - random.nextFloat() * 2.0f;
		time.jitterY = 1.0f - random.nextFloat() * 2.0f;
		time.jitterScale = 1.0f - random.nextFloat() * 2.0f;
	}

	public static void reloaded(Entity entity, InteractionHand hand, int reloadTicks) {
		AttackTime time = get(entity, hand);
		time.reloadTotal = reloadTicks * 50;
		time.reloadEnd = System.currentTimeMillis() + time.reloadTotal;
	}

	/** 0..1 through a running timer, 0 when it isn't running. */
	public static float progress(long end, int total) {
		long left = end - System.currentTimeMillis();
		return left <= 0 || left > total ? 0.0f : 1.0f - (float) left / total;
	}
}
