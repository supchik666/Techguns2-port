package techguns.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import techguns.Techguns;
import techguns.entities.projectiles.BioGunProjectile;

/** Bio gun blob: a glowing camera-facing sprite, bigger for more charge (1.12 RenderBioGunProjectile). */
public class BioGunProjectileRenderer extends EntityRenderer<BioGunProjectile> {
	private static final ResourceLocation TEXTURE = Techguns.id("textures/entity/bioblob.png");

	public BioGunProjectileRenderer(EntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	public void render(BioGunProjectile entity, float entityYaw, float partialTicks, PoseStack pose, MultiBufferSource buffers, int light) {
		float size = 0.15f * switch (entity.level) {
			case 1 -> 1.25f;
			case 2 -> 2.5f;
			case 3 -> 4.0f;
			default -> 1.0f;
		};
		pose.pushPose();
		pose.mulPose(entityRenderDispatcher.cameraOrientation());
		pose.scale(size, size, size);
		VertexConsumer consumer = buffers.getBuffer(ScreenEffect.additive(TEXTURE, 1.0f));
		PoseStack.Pose last = pose.last();
		LaserProjectileRenderer.vertex(last, consumer, -0.5f, -0.25f, 0, 0, 1, 1, 1, 1, 1);
		LaserProjectileRenderer.vertex(last, consumer, 0.5f, -0.25f, 0, 1, 1, 1, 1, 1, 1);
		LaserProjectileRenderer.vertex(last, consumer, 0.5f, 0.75f, 0, 1, 0, 1, 1, 1, 1);
		LaserProjectileRenderer.vertex(last, consumer, -0.5f, 0.75f, 0, 0, 0, 1, 1, 1, 1);
		pose.popPose();
		super.render(entity, entityYaw, partialTicks, pose, buffers, light);
	}

	@Override
	public ResourceLocation getTextureLocation(BioGunProjectile entity) {
		return TEXTURE;
	}
}
