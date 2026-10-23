package techguns.client.models;

import net.minecraft.world.entity.Entity;

/** 1.12 ModelBase: the texture size new {@link ModelRenderer}s pick up, and the no-op pose and render hooks models override or still call. */
public abstract class ModelBase {
	public int textureWidth = 64;
	public int textureHeight = 32;

	public void setRotationAngles(float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scale,
			Entity entity) {}

	public void render(Entity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scale) {}
}
