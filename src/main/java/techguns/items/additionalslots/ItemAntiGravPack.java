package techguns.items.additionalslots;

import java.util.List;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import techguns.TGItems;
import techguns.capabilities.TGExtendedPlayer;

/** Creative flight (TGPlayerEvents grants it) a quarter faster, one charge per tick flown. */
public class ItemAntiGravPack extends ItemTGSpecialSlotAmmo {
	public static final float FLYBONUS = 0.25f;

	public ItemAntiGravPack(int camoCount, int durability) {
		super(TGSlotType.BACK, camoCount, durability, TGItems.NUCLEAR_POWERCELL, TGItems.NUCLEAR_POWERCELL_EMPTY);
	}

	@Override
	public void onPlayerTick(ItemStack stack, Player player, TGExtendedPlayer tg) {
		if (stack.getDamageValue() + 1 >= stack.getMaxDamage()) tryReloadAndRepair(stack, player);
		if (player.getAbilities().flying && !player.getAbilities().instabuild) drain(stack, 1);
	}

	public static boolean givesFlight(ItemStack back) {
		return back.getItem() instanceof ItemAntiGravPack && hasCharge(back);
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
		super.appendHoverText(stack, context, tooltip, flag);
		tooltip.add(Component.translatable("techguns.armorTooltip.creativeFlight"));
		tooltip.add(Component.translatable("techguns.armorTooltip.flyspeed").append(": +" + FLYBONUS * 100.0f + "%"));
	}
}
