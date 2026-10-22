package techguns.client.models;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemDisplayContext;

/**
 * 1.12 ModelMultipart: an item model drawn in parts (part 0 static, higher parts animated), with the 1.12 render signature
 * so ported models keep their code. The renderer calls {@link LegacyGl#begin} first.
 */
public abstract class ModelMultipart extends ModelBase {
	public abstract void render(Entity entityIn, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch,
			float scale, int ammoLeft, float reloadProgress, ItemDisplayContext transformType, int part, float fireProgress, float chargeProgress);

	protected void setRotation(ModelRenderer model, float x, float y, float z) {
		model.rotateAngleX = x;
		model.rotateAngleY = y;
		model.rotateAngleZ = z;
	}
}
