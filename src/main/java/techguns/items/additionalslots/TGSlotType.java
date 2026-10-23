package techguns.items.additionalslots;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import techguns.Techguns;

/** The extra player slots (1.12 api.tginventory.TGSlotType, player part); names are the tooltip lang suffixes. */
public enum TGSlotType {
	FACE("face"), BACK("back"), HAND("hands"), AMMO("ammo"), FOOD("food"), HEAL("healing");

	/** Items the ammo slots take (1.12 shared items flagged AMMOSLOT); written by port_assets.py. */
	public static final TagKey<Item> AMMO_SLOT = ItemTags.create(Techguns.id("ammo_slot"));

	private final String name;

	TGSlotType(String name) {
		this.name = name;
	}

	public Component displayName() {
		return Component.translatable("techguns.tooltip.tgslottype." + name);
	}

	/** ponytail: nothing goes in the heal slot until the medkits are ported. */
	public boolean accepts(ItemStack stack) {
		return switch (this) {
			case FACE, BACK, HAND -> stack.getItem() instanceof ItemTGSpecialSlot item && item.slot == this;
			case AMMO -> stack.is(AMMO_SLOT);
			case FOOD -> stack.has(DataComponents.FOOD);
			case HEAL -> false;
		};
	}
}
