package techguns.entities.projectiles;

import javax.annotation.Nullable;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import techguns.TGEntities;

/** Tesla gun bolt: a beam that chains from each target it hits to the next one within 8 blocks, up to 4 times. */
public class TeslaProjectile extends AbstractBeamProjectile {
	static final int TTL = 10;
	static final int CHAIN_TARGETS = 4;
	static final float CHAIN_RANGE = 8.0f;
	static final float CHAIN_DAMAGE_FACTOR = 0.75f;

	private int chainTargets = CHAIN_TARGETS;
	/** Where a chain bolt came from, so it doesn't jump straight back. */
	@Nullable
	private Entity prevTarget;

	public TeslaProjectile(EntityType<? extends TeslaProjectile> type, Level level) {
		super(type, level);
	}

	public TeslaProjectile(Level level, LivingEntity shooter, Kind kind, float damage, float speed, int ticksToLive, float spread, float damageDropStart,
			float damageDropEnd, float damageMin, float side, double gravity) {
		super(TGEntities.TESLA_PROJECTILE.get(), level, shooter, kind, damage, speed, TTL, spread, damageDropStart, damageDropEnd, damageMin, side);
		trace();
	}

	/** A chain bolt from {@code source} to {@code target}; hits it at once. */
	private TeslaProjectile(TeslaProjectile parent, Entity source, LivingEntity target) {
		this(TGEntities.TESLA_PROJECTILE.get(), parent.level());
		setOwner(parent.getOwner());
		damage = parent.damage * CHAIN_DAMAGE_FACTOR;
		damageMin = parent.damageMin;
		damageDropStart = parent.damageDropStart;
		damageDropEnd = parent.damageDropEnd;
		maxTicks = TTL;
		chainTargets = parent.chainTargets - 1;
		prevTarget = source;
		Vec3 from = center(source), to = center(target);
		setPos(from);
		shoot(to.x - from.x, to.y - from.y, to.z - from.z, 1.0f, 0);
		distance = from.distanceTo(to);
		start = to;
		onHit(new EntityHitResult(target, to));
	}

	@Override
	protected void onHitEntity(EntityHitResult result) {
		super.onHitEntity(result);
		if (level().isClientSide || chainTargets <= 0) return;
		LivingEntity next = findNextTarget(result.getEntity());
		if (next != null) level().addFreshEntity(new TeslaProjectile(this, result.getEntity(), next));
	}

	@Nullable
	private LivingEntity findNextTarget(Entity last) {
		Vec3 from = center(last);
		for (LivingEntity entity : level().getEntitiesOfClass(LivingEntity.class, last.getBoundingBox().inflate(CHAIN_RANGE),
				e -> e != last && e != prevTarget && e != getOwner() && e.isAlive() && canHitEntity(e) && EntitySelector.NO_CREATIVE_OR_SPECTATOR.test(e))) {
			Vec3 to = center(entity);
			if (from.distanceTo(to) < CHAIN_RANGE
					&& level().clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this)).getType() == HitResult.Type.MISS) {
				return entity;
			}
		}
		return null;
	}

	private static Vec3 center(Entity entity) {
		return entity.position().add(0, entity.getEyeHeight() * 0.5, 0);
	}
}
