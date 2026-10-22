package techguns.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import techguns.Techguns;
import techguns.entities.projectiles.GenericProjectile;

/** Bullet tracer: four quads crossed around the flight axis, like an arrow shaft. */
public class GenericProjectileRenderer extends EntityRenderer<GenericProjectile> {
	private static final ResourceLocation TEXTURE = Techguns.id("textures/entity/bullet1.png");

	public GenericProjectileRenderer(EntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	public void render(GenericProjectile entity, float entityYaw, float partialTicks, PoseStack pose, MultiBufferSource buffers, int light) {
		if (entity.tickCount < 2 && partialTicks <= 0.25f) return; // don't draw it inside the gun
		pose.pushPose();
		pose.mulPose(Axis.YP.rotationDegrees(Mth.lerp(partialTicks, entity.yRotO, entity.getYRot()) - 90.0f));
		pose.mulPose(Axis.ZP.rotationDegrees(Mth.lerp(partialTicks, entity.xRotO, entity.getXRot())));
		pose.mulPose(Axis.XP.rotationDegrees(45.0f));
		pose.scale(0.0125f, 0.0125f, 0.0125f);
		VertexConsumer consumer = buffers.getBuffer(RenderType.entityTranslucentEmissive(TEXTURE));
		for (int i = 0; i < 4; i++) {
			pose.mulPose(Axis.XP.rotationDegrees(90.0f));
			PoseStack.Pose last = pose.last();
			vertex(last, consumer, -8, -2, 0, 0);
			vertex(last, consumer, 8, -2, 1, 0);
			vertex(last, consumer, 8, 2, 1, 1);
			vertex(last, consumer, -8, 2, 0, 1);
		}
		pose.popPose();
	}

	private static void vertex(PoseStack.Pose pose, VertexConsumer consumer, float x, float y, float u, float v) {
		consumer.addVertex(pose, x, y, 0).setColor(-1).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY)
				.setLight(LightTexture.FULL_BRIGHT).setNormal(pose, 0, 0, 1);
	}

	@Override
	public ResourceLocation getTextureLocation(GenericProjectile entity) {
		return TEXTURE;
	}
}
