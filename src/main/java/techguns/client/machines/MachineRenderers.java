package techguns.client.machines;

import java.util.function.Supplier;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import techguns.TGBlockEntities;
import techguns.TGBlocks;
import techguns.TGFluids;
import techguns.TGMenus;
import techguns.Techguns;
import techguns.client.models.LegacyGl;
import techguns.client.models.machines.ModelAmmoPress;
import techguns.client.models.machines.ModelChemLab;
import techguns.client.models.machines.ModelMachine;
import techguns.client.models.machines.ModelMetalPress;
import techguns.items.guns.GenericGun;
import techguns.machines.BlastFurnaceBlockEntity;
import techguns.machines.ChargingStationBlockEntity;
import techguns.machines.GrinderBlockEntity;
import techguns.machines.MachineBlock;
import techguns.machines.MachineBlockEntity;

/** Machine screens, block entity renderers (1.12 RenderMachine, RenderGrinder, RenderChargingStation, RenderBlastfurnace) and item renderers. */
@EventBusSubscriber(modid = Techguns.MODID, value = Dist.CLIENT)
public final class MachineRenderers {
	private static final ModelMachine AMMO_PRESS = new ModelAmmoPress();
	private static final ModelMachine METAL_PRESS = new ModelMetalPress();
	private static final ModelMachine CHEM_LAB = new ModelChemLab();
	private static final ResourceLocation AMMO_PRESS_TEXTURE = Techguns.id("textures/block/ammopress.png");
	private static final ResourceLocation METAL_PRESS_TEXTURE = Techguns.id("textures/block/metalpress.png");
	private static final ResourceLocation CHEM_LAB_TEXTURE = Techguns.id("textures/block/chemlab.png");
	private static final ResourceLocation GRINDER_ROLL = Techguns.id("textures/block/grinder_roll.png");
	private static final float UNIT = 1 / 16f;

	private MachineRenderers() {}

	@SubscribeEvent
	static void registerScreens(RegisterMenuScreensEvent event) {
		event.register(TGMenus.MACHINE.get(), MachineScreen::create);
	}

