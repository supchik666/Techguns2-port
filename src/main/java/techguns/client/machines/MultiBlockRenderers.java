package techguns.client.machines;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import techguns.Techguns;
import techguns.client.models.machines.ModelFabricator;
import techguns.client.models.machines.ModelMachine;
import techguns.machines.multiblock.FabricatorBlockEntity;
import techguns.machines.multiblock.MultiBlocks;
import techguns.machines.multiblock.ReactionChamberBlockEntity;

/**
 * 1.12 RenderFabricator: a formed fabricator's parts have no model, the controller draws the whole machine. Formed
 * reaction chamber and ore drill controllers draw their block model's glow ({@link MachineGlow}).
 * ponytail: no item on the fabricator's plate, no reaction chamber beam or ore drill head yet; formed parts don't glow
 * (the drill engines' green light), the client doesn't know their controller
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
				if (be.formed) MachineRenderers.drawModel(FABRICATOR, FABRICATOR_TEXTURE, pose, buffers, light, overlay, be.direction.get2DDataValue(),
						be.getProgress(), MachineGlow.strength(be));
			}

			@Override
			public AABB getRenderBoundingBox(FabricatorBlockEntity be) {
				return new AABB(be.getBlockPos()).inflate(2);
			}
		});
		event.registerBlockEntityRenderer(MultiBlocks.REACTION_CHAMBER.get(), c -> new BlockEntityRenderer<>() {
			@Override
			public void render(ReactionChamberBlockEntity be, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
				if (!be.formed) return;
				// flares at each reaction tick, fading until the next
				float pulse = Mth.clamp(be.nextTick, 0, ReactionChamberBlockEntity.TICKRATE) / (float) ReactionChamberBlockEntity.TICKRATE;
				MachineGlow.blockModel(be, pose, buffers, be.isWorking() ? MachineGlow.WORKING * (0.6f + 0.4f * pulse) : MachineGlow.IDLE);
			}

			/** The chamber's model reaches 3 blocks across and 4 up from the controller. */
			@Override
			public AABB getRenderBoundingBox(ReactionChamberBlockEntity be) {
				return new AABB(be.getBlockPos()).inflate(4);
			}
		});
		event.registerBlockEntityRenderer(MultiBlocks.ORE_DRILL.get(), c -> (be, partialTick, pose, buffers, light, overlay) -> {
			if (be.formed) MachineGlow.blockModel(be, pose, buffers, MachineGlow.strength(be));
		});
	}
}
