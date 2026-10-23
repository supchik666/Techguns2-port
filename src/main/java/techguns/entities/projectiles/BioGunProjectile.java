package techguns.entities.projectiles;

import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import techguns.TGEffects;
import techguns.TGEntities;
import techguns.TGSounds;
import techguns.damagesystem.TGDamageSource;
import techguns.damagesystem.TGDamageSource.DeathType;
import techguns.damagesystem.TGDamageType;

/**
 * Bio gun blob: poisons what it hits, splashes on blocks, dies in water. {@code level} is the ammo the charge used (1-3),
 * which sizes the blob. ponytail: no bio blob blocks on impact until the block exists.
 */
public class BioGunProjectile extends GenericProjectile {
	public int level;

	public BioGunProjectile(EntityType<? extends BioGunProjectile> type, Level level) {
		super(type, level);
	}

	/** {@link techguns.items.guns.ChargedProjectileFactory}. */
	public BioGunProjectile(Level level, LivingEntity shooter, Kind kind, float damage, float speed, int ticksToLive, float spread, float damageDropStart,
			float damageDropEnd, float damageMin, float side, double gravity, float charge, int ammoConsumed) {
		super(TGEntities.BIOGUN_PROJECTILE.get(), level, shooter, kind, damage, speed, ticksToLive, spread, damageDropStart, damageDropEnd, damageMin, side,
				gravity);
		this.level = ammoConsumed;
	}

	@Override
	public void tick() {
		super.tick();
		if (isInWater()) discard();
	}

	@Override
	protected void onHitEntity(EntityHitResult result) {
		impact(result.getLocation(), -getXRot(), -getYRot());
		super.onHitEntity(result);
		if (!level().isClientSide && result.getEntity() instanceof LivingEntity living) {
			living.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 3), getOwner());
		}
	}

	@Override
	protected void onHitBlock(BlockHitResult result) {
		Direction side = result.getDirection();
		float pitch = side == Direction.UP ? -90 : side == Direction.DOWN ? 90 : 0;
		impact(result.getLocation(), pitch, side.getAxis().isHorizontal() ? side.toYRot() : 0);
	}

	/** Client: the splash sound and TGFX. */
	private void impact(Vec3 pos, float pitch, float yaw) {
		if (!level().isClientSide) return;
		level().playLocalSound(pos.x, pos.y, pos.z, TGSounds.BIOGUN_IMPACT.get(), SoundSource.AMBIENT, 1.0f, 1.0f, true);
		TGEffects.createFX(level(), "biogunImpact", pos.x, pos.y, pos.z, 0, 0, 0, pitch, yaw, 1.0f);
	}

	@Override
	public void writeSpawnData(RegistryFriendlyByteBuf buffer) {
		super.writeSpawnData(buffer);
		buffer.writeByte(level);
	}

	@Override
	public void readSpawnData(RegistryFriendlyByteBuf buffer) {
		super.readSpawnData(buffer);
		level = buffer.readByte();
		TGEffects.createFXOnEntity(this, "BioGunTrail");
	}

	@Override
	protected TGDamageSource damageSource() {
		return source(TGDamageType.POISON, DeathType.BIO);
	}
}
