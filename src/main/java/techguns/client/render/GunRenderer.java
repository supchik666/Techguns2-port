package techguns.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import techguns.client.ShooterValues;
import techguns.client.TGClient;
import techguns.client.models.ModelMultipart;
import techguns.items.guns.GenericGun;

/**
 * Draws a gun from its {@link ModelMultipart}, with recoil, reload and muzzle flash. A port of 1.12 RenderItemBase +
 * RenderGunBase; the setters take the same numbers as the 1.12 ClientProxy registrations.
 * ponytail: scopes, charge animation, item particles and NPC gun scaling come with the guns that use them.
 */
public class GunRenderer extends BlockEntityWithoutLevelRenderer {
	private final ModelMultipart model;
	private final int parts;
	private final ResourceLocation texture;

	private float baseScale = 1.0f;
	private float scaleThirdp = 0.35f;
	private float scaleGround = 0.35f;
	private float scaleEgo = 0.5f;
	private float scaleGui = 0.4f;
	private float scaleItemframe = 0.5f;
	private float[] translateBase = {0f, 0f, 0f};
	/** First person, third person, GUI, ground, item frame. Mirrored for the left hand. */
	private float[][] translateType = {{0f, 0f, 0f}, {0f, 0f, 0f}, {0f, 0f, 0f}, {0f, 0f, 0f}, {0f, 0f, -0.05f}};

	private GunAnimation recoilAnim = GunAnimation.genericRecoil;
	private float[] recoilParams = {0.15f, 5.0f};
	private GunAnimation reloadAnim = GunAnimation.genericReload;
	private float[] reloadParams = {1.0f, 40.0f};
	private GunAnimation recoilAnim3p = GunAnimation.genericRecoil;
	private float[] recoilParams3p = {0f, 5.0f};
	private GunAnimation reloadAnim3p = GunAnimation.genericReload;
	private float[] reloadParams3p = {0f, 40.0f};

	private ScreenEffect muzzleFX;
	private float muzzleFX_x_r, muzzleFX_x_l, muzzleFX_y, muzzleFX_z, muzzleFX_scale = 1.0f;
	private float muzzleFX_3p_y, muzzleFX_3p_z, muzzleFX_3p_scale = 0.5f;
	private float jitterX, jitterY, jitterScale;

	public GunRenderer(ModelMultipart model, int parts, ResourceLocation texture) {
		super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
		this.model = model;
		this.parts = parts;
		this.texture = texture;
	}

	public GunRenderer setBaseTranslation(float x, float y, float z) {
		translateBase = new float[] {x, y, z};
		return this;
	}

	public GunRenderer setBaseScale(float baseScale) {
		this.baseScale = baseScale;
		return this;
	}

	public GunRenderer setGUIScale(float scale) {
		scaleGui = scale;
		return this;
	}

	public GunRenderer setTransformTranslations(float[][] translations) {
		translateType = translations;
		return this;
	}

	public GunRenderer setRecoilAnim(GunAnimation anim, float... params) {
		recoilAnim = anim;
		recoilParams = params;
		return this;
	}

	public GunRenderer setMuzzleFx(ScreenEffect fx, float x, float y, float z, float scale, float x_l) {
		muzzleFX = fx;
		muzzleFX_x_r = x;
		muzzleFX_x_l = x_l;
		muzzleFX_y = y;
		muzzleFX_z = z;
		muzzleFX_scale = scale;
		return this;
	}

	public GunRenderer setMuzzleFXPos3P(float y, float z) {
		muzzleFX_3p_y = y;
		muzzleFX_3p_z = z;
		return this;
	}

	/** Angle jitter isn't used by the 1.12 renderer either; kept so registrations read the same. */
	public GunRenderer setMuzzleFlashJitter(float x, float y, float angle, float scale) {
		jitterX = x;
		jitterY = y;
		jitterScale = scale;
		return this;
	}

	@Override
	public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
		boolean firstPerson = context.firstPerson();
		boolean thirdPerson = context == ItemDisplayContext.THIRD_PERSON_LEFT_HAND || context == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND;
		boolean leftHand = context == ItemDisplayContext.FIRST_PERSON_LEFT_HAND || context == ItemDisplayContext.THIRD_PERSON_LEFT_HAND;
		LivingEntity entity = firstPerson ? Minecraft.getInstance().player : thirdPerson ? TGClient.renderedEntity : null;

