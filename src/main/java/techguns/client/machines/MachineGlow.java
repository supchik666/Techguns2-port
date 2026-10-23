package techguns.client.machines;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;

import javax.annotation.Nullable;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;
import techguns.Techguns;
import techguns.client.render.Bloom;
import techguns.machines.MachineBlockEntity;

/**
 * Machines' lit texture details (lamps, indicator lights, screens): their clearly coloured pixels are cut out of the
 * texture into a mask, drawn again full bright with {@link Bloom}, dim while idle and stronger while working.
 * ponytail: masks are built once per texture and outlive resource reloads; an animated sprite's mask spans all its frames
 */
final class MachineGlow {
	static final float IDLE = 0.35f, WORKING = 1;
	/** How far glow quads sit in front of the block model's, so they don't z-fight the chunk mesh. */
	private static final float LIFT = 0.002f;
	private static final Direction[] SIDES = {null, Direction.DOWN, Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST};
	private static final RandomSource RANDOM = RandomSource.create();
	/** Texture to its mask, empty when nothing in it is lit. */
	private static final Map<ResourceLocation, Optional<ResourceLocation>> MASKS = new HashMap<>();
	private static final BiFunction<ResourceLocation, Float, RenderType> TYPES = Util.memoize((mask, bloom) -> Bloom.wrap(RenderType.entityTranslucent(mask), bloom));

	private MachineGlow() {}

	static float strength(MachineBlockEntity be) {
		return be.isWorking() ? WORKING : IDLE;
	}

	/** Steps of 0.05, so a pulsing glow makes a handful of render types. */
	static RenderType type(ResourceLocation mask, float bloom) {
		return TYPES.apply(mask, Math.round(bloom * 20) / 20f);
	}

	/** The mask of a texture file or a block sprite ({@code techguns:block/x}); null when nothing in it is lit. */
	@Nullable
	static ResourceLocation mask(ResourceLocation texture) {
		return MASKS.computeIfAbsent(texture, t -> Optional.ofNullable(makeMask(t.getPath().endsWith(".png") ? t : t.withPath(p -> "textures/" + p + ".png"))))
				.orElse(null);
	}

	@Nullable
	private static ResourceLocation makeMask(ResourceLocation file) {
		NativeImage image;
		try (InputStream in = Minecraft.getInstance().getResourceManager().open(file)) {
			image = NativeImage.read(in);
		} catch (IOException e) {
			return null;
		}
		boolean any = false;
		for (int y = 0; y < image.getHeight(); y++) {
			for (int x = 0; x < image.getWidth(); x++) {
				if (lit(image.getPixelRGBA(x, y))) any = true;
				else image.setPixelRGBA(x, y, 0);
			}
		}
		if (!any) {
			image.close();
			return null;
		}
		ResourceLocation mask = Techguns.id("glow/" + file.getPath());
		Minecraft.getInstance().getTextureManager().register(mask, new DynamicTexture(image));
		return mask;
	}

	/** Bright and saturated (lamps, indicator lights) or a bluish screen; not grey metal, rust or dark paint. */
	private static boolean lit(int abgr) {
		int r = abgr & 255, g = abgr >> 8 & 255, b = abgr >> 16 & 255, max = Math.max(r, Math.max(g, b)), min = Math.min(r, Math.min(g, b));
		return abgr >>> 24 >= 128 && (max >= 128 && (max - min) * 2 >= max || min == r && b - r >= 40 && max >= 115);
	}

	/** The glow of {@code be}'s block model, from its baked quads, so it follows the blockstate's model and rotation. */
	static void blockModel(BlockEntity be, PoseStack pose, MultiBufferSource buffers, float bloom) {
		BlockState state = be.getBlockState();
		var model = Minecraft.getInstance().getBlockRenderer().getBlockModel(state);
		for (Direction side : SIDES) {
			RANDOM.setSeed(42);
			for (BakedQuad quad : model.getQuads(state, side, RANDOM, ModelData.EMPTY, null)) quad(quad, pose, buffers, bloom);
		}
	}

	/** The quad with its UVs moved from the block atlas onto its sprite's mask. */
	private static void quad(BakedQuad quad, PoseStack pose, MultiBufferSource buffers, float bloom) {
		TextureAtlasSprite sprite = quad.getSprite();
		ResourceLocation mask = mask(sprite.contents().name());
		if (mask == null) return;
		VertexConsumer consumer = buffers.getBuffer(type(mask, bloom));
		Vec3i n = quad.getDirection().getNormal();
		int[] data = quad.getVertices();
		for (int i = 0; i < 4; i++) {
			int o = i * 8; // DefaultVertexFormat.BLOCK: position, color, uv, light, normal
			float u = (Float.intBitsToFloat(data[o + 4]) - sprite.getU0()) / (sprite.getU1() - sprite.getU0());
			float v = (Float.intBitsToFloat(data[o + 5]) - sprite.getV0()) / (sprite.getV1() - sprite.getV0());
			consumer.addVertex(pose.last(), Float.intBitsToFloat(data[o]) + n.getX() * LIFT, Float.intBitsToFloat(data[o + 1]) + n.getY() * LIFT,
					Float.intBitsToFloat(data[o + 2]) + n.getZ() * LIFT).setColor(-1).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY)
					.setLight(LightTexture.FULL_BRIGHT).setNormal(pose.last(), n.getX(), n.getY(), n.getZ());
		}
	}
}
