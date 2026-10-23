package techguns.client;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.neoforged.neoforge.network.PacketDistributor;
import techguns.TGRadiation;
import techguns.Techguns;
import techguns.capabilities.TGExtendedPlayer;
import techguns.damagesystem.DamageSystem;
import techguns.events.TGPlayerEvents.Action;
import techguns.events.TGPlayerEvents.PlayerAction;
import techguns.gui.TGPlayerInventoryMenu;
import techguns.items.armors.GenericArmor;

/** 1.12 TGPlayerInventoryGui: the extra slots, the player, and the toggle buttons on the left edge (1.12's five plus armor jump boost). */
public class TGPlayerInventoryScreen extends AbstractContainerScreen<TGPlayerInventoryMenu> {
	public static final ResourceLocation TEXTURE = Techguns.id("textures/gui/tgplayerinventory.png");
	/** Empty slot pictures by {@link techguns.items.additionalslots.TGSlotType} ordinal. */
	private static final ResourceLocation[] EMPTY_ICONS = {icon("face"), icon("back"), icon("hand"), icon("bullet"), icon("food"), icon("heal")};
	private static final String[] TOGGLES = {"togglehud", "togglenv", "togglesafe", "togglestep", "togglejetpack", "togglejump"};
	private static final Action[] ACTIONS = {Action.HUD, Action.NIGHTVISION, Action.SAFEMODE, Action.STEP_ASSIST, Action.JETPACK, Action.JUMP_BOOST};

