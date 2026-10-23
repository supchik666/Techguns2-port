package techguns.client.machines;

import java.util.List;
import java.util.Map;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import techguns.TGBlockEntities;
import techguns.TGBlocks;
import techguns.TGEntities;
import techguns.Techguns;
import techguns.client.TGClient;
import techguns.client.models.LegacyGl;
import techguns.client.models.ModelMultipart;
import techguns.client.models.machines.ModelTurretBase;
import techguns.client.models.npcs.ModelTurret;
import techguns.entities.npcs.NPCTurret;
import techguns.machines.MachineMenu;
import techguns.machines.TurretBlockEntity;

/** The turret's renderers (1.12 RenderNPCTurret, RenderTurret) and GUI (1.12 TurretGui). */
@EventBusSubscriber(modid = Techguns.MODID, value = Dist.CLIENT)
public final class TurretClient {
	private static final ModelMultipart TURRET = new ModelTurret();
	private static final ModelMultipart BASE = new ModelTurretBase();
	/** By armor tier. */
	private static final ResourceLocation[] TEXTURES = {Techguns.id("textures/block/turret_base.png"), Techguns.id("textures/block/turret_base2.png"),
			Techguns.id("textures/block/turret_base3.png"), Techguns.id("textures/block/turret_base4.png"), Techguns.id("textures/block/turret_base5.png")};
	/** 1.12 GenericGun.setTurretPosOffset, by gun id; others sit at 0. */
	private static final Map<String, Vec3> GUN_OFFSETS = Map.ofEntries(Map.entry("ak47", new Vec3(0, 0, 0.08)), Map.entry("m4", new Vec3(0, 0, 0.08)),
			Map.entry("m4_infiltrator", new Vec3(0, 0, 0.08)), Map.entry("thompson", new Vec3(0, 0, 0.04)), Map.entry("boltaction", new Vec3(0, 0, 0.14)),
			Map.entry("flamethrower", new Vec3(0, 0, 0.1)), Map.entry("rocketlauncher", new Vec3(0, 0, -0.1)), Map.entry("minigun", new Vec3(0, -0.49, -0.14)),
			Map.entry("grimreaper", new Vec3(0, 0.11, -0.16)), Map.entry("as50", new Vec3(0, 0.03, 0.13)), Map.entry("pulserifle", new Vec3(0, 0, -0.09)),
			Map.entry("lasergun", new Vec3(0, 0.01, 0.11)), Map.entry("alienblaster", new Vec3(0, -0.03, -0.04)),
			Map.entry("netherblaster", new Vec3(0, -0.16, 0.12)), Map.entry("chainsaw", new Vec3(0, -0.47, -0.08)),
			Map.entry("miningdrill", new Vec3(0, -0.47, -0.08)), Map.entry("shishkebap", new Vec3(0, -0.47, -0.08)),
			Map.entry("nucleardeathray", new Vec3(0, 0.04, -0.19)), Map.entry("mac10", new Vec3(0, 0, -0.07)), Map.entry("mibgun", new Vec3(0, -0.04, 0)),
			Map.entry("scar", new Vec3(0, 0.02, 0.09)), Map.entry("vector", new Vec3(0, -0.1, 0.15)), Map.entry("gaussrifle", new Vec3(0, -0.02, 0.12)),
			Map.entry("guidedmissilelauncher", new Vec3(0, 0.01, -0.12)));

	private TurretClient() {}

