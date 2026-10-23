package techguns.client.particle;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.WeakHashMap;

import org.joml.Vector3f;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.util.FastColor.ARGB32;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.RenderLivingEvent;
import techguns.TGConfig;
import techguns.TGEffects;
import techguns.TGSounds;
import techguns.Techguns;
import techguns.client.particle.TGParticleSystemType.ColorEntry;
import techguns.damagesystem.DeathEffects;
import techguns.damagesystem.TGDamageSource.DeathType;

/**
 * Client side of {@link DeathEffects} (1.12 DeathEffect, FlyingGibs and DeathEffectEntityRenderer). The dying entity's
 * main model is captured by running its renderer into a recording buffer. GORE hides the entity and turns each cube of
 * the model into a gib that tumbles off from where it was, trailing blood; BIO and LASER redraw the model upright each
 * frame, fading into a pulsing overlay texture (BIO also swells each cube).
 * ponytail: the capture is cut into cubes every 24 vertices, which every vanilla and ported model cube is; armor, held
 * items and other layers vanish, as in 1.12. Gibs don't bounce out of lava like 1.12's.
 */
@EventBusSubscriber(modid = Techguns.MODID, value = Dist.CLIENT)
public final class DeathEffect {
	private static final ResourceLocation BIO_OVERLAY = Techguns.id("textures/fx/bio.png");
	private static final ResourceLocation LASER_OVERLAY = Techguns.id("textures/fx/laserdeath.png");
	private static final int CUBE = 24;
	private static final Map<LivingEntity, DeathType> dying = new WeakHashMap<>();
	private static final List<Gib> gibs = new ArrayList<>();
	private static boolean capturing;
	private static ClientLevel level;

	private DeathEffect() {}

	public static void play(DeathEffects.Death death) {
		Minecraft mc = Minecraft.getInstance();
		if (!TGConfig.DEATH_EFFECTS.get() || !(mc.level.getEntity(death.entityId()) instanceof LivingEntity entity)) return;
		dying.put(entity, death.deathType());
		Vector3f motion = death.motion();
		double x = entity.getX(), y = entity.getY() + entity.getBbHeight() / 2, z = entity.getZ();
		switch (death.deathType()) {
			case GORE -> {
				gore(entity, motion);
				sound(entity, TGSounds.DEATH_GORE.get());
			}
			case BIO -> {
				TGEffects.createFX(mc.level, "biodeath", x, y, z, motion.x, motion.y, motion.z);
				sound(entity, TGSounds.DEATH_BIO.get());
			}
			case LASER -> {
				TGEffects.createFX(mc.level, "laserdeathFire", x, y, z, motion.x, 0, motion.z);
				TGEffects.createFX(mc.level, "laserdeathAsh", x, y, z, motion.x, 0, motion.z);
				sound(entity, TGSounds.DEATH_LASER.get());
			}
			default -> {}
		}
	}

	private static void sound(LivingEntity entity, SoundEvent sound) {
		entity.level().playLocalSound(entity.getX(), entity.getY() + entity.getBbHeight() / 2, entity.getZ(), sound, entity.getSoundSource(), 1, 1, false);
	}

	/** A blood burst at the feet and the model's cubes flying off. */
	private static void gore(LivingEntity entity, Vector3f motion) {
		int blood = bloodColor(entity);
		float scale = (entity.getBbWidth() + entity.getBbHeight()) / 2.5f; // ponytail: 1.12 set this per mob, the size is close
		TGParticleSystemType main = tinted("GoreFX_Blood", blood), trail = tinted("GoreTrailFX_Blood", blood);
		if (main != null) {
			TGParticleSystem system = new TGParticleSystem(main, entity.getX(), entity.getY(), entity.getZ(), motion.x, motion.y, motion.z);
			system.scale = scale;
			TGParticleManager.add(system);
		}
		Capture model = capture(entity, 1, LightTexture.FULL_BRIGHT);
		if (model == null) return;
		Random rand = new Random();
		for (int cube = 0; cube < model.cubes(); cube++) {
			double vy = (entity.onGround() ? rand.nextDouble() : 0.5 - rand.nextDouble()) * 0.35;
			Vec3 velocity = new Vec3(motion.x * 0.35 + (0.5 - rand.nextDouble()) * 0.35, motion.y * 0.35 + vy, motion.z * 0.35 + (0.5 - rand.nextDouble()) * 0.35);
			gibs.add(new Gib(model, cube, entity.position(), velocity, trail, scale, rand));
		}
	}

