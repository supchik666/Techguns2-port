package techguns.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import techguns.Techguns;
import techguns.entities.projectiles.NDRProjectile;

/**
 * Death ray: an animated beam (17 frame strip) drawn from the shooter's eyes along their current aim, wrapped in a
 * spiral near the muzzle. ponytail: no 1.12 view bobbing on the first-person beam.
 */
public class NDRProjectileRenderer extends EntityRenderer<NDRProjectile> {
	private static final ResourceLocation TEXTURE = Techguns.id("textures/fx/nukebeam.png");
	private static final double WIDTH = 3.0;
	private static final int FRAMES = 17;

	public NDRProjectileRenderer(EntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	public void render(NDRProjectile entity, float entityYaw, float partialTicks, PoseStack pose, MultiBufferSource buffers, int light) {
		if (!(entity.getOwner() instanceof LivingEntity shooter)) return;
		float age = entity.tickCount + partialTicks;
		float prog = age / entity.maxTicks;
		float brightness = Mth.sin(Mth.sqrt(prog) * Mth.PI);
		float width = (float) (WIDTH * brightness * 2);
		float pitch = shooter.getViewXRot(partialTicks), yaw = shooter.getViewYRot(partialTicks);

		Vec3 origin = shooter.getEyePosition(partialTicks).subtract(entity.getPosition(partialTicks));
		Minecraft mc = Minecraft.getInstance();
		if (shooter != mc.getCameraEntity() || !mc.options.getCameraType().isFirstPerson()) {
			origin = origin.add(-Mth.cos(yaw * Mth.DEG_TO_RAD) * 0.16, 0.1, -Mth.sin(yaw * Mth.DEG_TO_RAD) * 0.16);
		}
		pose.pushPose();
		pose.translate(origin.x, origin.y, origin.z);
		pose.mulPose(Axis.YP.rotationDegrees(90.0f - yaw));
		pose.mulPose(Axis.ZP.rotationDegrees(pitch));
		pose.translate(-0.25, -0.08, -0.12); // -x is forward
		float distance = (float) (entity.distance + 0.25) / LaserProjectileRenderer.SCALE;
		pose.mulPose(Axis.XP.rotationDegrees(45.0f + prog * 360.0f + 90.0f));
		pose.scale(LaserProjectileRenderer.SCALE, LaserProjectileRenderer.SCALE, LaserProjectileRenderer.SCALE);

		int frame = (int) (age * 0.5f % FRAMES);
		float v1 = (float) frame / FRAMES, v2 = (float) (frame + 1) / FRAMES;
		float u = (float) (distance / (WIDTH * 8.0 * 2.0));
		VertexConsumer consumer = buffers.getBuffer(ScreenEffect.additive(TEXTURE));
		for (int i = 0; i < 2; i++) {
			pose.mulPose(Axis.XP.rotationDegrees(90.0f));
			PoseStack.Pose last = pose.last();
			LaserProjectileRenderer.vertex(last, consumer, -distance, -width, 0, i, v1, 1, 1, 1, brightness);
			LaserProjectileRenderer.vertex(last, consumer, 0, -width, 0, u + i, v1, 1, 1, 1, brightness);
			LaserProjectileRenderer.vertex(last, consumer, 0, width, 0, u + i, v2, 1, 1, 1, brightness);
			LaserProjectileRenderer.vertex(last, consumer, -distance, width, 0, i, v2, 1, 1, 1, brightness);
		}

		// spiral around the first 5 blocks
		distance = Math.min(distance, 400.0f);
		int segments = (int) (distance / 8.0f);
		double w = 4.0, angle = Math.PI / 8.0;
		double cos2 = Math.cos((Math.PI - angle) * 0.5) * 2.0;
		double angleOffset = RandomSource.create(entity.getId()).nextDouble() * Math.PI * 2.0;
		double d = distance / segments;
		double prevX = 0, prevY = 0, prevZ = 0, prevU = 0;
		PoseStack.Pose last = pose.last();
		for (int i = 0; i < segments; i++) {
			double x = -d * i;
			double radius = 0.5 * (1 - Math.cos(2.0 * Math.sqrt((double) i / segments) * Math.PI)) * 8.0;
			double y = radius * Math.cos(angleOffset - angle * i), z = radius * Math.sin(angleOffset - angle * i);
			double su = prevU + cos2 * radius / 8.0;
			LaserProjectileRenderer.vertex(last, consumer, (float) (prevX - w), (float) prevY, (float) prevZ, (float) prevU, v2, 1, 1, 1, brightness);
			LaserProjectileRenderer.vertex(last, consumer, (float) (x - w), (float) y, (float) z, (float) su, v2, 1, 1, 1, brightness);
			LaserProjectileRenderer.vertex(last, consumer, (float) (x + w), (float) y, (float) z, (float) su, v1, 1, 1, 1, brightness);
			LaserProjectileRenderer.vertex(last, consumer, (float) (prevX + w), (float) prevY, (float) prevZ, (float) prevU, v1, 1, 1, 1, brightness);
			prevX = x;
			prevY = y;
			prevZ = z;
			prevU = su;
		}
		pose.popPose();
	}

	@Override
	public ResourceLocation getTextureLocation(NDRProjectile entity) {
		return TEXTURE;
	}
}
