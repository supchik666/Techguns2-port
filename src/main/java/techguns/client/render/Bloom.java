package techguns.client.render;

import java.io.IOException;

import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.EffectInstance;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import techguns.TGConfig;
import techguns.Techguns;

/**
 * Soft glow around our own glowing draws (muzzle flashes, beams, TGFX), not a brightness threshold over the screen.
 * Render types from {@link #wrap} draw a second time into a mask that shares the main depth buffer, so walls hide the
 * glow; it works in the hand pass too. The mask is blurred at 1/2 to 1/16 size and added onto the screen twice a frame:
 * after the world, so the held gun covers world glows (its own trail, a charge flare), and after the hand for muzzle flashes.
 * Off while an Iris/Oculus shader pack is on; a shader or framebuffer failure turns it off instead of crashing.
 */
@EventBusSubscriber(modid = Techguns.MODID, value = Dist.CLIENT)
public final class Bloom {
	/** The 8-bit mask holds bloom strength / this, so strong glows (explosions) don't clip. */
	private static final float MASK_SCALE = 4;
	private static final int LEVELS = 4;

	private static TextureTarget mask;
	/** Per level: the horizontal blur, then the vertical one that holds the level's result. Level i is 1/2^(i+1) size. */
	private static final TextureTarget[] targets = new TextureTarget[LEVELS * 2];
	private static EffectInstance blur, add;
	/** The depth texture attached to the mask. */
	private static int depthId = -1;
	/** The mask was drawn into this frame. */
	private static boolean drawn;
	private static boolean broken;

	private Bloom() {}

	/** {@code base}, also drawn into the bloom mask {@code bloom} times as bright; {@code base} itself for no bloom. */
	public static RenderType wrap(RenderType base, float bloom) {
		return bloom > 0 ? new Glowing(base, bloom) : base;
	}

	private static final class Glowing extends RenderType {
		private final float bloom;

		Glowing(RenderType base, float bloom) {
			super(base + "_bloom" + bloom, base.format(), base.mode(), base.bufferSize(), base.affectsCrumbling(), base.sortOnUpload(),
					base::setupRenderState, base::clearRenderState);
			this.bloom = bloom;
		}

		@Override
		public void draw(MeshData mesh) {
			setupRenderState();
			BufferUploader.drawWithShader(mesh);
			drawMask(format(), bloom);
			clearRenderState();
		}
	}

	private static java.lang.reflect.Method shaderPackInUse;
	private static Object irisApi;

	/** Iris/Oculus with a shader pack on: their pipeline replaces ours, so the screen passes step aside. Reflection keeps them off the build. */
	public static boolean shaderPack() {
		if (irisApi == null) {
			if (!ModList.get().isLoaded("iris") && !ModList.get().isLoaded("oculus")) return false;
			try {
				irisApi = Class.forName("net.irisshaders.iris.api.v0.IrisApi").getMethod("getInstance").invoke(null);
				shaderPackInUse = irisApi.getClass().getMethod("isShaderPackInUse");
			} catch (ReflectiveOperationException | LinkageError e) {
				return true;
			}
		}
		try {
			return (boolean) shaderPackInUse.invoke(irisApi);
		} catch (ReflectiveOperationException e) {
			return true;
		}
	}

	private static boolean ready() {
		return !broken && blur != null && add != null && mask != null && TGConfig.BLOOM.getAsBoolean() && !shaderPack();
	}

