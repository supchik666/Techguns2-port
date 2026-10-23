package techguns.items.additionalslots;

import java.util.function.Supplier;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import techguns.TGItems;
import techguns.capabilities.TGExtendedPlayer;

/** Night vision while toggled on (TGPlayerEvents applies the effect), one charge a tick. */
public class ItemNightVisionGoggles extends ItemTGSpecialSlotAmmo {
	public ItemNightVisionGoggles(int camoCount, int durability) {
		this(camoCount, durability, TGItems.REDSTONE_BATTERY, TGItems.REDSTONE_BATTERY_EMPTY);
	}

	protected ItemNightVisionGoggles(int camoCount, int durability, Supplier<? extends Item> ammo, Supplier<? extends Item> ammoEmpty) {
		super(TGSlotType.FACE, camoCount, durability, ammo, ammoEmpty);
	}

	@Override
	public void onPlayerTick(ItemStack stack, Player player, TGExtendedPlayer tg) {
		if (!hasCharge(stack)) tryReloadAndRepair(stack, player);
		else if (tg.enableNightVision) drain(stack, 1);
	}

	/** 1.12 TGArmorBonus.NIGHTVISION from the face slot. */
	public static boolean givesNightVision(ItemStack face) {
		return face.getItem() instanceof ItemNightVisionGoggles && hasCharge(face);
	}
}
