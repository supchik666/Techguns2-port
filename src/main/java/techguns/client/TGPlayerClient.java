package techguns.client;

import java.util.Map;
import java.util.function.Function;

import org.lwjgl.glfw.GLFW;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.ChatFormatting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import techguns.TGComponents;
import techguns.TGItems;
import techguns.TGMenus;
import techguns.TGSounds;
import techguns.TGEffects;
import techguns.Techguns;
import techguns.capabilities.TGExtendedPlayer;
import techguns.client.models.LegacyGl;
import techguns.client.models.ModelBiped;
import techguns.client.models.armor.*;
import techguns.events.TGPlayerEvents.Action;
import techguns.events.TGPlayerEvents.PlayerAction;
import techguns.items.additionalslots.ItemAntiGravPack;
import techguns.items.additionalslots.ItemJetpack;
import techguns.items.additionalslots.ItemJumpPack;
import techguns.items.additionalslots.ItemNightVisionGoggles;
import techguns.items.additionalslots.ItemTGSpecialSlot;
import techguns.items.additionalslots.ItemTGSpecialSlotAmmo;
import techguns.items.guns.GenericGun;

/** Client side of the extra slots: keys, inventory tabs, HUD icons, the worn models, jetpack flames and loop sounds. */
@EventBusSubscriber(modid = Techguns.MODID, value = Dist.CLIENT)
public class TGPlayerClient {
	private static final String CATEGORY = "techguns.key.categories.techguns";
	static final KeyMapping OPEN_INVENTORY = new KeyMapping("techguns.key.openTGInventory", GLFW.GLFW_KEY_I, CATEGORY);
	static final KeyMapping NIGHTVISION = new KeyMapping("techguns.key.toggleNightvision", GLFW.GLFW_KEY_N, CATEGORY);
	static final KeyMapping SAFEMODE = new KeyMapping("techguns.key.toggleSafemode", GLFW.GLFW_KEY_B, CATEGORY);
	static final KeyMapping STEP_ASSIST = new KeyMapping("techguns.key.toggleStepassist", GLFW.GLFW_KEY_V, CATEGORY);
	static final KeyMapping JETPACK = new KeyMapping("techguns.key.toggleJetpack", GLFW.GLFW_KEY_J, CATEGORY);
	private static final float FLY_SPEED = 0.05f;
	/** We sped up flying for the anti grav pack, so put it back when it's gone. */
	private static boolean flySpeedChanged;

	@SubscribeEvent
	static void registerKeys(RegisterKeyMappingsEvent event) {
		for (KeyMapping key : new KeyMapping[] {OPEN_INVENTORY, NIGHTVISION, SAFEMODE, STEP_ASSIST, JETPACK}) event.register(key);
	}

	@SubscribeEvent
	static void registerScreens(RegisterMenuScreensEvent event) {
		event.register(TGMenus.PLAYER_INVENTORY.get(), TGPlayerInventoryScreen::new);
	}

	@SubscribeEvent
	static void registerGuiLayers(RegisterGuiLayersEvent event) {
		event.registerAboveAll(Techguns.id("player_hud"), TGPlayerClient::renderHud);
	}

	@SubscribeEvent
	static void addLayers(EntityRenderersEvent.AddLayers event) {
		for (PlayerSkin.Model skin : event.getSkins()) {
			if (event.getSkin(skin) instanceof PlayerRenderer renderer) renderer.addLayer(new SlotLayer(renderer, skin == PlayerSkin.Model.SLIM));
		}
	}

	private static void send(Action action, boolean showMsg) {
		PacketDistributor.sendToServer(new PlayerAction(action, showMsg));
	}

