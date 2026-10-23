package techguns.items.additionalslots;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import techguns.capabilities.TGExtendedPlayer;

public class ItemGlider extends ItemTGSpecialSlot {
	public ItemGlider(int camoCount) {
		super(TGSlotType.BACK, camoCount, 0);
	}

	@Override
	public void onPlayerTick(ItemStack stack, Player player, TGExtendedPlayer tg) {
		glide(player, tg);
	}

	/** Sneaking in the air slows the fall and pushes along the look (1.12, from Modular Powersuits). Motion only counts on the client. */
	public static void glide(Player player, TGExtendedPlayer tg) {
		if (player.onGround() || !player.isShiftKeyDown()) return;
		Vec3 m = player.getDeltaMovement();
		if (m.y < -0.1) {
			tg.isGliding = true;
			Vec3 look = player.getLookAngle();
			double motion = Math.min(0.08, -0.1 - m.y);
			player.setDeltaMovement(m.add(look.x * motion, motion, look.z * motion));
		}
		player.fallDistance = 0;
	}
}
