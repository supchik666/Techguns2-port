package techguns.client;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.neoforged.neoforge.network.PacketDistributor;
import techguns.Techguns;
import techguns.capabilities.TGExtendedPlayer;
import techguns.events.TGPlayerEvents.Action;
import techguns.events.TGPlayerEvents.PlayerAction;
import techguns.gui.TGPlayerInventoryMenu;

/** 1.12 TGPlayerInventoryGui: the extra slots, the player, and the five toggle buttons on the left edge. */
public class TGPlayerInventoryScreen extends AbstractContainerScreen<TGPlayerInventoryMenu> {
	public static final ResourceLocation TEXTURE = Techguns.id("textures/gui/tgplayerinventory.png");
	/** Empty slot pictures by {@link techguns.items.additionalslots.TGSlotType} ordinal. */
	private static final ResourceLocation[] EMPTY_ICONS = {icon("face"), icon("back"), icon("hand"), icon("bullet"), icon("food"), icon("heal")};
	private static final String[] TOGGLES = {"togglehud", "togglenv", "togglesafe", "togglestep", "togglejetpack"};

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
		// ponytail: the total armor tooltip (98, 7, 14x14) and the radiation bar (116, 8) come with the damage system and radiation ports
	}

	@Override
	protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
		g.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
		for (Slot slot : menu.slots) {
			if (slot instanceof TGPlayerInventoryMenu.TGSlot s && !s.hasItem()) {
				g.blit(EMPTY_ICONS[s.type.ordinal()], leftPos + s.x, topPos + s.y, 0, 0, 16, 16, 16, 16);
			}
		}
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
			default -> tg.enableJetpack;
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
					b -> PacketDistributor.sendToServer(new PlayerAction(Action.values()[mode], false)), DEFAULT_NARRATION);
			this.mode = mode;
		}

		@Override
		protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
			g.blitSprite(SPRITES.get(active, isHoveredOrFocused()), getX(), getY(), width, height);
			int v = 7 * mode + (mode > 3 ? 14 : 0);
			g.blit(TEXTURE, getX() + 2, getY() + 2, 242 + (state(TGExtendedPlayer.get(minecraft.player), mode) ? 7 : 0), v, 7, 7);
		}
	}
}
