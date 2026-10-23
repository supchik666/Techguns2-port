package techguns.items.additionalslots;

import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import techguns.capabilities.TGExtendedPlayer;

/** Filters out poison and wither, one charge each time. */
public class ItemGasMask extends ItemTGSpecialSlot {
	/** Radiation resistance while worn in the face slot, see {@link techguns.items.armors.ArmorBonuses}. */
	public float radresist;

	public ItemGasMask(int camoCount, int durability) {
		super(TGSlotType.FACE, camoCount, durability);
	}

	public ItemGasMask setRadresist(float radresist) {
		this.radresist = radresist;
		return this;
	}

	@Override
	public void onPlayerTick(ItemStack stack, Player player, TGExtendedPlayer tg) {
		filter(stack, player);
	}

	static void filter(ItemStack stack, Player player) {
		if (player.hasEffect(MobEffects.POISON) && hasCharge(stack)) {
			player.removeEffect(MobEffects.POISON);
			drain(stack, 1);
		}
		if (player.hasEffect(MobEffects.WITHER) && hasCharge(stack)) {
			player.removeEffect(MobEffects.WITHER);
			drain(stack, 1);
		}
	}
}
