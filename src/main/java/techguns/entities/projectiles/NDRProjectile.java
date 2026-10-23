package techguns.entities.projectiles;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import techguns.TGEffects;
import techguns.TGEntities;
import techguns.damagesystem.TGDamageSource;
import techguns.damagesystem.TGDamageSource.DeathType;
import techguns.damagesystem.TGDamageType;

/**
 * Nuclear death ray: a beam that follows the shooter's aim and re-traces (and hits) every tick of its 10 tick life, on
 * both sides; the client trace only plays the impact effect. ponytail: no radiation effect on hit until the M4
 * radiation system; the client traces from the 3rd person muzzle also in first person.
 */
public class NDRProjectile extends AbstractBeamProjectile {
	static final int BEAM_LIFETIME = 10;

	private float range;

	public NDRProjectile(EntityType<? extends NDRProjectile> type, Level level) {
		super(type, level);
	}

	public NDRProjectile(Level level, LivingEntity shooter, Kind kind, float damage, float speed, int ticksToLive, float spread, float damageDropStart,
			float damageDropEnd, float damageMin, float side, double gravity) {
		super(TGEntities.NDR_PROJECTILE.get(), level, shooter, kind, damage, speed, BEAM_LIFETIME, spread, damageDropStart, damageDropEnd, damageMin, side);
		range = speed;
	}

	@Override
	public void tick() {
		if (getOwner() instanceof LivingEntity shooter) {
			float yaw = shooter.getYHeadRot() * Mth.DEG_TO_RAD;
			setPos(shooter.getX() - Mth.cos(yaw) * 0.16, shooter.getEyeY() - 0.1, shooter.getZ() - Mth.sin(yaw) * 0.16);
			Vec3 look = Vec3.directionFromRotation(shooter.getXRot(), shooter.getYHeadRot());
			shoot(look.x, look.y, look.z, range, 0);
			trace();
		}
		super.tick();
	}

	@Override
	protected void onHitBlock(BlockHitResult result) {
		Vec3 pos = result.getLocation();
		TGEffects.createFX(level(), "BeamGunImpactFX", pos.x, pos.y, pos.z, 0, 0, 0);
	}

	@Override
	public void writeSpawnData(RegistryFriendlyByteBuf buffer) {
		super.writeSpawnData(buffer);
		buffer.writeFloat(range);
	}

	@Override
	public void readSpawnData(RegistryFriendlyByteBuf buffer) {
		super.readSpawnData(buffer);
		range = buffer.readFloat();
		TGEffects.createFXOnEntity(this, "BeamGunMuzzleFX");
	}

	@Override
	protected TGDamageSource damageSource() {
		return source(TGDamageType.RADIATION, DeathType.LASER);
	}
}
