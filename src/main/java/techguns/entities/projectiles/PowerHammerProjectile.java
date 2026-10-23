package techguns.entities.projectiles;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
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

/**
 * The invisible, few blocks long hit of the power hammer's charged blow, and of the chainsaw, mining drill and
 * shishkebap right click (1.12 PowerHammerProjectile and ChainsawProjectile): a spark burst and a thud where it lands.
 * ponytail: one class for both; the 1.12 chainsaw hit ignored water drag, which over its single tick of life doesn't matter.
 */
public class PowerHammerProjectile extends GenericProjectile {
	public PowerHammerProjectile(EntityType<? extends PowerHammerProjectile> type, Level level) {
		super(type, level);
	}

	public PowerHammerProjectile(Level level, LivingEntity shooter, Kind kind, float damage, float speed, int ticksToLive, float spread,
			float damageDropStart, float damageDropEnd, float damageMin, float side, double gravity) {
		super(TGEntities.POWERHAMMER_PROJECTILE.get(), level, shooter, kind, damage, speed, ticksToLive, spread, damageDropStart, damageDropEnd,
				damageMin, side, gravity);
	}

	/** The charged blow: damage times the ammo used, as in 1.12. */
	public static PowerHammerProjectile charged(Level level, LivingEntity shooter, Kind kind, float damage, float speed, int ticksToLive,
			float spread, float damageDropStart, float damageDropEnd, float damageMin, float side, double gravity, float charge, int ammoConsumed) {
		return new PowerHammerProjectile(level, shooter, kind, damage * ammoConsumed, speed, ticksToLive, spread, damageDropStart, damageDropEnd,
				damageMin * ammoConsumed, side, gravity);
	}

	@Override
	protected TGDamageSource damageSource() {
		TGDamageSource source = super.damageSource();
		source.goreChance = 1;
		return source;
	}

	@Override
	protected void onHitEntity(EntityHitResult result) {
		super.onHitEntity(result);
		impact(result.getLocation());
	}

	@Override
	protected void onHitBlock(BlockHitResult result) {
		impact(result.getLocation());
	}

	private void impact(Vec3 pos) {
		if (!(level() instanceof ServerLevel server)) return;
		server.playSound(null, pos.x, pos.y, pos.z, TGSounds.POWERHAMMER_IMPACT.get(), SoundSource.PLAYERS, 1.0f, 1.0f);
		TGEffects.sendFX(server, "PowerhammerImpact", pos.x, pos.y, pos.z, 1.0f, 32);
	}
}
