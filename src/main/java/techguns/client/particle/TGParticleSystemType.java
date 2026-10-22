package techguns.client.particle;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import techguns.Techguns;

/** A particle system as defined in {@code particles/*.txt}: settings for the system and the particles it spawns. Min/max pairs are random ranges. */
public class TGParticleSystemType implements Cloneable {
	enum RenderMode { ADDITIVE, ALPHA, ALPHA_SHADED, SOLID, NO_Z_TEST }

	enum VelocityType { NONE, SPHERICAL, ORTHO, HEMISPHERICAL, OUTWARD }

	enum VolumeType { SPHERE, CYLINDER, CYLINDER2, POINT, HEMISPHERE, TRAIL }

	/** At {@code time} (0-1 of the particle's life) the color is r/g/b; colors between entries are blended in HSB. */
	record ColorEntry(float r, float g, float b, float time) {}

	record AlphaEntry(float alpha, float time) {}

	RenderMode renderType = RenderMode.SOLID;
	ResourceLocation texture = Techguns.id("textures/fx/fireball.png");
	/** Sprite sheet layout; 1.12 ignored Rows and used 1/columns for the frame height too, so only columns is kept. */
	int columns = 1;
	int frames = 1;
	/** Frames are random variations instead of an animation. */
	boolean hasVariations;
	/** Particles are joined into a ribbon. */
	boolean streak;

	float angleMin, angleMax;
	float angleRateMin, angleRateMax;
	float angleRateDampingMin, angleRateDampingMax;

	/** Particle lifetime in ticks. */
	int lifetimeMin = 20, lifetimeMax = 20;
	/** The system starts after this many ticks. */
	int initialDelayMin, initialDelayMax;
	/** 1 = one full animation over the particle's lifetime. */
	float animationSpeedMin = 1, animationSpeedMax = 1;

	float sizeMin = 1, sizeMax = 1;
	/** Size increase per tick, multiplied by the damping every tick. */
	float sizeRateMin = 1, sizeRateMax = 1;
	float sizeRateDampingMin = 1, sizeRateDampingMax = 1;
	/** Growth per tick of the start size of newly spawned particles. */
	float startSizeRateMin, startSizeRateMax;
	float startSizeRateDampingMin = 1, startSizeRateDampingMax = 1;

	List<ColorEntry> colorEntries = new ArrayList<>();
	List<AlphaEntry> alphaEntries = new ArrayList<>();

	/** Particles per spawn. */
	int particleCountMin = 1, particleCountMax = 1;
	/** Ticks between spawns. */
	int spawnDelayMin = 1, spawnDelayMax = 1;
	/** The system's lifetime in ticks; 0 on an entity means while the entity lives. */
	int systemLifetimeMin = 5, systemLifetimeMax = 5;

	VelocityType velocityType = VelocityType.NONE;
	float[] velocityDataMin = new float[3];
	float[] velocityDataMax = new float[3];
	float velocityDampingMin = 1, velocityDampingMax = 1;
	/** Downward drift per tick. */
	float gravity;
	/** How much of the system's velocity is added to the particle's. */
	float systemVelocityFactorMin, systemVelocityFactorMax;

	Vec3 offset = Vec3.ZERO;

	VolumeType volumeType = VolumeType.POINT;
	float[] volumeData = new float[3];
	boolean isHollow;

	boolean particlesMoveWithSystem;
	boolean particlesStickToSystem;
	boolean groundAligned;
	/** Effect spawned on each particle. */
	String attachedSystem;

	/** For EXTENDS. 1.12 skipped a few fields (particleCountMin, startSizeRate...) when extending; its one EXTENDS doesn't set them. */
	TGParticleSystemType copy() {
		try {
			TGParticleSystemType copy = (TGParticleSystemType) clone();
			copy.colorEntries = new ArrayList<>(colorEntries);
			copy.alphaEntries = new ArrayList<>(alphaEntries);
			copy.velocityDataMin = velocityDataMin.clone();
			copy.velocityDataMax = velocityDataMax.clone();
			copy.volumeData = volumeData.clone();
			return copy;
		} catch (CloneNotSupportedException e) {
			throw new AssertionError(e);
		}
	}
}
