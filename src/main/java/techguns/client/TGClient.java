package techguns.client;

import java.util.function.Supplier;

import org.lwjgl.glfw.GLFW;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.ChatFormatting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.event.RenderLivingEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.network.PacketDistributor;
import techguns.TGEntities;
import techguns.TGuns;
import techguns.Techguns;
import techguns.client.models.ModelPistol;
import techguns.client.render.GenericProjectileRenderer;
import techguns.client.render.GunAnimation;
import techguns.client.render.GunRenderer;
import techguns.client.render.ScreenEffect;
import techguns.items.guns.GenericGun;
import techguns.network.TGPackets.GunAction;

/** Client setup and per-frame hooks: gun input, held-gun rendering, crosshair and ammo HUD. */
@EventBusSubscriber(modid = Techguns.MODID, value = Dist.CLIENT)
public class TGClient {
	static final KeyMapping FORCE_RELOAD = new KeyMapping("techguns.key.forceReload", GLFW.GLFW_KEY_R, "techguns.key.categories.techguns");
	private static final ResourceLocation CROSSHAIRS = Techguns.id("textures/gui/tg_crosshairs.png");

	/** The living entity currently being rendered, so the gun renderer knows who holds it in third person. */
	public static LivingEntity renderedEntity;
	/** Attack was clicked; kept until the gun is ready so semi-auto clicks during the fire delay aren't lost. */
	private static boolean firePressed;
	/** Holding attack on a block re-sends the click event every tick; only the first one is a semi-auto shot. */
	private static boolean attackHeld;

	@SubscribeEvent
	static void registerKeys(RegisterKeyMappingsEvent event) {
		event.register(FORCE_RELOAD);
	}

