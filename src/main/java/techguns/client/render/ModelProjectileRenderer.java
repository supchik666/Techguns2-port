package techguns.client.render;

import java.util.function.Function;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import techguns.Techguns;
import techguns.client.models.LegacyGl;
import techguns.client.models.ModelMultipart;
import techguns.client.models.ModelObj;
import techguns.client.models.projectiles.ModelRocket;
import techguns.entities.projectiles.GenericProjectile;

/** A 1.12 model turned along the flight path: rockets and missiles (1.12 RenderRocketProjectile), 40mm grenades. */
public class ModelProjectileRenderer<T extends GenericProjectile> extends EntityRenderer<T> {
	private static final ResourceLocation ROCKET = Techguns.id("textures/guns/rocket.png");
	private static final ResourceLocation ROCKET_NUKE = Techguns.id("textures/guns/rocket_nuke.png");
	private static final ResourceLocation ROCKET_HV = Techguns.id("textures/guns/rocket_hv.png");
	private static final ResourceLocation GRENADE_40MM = Techguns.id("textures/entity/launchergrenade.png");

	private final ModelMultipart model;
	private final Function<T, ResourceLocation> texture;
	private final float scale;
	/** 1.12 RenderGrenade40mmProjectile turned its OBJ upside down. */
	private final boolean flip;

	public ModelProjectileRenderer(EntityRendererProvider.Context context, ModelMultipart model, Function<T, ResourceLocation> texture, float scale,
			boolean flip) {
		super(context);
		this.model = model;
		this.texture = texture;
		this.scale = scale;
		this.flip = flip;
	}

	public static <T extends GenericProjectile> ModelProjectileRenderer<T> rocket(EntityRendererProvider.Context context) {
		return new ModelProjectileRenderer<>(context, new ModelRocket(), e -> switch (e.getKind()) {
			case NUKE -> ROCKET_NUKE;
			case HIGH_VELOCITY -> ROCKET_HV;
			default -> ROCKET;
		}, 0.9f, false);
	}

	public static <T extends GenericProjectile> ModelProjectileRenderer<T> grenade40mm(EntityRendererProvider.Context context) {
		return new ModelProjectileRenderer<>(context, new ModelObj(Techguns.id("models/item/grenade40mm.obj"), false), e -> GRENADE_40MM, 0.5f, true);
	}

	@Override
	public void render(T entity, float entityYaw, float partialTicks, PoseStack pose, MultiBufferSource buffers, int light) {
		pose.pushPose();
		pose.mulPose(Axis.YP.rotationDegrees(Mth.lerp(partialTicks, entity.yRotO, entity.getYRot()) - 90.0f));
		pose.mulPose(Axis.ZP.rotationDegrees(Mth.lerp(partialTicks, entity.xRotO, entity.getXRot())));
		if (flip) pose.mulPose(Axis.XP.rotationDegrees(180.0f));
		pose.scale(scale, scale, scale);
		LegacyGl.begin(pose, buffers, getTextureLocation(entity), light, OverlayTexture.NO_OVERLAY);
		model.render(entity, 0, 0, 0, 0, 0, 0.0625f, 0, 0, null, 0, 0, 0);
		pose.popPose();
		super.render(entity, entityYaw, partialTicks, pose, buffers, light);
	}

	@Override
	public ResourceLocation getTextureLocation(T entity) {
		return texture.apply(entity);
	}
}
