package techguns.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import techguns.Techguns;
import techguns.entities.projectiles.BlasterProjectile;

/** Blaster bolt: the bullet tracer shape with the laser texture, additive. */
public class BlasterProjectileRenderer extends EntityRenderer<BlasterProjectile> {
	private static final ResourceLocation TEXTURE = Techguns.id("textures/fx/laser3.png");

	public BlasterProjectileRenderer(EntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	public void render(BlasterProjectile entity, float entityYaw, float partialTicks, PoseStack pose, MultiBufferSource buffers, int light) {
		if (entity.tickCount < 2 && partialTicks <= 0.25f) return; // don't draw it inside the gun
		pose.pushPose();
		pose.mulPose(Axis.YP.rotationDegrees(Mth.lerp(partialTicks, entity.yRotO, entity.getYRot()) - 90.0f));
		pose.mulPose(Axis.ZP.rotationDegrees(Mth.lerp(partialTicks, entity.xRotO, entity.getXRot())));
		pose.mulPose(Axis.XP.rotationDegrees(45.0f));
		pose.scale(LaserProjectileRenderer.SCALE, LaserProjectileRenderer.SCALE, LaserProjectileRenderer.SCALE);
		VertexConsumer consumer = buffers.getBuffer(ScreenEffect.additive(TEXTURE));
		for (int i = 0; i < 4; i++) {
			pose.mulPose(Axis.XP.rotationDegrees(90.0f));
			PoseStack.Pose last = pose.last();
			LaserProjectileRenderer.vertex(last, consumer, -8, -2, 0, 0, 0, 1, 1, 1, 1);
			LaserProjectileRenderer.vertex(last, consumer, 8, -2, 0, 1, 0, 1, 1, 1, 1);
			LaserProjectileRenderer.vertex(last, consumer, 8, 2, 0, 1, 1, 1, 1, 1, 1);
			LaserProjectileRenderer.vertex(last, consumer, -8, 2, 0, 0, 1, 1, 1, 1, 1);
		}
		pose.popPose();
	}

	@Override
	public ResourceLocation getTextureLocation(BlasterProjectile entity) {
		return TEXTURE;
	}
}