		float fireProgress = 0, reloadProgress = 0, muzzleProgress = 0;
		ShooterValues.AttackTime time = null;
		if (entity != null) {
			boolean offhand = leftHand == (entity.getMainArm() == HumanoidArm.RIGHT);
			time = ShooterValues.get(entity, offhand ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
			reloadProgress = ShooterValues.progress(time.reloadEnd, time.reloadTotal);
			if (reloadProgress == 0) fireProgress = ShooterValues.progress(time.recoilEnd, time.recoilTotal);
			muzzleProgress = ShooterValues.progress(time.muzzleFlashEnd, time.muzzleFlashTotal);
		}

		pose.pushPose();
		pose.translate(0.5f, 0.5f, 0.5f); // ItemRenderer shifts custom renderers by -0.5; 1.12 drew from the origin
		pose.pushPose();
		applyTranslation(pose, context, leftHand);
		if (firstPerson) {
			if (fireProgress > 0) {
				recoilAnim.play(pose, fireProgress, leftHand, recoilParams);
			} else if (reloadProgress > 0) {
				reloadAnim.play(pose, reloadProgress, leftHand, reloadParams);
			}
		} else if (thirdPerson) {
			if (fireProgress > 0) {
				recoilAnim3p.play(pose, fireProgress, leftHand, recoilParams3p);
			} else if (reloadProgress > 0) {
				reloadAnim3p.play(pose, reloadProgress, leftHand, reloadParams3p);
			}
		} else if (context == ItemDisplayContext.GUI) {
			pose.mulPose(Axis.YP.rotationDegrees(40.0f));
			pose.mulPose(Axis.XP.rotationDegrees(20.0f));
		} else if (context == ItemDisplayContext.FIXED) {
			pose.mulPose(Axis.YP.rotationDegrees(-90.0f));
		}
		float scale = baseScale * scaleFor(context);
		pose.scale(scale, scale, scale);
		pose.mulPose(Axis.XP.rotationDegrees(-180.0f));
		pose.mulPose(Axis.YP.rotationDegrees(180.0f));
		pose.translate(translateBase[0], translateBase[1], translateBase[2]);
		VertexConsumer consumer = buffers.getBuffer(RenderType.entityCutout(texture));
		int ammo = ((GenericGun) stack.getItem()).getAmmo(stack);
		for (int i = 0; i < parts; i++) {
			model.render(pose, consumer, light, overlay, i, fireProgress, reloadProgress, ammo);
		}
		pose.popPose();

		if (muzzleProgress > 0 && muzzleFX != null) {
			if (firstPerson) {
				float x = (leftHand ? muzzleFX_x_l : muzzleFX_x_r) + jitterX * time.jitterX;
				muzzleFX.render(pose, buffers, muzzleProgress, x, muzzleFX_y + jitterY * time.jitterY, muzzleFX_z,
						muzzleFX_scale + jitterScale * time.jitterScale, false);
			} else {
				muzzleFX.render(pose, buffers, muzzleProgress, 0, muzzleFX_3p_y, muzzleFX_3p_z, muzzleFX_scale * muzzleFX_3p_scale, true);
			}
		}
		pose.popPose();
	}

	private float scaleFor(ItemDisplayContext context) {
		return switch (context) {
			case FIRST_PERSON_LEFT_HAND, FIRST_PERSON_RIGHT_HAND -> scaleEgo;
			case THIRD_PERSON_LEFT_HAND, THIRD_PERSON_RIGHT_HAND -> scaleThirdp;
			case GUI -> scaleGui;
			case GROUND -> scaleGround;
			case FIXED -> scaleItemframe;
			default -> 1.0f;
		};
	}

	private void applyTranslation(PoseStack pose, ItemDisplayContext context, boolean leftHand) {
		int index = switch (context) {
			case FIRST_PERSON_LEFT_HAND, FIRST_PERSON_RIGHT_HAND -> 0;
			case THIRD_PERSON_LEFT_HAND, THIRD_PERSON_RIGHT_HAND -> 1;
			case GUI -> 2;
			case GROUND -> 3;
			case FIXED -> 4;
			default -> -1;
		};
		if (index >= 0) {
			float[] t = translateType[index];
			pose.translate(leftHand ? -t[0] : t[0], t[1], t[2]);
		}
	}
}
