package techguns.items.tools;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.level.LevelReader;

/**
 * A steel pickaxe that lets sneak-clicks reach the block it's used on (1.12 TGCrowbar).
 * ponytail: the 1.12 "wrench" tool class had no users; its TG damage type isn't applied (gore is, in DeathEffects).
 */
public class Crowbar extends PickaxeItem {
	public Crowbar(Properties properties) {
		super(TGTiers.STEEL, properties);
	}

	@Override
	public boolean doesSneakBypassUse(ItemStack stack, LevelReader level, BlockPos pos, Player player) {
		return true;
	}
}
