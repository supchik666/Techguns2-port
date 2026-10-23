package techguns.items.additionalslots;

import java.util.List;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import techguns.TGItems;
import techguns.capabilities.TGExtendedPlayer;

/** Jump boost and softer landings; the jump and fall events in TGPlayerEvents use these values. */
public class ItemJumpPack extends ItemTGSpecialSlotAmmo {
	public static final float JUMPBOOST = 0.8f;
	public static final float FREEHEIGHT = 8.0f;
	public static final float FALLDMG = 0.2f;

	public ItemJumpPack(int camoCount, int durability) {
		super(TGSlotType.BACK, camoCount, durability, TGItems.COMPRESSED_AIR_TANK, TGItems.COMPRESSED_AIR_TANK_EMPTY);
	}

	@Override
	public void onPlayerTick(ItemStack stack, Player player, TGExtendedPlayer tg) {
		if (!hasCharge(stack)) tryReloadAndRepair(stack, player);
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
		super.appendHoverText(stack, context, tooltip, flag);
		tooltip.add(Component.translatable("techguns.armorTooltip.jumpheight").append(": +" + JUMPBOOST));
		tooltip.add(Component.translatable("techguns.armorTooltip.falldamage").append(": -" + FALLDMG * 100.0f + "%"));
		tooltip.add(Component.translatable("techguns.armorTooltip.fallheight").append(": -" + FREEHEIGHT));
	}
}
