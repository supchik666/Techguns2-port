package techguns.client.render;

import java.io.IOException;

import org.joml.Matrix4f;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.EffectInstance;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import techguns.Techguns;
import techguns.client.LightPulses;

/**
 * The color of {@link LightPulses}, which vanilla block light can't carry: after the world, each pixel's position comes
 * from the depth buffer, and the screen is brightened by the pulses' colors around it (dst * (1 + light)), entities
 * included. Runs before {@link Bloom} composites so glows aren't tinted. Off with shader mods, like Bloom.
 */
@EventBusSubscriber(modid = Techguns.MODID, value = Dist.CLIENT)
public final class LightTint {
	/** ponytail: constant until the M8 config. */
	private static final float STRENGTH = 1.5f;
	private static final int MAX_LIGHTS = 16;

	private static final float[] lights = new float[MAX_LIGHTS * 8];
	/** The light, drawn apart from the main target because the shader reads main's depth. */
	private static TextureTarget target;
	private static EffectInstance tint, add;
	private static boolean broken;

	private LightTint() {}

	@SubscribeEvent
	static void registerReloadListener(RegisterClientReloadListenersEvent event) {
		event.registerReloadListener((ResourceManagerReloadListener) manager -> {
			if (tint != null) tint.close();
			if (add != null) add.close();
			tint = add = null;
			if (ModList.get().isLoaded("iris") || ModList.get().isLoaded("oculus")) return;
			try {
				tint = new EffectInstance(manager, Techguns.id("light_tint").toString());
				add = new EffectInstance(manager, Techguns.id("bloom_add").toString());
				broken = false;
			} catch (IOException | RuntimeException e) {
				fail("loading its shaders", e);
			}
		});
	}

	@SubscribeEvent(priority = EventPriority.HIGH)
	static void afterLevel(RenderLevelStageEvent event) {
		if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL || broken || tint == null) return;
		int count = LightPulses.tints(lights, event.getCamera().getPosition(), event.getPartialTick().getGameTimeDeltaPartialTick(false));
		if (count == 0) return;
		RenderTarget main = Minecraft.getInstance().getMainRenderTarget();
		RenderSystem.disableDepthTest();
		RenderSystem.depthMask(false);
		try {
			if (target == null || target.width != main.width || target.height != main.height) {
				if (target != null) target.destroyBuffers();
				target = Bloom.target(main.width, main.height);
			}
			tint.setSampler("DepthSampler", main::getDepthTextureId);
			tint.safeGetUniform("InvViewProj").set(new Matrix4f(event.getProjectionMatrix()).mul(event.getModelViewMatrix()).invert());
			tint.safeGetUniform("LightCount").set(count);
			tint.safeGetUniform("Lights").set(lights);
			Bloom.pass(tint, main, target, null);
			add.safeGetUniform("ColorModulate").set(STRENGTH, STRENGTH, STRENGTH, 0);
			Bloom.pass(add, target, main, GlStateManager.SourceFactor.DST_COLOR);
		} catch (RuntimeException e) {
			fail("rendering", e);
		}
		main.bindWrite(true);
		RenderSystem.disableBlend();
		RenderSystem.defaultBlendFunc();
		RenderSystem.depthMask(true);
		RenderSystem.enableDepthTest();
	}

	private static void fail(String what, Exception e) {
		broken = true;
		Techguns.LOGGER.error("Light tint failed {}, turning it off", what, e);
	}
}
