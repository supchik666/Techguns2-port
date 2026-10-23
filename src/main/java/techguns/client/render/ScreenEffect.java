package techguns.client.render;

import java.util.function.BiFunction;

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

/**
 * Animated sprite sheet drawn as an unlit quad, additive or alpha blended, tinted by ARGB {@code color} (1.12 ScreenEffect).
 * Additive ones glow with {@link Bloom} {@code bloom} strong; alpha ones (scopes) don't glow.
 */
public record ScreenEffect(ResourceLocation texture, int cols, int rows, int numSprites, boolean additive, boolean flipX, boolean flipY, int color,
		float bloom) {
	/** Bloom of muzzle flashes and energy shots. */
	public static final float SHOT_BLOOM = 2.0f;
	public static final ScreenEffect muzzleFlash_gun = additive("muzzleflashnew_2_add_2", 4, 4, 11);
	public static final ScreenEffect muzzleFlash_rifle = additive("muzzleflashnew_add", 4, 4, 12);
	public static final ScreenEffect muzzleFlash_minigun = additive("muzzleflash_minigun", 2, 2, 4);
	public static final ScreenEffect muzzleFlash_blue = new ScreenEffect(fx("bluemuzzleflash"), 4, 4, 8, true, true, true);
	public static final ScreenEffect sniperScope = new ScreenEffect(fx("testscope"), 1, 1, 1, false, false, false);
	public static final ScreenEffect techScope = new ScreenEffect(fx("techscope"), 1, 1, 1, false, false, true);
	/** Tinted deep red. */
	public static final ScreenEffect muzzleFlashLaser = new ScreenEffect(fx("laserflare02"), 4, 4, 7, true, false, false, 0xFFFF1A10);
	public static final ScreenEffect muzzleFlashFireball_alpha = new ScreenEffect(fx("fireball_4x4_alpha"), 4, 4, 16, false, false, false);
	public static final ScreenEffect muzzleFlashAlienBlaster = additive("alienflare", 4, 4, 14);
	public static final ScreenEffect muzzleFlashLightning = additive("teslaflare01", 4, 4, 14);
	public static final ScreenEffect FlamethrowerMuzzleFlame = new ScreenEffect(fx("flamethrower2"), 4, 4, 16, true, false, true);
	public static final ScreenEffect FlamethrowerMuzzleFlash = new ScreenEffect(fx("flamethrower"), 4, 4, 16, true, false, true);
	public static final ScreenEffect muzzleFlashSonic = additive("sonicwave4x4", 4, 4, 16);
	public static final ScreenEffect muzzleFlashNukeBeam = additive("nukebeamflare", 4, 4, 16);
	/** Orange, to match its light (1.12 was green 0xFF28FF8C). */
	public static final ScreenEffect muzzleFlashMibGun = new ScreenEffect(fx("alienflare"), 4, 4, 14, true, false, false, 0xFFFF8010);
	/** ponytail: 1.12 layered a green glow and flash over this with fades; just the main flare until ScreenEffect can stack. */
	public static final ScreenEffect muzzleFlashTFG = new ScreenEffect(fx("tfg_flare"), 4, 4, 16, true, false, false, -1, 1.0f);
	/** 1.12 color (0.5, 1.0, 0.25); its smooth fade is skipped. */
	/** The glint on scope lenses (GunRenderer.setScopeLenses). */
	public static final ScreenEffect scopeLens = new ScreenEffect(fx("glow"), 1, 1, 1, true, false, false, 0xFF6E7378, 0.4f);
	public static final ScreenEffect muzzleGreenFlare = new ScreenEffect(fx("lensflare1"), 1, 1, 1, true, false, false, 0xFF80FF40);

	public ScreenEffect(ResourceLocation texture, int cols, int rows, int numSprites, boolean additive, boolean flipX, boolean flipY) {
		this(texture, cols, rows, numSprites, additive, flipX, flipY, -1);
	}

	public ScreenEffect(ResourceLocation texture, int cols, int rows, int numSprites, boolean additive, boolean flipX, boolean flipY, int color) {
		this(texture, cols, rows, numSprites, additive, flipX, flipY, color, SHOT_BLOOM);
	}

	private static final BiFunction<ResourceLocation, Float, RenderType> ADDITIVE = Util.memoize((texture, bloom) -> Bloom.wrap(
			unlit("techguns_additive", texture, RenderStateShard.LIGHTNING_TRANSPARENCY), bloom));
	private static final BiFunction<ResourceLocation, Float, RenderType> ALPHA = Util.memoize((texture, bloom) -> Bloom.wrap(
			unlit("techguns_alpha", texture, RenderStateShard.TRANSLUCENT_TRANSPARENCY), bloom));

	/** Unlit, additively blended, with {@link Bloom}: for glowing sprites. */
	public static RenderType additive(ResourceLocation texture) {
		return additive(texture, SHOT_BLOOM);
	}

	/** As {@link #additive(ResourceLocation)}, glowing {@code bloom} strong ({@link #SHOT_BLOOM} for shots, explosions more). */
	public static RenderType additive(ResourceLocation texture, float bloom) {
		return ADDITIVE.apply(texture, bloom);
	}

	/** Unlit, alpha blended, no bloom. */
	public static RenderType alpha(ResourceLocation texture) {
		return alpha(texture, 0);
	}

	/** As {@link #alpha(ResourceLocation)}, glowing {@code bloom} times as strongly as a muzzle flash (for fire). */
	public static RenderType alpha(ResourceLocation texture, float bloom) {
		return ALPHA.apply(texture, bloom);
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
		VertexConsumer consumer = buffers.getBuffer(additive ? additive(texture, bloom) : alpha(texture));
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
