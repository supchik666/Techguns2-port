package techguns.damagesystem;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.EventHooks;
import techguns.TGConfig;
import techguns.entities.projectiles.GenericProjectile;
import techguns.capabilities.TGExtendedPlayer;
import techguns.plugins.sable.SableCompat;

/**
 * The 1.12 TG explosion: full damage within {@code primaryRadius}, falling to {@code secondaryDamage} at
 * {@code secondaryRadius}, nothing beyond; blocks give way along rays while their resistance is below the remaining
 * power times {@code blockDamageFactor}. A vanilla {@link Explosion} stands in for resistance, events and block breaking.
 * Hurts with TG explosion damage carrying the projectile's penetration. Rays also reach Sable ships: they push every ship
 * block they touch and break them like world blocks. Damage factors (PvP, NPC) apply in {@link DamageSystem}.
 */
public class TGExplosion {
	private final ServerLevel level;
	@Nullable
	private final Entity exploder;
	private final Entity projectile;
	private final Vec3 center;
	private final double primaryDamage, secondaryDamage, primaryRadius, secondaryRadius, blockDamageFactor;
	public float blockDropChance = 0.25f;
	/** Knockback strength in multiples of a vanilla hit (1.12 TGDamageSource.knockbackMultiplier). */
	public float knockback = 1.0f;

	public TGExplosion(ServerLevel level, @Nullable Entity exploder, Entity projectile, double x, double y, double z, double primaryDamage,
			double secondaryDamage, double primaryRadius, double secondaryRadius, double blockDamageFactor) {
		this.level = level;
		this.exploder = exploder;
		this.projectile = projectile;
		this.center = new Vec3(x, y, z);
		this.primaryDamage = primaryDamage;
		this.secondaryDamage = secondaryDamage;
		this.primaryRadius = primaryRadius;
		this.secondaryRadius = secondaryRadius;
		this.blockDamageFactor = blockDamageFactor;
	}

	/** 1.12 GenericGun.getDoBlockDamage: only players break blocks, and not in safe mode. */
	public static boolean blockDamage(@Nullable Entity shooter) {
		return TGConfig.EXPLOSION_BLOCK_DAMAGE.getAsBoolean() && shooter instanceof Player p && !TGExtendedPlayer.isSafemode(p);
	}

	/**
	 * Power at {@code distance}: 1.12 interpolated the wrong way round (rising to full power at the outer radius); this
	 * falls off as its names say.
	 */
	private double power(double distance) {
		if (distance <= primaryRadius) return primaryDamage;
		if (distance <= secondaryRadius) return primaryDamage + (distance - primaryRadius) / (secondaryRadius - primaryRadius) * (secondaryDamage - primaryDamage);
		return 0.0;
	}

