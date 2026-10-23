package techguns.client.machines;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import techguns.Techguns;
import techguns.machines.AmmoPressBlockEntity;
import techguns.machines.BlastFurnaceBlockEntity;
import techguns.machines.ChargingStationBlockEntity;
import techguns.machines.ChemLabBlockEntity;
import techguns.machines.GrinderBlockEntity;
import techguns.machines.MachineBlockEntity;
import techguns.machines.MachineMenu;
import techguns.machines.MetalPressBlockEntity;
import techguns.machines.TGBlockEntity;
import techguns.machines.multiblock.FabricatorBlockEntity;
import techguns.machines.multiblock.OreDrillBlockEntity;
import techguns.machines.multiblock.ReactionChamberBlockEntity;
import techguns.recipes.MachineRecipe;

/**
 * 1.12 TGBaseGui, RedstoneTileEntGui, OwnedTileEntGui and PoweredTileEntGui: the machine's texture, redstone and security
 * buttons on the left, the power bar and the upgrade slot. Subclasses draw each machine's progress and extra buttons.
 */
public class MachineScreen extends AbstractContainerScreen<MachineMenu> {
	/** Holds the shared parts: button panels, redstone lights, power bar, upgrade slot, button icons. */
	private static final ResourceLocation COMMON = Techguns.id("textures/gui/ammo_press_gui.png");
	protected final ResourceLocation texture;
	protected final MachineBlockEntity be;

	protected MachineScreen(MachineMenu menu, Inventory inventory, Component title, String texture) {
		super(menu, inventory, title);
		this.texture = Techguns.id("textures/gui/" + texture + "_gui.png");
		this.be = (MachineBlockEntity) menu.be;
	}

	public static MachineScreen create(MachineMenu menu, Inventory inventory, Component title) {
		return switch (menu.be) {
			case AmmoPressBlockEntity be -> new AmmoPress(menu, inventory, title, be);
			case MetalPressBlockEntity be -> new MetalPress(menu, inventory, title, be);
			case GrinderBlockEntity be -> new Grinder(menu, inventory, title);
			case BlastFurnaceBlockEntity be -> new BlastFurnace(menu, inventory, title);
			case ChargingStationBlockEntity be -> new ChargingStation(menu, inventory, title);
			case ChemLabBlockEntity be -> new ChemLab(menu, inventory, title, be);
			case FabricatorBlockEntity be -> new MultiBlockScreens.Fabricator(menu, inventory, title);
			case ReactionChamberBlockEntity be -> new MultiBlockScreens.ReactionChamber(menu, inventory, title, be);
			case OreDrillBlockEntity be -> new MultiBlockScreens.OreDrill(menu, inventory, title, be);
			default -> throw new IllegalStateException("No screen for " + menu.be);
		};
	}

	@Override
	protected void init() {
		super.init();
		titleLabelX = (imageWidth - font.width(title)) / 2;
		addRenderableWidget(button(-22, 10, 20, 20, "", TGBlockEntity.BUTTON_REDSTONE));
		addRenderableWidget(button(-22, 40, 20, 20, "", TGBlockEntity.BUTTON_SECURITY));
	}

