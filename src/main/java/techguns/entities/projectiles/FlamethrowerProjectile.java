package techguns.entities.projectiles;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import techguns.TGEntities;
import techguns.damagesystem.TGDamageSource;
import techguns.damagesystem.TGDamageSource.DeathType;
import techguns.damagesystem.TGDamageType;
import techguns.damagesystem.TGExplosion;

/**
 * Flamethrower flame: an invisible bolt drawn by its TGFX trail; fire damage, ignites, goes out in water or rain.
 * Sets blocks alight (1.12 burnBlocks, half the hits) when its player shooter isn't in safe mode.
 */
public class FlamethrowerProjectile extends BlasterProjectile {
	private static final float IGNITE_CHANCE = 0.5f;

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

	@Override
	protected void onHitBlock(BlockHitResult result) {
		super.onHitBlock(result);
		if (level().isClientSide || !TGExplosion.blockDamage(getOwner()) || random.nextFloat() >= IGNITE_CHANCE) return;
		BlockPos pos = result.getBlockPos().relative(result.getDirection());
		if (BaseFireBlock.canBePlacedAt(level(), pos, result.getDirection())) level().setBlock(pos, BaseFireBlock.getState(level(), pos), Block.UPDATE_ALL_IMMEDIATE);
	}

	@Override
	protected TGDamageSource damageSource() {
		return source(TGDamageType.FIRE, DeathType.DEFAULT);
	}
}
