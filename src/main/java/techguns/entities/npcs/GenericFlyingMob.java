package techguns.entities.npcs;

import java.util.EnumSet;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.FlyingMob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * 1.12 GenericFlyingMob: a hostile flyer that cruises {@link #getTargetFlyHeight} blocks above the ground and faces its
 * target. Gone in peaceful. ponytail: no spawner link (ITGSpawnerNPC) until the shared NPC spawn system.
 */
public abstract class GenericFlyingMob extends FlyingMob implements Enemy {
	protected GenericFlyingMob(EntityType<? extends GenericFlyingMob> type, Level level) {
		super(type, level);
		moveControl = new FlyingMobMoveControl(this);
	}

	protected abstract int getTargetFlyHeight();

	@Override
	public boolean shouldDespawnInPeaceful() {
		return true;
	}

	/** 1.12 AILookAround: faces where it flies, or its target within 96 blocks, pitching down at it from above. */
	protected static class LookAroundGoal extends Goal {
		private final GenericFlyingMob mob;

		public LookAroundGoal(GenericFlyingMob mob) {
			this.mob = mob;
			setFlags(EnumSet.of(Goal.Flag.LOOK));
		}

		@Override
		public boolean canUse() {
			return true;
		}

		@Override
		public boolean requiresUpdateEveryTick() {
			return true;
		}

		@Override
		public void tick() {
			LivingEntity target = mob.getTarget();
			if (target == null) {
				Vec3 motion = mob.getDeltaMovement();
				mob.setYRot(-(float) Mth.atan2(motion.x, motion.z) * Mth.RAD_TO_DEG);
				mob.yBodyRot = mob.getYRot();
			} else if (target.distanceToSqr(mob) < 96.0 * 96.0) {
				mob.setYRot(-(float) Mth.atan2(target.getX() - mob.getX(), target.getZ() - mob.getZ()) * Mth.RAD_TO_DEG);
				mob.yBodyRot = mob.getYRot();
				if (mob.getY() > target.getY()) mob.getLookControl().setLookAt(target, 45.0f, 90.0f);
			}
		}
	}

	/** 1.12 AIRandomFly: when idle or far off course, picks a point within 16 blocks at the fly height over the ground. */
	protected static class RandomFlyGoal extends Goal {
		private final GenericFlyingMob mob;

		public RandomFlyGoal(GenericFlyingMob mob) {
			this.mob = mob;
			setFlags(EnumSet.of(Goal.Flag.MOVE));
		}

		@Override
		public boolean canUse() {
			MoveControl control = mob.getMoveControl();
			if (!control.hasWanted()) return true;
			double distance = mob.distanceToSqr(control.getWantedX(), control.getWantedY(), control.getWantedZ());
			return distance < 1.0 || distance > 3600.0;
		}

		@Override
		public boolean canContinueToUse() {
			return false;
		}

		@Override
		public void start() {
			double x = mob.getX() + (mob.getRandom().nextFloat() * 2.0f - 1.0f) * 16.0f;
			double z = mob.getZ() + (mob.getRandom().nextFloat() * 2.0f - 1.0f) * 16.0f;
			int ground = mob.level().getHeight(Heightmap.Types.MOTION_BLOCKING, mob.getBlockX(), mob.getBlockZ());
			mob.getMoveControl().setWantedPosition(x, ground + mob.getTargetFlyHeight(), z, 1.0);
		}
	}

	/** 1.12 FlyingMobMoveHelper (the ghast's): nudges toward the wanted point every few ticks while the way is clear. */
	protected static class FlyingMobMoveControl extends MoveControl {
		private int courseChangeCooldown;

		public FlyingMobMoveControl(GenericFlyingMob mob) {
			super(mob);
		}

		@Override
		public void tick() {
			if (operation != MoveControl.Operation.MOVE_TO || courseChangeCooldown-- > 0) return;
			courseChangeCooldown += mob.getRandom().nextInt(5) + 2;
			Vec3 delta = new Vec3(wantedX - mob.getX(), wantedY - mob.getY(), wantedZ - mob.getZ());
			double length = delta.length();
			Vec3 step = delta.normalize();
			AABB box = mob.getBoundingBox();
			for (int i = 1; i < length; i++) {
				box = box.move(step);
				if (!mob.level().noCollision(mob, box)) {
					operation = MoveControl.Operation.WAIT;
					return;
				}
			}
			mob.setDeltaMovement(mob.getDeltaMovement().add(step.scale(0.1)));
		}
	}
}
