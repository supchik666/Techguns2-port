package techguns.client.particle;

import java.awt.Color;
import java.util.List;

import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Camera;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import techguns.client.particle.TGParticleSystemType.AlphaEntry;
import techguns.client.particle.TGParticleSystemType.ColorEntry;
import techguns.client.particle.TGParticleSystemType.RenderMode;
import techguns.client.render.ScreenEffect;

/** A particle of a {@link TGParticleSystem}: a camera-facing (or ground-aligned) quad, unlit, animated from a sprite sheet. */
public class TGParticle {
	final TGParticleSystem system;
	final TGParticleSystemType type;
	Vec3 pos, prevPos;
	private Vec3 velocity;
	private int age;
	private final int maxAge;
	private float size, prevSize;
	private float sizeRate;
	private final float sizeRateDamping;
	private float angle, angleRate;
	private final float angleRateDamping;
	private final float animationSpeed;
	private final float velocityDamping;
	private final int variationFrame;
	boolean dead;
	/** Set by {@link #prepare}. */
	float r, g, b, alpha, renderSize;

	TGParticle(TGParticleSystem system, Vec3 pos, Vec3 motion) {
		this.system = system;
		this.type = system.type;
		this.pos = this.prevPos = pos;
		maxAge = TGParticleSystem.randomInt(type.lifetimeMin, type.lifetimeMax);
		size = TGParticleSystem.randomFloat(type.sizeMin, type.sizeMax) * system.scale + system.startSize;
		sizeRate = TGParticleSystem.randomFloat(type.sizeRateMin, type.sizeRateMax) * system.scale;
		sizeRateDamping = TGParticleSystem.randomFloat(type.sizeRateDampingMin, type.sizeRateDampingMax);
		animationSpeed = TGParticleSystem.randomFloat(type.animationSpeedMin, type.animationSpeedMax);
		velocityDamping = TGParticleSystem.randomFloat(type.velocityDampingMin, type.velocityDampingMax);
		float systemVelocityFactor = TGParticleSystem.randomFloat(type.systemVelocityFactorMin, type.systemVelocityFactorMax);
		angle = TGParticleSystem.randomFloat(type.angleMin, type.angleMax);
		angleRate = TGParticleSystem.randomFloat(type.angleRateMin, type.angleRateMax);
		angleRateDamping = TGParticleSystem.randomFloat(type.angleRateDampingMin, type.angleRateDampingMax);
		velocity = motion.add(system.motion.scale(systemVelocityFactor));
		variationFrame = TGParticleSystem.RAND.nextInt(type.frames);
	}

	void tick() {
		prevPos = pos;
		prevSize = size;
		if (age++ >= maxAge) {
			dead = true;
			return;
		}
		if (type.particlesStickToSystem) {
			Entity entity = system.entity;
			if (system.dead || entity != null && !entity.isAlive()) {
				dead = true;
				return;
			}
			if (entity == null) {
				pos = system.pos;
			} else if (type.particlesMoveWithSystem && system.attachToHead && entity instanceof LivingEntity living) {
				Vec3 offset = system.entityOffset.add(type.offset);
				float eyes = living.getEyeHeight();
				prevPos = new Vec3(living.xo, living.yo + eyes, living.zo)
						.add(offset.xRot(-living.xRotO * Mth.DEG_TO_RAD).yRot(-living.yHeadRotO * Mth.DEG_TO_RAD));
				pos = living.position().add(0, eyes, 0)
						.add(offset.xRot(-living.getXRot() * Mth.DEG_TO_RAD).yRot(-living.getYHeadRot() * Mth.DEG_TO_RAD));
			} else {
				prevPos = new Vec3(entity.xo, entity.yo, entity.zo);
				pos = entity.position();
			}
		} else if (type.particlesMoveWithSystem) {
			// sic: 1.12 mirrors the particle through the system, then rotates by the system's turn this tick
			float dPitch = (system.pitch - system.prevPitch) * Mth.DEG_TO_RAD, dYaw = (system.yaw - system.prevYaw) * Mth.DEG_TO_RAD;
			pos = system.pos.add(system.pos.subtract(pos).yRot(-dYaw).xRot(-dPitch));
		}
		pos = pos.add(velocity.x, velocity.y - type.gravity, velocity.z);
		velocity = velocity.scale(velocityDamping);
		size = Math.max(0, size + sizeRate);
		sizeRate *= sizeRateDamping;
		angle = (angle + angleRate) % 360;
		angleRate *= angleRateDamping;
	}