	protected Button button(int x, int y, int w, int h, String text, int id) {
		return Button.builder(Component.literal(text), b -> minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id))
				.bounds(leftPos + x, topPos + y, w, h).build();
	}

	protected static boolean isInRect(int mx, int my, int x, int y, int w, int h) {
		return mx >= x && mx <= x + w && my >= y && my <= y + h;
	}

	@Override
	protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
		int k = leftPos, l = topPos;
		g.blit(texture, k, l, 0, 0, imageWidth, imageHeight);
		g.blit(COMMON, k - 27, l + 5, 195, 0, 27, 30);
		g.blit(COMMON, k - 27, l + 35, 195, 0, 27, 30);
		boolean on = be.redstoneEnabled();
		g.blit(COMMON, k + 6, l + 5, 240, on ? 0 : 5, 5, 5);
		g.blit(COMMON, k + 11, l + 5, 245, on ? 5 : 0, 5, 5);
		int diff = 48 - menu.energy() * 48 / be.energy.getMaxEnergyStored();
		g.blit(COMMON, k + 8, l + 17 + diff, 251, 1 + diff, 4, 48 - diff);
		if (be.upgradeSlot() >= 0) {
			int[] upgrade = be.slots[be.upgradeSlot()];
			g.blit(COMMON, k + upgrade[0] - 1, l + upgrade[1] - 1, 119, 59, 20, 20);
		}
		if (be.isWorking()) renderProgress(g, k, l);
	}

	/** Draws the progress of a working machine; {@code k, l} are the GUI's corner. */
	protected void renderProgress(GuiGraphics g, int k, int l) {}

	@Override
	protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
		super.renderLabels(g, mouseX, mouseY);
		// button icons, over the buttons that renderBg can't draw on
		g.blit(COMMON, -20, 12, 234, 10 + 16 * be.redstoneMode, 16, 16);
		g.blit(COMMON, -20, 42, 234, 58 + 16 * be.security, 16, 16);
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		super.render(g, mouseX, mouseY, partialTick);
		renderTooltip(g, mouseX, mouseY);
		List<Component> tooltip = tooltip(mouseX - leftPos, mouseY - topPos);
		if (!tooltip.isEmpty()) g.renderComponentTooltip(font, tooltip, mouseX, mouseY);
	}

	/** The tooltip at GUI-relative mouse coordinates. */
	protected List<Component> tooltip(int mx, int my) {
		if (isInRect(mx, my, 6, 5, 10, 5) || isInRect(mx, my, -22, 10, 20, 20)) {
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
		if (isInRect(mx, my, -22, 40, 20, 20)) {
			PlayerInfo info = be.owner == null ? null : minecraft.getConnection().getPlayerInfo(be.owner);
			Component owner = be.owner == null ? Component.translatable("techguns.container.security.tooltip.unowned")
					: Component.translatable("techguns.container.security.tooltip.owner").append(": " + (info != null ? info.getProfile().getName() : be.owner));
			return List.of(Component.translatable("techguns.container.security.owner").append(": ").append(owner),
					Component.translatable("techguns.container.security.tooltip").append(": ").append(Component.translatable(
							"techguns.container.security.tooltip." + be.security).withStyle(be.security == 0 ? ChatFormatting.GREEN : ChatFormatting.RED)),
					Component.translatable("techguns.container.security.tooltip.description." + be.security),
					Component.translatable("techguns.container.security.tooltip.descr2"));
		}
		if (isInRect(mx, my, 7, 17, 5, 49)) {
			return List.of(Component.translatable("techguns.container.power").append(": " + menu.energy() + "/" + be.energy.getMaxEnergyStored() + " FE"));
		}
		return List.of();
	}

	static class AmmoPress extends MachineScreen {
		private final AmmoPressBlockEntity press;

		AmmoPress(MachineMenu menu, Inventory inventory, Component title, AmmoPressBlockEntity press) {
			super(menu, inventory, title, "ammo_press");
			this.press = press;
		}

		@Override
		protected void init() {
			super.init();
			addRenderableWidget(button(40, 50, 20, 20, ">", AmmoPressBlockEntity.BUTTON_NEXT));
			addRenderableWidget(button(20, 50, 20, 20, "<", AmmoPressBlockEntity.BUTTON_PREV));
		}

		@Override
		protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
			super.renderLabels(g, mouseX, mouseY);
			g.drawString(font, Component.translatable("techguns.gui.ammopress.build").append(":"), 20, 30, 4210752, false);
			RecipeHolder<MachineRecipe> plan = press.selectedPlan();
			if (plan != null) g.drawString(font, plan.value().results().getFirst().getHoverName(), 20, 40, 4210752, false);
		}

		@Override
		protected void renderProgress(GuiGraphics g, int k, int l) {
			g.blit(texture, k + 119, l + 36, 176, 0, 19, menu.progressScaled(21) + 1);
		}
	}

	static class MetalPress extends MachineScreen {
		private final MetalPressBlockEntity press;

		MetalPress(MachineMenu menu, Inventory inventory, Component title, MetalPressBlockEntity press) {
			super(menu, inventory, title, "metal_press");
			this.press = press;
		}

		@Override
		protected void init() {
			super.init();
			addRenderableWidget(button(20, 50, 40, 20, "", MetalPressBlockEntity.BUTTON_AUTOSPLIT)).setMessage(Component.translatable("techguns.button.switch"));
		}

		@Override
		protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
			super.renderLabels(g, mouseX, mouseY);
			g.drawString(font, Component.translatable("techguns.container.metalpress.autosplit").append(":"), 20, 30, 4210752, false);
			g.drawString(font, Component.translatable("techguns.container.info." + (press.autoSplit ? "on" : "off")), 20, 40, 4210752, false);
		}

		@Override
		protected void renderProgress(GuiGraphics g, int k, int l) {
			g.blit(texture, k + 119, l + 36, 176, 0, 19, menu.progressScaled(21) + 1);
		}
	}

	static class Grinder extends MachineScreen {
		Grinder(MachineMenu menu, Inventory inventory, Component title) {
			super(menu, inventory, title, "grinder");
		}

		@Override
		protected void renderProgress(GuiGraphics g, int k, int l) {
			int i1 = menu.progressScaled(21), i2 = 20 - i1;
			g.blit(texture, k + 31, l + 39, 0, 167, i1 + 1, 22);
			g.blit(texture, k + 51 + i2, l + 39, i2, 167, i1 + 1, 22);
		}
	}

	static class BlastFurnace extends MachineScreen {
		BlastFurnace(MachineMenu menu, Inventory inventory, Component title) {
			super(menu, inventory, title, "blast_furnace");
		}

		@Override
		protected void renderProgress(GuiGraphics g, int k, int l) {
			g.blit(texture, k + 19, l + 53, 0, 167, menu.progressScaled(90) + 1, 11);
		}
	}

	static class ChargingStation extends MachineScreen {
		ChargingStation(MachineMenu menu, Inventory inventory, Component title) {
			super(menu, inventory, title, "charging_station");
		}

		@Override
		protected void renderProgress(GuiGraphics g, int k, int l) {
			g.blit(texture, k + 39, l + 19, 0, 167, menu.progressScaled(26) + 1, 10);
		}
	}

	static class ChemLab extends MachineScreen {
		private static final int TANK_Y = 17, TANK_W = 10, TANK_H = 50;
		private final ChemLabBlockEntity lab;

		ChemLab(MachineMenu menu, Inventory inventory, Component title, ChemLabBlockEntity lab) {
			super(menu, inventory, title, "chem_lab");
			this.lab = lab;
		}

		@Override
		protected void init() {
			super.init();
			addRenderableWidget(button(20, 7, 8, 8, "x", ChemLabBlockEntity.BUTTON_DUMP_INPUT));
			addRenderableWidget(button(159, 7, 8, 8, "x", ChemLabBlockEntity.BUTTON_DUMP_OUTPUT));
			addRenderableWidget(button(156, 68, 14, 14, "", ChemLabBlockEntity.BUTTON_TOGGLE_DRAIN));
		}

		@Override
		protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
			super.renderBg(g, partialTick, mouseX, mouseY);
			tank(g, lab.inputTank, leftPos + 18);
			tank(g, lab.outputTank, leftPos + 157);
		}

		/** The fluid, tiled up from the bottom, under the tank's scale. */
		private void tank(GuiGraphics g, FluidTank tank, int x) {
			FluidStack fluid = tank.getFluid();
			int y = topPos + TANK_Y;
			if (!fluid.isEmpty()) {
				int px = tank.getFluidAmount() * TANK_H / tank.getCapacity();
				var ext = IClientFluidTypeExtensions.of(fluid.getFluid());
				TextureAtlasSprite sprite = minecraft.getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(ext.getStillTexture(fluid));
				int tint = ext.getTintColor(fluid);
				g.enableScissor(x + 1, y + TANK_H - px, x + 1 + TANK_W, y + TANK_H);
				for (int offset = 0; offset < px; offset += 16) {
					g.blit(x + 1, y + TANK_H - offset - 16, 0, TANK_W, 16, sprite, (tint >> 16 & 255) / 255f, (tint >> 8 & 255) / 255f, (tint & 255) / 255f, 1);
				}
				g.disableScissor();
			}
			g.blit(texture, x, y, 176, 32, TANK_W + 2, TANK_H + 2);
		}

		@Override
		protected void renderProgress(GuiGraphics g, int k, int l) {
			float p = menu.totaltime() > 0 ? (float) menu.progress() / menu.totaltime() : 0;
			g.blit(texture, k + 81, l + 19, 178, 5, 8, (int) (Math.min(p * 5, 1) * 25));
			if (p >= 0.2f) g.blit(texture, k + 88, l + 39, 186, 25, (int) (Math.min((p - 0.2f) * 5, 1) * 9), 1);
			if (p >= 0.4f) {
				int i3 = (int) (Math.min((p - 0.4f) * 5, 1) * 20);
				g.blit(texture, k + 97, l + 23 + 20 - i3, 194, 9 + 20 - i3, 14, i3);
			}
			if (p >= 0.6f) g.blit(texture, k + 108, l + 17, 205, 3, (int) (Math.min((p - 0.6f) * 5, 1) * 16), 6);
			if (p >= 0.8f) g.blit(texture, k + 117, l + 20, 214, 6, 14, (int) ((p - 0.8f) * 5 * 24) + 1);
		}

		@Override
		protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
			super.renderLabels(g, mouseX, mouseY);
			g.blit(texture, 157, 69, 188, 31 + 12 * (lab.drainInput ? 1 : 0), 12, 12);
		}

		@Override
		protected List<Component> tooltip(int mx, int my) {
			if (isInRect(mx, my, 18, TANK_Y - 1, TANK_W + 1, TANK_H + 1)) return tankTooltip(lab.inputTank);
			if (isInRect(mx, my, 157, TANK_Y - 1, TANK_W + 1, TANK_H + 1)) return tankTooltip(lab.outputTank);
			if (isInRect(mx, my, 20, 7, 8, 8)) {
				return List.of(Component.translatable("techguns.chemlab.dumpinput"), Component.translatable("techguns.chemlab.dumpinput.tooltip"));
			}
			if (isInRect(mx, my, 159, 7, 8, 8)) {
				return List.of(Component.translatable("techguns.chemlab.dumpoutput"), Component.translatable("techguns.chemlab.dumpoutput.tooltip"));
			}
			if (isInRect(mx, my, 156, 68, 14, 14)) {
				return List.of(Component.translatable("techguns.chemlab.toogledrain"),
						Component.translatable("techguns.chemlab.toogledrain.tooltip." + (lab.drainInput ? 1 : 0)));
			}
			return super.tooltip(mx, my);
		}

		private static List<Component> tankTooltip(FluidTank tank) {
			List<Component> tooltip = new ArrayList<>();
			tooltip.add(tank.isEmpty() ? Component.translatable("techguns.gui.empty") : tank.getFluid().getHoverName());
			tooltip.add(Component.literal(tank.getFluidAmount() + "/" + tank.getCapacity() + "mB"));
			return tooltip;
		}
	}
}
