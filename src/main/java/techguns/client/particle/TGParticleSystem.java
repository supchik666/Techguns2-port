package techguns.client.particle;

import java.util.Random;
import java.util.function.Predicate;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import techguns.client.particle.TGParticleSystemType.VolumeType;

/** Spawns the particles of a {@link TGParticleSystemType}; draws nothing itself. Can follow an entity or a particle. */
public class TGParticleSystem {
	static final Random RAND = new Random();

	final TGParticleSystemType type;
	/** Followed while {@link #condition} holds. */
	final Entity entity;
	Predicate<Entity> condition = Entity::isAlive;
	/** Followed while alive (AttachedSystem). */
	final TGParticle parent;
	/** On an entity: offset in its rotated frame, from its eyes if attachToHead; in the held gun's model frame if itemAttached. */
	Vec3 entityOffset = Vec3.ZERO;
	boolean attachToHead;
	/** Lives in the frame of the entity's held gun model, drawn by its renderer (1.12 createFXOnEntityItemAttached). */
	boolean itemAttached;

	Vec3 pos;
	Vec3 motion;
	float pitch, yaw, prevPitch, prevYaw;
	float scale = 1;
	boolean dead;

	private final int systemLifetime;
	private int initialDelay;
	private int spawnDelay;
	private int ticksExisted;
	float startSize;
	private float startSizeRate;
	private final float startSizeRateDamping;
	private TGParticleStreak lastStreak;
	/** Direction data from the volume for the velocity (1.12 DirResult): a yaw angle, a direction, or nothing. */
	private float[] dir;

	private TGParticleSystem(TGParticleSystemType type, Vec3 pos, Vec3 motion, Entity entity, TGParticle parent) {
		this.type = type;
		this.pos = pos.add(type.offset);
		this.motion = motion;
		this.entity = entity;
		this.parent = parent;
		systemLifetime = randomInt(type.systemLifetimeMin, type.systemLifetimeMax);
		initialDelay = randomInt(type.initialDelayMin, type.initialDelayMax);
		startSizeRate = randomFloat(type.startSizeRateMin, type.startSizeRateMax);
		startSizeRateDamping = randomFloat(type.startSizeRateDampingMin, type.startSizeRateDampingMax);
	}

	TGParticleSystem(TGParticleSystemType type, double x, double y, double z, double motionX, double motionY, double motionZ) {
		this(type, new Vec3(x, y, z), new Vec3(motionX, motionY, motionZ), null, null);
	}

	TGParticleSystem(TGParticleSystemType type, Entity entity) {
		this(type, entity.position(), Vec3.ZERO, entity, null);
	}

	TGParticleSystem(TGParticleSystemType type, TGParticle parent) {
		this(type, parent.pos, Vec3.ZERO, null, parent);
	}

	void tick() {
		prevPitch = pitch;
		prevYaw = yaw;
		if (entity != null) {
			if (!condition.test(entity)) {
				dead = true;
				return;
			}
			Vec3 offset = type.offset.add(entityOffset);
			if (itemAttached) {
				pos = offset;
			} else {
				Vec3 base = new Vec3(entity.xo, entity.yo, entity.zo);
				if (attachToHead && entity instanceof LivingEntity living) {
					pitch = living.getXRot();
					yaw = living.getYHeadRot();
					base = base.add(0, living.getEyeHeight(), 0);
				} else {
					pitch = entity.getXRot();
					yaw = entity.getYRot();
				}
				pos = base.add(offset.xRot(-pitch * Mth.DEG_TO_RAD).yRot(-yaw * Mth.DEG_TO_RAD));
				motion = entity.getDeltaMovement();
			}
		} else if (parent != null) {
			pos = parent.pos.add(type.offset);
		} else {
			pos = pos.add(motion);
		}

		if (initialDelay-- > 0) return;
		startSize += startSizeRate;
		startSizeRate *= startSizeRateDamping;
		if (spawnDelay-- <= 0) {
			int count = randomInt(type.particleCountMin, type.particleCountMax);
			for (int i = 0; i < count; i++) spawn(i, count);
			spawnDelay = randomInt(type.spawnDelayMin, type.spawnDelayMax);
		}

		if (entity != null ? systemLifetime > 0 && ticksExisted >= systemLifetime : parent != null ? parent.dead : ticksExisted >= systemLifetime) {
			dead = true;
		}
		ticksExisted++;
	}

