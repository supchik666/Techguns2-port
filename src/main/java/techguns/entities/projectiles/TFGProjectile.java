package techguns.entities.projectiles;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import techguns.TGEffects;
import techguns.TGEntities;
import techguns.TGSounds;
import techguns.damagesystem.TGExplosion;

/**
 * TFG ball: drawn by its TGFX trail alone. Hits like a bullet, then explodes; charge grows its size (1 to 4, 5 when fully
 * charged), which scales the trail, the explosion's damage and its radii. ponytail: no light pulse.
 */
public class TFGProjectile extends GenericProjectile {
	public float size = 1.0f;

	public TFGProjectile(EntityType<? extends TFGProjectile> type, Level level) {
		super(type, level);
	}

	/** {@link techguns.items.guns.ChargedProjectileFactory}. */
	public TFGProjectile(Level level, LivingEntity shooter, Kind kind, float damage, float speed, int ticksToLive, float spread, float damageDropStart,
			float damageDropEnd, float damageMin, float side, double gravity, float charge, int ammoConsumed) {
		super(TGEntities.TFG_PROJECTILE.get(), level, shooter, kind, damage, speed, ticksToLive, spread, damageDropStart, damageDropEnd, damageMin, side,
				gravity);
		size = 1.0f + charge * 3.0f + (charge >= 1.0f ? 1.0f : 0.0f);
	}

	@Override
	protected void onHitEntity(EntityHitResult result) {
		super.onHitEntity(result);
		if (level() instanceof ServerLevel server) explode(server);
	}

	@Override
	protected void onHitBlock(BlockHitResult result) {
		if (level() instanceof ServerLevel server) explode(server);
	}

	private void explode(ServerLevel server) {
		TGEffects.sendFX(server, "TFGExplosion", getX(), getY(), getZ(), size * 0.75f, 100);
		TGExplosion explosion = new TGExplosion(server, getOwner(), this, getX(), getY(), getZ(), damage * 0.66f * size, damage * 0.33f * size, size,
				size * 2.0f, TGExplosion.blockDamage(getOwner()) ? 0.5 : 0.0);
		explosion.blockDropChance = 0.1f;
		explosion.doExplosion(false);
		server.playSound(null, getX(), getY(), getZ(), TGSounds.TFG_EXPLOSION.get(), SoundSource.BLOCKS, 5.0f, 1.0f);
		if (size > 3.0f) server.playSound(null, getX(), getY(), getZ(), TGSounds.TFG_EXPLOSION_ECHO.get(), SoundSource.BLOCKS, 10.0f, 1.0f);
	}

	@Override
	public void writeSpawnData(RegistryFriendlyByteBuf buffer) {
		super.writeSpawnData(buffer);
		buffer.writeFloat(size);
	}

	@Override
	public void readSpawnData(RegistryFriendlyByteBuf buffer) {
		super.readSpawnData(buffer);
		size = buffer.readFloat();
		TGEffects.createFXOnEntity(this, "TFGTrail", size * 0.75f);
	}
}
