package techguns.client.models;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.Entity;

/**
 * 1.12 ModelBiped for the ported armor and slot models. Nothing animates it: {@link #copyFrom} takes the pose of the
 * vanilla model the layer draws over.
 */
public class ModelBiped extends ModelBase {
	/** How far 1.21 lowers the body of a crouching humanoid; 1.12 models translate by it themselves. */
	private static final float CROUCH_OFFSET = 3.2f;

	private boolean crouching;
	public ModelRenderer bipedHead, bipedHeadwear, bipedBody, bipedRightArm, bipedLeftArm, bipedRightLeg, bipedLeftLeg;

	public ModelBiped() {
		this(0.0f);
	}

	public ModelBiped(float modelSize) {
		this(modelSize, 0.0f, 64, 32);
	}

	public ModelBiped(float modelSize, float yOffset, int textureWidth, int textureHeight) {
		this.textureWidth = textureWidth;
		this.textureHeight = textureHeight;
		bipedHead = new ModelRenderer(this, 0, 0);
		bipedHead.addBox(-4.0f, -8.0f, -4.0f, 8, 8, 8, modelSize);
		bipedHead.setRotationPoint(0.0f, yOffset, 0.0f);
		bipedHeadwear = new ModelRenderer(this, 32, 0);
		bipedHeadwear.addBox(-4.0f, -8.0f, -4.0f, 8, 8, 8, modelSize + 0.5f);
		bipedHeadwear.setRotationPoint(0.0f, yOffset, 0.0f);
		bipedBody = new ModelRenderer(this, 16, 16);
		bipedBody.addBox(-4.0f, 0.0f, -2.0f, 8, 12, 4, modelSize);
		bipedBody.setRotationPoint(0.0f, yOffset, 0.0f);
		bipedRightArm = new ModelRenderer(this, 40, 16);
		bipedRightArm.addBox(-3.0f, -2.0f, -2.0f, 4, 12, 4, modelSize);
		bipedRightArm.setRotationPoint(-5.0f, 2.0f + yOffset, 0.0f);
		bipedLeftArm = new ModelRenderer(this, 40, 16);
		bipedLeftArm.mirror = true;
		bipedLeftArm.addBox(-1.0f, -2.0f, -2.0f, 4, 12, 4, modelSize);
		bipedLeftArm.setRotationPoint(5.0f, 2.0f + yOffset, 0.0f);
		bipedRightLeg = new ModelRenderer(this, 0, 16);
		bipedRightLeg.addBox(-2.0f, 0.0f, -2.0f, 4, 12, 4, modelSize);
		bipedRightLeg.setRotationPoint(-1.9f, 12.0f + yOffset, 0.0f);
		bipedLeftLeg = new ModelRenderer(this, 0, 16);
		bipedLeftLeg.mirror = true;
		bipedLeftLeg.addBox(-2.0f, 0.0f, -2.0f, 4, 12, 4, modelSize);
		bipedLeftLeg.setRotationPoint(1.9f, 12.0f + yOffset, 0.0f);
	}

	public void render(Entity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scale) {
		setRotationAngles(limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale, entity);
		render(scale);
	}

	/** Draws the parts as posed by {@link #copyFrom}. */
	public void render(float scale) {
		LegacyGl.pushMatrix();
		if (crouching) LegacyGl.translate(0.0f, 0.2f, 0.0f);
		bipedHead.render(scale);
		bipedBody.render(scale);
		bipedRightArm.render(scale);
		bipedLeftArm.render(scale);
		bipedRightLeg.render(scale);
		bipedLeftLeg.render(scale);
		bipedHeadwear.render(scale);
		LegacyGl.popMatrix();
	}

	/** 1.12 setModelAttributes plus the pose 1.12 setRotationAngles worked out, and which parts show, read off the vanilla model. */
	public void copyFrom(HumanoidModel<?> model) {
		crouching = model.crouching;
		float lower = crouching ? CROUCH_OFFSET : 0.0f;
		copy(model.head, bipedHead, lower);
		copy(model.hat, bipedHeadwear, lower);
		copy(model.body, bipedBody, lower);
		copy(model.rightArm, bipedRightArm, lower);
		copy(model.leftArm, bipedLeftArm, lower);
		copy(model.rightLeg, bipedRightLeg, lower);
		copy(model.leftLeg, bipedLeftLeg, lower);
	}

	private static void copy(ModelPart from, ModelRenderer to, float lower) {
		to.setRotationPoint(from.x, from.y - lower, from.z);
		to.rotateAngleX = from.xRot;
		to.rotateAngleY = from.yRot;
		to.rotateAngleZ = from.zRot;
		to.showModel = from.visible;
	}
}
