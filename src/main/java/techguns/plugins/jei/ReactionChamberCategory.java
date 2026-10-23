package techguns.plugins.jei;

import java.util.List;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.neoforge.NeoForgeTypes;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.fluids.FluidStack;
import techguns.machines.multiblock.MultiBlocks;
import techguns.machines.multiblock.ReactionChamberBlockEntity;
import techguns.recipes.ReactionChamberRecipe;

/**
 * 1.12 ReactionChamberJeiRecipeCategory: input, focus, the fluid at the recipe's level and the results, with the wanted
 * intensity, completion and progress bars, the risk icon and their tooltips. Power is shown as FE/t averaged over a
 * reaction tick.
 */
public class ReactionChamberCategory extends TGCategory<RecipeHolder<ReactionChamberRecipe>> {
	private static final int TICKRATE = ReactionChamberBlockEntity.TICKRATE;
	private static final int[][] OUTPUTS = {{134, 17}, {152, 17}, {134, 35}, {152, 35}};
	private final IDrawable scale;

	public ReactionChamberCategory(IGuiHelper gui, Block block, Rect2i clickArea) {
		super(gui, RecipeType.createFromVanilla(MultiBlocks.REACTION_CHAMBER_RECIPE.get()), block.getName(), block, clickArea);
		scale = gui.createDrawable(texture, 177, 32, 10, 50);
	}

	@Override
	public List<RecipeHolder<ReactionChamberRecipe>> recipes(RecipeManager recipes) {
		return recipes.getAllRecipesFor(MultiBlocks.REACTION_CHAMBER_RECIPE.get());
	}

	@Override
	protected int power(RecipeHolder<ReactionChamberRecipe> recipe) {
		return recipe.value().power() / TICKRATE;
	}

	@Override
	protected int time(RecipeHolder<ReactionChamberRecipe> recipe) {
		return recipe.value().ticks() * TICKRATE;
	}

	@Override
	public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<ReactionChamberRecipe> holder, IFocusGroup focuses) {
		ReactionChamberRecipe r = holder.value();
		builder.addSlot(RecipeIngredientRole.INPUT, 35 - X, 17 - Y).addIngredients(r.input());
		builder.addSlot(RecipeIngredientRole.CATALYST, 93 - X, 17 - Y).addItemStack(r.focus().getDefaultInstance());
		List<FluidStack> fluids = List.of(r.fluid().getStacks()).stream().map(f -> f.copyWithAmount(r.liquidLevel() * 1000)).toList();
		builder.addSlot(RecipeIngredientRole.INPUT, 19 - X, 17 - Y).setFluidRenderer(10000, false, 10, 50).setOverlay(scale, 0, 0)
				.addIngredients(NeoForgeTypes.FLUID_STACK, fluids)
				.addRichTooltipCallback((view, tooltip) -> tooltip.add(Component.translatable("techguns.container.reactionchamber.consumption")
						.append(": " + r.liquidConsumption() + " mB").withStyle(ChatFormatting.GRAY)));
		for (int i = 0; i < Math.min(r.results().size(), OUTPUTS.length); i++) {
			builder.addSlot(RecipeIngredientRole.OUTPUT, OUTPUTS[i][0] - X, OUTPUTS[i][1] - Y).addItemStack(r.results().get(i));
		}
	}

	@Override
	protected void drawGui(RecipeHolder<ReactionChamberRecipe> holder, GuiGraphics g, float p) {
		ReactionChamberRecipe r = holder.value();
		int done = Math.min((int) (p * r.ticks()), r.requiredCompletion());
		g.blit(texture, 67, 61, 0, 167, 100 * done / r.requiredCompletion(), 4);
		g.blit(texture, 67, 69, 0, 175, (int) (p * 100), 4);
		int px = (10 - r.intensity()) * 4; // ponytail: static bar, 1.12 animated the drift; unstable recipes draw it in the "off" color
		g.blit(texture, 114, 16 + px, r.margin() > 0 ? 198 : 190, px, 5, 40 - px);
		g.blit(texture, 112, 15 + 40 - r.intensity() * 4, 178, 22, 5, 3);
		int risk = switch (r.risk()) {
			case NONE, BREAK_ITEM -> 0;
			case EXPLOSION_LOW, EXPLOSION_MEDIUM -> 2;
		};
		g.blit(texture, 96, 38, 205, 12 * risk, 12, 12);
		var font = Minecraft.getInstance().font;
		g.drawString(font, r.liquidLevel() * 10 + "%", 34, 50, 4210752, false);
		g.drawString(font, String.valueOf(r.intensity()), 121, 18, 4210752, false);
	}

	@Override
	public void getTooltip(ITooltipBuilder tooltip, RecipeHolder<ReactionChamberRecipe> holder, IRecipeSlotsView slots, double mouseX, double mouseY) {
		ReactionChamberRecipe r = holder.value();
		double x = mouseX + X, y = mouseY + Y;
		if (in(x, y, 66, 60, 102, 14)) {
			tooltip.add(key("completion").append(": ").append(Component.literal("" + r.requiredCompletion()).withStyle(ChatFormatting.GREEN)));
			tooltip.add(key("recipeticks").append(": ").append(Component.literal("" + r.ticks()).withStyle(ChatFormatting.RED)));
			tooltip.add(key("time").append(": " + r.ticks() * TICKRATE / 20 + "s"));
		} else if (in(x, y, 113, 16, 7, 41)) {
			tooltip.add(key("intensity").append(": " + r.intensity()));
			if (r.margin() > 0) {
				tooltip.add(key("intensitymargin").append(": +/-" + r.margin()));
				tooltip.add(key("chance").append(": " + (int) (r.instability() * 100) + "%"));
			} else {
				tooltip.add(key("stable").withStyle(ChatFormatting.GREEN));
			}
		} else if (in(x, y, 96, 38, 12, 12)) {
			tooltip.add(key("risk").append(": " + r.risk().getSerializedName()));
		} else if (in(x, y, 8, 17, 4, 48)) {
			tooltip.add(Component.translatable("techguns.container.power").append(":"));
			tooltip.add(Component.literal("-" + r.power() + " FE/").append(key("recipetick")));
			tooltip.add(Component.literal("-" + r.power() * r.requiredCompletion() + " FE"));
		}
	}

	private static boolean in(double x, double y, int rx, int ry, int w, int h) {
		return x >= rx && x < rx + w && y >= ry && y < ry + h;
	}

	private static MutableComponent key(String name) {
		return Component.translatable("techguns.container.reactionchamber." + name);
	}
}
