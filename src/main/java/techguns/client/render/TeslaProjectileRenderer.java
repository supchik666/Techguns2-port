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
import techguns.entities.projectiles.TeslaProjectile;

/**
 * Lightning bolt: a polyline bent by a few overlapping sine waves (fixed per bolt), drawn as two crossed ribbons. A
 * bright pulse runs along it over its life.
 */
public class TeslaProjectileRenderer extends EntityRenderer<TeslaProjectile> {
	private static final ResourceLocation TEXTURE = Techguns.id("textures/fx/laser_blue.png");
	private static final int SIN_COUNT = 5;
	private static final double WIDTH = 0.5;
	/** Ideal length of one sine curve. */
	private static final double SIN_DISTANCE = 10.0;
	/** Distance between bolt vertices. */
	private static final double STEP = 0.2;

	public TeslaProjectileRenderer(EntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	public void render(TeslaProjectile entity, float entityYaw, float partialTicks, PoseStack pose, MultiBufferSource buffers, int light) {
		double distance = entity.distance;
		double progress = (entity.tickCount + partialTicks) / entity.maxTicks;
		RandomSource rand = RandomSource.create(entity.getId()); // same shape every frame
		double[] dY = new double[SIN_COUNT], dZ = new double[SIN_COUNT];
		for (int i = 0; i < SIN_COUNT; i++) {
			dY[i] = 0.5 - rand.nextDouble();
			dZ[i] = 0.5 - rand.nextDouble();
		}
		int count = Math.max(1, (int) Math.round(distance / STEP));
		double step = distance / count;
		int xreps = Math.max(1, (int) Math.round(distance / SIN_DISTANCE));

		pose.pushPose();
		pose.mulPose(Axis.YP.rotationDegrees(entity.getYRot() - 90.0f));
		pose.mulPose(Axis.ZP.rotationDegrees(entity.getXRot()));
		pose.mulPose(Axis.XP.rotationDegrees(45.0f));
		PoseStack.Pose last = pose.last();
		VertexConsumer consumer = buffers.getBuffer(ScreenEffect.additive(TEXTURE));
		double xprev = 0, yprev = 0, zprev = 0, widthprev = 1.0, alphaprev = 1.0;
		for (int i = 0; i <= count; i++) {
			double d = (double) i / count;
			double x = i * step, y = 0, z = 0;
			if (i > 1) {
				for (int j = 1; j <= SIN_COUNT; j++) {
					double wave = Math.sin(d * Math.PI * j * xreps);
					y += wave * ((rand.nextDouble() - 0.5) + progress * dY[j - 1]) * (2.0 / j);
					z += wave * ((rand.nextDouble() - 0.5) + progress * dZ[j - 1]) * (2.0 / j);
				}
			}
			double pulse = 1.0 - Math.sqrt(Math.abs(progress - d) * 2.0);
			double width = Math.max(0.0, WIDTH * pulse);
			if (i >= 1) segment(last, consumer, xprev, yprev, zprev, x, y, z, widthprev, width, (float) alphaprev);
			widthprev = width;
			alphaprev = pulse;
			xprev = x;
			yprev = y;
			zprev = z;
		}
		pose.popPose();
	}

	/** Two crossed ribbons from 1 to 2; the bend and width are halved. */
	private static void segment(PoseStack.Pose pose, VertexConsumer consumer, double x1, double y1, double z1, double x2, double y2, double z2, double w1,
			double w2, float alpha) {
		float ax = (float) x1, ay = (float) (y1 * 0.5), az = (float) (z1 * 0.5), aw = (float) (w1 * 0.5);
		float bx = (float) x2, by = (float) (y2 * 0.5), bz = (float) (z2 * 0.5), bw = (float) (w2 * 0.5);
		LaserProjectileRenderer.vertex(pose, consumer, ax, ay - aw, az, 0, 0, 1, 1, 1, alpha);
		LaserProjectileRenderer.vertex(pose, consumer, ax, ay + aw, az, 0, 1, 1, 1, 1, alpha);
		LaserProjectileRenderer.vertex(pose, consumer, bx, by + bw, bz, 1, 1, 1, 1, 1, alpha);
		LaserProjectileRenderer.vertex(pose, consumer, bx, by - bw, bz, 1, 0, 1, 1, 1, alpha);
		LaserProjectileRenderer.vertex(pose, consumer, ax, ay, az - aw, 0, 0, 1, 1, 1, alpha);
		LaserProjectileRenderer.vertex(pose, consumer, ax, ay, az + aw, 0, 1, 1, 1, 1, alpha);
		LaserProjectileRenderer.vertex(pose, consumer, bx, by, bz + bw, 1, 1, 1, 1, 1, alpha);
		LaserProjectileRenderer.vertex(pose, consumer, bx, by, bz - bw, 1, 0, 1, 1, 1, alpha);
	}

	@Override
	public ResourceLocation getTextureLocation(TeslaProjectile entity) {
		return TEXTURE;
	}
}