	@SubscribeEvent
	static void onClientTickPre(ClientTickEvent.Pre event) {
		LocalPlayer player = Minecraft.getInstance().player;
		if (player == null) return;
		while (OPEN_INVENTORY.consumeClick()) send(Action.OPEN_INVENTORY, false);
		while (NIGHTVISION.consumeClick()) send(Action.NIGHTVISION, true);
		while (SAFEMODE.consumeClick()) send(Action.SAFEMODE, true);
		while (STEP_ASSIST.consumeClick()) send(Action.STEP_ASSIST, true);
		while (JETPACK.consumeClick()) send(Action.JETPACK, true);

		// jetpack boost follows the jump key (1.12 ItemJetpack.onPlayerTick, client part)
		TGExtendedPlayer tg = TGExtendedPlayer.get(player);
		ItemStack back = tg.getSlot(TGExtendedPlayer.SLOT_BACK);
		boolean boost = back.getItem() instanceof ItemJetpack && ItemTGSpecialSlot.hasCharge(back) && tg.enableJetpack
				&& Minecraft.getInstance().options.keyJump.isDown();
		if (boost != tg.isJumpkeyPressed) {
			tg.isJumpkeyPressed = boost;
			send(boost ? Action.BOOST_START : Action.BOOST_STOP, false);
		}

		boolean fast = tg.enableJetpack && ItemAntiGravPack.givesFlight(back);
		if (fast || flySpeedChanged) player.getAbilities().setFlyingSpeed(fast ? FLY_SPEED * (1 + ItemAntiGravPack.FLYBONUS) : FLY_SPEED);
		flySpeedChanged = fast;
	}

