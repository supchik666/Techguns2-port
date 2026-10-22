package techguns.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import techguns.Techguns;
import techguns.entities.projectiles.SonicShotgunProjectile;

/** Sonic shotgun: six growing, fading shockwave squares across the center projectile's path; the ring ones aren't drawn. */
public class SonicShotgunProjectileRenderer extends EntityRenderer<SonicShotgunProjectile> {
	private static final ResourceLocation TEXTURE = Techguns.id("textures/fx/shockwave.png");

	public SonicShotgunProjectileRenderer(EntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	public void render(SonicShotgunProjectile entity, float entityYaw, float partialTicks, PoseStack pose, MultiBufferSource buffers, int light) {
		if (!entity.mainProjectile || entity.tickCount < 3) return;
		RandomSource rand = RandomSource.create(entity.getId());
		float age = entity.tickCount + partialTicks;
		float prog = Math.min(1.0f, age / 20.0f);
		float opacity = 1.0f - prog * prog;
		pose.pushPose();
		pose.mulPose(Axis.YP.rotationDegrees(entity.getYRot() - 90.0f));
		pose.mulPose(Axis.ZP.rotationDegrees(entity.getXRot()));
		pose.mulPose(Axis.XP.rotationDegrees(90.0f));
		PoseStack.Pose last = pose.last();
		VertexConsumer consumer = buffers.getBuffer(ScreenEffect.additive(TEXTURE));
		for (int i = 0; i <= 5; i++) {
			float r = opacity * rand.nextFloat(), g = opacity * rand.nextFloat(), b = opacity * (0.5f + rand.nextFloat() * 0.5f);
			float x = 2.5f - i;
			float s = age * 0.3f * (0.5f + rand.nextFloat());
			LaserProjectileRenderer.vertex(last, consumer, x, -s, -s, 0, 0, r, g, b, opacity);
			LaserProjectileRenderer.vertex(last, consumer, x, -s, s, 0, 1, r, g, b, opacity);
			LaserProjectileRenderer.vertex(last, consumer, x, s, s, 1, 1, r, g, b, opacity);
			LaserProjectileRenderer.vertex(last, consumer, x, s, -s, 1, 0, r, g, b, opacity);
		}
		pose.popPose();
	}

	@Override
	public ResourceLocation getTextureLocation(SonicShotgunProjectile entity) {
		return TEXTURE;
	}
}