	public void doExplosion(boolean playSound) {
		double totalRadius = Math.max(primaryRadius, secondaryRadius);
		boolean damagesTerrain = blockDamageFactor >= 0.01;
		TGDamageSource source = new TGDamageSource(TGDamageType.EXPLOSION, TGDamageSource.DeathType.GORE, projectile, exploder);
		if (projectile instanceof GenericProjectile p) source.penetration = p.getPenetration();
		Explosion explosion = new Explosion(level, projectile, source, null, center.x, center.y, center.z, (float) totalRadius, false,
				damagesTerrain ? Explosion.BlockInteraction.DESTROY : Explosion.BlockInteraction.KEEP, ParticleTypes.EXPLOSION,
				ParticleTypes.EXPLOSION_EMITTER, SoundEvents.GENERIC_EXPLODE);
		if (EventHooks.onExplosionStart(level, explosion)) return;
		if (playSound) {
			level.playSound(null, center.x, center.y, center.z, SoundEvents.GENERIC_EXPLODE, SoundSource.BLOCKS, 4.0f,
					(1.0f + (level.random.nextFloat() - level.random.nextFloat()) * 0.2f) * 0.7f);
		}

		SableCompat.Ships ships = SableCompat.ships(level, center, totalRadius);
		Map<BlockPos, Double> blocks = damagesTerrain || ships != null ? traceBlocks(explosion, totalRadius, damagesTerrain, ships) : Map.of();
		explosion.getToBlow().addAll(blocks.keySet());
		List<Entity> entities = level.getEntities(projectile, new AABB(center, center).inflate(totalRadius + 1));
		EventHooks.onExplosionDetonate(level, explosion, entities, totalRadius);

		// break blocks; beyond the primary radius they drop with a chance growing outwards
		for (BlockPos pos : explosion.getToBlow()) {
			double distance = blocks.getOrDefault(pos, 0.0);
			double dropChance = distance > primaryRadius ? blockDropChance * (distance - primaryRadius) / (secondaryRadius - primaryRadius) : 0;
			level.getBlockState(pos).onExplosionHit(level, pos, explosion, (stack, p) -> {
				if (level.random.nextDouble() < dropChance) Block.popResource(level, p, stack);
			});
		}

		for (Entity entity : entities) {
			if (entity.ignoreExplosion(explosion) || !entity.isAlive() || !entity.isPickable() || !EntitySelector.NO_CREATIVE_OR_SPECTATOR.test(entity)) continue;
			double damage = power(center.distanceTo(entity.getEyePosition()));
			if (damage <= 0) continue;
			Vec3 middle = new Vec3(entity.getX(), entity.getY() + entity.getEyeHeight() * 0.5, entity.getZ());
			if (level.clip(new ClipContext(center, middle, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, projectile)).getType() == HitResult.Type.BLOCK) continue;
			if (entity.hurt(source, (float) damage) && entity instanceof LivingEntity living) {
				living.knockback(0.4 * knockback, center.x - entity.getX(), center.z - entity.getZ());
			}
		}
	}

	/**
	 * 1.12 ray march from the center to every cell of the bounding cube's shell; returns the blocks to break (none unless
	 * {@code damagesTerrain}) and their distances. Each step looks at the world block, then at each ship's block there.
	 */
	private Map<BlockPos, Double> traceBlocks(Explosion explosion, double totalRadius, boolean damagesTerrain, @Nullable SableCompat.Ships ships) {
		int shipCount = ships == null ? 0 : ships.size();
		Map<BlockPos, Double> set = new HashMap<>();
		int radius = (int) Math.ceil(totalRadius);
		double stepOffset = 0.3;
		int steps = (int) Math.ceil(radius / stepOffset);
		for (int j = -radius; j < radius; ++j) {
			for (int k = -radius; k < radius; ++k) {
				for (int l = -radius; l < radius; ++l) {
					if (j != -radius && j != radius - 1 && k != -radius && k != radius - 1 && l != -radius && l != radius - 1) continue;
					Vec3 dir = new Vec3(j, k, l).normalize().scale(stepOffset);
					Vec3 p = center;
					double damping = 0.0;
					double power = primaryDamage;
					Set<BlockPos> damped = new HashSet<>();
					for (int i = 0; i < steps && power > 0.0; i++, p = p.add(dir)) {
						BlockPos pos = BlockPos.containing(p);
						if (!level.isInWorldBounds(pos)) break;
						double distance = center.distanceTo(pos.getCenter());
						power = power(distance);
						for (int s = -1; s < shipCount && power > 0.0; s++) {
							BlockPos at = s < 0 ? pos : ships.blockAt(s, p);
							BlockState state = level.getBlockState(at);
							if (state.isAir()) continue;
							if (s >= 0) ships.push(s, at);
							float resistance = state.getExplosionResistance(level, at, explosion);
							if (power - damping > 0 && resistance < (power - damping) * blockDamageFactor) {
								if (damagesTerrain) set.put(at, distance);
								if (damped.add(at)) damping += resistance;
							} else {
								power = 0.0;
							}
						}
					}
				}
			}
		}
		return set;
	}
}