	@SubscribeEvent
	static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
		event.registerEntityRenderer(TGEntities.GENERIC_PROJECTILE.get(), GenericProjectileRenderer::new);
	}

	@SubscribeEvent
	static void registerGuiLayers(RegisterGuiLayersEvent event) {
		event.registerAboveAll(Techguns.id("ammo"), TGClient::renderAmmoCount);
	}

	@SubscribeEvent
	static void registerItemExtensions(RegisterClientExtensionsEvent event) {
		event.registerItem(gun(() -> new GunRenderer(new ModelPistol(), 2, Techguns.id("textures/guns/pistol3.png"))
				.setBaseTranslation(0.0625f * 0.5f, -0.3f, -0.4f).setBaseScale(1.2f).setGUIScale(0.9f)
				.setMuzzleFx(ScreenEffect.muzzleFlash_gun, 0.03f, 0.2f, -0.5f, 0.55f, -0.03f)
				.setTransformTranslations(new float[][] {{0, 0.09f, -0.02f}, {0, -0.03f, 0}, {0.02f, -0.08f, 0}, {0.02f, -0.08f, 0}, {0, 0, 0}})
				.setRecoilAnim(GunAnimation.genericRecoil, 0.025f, 12.0f).setMuzzleFlashJitter(0.01f, 0.01f, 5.0f, 0.05f)
				.setMuzzleFXPos3P(0.07f, -0.26f)), TGuns.PISTOL.get());
	}

	/** Renderer is built on first use: the BEWLR constructor needs Minecraft's renderers to exist. */
	private static IClientItemExtensions gun(Supplier<GunRenderer> factory) {
		return new IClientItemExtensions() {
			private GunRenderer renderer;

			@Override
			public BlockEntityWithoutLevelRenderer getCustomRenderer() {
				if (renderer == null) renderer = factory.get();
				return renderer;
			}

			@Override
			public HumanoidModel.ArmPose getArmPose(LivingEntity entity, InteractionHand hand, ItemStack stack) {
				return hand == InteractionHand.MAIN_HAND ? HumanoidModel.ArmPose.BOW_AND_ARROW : null;
			}
		};
	}

	/** Left click with a gun shoots instead of attacking or mining. */
	@SubscribeEvent
	static void onClickInput(InputEvent.InteractionKeyMappingTriggered event) {
		LocalPlayer player = Minecraft.getInstance().player;
		if (event.isAttack() && player != null && player.getMainHandItem().getItem() instanceof GenericGun) {
			event.setCanceled(true);
			event.setSwingHand(false);
			if (!attackHeld) firePressed = true;
			attackHeld = true;
		}
	}

	@SubscribeEvent
	static void onClientTick(ClientTickEvent.Post event) {
		Minecraft mc = Minecraft.getInstance();
		LocalPlayer player = mc.player;
		if (!mc.options.keyAttack.isDown()) attackHeld = false;
		if (player == null || !(player.getMainHandItem().getItem() instanceof GenericGun gun)) {
			firePressed = false;
			while (FORCE_RELOAD.consumeClick()) {}
			return;
		}
		ItemStack stack = player.getMainHandItem();
		boolean holding = !gun.isSemiAuto() && mc.screen == null && mc.options.keyAttack.isDown();
		if ((firePressed || holding) && GenericGun.isReady(player, InteractionHand.MAIN_HAND)) {
			firePressed = false;
			PacketDistributor.sendToServer(new GunAction(InteractionHand.MAIN_HAND, false));
			gun.shootGunPrimary(stack, player.level(), player, InteractionHand.MAIN_HAND);
		}
		while (FORCE_RELOAD.consumeClick()) {
			PacketDistributor.sendToServer(new GunAction(InteractionHand.MAIN_HAND, true));
			gun.tryForcedReload(stack, player.level(), player, InteractionHand.MAIN_HAND);
		}
	}

	@SubscribeEvent
	static void onRenderLivingPre(RenderLivingEvent.Pre<?, ?> event) {
		renderedEntity = event.getEntity();
	}

	@SubscribeEvent
	static void onRenderLivingPost(RenderLivingEvent.Post<?, ?> event) {
		renderedEntity = null;
	}

	/** Gun crosshair (1.12 GUN_DYNAMIC): four ticks pushed apart by the gun's spread. */
	@SubscribeEvent
	static void onRenderCrosshair(RenderGuiLayerEvent.Pre event) {
		Minecraft mc = Minecraft.getInstance();
		if (!event.getName().equals(VanillaGuiLayers.CROSSHAIR) || mc.player == null || !mc.options.getCameraType().isFirstPerson()
				|| mc.getDebugOverlay().showDebugScreen() || !(mc.player.getMainHandItem().getItem() instanceof GenericGun gun)) {
			return;
		}
		event.setCanceled(true);
		GuiGraphics graphics = event.getGuiGraphics();
		int x = graphics.guiWidth() / 2;
		int y = graphics.guiHeight() / 2;
		int spacing = Mth.clamp(Math.round(100 * gun.getSpread()), 1, 10);
		RenderSystem.enableBlend();
		RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.ONE_MINUS_DST_COLOR, GlStateManager.DestFactor.ONE_MINUS_SRC_COLOR,
				GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
		graphics.blit(CROSSHAIRS, x - (4 + spacing), y, 3, 7, 4, 1);
		graphics.blit(CROSSHAIRS, x + (1 + spacing), y, 8, 7, 4, 1);
		graphics.blit(CROSSHAIRS, x, y, 7, 7, 1, 1);
		graphics.blit(CROSSHAIRS, x, y - (4 + spacing), 7, 3, 1, 4);
		graphics.blit(CROSSHAIRS, x, y + (1 + spacing), 7, 8, 1, 4);
		RenderSystem.defaultBlendFunc();
		RenderSystem.disableBlend();
	}

	/** "ammo/clipsize xMagazines" in the bottom right corner. */
	private static void renderAmmoCount(GuiGraphics graphics, DeltaTracker delta) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.options.hideGui || mc.player == null || !(mc.player.getMainHandItem().getItem() instanceof GenericGun gun)) return;
		ItemStack stack = mc.player.getMainHandItem();
		int mags = mc.player.getInventory().countItem(gun.getAmmoType().ammo().get());
		String text = gun.getAmmo(stack) + "/" + gun.getClipsize() + ChatFormatting.YELLOW + "x" + mags;
		graphics.drawString(mc.font, text, graphics.guiWidth() - 1 - mc.font.width(text), graphics.guiHeight() - mc.font.lineHeight - 2, 0xFFFFFFFF, false);
	}
}