	/** {@code name} with its first color replaced by {@code rgb} (1.12 GoreData.getExtendedType). */
	private static TGParticleSystemType tinted(String name, int rgb) {
		TGParticleSystemType type = TGFX.type(name);
		if (type == null) return null;
		type = type.copy();
		if (!type.colorEntries.isEmpty()) {
			type.colorEntries.set(0, new ColorEntry(ARGB32.red(rgb) / 255f, ARGB32.green(rgb) / 255f, ARGB32.blue(rgb) / 255f, type.colorEntries.get(0).time()));
		}
		return type;
	}

	/** 1.12 GoreData blood colors. */
	private static int bloodColor(LivingEntity entity) {
		return switch (BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).getPath()) {
			case "wither_skeleton" -> 0x323232;
			case "skeleton", "stray", "skeletonsoldier", "ghast", "ghastling" -> 0xFFFFFF;
			case "evoker", "vindicator" -> 0x6E1529;
			case "villager" -> 0x961533;
			case "creeper" -> 0x32AF39;
			case "enderman" -> 0xA024A7;
			case "spider", "cave_spider", "cyberdemon" -> 0x559C11;
			case "zombified_piglin", "zombiepigmansoldier" -> 0x6E330B;
			case "supermutantbasic", "supermutantelite", "supermutantheavy" -> 0x6D3C19;
			case "slime" -> 0x28FF28;
			case "magma_cube" -> 0x5C1A00;
			case "alienbug" -> 0xEBFF46;
			case "silverfish" -> 0x5A101B;
			case "vex" -> 0xD7D7D7;
			case "shulker" -> 0x7D006A;
			default -> entity.getType().is(EntityTypeTags.SKELETONS) ? 0xFFFFFF : entity.getType().is(EntityTypeTags.UNDEAD) ? 0x6E1529
					: entity instanceof Animal ? 0xAA1A25 : 0xA0151F;
		};
	}

	@SuppressWarnings("unchecked")
	private static LivingEntityRenderer<LivingEntity, ?> renderer(LivingEntity entity) {
		return Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(entity) instanceof LivingEntityRenderer<?, ?> renderer
				? (LivingEntityRenderer<LivingEntity, ?>) renderer : null;
	}

	/** {@code entity}'s main model as its renderer draws it, standing, relative to its feet; null if it draws none. */
	private static Capture capture(LivingEntity entity, float partialTick, int light) {
		LivingEntityRenderer<LivingEntity, ?> renderer = renderer(entity);
		if (renderer == null) return null;
		Capture capture = new Capture();
		int deathTime = entity.deathTime;
		entity.deathTime = 0; // no falling over, as 1.12 drew them
		capturing = true;
		try {
			renderer.render(entity, 0, partialTick, new PoseStack(), capture, light);
		} finally {
			capturing = false;
			entity.deathTime = deathTime;
		}
		return capture.type == null || capture.cubes() == 0 ? null : capture;
	}

	/** Hides a gored entity; draws a BIO or LASER one fading out. */
	@SubscribeEvent(priority = EventPriority.HIGHEST)
	static void onRenderLiving(RenderLivingEvent.Pre<?, ?> event) {
		LivingEntity entity = event.getEntity();
		DeathType type = capturing ? null : dying.get(entity);
		if (type == null) return;
		event.setCanceled(true);
		if (type == DeathType.GORE) return;
		float partialTick = event.getPartialTick();
		Capture model = capture(entity, partialTick, event.getPackedLight());
		if (model == null) return;
		float prog = Math.min(1, (entity.deathTime + partialTick) / 20), fade = 1 - prog * prog;
		int alpha = (int) ((1 - prog) * (1 - prog) * 255), f = (int) (fade * 255);
		int tint = type == DeathType.BIO ? ARGB32.color(alpha, f, 255, f) : ARGB32.color(alpha, 255, f, f);
		int pulse = (int) ((0.5 + Math.sin((Math.sqrt(prog) + 0.75) * 2 * Math.PI) / 2) * 255);
		float[] scales = new float[model.cubes()];
		Random rand = new Random(entity.getId());
		for (int cube = 0; cube < scales.length; cube++) scales[cube] = type == DeathType.BIO ? 1 + rand.nextFloat() * prog : 1;
		PoseStack pose = event.getPoseStack();
		MultiBufferSource buffers = event.getMultiBufferSource();
		// one buffer at a time: asking for the next one draws the previous
		draw(buffers.getBuffer(RenderType.entityTranslucent(renderer(entity).getTextureLocation(entity))), pose, model, scales, tint, event.getPackedLight());
		draw(buffers.getBuffer(RenderType.eyes(type == DeathType.BIO ? BIO_OVERLAY : LASER_OVERLAY)), pose, model, scales, ARGB32.color(255, pulse, pulse, pulse),
				LightTexture.FULL_BRIGHT);
	}

	private static void draw(VertexConsumer consumer, PoseStack pose, Capture model, float[] scales, int tint, int light) {
		for (int cube = 0; cube < scales.length; cube++) {
			Vec3 center = model.bounds(cube).getCenter();
			pose.pushPose();
			pose.translate(center.x, center.y, center.z);
			pose.scale(scales[cube], scales[cube], scales[cube]);
			pose.translate(-center.x, -center.y, -center.z);
			model.draw(consumer, pose.last(), cube, tint, light);
			pose.popPose();
		}
	}

	@SubscribeEvent
	static void tick(ClientTickEvent.Post event) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level != level) {
			level = mc.level;
			gibs.clear();
			dying.clear();
		}
		if (level == null || mc.isPaused()) return;
		gibs.forEach(gib -> gib.tick(level));
		gibs.removeIf(gib -> gib.ttl <= 0);
	}

	@SubscribeEvent
	static void render(RenderLevelStageEvent event) {
		if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES || gibs.isEmpty()) return;
		float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);
		Vec3 cam = event.getCamera().getPosition();
		MultiBufferSource.BufferSource buffers = Minecraft.getInstance().renderBuffers().bufferSource();
		PoseStack pose = new PoseStack();
		for (Gib gib : gibs) gib.render(pose, buffers, cam, partialTick);
		buffers.endLastBatch();
	}

	/** Records the first buffer asked for, the main model; drops everything else. */
	private static final class Capture implements MultiBufferSource, VertexConsumer {
		RenderType type;
		/** Per vertex: x, y, z, u, v, normal x, y, z. */
		final FloatArrayList vertices = new FloatArrayList();
		final IntArrayList colors = new IntArrayList();

		int cubes() {
			return colors.size() / CUBE;
		}

		AABB bounds(int cube) {
			float[] v = vertices.elements();
			AABB box = null;
			for (int i = cube * CUBE * 8; i < (cube + 1) * CUBE * 8; i += 8) {
				AABB point = new AABB(v[i], v[i + 1], v[i + 2], v[i], v[i + 1], v[i + 2]);
				box = box == null ? point : box.minmax(point);
			}
			return box;
		}

		void draw(VertexConsumer consumer, PoseStack.Pose pose, int cube, int tint, int light) {
			float[] v = vertices.elements();
			for (int i = cube * CUBE; i < (cube + 1) * CUBE; i++) {
				int o = i * 8;
				consumer.addVertex(pose, v[o], v[o + 1], v[o + 2]).setColor(ARGB32.multiply(colors.getInt(i), tint)).setUv(v[o + 3], v[o + 4])
						.setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, v[o + 5], v[o + 6], v[o + 7]);
			}
		}

		@Override
		public VertexConsumer getBuffer(RenderType renderType) {
			if (type != null) return NONE;
			type = renderType;
			return this;
		}

		@Override
		public VertexConsumer addVertex(float x, float y, float z) {
			vertices.addElements(vertices.size(), new float[] { x, y, z, 0, 0, 0, 1, 0 });
			colors.add(-1);
			return this;
		}

		@Override
		public VertexConsumer setColor(int red, int green, int blue, int alpha) {
			colors.set(colors.size() - 1, ARGB32.color(alpha, red, green, blue));
			return this;
		}

		@Override
		public VertexConsumer setUv(float u, float v) {
			vertices.set(vertices.size() - 5, u);
			vertices.set(vertices.size() - 4, v);
			return this;
		}

		@Override
		public VertexConsumer setUv1(int u, int v) {
			return this;
		}

		@Override
		public VertexConsumer setUv2(int u, int v) {
			return this;
		}

		@Override
		public VertexConsumer setNormal(float x, float y, float z) {
			vertices.set(vertices.size() - 3, x);
			vertices.set(vertices.size() - 2, y);
			vertices.set(vertices.size() - 1, z);
			return this;
		}
	}

	private static final VertexConsumer NONE = new VertexConsumer() {
		@Override
		public VertexConsumer addVertex(float x, float y, float z) {
			return this;
		}

		@Override
		public VertexConsumer setColor(int red, int green, int blue, int alpha) {
			return this;
		}

		@Override
		public VertexConsumer setUv(float u, float v) {
			return this;
		}

		@Override
		public VertexConsumer setUv1(int u, int v) {
			return this;
		}

		@Override
		public VertexConsumer setUv2(int u, int v) {
			return this;
		}

		@Override
		public VertexConsumer setNormal(float x, float y, float z) {
			return this;
		}
	};

	/** One cube of a gored model (1.12 FlyingGibs): falls, slides to a stop and sinks into the ground at the end. */
	private static final class Gib {
		final Capture model;
		final int cube;
		/** The cube's center in the model, and its collision box around it: as thick as its thinnest side. */
		final Vec3 center;
		final AABB box;
		final Vector3f axis;
		final TGParticleSystemType trail;
		final float trailScale;
		final int maxTtl;
		int ttl, hitGroundTtl;
		Vec3 pos, prevPos, motion;

		Gib(Capture model, int cube, Vec3 origin, Vec3 motion, TGParticleSystemType trail, float trailScale, Random rand) {
			this.model = model;
			this.cube = cube;
			AABB bounds = model.bounds(cube);
			center = bounds.getCenter();
			double half = Math.max(0.05, Math.min(bounds.getXsize(), Math.min(bounds.getYsize(), bounds.getZsize())) / 2);
			box = new AABB(-half, -half, -half, half, half, half);
			axis = new Vector3f(rand.nextFloat(), rand.nextFloat(), rand.nextFloat()).normalize();
			this.trail = trail;
			this.trailScale = trailScale;
			maxTtl = ttl = 75 + rand.nextInt(50);
			pos = prevPos = origin.add(center);
			this.motion = motion;
		}

		void tick(ClientLevel level) {
			ttl--;
			prevPos = pos;
			motion = motion.add(0, -0.03, 0);
			Vec3 move = Entity.collideBoundingBox(null, motion, box.move(pos), level, List.of());
			boolean onGround = motion.y < 0 && move.y != motion.y;
			pos = pos.add(move);
			motion = new Vec3(move.x != motion.x ? 0 : motion.x, move.y != motion.y ? 0 : motion.y, move.z != motion.z ? 0 : motion.z);
			if (onGround && hitGroundTtl == 0) hitGroundTtl = ttl;
			float friction = onGround ? 0.6f * 0.98f : 0.98f; // ponytail: 1.12's ground friction took the block the gib is in, nearly always air
			motion = new Vec3(motion.x * friction, motion.y * 0.98, motion.z * friction);
			if (hitGroundTtl == 0 && trail != null) {
				TGParticleSystem system = new TGParticleSystem(trail, pos.x, pos.y, pos.z, 0, 0, 0);
				system.scale = trailScale;
				TGParticleManager.add(system);
			}
		}

		void render(PoseStack pose, MultiBufferSource buffers, Vec3 cam, float partialTick) {
			Vec3 p = prevPos.lerp(pos, partialTick).subtract(cam);
			// spins slower over its life, and stops where it lands
			float rot = hitGroundTtl == 0 ? (maxTtl - ttl + partialTick) * (5 + 15f * ttl / maxTtl)
					: (maxTtl - hitGroundTtl) * (5 + 15f * hitGroundTtl / maxTtl);
			float sink = ttl < 20 ? (20 - ttl + partialTick) * -0.05f : 0;
			pose.pushPose();
			pose.translate(p.x, p.y + sink, p.z);
			pose.mulPose(Axis.of(axis).rotationDegrees(rot));
			pose.translate(-center.x, -center.y, -center.z);
			model.draw(buffers.getBuffer(model.type), pose.last(), cube, -1, LevelRenderer.getLightColor(Minecraft.getInstance().level, BlockPos.containing(pos)));
			pose.popPose();
		}
	}
}
