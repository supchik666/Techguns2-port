package techguns.client.machines;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.InventoryMenu;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import techguns.machines.MachineMenu;
import techguns.machines.multiblock.OreDrillBlockEntity;
import techguns.machines.multiblock.ReactionChamberBlockEntity;
import techguns.recipes.ReactionChamberRecipe;

/** 1.12 FabricatorGui, ReactionChamberGui and OreDrillGui. */
final class MultiBlockScreens {
	private static final int TANK_Y = 17, TANK_W = 10, TANK_H = 50;

	private MultiBlockScreens() {}

	/** The fluid, tiled up from the bottom of a 1.12-sized tank at {@code x, y}. */
	static void fluid(GuiGraphics g, FluidTank tank, int x, int y) {
		FluidStack fluid = tank.getFluid();
		if (fluid.isEmpty()) return;
		int px = tank.getFluidAmount() * TANK_H / tank.getCapacity();
		var ext = IClientFluidTypeExtensions.of(fluid.getFluid());
		TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(ext.getStillTexture(fluid));
		int tint = ext.getTintColor(fluid);
		g.enableScissor(x + 1, y + TANK_H - px, x + 1 + TANK_W, y + TANK_H);
		for (int offset = 0; offset < px; offset += 16) {
			g.blit(x + 1, y + TANK_H - offset - 16, 0, TANK_W, 16, sprite, (tint >> 16 & 255) / 255f, (tint >> 8 & 255) / 255f, (tint & 255) / 255f, 1);
		}
		g.disableScissor();
	}

	static List<Component> tankTooltip(FluidTank tank) {
		return List.of(tank.isEmpty() ? Component.translatable("techguns.gui.empty") : tank.getFluid().getHoverName(),
				Component.literal(tank.getFluidAmount() + "/" + tank.getCapacity() + "mB"));
	}

	static class Fabricator extends MachineScreen {
		Fabricator(MachineMenu menu, Inventory inventory, Component title) {
			super(menu, inventory, title, "fabricator");
		}

		@Override
		protected void renderProgress(GuiGraphics g, int k, int l) {
			g.blit(texture, k + 21, l + 53, 0, 167, menu.progressScaled(90) + 1, 11);
		}
	}

	static class ReactionChamber extends MachineScreen {
		private final ReactionChamberBlockEntity rc;

		ReactionChamber(MachineMenu menu, Inventory inventory, Component title, ReactionChamberBlockEntity rc) {
			super(menu, inventory, title, "reaction_chamber");
			this.rc = rc;
		}

		@Override
		protected void init() {
			super.init();
			addRenderableWidget(button(33, 58, 10, 10, "+", ReactionChamberBlockEntity.BUTTON_LEVEL_UP));
			addRenderableWidget(button(43, 58, 10, 10, "-", ReactionChamberBlockEntity.BUTTON_LEVEL_DOWN));
			addRenderableWidget(button(91, 36, 10, 10, "+", ReactionChamberBlockEntity.BUTTON_INTENSITY_UP));
			addRenderableWidget(button(101, 36, 10, 10, "-", ReactionChamberBlockEntity.BUTTON_INTENSITY_DOWN));
			addRenderableWidget(button(20, 7, 8, 8, "x", ReactionChamberBlockEntity.BUTTON_DUMP));
		}

		@Override
		protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
			super.renderBg(g, partialTick, mouseX, mouseY);
			int k = leftPos, l = topPos;
			fluid(g, rc.tank, k + 18, l + TANK_Y);
			g.blit(texture, k + 18, l + 17, 176, 32, 12, 52);
			g.blit(texture, k + 19, l + 17 + 50 - rc.liquidLevel * 5, 177, 32, 7, 1);
			int px = (10 - rc.intensity) * 4;
			ReactionChamberRecipe recipe = rc.recipe();
			boolean off = recipe != null && rc.requiredIntensity != rc.intensity;
			g.blit(texture, k + 114, l + 16 + px, off ? 198 : 190, px, 5, 40 - px);
			if (recipe != null) {
				g.blit(texture, k + 67, l + 61, 0, 167, Math.round(100f * rc.completion / recipe.requiredCompletion()), 4);
				g.blit(texture, k + 67, l + 69, 0, 175, Math.round(100f * rc.progress / Math.max(rc.totaltime, 1)), 4);
				g.blit(texture, k + 112, l + 15 + 40 - rc.requiredIntensity * 4, 178, 22, 5, 3);
			}
		}

