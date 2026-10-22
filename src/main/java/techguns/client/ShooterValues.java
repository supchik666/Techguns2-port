package techguns.client;

import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ThreadLocalRandom;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;

/**
 * Client-side animation timers per shooter and hand (the 1.12 AttackTime). Wall-clock ms, so animations run smoothly
 * between ticks. Only touched from the client thread.
 */
public class ShooterValues {
	public static class AttackTime {
		public long recoilEnd, reloadEnd, muzzleFlashEnd;
		public int recoilTotal, reloadTotal, muzzleFlashTotal;
		public float jitterX, jitterY, jitterScale;
	}

	private static final Map<Entity, AttackTime[]> TIMES = new WeakHashMap<>();

	public static AttackTime get(Entity entity, InteractionHand hand) {
		return TIMES.computeIfAbsent(entity, e -> new AttackTime[] {new AttackTime(), new AttackTime()})[hand.ordinal()];
	}

	public static void fired(Entity entity, InteractionHand hand, int recoilTicks, int muzzleFlashTicks) {
		AttackTime time = get(entity, hand);
		long now = System.currentTimeMillis();
		time.recoilTotal = recoilTicks * 50;
		time.recoilEnd = now + time.recoilTotal;
		time.muzzleFlashTotal = muzzleFlashTicks * 50;
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
