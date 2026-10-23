package techguns.client.models;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;

import org.joml.Vector3f;

import techguns.client.render.Bloom;

/**
 * Stands in for 1.12 GlStateManager inside ported models: the matrix calls go to the PoseStack of the item being drawn.
 * Set by the item renderer (one at a time, on the render thread) before it calls a model.
 */
public final class LegacyGl {
	/** 1.12 TGRenderHelper.RenderType; only ALPHA does anything here. */
	public enum RenderType {
		ALPHA, ADDITIVE, SOLID, ALPHA_SHADED, NO_Z_TEST
	}

	static PoseStack pose;
	static VertexConsumer consumer;
	static int light;
	static int overlay;
	/** ARGB tint (1.12 GlStateManager.color), white unless set after {@link #begin}. */
	static int color = -1;
	private static int savedLight;
	private static MultiBufferSource buffers;
	private static ResourceLocation texture;
	private static final Function<ResourceLocation, net.minecraft.client.renderer.RenderType> CUTOUT = net.minecraft.client.renderer.RenderType::entityCutout;
	private static final Function<ResourceLocation, net.minecraft.client.renderer.RenderType> TRANSLUCENT = net.minecraft.client.renderer.RenderType::entityTranslucent;
	/** The layer blend mode picks; glowing parts draw into its bloom version. */
	private static Function<ResourceLocation, net.minecraft.client.renderer.RenderType> layer = CUTOUT;
	private static boolean glowing;
	/** Bloom strength of glowing (full bright) model parts. */
	private static final float GLOW = 1.0f;
	private static final Map<net.minecraft.client.renderer.RenderType, net.minecraft.client.renderer.RenderType> GLOWING = new HashMap<>();

	private LegacyGl() {}

	/** Draws into a fixed consumer; blend mode switches need the other overload. */
	public static void begin(PoseStack pose, VertexConsumer consumer, int light, int overlay) {
		LegacyGl.pose = pose;
		LegacyGl.consumer = consumer;
		LegacyGl.light = light;
		LegacyGl.overlay = overlay;
		color = -1;
		buffers = null;
	}

	public static void begin(PoseStack pose, MultiBufferSource buffers, ResourceLocation texture, int light, int overlay) {
		LegacyGl.pose = pose;
		LegacyGl.buffers = buffers;
		LegacyGl.texture = texture;
		LegacyGl.light = light;
		LegacyGl.overlay = overlay;
		layer = CUTOUT;
		glowing = false;
		color = -1;
		updateConsumer();
	}

	public static void color(int argb) {
		color = argb;
	}

	private static void updateConsumer() {
		if (buffers == null) return;
		net.minecraft.client.renderer.RenderType type = layer.apply(texture);
		consumer = buffers.getBuffer(glowing ? GLOWING.computeIfAbsent(type, t -> Bloom.wrap(t, GLOW)) : type);
	}

	public static void pushMatrix() {
		pose.pushPose();
	}

	public static void popMatrix() {
		pose.popPose();
	}

	public static void translate(double x, double y, double z) {
		pose.translate(x, y, z);
	}

	public static void scale(double x, double y, double z) {
		pose.scale((float) x, (float) y, (float) z);
	}

	public static void rotate(float angle, float x, float y, float z) {
		pose.mulPose(Axis.of(new Vector3f(x, y, z).normalize()).rotationDegrees(angle));
	}

	/**
	 * 1.12 TGRenderHelper.enableFXLighting: draw full bright, with bloom (glowing parts).
	 * ponytail: GUI icons draw into the bloom mask too, after it's composited; it's discarded at the next frame.
	 */
	public static void enableFXLighting() {
		savedLight = light;
		light = LightTexture.FULL_BRIGHT;
		glowing = true;
		updateConsumer();
	}

	public static void disableFXLighting() {
		light = savedLight;
		glowing = false;
		updateConsumer();
	}

	/** ALPHA switches to the translucent (and unculled) entity layer until {@link #disableBlendMode}. */
	public static void enableBlendMode(RenderType type) {
		if (type == RenderType.ALPHA) {
			layer = TRANSLUCENT;
			updateConsumer();
		}
	}

	public static void disableBlendMode(RenderType type) {
		if (type == RenderType.ALPHA) {
			layer = CUTOUT;
			updateConsumer();
		}
	}

	/** The translucent layer is already unculled. */
	public static void disableCull() {}

	public static void enableCull() {}
}
