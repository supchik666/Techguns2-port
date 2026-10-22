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

/** Animated sprite sheet drawn as an unlit quad, additive or alpha blended, tinted by ARGB {@code color} (1.12 ScreenEffect). */
public record ScreenEffect(ResourceLocation texture, int cols, int rows, int numSprites, boolean additive, boolean flipX, boolean flipY, int color) {
	public static final ScreenEffect muzzleFlash_gun = additive("muzzleflashnew_2_add_2", 4, 4, 11);
	public static final ScreenEffect muzzleFlash_rifle = additive("muzzleflashnew_add", 4, 4, 12);
	public static final ScreenEffect muzzleFlash_minigun = additive("muzzleflash_minigun", 2, 2, 4);
	public static final ScreenEffect muzzleFlash_blue = new ScreenEffect(fx("bluemuzzleflash"), 4, 4, 8, true, true, true);
	public static final ScreenEffect sniperScope = new ScreenEffect(fx("testscope"), 1, 1, 1, false, false, false);
	public static final ScreenEffect techScope = new ScreenEffect(fx("techscope"), 1, 1, 1, false, false, true);
	public static final ScreenEffect muzzleFlashLaser = additive("laserflare02", 4, 4, 7);
	public static final ScreenEffect muzzleFlashFireball_alpha = new ScreenEffect(fx("fireball_4x4_alpha"), 4, 4, 16, false, false, false);
	public static final ScreenEffect muzzleFlashAlienBlaster = additive("alienflare", 4, 4, 14);
	public static final ScreenEffect muzzleFlashLightning = additive("teslaflare01", 4, 4, 14);
	public static final ScreenEffect FlamethrowerMuzzleFlame = new ScreenEffect(fx("flamethrower2"), 4, 4, 16, true, false, true);
	public static final ScreenEffect FlamethrowerMuzzleFlash = new ScreenEffect(fx("flamethrower"), 4, 4, 16, true, false, true);
	public static final ScreenEffect muzzleFlashSonic = additive("sonicwave4x4", 4, 4, 16);
	public static final ScreenEffect muzzleFlashNukeBeam = additive("nukebeamflare", 4, 4, 16);
	/** 1.12 color (0.15686, 1.0, 0.549). */
	public static final ScreenEffect muzzleFlashMibGun = new ScreenEffect(fx("alienflare"), 4, 4, 14, true, false, false, 0xFF28FF8C);

	public ScreenEffect(ResourceLocation texture, int cols, int rows, int numSprites, boolean additive, boolean flipX, boolean flipY) {
		this(texture, cols, rows, numSprites, additive, flipX, flipY, -1);
	}

	private static final Function<ResourceLocation, RenderType> ADDITIVE = Util.memoize(texture -> unlit("techguns_additive", texture,
			RenderStateShard.LIGHTNING_TRANSPARENCY));
	private static final Function<ResourceLocation, RenderType> ALPHA = Util.memoize(texture -> unlit("techguns_alpha", texture,
			RenderStateShard.TRANSLUCENT_TRANSPARENCY));

	/** Unlit, additively blended: for glowing sprites. */
	public static RenderType additive(ResourceLocation texture) {
		return ADDITIVE.apply(texture);
	}

	/** Unlit, alpha blended. */
	public static RenderType alpha(ResourceLocation texture) {
		return ALPHA.apply(texture);
	}

	private static ResourceLocation fx(String name) {
		return Techguns.id("textures/fx/" + name + ".png");
	}

	private static ScreenEffect additive(String name, int cols, int rows, int numSprites) {
		return new ScreenEffect(fx(name), cols, rows, numSprites, true, false, false);
	}

	private static RenderType unlit(String name, ResourceLocation texture, RenderStateShard.TransparencyStateShard transparency) {
		return RenderType.create(name, DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 1536, false, true, RenderType.CompositeState.builder()
				.setShaderState(RenderStateShard.RENDERTYPE_EYES_SHADER)
				.setTextureState(new RenderStateShard.TextureStateShard(texture, false, false))
				.setTransparencyState(transparency)
				.setCullState(RenderStateShard.NO_CULL)
				.setWriteMaskState(RenderStateShard.COLOR_WRITE)
				.createCompositeState(false));
	}

	/** Third person adds two quads crossing the first one, so the flash shows from every side. */
	public void render(PoseStack pose, MultiBufferSource buffers, float progress, float x, float y, float z, float scale, boolean is3p) {
		int frame = (int) (numSprites * progress);
		if (frame >= numSprites) return;
		float u1 = (float) (frame % cols) / cols, u2 = u1 + 1.0f / cols;
		float v1 = (float) (frame / cols) / rows, v2 = v1 + 1.0f / rows;
		if (flipX) {
			float t = u1;
			u1 = u2;
			u2 = t;
		}
		if (flipY) {
			float t = v1;
			v1 = v2;
			v2 = t;
		}
		float f = scale / 2.0f;
		VertexConsumer consumer = buffers.getBuffer((additive ? ADDITIVE : ALPHA).apply(texture));
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

	private void vertex(PoseStack.Pose pose, VertexConsumer consumer, float x, float y, float z, float u, float v) {
		consumer.addVertex(pose, x, y, z).setColor(color).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY)
				.setLight(LightTexture.FULL_BRIGHT).setNormal(pose, 0, 1, 0);
	}
}
