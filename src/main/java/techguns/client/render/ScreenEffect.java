package techguns.client.render;

import java.util.function.Function;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;

import net.minecraft.Util;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import techguns.Techguns;

/** Animated sprite sheet drawn as a billboard, additively blended (1.12 ScreenEffect with RenderType.ADDITIVE). */
public record ScreenEffect(ResourceLocation texture, int cols, int rows, int numSprites) {
	public static final ScreenEffect muzzleFlash_gun = new ScreenEffect(Techguns.id("textures/fx/muzzleflashnew_2_add_2.png"), 4, 4, 11);

	private static final Function<ResourceLocation, RenderType> ADDITIVE = Util.memoize(texture -> RenderType.create("techguns_additive",
			DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 1536, false, true, RenderType.CompositeState.builder()
					.setShaderState(RenderStateShard.RENDERTYPE_EYES_SHADER)
					.setTextureState(new RenderStateShard.TextureStateShard(texture, false, false))
					.setTransparencyState(RenderStateShard.LIGHTNING_TRANSPARENCY)
					.setCullState(RenderStateShard.NO_CULL)
					.setWriteMaskState(RenderStateShard.COLOR_WRITE)
					.createCompositeState(false)));

	/** Third person adds two quads crossing the first one, so the flash shows from every side. */
	public void render(PoseStack pose, MultiBufferSource buffers, float progress, float x, float y, float z, float scale, boolean is3p) {
		int frame = (int) (numSprites * progress);
		if (frame >= numSprites) return;
		float u1 = (float) (frame % cols) / cols, u2 = u1 + 1.0f / cols;
		float v1 = (float) (frame / cols) / rows, v2 = v1 + 1.0f / rows;
		float f = scale / 2.0f;
		VertexConsumer consumer = buffers.getBuffer(ADDITIVE.apply(texture));
		PoseStack.Pose last = pose.last();
		vertex(last, consumer, x - f, y + f, z, u1, v2);
		vertex(last, consumer, x - f, y - f, z, u1, v1);
		vertex(last, consumer, x + f, y - f, z, u2, v1);
		vertex(last, consumer, x + f, y + f, z, u2, v2);
		if (is3p) {
			vertex(last, consumer, x - f, y, z + f, u1, v2);
			vertex(last, consumer, x - f, y, z - f, u1, v1);
			vertex(last, consumer, x + f, y, z - f, u2, v1);
			vertex(last, consumer, x + f, y, z + f, u2, v2);
			vertex(last, consumer, x, y - f, z + f, u1, v2);
			vertex(last, consumer, x, y - f, z - f, u1, v1);
			vertex(last, consumer, x, y + f, z - f, u2, v1);
			vertex(last, consumer, x, y + f, z + f, u2, v2);
		}
	}

	private static void vertex(PoseStack.Pose pose, VertexConsumer consumer, float x, float y, float z, float u, float v) {
		consumer.addVertex(pose, x, y, z).setColor(-1).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY)
				.setLight(LightTexture.FULL_BRIGHT).setNormal(pose, 0, 1, 0);
	}
}
