package techguns.entities.projectiles;

import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.entity.IEntityWithComplexSpawn;
import net.neoforged.neoforge.event.EventHooks;
import techguns.TGEffects;
import techguns.TGEntities;
import techguns.TGSounds;
import techguns.Techguns;

/**
 * A bullet; its {@link Kind} stands in for the 1.12 subclasses (incendiary, explosive, stone, advanced). Hits are only
 * applied on the server; the client copy just flies for the tracer, so gravity and kind go in the spawn packet.
 * ponytail: armor penetration, block damage and the TG damage system (fire/explosion damage types) come with M4.
 */
public class GenericProjectile extends Projectile implements IEntityWithComplexSpawn {
	public static final ResourceKey<DamageType> BULLET = ResourceKey.create(Registries.DAMAGE_TYPE, Techguns.id("bullet"));

	public enum Kind {
		BULLET(1.0f), INCENDIARY_BULLET(1.1f), INCENDIARY_SHELL(1.1f), EXPLOSIVE_BULLET(1.15f), STONE(1.0f), ADVANCED(1.0f),
		/** Rocket and missile variants. */
		NUKE(1.0f), HIGH_VELOCITY(1.0f);

		public final float damageMultiplier;

		Kind(float damageMultiplier) {
			this.damageMultiplier = damageMultiplier;
		}
	}

	/** Side offset of the muzzle from the eyes, by the hand the gun is in; CENTER for scoped shots. */
	public static final float LEFT = 0.16f, RIGHT = -0.16f, CENTER = 0f;
	/** Shots fly this many times faster than in 1.12, living as much shorter so ranges stay the same. */
	public static final float SPEED_FACTOR = 2.0f;
	/** Players this close see block impacts (1.12 bulletTrackRange). */
	private static final double IMPACT_FX_RANGE = 128;

	protected float damage;
	protected float damageMin;
	protected float damageDropStart;
	protected float damageDropEnd;
	protected int ticksToLive;
	protected Vec3 start = Vec3.ZERO;
	protected Kind kind = Kind.BULLET;
	protected double gravity;
	/** {@link #LEFT}/{@link #RIGHT}/{@link #CENTER}, sent to clients for muzzle effects. */
	protected float side;

	public GenericProjectile(EntityType<? extends GenericProjectile> type, Level level) {
		super(type, level);
	}

	/** Fired from the shooter's eyes, {@code side} to the side, with up to ±spread*40 degrees of random spread. */
	public GenericProjectile(Level level, LivingEntity shooter, Kind kind, float damage, float speed, int ticksToLive, float spread,
			float damageDropStart, float damageDropEnd, float damageMin, float side, double gravity) {
		this(TGEntities.GENERIC_PROJECTILE.get(), level, shooter, kind, damage, speed, ticksToLive, spread, damageDropStart, damageDropEnd,
				damageMin, side, gravity);
	}

	/** For subclasses: their own entity type, the rest as above (matches {@link techguns.items.guns.ProjectileFactory}). */
	protected GenericProjectile(EntityType<? extends GenericProjectile> type, Level level, LivingEntity shooter, Kind kind, float damage,
			float speed, int ticksToLive, float spread, float damageDropStart, float damageDropEnd, float damageMin, float side, double gravity) {
		this(type, level);
		setOwner(shooter);
		this.kind = kind;
		this.gravity = gravity;
		this.side = side;
		this.damage = damage * kind.damageMultiplier;
		this.damageMin = damageMin * kind.damageMultiplier;
		this.damageDropStart = damageDropStart;
		this.damageDropEnd = damageDropEnd;
		float factor = speedFactor();
		speed *= factor;
		this.ticksToLive = (int) Math.ceil(ticksToLive / factor);

		float yaw = shooter.getYHeadRot() + (float) (spread - 2 * Math.random() * spread) * 40.0f;
		float pitch = shooter.getXRot() + (float) (spread - 2 * Math.random() * spread) * 40.0f;
		setPos(shooter.getX() + Mth.cos(yaw * Mth.DEG_TO_RAD) * side, shooter.getEyeY() - 0.1, shooter.getZ() + Mth.sin(yaw * Mth.DEG_TO_RAD) * side);
		Vec3 direction = Vec3.directionFromRotation(pitch, yaw);
		shoot(direction.x, direction.y, direction.z, speed, 1.0f);
		start = position();
	}

	/** {@link #SPEED_FACTOR}; 1 where speed means something else (beam range) or for thrown things. Called from the constructor. */
	protected float speedFactor() {
		return SPEED_FACTOR;
	}

	public Kind getKind() {
		return kind;
	}

	/** Client: plays {@code name} at the shooter's muzzle, 1.12 PacketSpawnParticleOnEntity offsets (x for the left hand). */
	protected void muzzleFX(String name, float x, float y, float z) {
		if (getOwner() != null) TGEffects.createFXOnEntity(getOwner(), name, new Vec3(Math.signum(side) * x, y, z), true);
	}

	/** 1.12 shiftForward: start {@code factor} ticks of flight further along. */
	public void shiftForward(float factor) {
		setPos(position().add(getDeltaMovement().scale(factor)));
	}

	@Override
	public void writeSpawnData(RegistryFriendlyByteBuf buffer) {
		buffer.writeEnum(kind);
		buffer.writeDouble(gravity);
		buffer.writeFloat(side);
		Vec3 motion = getDeltaMovement();
		buffer.writeDouble(motion.x);
		buffer.writeDouble(motion.y);
		buffer.writeDouble(motion.z);
	}

