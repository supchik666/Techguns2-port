package techguns.items.additionalslots;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import techguns.TGItems;
import techguns.capabilities.TGExtendedPlayer;

/** Night vision goggles, gas mask and oxygen mask in one, on energy cells. */
public class ItemTacticalMask extends ItemNightVisionGoggles {
	public ItemTacticalMask(int camoCount, int durability) {
		super(camoCount, durability, TGItems.ENERGY_CELL, TGItems.ENERGY_CELL_EMPTY);
	}

	@Override
	public void onPlayerTick(ItemStack stack, Player player, TGExtendedPlayer tg) {
		super.onPlayerTick(stack, player, tg);
		ItemGasMask.filter(stack, player);
	}
}