	@SubscribeEvent
	static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
		event.registerBlockEntityRenderer(TGBlockEntities.AMMO_PRESS.get(), c -> model(AMMO_PRESS, AMMO_PRESS_TEXTURE));
		event.registerBlockEntityRenderer(TGBlockEntities.METAL_PRESS.get(), c -> model(METAL_PRESS, METAL_PRESS_TEXTURE));
		event.registerBlockEntityRenderer(TGBlockEntities.CHEM_LAB.get(), c -> model(CHEM_LAB, CHEM_LAB_TEXTURE));
		event.registerBlockEntityRenderer(TGBlockEntities.GRINDER.get(), c -> MachineRenderers::renderGrinder);
		event.registerBlockEntityRenderer(TGBlockEntities.CHARGING_STATION.get(), c -> MachineRenderers::renderChargingStation);
		event.registerBlockEntityRenderer(TGBlockEntities.BLAST_FURNACE.get(), c -> MachineRenderers::renderBlastFurnace);
	}

	@SubscribeEvent
	static void registerClientExtensions(RegisterClientExtensionsEvent event) {
		event.registerItem(item(AMMO_PRESS, AMMO_PRESS_TEXTURE), TGBlocks.AMMO_PRESS.asItem());
		event.registerItem(item(METAL_PRESS, METAL_PRESS_TEXTURE), TGBlocks.METAL_PRESS.asItem());
		event.registerItem(item(CHEM_LAB, CHEM_LAB_TEXTURE), TGBlocks.CHEM_LAB.asItem());
		event.registerFluidType(new IClientFluidTypeExtensions() {
			@Override
			public ResourceLocation getStillTexture() {
				return Techguns.id("block/acid_still");
			}

			@Override
			public ResourceLocation getFlowingTexture() {
				return Techguns.id("block/acid_flow");
			}
		}, TGFluids.CREEPER_ACID_TYPE.get());
	}

	private static BlockEntityRenderer<MachineBlockEntity> model(ModelMachine model, ResourceLocation texture) {
		return (be, partialTick, pose, buffers, light, overlay) -> {
			int rotation = be.getBlockState().getValue(MachineBlock.FACING).getOpposite().get2DDataValue();
			drawModel(model, texture, pose, buffers, light, overlay, rotation, be.getProgress());
		};
	}

	/** 1.12 rendered these items with the block entity renderer, unrotated and idle. */
	private static IClientItemExtensions item(ModelMachine model, ResourceLocation texture) {
		Supplier<BlockEntityWithoutLevelRenderer> renderer = () -> new BlockEntityWithoutLevelRenderer(Minecraft.getInstance().getBlockEntityRenderDispatcher(),
				Minecraft.getInstance().getEntityModels()) {
			@Override
			public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
				drawModel(model, texture, pose, buffers, light, overlay, 0, 0);
			}
		};
		return new IClientItemExtensions() {
			private BlockEntityWithoutLevelRenderer instance;

			@Override
			public BlockEntityWithoutLevelRenderer getCustomRenderer() {
				if (instance == null) instance = renderer.get();
				return instance;
			}
		};
	}

	/** 1.12 RenderMachine; {@code rotation} is the placer's horizontal index, as 1.12 stored it. */
	private static void drawModel(ModelMachine model, ResourceLocation texture, PoseStack pose, MultiBufferSource buffers, int light, int overlay,
			int rotation, float progress) {
		LegacyGl.begin(pose, buffers, texture, light, overlay);
		pose.pushPose();
		pose.translate(0.5, 1.5, 0.5);
		pose.mulPose(Axis.ZP.rotationDegrees(180));
		pose.mulPose(Axis.YP.rotationDegrees(180 + 90 * rotation));
		model.render(null, 0, 0, 0, 0, 0, 0.0625f, progress);
		pose.popPose();
	}

	private static void renderGrinder(GrinderBlockEntity be, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
		Direction facing = be.getBlockState().getValue(MachineBlock.FACING);
		float rot = be.isWorking() ? (45 + 1080 * be.getProgress()) % 360 : 45;
		pose.pushPose();
		pose.mulPose(Axis.YP.rotationDegrees(facing.toYRot()));
		switch (facing) {
			case NORTH -> pose.translate(-1, 0, -1);
			case EAST -> pose.translate(0, 0, -1);
			case WEST -> pose.translate(-1, 0, 0);
			default -> {}
		}
		VertexConsumer consumer = buffers.getBuffer(RenderType.entityCutoutNoCull(GRINDER_ROLL));
		for (int i = 0; i < 2; i++) {
			pose.pushPose();
			pose.translate(0, 7.5 * UNIT, (i == 0 ? 6.5 : 9.5) * UNIT);
			pose.mulPose(Axis.XP.rotationDegrees(i == 0 ? rot : -rot));
			drawGrinderRoll(pose, consumer, light);
			pose.popPose();
		}
		ItemStack item = be.grinding();
		if (!item.isEmpty()) {
			boolean gun = item.getItem() instanceof GenericGun;
			pose.pushPose();
			pose.translate(0.5, 0.6 - be.getProgress() * 0.4 + (gun ? 0.15 : 0), 0.5);
			if (gun) {
				pose.scale(0.5f, 0.5f, 0.5f);
				pose.mulPose(Axis.YP.rotationDegrees(90));
			}
			Minecraft.getInstance().getItemRenderer().renderStatic(item, ItemDisplayContext.GROUND, light, OverlayTexture.NO_OVERLAY, pose, buffers, be.getLevel(), 0);
			pose.popPose();
		}
		pose.popPose();
	}

	/** A roll of radius 1.5 px, 12 px long, from x = 2 px: its four sides. */
	private static void drawGrinderRoll(PoseStack pose, VertexConsumer consumer, int light) {
		float r = 1.5f * UNIT, x0 = 2 * UNIT, x1 = x0 + 12 * UNIT, u0 = 2 * UNIT, u1 = u0 + 12 * UNIT, v0 = 2 * UNIT, v1 = v0 + 3 * UNIT;
		float[][] corners = {{r, -r}, {r, r}, {-r, r}, {-r, -r}};
		for (int side = 0; side < 4; side++) {
			float[] a = corners[side], b = corners[(side + 1) % 4];
			float ny = (a[0] + b[0]) / 2 / r, nz = (a[1] + b[1]) / 2 / r;
			vertex(pose, consumer, x0, a[0], a[1], u0, v0, ny, nz, light);
			vertex(pose, consumer, x0, b[0], b[1], u0, v1, ny, nz, light);
			vertex(pose, consumer, x1, b[0], b[1], u1, v1, ny, nz, light);
			vertex(pose, consumer, x1, a[0], a[1], u1, v0, ny, nz, light);
		}
	}

	private static void vertex(PoseStack pose, VertexConsumer consumer, float x, float y, float z, float u, float v, float ny, float nz, int light) {
		consumer.addVertex(pose.last(), x, y, z).setColor(-1).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose.last(), 0, ny, nz);
	}

	private static void renderChargingStation(ChargingStationBlockEntity be, float partialTick, PoseStack pose, MultiBufferSource buffers, int light,
			int overlay) {
		ItemStack item = be.charged();
		if (item.isEmpty()) return;
		pose.pushPose();
		pose.translate(0.5, 0.4, 0.5);
		pose.mulPose(Axis.YP.rotationDegrees(-90 * be.getBlockState().getValue(MachineBlock.FACING).get2DDataValue()));
		Minecraft.getInstance().getItemRenderer().renderStatic(item, ItemDisplayContext.GROUND, light, OverlayTexture.NO_OVERLAY, pose, buffers, be.getLevel(), 0);
		pose.popPose();
	}

	/** Flowing lava glowing through the front grate while working. */
	private static void renderBlastFurnace(BlastFurnaceBlockEntity be, float partialTick, PoseStack pose, MultiBufferSource buffers, int light,
			int overlay) {
		if (!be.isWorking()) return;
		TextureAtlasSprite lava = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(ResourceLocation.withDefaultNamespace("block/lava_flow"));
		Direction facing = be.getBlockState().getValue(MachineBlock.FACING);
		pose.pushPose();
		pose.translate(0.5, 0, 0.5);
		pose.mulPose(Axis.YP.rotationDegrees(-facing.toYRot()));
		pose.translate(-0.5, 0, -0.5);
		// the pane faces south here: 10 x 6 px, 3 px up, just in front of the grate
		VertexConsumer consumer = buffers.getBuffer(RenderType.entitySolid(InventoryMenu.BLOCK_ATLAS));
		float x0 = 3 * UNIT, x1 = 13 * UNIT, y0 = 3 * UNIT, y1 = 9 * UNIT, z = 15 * UNIT + 0.0025f;
		float u0 = lava.getU0(), u1 = lava.getU1(), v0 = lava.getV0(), v1 = lava.getV1();
		int bright = LightTexture.FULL_BRIGHT;
		consumer.addVertex(pose.last(), x0, y0, z).setColor(-1).setUv(u0, v0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(bright).setNormal(pose.last(), 0, 0, 1);
		consumer.addVertex(pose.last(), x1, y0, z).setColor(-1).setUv(u1, v0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(bright).setNormal(pose.last(), 0, 0, 1);
		consumer.addVertex(pose.last(), x1, y1, z).setColor(-1).setUv(u1, v1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(bright).setNormal(pose.last(), 0, 0, 1);
		consumer.addVertex(pose.last(), x0, y1, z).setColor(-1).setUv(u0, v1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(bright).setNormal(pose.last(), 0, 0, 1);
		pose.popPose();
	}
}
