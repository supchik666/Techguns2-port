package techguns.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import techguns.Techguns;
import techguns.entities.projectiles.LaserProjectile;

/** Laser beam: two crossed quads that swell and fade over its life and spin around the beam axis; a start piece at the muzzle. */
public class LaserProjectileRenderer extends EntityRenderer<LaserProjectile> {
	private static final ResourceLocation TEXTURE = Techguns.id("textures/fx/laser3.png");
	private static final ResourceLocation TEXTURE_START = Techguns.id("textures/fx/laser3_start.png");
	private static final float WIDTH = 3.0f;
	/** Beams are modelled in 1/80 block units (1.12 used a 0.0125 scale). */
	static final float SCALE = 0.0125f;

	public LaserProjectileRenderer(EntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	public void render(LaserProjectile entity, float entityYaw, float partialTicks, PoseStack pose, MultiBufferSource buffers, int light) {
		float prog = (entity.tickCount + partialTicks) / entity.maxTicks;
		float brightness = Mth.sin(Mth.sqrt(prog) * Mth.PI);
		float width = WIDTH * brightness * 2;
		float distance = (float) entity.distance / SCALE;
		float start = Math.min(1.0f, (float) entity.distance) / SCALE;
		float u = (float) entity.distance / WIDTH * 2;

		pose.pushPose();
		pose.mulPose(Axis.YP.rotationDegrees(entity.getYRot() - 90.0f));
		pose.mulPose(Axis.ZP.rotationDegrees(entity.getXRot()));
		pose.mulPose(Axis.XP.rotationDegrees(45.0f + prog * 180.0f + 90.0f));
		pose.scale(SCALE, SCALE, SCALE);
		if (distance > start) {
			VertexConsumer beam = buffers.getBuffer(ScreenEffect.additive(TEXTURE));
			for (int i = 1; i <= 2; i++) {
				pose.pushPose();
				pose.mulPose(Axis.XP.rotationDegrees(90.0f * i));
				PoseStack.Pose last = pose.last();
				vertex(last, beam, distance, -width, 0, u + prog, 0, 1, 1, 1, brightness);
				vertex(last, beam, start, -width, 0, prog, 0, 1, 1, 1, brightness);
				vertex(last, beam, start, width, 0, prog, 1, 1, 1, 1, brightness);
				vertex(last, beam, distance, width, 0, u + prog, 1, 1, 1, 1, brightness);
				pose.popPose();
			}
		}
		VertexConsumer muzzle = buffers.getBuffer(ScreenEffect.additive(TEXTURE_START));
		for (int i = 1; i <= 2; i++) {
			pose.pushPose();
			pose.mulPose(Axis.XP.rotationDegrees(90.0f * i));
			PoseStack.Pose last = pose.last();
			vertex(last, muzzle, start, -width, 0, 1, 0, 1, 1, 1, brightness);
			vertex(last, muzzle, 0, -width, 0, 0, 0, 1, 1, 1, brightness);
			vertex(last, muzzle, 0, width, 0, 0, 1, 1, 1, 1, brightness);
			vertex(last, muzzle, start, width, 0, 1, 1, 1, 1, 1, brightness);
			pose.popPose();
		}
		pose.popPose();
	}

	/** A full-bright vertex for the additive render type; alpha is clamped to 0-1. */
	static void vertex(PoseStack.Pose pose, VertexConsumer consumer, float x, float y, float z, float u, float v, float r, float g, float b, float a) {
		consumer.addVertex(pose, x, y, z).setColor(r, g, b, Mth.clamp(a, 0.0f, 1.0f)).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY)
				.setLight(LightTexture.FULL_BRIGHT).setNormal(pose, 0, 0, 1);
	}

	@Override
	public ResourceLocation getTextureLocation(LaserProjectile entity) {
		return TEXTURE;
	}
}
