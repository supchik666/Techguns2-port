package techguns.items.tools;

import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.common.SimpleTier;

public final class TGTiers {
	/** 1.12 TG_STEEL: harvest level 2 (iron), 1000 uses, speed 7.5, +2.5 damage, enchantability 12, no repair item. */
	public static final Tier STEEL = new SimpleTier(BlockTags.INCORRECT_FOR_IRON_TOOL, 1000, 7.5f, 2.5f, 12, () -> Ingredient.EMPTY);

	private TGTiers() {}
}
