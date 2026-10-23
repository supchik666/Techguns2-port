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
	private static final ResourceLocation ADVANCED = Techguns.id("textures/entity/bullet_blue.png");
	/** Tracers glow half as much as a muzzle flash. */
	private static final RenderType TRACER = Bloom.wrap(RenderType.entityTranslucentEmissive(TEXTURE), ScreenEffect.SHOT_BLOOM / 2);
	private static final ResourceLocation STONE = Techguns.id("textures/entity/handgunbullet.png");

	public GenericProjectileRenderer(EntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	public void render(GenericProjectile entity, float entityYaw, float partialTicks, PoseStack pose, MultiBufferSource buffers, int light) {
		pose.pushPose();
		if (entity.getKind() == GenericProjectile.Kind.STONE) {
			// 1.12 RenderTextureProjectile: a camera-facing sprite, 0.1 blocks
			pose.mulPose(entityRenderDispatcher.cameraOrientation());
			pose.scale(0.1f, 0.1f, 0.1f);
			VertexConsumer consumer = buffers.getBuffer(RenderType.entityCutoutNoCull(STONE));
			PoseStack.Pose last = pose.last();
			vertex(last, consumer, -0.5f, -0.25f, 0, 1, light);
			vertex(last, consumer, 0.5f, -0.25f, 1, 1, light);
			vertex(last, consumer, 0.5f, 0.75f, 1, 0, light);
			vertex(last, consumer, -0.5f, 0.75f, 0, 0, light);
			pose.popPose();
			return;
		}
		boolean advanced = entity.getKind() == GenericProjectile.Kind.ADVANCED;
		float length = advanced ? 16 : 8;
		pose.mulPose(Axis.YP.rotationDegrees(Mth.lerp(partialTicks, entity.yRotO, entity.getYRot()) - 90.0f));
		pose.mulPose(Axis.ZP.rotationDegrees(Mth.lerp(partialTicks, entity.xRotO, entity.getXRot())));
		pose.mulPose(Axis.XP.rotationDegrees(45.0f));
		pose.scale(0.0125f, 0.0125f, 0.0125f);
		VertexConsumer consumer = buffers.getBuffer(advanced ? ScreenEffect.additive(ADVANCED) : TRACER);
		for (int i = 0; i < 4; i++) {
			pose.mulPose(Axis.XP.rotationDegrees(90.0f));
			PoseStack.Pose last = pose.last();
			vertex(last, consumer, -length, -2, 0, 0, LightTexture.FULL_BRIGHT);
			vertex(last, consumer, length, -2, 1, 0, LightTexture.FULL_BRIGHT);
			vertex(last, consumer, length, 2, 1, 1, LightTexture.FULL_BRIGHT);
			vertex(last, consumer, -length, 2, 0, 1, LightTexture.FULL_BRIGHT);
		}
		pose.popPose();
	}

	private static void vertex(PoseStack.Pose pose, VertexConsumer consumer, float x, float y, float u, float v, int light) {
		consumer.addVertex(pose, x, y, 0).setColor(-1).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY)
				.setLight(light).setNormal(pose, 0, 0, 1);
	}

	@Override
	public ResourceLocation getTextureLocation(GenericProjectile entity) {
		return entity.getKind() == GenericProjectile.Kind.STONE ? STONE : entity.getKind() == GenericProjectile.Kind.ADVANCED ? ADVANCED : TEXTURE;
	}
}
