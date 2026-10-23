package techguns.client.models.armor;

import techguns.client.models.ModelRenderer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import techguns.capabilities.TGExtendedPlayer;
import net.minecraft.util.Mth;

public class ModelJetPack extends ModelAdditionalSlotBase {

	// fields
	ModelRenderer RocketTopR;
	ModelRenderer TopR02;
	ModelRenderer MidR02;
	ModelRenderer TopL02;
	ModelRenderer InnerR02;
	ModelRenderer MidL02;
	ModelRenderer InnerL02;
	ModelRenderer BackBack;
	ModelRenderer RocketR;
	ModelRenderer RocketL;
	ModelRenderer RocketTopL;

	private int variant;

	public ModelJetPack(int variant) {
		// super(0.0625f,0,32,32);
		textureWidth = 32;
		textureHeight = 32;

		this.variant = variant;

		BackBack = new ModelRenderer(this, 16, 0);
		BackBack.addBox(-2F, 0F, 0F, 4, 7, 4);
		BackBack.setRotationPoint(0F, 1F, 2F);
		BackBack.setTextureSize(32, 32);
		BackBack.mirror = true;
		setRotation(BackBack, 0F, 0F, 0F);

		RocketR = new ModelRenderer(this, 0, 0);
		RocketR.addBox(-3F, 0F, 0F, 3, 9, 3);
		RocketR.setRotationPoint(-2F, -1F, 0.5F);
		RocketR.setTextureSize(32, 32);
		RocketR.mirror = true;
		setRotation(RocketR, 0F, 0F, 0F);
		this.BackBack.addChild(RocketR);
		// this.bipedBody.addChild(RocketR);

		RocketL = new ModelRenderer(this, 0, 0);
		RocketL.addBox(-3F, 0F, 0F, 3, 9, 3);
		RocketL.setRotationPoint(5F, -1F, 0.5F);
		RocketL.setTextureSize(32, 32);
		RocketL.mirror = true;
		setRotation(RocketL, 0F, 0F, 0F);
		this.BackBack.addChild(RocketL);

		RocketTopR = new ModelRenderer(this, 0, 13);
		RocketTopR.addBox(1F, 0F, -3.5F, 2, 2, 2);
		RocketTopR.setRotationPoint(-3.5F, -2F, 4F);
		RocketTopR.setTextureSize(32, 32);
		RocketTopR.mirror = true;
		setRotation(RocketTopR, 0F, 0F, 0F);
		this.RocketR.addChild(RocketTopR);
		// this.bipedBody.addChild(RocketTopR);

		RocketTopL = new ModelRenderer(this, 0, 13);
		RocketTopL.addBox(-6F, 0F, -3.5F, 2, 2, 2);
		RocketTopL.setRotationPoint(3.5F, -2F, 4F);
		RocketTopL.setTextureSize(32, 32);
		RocketTopL.mirror = true;
		setRotation(RocketTopL, 0F, 0F, 0F);
		this.RocketL.addChild(RocketTopL);

		if (this.variant == 0) {
			InnerR02 = new ModelRenderer(this, 0, 24);
			InnerR02.addBox(0F, -1F, -2F, 5, 6, 1);
			InnerR02.setRotationPoint(-10F, 2F, 3.5F);
			InnerR02.setTextureSize(32, 32);
			InnerR02.mirror = true;
			setRotation(InnerR02, 0F, 0F, 0F);
			this.BackBack.addChild(InnerR02);

			TopR02 = new ModelRenderer(this, 13, 15);
			TopR02.addBox(10F, -3F, -5.5F, 5, 1, 2);
			TopR02.setRotationPoint(-10F, 1F, 3F);
			TopR02.setTextureSize(32, 32);
			TopR02.mirror = true;
			setRotation(TopR02, 0F, 0F, 0F);
			this.InnerR02.addChild(TopR02);
			// this.bipedBody.addChild(TopR02);

			MidR02 = new ModelRenderer(this, 21, 19);
			MidR02.addBox(10F, -3F, -5.5F, 1, 6, 2);
			MidR02.setRotationPoint(-11F, 2F, 3F);
			MidR02.setTextureSize(32, 32);
			MidR02.mirror = true;
			setRotation(MidR02, 0F, 0F, 0F);
			this.InnerR02.addChild(MidR02);
			// this.bipedBody.addChild(MidR02);

			InnerL02 = new ModelRenderer(this, 0, 24);
			InnerL02.addBox(0F, -1F, -2F, 5, 6, 1);
			InnerL02.setRotationPoint(5F, 2F, 3.5F);
			InnerL02.setTextureSize(32, 32);
			InnerL02.mirror = true;
			setRotation(InnerL02, 0F, 0F, 0F);
			this.BackBack.addChild(InnerL02);

			TopL02 = new ModelRenderer(this, 13, 15);
			TopL02.addBox(-5F, -3F, -5.5F, 5, 1, 2);
			TopL02.setRotationPoint(5F, 1F, 3F);
			TopL02.setTextureSize(32, 32);
			TopL02.mirror = true;
			setRotation(TopL02, 0F, 0F, 0F);
			this.InnerL02.addChild(TopL02);
			// this.bipedBody.addChild(TopL02);

			// this.bipedBody.addChild(InnerR02);

			MidL02 = new ModelRenderer(this, 21, 19);
			MidL02.addBox(-5F, -3F, -5.5F, 1, 6, 2);
			MidL02.setRotationPoint(10F, 2F, 3F);
			MidL02.setTextureSize(32, 32);
			MidL02.mirror = true;
			setRotation(MidL02, 0F, 0F, 0F);
			this.InnerL02.addChild(MidL02);
			// this.bipedBody.addChild(MidL02);
		}

	}

	@Override
	public void render(float scale, Entity entityIn) {
		BackBack.render(scale);
		
		// Flames: TGPlayerClient spawns them each tick for players boosting.
		
	}

	@Override
	public void copyRotateAngles() {
		BackBack.rotateAngleX = this.bipedBody.rotateAngleX;
	}

}
