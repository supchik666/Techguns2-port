package techguns.items.additionalslots;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import techguns.capabilities.TGExtendedPlayer;

/**
 * An item worn in the face, back or hand slot. Durability is its charge: at full damage it's empty, not broken, and
 * reloads from the inventory if it takes ammo ({@link ItemTGSpecialSlotAmmo}).
 */
public class ItemTGSpecialSlot extends Item {
	final TGSlotType slot;
	/** Textures it can be drawn with (1.12 ICamoChangeable). */
	public final int camoCount;

	public ItemTGSpecialSlot(TGSlotType slot, int camoCount, int durability) {
		super(durability > 0 ? new Item.Properties().durability(durability) : new Item.Properties().stacksTo(1));
		this.slot = slot;
		this.camoCount = camoCount;
	}

	/** Each tick while worn, on the server and the wearer's client. */
	public void onPlayerTick(ItemStack stack, Player player, TGExtendedPlayer tg) {}

	public static boolean hasCharge(ItemStack stack) {
		return !stack.isDamageableItem() || stack.getDamageValue() < stack.getMaxDamage();
	}

	/** Uses {@code amount} charge; stops at empty. */
	public static void drain(ItemStack stack, int amount) {
		stack.setDamageValue(stack.getDamageValue() + amount);
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
		tooltip.add(Component.translatable("techguns.tooltip.slot").append(": ").append(slot.displayName()).withStyle(ChatFormatting.GRAY));
	}

	/** Right click wears it if its slot is free. */
	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		TGExtendedPlayer tg = TGExtendedPlayer.get(player);
		int index = slot.ordinal(); // FACE, BACK, HAND are slots 0-2
		if (!tg.getSlot(index).isEmpty()) return InteractionResultHolder.pass(stack);
		tg.inventory.setItem(index, stack.copyAndClear());
		return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
	}
}
