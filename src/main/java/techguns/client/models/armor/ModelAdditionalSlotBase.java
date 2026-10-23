package techguns.client.models.armor;

import techguns.client.models.ModelBiped;
import techguns.client.models.ModelRenderer;
import techguns.client.models.LegacyGl;
import net.minecraft.world.entity.Entity;

public abstract class ModelAdditionalSlotBase extends ModelBiped {
	
	public ModelAdditionalSlotBase() {
		super();
	}

	public ModelAdditionalSlotBase(float modelSize, float p_i1149_2_, int textureWidthIn, int textureHeightIn) {
		super(modelSize, p_i1149_2_, textureWidthIn, textureHeightIn);
	}

	public ModelAdditionalSlotBase(float modelSize) {
		super(modelSize);
	}

	@Override
	public void render(Entity entityIn, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scale) {
		this.setRotationAngles(limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale, entityIn);
		LegacyGl.pushMatrix();
		if (entityIn.isCrouching()) {
			LegacyGl.translate(0.0F, 0.2F, 0.0F);
		}
		this.render(scale, entityIn);
		LegacyGl.popMatrix();
	}
	
	public abstract void render(float scale, Entity entityIn);
	
	public abstract void copyRotateAngles();
	
	@Override
	public void setRotationAngles(float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scaleFactor, Entity entityIn) {
		super.setRotationAngles(limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scaleFactor, entityIn);
		this.copyRotateAngles();
	}
	
	protected void setRotation(ModelRenderer model, float x, float y, float z) {
		model.rotateAngleX = x;
		model.rotateAngleY = y;
		model.rotateAngleZ = z;
	}

}
