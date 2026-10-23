package techguns.client.machines;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.model.data.ModelData;
import techguns.TGBlockEntities;
import techguns.Techguns;
import techguns.blocks.Door3x3Block;
import techguns.machines.Door3x3BlockEntity;

/**
 * 1.12 RenderDoor3x3Fast: while the door moves, its master block shows only the frame and this draws the two wings,
 * slid apart by up to 18 px each. Opened and closed doors are all block model.
 */
@EventBusSubscriber(modid = Techguns.MODID, value = Dist.CLIENT)
public final class DoorRenderer implements BlockEntityRenderer<Door3x3BlockEntity> {
	private static final ModelResourceLocation LEFT = ModelResourceLocation.standalone(Techguns.id("block/techdoor3x3_left"));
	private static final ModelResourceLocation RIGHT = ModelResourceLocation.standalone(Techguns.id("block/techdoor3x3_right"));
	private static final float DISTANCE = 1.125f;

	@SubscribeEvent
	static void registerModels(ModelEvent.RegisterAdditional event) {
		event.register(LEFT);
		event.register(RIGHT);
	}

	@SubscribeEvent
	static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
		event.registerBlockEntityRenderer(TGBlockEntities.DOOR3X3.get(), c -> new DoorRenderer());
	}

	@Override
	public void render(Door3x3BlockEntity door, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
		BlockState state = door.getBlockState();
		Door3x3Block.State s = state.getValue(Door3x3Block.STATE);
		if (s == Door3x3Block.State.OPENED || s == Door3x3Block.State.CLOSED) return;
		boolean zplane = state.getValue(Door3x3Block.ZPLANE);
		float offset = DISTANCE * door.openness(partialTick);
		wing(LEFT, -offset, zplane, state, pose, buffers, light, overlay);
		wing(RIGHT, offset, zplane, state, pose, buffers, light, overlay);
	}

	private static void wing(ModelResourceLocation id, float offset, boolean zplane, BlockState state, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
		BakedModel model = Minecraft.getInstance().getModelManager().getModel(id);
		pose.pushPose();
		if (zplane) {
			// 1.12 baked the wings turned 90 degrees about the block corner, then moved them one block along z
			pose.translate(0, 0, 1 - offset);
			pose.mulPose(Axis.YP.rotationDegrees(90));
		} else {
			pose.translate(offset, 0, 0);
		}
		Minecraft.getInstance().getBlockRenderer().getModelRenderer().renderModel(pose.last(), buffers.getBuffer(Sheets.cutoutBlockSheet()), state, model,
				1, 1, 1, light, overlay, ModelData.EMPTY, Sheets.cutoutBlockSheet());
		pose.popPose();
	}

	@Override
	public AABB getRenderBoundingBox(Door3x3BlockEntity door) {
		return new AABB(door.getBlockPos()).inflate(3, 1, 3);
	}
}
