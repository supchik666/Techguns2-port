package techguns.entities.projectiles;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import techguns.TGEffects;
import techguns.TGEntities;
import techguns.damagesystem.TGExplosion;

/**
 * Thrown stick grenade (stielgranate), and the base of the other grenades: bounces off blocks at half speed until its
 * bounces run out, then explodes on the next impact, or at once when it hits an entity. As in 1.12 there is no fuse:
 * running out of ticksToLive just removes it. Never breaks blocks. Like 1.12 each bounce replaces it with a copy.
 */
public class GrenadeProjectile extends GenericProjectile {
	protected int bounces;

	public GrenadeProjectile(EntityType<? extends GrenadeProjectile> type, Level level) {
		super(type, level);
	}

	/**
	 * {@link techguns.items.guns.ChargedProjectileFactory}: {@code charge} is the throw strength (0..1), giving gravity
	 * 0.015/charge as 1.12 GenericGrenade did (the gravity parameter is ignored); damageDropStart/End are the radii.
	 */
	@Override
	protected float speedFactor() {
		return 1; // thrown by hand
	}

	public GrenadeProjectile(Level level, LivingEntity shooter, Kind kind, float damage, float speed, int ticksToLive, float spread,
			float damageDropStart, float damageDropEnd, float damageMin, float side, double gravity, float charge, int ammoConsumed) {
		this(TGEntities.GRENADE_PROJECTILE.get(), level, shooter, kind, damage, speed, ticksToLive, spread, damageDropStart, damageDropEnd, damageMin, side,
				0.015 / Math.max(charge, 0.01f), 3);
	}

	protected GrenadeProjectile(EntityType<? extends GrenadeProjectile> type, Level level, LivingEntity shooter, Kind kind, float damage, float speed,
			int ticksToLive, float spread, float damageDropStart, float damageDropEnd, float damageMin, float side, double gravity, int bounces) {
		super(type, level, shooter, kind, damage, speed, ticksToLive, spread, damageDropStart, damageDropEnd, damageMin, side, gravity);
		this.bounces = bounces;
	}

	@Override
	public void tick() {
		super.tick();
		updateRotation();
	}

	@Override
	protected void onHitEntity(EntityHitResult result) {
		if (level().isClientSide) return;
		result.getEntity().hurt(damageSources().explosion(getOwner(), this), getDamage());
		explode((ServerLevel) level());
	}

	@Override
	protected void onHitBlock(BlockHitResult result) {
		if (!(level() instanceof ServerLevel server)) return;
		if (bounces <= 0) {
			explode(server);
			return;
		}
		Vec3 m = getDeltaMovement();
		Vec3 reflected = switch (result.getDirection().getAxis()) {
			case X -> new Vec3(-m.x, m.y, m.z);
			case Z -> new Vec3(m.x, m.y, -m.z);
			case Y -> new Vec3(m.x, -m.y, m.z);
		};
		server.playSound(null, getX(), getY(), getZ(), SoundEvents.IRON_GOLEM_HURT, SoundSource.PLAYERS, 1.0f, 1.0f);
		GrenadeProjectile copy = (GrenadeProjectile) getType().create(server);
		copy.setOwner(getOwner());
		copy.kind = kind;
		copy.damage = damage;
		copy.damageMin = damageMin;
		copy.damageDropStart = damageDropStart;
		copy.damageDropEnd = damageDropEnd;
		copy.ticksToLive = ticksToLive;
		copy.gravity = gravity;
		copy.bounces = bounces - 1;
		copy.setDeltaMovement(reflected.scale(0.5));
		Vec3 pos = result.getLocation().add(copy.getDeltaMovement());
		copy.moveTo(pos.x, pos.y, pos.z, getYRot(), getXRot());
		copy.start = copy.position();
		server.addFreshEntity(copy);
	}

	protected void explode(ServerLevel server) {
		TGEffects.sendFX(server, "RocketExplosion", getX(), getY(), getZ(), 1.0f, 50);
		new TGExplosion(server, getOwner(), this, getX(), getY(), getZ(), damage, damageMin, damageDropStart, damageDropEnd, 0).doExplosion(true);
	}
}
