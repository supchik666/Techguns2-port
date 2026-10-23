package techguns.items.additionalslots;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import techguns.TGItems;
import techguns.capabilities.TGExtendedPlayer;
import techguns.items.armors.GenericArmor;
import techguns.items.armors.TGArmorBonus;

/** Refills air under water, if the face slot holds oxygen gear. */
public class ItemScubaTanks extends ItemTGSpecialSlotAmmo {
	public ItemScubaTanks(int camoCount, int durability) {
		super(TGSlotType.BACK, camoCount, durability, TGItems.COMPRESSED_AIR_TANK, TGItems.COMPRESSED_AIR_TANK_EMPTY);
	}

	@Override
	public void onPlayerTick(ItemStack stack, Player player, TGExtendedPlayer tg) {
		if (!hasCharge(stack)) tryReloadAndRepair(stack, player);
		if (hasCharge(stack) && player.getAirSupply() <= 280 && isOxygenGear(player, tg.getSlot(TGExtendedPlayer.SLOT_FACE))) {
			player.setAirSupply(player.getAirSupply() + 20);
			drain(stack, 1);
		}
	}

	/** 1.12 TGArmorBonus.OXYGEN_GEAR, from the face slot or the armor. */
	public static boolean isOxygenGear(Player player, ItemStack face) {
		return face.is(TGItems.OXYGEN_MASK) || face.getItem() instanceof ItemTacticalMask && hasCharge(face)
				|| GenericArmor.bonus(player, TGArmorBonus.OXYGEN_GEAR, false) > 0;
	}
}
