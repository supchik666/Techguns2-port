package techguns.client.models;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;

import org.joml.Vector3f;

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
	private static int savedLight;
	private static MultiBufferSource buffers;
	private static ResourceLocation texture;

	private LegacyGl() {}

	/** Draws into a fixed consumer; blend mode switches need the other overload. */
	public static void begin(PoseStack pose, VertexConsumer consumer, int light, int overlay) {
		LegacyGl.pose = pose;
		LegacyGl.consumer = consumer;
		LegacyGl.light = light;
		LegacyGl.overlay = overlay;
		buffers = null;
	}

	public static void begin(PoseStack pose, MultiBufferSource buffers, ResourceLocation texture, int light, int overlay) {
		LegacyGl.pose = pose;
		LegacyGl.buffers = buffers;
		LegacyGl.texture = texture;
		LegacyGl.consumer = buffers.getBuffer(net.minecraft.client.renderer.RenderType.entityCutout(texture));
		LegacyGl.light = light;
		LegacyGl.overlay = overlay;
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

	/** 1.12 TGRenderHelper.enableFXLighting: draw full bright (glowing parts). */
	public static void enableFXLighting() {
		savedLight = light;
		light = LightTexture.FULL_BRIGHT;
	}

	public static void disableFXLighting() {
		light = savedLight;
	}

	/** ALPHA switches to the translucent (and unculled) entity layer until {@link #disableBlendMode}. */
	public static void enableBlendMode(RenderType type) {
		if (type == RenderType.ALPHA && buffers != null) consumer = buffers.getBuffer(net.minecraft.client.renderer.RenderType.entityTranslucent(texture));
	}

	public static void disableBlendMode(RenderType type) {
		if (type == RenderType.ALPHA && buffers != null) consumer = buffers.getBuffer(net.minecraft.client.renderer.RenderType.entityCutout(texture));
	}

	/** The translucent layer is already unculled. */
	public static void disableCull() {}

	public static void enableCull() {}
}
