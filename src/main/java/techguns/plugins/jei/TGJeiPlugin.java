package techguns.plugins.jei;

import java.util.Collection;
import java.util.List;
import java.util.function.UnaryOperator;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.handlers.IGuiClickableArea;
import mezz.jei.api.gui.handlers.IGuiContainerHandler;
import mezz.jei.api.gui.ingredient.ICraftingGridHelper;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.category.extensions.vanilla.crafting.ICraftingCategoryExtension;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.IVanillaCategoryExtensionRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import techguns.TGBlocks;
import techguns.TGComponents;
import techguns.Techguns;
import techguns.client.machines.MachineScreen;
import techguns.items.guns.GenericGunMeleeCharge;
import techguns.recipes.AmmoChangeRecipe;
import techguns.recipes.MiningToolUpgradeRecipe;

/**
 * 1.12 TGJeiPlugin: a category per machine (see {@link #categories}), the machines as catalysts, recipe-click areas on
 * their screens, and the ammo change and mining tool upgrade crafting recipes with their real results. Only JEI loads
 * this class, so the mod runs without it.
 */
@JeiPlugin
public class TGJeiPlugin implements IModPlugin {
	private List<TGCategory<?>> categories = List.of();

	@Override
	public ResourceLocation getPluginUid() {
		return Techguns.id("jei");
	}

	/** Every TG category; a new machine adds one line here. Click areas are the 1.12 ones, in GUI coordinates. */
	private static List<TGCategory<?>> categories(IGuiHelper gui) {
		MachineCategory.Progress press = (g, t, p) -> g.blit(t, 119, 36, 176, 0, 19, (int) (p * 21) + 1);
		return List.of(
				new MachineCategory(gui, TGBlocks.AMMO_PRESS.get(), new Rect2i(119, 36, 19, 22), press),
				new MachineCategory(gui, TGBlocks.METAL_PRESS.get(), new Rect2i(119, 36, 19, 22), press),
				new MachineCategory(gui, TGBlocks.GRINDER.get(), new Rect2i(30, 38, 43, 23), (g, t, p) -> {
					int i1 = (int) (p * 21), i2 = 20 - i1;
					g.blit(t, 31, 39, 0, 167, i1 + 1, 22);
					g.blit(t, 51 + i2, 39, i2, 167, i1 + 1, 22);
				}),
				new MachineCategory(gui, TGBlocks.BLAST_FURNACE.get(), new Rect2i(18, 52, 90, 12),
						(g, t, p) -> g.blit(t, 19, 53, 0, 167, (int) (p * 90) + 1, 11)),
				new MachineCategory(gui, TGBlocks.CHARGING_STATION.get(), new Rect2i(38, 18, 28, 12),
						(g, t, p) -> g.blit(t, 39, 19, 0, 167, (int) (p * 26) + 1, 10)),
				new MachineCategory.ChemLab(gui, TGBlocks.CHEM_LAB.get(), new Rect2i(80, 15, 52, 30), (g, t, p) -> {
					g.blit(t, 81, 19, 178, 5, 8, (int) (Math.min(p * 5, 1) * 25));
					if (p >= 0.2f) g.blit(t, 88, 39, 186, 25, (int) (Math.min((p - 0.2f) * 5, 1) * 9), 1);
					if (p >= 0.4f) {
						int i3 = (int) (Math.min((p - 0.4f) * 5, 1) * 20);
						g.blit(t, 97, 23 + 20 - i3, 194, 9 + 20 - i3, 14, i3);
					}
					if (p >= 0.6f) g.blit(t, 108, 17, 205, 3, (int) (Math.min((p - 0.6f) * 5, 1) * 16), 6);
					if (p >= 0.8f) g.blit(t, 117, 20, 214, 6, 14, (int) ((p - 0.8f) * 5 * 24) + 1);
				}));
	}

	@Override
	public void registerCategories(IRecipeCategoryRegistration registration) {
		categories = categories(registration.getJeiHelpers().getGuiHelper());
		categories.forEach(registration::addRecipeCategories);
	}

	@Override
	public void registerRecipes(IRecipeRegistration registration) {
		categories.forEach(c -> add(registration, c));
	}

	private static <T> void add(IRecipeRegistration registration, TGCategory<T> category) {
		registration.addRecipes(category.getRecipeType(), category.recipes(Minecraft.getInstance().level.getRecipeManager()));
	}

	@Override
	public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
		categories.forEach(c -> registration.addRecipeCatalyst(c.block, c.getRecipeType()));
	}

	@Override
	public void registerGuiHandlers(IGuiHandlerRegistration registration) {
		registration.addGuiContainerHandler(MachineScreen.class, new IGuiContainerHandler<>() {
			@Override
			public Collection<IGuiClickableArea> getGuiClickableAreas(MachineScreen screen, double mouseX, double mouseY) {
				return categories.stream().filter(c -> screen.getMenu().be.getBlockState().is(c.block)).map(c -> IGuiClickableArea.createBasic(
						c.clickArea.getX(), c.clickArea.getY(), c.clickArea.getWidth(), c.clickArea.getHeight(), c.getRecipeType())).toList();
			}
		});
	}

	@Override
	public void registerVanillaCategoryExtensions(IVanillaCategoryExtensionRegistration registration) {
		registration.getCraftingCategory().addExtension(AmmoChangeRecipe.class, new Assembled<>(in -> in));
		registration.getCraftingCategory().addExtension(MiningToolUpgradeRecipe.class, new Assembled<>(TGJeiPlugin::previousHead));
	}

	/** The tool fitted with the head before the one in the grid, which the upgrade needs. */
	private static List<ItemStack> previousHead(List<ItemStack> in) {
		ItemStack head = in.stream().filter(s -> !(s.getItem() instanceof GenericGunMeleeCharge)).findFirst().orElse(ItemStack.EMPTY);
		return in.stream().map(s -> {
			if (!(s.getItem() instanceof GenericGunMeleeCharge tool)) return s;
			ItemStack withHead = s.copy();
			withHead.set(TGComponents.MINING_HEAD, tool.getMiningHeadLevelForHead(head) - 1);
			return withHead;
		}).toList();
	}

	/**
	 * A shapeless recipe whose result depends on the grid (1.12 AmmoSwitchRecipeWrapper, MiningToolUpgradeRecipeWrapper):
	 * shows the first item of each ingredient, changed by {@code inputs}, and what the recipe assembles from them.
	 */
	private record Assembled<R extends CraftingRecipe>(UnaryOperator<List<ItemStack>> inputs) implements ICraftingCategoryExtension<R> {
		@Override
		public void setRecipe(RecipeHolder<R> holder, IRecipeLayoutBuilder builder, ICraftingGridHelper grid, IFocusGroup focuses) {
			List<ItemStack> in = inputs.apply(holder.value().getIngredients().stream()
					.map(i -> i.isEmpty() ? ItemStack.EMPTY : i.getItems()[0]).toList());
			ItemStack out = holder.value().assemble(CraftingInput.of(in.size(), 1, in), Minecraft.getInstance().level.registryAccess());
			grid.createAndSetInputs(builder, VanillaTypes.ITEM_STACK, in.stream().map(List::of).toList(), 0, 0);
			grid.createAndSetOutputs(builder, VanillaTypes.ITEM_STACK, List.of(out));
		}
	}
}
