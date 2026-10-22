package techguns.client.models.guns;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemDisplayContext;
import techguns.Techguns;
import techguns.client.models.LegacyGl;
import techguns.client.models.ModelMultipart;
import techguns.client.models.ModelObj;

/** The grenade launcher OBJ with its drum turning a chamber on each shot (1.12 ModelBaseBakedGrenadeLauncher). */
public class ModelGrenadeLauncher extends ModelMultipart {
	private final ModelObj body = new ModelObj(Techguns.id("models/item/grenadelauncher.obj"));
	private final ModelObj drum = new ModelObj(Techguns.id("models/item/grenadelauncher_1.obj"));

	@Override
	public void render(Entity entityIn, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scale,
			int ammoLeft, float reloadProgress, ItemDisplayContext transformType, int part, float fireProgress, float chargeProgress) {
		body.render(entityIn, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale, ammoLeft, reloadProgress, transformType, part,
				fireProgress, chargeProgress);
		LegacyGl.pushMatrix();
		LegacyGl.translate(0, -2, 0);
		LegacyGl.rotate(60.0f * fireProgress, 1, 0, 0);
		LegacyGl.translate(0, 2, 0);
		drum.render(entityIn, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale, ammoLeft, reloadProgress, transformType, part,
				fireProgress, chargeProgress);
		LegacyGl.popMatrix();
	}
}
