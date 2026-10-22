package techguns.entities.projectiles;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import techguns.TGEffects;
import techguns.TGEntities;

/**
 * Gauss rifle slug: an {@link Kind#ADVANCED} bullet (blue tracer) with a trail and a bigger block impact.
 * ponytail: the 1.12 light pulses are skipped.
 */
public class GaussProjectile extends GenericProjectile {
	public GaussProjectile(EntityType<? extends GaussProjectile> type, Level level) {
		super(type, level);
	}

	public GaussProjectile(Level level, LivingEntity shooter, Kind kind, float damage, float speed, int ticksToLive, float spread, float damageDropStart,
			float damageDropEnd, float damageMin, float side, double gravity) {
		super(TGEntities.GAUSS_PROJECTILE.get(), level, shooter, Kind.ADVANCED, damage, speed, ticksToLive, spread, damageDropStart, damageDropEnd,
				damageMin, side, gravity);
	}

	@Override
	protected void onHitBlock(BlockHitResult result) {
		super.onHitBlock(result);
		Vec3 pos = result.getLocation();
		if (level() instanceof ServerLevel server) TGEffects.sendFX(server, "GaussRifleImpact_Block", pos.x, pos.y, pos.z, 1.0f, 50);
	}

	@Override
	public void readSpawnData(RegistryFriendlyByteBuf buffer) {
		super.readSpawnData(buffer);
		TGEffects.createFXOnEntity(this, "GaussProjectileTrail");
		muzzleFX("GaussFireFX", 0.14f, -0.09f, 0.5f);
	}
}
