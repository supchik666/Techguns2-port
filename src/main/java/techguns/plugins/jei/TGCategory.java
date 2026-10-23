package techguns.plugins.jei;

import java.util.List;

import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.AbstractRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.block.Block;
import techguns.Techguns;

/**
 * A JEI category drawn from a TG machine's GUI (1.12 BasicRecipeCategory): the part of its texture at (X, Y, W, H), the
 * full power bar and a power/duration line under it, the block as catalyst and a recipe-click area on its screen.
 * {@link TGJeiPlugin#categories} lists them; subclasses add the slots and what they draw over the texture.
 */
public abstract class TGCategory<T> extends AbstractRecipeCategory<T> {
	/** GUI coordinates (x, y) are (x - X, y - Y) in the category. */
	public static final int X = 7, Y = 15, W = 162, H = 63;
	/** Holds the power bar, as in MachineScreen. */
	private static final ResourceLocation COMMON = Techguns.id("textures/gui/ammo_press_gui.png");
	public final Block block;
	/** The recipe-click area on the block's screen, in GUI coordinates. */
	public final Rect2i clickArea;
	/** The machine's GUI texture, textures/gui/(block id without "_controller")_gui.png. */
	protected final ResourceLocation texture;

	protected TGCategory(IGuiHelper gui, RecipeType<T> type, Component title, Block block, Rect2i clickArea) {
		super(type, title, gui.createDrawableItemLike(block), W, H + 10);
		this.block = block;
		this.clickArea = clickArea;
		this.texture = Techguns.id("textures/gui/" + BuiltInRegistries.BLOCK.getKey(block).getPath().replace("_controller", "") + "_gui.png");
	}

	public abstract List<T> recipes(RecipeManager recipes);

	/** FE/t, 0 for none. */
	protected abstract int power(T recipe);

	/** Ticks. */
	protected abstract int time(T recipe);

	/** Draws over the texture, in GUI coordinates; {@code p} runs from 0 to 1 every {@link #time} ticks. */
	protected void drawGui(T recipe, GuiGraphics g, float p) {}

	@Override
	public void draw(T recipe, IRecipeSlotsView slots, GuiGraphics g, double mouseX, double mouseY) {
		g.blit(texture, 0, 0, X, Y, W, H);
		int time = Math.max(time(recipe), 1);
		float p = (float) (Minecraft.getInstance().level.getGameTime() % time) / time;
		g.pose().pushPose();
		g.pose().translate(-X, -Y, 0);
		if (power(recipe) > 0) g.blit(COMMON, 8, 17, 251, 1, 4, 48);
		drawGui(recipe, g, p);
		g.pose().popPose();
		Font font = Minecraft.getInstance().font;
		if (power(recipe) > 0) g.drawString(font, power(recipe) + " FE/t", 0, H + 1, 0xFF808080, false);
		Component duration = Component.translatable("techguns.tooltip.duration").append(": " + time(recipe) + " ")
				.append(Component.translatable("techguns.tooltip.ticks"));
		g.drawString(font, duration, W - font.width(duration), H + 1, 0xFF808080, false);
	}

	@Override
	public void getTooltip(ITooltipBuilder tooltip, T recipe, IRecipeSlotsView slots, double mouseX, double mouseY) {
		int power = power(recipe);
		if (power > 0 && mouseX >= 8 - X && mouseX < 12 - X && mouseY >= 17 - Y && mouseY < 65 - Y) {
			tooltip.add(Component.translatable("techguns.container.power").append(":"));
			tooltip.add(Component.literal("-" + power + " FE/t"));
			tooltip.add(Component.literal("-" + power * time(recipe) + " FE"));
		}
	}
}