		@Override
		protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
			super.renderLabels(g, mouseX, mouseY);
			g.drawString(font, rc.liquidLevel * 10 + "%", 34, 50, 4210752, false);
			g.drawString(font, String.valueOf(rc.intensity), 121, 18, 4210752, false);
		}

		@Override
		protected List<Component> tooltip(int mx, int my) {
			if (isInRect(mx, my, 18, 16, 11, 51)) return tankTooltip(rc.tank);
			if (isInRect(mx, my, 20, 7, 8, 8)) {
				return List.of(Component.translatable("techguns.chemlab.dumpinput"), Component.translatable("techguns.chemlab.dumpinput.tooltip"));
			}
			return super.tooltip(mx, my);
		}
	}

	static class OreDrill extends MachineScreen {
		private static final int TANK_X = 15;
		private final OreDrillBlockEntity drill;

		OreDrill(MachineMenu menu, Inventory inventory, Component title, OreDrillBlockEntity drill) {
			super(menu, inventory, title, "ore_drill");
			this.drill = drill;
		}

		@Override
		protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
			super.renderBg(g, partialTick, mouseX, mouseY);
			int k = leftPos, l = topPos;
			fluid(g, drill.tank, k + TANK_X, l + TANK_Y);
			g.blit(texture, k + TANK_X, l + TANK_Y, 176, 40, TANK_W + 2, TANK_H + 2);
			if (drill.fuelBufferMax > 0) {
				int s = 12 - drill.fuelBuffer * 12 / drill.fuelBufferMax;
				g.blit(texture, k + 35, l + 36 + s, 188, 39 + s, 6, 12 - s);
			}
		}

		@Override
		protected void renderProgress(GuiGraphics g, int k, int l) {
			g.blit(texture, k + 73, l + 17, 177, 1, 25, menu.progressScaled(36) + 1);
		}

		@Override
		protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
			super.renderLabels(g, mouseX, mouseY);
			Component warning = !drill.formed ? Component.translatable("techguns.container.oredrill.broken") : null;
			if (warning != null) g.drawString(font, warning, (imageWidth - font.width(warning)) / 2 + 14, 24, 0xFFFF2020, false);
		}

		@Override
		protected List<Component> tooltip(int mx, int my) {
			if (isInRect(mx, my, 74, 16, 27, 39) && drill.totaltime > 0) {
				int power = drill.operation != null ? drill.operation.power() : 0;
				return List.of(Component.literal(String.format("%.2f%%", 100f * drill.progress / drill.totaltime)),
						Component.translatable("techguns.gui.oredrill.orehour").append(String.format(": %.2f", 72000.0 / drill.totaltime)),
						Component.translatable("techguns.gui.oredrill.rftick").append(": " + power), Component.empty(),
						Component.translatable("techguns.oredrill.drillsize").append(": " + (drill.rods + drill.engines)),
						Component.translatable("techguns.oredrill.drillradius").append(": " + drill.drillRadius()));
			}
			if (isInRect(mx, my, TANK_X, TANK_Y - 1, TANK_W + 1, TANK_H + 1)) return tankTooltip(drill.tank);
			if (isInRect(mx, my, 34, 36, 8, 14)) {
				if (drill.fuelBufferMax <= 0) return List.of(Component.translatable("techguns.oredrill.nofuel"));
				return List.of(Component.translatable("techguns.oredrill.currentfuel").append(" " + drill.fuelBuffer + "/" + drill.fuelBufferMax));
			}
			return super.tooltip(mx, my);
		}
	}
}