	/** Interpolates color, alpha and size for this frame. */
	void prepare(float partialTick) {
		float progress = (age + partialTick) / maxAge;
		List<ColorEntry> colors = type.colorEntries;
		if (colors.isEmpty()) {
			r = g = b = 1;
		} else {
			ColorEntry c1 = colors.get(0), c2 = c1;
			for (int i = 1; i < colors.size(); i++) {
				c2 = colors.get(i);
				if (progress < c2.time()) break;
				c1 = c2;
			}
			if (c1 == c2) {
				r = c1.r();
				g = c1.g();
				b = c1.b();
			} else {
				float p = (progress - c1.time()) / (c2.time() - c1.time());
				float[] hsb1 = Color.RGBtoHSB((int) (c1.r() * 255), (int) (c1.g() * 255), (int) (c1.b() * 255), null);
				float[] hsb2 = Color.RGBtoHSB((int) (c2.r() * 255), (int) (c2.g() * 255), (int) (c2.b() * 255), null);
				int rgb = Color.HSBtoRGB(Mth.lerp(p, hsb1[0], hsb2[0]), Mth.lerp(p, hsb1[1], hsb2[1]), Mth.lerp(p, hsb1[2], hsb2[2]));
				r = (rgb >> 16 & 255) / 255f;
				g = (rgb >> 8 & 255) / 255f;
				b = (rgb & 255) / 255f;
			}
		}
		List<AlphaEntry> alphas = type.alphaEntries;
		if (alphas.isEmpty()) {
			alpha = 1;
		} else {
			AlphaEntry a1 = alphas.get(0), a2 = a1;
			for (int i = 1; i < alphas.size(); i++) {
				a2 = alphas.get(i);
				if (progress < a2.time()) break;
				a1 = a2;
			}
			alpha = a1.time() != a2.time() ? Mth.lerp((progress - a1.time()) / (a2.time() - a1.time()), a1.alpha(), a2.alpha()) : a1.alpha();
		}
		renderSize = Mth.lerp(partialTick, prevSize, size);
	}

	/** Sprite sheet cell of this frame: u1, v1, u2, v2. Frame height is 1/columns too, as in 1.12 (the 4x2 sheets rely on it). */
	float[] uv(float partialTick) {
		int frame = type.hasVariations ? variationFrame : (int) (type.frames * ((age + partialTick) / maxAge * animationSpeed)) % type.frames;
		float cell = 1.0f / type.columns;
		float u = frame % type.columns * cell, v = frame / type.columns * cell;
		return new float[] { u, v, u + cell, v + cell };
	}

	RenderType renderType() {
		// ponytail: SOLID/ALPHA_SHADED draw as ALPHA and NO_Z_TEST as ADDITIVE with depth test; no effect in use needs more
		return type.renderType == RenderMode.ADDITIVE || type.renderType == RenderMode.NO_Z_TEST ? ScreenEffect.additive(type.texture, type.bloom)
				: ScreenEffect.alpha(type.texture, type.bloom);
	}

	void render(MultiBufferSource buffers, Camera camera, float partialTick) {
		prepare(partialTick);
		float[] uv = uv(partialTick);
		Vec3 center = prevPos.lerp(pos, partialTick).subtract(camera.getPosition());
		float a = (angle + partialTick * angleRate) * Mth.DEG_TO_RAD;
		Quaternionf rotation = type.groundAligned ? new Quaternionf().rotationY(a) : new Quaternionf(camera.rotation()).rotateZ(a);
		float s = 0.1f * renderSize;
		VertexConsumer consumer = buffers.getBuffer(renderType());
		if (type.groundAligned) {
			vertex(consumer, center, rotation, -s, 0, -s, uv[2], uv[3]);
			vertex(consumer, center, rotation, s, 0, -s, uv[2], uv[1]);
			vertex(consumer, center, rotation, s, 0, s, uv[0], uv[1]);
			vertex(consumer, center, rotation, -s, 0, s, uv[0], uv[3]);
		} else {
			vertex(consumer, center, rotation, s, -s, 0, uv[2], uv[3]);
			vertex(consumer, center, rotation, s, s, 0, uv[2], uv[1]);
			vertex(consumer, center, rotation, -s, s, 0, uv[0], uv[1]);
			vertex(consumer, center, rotation, -s, -s, 0, uv[0], uv[3]);
		}
	}

	/** An item-attached particle, in the gun model's frame: a quad in its x-y plane. */
	void renderAttached(PoseStack pose, MultiBufferSource buffers, float partialTick) {
		prepare(partialTick);
		float[] uv = uv(partialTick);
		Vec3 c = prevPos.lerp(pos, partialTick);
		Matrix4f m = new Matrix4f(pose.last().pose()).translate((float) c.x, (float) c.y, (float) c.z)
				.rotateZ((angle + partialTick * angleRate) * Mth.DEG_TO_RAD);
		float s = 0.1f * renderSize;
		VertexConsumer consumer = buffers.getBuffer(renderType());
		vertex(consumer, m, s, -s, uv[2], uv[3]);
		vertex(consumer, m, s, s, uv[2], uv[1]);
		vertex(consumer, m, -s, s, uv[0], uv[1]);
		vertex(consumer, m, -s, -s, uv[0], uv[3]);
	}

	private void vertex(VertexConsumer consumer, Matrix4f m, float x, float y, float u, float v) {
		Vector3f p = m.transformPosition(x, y, 0, new Vector3f());
		vertex(consumer, p.x, p.y, p.z, u, v, r, g, b, alpha);
	}

	private void vertex(VertexConsumer consumer, Vec3 center, Quaternionf rotation, float x, float y, float z, float u, float v) {
		Vector3f p = rotation.transform(new Vector3f(x, y, z));
		vertex(consumer, center.x + p.x, center.y + p.y, center.z + p.z, u, v, r, g, b, alpha);
	}

	static void vertex(VertexConsumer consumer, double x, double y, double z, float u, float v, float r, float g, float b, float alpha) {
		consumer.addVertex((float) x, (float) y, (float) z).setColor(r, g, b, alpha).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY)
				.setLight(LightTexture.FULL_BRIGHT).setNormal(0, 1, 0);
	}
}