	@Override
	public void readSpawnData(RegistryFriendlyByteBuf buffer) {
		kind = buffer.readEnum(Kind.class);
		gravity = buffer.readDouble();
		side = buffer.readFloat();
		setDeltaMovement(buffer.readDouble(), buffer.readDouble(), buffer.readDouble());
		if (kind == Kind.INCENDIARY_SHELL) TGEffects.createFXOnEntity(this, "IncendiaryShotgunTrail");
	}

	/**
	 * Velocity packets cap each axis at 3.9 blocks/tick, which bends fast shots, and vanilla re-aims the projectile along
	 * them. The spawn data has the exact velocity; later packets only apply when uncapped, and never turn it.
	 */
	@Override
	public void lerpMotion(double x, double y, double z) {
		if (Math.max(Math.abs(x), Math.max(Math.abs(y), Math.abs(z))) < 3.9) setDeltaMovement(x, y, z);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {}

	@Override
	public boolean shouldBeSaved() {
		return false; // lives about a second, and its stats aren't saved
	}

	@Override
	public void tick() {
		super.tick();
		HitResult hit = ProjectileUtil.getHitResultOnMoveVector(this, this::canHitEntity);
		if (hit.getType() != HitResult.Type.MISS && !EventHooks.onProjectileImpact(this, hit)) {
			onHit(hit);
			discard();
			return;
		}
		Vec3 motion = getDeltaMovement();
		setPos(position().add(motion));
		float drag = 0.99f;
		if (isInWater()) {
			for (int i = 0; i < 4; i++) {
				level().addParticle(ParticleTypes.BUBBLE, getX() - motion.x * 0.25, getY() - motion.y * 0.25, getZ() - motion.z * 0.25, motion.x, motion.y, motion.z);
			}
			drag = 0.85f;
		}
		setDeltaMovement(motion.scale(drag).subtract(0, gravity, 0));
		if (!level().isClientSide && --ticksToLive <= 0) discard();
	}

	@Override
	protected boolean canHitEntity(Entity target) {
		return target != getOwner() && super.canHitEntity(target);
	}

	@Override
	protected void onHitEntity(EntityHitResult result) {
		if (level().isClientSide) return;
		Entity target = result.getEntity();
		DamageSource source = new DamageSource(level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(BULLET), this, getOwner());
		target.hurt(source, getDamage());
		if (kind == Kind.INCENDIARY_BULLET || kind == Kind.INCENDIARY_SHELL) {
			if (!target.fireImmune()) target.igniteForSeconds(3);
		} else if (kind == Kind.EXPLOSIVE_BULLET) {
			explode(result.getLocation());
		}
	}

	private void explode(Vec3 pos) {
		level().explode(this, pos.x, pos.y, pos.z, 1.5f, Level.ExplosionInteraction.NONE); // ponytail: no block damage until the M4 safemode config
	}

	@Override
	protected void onHitBlock(BlockHitResult result) {
		super.onHitBlock(result);
		if (!(level() instanceof ServerLevel server)) return;
		BlockState state = server.getBlockState(result.getBlockPos());
		Vec3 pos = result.getLocation();
		if (kind == Kind.EXPLOSIVE_BULLET) {
			explode(pos);
			return;
		}
		// the 1.12 PacketGunImpactFX: a TGFX impact by block material, facing out of the hit side
		Direction side = result.getDirection();
		float pitch = side == Direction.UP ? -90 : side == Direction.DOWN ? 90 : 0;
		float yaw = side.getAxis().isHorizontal() ? side.toYRot() : 0;
		String material = kind == Kind.INCENDIARY_SHELL ? "Dirt" : impactMaterial(state.getSoundType(server, result.getBlockPos(), this));
		server.playSound(null, pos.x, pos.y, pos.z, impactSound(material), SoundSource.AMBIENT, 1.0f, 1.0f);
		if (kind == Kind.INCENDIARY_BULLET || kind == Kind.INCENDIARY_SHELL) {
			TGEffects.sendFX(server, "Impact_IncendiaryBullet", pos.x, pos.y, pos.z, pitch, yaw, 1.0f, IMPACT_FX_RANGE);
		}
		if (kind != Kind.INCENDIARY_SHELL) {
			String fx = "Impact_Bullet" + material + (kind == Kind.ADVANCED ? "_Blue" : "");
			TGEffects.sendFX(server, fx, pos.x, pos.y, pos.z, pitch, yaw, 1.0f, IMPACT_FX_RANGE);
		}
	}

	/** Names the TGFX impact and picks the sound, by block sound as in 1.12 (GROUND there is GRAVEL now). */
	private static String impactMaterial(SoundType sound) {
		if (sound == SoundType.STONE) return "Rock";
		if (sound == SoundType.WOOD || sound == SoundType.LADDER) return "Wood";
		if (sound == SoundType.GLASS) return "Glass";
		if (sound == SoundType.METAL || sound == SoundType.ANVIL) return "Metal";
		if (sound == SoundType.GRAVEL || sound == SoundType.SAND) return "Dirt";
		return "Default";
	}

	private static SoundEvent impactSound(String material) {
		return switch (material) {
			case "Rock" -> TGSounds.BULLET_IMPACT_STONE.get();
			case "Wood" -> TGSounds.BULLET_IMPACT_WOOD.get();
			case "Glass" -> TGSounds.BULLET_IMPACT_GLASS.get();
			case "Metal" -> TGSounds.BULLET_IMPACT_METAL.get();
			default -> TGSounds.BULLET_IMPACT_DIRT.get();
		};
	}

	/** Falls linearly from damage to damageMin between damageDropStart and damageDropEnd blocks from the muzzle. */
	protected float getDamage() {
		double distance = position().distanceTo(start);
		if (distance <= damageDropStart) return damage;
		if (distance >= damageDropEnd) return damageMin;
		return (float) Mth.lerp((distance - damageDropStart) / (damageDropEnd - damageDropStart), damage, damageMin);
	}
}
