package techguns.client.machines;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import techguns.Techguns;
import techguns.capabilities.TGExtendedPlayer;
import techguns.machines.CamoBenchBlockEntity;
import techguns.machines.Door3x3BlockEntity;
import techguns.machines.ExplosiveChargeBlockEntity;
import techguns.machines.MachineMenu;
import techguns.machines.RepairBenchBlockEntity;
import techguns.machines.TGBlockEntity;
import techguns.machines.UpgradeBenchBlockEntity;

/**
 * Screens of the benches, charges and doors: 1.12 RepairBenchGui, CamoBenchGui, UpgradeBenchGui, ExplosiveChargeGui and
 * Door3x3Gui, on OwnedTileEntGui / RedstoneTileEntGui's security and redstone buttons. Unlike {@link MachineScreen}, no power.
 */
public final class BenchScreens {
	private static final ResourceLocation COMMON = Techguns.id("textures/gui/ammo_press_gui.png");
	private static final int COLOR = 4210752;

	private BenchScreens() {}

	/** The screen for {@code menu}'s block entity, or null if it isn't one of these. */
	@Nullable
	public static AbstractContainerScreen<MachineMenu> create(MachineMenu menu, Inventory inventory, Component title) {
		return switch (menu.be) {
			case RepairBenchBlockEntity be -> new Repair(menu, inventory, title, be);
			case CamoBenchBlockEntity be -> new Camo(menu, inventory, title, be);
			case UpgradeBenchBlockEntity be -> new Upgrade(menu, inventory, title);
			case ExplosiveChargeBlockEntity be -> new Charge(menu, inventory, title, be);
			case Door3x3BlockEntity be -> new Door(menu, inventory, title, be);
			default -> null;
		};
	}

	/** 1.12 TGBaseGui with OwnedTileEntGui's security button at {@code securityY} (-1: none) and RedstoneTileEntGui's at 10. */
	abstract static class Bench extends AbstractContainerScreen<MachineMenu> {
		protected final ResourceLocation texture;
		protected final TGBlockEntity be;
		protected int securityY = 40;
		protected boolean redstone, showInventory = true, showTitle = true;

		Bench(MachineMenu menu, Inventory inventory, Component title, String texture) {
			super(menu, inventory, title);
			this.texture = Techguns.id("textures/gui/" + texture + ".png");
			this.be = menu.be;
		}

		@Override
		protected void init() {
			super.init();
			titleLabelX = (imageWidth - font.width(title)) / 2;
			if (redstone) addRenderableWidget(button(-22, 10, 20, 20, "", TGBlockEntity.BUTTON_REDSTONE));
			if (securityY >= 0) addRenderableWidget(button(-22, securityY, 20, 20, "", TGBlockEntity.BUTTON_SECURITY));
		}

		protected Button button(int x, int y, int w, int h, String text, int id) {
			return Button.builder(Component.literal(text), b -> minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id))
					.bounds(leftPos + x, topPos + y, w, h).build();
		}

