package techguns.entities.npcs;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LeapAtTargetGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.monster.Spider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import techguns.TGSounds;
import techguns.damagesystem.INpcTGDamageSystem;
import techguns.damagesystem.TGDamageType;

/**
 * 1.12 AlienBug: a fast, wall-climbing spider that hunts players and iron golems by day too, screeching when it picks a
 * target. An arthropod (entity type tag), on the HOSTILE side. Drops nothing, as in 1.12. ponytail: no spawner link
 * (ITGSpawnerNPC) until M7's structure spawners.
 */
public class AlienBug extends Spider implements INpcTGDamageSystem, TGNpcFaction.Member {
	/** Jaw animation ticks after a bite, mirrored to clients by entity event 4. */
	private int attackTimer;

	public AlienBug(EntityType<? extends AlienBug> type, Level level) {
		super(type, level);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Spider.createAttributes().add(Attributes.ATTACK_DAMAGE, 4.0).add(Attributes.MAX_HEALTH, 20.0).add(Attributes.MOVEMENT_SPEED, 1.0);
	}

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(1, new FloatGoal(this));
		goalSelector.addGoal(3, new LeapAtTargetGoal(this, 0.4f));
		goalSelector.addGoal(4, new MeleeAttackGoal(this, 1.0, true) {
			// 1.12 AISpiderAttack: in bright light it sometimes loses interest
			@Override
			public boolean canContinueToUse() {
				if (mob.getLightLevelDependentMagicValue() >= 0.5f && mob.getRandom().nextInt(100) == 0) {
					mob.setTarget(null);
					return false;
				}
				return super.canContinueToUse();
			}
		});
		goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
		goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0f));
		goalSelector.addGoal(6, new RandomLookAroundGoal(this));
		targetSelector.addGoal(1, new HurtByTargetGoal(this));
		targetSelector.addGoal(2, new AggroTargetGoal<>(this, Player.class));
		targetSelector.addGoal(3, new AggroTargetGoal<>(this, IronGolem.class));
	}

	/** Screeches on picking a target. */
	static class AggroTargetGoal<T extends LivingEntity> extends NearestAttackableTargetGoal<T> {
		AggroTargetGoal(Mob mob, Class<T> targetType) {
			super(mob, targetType, true);
		}

		@Override
		public void start() {
			super.start();
			mob.playSound(TGSounds.ALIENBUG_AGGRO.get(), 1.0f, 1.0f);
		}
	}

	@Override
	public boolean doHurtTarget(Entity target) {
		boolean hit = super.doHurtTarget(target);
		attackTimer = 10;
		playSound(TGSounds.ALIENBUG_BITE.get(), 2.0f, 1.0f);
		level().broadcastEntityEvent(this, (byte) 4);
		return hit;
	}

	@Override
	public void handleEntityEvent(byte id) {
		if (id == 4) attackTimer = 10;
		else super.handleEntityEvent(id);
	}

	@Override
	public void aiStep() {
		super.aiStep();
		if (attackTimer > 0) --attackTimer;
	}

	public int getAttackTimer() {
		return attackTimer;
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return TGSounds.ALIENBUG_IDLE.get();
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return TGSounds.ALIENBUG_HURT.get();
	}

	@Override
	protected SoundEvent getDeathSound() {
		return TGSounds.ALIENBUG_DEATH.get();
	}

	@Override
	protected void playStepSound(BlockPos pos, BlockState state) {
		playSound(TGSounds.ALIENBUG_STEP.get(), 0.15f, 1.0f);
	}

	@Override
	public TGNpcFaction getTGFaction() {
		return TGNpcFaction.HOSTILE;
	}

	@Override
	public float getTotalArmorAgainstType(TGDamageType type) {
		return armorAgainst(type);
	}

	/** 1.12 per-type armor. */
	public static float armorAgainst(TGDamageType type) {
		return switch (type) {
			case ENERGY, EXPLOSION, ICE, LIGHTNING -> 5.0f;
			case PHYSICAL, PROJECTILE -> 10.0f;
			case POISON, RADIATION -> 20.0f;
			default -> 0.0f;
		};
	}
}