	@SubscribeEvent
	static void onClientTickPost(ClientTickEvent.Post event) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null || mc.isPaused()) return;
		for (Player player : mc.level.players()) {
			TGExtendedPlayer tg = TGExtendedPlayer.get(player);
			boolean boosting = tg.isJumpkeyPressed && tg.getSlot(TGExtendedPlayer.SLOT_BACK).getItem() instanceof ItemJetpack;
			if (boosting && tg.jetpackSound == null) {
				playAt(player, TGSounds.JETPACK_START.get());
				tg.jetpackSound = loop(player, TGSounds.JETPACK_LOOP.get(), t -> t.jetpackSound);
			} else if (!boosting && tg.jetpackSound != null) {
				tg.jetpackSound = null;
				playAt(player, TGSounds.JETPACK_END.get());
			}
			if (tg.isGliding && tg.gliderSound == null) tg.gliderSound = loop(player, TGSounds.GLIDER_LOOP.get(), t -> t.gliderSound);
			else if (!tg.isGliding) tg.gliderSound = null;

			double dy = player.getY() - player.yo;
			if (boosting && dy > -0.1) jetpackFlames(player, (float) Math.min(-0.025, dy));
		}
	}

	/** Two flames under the jetpack's nozzles (1.12 ModelJetPack.render). */
	private static void jetpackFlames(Player player, float motionY) {
		float rot = -player.yBodyRot * Mth.DEG_TO_RAD;
		double dZ = player.isCrouching() ? -0.45 : -0.3;
		for (double dX : new double[] {0.2, -0.2}) {
			double x = dZ * Mth.sin(rot) + dX * Mth.cos(rot);
			double z = dZ * Mth.cos(rot) - dX * Mth.sin(rot);
			TGEffects.createFX(player.level(), "FlamethrowerTrailFlames", player.getX() + x, player.getY() + 1.5, player.getZ() + z, 0, motionY, 0);
		}
	}

	private static void playAt(Player player, SoundEvent sound) {
		player.level().playLocalSound(player.getX(), player.getY(), player.getZ(), sound, SoundSource.PLAYERS, 0.75f, 1.0f, false);
	}

	private static Object loop(Player player, SoundEvent sound, Function<TGExtendedPlayer, Object> field) {
		LoopSound s = new LoopSound(player, sound, field);
		Minecraft.getInstance().getSoundManager().play(s);
		return s;
	}

	/** Follows the player until the field it's stored in no longer points at it (1.12 TGSound with setDonePlaying). */
	private static class LoopSound extends AbstractTickableSoundInstance {
		private final Player player;
		private final Function<TGExtendedPlayer, Object> field;

		LoopSound(Player player, SoundEvent sound, Function<TGExtendedPlayer, Object> field) {
			super(sound, SoundSource.PLAYERS, player.getRandom());
			this.player = player;
			this.field = field;
			looping = true;
			volume = 0.75f;
			x = player.getX();
			y = player.getY();
			z = player.getZ();
		}

		@Override
		public void tick() {
			if (player.isRemoved() || field.apply(TGExtendedPlayer.get(player)) != this) stop();
			x = player.getX();
			y = player.getY();
			z = player.getZ();
		}
	}

	/** Vanilla and Techguns tabs above both inventory screens (1.12 used Galacticraft's tab registry). */
	@SubscribeEvent
	static void onScreenInit(ScreenEvent.Init.Post event) {
		if (event.getScreen() instanceof InventoryScreen || event.getScreen() instanceof TGPlayerInventoryScreen) {
			AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) event.getScreen();
			boolean tg = screen instanceof TGPlayerInventoryScreen;
			event.addListener(new Tab(screen, 0, new ItemStack(Items.CRAFTING_TABLE), !tg));
			event.addListener(new Tab(screen, 1, new ItemStack(TGItems.PISTOL_ROUNDS.get()), tg));
		}
	}

	private static class Tab extends AbstractButton {
		private static final ResourceLocation SELECTED = ResourceLocation.withDefaultNamespace("container/creative_inventory/tab_top_selected_1");
		private static final ResourceLocation UNSELECTED = ResourceLocation.withDefaultNamespace("container/creative_inventory/tab_top_unselected_1");
		private final AbstractContainerScreen<?> screen;
		private final int index;
		private final ItemStack icon;
		private final boolean selected;

		Tab(AbstractContainerScreen<?> screen, int index, ItemStack icon, boolean selected) {
			super(0, 0, 26, 32, Component.translatable(index == 0 ? "container.inventory" : "techguns.extendedinventory"));
			this.screen = screen;
			this.index = index;
			this.icon = icon;
			this.selected = selected;
		}

		@Override
		protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
			// follows the screen, which moves when the recipe book opens
			setPosition(screen.getGuiLeft() + 27 * index, screen.getGuiTop() - 28);
			g.blitSprite(selected ? SELECTED : UNSELECTED, getX(), getY(), width, height);
			g.renderItem(icon, getX() + 5, getY() + 9);
		}

		@Override
		public void onPress() {
			if (selected) return;
			Minecraft mc = Minecraft.getInstance();
			if (index == 1) {
				send(Action.OPEN_INVENTORY, false);
			} else {
				mc.player.closeContainer();
				mc.setScreen(new InventoryScreen(mc.player));
			}
		}

		@Override
		protected void updateWidgetNarration(NarrationElementOutput output) {
			defaultButtonNarrationText(output);
		}
	}

	/** 1.12 TGGuiEvents: state icons up the right edge, above the ammo count. */
	private static void renderHud(GuiGraphics g, DeltaTracker delta) {
		Minecraft mc = Minecraft.getInstance();
		Player player = mc.player;
		if (mc.options.hideGui || player == null || player.isSpectator()) return;
		TGExtendedPlayer tg = TGExtendedPlayer.get(player);
		if (!tg.showTGHudElements) return;
		ResourceLocation tex = TGPlayerInventoryScreen.TEXTURE;
		int x = g.guiWidth() - 10;
		int y = g.guiHeight() - 32;
		if (player.getMainHandItem().getItem() instanceof GenericGun || player.getOffhandItem().getItem() instanceof GenericGun) {
			g.blit(tex, x, y, 242 + (tg.enableSafemode ? 7 : 0), 14, 7, 7);
		}
		y -= 10;
		// ponytail: the powered armor charge line (1.12 v=35) comes with the armor port
		ItemStack back = tg.getSlot(TGExtendedPlayer.SLOT_BACK);
		if (back.getItem() instanceof ItemJetpack || back.getItem() instanceof ItemJumpPack || back.getItem() instanceof ItemAntiGravPack) {
			g.blit(tex, x, y, 242 + (tg.enableJetpack ? 7 : 0), 42, 7, 7);
			int percent = (int) (100 * (1 - back.getDamageValue() / (float) back.getMaxDamage()));
			String text = ChatFormatting.YELLOW + "" + TGExtendedPlayer.countAmmo(player, ((ItemTGSpecialSlotAmmo) back.getItem()).getAmmo()) + "x"
					+ ChatFormatting.WHITE + percent + "%";
			g.drawString(mc.font, text, x - 1 - mc.font.width(text), y, 0xFFFFFFFF, false);
			y -= 10;
		}
		// ponytail: night vision and step assist from armor show once the armor port reports them here
		if (ItemNightVisionGoggles.givesNightVision(tg.getSlot(TGExtendedPlayer.SLOT_FACE))) {
			g.blit(tex, x, y, 242 + (tg.enableNightVision ? 7 : 0), 7, 7, 7);
			y -= 10;
		}
		int armorWarning = 0;
		for (EquipmentSlot slot : EquipmentSlot.values()) {
			ItemStack armor = player.getItemBySlot(slot);
			if (slot.getType() != EquipmentSlot.Type.HUMANOID_ARMOR || !armor.isDamageableItem()) continue;
			if (armor.getDamageValue() >= armor.getMaxDamage() - 1) armorWarning = 2;
			else if (armor.getMaxDamage() - armor.getDamageValue() < 0.35 * armor.getMaxDamage()) armorWarning = Math.max(armorWarning, 1);
		}
		if (armorWarning > 0) {
			g.blit(tex, x, y, 242 + (armorWarning == 2 ? 7 : 0), 28, 7, 7);
			y -= 10;
		}
		// ponytail: the RAD line goes here with the radiation port
	}

	/** Draws the face, back and hand slot items on the player (1.12 TGLayerRendererer and RenderAdditionalSlotItem). */
	private static class SlotLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
		/** Model, slim arms model (or null), texture path without the camo suffix and ".png". */
		private record Look(ModelBiped model, ModelBiped slim, String texture) {}

		private final Map<Item, Look> looks;
		private final boolean slim;

		SlotLayer(PlayerRenderer renderer, boolean slim) {
			super(renderer);
			this.slim = slim;
			ModelBiped faceMask = new ModelFaceMask(true);
			looks = Map.of(TGItems.GAS_MASK.get(), new Look(new ModelGasMask(), null, "textures/armors/gasmask"),
					TGItems.GLIDER.get(), new Look(new ModelGlider(), null, "textures/armors/glider"),
					TGItems.JUMPPACK.get(), new Look(new ModelJetPack(1), null, "textures/armors/jetpack"),
					TGItems.JETPACK.get(), new Look(new ModelJetPack(0), null, "textures/armors/jetpack"),
					TGItems.SCUBA_TANKS.get(), new Look(new ModelOxygenTanks(), null, "textures/armors/oxygentanks"),
					TGItems.NIGHTVISION_GOGGLES.get(), new Look(new ModelNightVisionGoggles(), null, "textures/armors/nightvisiongoggles"),
					TGItems.ANTI_GRAV_PACK.get(), new Look(new ModelAntiGravPack(), null, "textures/armors/antigravpack"),
					TGItems.TACTICAL_MASK.get(), new Look(faceMask, null, "textures/armors/tacticalmask"),
					TGItems.OXYGEN_MASK.get(), new Look(faceMask, null, "textures/armors/oxygenmask"),
					TGItems.WORKING_GLOVES.get(), new Look(new ModelGloves(0.45f, false), new ModelGloves(0.45f, true), "textures/models/armor/working_gloves"));
		}

		@Override
		public void render(PoseStack pose, MultiBufferSource buffers, int light, AbstractClientPlayer player, float limbSwing, float limbSwingAmount,
				float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
			if (player.isInvisible()) return;
			TGExtendedPlayer tg = TGExtendedPlayer.get(player);
			// ponytail: armor that hides a slot (1.12 GenericArmor.isHideFaceslot/Backslot/Gloveslot) comes with the armor port
			for (int i = TGExtendedPlayer.SLOT_FACE; i <= TGExtendedPlayer.SLOT_HAND; i++) {
				ItemStack stack = tg.getSlot(i);
				Look look = looks.get(stack.getItem());
				if (look == null) continue;
				boolean slimArms = slim && look.slim != null;
				ModelBiped model = slimArms ? look.slim : look.model;
				int camo = stack.getOrDefault(TGComponents.CAMO, 0);
				ResourceLocation texture = Techguns.id(look.texture + (slimArms ? "_slim" : "") + (camo == 0 ? "" : "_" + camo) + ".png");
				model.copyFrom(getParentModel());
				LegacyGl.begin(pose, buffers, texture, light, OverlayTexture.NO_OVERLAY);
				model.render(player, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, 0.0625f);
			}
		}
	}
}
