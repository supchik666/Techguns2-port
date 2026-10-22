package techguns.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import techguns.client.models.ModelMultipart;
import techguns.items.guns.GenericGrenade;

/** A hand grenade that tilts back as it is wound up in first person (1.12 RenderGrenade). */
public class GrenadeRenderer extends ItemModelRenderer {
	public GrenadeRenderer(ModelMultipart model, ResourceLocation texture, float baseRotY) {
		super(model, 1, texture);
		setBaseRotation((pose, context) -> {
			pose.mulPose(Axis.XP.rotationDegrees(-180.0f));
			pose.mulPose(Axis.YP.rotationDegrees(baseRotY));
		});
	}

	@Override
	public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
		float charge = 0;
		pose.pushPose();
		pose.translate(0.5f, 0.5f, 0.5f); // ItemRenderer shifts custom renderers by -0.5; 1.12 drew from the origin
		applyTranslation(pose, context);
		LocalPlayer player = Minecraft.getInstance().player;
		if (context.firstPerson() && player != null && player.isUsingItem() && player.getUseItem() == stack) {
			charge = Math.min(player.getTicksUsingItem() / ((GenericGrenade) stack.getItem()).getFullChargeTime(), 1.0f);
			pose.mulPose(Axis.XP.rotationDegrees(25.0f * charge));
		} else if (context == ItemDisplayContext.GUI) {
			pose.mulPose(Axis.YP.rotationDegrees(40.0f));
			pose.mulPose(Axis.XP.rotationDegrees(20.0f));
		} else if (context == ItemDisplayContext.FIXED) {
			pose.mulPose(Axis.YP.rotationDegrees(-90.0f));
		}
		renderModel(stack, context, pose, buffers, light, overlay, 0, 0, charge, charge);
		pose.popPose();
	}
}
