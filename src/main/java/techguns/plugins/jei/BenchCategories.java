package techguns.plugins.jei;

import java.util.List;
import java.util.function.Supplier;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import techguns.Techguns;
import techguns.items.armors.GenericArmor;
import techguns.machines.CamoBenchBlockEntity;
import techguns.TGConfig;
import techguns.recipes.UpgradeBenchRecipe;

/** The benches' categories (1.12 CamoBenchJeiRecipeCategory, UpgradeBenchJeiRecipeCategory); no power or duration. */
final class BenchCategories {
	private BenchCategories() {}

	/** A block group the camo bench cycles through ({@link CamoBenchBlockEntity#GROUPS}): every block of it, both ways. */
	static class Camo extends TGCategory<List<Supplier<? extends ItemLike>>> {
		@SuppressWarnings({"unchecked", "rawtypes"})
		Camo(IGuiHelper gui, Block block) {
			super(gui, RecipeType.create(Techguns.MODID, "camo_bench", (Class) List.class), block.getName(), block, null);
		}

		@Override
		public List<List<Supplier<? extends ItemLike>>> recipes(RecipeManager recipes) {
			return CamoBenchBlockEntity.GROUPS;
		}

		@Override
		protected int power(List<Supplier<? extends ItemLike>> group) {
			return 0;
		}

		@Override
		protected int time(List<Supplier<? extends ItemLike>> group) {
			return 0;
		}

		@Override
		public void setRecipe(IRecipeLayoutBuilder builder, List<Supplier<? extends ItemLike>> group, IFocusGroup focuses) {
			List<ItemStack> stacks = group.stream().map(s -> new ItemStack(s.get())).toList();
			builder.addSlot(RecipeIngredientRole.INPUT, 17 - X, 18 - Y).addItemStacks(stacks);
			builder.addInvisibleIngredients(RecipeIngredientRole.OUTPUT).addItemStacks(stacks);
		}
	}

	/** Every TG armor with the upgrade's enchantment, and its XP cost from no enchantment. */
	static class Upgrade extends TGCategory<RecipeHolder<UpgradeBenchRecipe>> {
		Upgrade(IGuiHelper gui, Block block, Rect2i clickArea) {
			super(gui, RecipeType.createFromVanilla(UpgradeBenchRecipe.TYPE.get()), block.getName(), block, clickArea);
		}

		@Override
		public List<RecipeHolder<UpgradeBenchRecipe>> recipes(RecipeManager recipes) {
			return recipes.getAllRecipesFor(UpgradeBenchRecipe.TYPE.get());
		}

		@Override
		protected int power(RecipeHolder<UpgradeBenchRecipe> recipe) {
			return 0;
		}

		@Override
		protected int time(RecipeHolder<UpgradeBenchRecipe> recipe) {
			return 0;
		}

		@Override
		public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<UpgradeBenchRecipe> holder, IFocusGroup focuses) {
			UpgradeBenchRecipe r = holder.value();
			List<ItemStack> armors = BuiltInRegistries.ITEM.stream().filter(i -> i instanceof GenericArmor).map(ItemStack::new).toList();
			List<ItemStack> upgraded = armors.stream().map(a -> {
				ItemStack out = a.copy();
				EnchantmentHelper.updateEnchantments(out, e -> e.set(r.enchantment(), r.level()));
				return out;
			}).toList();
			IRecipeSlotBuilder in = builder.addSlot(RecipeIngredientRole.INPUT, 17 - X, 47 - Y).addItemStacks(armors);
			builder.addSlot(RecipeIngredientRole.INPUT, 53 - X, 47 - Y).addIngredients(r.upgrade());
			IRecipeSlotBuilder out = builder.addSlot(RecipeIngredientRole.OUTPUT, 116 - X, 47 - Y).addItemStacks(upgraded);
			builder.createFocusLink(in, out);
		}

		@Override
		protected void drawGui(RecipeHolder<UpgradeBenchRecipe> recipe, GuiGraphics g, float p) {
			int cost = TGConfig.UPGRADE_XP_COST.get() * recipe.value().level();
			g.drawString(Minecraft.getInstance().font, Component.translatable("techguns.gui.xpcost").append(": " + cost), 95, 17, 8453920);
		}
	}
}
