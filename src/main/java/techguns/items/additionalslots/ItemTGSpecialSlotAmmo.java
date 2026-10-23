package techguns.items.additionalslots;

import java.util.List;
import java.util.function.Supplier;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import techguns.capabilities.TGExtendedPlayer;

/** A slot item that refills from {@code ammo} (a fuel tank, a battery), giving back {@code ammoEmpty}. */
public class ItemTGSpecialSlotAmmo extends ItemTGSpecialSlot {
	private final Supplier<? extends Item> ammo, ammoEmpty;

	public ItemTGSpecialSlotAmmo(TGSlotType slot, int camoCount, int durability, Supplier<? extends Item> ammo, Supplier<? extends Item> ammoEmpty) {
		super(slot, camoCount, durability);
		this.ammo = ammo;
		this.ammoEmpty = ammoEmpty;
	}

	public Item getAmmo() {
		return ammo.get();
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
		super.appendHoverText(stack, context, tooltip, flag);
		tooltip.add(Component.translatable("techguns.tooltip.reloads_with").append(": ").append(ammo.get().getDescription()).withStyle(ChatFormatting.AQUA));
	}

	/** Server only; the client gets the result synced. */
	protected void tryReloadAndRepair(ItemStack stack, Player player) {
		if (player.level().isClientSide || !TGExtendedPlayer.consumeAmmo(player, ammo.get())) return;
		stack.setDamageValue(0);
		TGExtendedPlayer.giveAmmo(player, new ItemStack(ammoEmpty.get()));
	}
}
