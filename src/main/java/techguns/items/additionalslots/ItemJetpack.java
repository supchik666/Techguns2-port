package techguns.items.additionalslots;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import techguns.TGItems;
import techguns.capabilities.TGExtendedPlayer;

/** Boosts up while the jump key is held ({@link TGExtendedPlayer#isJumpkeyPressed}, set by the client), and glides. */
public class ItemJetpack extends ItemTGSpecialSlotAmmo {
	public ItemJetpack(int camoCount, int durability) {
		super(TGSlotType.BACK, camoCount, durability, TGItems.FUEL_TANK, TGItems.FUEL_TANK_EMPTY);
	}

	@Override
	public void onPlayerTick(ItemStack stack, Player player, TGExtendedPlayer tg) {
		if (!hasCharge(stack)) {
			tryReloadAndRepair(stack, player);
			return;
		}
		if (tg.isJumpkeyPressed) {
			Vec3 m = player.getDeltaMovement();
			player.setDeltaMovement(m.x, Math.min(m.y + 0.15, 0.4), m.z);
			player.fallDistance = 0;
			drain(stack, 1);
		}
		ItemGlider.glide(player, tg);
	}
}
