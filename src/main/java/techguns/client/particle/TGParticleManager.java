package techguns.client.particle;

import java.util.function.Predicate;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import techguns.TGEffects;
import techguns.client.LightPulses;
import techguns.Techguns;

/**
 * Ticks and draws all TGFX systems and particles, and is the client's {@link TGEffects.Handler}. Particles are drawn
 * after vanilla's, back to front; streaks after them in spawn order; those attached to a held gun by its renderer.
 * ponytail: DeathEffect isn't ported.
 */
@EventBusSubscriber(modid = Techguns.MODID, value = Dist.CLIENT)
public final class TGParticleManager implements TGEffects.Handler {
	private static final List<TGParticleSystem> systems = new ArrayList<>();
	private static final List<TGParticle> particles = new ArrayList<>();
	private static final List<TGParticle> streaks = new ArrayList<>();
	private static final List<TGParticle> attached = new ArrayList<>();
	private static ClientLevel level;

	static void add(TGParticleSystem system) {
		systems.add(system);
	}

	static void add(TGParticle particle) {
		(particle.system.itemAttached ? attached : particle instanceof TGParticleStreak ? streaks : particles).add(particle);
	}

	@Override
	public void createFX(Level level, String name, double x, double y, double z, double motionX, double motionY, double motionZ, float pitch,
			float yaw, float scale) {
		for (TGParticleSystem system : TGFX.create(name, type -> new TGParticleSystem(type, x, y, z, motionX, motionY, motionZ))) {
			system.pitch = pitch;
			system.yaw = yaw;
			system.scale = scale;
			add(system);
		}
		LightPulses.fx(name, new Vec3(x, y, z), scale);
	}

	@Override
	public void createFXOnEntity(Entity entity, String name, float scale, Vec3 offset, boolean attachToHead, Predicate<Entity> condition) {
		for (TGParticleSystem system : TGFX.create(name, type -> new TGParticleSystem(type, entity))) {
			system.scale = scale;
			system.entityOffset = offset;
			system.attachToHead = attachToHead;
			system.condition = condition;
			add(system);
		}
	}

	/** {@code name} on the gun {@code entity} holds, while {@code condition} holds; see {@link #renderAttached}. */
	public static void createItemAttached(Entity entity, String name, float scale, Vec3 offset, Predicate<Entity> condition) {
		for (TGParticleSystem system : TGFX.create(name, type -> new TGParticleSystem(type, entity))) {
			system.scale = scale;
			system.entityOffset = offset;
			system.itemAttached = true;
			system.condition = condition;
			add(system);
		}
	}

	/** Draws {@code entity}'s item-attached particles in {@code pose}, the frame of its held gun model. */
	public static void renderAttached(Entity entity, PoseStack pose, MultiBufferSource buffers, float partialTick) {
		for (TGParticle particle : attached) {
			if (particle.system.entity == entity) particle.renderAttached(pose, buffers, partialTick);
		}
	}

	@SubscribeEvent
	static void setup(FMLClientSetupEvent event) {
		TGEffects.client = new TGParticleManager();
	}

	@SubscribeEvent
	static void registerReloadListener(RegisterClientReloadListenersEvent event) {
		event.registerReloadListener((ResourceManagerReloadListener) TGFX::load);
	}

	@SubscribeEvent
	static void tick(ClientTickEvent.Post event) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level != level) {
			level = mc.level;
			systems.clear();
			particles.clear();
			streaks.clear();
			attached.clear();
		}
		if (level == null || mc.isPaused()) return;
		for (int i = 0; i < systems.size(); i++) systems.get(i).tick(); // index loop: attached systems get added while ticking
		systems.removeIf(system -> system.dead);
		particles.forEach(TGParticle::tick);
		particles.removeIf(particle -> particle.dead);
		streaks.forEach(TGParticle::tick);
		streaks.removeIf(particle -> particle.dead);
		attached.forEach(TGParticle::tick);
		attached.removeIf(particle -> particle.dead);
	}

	@SubscribeEvent
	static void render(RenderLevelStageEvent event) {
		if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES || particles.isEmpty() && streaks.isEmpty()) return;
		Camera camera = event.getCamera();
		float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);
		Vec3 cam = camera.getPosition(), look = new Vec3(camera.getLookVector());
		particles.sort(Comparator.comparingDouble(particle -> -look.dot(particle.pos.subtract(cam))));
		MultiBufferSource.BufferSource buffers = Minecraft.getInstance().renderBuffers().bufferSource();
		for (TGParticle particle : particles) particle.render(buffers, camera, partialTick);
		for (TGParticle streak : streaks) streak.render(buffers, camera, partialTick);
		buffers.endBatch();
	}
}
