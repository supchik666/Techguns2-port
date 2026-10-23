package techguns.client.models;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;

/**
 * 1.12 ModelBiped: the seven biped parts as {@link ModelRenderer}s, posed by copying a vanilla {@link HumanoidModel}.
 * ponytail: no baby scaling (1.12 isChild), add it when armor shows up on child mobs.
 */
public class ModelBiped extends ModelBase {
	public ModelRenderer bipedHead, bipedHeadwear, bipedBody, bipedRightArm, bipedLeftArm, bipedRightLeg, bipedLeftLeg;

	public ModelBiped() {
		this(0);
	}

	public ModelBiped(float scale) {
		this(scale, 0, 64, 32);
	}

	public ModelBiped(float scale, float y, int width, int height) {
		textureWidth = width;
		textureHeight = height;
		bipedHead = new ModelRenderer(this, 0, 0);
		bipedHead.addBox(-4, -8, -4, 8, 8, 8, scale);
		bipedHead.setRotationPoint(0, y, 0);
		bipedHeadwear = new ModelRenderer(this, 32, 0);
		bipedHeadwear.addBox(-4, -8, -4, 8, 8, 8, scale + 0.5f);
		bipedHeadwear.setRotationPoint(0, y, 0);
		bipedBody = new ModelRenderer(this, 16, 16);
		bipedBody.addBox(-4, 0, -2, 8, 12, 4, scale);
		bipedBody.setRotationPoint(0, y, 0);
		bipedRightArm = new ModelRenderer(this, 40, 16);
		bipedRightArm.addBox(-3, -2, -2, 4, 12, 4, scale);
		bipedRightArm.setRotationPoint(-5, 2 + y, 0);
		bipedLeftArm = new ModelRenderer(this, 40, 16);
		bipedLeftArm.mirror = true;
		bipedLeftArm.addBox(-1, -2, -2, 4, 12, 4, scale);
		bipedLeftArm.setRotationPoint(5, 2 + y, 0);
		bipedRightLeg = new ModelRenderer(this, 0, 16);
		bipedRightLeg.addBox(-2, 0, -2, 4, 12, 4, scale);
		bipedRightLeg.setRotationPoint(-1.9f, 12 + y, 0);
		bipedLeftLeg = new ModelRenderer(this, 0, 16);
		bipedLeftLeg.mirror = true;
		bipedLeftLeg.addBox(-2, 0, -2, 4, 12, 4, scale);
		bipedLeftLeg.setRotationPoint(1.9f, 12 + y, 0);
	}

	/** Takes the pose (incl. sneaking and arm swing) vanilla computed for the wearer, and which parts it shows. */
	public void copyFrom(HumanoidModel<?> model) {
		copy(model.head, bipedHead);
		copy(model.hat, bipedHeadwear);
		copy(model.body, bipedBody);
		copy(model.rightArm, bipedRightArm);
		copy(model.leftArm, bipedLeftArm);
		copy(model.rightLeg, bipedRightLeg);
		copy(model.leftLeg, bipedLeftLeg);
	}

	private static void copy(ModelPart from, ModelRenderer to) {
		to.setRotationPoint(from.x, from.y, from.z);
		to.rotateAngleX = from.xRot;
		to.rotateAngleY = from.yRot;
		to.rotateAngleZ = from.zRot;
		to.showModel = from.visible;
	}

	public void render(float scale) {
		bipedHead.render(scale);
		bipedBody.render(scale);
		bipedRightArm.render(scale);
		bipedLeftArm.render(scale);
		bipedRightLeg.render(scale);
		bipedLeftLeg.render(scale);
		bipedHeadwear.render(scale);
	}
}
