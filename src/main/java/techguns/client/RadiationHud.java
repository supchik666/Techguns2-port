package techguns.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.EffectRenderingInventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.extensions.common.IClientMobEffectExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import techguns.TGRadiation;
import techguns.Techguns;

/** The radiation HUD (1.12 TGGuiEvents), effect icons and the tooltip of radioactive items. */
@EventBusSubscriber(modid = Techguns.MODID, value = Dist.CLIENT)
public final class RadiationHud {
	/** 1.12 drew the effect icons from the player inventory GUI texture, 16x16 at v 168. */
	private static final ResourceLocation ICONS = Techguns.id("textures/gui/tgplayerinventory.png");

	private RadiationHud() {}

	@SubscribeEvent
	static void registerEffectIcons(RegisterClientExtensionsEvent event) {
		event.registerMobEffect(icon(0), TGRadiation.RADIATION.get());
		event.registerMobEffect(icon(16), TGRadiation.RAD_RESISTANCE.get());
		event.registerMobEffect(icon(32), TGRadiation.RAD_REGENERATION.get());
	}

	/** "Radiation II" on items in the {@link TGRadiation#ITEM_RADIATION} data map. */
	@SubscribeEvent
	static void tooltip(ItemTooltipEvent event) {
		Integer amount = event.getItemStack().getItem().builtInRegistryHolder().getData(TGRadiation.ITEM_RADIATION);
		if (amount != null) {
			event.getToolTip().add(Component.translatable("effect.techguns.radiation").append(" ")
					.append(Component.translatable("potion.potency." + (amount - 1))).withStyle(ChatFormatting.GREEN));
		}
	}

	/**
	 * "N RAD" at the right edge at {@code y}, colored by poisoning stage, and above it what the radiation effect adds each
	 * second. Drawn on top of the other Techguns HUD icons by {@link TGPlayerClient}.
	 */
	static void render(GuiGraphics graphics, int y) {
		Player player = Minecraft.getInstance().player;
		int rad = TGRadiation.getRadiation(player);
		if (rad <= 0) return;
		ChatFormatting color = rad >= TGRadiation.LETHAL_POISONING ? ChatFormatting.RED
				: rad >= TGRadiation.SEVERE_POISONING ? ChatFormatting.GOLD : rad >= TGRadiation.MINOR_POISONING ? ChatFormatting.YELLOW : ChatFormatting.WHITE;
		drawRight(graphics, Component.literal(rad + " RAD").withStyle(color), y);
		MobEffectInstance effect = player.getEffect(TGRadiation.RADIATION);
		if (effect != null) drawRight(graphics, Component.literal("+[" + TGRadiation.radPerSecond(player, effect.getAmplifier()) + " RAD/s]"), y - 10);
	}

	private static void drawRight(GuiGraphics graphics, Component text, int y) {
		Minecraft mc = Minecraft.getInstance();
		graphics.drawString(mc.font, text, graphics.guiWidth() - 2 - mc.font.width(text), y, 0xFFFFFFFF, false);
	}

	/** Draws the effect's icon at {@code u} in {@link #ICONS} instead of a mob_effect sprite. */
	private static IClientMobEffectExtensions icon(int u) {
		return new IClientMobEffectExtensions() {
			@Override
			public boolean renderInventoryIcon(MobEffectInstance instance, EffectRenderingInventoryScreen<?> screen, GuiGraphics graphics, int x, int y,
					int blitOffset) {
				graphics.blit(ICONS, x + 1, y + 8, u, 168, 16, 16);
				return true;
			}

			@Override
			public boolean renderGuiIcon(MobEffectInstance instance, Gui gui, GuiGraphics graphics, int x, int y, float z, float alpha) {
				graphics.setColor(1, 1, 1, alpha);
				graphics.blit(ICONS, x + 4, y + 4, u, 168, 16, 16);
				graphics.setColor(1, 1, 1, 1);
				return true;
			}
		};
	}
}
