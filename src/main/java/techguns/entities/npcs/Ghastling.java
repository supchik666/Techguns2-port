package techguns.entities.npcs;

import java.util.EnumSet;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.pathfinder.PathType;
import techguns.entities.projectiles.AlienBlasterProjectile;
import techguns.entities.projectiles.GenericProjectile;

/**
 * 1.12 Ghastling: a walking little ghast from the nether that shoots volleys of three alien blaster bolts like a blaze
 * and hits at close range. ponytail: no spawner link (ITGSpawnerNPC) until the shared NPC spawn system.
 */
public class Ghastling extends Monster {
	private static final EntityDataAccessor<Boolean> ATTACKING = SynchedEntityData.defineId(Ghastling.class, EntityDataSerializers.BOOLEAN);

	public Ghastling(EntityType<? extends Ghastling> type, Level level) {
		super(type, level);
		setPathfindingMalus(PathType.WATER, -1.0f);
		setPathfindingMalus(PathType.LAVA, 8.0f);
		setPathfindingMalus(PathType.DANGER_FIRE, 0.0f);
		setPathfindingMalus(PathType.DAMAGE_FIRE, 0.0f);
		xpReward = 10;
	}

	public static AttributeSupplier.Builder createAttributes() {
		return createMonsterAttributes().add(Attributes.MAX_HEALTH, 20.0).add(Attributes.FOLLOW_RANGE, 64.0).add(Attributes.ARMOR, 10.0);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(ATTACKING, false);
	}

	/** Winding up or firing a volley: the renderer shows the shooting ghast face. */
	public boolean isAttacking() {
		return entityData.get(ATTACKING);
	}

	public void setAttacking(boolean attacking) {
		entityData.set(ATTACKING, attacking);
	}

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(4, new FireballAttackGoal(this));
		goalSelector.addGoal(5, new MoveTowardsRestrictionGoal(this, 1.0));
		goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 1.0, 0.0f));
		goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0f));
		goalSelector.addGoal(8, new RandomLookAroundGoal(this));
		targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers());
		targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return SoundEvents.GHAST_AMBIENT;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.GHAST_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.GHAST_DEATH;
	}

	@Override
	protected float getSoundVolume() {
		return 10.0f;
	}

	/** 1.12 AIFireballAttack (the blaze's): melee within 2 blocks, else a 1.5s wind-up and three bolts, then a 2.5s pause. */
	static class FireballAttackGoal extends Goal {
		private final Ghastling mob;
		private int attackStep;
		private int attackTime;

		FireballAttackGoal(Ghastling mob) {
			this.mob = mob;
			setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
		}

		@Override
		public boolean canUse() {
			LivingEntity target = mob.getTarget();
			return target != null && target.isAlive();
		}

		@Override
		public void start() {
			attackStep = 0;
		}

		@Override
		public void stop() {
			mob.setAttacking(false);
		}

		@Override
		public boolean requiresUpdateEveryTick() {
			return true;
		}

		@Override
		public void tick() {
			--attackTime;
			LivingEntity target = mob.getTarget();
			double distance = mob.distanceToSqr(target);
			double follow = mob.getAttributeValue(Attributes.FOLLOW_RANGE);
			if (distance < 4.0) {
				if (attackTime <= 0) {
					attackTime = 20;
					mob.doHurtTarget(target);
				}
				mob.getMoveControl().setWantedPosition(target.getX(), target.getY(), target.getZ(), 1.0);
			} else if (distance < follow * follow) {
				if (attackTime <= 0) {
					++attackStep;
					if (attackStep == 1) {
						attackTime = 30;
						mob.setAttacking(true);
					} else if (attackStep <= 4) {
						attackTime = 6;
					} else {
						attackTime = 50;
						attackStep = 0;
						mob.setAttacking(false);
					}
					if (attackStep > 1) {
						mob.level().levelEvent(null, LevelEvent.SOUND_BLAZE_FIREBALL, mob.blockPosition(), 0);
						mob.level().addFreshEntity(new AlienBlasterProjectile(mob.level(), mob, GenericProjectile.Kind.BULLET, 6, 1.5f, 200, 0.05f, 200, 200, 6,
								GenericProjectile.CENTER, 0));
					}
				}
				mob.getLookControl().setLookAt(target, 10.0f, 10.0f);
			} else {
				mob.getNavigation().stop();
				mob.getMoveControl().setWantedPosition(target.getX(), target.getY(), target.getZ(), 1.0);
			}
		}
	}
}
