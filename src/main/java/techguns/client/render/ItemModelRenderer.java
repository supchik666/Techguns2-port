package techguns.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import techguns.client.models.LegacyGl;
import techguns.client.models.ModelMultipart;

/**
 * Draws an item from a {@link ModelMultipart}, a port of 1.12 RenderItemBase; the setters take the same numbers as the
 * 1.12 ClientProxy registrations. ponytail: held-item particles and NPC gun scaling are left out.
 */
public class ItemModelRenderer extends BlockEntityWithoutLevelRenderer {
	public static final float SCALE = 0.0625f;

	/** The turn from 1.12 model space to item space; 1.12 had a renderer subclass per variant. */
	public interface BaseRotation {
		void apply(PoseStack pose, ItemDisplayContext context);
	}

	public static final BaseRotation DEFAULT = (pose, context) -> {
		pose.mulPose(Axis.XP.rotationDegrees(-180.0f));
		pose.mulPose(Axis.YP.rotationDegrees(180.0f));
	};
	/** 1.12 RenderGunBase90. */
	public static final BaseRotation GUN90 = (pose, context) -> {
		pose.mulPose(Axis.XP.rotationDegrees(-180.0f));
		pose.mulPose(Axis.YP.rotationDegrees(-90.0f));
	};
	/** 1.12 RenderItemLMGMag. */
	public static final BaseRotation LMG_MAG = (pose, context) -> {
		pose.mulPose(Axis.XP.rotationDegrees(-180.0f));
		boolean flat = context == ItemDisplayContext.GUI || context == ItemDisplayContext.FIXED;
		pose.mulPose(Axis.YP.rotationDegrees(flat ? 90.0f : 180.0f));
	};
	/** 1.12 RenderItemBaseRocketItem. */
	public static final BaseRotation ROCKET = (pose, context) -> {
		pose.mulPose(Axis.XP.rotationDegrees(-180.0f));
		pose.mulPose(Axis.YP.rotationDegrees(-90.0f));
		pose.mulPose(Axis.ZP.rotationDegrees(-90.0f));
	};

	protected final ModelMultipart model;
	protected final int parts;
	private final ResourceLocation texture;
	protected BaseRotation baseRotation = DEFAULT;

	protected float baseScale = 1.0f;
	protected float scaleThirdp = 0.35f;
	protected float scaleGround = 0.5f;
	protected float scaleEgo = 0.5f;
	protected float scaleGui = 0.4f;
	protected float scaleItemframe = 0.5f;
	protected float[] translateBase = {0f, 0f, 0f};
	/** First person, third person, GUI, ground, item frame. Mirrored for the left hand. */
	protected float[][] translateType = {{0f, 0f, 0f}, {0f, 0f, 0f}, {0f, 0f, 0f}, {0f, 0f, 0f}, {0f, 0f, -0.05f}};

	public ItemModelRenderer(ModelMultipart model, int parts, ResourceLocation texture) {
		super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
		this.model = model;
		this.parts = parts;
		this.texture = texture;
	}

	public ItemModelRenderer setBaseRotation(BaseRotation rotation) {
		baseRotation = rotation;
		return this;
	}

	public ItemModelRenderer setBaseTranslation(float x, float y, float z) {
		translateBase = new float[] {x, y, z};
		return this;
	}

	public ItemModelRenderer setBaseScale(float baseScale) {
		this.baseScale = baseScale;
		return this;
	}

	public ItemModelRenderer setGUIScale(float scale) {
		scaleGui = scale;
		return this;
	}

	public ItemModelRenderer setFirstPersonScale(float scale) {
		scaleEgo = scale;
		return this;
	}

	public ItemModelRenderer setGroundAndFrameScale(float scale) {
		scaleGround = scale;
		scaleItemframe = scale;
		return this;
	}

	public ItemModelRenderer setTransformTranslations(float[][] translations) {
		translateType = translations;
		return this;
	}

	protected ResourceLocation getTexture(ItemStack stack) {
		return texture;
	}

	@Override
	public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
		pose.pushPose();
		pose.translate(0.5f, 0.5f, 0.5f); // ItemRenderer shifts custom renderers by -0.5; 1.12 drew from the origin
		applyTranslation(pose, context);
		if (context == ItemDisplayContext.GUI) {
			pose.mulPose(Axis.YP.rotationDegrees(40.0f));
			pose.mulPose(Axis.XP.rotationDegrees(20.0f));
		} else if (context == ItemDisplayContext.FIXED) {
			pose.mulPose(Axis.YP.rotationDegrees(-90.0f));
		}
		renderModel(stack, context, pose, buffers, light, overlay, 0, 0, 0, 0);
		pose.popPose();
	}

	/** Scale, base rotation and translation, then every part of the model. */
	protected void renderModel(ItemStack stack, ItemDisplayContext context, PoseStack pose, MultiBufferSource buffers, int light, int overlay,
			int ammo, float reloadProgress, float fireProgress, float chargeProgress) {
		float scale = baseScale * scaleFor(context);
		pose.scale(scale, scale, scale);
		baseRotation.apply(pose, context);
		pose.translate(translateBase[0], translateBase[1], translateBase[2]);
		LegacyGl.begin(pose, buffers.getBuffer(RenderType.entityCutout(getTexture(stack))), light, overlay);
		for (int i = 0; i < parts; i++) {
			model.render(null, 0, 0, 0, 0, 0, SCALE, ammo, reloadProgress, context, i, fireProgress, chargeProgress);
		}
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

	protected void applyTranslation(PoseStack pose, ItemDisplayContext context) {
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
			boolean left = context == ItemDisplayContext.FIRST_PERSON_LEFT_HAND || context == ItemDisplayContext.THIRD_PERSON_LEFT_HAND;
			pose.translate(left ? -t[0] : t[0], t[1], t[2]);
		}
	}
}
