package techguns.entities.npcs;

import java.util.EnumSet;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import techguns.TGConfig;
import techguns.TGEffects;
import techguns.TGSounds;
import techguns.damagesystem.INpcTGDamageSystem;
import techguns.damagesystem.TGDamageType;
import techguns.entities.projectiles.GenericProjectile;
import techguns.entities.projectiles.RocketProjectile;

/**
 * 1.12 AttackHelicopter: cruises 24 blocks up, fires bullet bursts and a rocket at players within 64 blocks in sight, and
 * crashes spinning for 5 seconds when shot down.
 */
public class AttackHelicopter extends GenericFlyingMob implements INpcTGDamageSystem {
	public static final int MAX_DEATH_TIME = 100;
	private int rotorSoundTick;

	public AttackHelicopter(EntityType<? extends AttackHelicopter> type, Level level) {
		super(type, level);
		xpReward = 5;
	}

	public static AttributeSupplier.Builder createAttributes() {
		return createMobAttributes().add(Attributes.MAX_HEALTH, 100.0).add(Attributes.FOLLOW_RANGE, 128.0).add(Attributes.ARMOR, 16.0)
				.add(Attributes.ARMOR_TOUGHNESS, 5.0);
	}

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(5, new RandomFlyGoal(this));
		goalSelector.addGoal(7, new LookAroundGoal(this));
		goalSelector.addGoal(7, new AttackGoal(this));
		targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, false));
	}

	@Override
	protected int getTargetFlyHeight() {
		return 24;
	}

	@Override
	public SoundSource getSoundSource() {
		return SoundSource.HOSTILE;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return TGSounds.HELICOPTER_HIT.get();
	}

	@Override
	protected SoundEvent getDeathSound() {
		return TGSounds.HELICOPTER_HIT.get();
	}

	@Override
	protected float getSoundVolume() {
		return 10.0f;
	}

	/** 1.12 spawn check: one in 20 tries, not in peaceful. */
	@Override
	public boolean checkSpawnRules(LevelAccessor level, MobSpawnType reason) {
		return random.nextInt(20) == 0 && super.checkSpawnRules(level, reason) && level.getDifficulty() != Difficulty.PEACEFUL;
	}

	@Override
	public int getMaxSpawnClusterSize() {
		return 1;
	}

	/** Its own rockets explode with a 30 block radius from below; they only hurt it with the helicopterSelfDamage config (1.12 behavior). */
	@Override
	public boolean isInvulnerableTo(DamageSource source) {
		return (source.getEntity() == this && !TGConfig.HELICOPTER_SELF_DAMAGE.getAsBoolean()) || super.isInvulnerableTo(source);
	}

	@Override
	public void baseTick() {
		super.baseTick();
		if (!level().isClientSide && --rotorSoundTick <= 0 && deathTime == 0) {
			playSound(TGSounds.HELICOPTER_ROTOR.get(), 6.0f, 1.0f);
			rotorSoundTick = 61;
		}
	}

	/** Falls and spins (see the renderer) for {@link #MAX_DEATH_TIME} ticks, then explodes. */
	@Override
	protected void tickDeath() {
		if (deathTime == 0 && !level().isClientSide) playSound(TGSounds.HELICOPTER_DEATH.get(), 6.0f, 1.0f);
		if (++deathTime == MAX_DEATH_TIME && level() instanceof ServerLevel server) {
			playSound(TGSounds.HELICOPTER_EXPLODE.get(), 6.0f, 1.0f);
			TGEffects.sendFX(server, "HelicopterDeathExplosion", getX(), getY() - 8.0, getZ(), 1.0f, 150);
			remove(RemovalReason.KILLED);
		}
	}

	@Override
	public float getTotalArmorAgainstType(TGDamageType type) {
		return armorAgainst(type);
	}

	@Override
	public float getPenetrationResistance(TGDamageType type) {
		return penetrationResistance(type);
	}

	/** 1.12 per-type armor; replaces the vanilla armor 16. */
	public static float armorAgainst(TGDamageType type) {
		return switch (type) {
			case LIGHTNING, ENERGY -> 10.0f;
			case FIRE, ICE, PHYSICAL, PROJECTILE, POISON, RADIATION -> 20.0f;
			default -> 0.0f;
		};
	}

	/** 1.12 penetration resistance, see {@link #armorAgainst}. */
	public static float penetrationResistance(TGDamageType type) {
		return type == TGDamageType.PROJECTILE ? 0.5f : 0.0f;
	}

	/** 1.12 AIHelicopterAttack: a 5 bullet burst from tick 14, a rocket from a random side at tick 35, then a 1.5s pause. */
	static class AttackGoal extends Goal {
		private final AttackHelicopter heli;
		private int attackTimer;

		AttackGoal(AttackHelicopter heli) {
			this.heli = heli;
			setFlags(EnumSet.noneOf(Goal.Flag.class));
		}

		@Override
		public boolean canUse() {
			return heli.getTarget() != null;
		}

		@Override
		public void start() {
			attackTimer = 0;
		}

		@Override
		public boolean requiresUpdateEveryTick() {
			return true;
		}

		@Override
		public void tick() {
			LivingEntity target = heli.getTarget();
			if (target.distanceToSqr(heli) < 4096.0 && heli.hasLineOfSight(target)) {
				++attackTimer;
				if (attackTimer >= 14 && attackTimer < 24 && attackTimer % 2 == 0) {
					if (attackTimer == 14) heli.playSound(TGSounds.HELICOPTER_BURST.get(), 8.0f, 1.0f);
					GenericProjectile bullet = new GenericProjectile(heli.level(), heli, GenericProjectile.Kind.BULLET, 12.0f, 1.0f, 100, 0.05f, 30, 40, 8.0f,
							GenericProjectile.CENTER, 0);
					bullet.setPenetration(0.25f);
					heli.level().addFreshEntity(bullet);
				} else if (attackTimer == 35) {
					heli.playSound(TGSounds.ROCKET_FIRE.get(), 8.0f, 1.0f);
					// 1.12 side offset: 0.16 plus getBulletOffsetSide 2, from a random rocket pod
					GenericProjectile rocket = RocketProjectile.create(heli.level(), heli, GenericProjectile.Kind.BULLET, 12.0f, 1.0f, 100, 0.05f, 30, 40, 8.0f,
							heli.random.nextBoolean() ? 2.16f : -2.16f, 0);
					rocket.setPenetration(0.25f);
					heli.level().addFreshEntity(rocket);
				} else if (attackTimer > 35) {
					attackTimer = -30;
				}
			} else if (attackTimer > 0) {
				--attackTimer;
			}
		}
	}
}
