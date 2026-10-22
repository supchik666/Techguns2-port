package techguns.entities.projectiles;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import techguns.TGEntities;

/**
 * Flamethrower flame: an invisible bolt drawn by its TGFX trail; fire damage, ignites, goes out in water or rain.
 * ponytail: no block burning until the M4 block damage config.
 */
public class FlamethrowerProjectile extends BlasterProjectile {
	public FlamethrowerProjectile(EntityType<? extends FlamethrowerProjectile> type, Level level) {
		super(type, level);
	}

	public FlamethrowerProjectile(Level level, LivingEntity shooter, Kind kind, float damage, float speed, int ticksToLive, float spread,
			float damageDropStart, float damageDropEnd, float damageMin, float side, double gravity) {
		super(TGEntities.FLAMETHROWER_PROJECTILE.get(), level, shooter, kind, damage, speed, ticksToLive, spread, damageDropStart, damageDropEnd, damageMin,
				side, gravity);
	}

	@Override
	public void tick() {
		if (isInWaterOrRain()) {
			discard();
			return;
		}
		super.tick();
	}

	@Override
	protected String trail() {
		return "FlamethrowerTrail";
	}

	@Override
	protected String impact() {
		return "FlamethrowerImpact";
	}

	@Override
	public void readSpawnData(RegistryFriendlyByteBuf buffer) {
		super.readSpawnData(buffer);
		muzzleFX("FlamethrowerFireFX", 0.15f, -0.05f, 0.5f);
	}

	@Override
	protected void onHitEntity(EntityHitResult result) {
		fireHit(result, 3);
	}
}
