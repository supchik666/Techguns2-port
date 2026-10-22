package techguns.entities.projectiles;

import net.minecraft.core.particles.BlockParticleOption;
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
import techguns.TGEntities;
import techguns.TGSounds;
import techguns.Techguns;

/**
 * A bullet; its {@link Kind} stands in for the 1.12 subclasses (incendiary, explosive, stone, advanced). Hits are only
 * applied on the server; the client copy just flies for the tracer, so gravity and kind go in the spawn packet.
 * ponytail: armor penetration, block damage and the TG damage system (fire/explosion damage types) come with M4;
 * TGFX impact particles with M3.2.
 */
public class GenericProjectile extends Projectile implements IEntityWithComplexSpawn {
	public static final ResourceKey<DamageType> BULLET = ResourceKey.create(Registries.DAMAGE_TYPE, Techguns.id("bullet"));

	public enum Kind {
		BULLET(1.0f), INCENDIARY_BULLET(1.1f), INCENDIARY_SHELL(1.1f), EXPLOSIVE_BULLET(1.15f), STONE(1.0f), ADVANCED(1.0f);

		public final float damageMultiplier;

		Kind(float damageMultiplier) {
			this.damageMultiplier = damageMultiplier;
		}
	}

	/** Side offset of the muzzle from the eyes, by the hand the gun is in; CENTER for scoped shots. */
	public static final float LEFT = 0.16f, RIGHT = -0.16f, CENTER = 0f;

	private float damage;
	private float damageMin;
	private float damageDropStart;
	private float damageDropEnd;
	private int ticksToLive;
	private Vec3 start = Vec3.ZERO;
	private Kind kind = Kind.BULLET;
	private double gravity;

	public GenericProjectile(EntityType<? extends GenericProjectile> type, Level level) {
		super(type, level);
	}

	/** Fired from the shooter's eyes, {@code side} to the side, with up to ±spread*40 degrees of random spread. */
	public GenericProjectile(Level level, LivingEntity shooter, Kind kind, float damage, float speed, int ticksToLive, float spread,
			float damageDropStart, float damageDropEnd, float damageMin, float side, double gravity) {
		this(TGEntities.GENERIC_PROJECTILE.get(), level);
		setOwner(shooter);
		this.kind = kind;
		this.gravity = gravity;
		this.damage = damage * kind.damageMultiplier;
		this.damageMin = damageMin * kind.damageMultiplier;
		this.damageDropStart = damageDropStart;
		this.damageDropEnd = damageDropEnd;
		this.ticksToLive = ticksToLive;

		float yaw = shooter.getYHeadRot() + (float) (spread - 2 * Math.random() * spread) * 40.0f;
		float pitch = shooter.getXRot() + (float) (spread - 2 * Math.random() * spread) * 40.0f;
		setPos(shooter.getX() + Mth.cos(yaw * Mth.DEG_TO_RAD) * side, shooter.getEyeY() - 0.1, shooter.getZ() + Mth.sin(yaw * Mth.DEG_TO_RAD) * side);
		Vec3 direction = Vec3.directionFromRotation(pitch, yaw);
		shoot(direction.x, direction.y, direction.z, speed, 1.0f);
		start = position();
	}

	public Kind getKind() {
		return kind;
	}

	/** 1.12 shiftForward: start {@code factor} ticks of flight further along. */
	public void shiftForward(float factor) {
		setPos(position().add(getDeltaMovement().scale(factor)));
	}

	@Override
	public void writeSpawnData(RegistryFriendlyByteBuf buffer) {
		buffer.writeEnum(kind);
		buffer.writeDouble(gravity);
	}

	@Override
	public void readSpawnData(RegistryFriendlyByteBuf buffer) {
		kind = buffer.readEnum(Kind.class);
		gravity = buffer.readDouble();
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
		if (kind == Kind.INCENDIARY_SHELL && level().isClientSide && !isInWater()) {
			level().addParticle(ParticleTypes.FLAME, getX() - motion.x * 0.25, getY() - motion.y * 0.25, getZ() - motion.z * 0.25,
					motion.x * 0.05, motion.y * 0.05, motion.z * 0.05);
		}
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
		server.playSound(null, pos.x, pos.y, pos.z, impactSound(state.getSoundType(server, result.getBlockPos(), this)), SoundSource.AMBIENT, 1.0f, 1.0f);
		server.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state), pos.x, pos.y, pos.z, 6, 0.05, 0.05, 0.05, 0.1);
	}

	private static SoundEvent impactSound(SoundType sound) {
		if (sound == SoundType.STONE) return TGSounds.BULLET_IMPACT_STONE.get();
		if (sound == SoundType.WOOD || sound == SoundType.LADDER) return TGSounds.BULLET_IMPACT_WOOD.get();
		if (sound == SoundType.GLASS) return TGSounds.BULLET_IMPACT_GLASS.get();
		if (sound == SoundType.METAL || sound == SoundType.ANVIL) return TGSounds.BULLET_IMPACT_METAL.get();
		return TGSounds.BULLET_IMPACT_DIRT.get();
	}

	/** Falls linearly from damage to damageMin between damageDropStart and damageDropEnd blocks from the muzzle. */
	private float getDamage() {
		double distance = position().distanceTo(start);
		if (distance <= damageDropStart) return damage;
		if (distance >= damageDropEnd) return damageMin;
		return (float) Mth.lerp((distance - damageDropStart) / (damageDropEnd - damageDropStart), damage, damageMin);
	}
}
