package techguns.plugins.jei;

import java.util.List;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.neoforge.NeoForgeTypes;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import techguns.TGConfig;
import techguns.machines.ChemLabBlockEntity;
import techguns.machines.MachineBlockEntity;
import techguns.recipes.MachineRecipe;

/**
 * The {@link MachineRecipe}s of one machine: slots where a block entity made for the category has them, inputs in order,
 * the bottle, results with their chances, and the progress drawn as MachineScreen draws it.
 */
public class MachineCategory extends TGCategory<RecipeHolder<MachineRecipe>> {
	/** Draws the progress at {@code p} (0 to 1) in GUI coordinates. */
	public interface Progress {
		void draw(GuiGraphics g, ResourceLocation texture, float p);
	}

	protected final MachineBlockEntity be;
	private final Progress progress;

	public MachineCategory(IGuiHelper gui, Block block, Rect2i clickArea, Progress progress) {
		this(gui, block, (MachineBlockEntity) ((EntityBlock) block).newBlockEntity(BlockPos.ZERO, block.defaultBlockState()), clickArea, progress);
	}

	private MachineCategory(IGuiHelper gui, Block block, MachineBlockEntity be, Rect2i clickArea, Progress progress) {
		super(gui, RecipeType.createFromVanilla(be.kind.type.get()), be.getDisplayName(), block, clickArea);
		this.be = be;
		this.progress = progress;
	}

	@Override
	public List<RecipeHolder<MachineRecipe>> recipes(RecipeManager recipes) {
		return recipes.getAllRecipesFor(be.kind.type.get());
	}

	@Override
	protected int power(RecipeHolder<MachineRecipe> recipe) {
		double scale = be.kind == MachineRecipe.CHARGING_STATION ? 1 : TGConfig.MACHINE_POWER_MULTIPLIER.get();
		return (int) ((recipe.value().power() > 0 ? recipe.value().power() : be.defaultPower) * scale);
	}

	@Override
	protected int time(RecipeHolder<MachineRecipe> recipe) {
		return recipe.value().time();
	}

	/** A slot at the block entity's slot {@code index}. */
	protected IRecipeSlotBuilder slot(IRecipeLayoutBuilder builder, RecipeIngredientRole role, int index) {
		return builder.addSlot(role, be.slots[index][0] - X, be.slots[index][1] - Y);
	}

	@Override
	public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<MachineRecipe> holder, IFocusGroup focuses) {
		MachineRecipe recipe = holder.value();
		for (int i = 0; i < Math.min(recipe.inputs().size(), be.inputs); i++) {
			slot(builder, RecipeIngredientRole.INPUT, i).addItemStacks(List.of(recipe.inputs().get(i).getItems()));
		}
		if (be.bottle) recipe.bottle().ifPresent(b -> slot(builder, RecipeIngredientRole.INPUT, be.inputs).addItemStacks(List.of(b.getItems())));
		for (int i = 0; i < Math.min(recipe.results().size(), be.outputs); i++) {
			IRecipeSlotBuilder slot = slot(builder, RecipeIngredientRole.OUTPUT, be.outputStart + i).addItemStack(recipe.results().get(i));
			float chance = i < recipe.chances().size() ? recipe.chances().get(i) : 1;
			if (chance != 1) slot.addRichTooltipCallback((view, tooltip) -> tooltip.add(
					Component.translatable("techguns.oredrill.jei.chance").append(": " + String.format("%.2f", chance * 100) + "%")));
		}
	}

	@Override
	protected void drawGui(RecipeHolder<MachineRecipe> recipe, GuiGraphics g, float p) {
		progress.draw(g, texture, p);
	}

	/** Adds the input and output tanks. */
	static class ChemLab extends MachineCategory {
		private final IDrawable scale;

		ChemLab(IGuiHelper gui, Block block, Rect2i clickArea, Progress progress) {
			super(gui, block, clickArea, progress);
			scale = gui.createDrawable(texture, 177, 32, 10, 50);
		}

		@Override
		public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<MachineRecipe> holder, IFocusGroup focuses) {
			super.setRecipe(builder, holder, focuses);
			MachineRecipe recipe = holder.value();
			ChemLabBlockEntity lab = (ChemLabBlockEntity) be;
			recipe.fluid().ifPresent(f -> builder.addSlot(RecipeIngredientRole.INPUT, 19 - X, 17 - Y).setFluidRenderer(lab.inputTank.getCapacity(), false, 10, 50)
					.setOverlay(scale, 0, 0).addIngredients(NeoForgeTypes.FLUID_STACK, List.of(f.getFluids())));
			recipe.fluidResult().ifPresent(f -> builder.addSlot(RecipeIngredientRole.OUTPUT, 158 - X, 17 - Y).setFluidRenderer(lab.outputTank.getCapacity(), false, 10, 50)
					.setOverlay(scale, 0, 0).addIngredient(NeoForgeTypes.FLUID_STACK, f));
		}
	}
}
