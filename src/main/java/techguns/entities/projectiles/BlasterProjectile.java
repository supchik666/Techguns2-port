package techguns.entities.projectiles;

import javax.annotation.Nullable;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import techguns.TGEffects;
import techguns.TGEntities;
import techguns.damagesystem.TGDamageSource;
import techguns.damagesystem.TGDamageSource.DeathType;
import techguns.damagesystem.TGDamageType;

/**
 * Blaster rifle bolt, and the base of the other bolts: a bullet with a TGFX trail and a TGFX burst where it hits a
 * block, played by the client copy (as in 1.12). ponytail: bolts slow down in water like bullets (1.12 blasters didn't).
 */
public class BlasterProjectile extends GenericProjectile {
	public BlasterProjectile(EntityType<? extends BlasterProjectile> type, Level level) {
		super(type, level);
	}

	public BlasterProjectile(Level level, LivingEntity shooter, Kind kind, float damage, float speed, int ticksToLive, float spread, float damageDropStart,
			float damageDropEnd, float damageMin, float side, double gravity) {
		this(TGEntities.BLASTER_PROJECTILE.get(), level, shooter, kind, damage, speed, ticksToLive, spread, damageDropStart, damageDropEnd, damageMin, side,
				gravity);
	}

	protected BlasterProjectile(EntityType<? extends BlasterProjectile> type, Level level, LivingEntity shooter, Kind kind, float damage, float speed,
			int ticksToLive, float spread, float damageDropStart, float damageDropEnd, float damageMin, float side, double gravity) {
		super(type, level, shooter, kind, damage, speed, ticksToLive, spread, damageDropStart, damageDropEnd, damageMin, side, gravity);
	}

	/** TGFX system that follows the bolt, or null. */
	@Nullable
	protected String trail() {
		return null;
	}

	/** TGFX system played where it hits a block. */
	protected String impact() {
		return "LaserGunImpact";
	}

	/** Hurts the target as usual, setting it alight for {@code seconds}. */
	protected void fireHit(EntityHitResult result, int seconds) {
		if (level().isClientSide) return;
		Entity target = result.getEntity();
		if (damage(target) && seconds > 0) target.igniteForSeconds(seconds);
	}

	@Override
	protected void onHitBlock(BlockHitResult result) {
		Vec3 pos = result.getLocation();
		TGEffects.createFX(level(), impact(), pos.x, pos.y, pos.z, 0, 0, 0);
	}

	@Override
	public void readSpawnData(RegistryFriendlyByteBuf buffer) {
		super.readSpawnData(buffer);
		if (trail() != null) TGEffects.createFXOnEntity(this, trail());
	}

	@Override
	protected TGDamageSource damageSource() {
		return source(TGDamageType.ENERGY, DeathType.LASER);
	}
}
