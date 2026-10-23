package techguns.recipes;

import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.level.Level;
import techguns.TGComponents;
import techguns.items.guns.GenericGunMeleeCharge;

/**
 * Mining tool + its next upgrade head: the same tool with that head fitted (1.12 MiningToolUpgradeHeadRecipe). Heads go
 * in order, so the tool must have the head before this one. The json is a plain shapeless recipe.
 */
public class MiningToolUpgradeRecipe extends ShapelessRecipe {
	MiningToolUpgradeRecipe(ShapelessRecipe recipe) {
		super(recipe.getGroup(), recipe.category(), recipe.getResultItem(null), recipe.getIngredients());
	}

	/** The head's level, or -1 if the grid isn't a mining tool and the next head for it. */
	private static int newHead(CraftingInput input) {
		ItemStack tool = ItemStack.EMPTY;
		ItemStack head = ItemStack.EMPTY;
		for (ItemStack stack : input.items()) {
			if (stack.getItem() instanceof GenericGunMeleeCharge) tool = stack;
			else if (!stack.isEmpty()) head = stack;
		}
		if (!(tool.getItem() instanceof GenericGunMeleeCharge item)) return -1;
		int level = item.getMiningHeadLevelForHead(head);
		return level == item.getMiningHead(tool) + 1 ? level : -1;
	}

	@Override
	public boolean matches(CraftingInput input, Level level) {
		return super.matches(input, level) && newHead(input) > 0;
	}

	@Override
	public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
		for (ItemStack stack : input.items()) {
			if (stack.getItem() instanceof GenericGunMeleeCharge) {
				ItemStack out = stack.copyWithCount(1);
				out.set(TGComponents.MINING_HEAD, newHead(input));
				return out;
			}
		}
		return super.assemble(input, registries);
	}

	@Override
	public RecipeSerializer<?> getSerializer() {
		return AmmoChangeRecipe.MININGTOOL_UPGRADE.get();
	}
}