	public TGPlayerInventoryScreen(TGPlayerInventoryMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title);
	}

	private static ResourceLocation icon(String name) {
		return Techguns.id("textures/gui/emptyslots/emptyslot_" + name + ".png");
	}

	@Override
	protected void init() {
		super.init();
		for (int i = 0; i < TOGGLES.length; i++) addRenderableWidget(new Toggle(leftPos - 8, topPos + 7 + i * 11, i));
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		super.render(g, mouseX, mouseY, partialTick);
		renderTooltip(g, mouseX, mouseY);
		for (var child : children()) {
			if (child instanceof Toggle t && t.isHovered()) g.renderComponentTooltip(font, tooltip(t.mode), mouseX, mouseY);
		}
		int mx = mouseX - leftPos, my = mouseY - topPos;
		if (mx >= 98 && mx < 112 && my >= 7 && my < 21) g.renderComponentTooltip(font, armorTooltip(), mouseX, mouseY);
		if (mx >= 115 && mx < 169 && my >= 7 && my < 13) g.renderComponentTooltip(font, radiationTooltip(), mouseX, mouseY);
	}

	private List<Component> armorTooltip() {
		List<Component> tooltip = new ArrayList<>();
		tooltip.add(Component.translatable("techguns.tgguitooltip.totalarmorvalues").withStyle(ChatFormatting.UNDERLINE));
		GenericArmor.resistances(tooltip, type -> (float) DamageSystem.protectionPercent(DamageSystem.armorValue(minecraft.player, type)), "%");
		return tooltip;
	}

	/** 1.12 getTooltipRadiationBar: the level and what poisoning it causes, see {@link TGRadiation}. */
	private List<Component> radiationTooltip() {
		int rad = TGRadiation.getRadiation(minecraft.player);
		List<Component> tooltip = new ArrayList<>();
		tooltip.add(Component.translatable("techguns.tgguitooltip.currentrad").withStyle(ChatFormatting.GREEN, ChatFormatting.UNDERLINE));
		tooltip.add(Component.literal(rad + "/" + TGRadiation.LETHAL_POISONING));
		Component effects = Component.translatable("techguns.tgguitooltip.radeffects").append(":");
		if (rad < TGRadiation.MINOR_POISONING) {
			tooltip.add(effects.copy().append(" ").append(Component.translatable("techguns.tgguitooltip.radeffects.none").withStyle(ChatFormatting.GREEN)));
			return tooltip;
		}
		tooltip.add(effects);
		Component two = Component.translatable("potion.potency.1");
		if (rad < TGRadiation.SEVERE_POISONING) {
			tooltip.add(Component.literal(" ").append(MobEffects.HUNGER.value().getDisplayName()));
			return tooltip;
		}
		for (var effect : List.of(MobEffects.HUNGER, MobEffects.DIG_SLOWDOWN, MobEffects.WEAKNESS)) {
			tooltip.add(Component.literal(" ").append(effect.value().getDisplayName()).append(" ").append(two));
		}
		if (rad >= TGRadiation.LETHAL_POISONING) {
			for (var effect : List.of(MobEffects.CONFUSION, MobEffects.MOVEMENT_SLOWDOWN)) {
				tooltip.add(Component.literal(" ").append(effect.value().getDisplayName()).append(" ").append(two));
			}
			tooltip.add(Component.literal(" ").append(Component.translatable("techguns.gun.tooltip.damage")));
		}
		return tooltip;
	}

	@Override
	protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
		g.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
		for (Slot slot : menu.slots) {
			if (slot instanceof TGPlayerInventoryMenu.TGSlot s && !s.hasItem()) {
				g.blit(EMPTY_ICONS[s.type.ordinal()], leftPos + s.x, topPos + s.y, 0, 0, 16, 16, 16, 16);
			}
		}
		int rad = TGRadiation.getRadiation(minecraft.player);
		if (rad > 0) g.blit(TEXTURE, leftPos + 116, topPos + 8, 177, 0, Math.min((int) Math.ceil(rad / (double) TGRadiation.LETHAL_POISONING * 52), 52), 4);
		InventoryScreen.renderEntityInInventoryFollowsMouse(g, leftPos + 26, topPos + 8, leftPos + 75, topPos + 78, 30, 0.0625f, mouseX, mouseY,
				minecraft.player);
	}

	@Override
	protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {}

	static boolean state(TGExtendedPlayer tg, int mode) {
		return switch (mode) {
			case 0 -> tg.showTGHudElements;
			case 1 -> tg.enableNightVision;
			case 2 -> tg.enableSafemode;
			case 3 -> tg.enableStepAssist;
			case 4 -> tg.enableJetpack;
			default -> tg.enableJumpBoost;
		};
	}

	private List<Component> tooltip(int mode) {
		String key = "techguns.tgguitooltip." + TOGGLES[mode];
		boolean on = state(TGExtendedPlayer.get(minecraft.player), mode);
		return List.of(Component.translatable(key), Component.translatable("techguns.tgguitooltip.current").append(": ")
				.append(Component.translatable("techguns.tgguitooltip." + (on ? "on" : "off")).withStyle(on ? ChatFormatting.GREEN : ChatFormatting.RED)),
				Component.translatable(key + ".tooltip1"), Component.translatable(key + ".tooltip2"));
	}

	/** 1.12 TGGuiToggleButton: a small button showing the toggle's on/off icon. */
	private class Toggle extends Button {
		final int mode;

		Toggle(int x, int y, int mode) {
			super(x, y, 11, 11, Component.translatable("techguns.tgguitooltip." + TOGGLES[mode]),
					b -> PacketDistributor.sendToServer(new PlayerAction(ACTIONS[mode], false)), DEFAULT_NARRATION);
			this.mode = mode;
		}

		@Override
		protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
			g.blitSprite(SPRITES.get(active, isHoveredOrFocused()), getX(), getY(), width, height);
			boolean on = state(TGExtendedPlayer.get(minecraft.player), mode);
			if (mode == 5) {
				// the texture has no jump icon: an up arrow, green or red like the others
				int x = getX() + 2, y = getY() + 2, color = on ? 0xFF00C000 : 0xFFE00000;
				for (int row = 0; row < 3; row++) g.fill(x + 3 - row, y + row, x + 4 + row, y + row + 1, color);
				g.fill(x + 2, y + 3, x + 5, y + 7, color);
				return;
			}
			int v = 7 * mode + (mode > 3 ? 14 : 0);
			g.blit(TEXTURE, getX() + 2, getY() + 2, 242 + (on ? 7 : 0), v, 7, 7);
		}
	}
}