	/** Draws the immediate vertex buffer of {@code format} (just uploaded and drawn) again into the mask. */
	private static void drawMask(VertexFormat format, float bloom) {
		if (!ready() || !RenderSystem.isOnRenderThread()) return;
		int previous = GlStateManager.getBoundFramebuffer();
		GlStateManager._glBindFramebuffer(GL30.GL_FRAMEBUFFER, mask.frameBufferId);
		// main's depth, checked per draw: Veil (Sable) swaps in a first-person target with its own depth for the hand pass
		int depth = Minecraft.getInstance().getMainRenderTarget().getDepthTextureId();
		if (depth != depthId) {
			depthId = depth;
			GlStateManager._glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_DEPTH_ATTACHMENT, GL11.GL_TEXTURE_2D, depth, 0);
		}
		if (!drawn) {
			drawn = true;
			GL30.glClearBufferfv(GL11.GL_COLOR, 0, new float[4]); // not glClear: that would change vanilla's clear color
		}
		float[] color = RenderSystem.getShaderColor();
		float r = color[0], g = color[1], b = color[2], a = color[3], k = bloom / MASK_SCALE;
		RenderSystem.setShaderColor(r * k, g * k, b * k, a);
		format.getImmediateDrawVertexBuffer().drawWithShader(RenderSystem.getModelViewMatrix(), RenderSystem.getProjectionMatrix(),
				RenderSystem.getShader());
		RenderSystem.setShaderColor(r, g, b, a);
		GlStateManager._glBindFramebuffer(GL30.GL_FRAMEBUFFER, previous);
	}

	@SubscribeEvent
	static void registerReloadListener(RegisterClientReloadListenersEvent event) {
		event.registerReloadListener((ResourceManagerReloadListener) manager -> {
			if (blur != null) blur.close();
			if (add != null) add.close();
			blur = add = null;
			try {
				blur = new EffectInstance(manager, "box_blur");
				add = new EffectInstance(manager, Techguns.id("bloom_add").toString());
				broken = false;
			} catch (IOException | RuntimeException e) {
				fail("loading its shaders", e);
			}
		});
	}

	/** Starts a frame: the mask gets cleared by its first draw; targets follow the main target's size. */
	@SubscribeEvent
	static void frame(RenderFrameEvent.Pre event) {
		drawn = false;
		if (broken || blur == null) return;
		RenderTarget main = Minecraft.getInstance().getMainRenderTarget();
		if (mask != null && mask.width == main.width && mask.height == main.height) return;
		int previous = GlStateManager.getBoundFramebuffer();
		try {
			if (mask != null) mask.destroyBuffers(); // the mask has no depth of its own, so this leaves main's alone
			for (TextureTarget target : targets) if (target != null) target.destroyBuffers();
			mask = target(main.width, main.height);
			for (int i = 0; i < LEVELS; i++) {
				int w = Math.max(1, main.width >> i + 1), h = Math.max(1, main.height >> i + 1);
				targets[2 * i] = target(w, h);
				targets[2 * i + 1] = target(w, h);
			}
			depthId = -1; // attached by the first draw
		} catch (RuntimeException e) {
			fail("creating its framebuffers", e);
		}
		GlStateManager._glBindFramebuffer(GL30.GL_FRAMEBUFFER, previous);
		RenderSystem.viewport(0, 0, main.width, main.height);
	}

	static TextureTarget target(int width, int height) {
		TextureTarget target = new TextureTarget(width, height, false, Minecraft.ON_OSX);
		target.setFilterMode(GL11.GL_LINEAR);
		return target;
	}

	@SubscribeEvent
	static void afterLevel(RenderLevelStageEvent event) {
		if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_LEVEL) composite();
	}

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	static void afterHand(RenderGuiEvent.Pre event) {
		composite();
	}

	/** Blurs the mask and adds it onto the screen; the next glowing draw starts a fresh mask. */
	private static void composite() {
		if (!drawn || !ready()) return;
		drawn = false;
		RenderTarget main = Minecraft.getInstance().getMainRenderTarget();
		RenderSystem.disableDepthTest();
		RenderSystem.depthMask(false);
		try {
			RenderTarget in = mask;
			for (int i = 0; i < LEVELS; i++) {
				// box_blur's Radius is in texels of its input
				blurPass(in, targets[2 * i], 1, 0, 6);
				blurPass(targets[2 * i], targets[2 * i + 1], 0, 1, 3);
				in = targets[2 * i + 1];
			}
			float k = TGConfig.BLOOM_INTENSITY.get().floatValue() * MASK_SCALE / LEVELS;
			add.safeGetUniform("ColorModulate").set(k, k, k, 0);
			for (int i = 0; i < LEVELS; i++) pass(add, targets[2 * i + 1], main, GlStateManager.SourceFactor.ONE);
		} catch (RuntimeException e) {
			fail("compositing", e);
		}
		main.bindWrite(true);
		RenderSystem.disableBlend();
		RenderSystem.defaultBlendFunc();
		RenderSystem.depthMask(true);
		RenderSystem.enableDepthTest();
	}

	private static void blurPass(RenderTarget in, RenderTarget out, float dirX, float dirY, float radius) {
		blur.safeGetUniform("BlurDir").set(dirX, dirY);
		blur.safeGetUniform("Radius").set(radius);
		pass(blur, in, out, null);
	}

	/** A full-target quad sampling {@code in}, blended onto {@code out} as {@code blend} * src + dst, or replacing it if null. */
	static void pass(EffectInstance effect, RenderTarget in, RenderTarget out, GlStateManager.SourceFactor blend) {
		out.bindWrite(true);
		effect.setSampler("DiffuseSampler", in::getColorTextureId);
		effect.safeGetUniform("ProjMat").set(new Matrix4f().setOrtho(0, out.width, 0, out.height, 0.1f, 1000));
		effect.safeGetUniform("InSize").set((float) in.width, (float) in.height);
		effect.safeGetUniform("OutSize").set((float) out.width, (float) out.height);
		effect.apply();
		// set here, not in the JSON: EffectInstance skips blend changes it thinks are already applied
		if (blend != null) {
			RenderSystem.enableBlend();
			RenderSystem.blendFunc(blend, GlStateManager.DestFactor.ONE);
		} else {
			RenderSystem.disableBlend();
		}
		BufferBuilder quad = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);
		quad.addVertex(0, 0, 500);
		quad.addVertex(out.width, 0, 500);
		quad.addVertex(out.width, out.height, 500);
		quad.addVertex(0, out.height, 500);
		BufferUploader.draw(quad.buildOrThrow());
		effect.clear();
	}

	private static void fail(String what, Exception e) {
		broken = true;
		Techguns.LOGGER.error("Bloom failed {}, turning it off", what, e);
	}
}