		@Override
		protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
			int k = leftPos, l = topPos;
			g.blit(texture, k, l, 0, 0, imageWidth, imageHeight);
			if (securityY >= 0) g.blit(COMMON, k - 27, l + securityY - 5, 195, 0, 27, 30);
			if (redstone) {
				g.blit(COMMON, k - 27, l + 5, 195, 0, 27, 30);
				boolean on = be.redstoneEnabled();
				g.blit(COMMON, k + 6, l + 5, 240, on ? 0 : 5, 5, 5);
				g.blit(COMMON, k + 11, l + 5, 245, on ? 5 : 0, 5, 5);
			}
		}

		@Override
		protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
			if (showTitle) g.drawString(font, title, titleLabelX, titleLabelY, COLOR, false);
			if (showInventory) g.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, COLOR, false);
			if (redstone) g.blit(COMMON, -20, 12, 234, 10 + 16 * be.redstoneMode, 16, 16);
			if (securityY >= 0) g.blit(COMMON, -20, securityY + 2, 234, 58 + 16 * be.security, 16, 16);
		}

		@Override
		public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
			super.render(g, mouseX, mouseY, partialTick);
			renderTooltip(g, mouseX, mouseY);
			List<Component> tooltip = tooltip(mouseX - leftPos, mouseY - topPos);
			if (!tooltip.isEmpty()) g.renderComponentTooltip(font, tooltip, mouseX, mouseY);
		}

		protected static boolean isInRect(int mx, int my, int x, int y, int w, int h) {
			return mx >= x && mx <= x + w && my >= y && my <= y + h;
		}

		/** The tooltip at GUI-relative mouse coordinates; the same texts as {@link MachineScreen}. */
		protected List<Component> tooltip(int mx, int my) {
			if (redstone && (isInRect(mx, my, 6, 5, 10, 5) || isInRect(mx, my, -22, 10, 20, 20))) {
				String mode = switch (be.redstoneMode) {
					case 1 -> "high";
					case 2 -> "low";
					default -> "ignore";
				};
				return List.of(Component.translatable("techguns.container.redstone"),
						Component.translatable("techguns.container.redstone.mode").append(": ").append(Component.translatable("techguns.redstonemode." + mode)),
						Component.translatable("techguns.container.redstone.signal").append(": ")
								.append(Component.translatable("techguns.redstonemode.signal." + (be.hasSignal ? "high" : "low"))));
			}
			if (securityY >= 0 && isInRect(mx, my, -22, securityY, 20, 20)) {
				PlayerInfo info = be.owner == null ? null : minecraft.getConnection().getPlayerInfo(be.owner);
				Component owner = be.owner == null ? Component.translatable("techguns.container.security.tooltip.unowned")
						: Component.translatable("techguns.container.security.tooltip.owner").append(": " + (info != null ? info.getProfile().getName() : be.owner));
				return List.of(Component.translatable("techguns.container.security.owner").append(": ").append(owner),
						Component.translatable("techguns.container.security.tooltip").append(": ").append(Component.translatable(
								"techguns.container.security.tooltip." + be.security).withStyle(be.security == 0 ? ChatFormatting.GREEN : ChatFormatting.RED)),
						Component.translatable("techguns.container.security.tooltip.description." + be.security),
						Component.translatable("techguns.container.security.tooltip.descr2"));
			}
			return List.of();
		}
	}

	/** Repair buttons under the worn armor, the offhand and slot 9; hovering shows what the repair takes. */
	static class Repair extends Bench {
		private static final int[][] BUTTONS = {{90, 39}, {110, 39}, {130, 39}, {150, 39}, {65, 39}, {9, 39}};
		private final RepairBenchBlockEntity bench;

		Repair(MachineMenu menu, Inventory inventory, Component title, RepairBenchBlockEntity bench) {
			super(menu, inventory, title, "repair_bench_gui");
			this.bench = bench;
			securityY = 10;
			showInventory = false;
		}

		@Override
		protected void init() {
			super.init();
			for (int i = 0; i < BUTTONS.length; i++) addRenderableWidget(button(BUTTONS[i][0], BUTTONS[i][1], 14, 14, "", RepairBenchBlockEntity.BUTTON_ARMOR + i));
		}

		@Override
		protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
			super.renderLabels(g, mouseX, mouseY);
			for (int[] b : BUTTONS) g.blit(texture, b[0] + 1, b[1] + 1, 176, 0, 12, 12);
		}

		@Override
		protected List<Component> tooltip(int mx, int my) {
			for (int i = 0; i < BUTTONS.length; i++) {
				if (isInRect(mx, my, BUTTONS[i][0], BUTTONS[i][1], 14, 14)) return materials(bench.target(RepairBenchBlockEntity.BUTTON_ARMOR + i, minecraft.player));
			}
			return super.tooltip(mx, my);
		}

		private List<Component> materials(ItemStack stack) {
			if (stack.isEmpty()) return List.of(Component.translatable("techguns.repairBench.noarmor"));
			if (!RepairBenchBlockEntity.canRepair(stack)) return List.of(Component.translatable("techguns.repairBench.cantrepair"));
			if (!stack.isDamaged()) return List.of(Component.translatable("techguns.repairBench.fullcondition"));
			List<Component> tooltip = new ArrayList<>();
			tooltip.add(Component.translatable("techguns.repairBench.requiredMats"));
			for (ItemStack mat : RepairBenchBlockEntity.repairMaterials(stack)) {
				MutableComponent line = Component.literal(mat.getCount() + "x ").append(mat.getHoverName());
				tooltip.add(bench.count(mat.getItem()) < mat.getCount() ? line.withStyle(ChatFormatting.RED) : line);
			}
			return tooltip;
		}
	}

	/** Camo arrows for the slot's item and +/- for each worn armor piece; the player stands on the left. */
	static class Camo extends Bench {
		Camo(MachineMenu menu, Inventory inventory, Component title, CamoBenchBlockEntity bench) {
			super(menu, inventory, title, "camo_bench_gui");
			securityY = 120;
		}

		@Override
		protected void init() {
			super.init();
			addRenderableWidget(button(26, 59, 12, 12, ">", CamoBenchBlockEntity.BUTTON_NEXT));
			addRenderableWidget(button(14, 59, 12, 12, "<", CamoBenchBlockEntity.BUTTON_PREV));
			for (int k = 0; k < 4; k++) {
				addRenderableWidget(button(102 + k * 18, 59, 10, 10, "+", CamoBenchBlockEntity.BUTTON_ARMOR + 2 * k));
				addRenderableWidget(button(102 + k * 18, 69, 10, 10, "-", CamoBenchBlockEntity.BUTTON_ARMOR + 2 * k + 1));
			}
		}

		@Override
		protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
			super.renderBg(g, partialTick, mouseX, mouseY);
			int k = leftPos, l = topPos;
			g.blit(COMMON, k - 22, l + 90, 195, 0, 22, 20);
			g.blit(COMMON, k - 22, l + 110, 195, 25, 22, 5);
			g.blit(texture, k - 45, l + 6, 178, 0, 45, 84);
			g.blit(texture, k - 18, l + 93, 7, 83, 18, 18);
			InventoryScreen.renderEntityInInventoryFollowsMouse(g, k - 42, l + 8, k - 3, l + 86, 30, 0.0625f, mouseX, mouseY, minecraft.player);
		}

		@Override
		protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
			super.renderLabels(g, mouseX, mouseY);
			ItemStack stack = menu.be.inventory.getStackInSlot(0);
			if (!stack.isEmpty()) {
				g.drawString(font, CamoBenchBlockEntity.camoIndex(stack), 10, 40, COLOR, false);
				g.drawString(font, CamoBenchBlockEntity.camoName(stack), 10, 50, COLOR, false);
			} else {
				g.drawString(font, Component.translatable("techguns.item.invalidcamo"), 10, 50, COLOR, false);
			}
			for (int k = 0; k < 4; k++) g.drawString(font, camo(minecraft.player.getInventory().armor.get(3 - k)), 104 + k * 18, 38, COLOR, false);
			TGExtendedPlayer tg = TGExtendedPlayer.get(minecraft.player);
			int[] x = {49, 68, 86};
			for (int i = 0; i < 3; i++) g.drawString(font, camo(tg.inventory.getItem(TGExtendedPlayer.SLOT_FACE + i)), x[i], 38, COLOR, false);
		}

		/** 1.12 getArmorCamo: the camo number from 1, 0 for none. */
		private static String camo(ItemStack stack) {
			return String.valueOf(CamoBenchBlockEntity.camoCount(stack) > 0 ? CamoBenchBlockEntity.camo(stack) + 1 : 0);
		}
	}

	/** XP and cost; a flashing glow while there is a result. */
	static class Upgrade extends Bench {
		Upgrade(MachineMenu menu, Inventory inventory, Component title) {
			super(menu, inventory, title, "upgrade_bench_gui");
		}

		@Override
		protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
			super.renderBg(g, partialTick, mouseX, mouseY);
			if (!((UpgradeBenchBlockEntity) be).output.getItem(0).isEmpty()) {
				long time = minecraft.level.getGameTime();
				int i = 3 - (int) (time % 4);
				Color c = new Color(Color.HSBtoRGB((time % 360) / 360f, 1, 1));
				g.setColor(c.getRed() / 255f, c.getGreen() / 255f, c.getBlue() / 255f, 1);
				g.blit(texture, leftPos + 37, topPos + 40, i >= 2 ? 0 : 89, 167 + (i % 2 == 1 ? 0 : 31), 89, 31);
				g.setColor(1, 1, 1, 1);
			}
		}

		@Override
		protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
			super.renderLabels(g, mouseX, mouseY);
			int cost = menu.data.get(0), xp = minecraft.player.totalExperience;
			int color = minecraft.player.getAbilities().instabuild || cost <= xp ? 8453920 : 16736352; // the anvil's colors
			xpText(g, Component.translatable("techguns.gui.xp").append(": " + xp), 95, 17, 8453920);
			xpText(g, Component.translatable("techguns.gui.xpcost").append(": " + cost), 95, 27, color);
		}

		/** 1.12 drawXPText: text over a dark outline. */
		private void xpText(GuiGraphics g, Component s, int x, int y, int color) {
			int dark = 0xFF000000 | (color & 0xFCFCFC) >> 2;
			g.drawString(font, s, x, y + 1, dark, false);
			g.drawString(font, s, x + 1, y, dark, false);
			g.drawString(font, s, x + 1, y + 1, dark, false);
			g.drawString(font, s, x, y, color, false);
		}
	}

	/** Radius, length and delay with +/- buttons; Detonate arms the charge and closes the screen. */
	static class Charge extends Bench {
		private final ExplosiveChargeBlockEntity charge;

		Charge(MachineMenu menu, Inventory inventory, Component title, ExplosiveChargeBlockEntity charge) {
			super(menu, inventory, title, charge.advanced ? "exp_charge_gui" : "tnt_charge_gui");
			this.charge = charge;
			securityY = -1;
			showInventory = false;
			showTitle = false;
		}

		@Override
		protected void init() {
			super.init();
			addRenderableWidget(Button.builder(Component.literal("Detonate"), b -> {
				minecraft.gameMode.handleInventoryButtonClick(menu.containerId, ExplosiveChargeBlockEntity.BUTTON_DETONATE);
				onClose();
			}).bounds(leftPos + 57, topPos + 73, 60, 20).build());
			int[] ids = {ExplosiveChargeBlockEntity.BUTTON_RADIUS, ExplosiveChargeBlockEntity.BUTTON_LENGTH, ExplosiveChargeBlockEntity.BUTTON_FUSE};
			for (int i = 0; i < 3; i++) {
				addRenderableWidget(button(120, 40 + i * 10, 10, 10, "+", ids[i]));
				addRenderableWidget(button(130, 40 + i * 10, 10, 10, "-", ids[i] + 1));
			}
		}

		@Override
		protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
			super.renderLabels(g, mouseX, mouseY);
			g.drawString(font, "Radius: " + charge.radius, 37, 40, COLOR, false);
			g.drawString(font, "Length: " + charge.length, 37, 50, COLOR, false);
			g.drawString(font, "Delay: " + charge.fuse / 20 + "s", 37, 60, COLOR, false);
		}
	}

	/** Door mode and auto close; warns when redstone mode has the redstone setting on ignore. */
	static class Door extends Bench {
		private final Door3x3BlockEntity door;
		private Button autoClose;

		Door(MachineMenu menu, Inventory inventory, Component title, Door3x3BlockEntity door) {
			super(menu, inventory, title, "dungeon_scanner_gui");
			this.door = door;
			redstone = true;
			showInventory = false;
		}

		@Override
		protected void init() {
			super.init();
			addRenderableWidget(button(8, 16, 32, 16, "", Door3x3BlockEntity.BUTTON_MODE)).setMessage(Component.translatable("techguns.mode"));
			autoClose = addRenderableWidget(button(8, 36, 32, 16, "", Door3x3BlockEntity.BUTTON_AUTOCLOSE));
		}

		@Override
		public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
			autoClose.setMessage(Component.translatable(door.autoClose ? "techguns.yes" : "techguns.no"));
			super.render(g, mouseX, mouseY, partialTick);
		}

		@Override
		protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
			super.renderLabels(g, mouseX, mouseY);
			g.drawString(font, Component.translatable("techguns.container.door3x3.mode." + door.mode), 44, 20, COLOR, false);
			MutableComponent close = Component.translatable("techguns.container.door3x3.autoclose");
			g.drawString(font, door.mode == 0 ? close : close.withStyle(ChatFormatting.STRIKETHROUGH), 44, 40, COLOR, false);
			if (door.mode == 1 && door.redstoneMode == 0) {
				g.drawString(font, Component.translatable("techguns.container.door3x3.redstonewarning").withStyle(ChatFormatting.RED), 44, 60, COLOR, false);
			}
		}
	}
}
