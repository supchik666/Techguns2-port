package techguns.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import techguns.Techguns;
import techguns.entities.projectiles.GrenadeProjectile;

/**
 * Thrown grenade: its item, tumbling once a second along the flight path (1.12 RenderGrenadeProjectile drew the item's
 * model the same way). ponytail: the item's ground transform instead of the 1.12 model offsets.
 */
public class ThrownGrenadeRenderer extends EntityRenderer<GrenadeProjectile> {
	private final ItemRenderer itemRenderer;
	private final ItemStack stack;

	/** {@code item}: id of the grenade item drawn. */
	public ThrownGrenadeRenderer(EntityRendererProvider.Context context, String item) {
		super(context);
		this.itemRenderer = context.getItemRenderer();
		this.stack = new ItemStack(BuiltInRegistries.ITEM.get(Techguns.id(item)));
	}

	@Override
	public void render(GrenadeProjectile entity, float entityYaw, float partialTicks, PoseStack pose, MultiBufferSource buffers, int light) {
		pose.pushPose();
		pose.mulPose(Axis.YP.rotationDegrees(Mth.lerp(partialTicks, entity.yRotO, entity.getYRot()) - 90.0f));
		pose.mulPose(Axis.ZP.rotationDegrees(Mth.lerp(partialTicks, entity.xRotO, entity.getXRot())));
		pose.mulPose(Axis.ZP.rotationDegrees(-360.0f * (entity.tickCount % 20 + partialTicks) / 20.0f));
		itemRenderer.renderStatic(stack, ItemDisplayContext.GROUND, light, OverlayTexture.NO_OVERLAY, pose, buffers, entity.level(), entity.getId());
		pose.popPose();
		super.render(entity, entityYaw, partialTicks, pose, buffers, light);
	}

	@Override
	public ResourceLocation getTextureLocation(GrenadeProjectile entity) {
		return TextureAtlas.LOCATION_BLOCKS;
	}
}
