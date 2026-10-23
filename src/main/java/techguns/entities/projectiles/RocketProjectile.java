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
import techguns.damagesystem.TGDamageSource;
import techguns.damagesystem.TGDamageSource.DeathType;
import techguns.damagesystem.TGDamageType;
import techguns.damagesystem.TGExplosion;
import techguns.entities.special.RadiationEntity;

/**
 * Rocket launcher rocket: explodes on impact, radii and minimum damage from damageDropStart/End and damageMin (as in
 * 1.12). The nuke and high velocity rockets (1.12 subclasses) are its {@link Kind}s, picked by {@link #create}.
 * ponytail: no light pulse.
 */
public class RocketProjectile extends GenericProjectile {
	public RocketProjectile(EntityType<? extends RocketProjectile> type, Level level) {
		super(type, level);
	}

	protected RocketProjectile(EntityType<? extends RocketProjectile> type, Level level, LivingEntity shooter, Kind kind, float damage, float speed,
			int ticksToLive, float spread, float damageDropStart, float damageDropEnd, float damageMin, float side, double gravity) {
		super(type, level, shooter, kind, damage, speed, ticksToLive, spread, damageDropStart, damageDropEnd, damageMin, side, gravity);
	}

	/** {@link techguns.items.guns.ProjectileFactory}; NUKE and HIGH_VELOCITY apply the 1.12 ammo DamageModifiers. */
	public static GenericProjectile create(Level level, LivingEntity shooter, Kind kind, float damage, float speed, int ticksToLive, float spread,
			float damageDropStart, float damageDropEnd, float damageMin, float side, double gravity) {
		EntityType<RocketProjectile> type = TGEntities.ROCKET_PROJECTILE.get();
		return switch (kind) {
			case NUKE -> new RocketProjectile(type, level, shooter, kind, damage * 5, speed, ticksToLive, spread, damageDropStart * 5, damageDropEnd * 5,
					damageMin * 5, side, gravity);
			case HIGH_VELOCITY -> new RocketProjectile(type, level, shooter, kind, damage, speed * 2, Math.round(ticksToLive * 0.75f), spread,
					damageDropStart * 0.75f, damageDropEnd * 0.75f, damageMin, side, gravity);
			default -> new RocketProjectile(type, level, shooter, kind, damage, speed, ticksToLive, spread, damageDropStart, damageDropEnd, damageMin, side,
					gravity);
		};
	}

	@Override
	public void tick() {
		super.tick();
		updateRotation();
	}

	/** TGFX exhaust that follows the rocket. */
	protected String trail() {
		return "RocketLauncherExhaust";
	}

	@Override
	public void readSpawnData(RegistryFriendlyByteBuf buffer) {
		super.readSpawnData(buffer);
		TGEffects.createFXOnEntity(this, trail());
	}

	@Override
	protected void onHitEntity(EntityHitResult result) {
		if (level().isClientSide) return;
		damage(result.getEntity());
		explode((ServerLevel) level());
	}

	@Override
	protected void onHitBlock(BlockHitResult result) {
		if (level() instanceof ServerLevel server) explode(server);
	}

	protected void explode(ServerLevel server) {
		if (kind == Kind.NUKE) {
			TGEffects.sendFX(server, "NukeExplosion", getX(), getY(), getZ(), 1.0f, 150);
			TGExplosion explosion = new TGExplosion(server, getOwner(), this, getX(), getY(), getZ(), damage, damageMin, damageDropStart, damageDropEnd,
					TGExplosion.blockDamage(getOwner()) ? 0.5 : 0.0);
			explosion.blockDropChance = 0.05f;
			explosion.knockback = 3.0f;
			explosion.doExplosion(false);
			server.playSound(null, getX(), getY(), getZ(), TGSounds.NUKE_EXPLOSION.get(), SoundSource.BLOCKS, 4.0f, 1.0f);
			server.addFreshEntity(new RadiationEntity(server, position(), 20 * 60, damageDropStart, 9, damageDropEnd, 2));
		} else {
			TGEffects.sendFX(server, "RocketExplosion", getX(), getY(), getZ(), 1.0f, 50);
			new TGExplosion(server, getOwner(), this, getX(), getY(), getZ(), damage, damageMin, damageDropStart, damageDropEnd,
					TGExplosion.blockDamage(getOwner()) ? 0.5 : 0.0).doExplosion(true);
		}
	}

	@Override
	protected TGDamageSource damageSource() {
		return source(TGDamageType.EXPLOSION, kind == Kind.NUKE ? DeathType.LASER : DeathType.GORE);
	}

	@Override
	protected float knockback() {
		return 3;
	}
}
