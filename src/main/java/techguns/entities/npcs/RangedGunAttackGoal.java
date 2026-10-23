package techguns.entities.npcs;

import java.util.EnumSet;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.RangedAttackMob;

/**
 * 1.12 EntityAIRangedAttack: walks into range, then fires {@link RangedAttackMob#performRangedAttack} every
 * {@code variance..attackTime} ticks (longer the farther the target), in bursts of {@code maxBurst} shots {@code burstDelay} apart.
 * Made by {@link techguns.items.guns.GenericGun#getAIAttack}; a mob that can't move (a turret) just never closes in.
 */
public class RangedGunAttackGoal<T extends Mob & RangedAttackMob> extends Goal {
	private final T mob;
	private final double speed;
	private final int variance, attackTime, maxBurst, burstDelay;
	private final float range;
	private LivingEntity target;
	private int seeTime;
	private int timer = -1;
	private int burst;

	public RangedGunAttackGoal(T mob, double speed, int variance, int attackTime, float range, int maxBurst, int burstDelay) {
		this.mob = mob;
		this.speed = speed;
		this.variance = variance;
		this.attackTime = attackTime;
		this.range = range;
		this.maxBurst = maxBurst;
		this.burstDelay = burstDelay;
		this.burst = maxBurst;
		setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
	}

	@Override
	public boolean canUse() {
		target = mob.getTarget();
		return target != null && target.isAlive();
	}

	@Override
	public boolean canContinueToUse() {
		return canUse() || !mob.getNavigation().isDone();
	}

	@Override
	public void stop() {
		target = null;
		seeTime = 0;
		timer = -1;
	}

	@Override
	public boolean requiresUpdateEveryTick() {
		return true;
	}

	@Override
	public void tick() {
		if (target == null) return;
		double distSqr = mob.distanceToSqr(target.getX(), target.getY(), target.getZ());
		boolean sees = mob.getSensing().hasLineOfSight(target);
		seeTime = sees ? seeTime + 1 : 0;
		if (distSqr <= range * range && seeTime >= 20) mob.getNavigation().stop();
		else mob.getNavigation().moveTo(target, speed);
		mob.getLookControl().setLookAt(target, 30.0f, 55.0f);
		float f = (float) Math.sqrt(distSqr) / range;
		if (--timer == 0) {
			if (distSqr > range * range || !sees) return;
			mob.performRangedAttack(target, Mth.clamp(f, 0.1f, 1.0f));
			if (maxBurst > 0) burst--;
			if (burst > 0) {
				timer = burstDelay;
			} else {
				burst = maxBurst;
				timer = Mth.floor(f * (attackTime - variance) + variance);
			}
		} else if (timer < 0) {
			timer = Mth.floor(f * (attackTime - variance) + variance);
		}
	}
}