	private void spawn(int i, int count) {
		dir = new float[0];
		Vec3 position = volumePosition(i, count).scale(scale);
		Vec3 velocity = velocity().scale(scale).xRot(-pitch * Mth.DEG_TO_RAD).yRot(-yaw * Mth.DEG_TO_RAD);
		if (type.volumeType != VolumeType.TRAIL) position = position.xRot(-pitch * Mth.DEG_TO_RAD).yRot(-yaw * Mth.DEG_TO_RAD);
		position = pos.add(position);
		velocity = velocity.scale(0.05); // the files give velocities per second
		TGParticle particle;
		if (type.streak) {
			TGParticleStreak streak = new TGParticleStreak(this, position, velocity);
			if (lastStreak != null) {
				streak.prev = lastStreak;
				lastStreak.next = streak;
			}
			lastStreak = streak;
			particle = streak;
		} else {
			particle = new TGParticle(this, position, velocity);
		}
		TGParticleManager.add(particle);
		if (type.attachedSystem != null) {
			for (TGParticleSystem system : TGFX.create(type.attachedSystem, t -> new TGParticleSystem(t, particle))) {
				system.scale = scale;
				TGParticleManager.add(system);
			}
		}
	}

	/** Spawn offset from the system; sets {@link #dir}. */
	private Vec3 volumePosition(int i, int count) {
		float[] v = type.volumeData;
		float r = type.isHollow ? v[0] : randomFloat(0, v[0]);
		float angle = RAND.nextFloat() * Mth.TWO_PI;
		switch (type.volumeType) {
			case CYLINDER -> {
				Vec3 direction = new Vec3(1, 0, 0).yRot(angle);
				dir = new float[] { angle };
				return new Vec3(direction.x * r, RAND.nextFloat() * v[1], direction.z * r);
			}
			case CYLINDER2 -> {
				Vec3 direction = new Vec3(0, 1, 0).xRot(angle);
				setDir(direction);
				return new Vec3(direction.x * r, direction.y * r, RAND.nextFloat() * v[1]);
			}
			case SPHERE, HEMISPHERE -> {
				double y = type.volumeType == VolumeType.SPHERE ? 1 - RAND.nextDouble() * 2 : RAND.nextDouble();
				double a = Math.sqrt(1 - y * y);
				Vec3 direction = new Vec3(a * Math.cos(angle), y, a * Math.sin(angle));
				setDir(direction);
				return direction.scale(r);
			}
			case TRAIL -> {
				if (entity == null) return Vec3.ZERO;
				Vec3 direction = entity.getDeltaMovement().normalize();
				setDir(direction);
				return direction.scale(-(float) i / count * v[0]);
			}
			default -> {
				return Vec3.ZERO;
			}
		}
	}

	private void setDir(Vec3 direction) {
		dir = new float[] { (float) direction.x, (float) direction.y, (float) direction.z };
	}

	private Vec3 velocity() {
		float[] min = type.velocityDataMin, max = type.velocityDataMax;
		float r = randomFloat(min[0], max[0]);
		double angle = RAND.nextDouble() * 2 * Math.PI;
		return switch (type.velocityType) {
			case ORTHO -> new Vec3(r, randomFloat(min[1], max[1]), randomFloat(min[2], max[2]));
			case OUTWARD -> {
				float y = randomFloat(min[1], max[1]);
				if (dir.length == 1) yield new Vec3(r * Math.cos(-dir[0]), y, r * Math.sin(-dir[0])); // cylinder: its yaw angle
				if (dir.length == 3) yield new Vec3(dir[0] * r, dir[1] * r, dir[2] * r);
				yield new Vec3(r * Math.cos(angle), y, r * Math.sin(angle));
			}
			case SPHERICAL -> {
				double y = 1 - RAND.nextDouble() * 2, a = Math.sqrt(1 - y * y);
				yield new Vec3(r * a * Math.cos(angle), r * y, r * a * Math.sin(angle));
			}
			case HEMISPHERICAL -> { // sic: z is the "up" axis here in 1.12
				double y = RAND.nextDouble(), a = Math.sqrt(1 - y * y);
				yield new Vec3(r * a * Math.cos(angle), r * a * Math.sin(angle), r * y);
			}
			case NONE -> Vec3.ZERO;
		};
	}

	/** Inclusive; the bounds may be in either order. */
	static int randomInt(int min, int max) {
		return Math.min(min, max) + RAND.nextInt(Math.abs(max - min) + 1);
	}

	static float randomFloat(float min, float max) {
		return min + RAND.nextFloat() * (max - min);
	}
}
