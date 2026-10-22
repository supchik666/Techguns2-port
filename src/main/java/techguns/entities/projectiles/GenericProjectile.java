package techguns.entities.projectiles;

import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
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
import net.neoforged.neoforge.event.EventHooks;
import techguns.TGEntities;
import techguns.TGSounds;
import techguns.Techguns;

/**
 * Straight-flying bullet. Hits are only applied on the server; the client copy just flies for the tracer.
 * ponytail: armor penetration, block damage and the TG damage system come with M4; TGFX impact particles with M3.
 */
public class GenericProjectile extends Projectile {
	public static final ResourceKey<DamageType> BULLET = ResourceKey.create(Registries.DAMAGE_TYPE, Techguns.id("bullet"));

	private float damage;
	private float damageMin;
	private float damageDropStart;
	private float damageDropEnd;
	private int ticksToLive;
	private Vec3 start = Vec3.ZERO;

	public GenericProjectile(EntityType<? extends GenericProjectile> type, Level level) {
		super(type, level);
	}

	/** Fired from the shooter's eyes, 0.16 to the side of the gun hand, with up to ±spread*40 degrees of random spread. */
	public GenericProjectile(Level level, LivingEntity shooter, float damage, float speed, int ticksToLive, float spread,
			float damageDropStart, float damageDropEnd, float damageMin, boolean leftHand) {
		this(TGEntities.GENERIC_PROJECTILE.get(), level);
		setOwner(shooter);
		this.damage = damage;
		this.damageMin = damageMin;
		this.damageDropStart = damageDropStart;
		this.damageDropEnd = damageDropEnd;
		this.ticksToLive = ticksToLive;

		float yaw = shooter.getYHeadRot() + (float) (spread - 2 * Math.random() * spread) * 40.0f;
		float pitch = shooter.getXRot() + (float) (spread - 2 * Math.random() * spread) * 40.0f;
		float side = leftHand ? 0.16f : -0.16f;
		setPos(shooter.getX() + Mth.cos(yaw * Mth.DEG_TO_RAD) * side, shooter.getEyeY() - 0.1, shooter.getZ() + Mth.sin(yaw * Mth.DEG_TO_RAD) * side);
		Vec3 direction = Vec3.directionFromRotation(pitch, yaw);
		shoot(direction.x, direction.y, direction.z, speed, 1.0f);
		start = position();
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
		setDeltaMovement(motion.scale(drag));
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
	}

	@Override
	protected void onHitBlock(BlockHitResult result) {
		super.onHitBlock(result);
		if (!(level() instanceof ServerLevel server)) return;
		BlockState state = server.getBlockState(result.getBlockPos());
		Vec3 pos = result.getLocation();
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
