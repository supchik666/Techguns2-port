package techguns.items.armors;

import java.util.List;
import java.util.function.Supplier;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.TooltipFlag;
import techguns.damagesystem.ShieldStats;

/**
 * 1.12 GenericShield: a shield that stops TG damage by its {@link ShieldStats}, drawn by the client shield renderer with
 * the camo the camo bench set (the CAMO component). The repair bench fixes it with its repair item.
 */
public class GenericShield extends ShieldItem {
	public final ShieldStats stats;
	private final Supplier<? extends Item> repair;
	public final int camoCount;

	public GenericShield(int durability, ShieldStats stats, Supplier<? extends Item> repair, int camoCount) {
		super(new Item.Properties().durability(durability));
		this.stats = stats;
		this.repair = repair;
		this.camoCount = camoCount;
	}

	public Item repairItem() {
		return repair.get();
	}

	@Override
	public boolean isValidRepairItem(ItemStack stack, ItemStack repairCandidate) {
		return repairCandidate.is(repair.get());
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
		if (!Screen.hasShiftDown()) {
			GenericArmor.shiftHint(tooltip);
			return;
		}
		tooltip.add(GenericArmor.line("durability", (stack.getMaxDamage() - stack.getDamageValue()) + "/" + stack.getMaxDamage()));
		GenericArmor.resistances(tooltip, type -> stats.rate(type) * 100, "%");
	}
}
