package techguns.client.machines;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.AABB;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import techguns.Techguns;
import techguns.client.models.LegacyGl;
import techguns.client.models.machines.ModelFabricator;
import techguns.client.models.machines.ModelMachine;
import techguns.machines.multiblock.FabricatorBlockEntity;
import techguns.machines.multiblock.MultiBlocks;

/**
 * 1.12 RenderFabricator: a formed fabricator's parts have no model, the controller draws the whole machine.
 * ponytail: no item on the fabricator's plate, no reaction chamber beam or ore drill head yet
 */
@EventBusSubscriber(modid = Techguns.MODID, value = Dist.CLIENT)
public final class MultiBlockRenderers {
	private static final ModelMachine FABRICATOR = new ModelFabricator();
	private static final ResourceLocation FABRICATOR_TEXTURE = Techguns.id("textures/block/fabricator.png");

	private MultiBlockRenderers() {}

	@SubscribeEvent
	static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
		event.registerBlockEntityRenderer(MultiBlocks.FABRICATOR.get(), c -> new BlockEntityRenderer<>() {
			@Override
			public void render(FabricatorBlockEntity be, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
				if (!be.formed) return;
				LegacyGl.begin(pose, buffers, FABRICATOR_TEXTURE, light, overlay);
				pose.pushPose();
				pose.translate(0.5, 1.5, 0.5);
				pose.mulPose(Axis.ZP.rotationDegrees(180));
				pose.mulPose(Axis.YP.rotationDegrees(180 + 90 * be.direction.get2DDataValue()));
				FABRICATOR.render(null, 0, 0, 0, 0, 0, 0.0625f, be.getProgress());
				pose.popPose();
			}

			@Override
			public AABB getRenderBoundingBox(FabricatorBlockEntity be) {
				return new AABB(be.getBlockPos()).inflate(2);
			}
		});
	}
}