	@SubscribeEvent
	static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
		event.registerEntityRenderer(TGEntities.TURRET.get(), TurretRenderer::new);
		event.registerBlockEntityRenderer(TGBlockEntities.TURRET.get(), c -> (be, partialTick, pose, buffers, light, overlay) ->
				drawBase(pose, buffers, TEXTURES[be.tier()], light, overlay, be.hanging()));
	}

	@SubscribeEvent
	static void registerClientExtensions(RegisterClientExtensionsEvent event) {
		event.registerItem(new IClientItemExtensions() {
			private BlockEntityWithoutLevelRenderer renderer;

			@Override
			public BlockEntityWithoutLevelRenderer getCustomRenderer() {
				if (renderer == null) renderer = new BlockEntityWithoutLevelRenderer(Minecraft.getInstance().getBlockEntityRenderDispatcher(),
						Minecraft.getInstance().getEntityModels()) {
					@Override
					public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
						drawBase(pose, buffers, TEXTURES[0], light, overlay, false);
					}
				};
				return renderer;
			}
		}, TGBlocks.TURRET.asItem());
	}

	/** 1.12 RenderTurret: the base model, flipped under a ceiling. */
	private static void drawBase(PoseStack pose, MultiBufferSource buffers, ResourceLocation texture, int light, int overlay, boolean hanging) {
		LegacyGl.begin(pose, buffers, texture, light, overlay);
		pose.pushPose();
		if (hanging) {
			pose.translate(0.5, -0.5, 0.5);
		} else {
			pose.translate(0.5, 1.5, 0.5);
			pose.mulPose(Axis.ZP.rotationDegrees(180));
		}
		pose.mulPose(Axis.YP.rotationDegrees(180));
		BASE.render(null, 0, 0, 0, 0, 0, 0.0625f, 0, 0, ItemDisplayContext.FIXED, 0, 0, 0);
		pose.popPose();
	}

	/** 1.12 RenderNPCTurret: the mount turns with the head, the top pitches, and the gun sits on top. */
	static class TurretRenderer extends EntityRenderer<NPCTurret> {
		TurretRenderer(EntityRendererProvider.Context context) {
			super(context);
		}

		@Override
		public void render(NPCTurret turret, float entityYaw, float partialTicks, PoseStack pose, MultiBufferSource buffers, int light) {
			TurretBlockEntity be = turret.base();
			boolean hanging = turret.isHanging();
			float mirror = hanging ? -1 : 1;
			pose.pushPose();
			LegacyGl.begin(pose, buffers, getTextureLocation(turret), light, LivingEntityRenderer.getOverlayCoords(turret, 0));
			if (hanging) {
				pose.translate(0, -0.6, 0);
			} else {
				pose.translate(0, 1.5, 0);
				pose.mulPose(Axis.XP.rotationDegrees(180));
			}
			pose.mulPose(Axis.YP.rotationDegrees(Mth.rotLerp(partialTicks, turret.yHeadRotO, turret.yHeadRot) * mirror + (hanging ? 180 : 0)));
			TURRET.render(turret, 0, 0, 0, 0, 0, 0.0625f, 0, 0, ItemDisplayContext.FIXED, 0, 0, 0);
			pose.translate(0, 0.9, 0);
			pose.mulPose(Axis.XP.rotationDegrees(Mth.lerp(partialTicks, turret.xRotO, turret.getXRot()) * mirror));
			pose.translate(0, -0.9, 0);
			TURRET.render(turret, 0, 0, 0, 0, 0, 0.0625f, 0, 0, ItemDisplayContext.FIXED, 1, 0, 0);
			ItemStack gun = be == null ? ItemStack.EMPTY : be.weapon();
			if (!gun.isEmpty()) {
				Vec3 offset = GUN_OFFSETS.getOrDefault(BuiltInRegistries.ITEM.getKey(gun.getItem()).getPath(), Vec3.ZERO);
				pose.translate(offset.x, offset.y + 0.85, offset.z);
				pose.mulPose(Axis.XP.rotationDegrees(180));
				pose.mulPose(Axis.YP.rotationDegrees(180));
				LivingEntity rendered = TGClient.renderedEntity;
				TGClient.renderedEntity = turret; // the gun renderer's third person recoil and reload
				Minecraft.getInstance().getItemRenderer().renderStatic(turret, gun, ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, false, pose, buffers,
						turret.level(), light, OverlayTexture.NO_OVERLAY, turret.getId());
				TGClient.renderedEntity = rendered;
			}
			pose.popPose();
			super.render(turret, entityYaw, partialTicks, pose, buffers, light);
		}

		@Override
		public ResourceLocation getTextureLocation(NPCTurret turret) {
			TurretBlockEntity be = turret.base();
			return TEXTURES[be == null ? 0 : be.tier()];
		}
	}

	/** 1.12 TurretGui: target animals and PvP buttons, and the repair bar (health tooltip while alive). */
	static class Screen extends MachineScreen {
		private final TurretBlockEntity turret;

		Screen(MachineMenu menu, Inventory inventory, Component title, TurretBlockEntity turret) {
			super(menu, inventory, title, "turret_base");
			this.turret = turret;
		}

		@Override
		protected void init() {
			super.init();
			addRenderableWidget(button(-22, 70, 20, 20, "", TurretBlockEntity.BUTTON_TARGET_ANIMALS));
			addRenderableWidget(button(-22, 100, 20, 20, "", TurretBlockEntity.BUTTON_PVP));
		}

		@Override
		protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
			super.renderBg(g, partialTick, mouseX, mouseY);
			int k = leftPos, l = topPos;
			g.blit(texture, k - 27, l + 65, 195, 0, 27, 30);
			g.blit(texture, k - 27, l + 95, 195, 0, 27, 30);
			if (menu.totaltime() > 0) g.blit(texture, k + 116, l + 74, 177, 1, 52 - menu.progress() * 52 / menu.totaltime() + 1, 4);
		}

		@Override
		protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
			super.renderLabels(g, mouseX, mouseY);
			g.blit(texture, -20, 72, 176, 6 + (turret.attackAnimals ? 16 : 0), 16, 16);
			g.blit(texture, -20, 102, 193, 6 + 16 * turret.pvp, 16, 16);
		}

		@Override
		protected List<Component> tooltip(int mx, int my) {
			if (isInRect(mx, my, 115, 73, 53, 5)) {
				if (menu.totaltime() > 0) return List.of(Component.translatable("techguns.container.repair").append(" " + menu.progress() / 20 + "s"));
				return List.of(Component.translatable("techguns.turret.health").append(": " + menu.data.get(4) + "/" + menu.data.get(5)));
			}
			if (isInRect(mx, my, -22, 70, 20, 20)) {
				return List.of(Component.translatable("techguns.turret.targetAnimals").append(": ").append(turret.attackAnimals
						? Component.translatable("techguns.yes").withStyle(ChatFormatting.RED) : Component.translatable("techguns.no").withStyle(ChatFormatting.GREEN)));
			}
			if (isInRect(mx, my, -22, 100, 20, 20)) {
				return List.of(Component.translatable("techguns.turret.pvpsetting").append(": ").append(Component.translatable("techguns.turret.pvpsetting." + turret.pvp)),
						Component.translatable("techguns.container.security.tooltip.descr2"));
			}
			return super.tooltip(mx, my);
		}
	}
}
