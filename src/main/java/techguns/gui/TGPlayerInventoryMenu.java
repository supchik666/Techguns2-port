package techguns.gui;

import com.mojang.datafixers.util.Pair;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import techguns.TGMenus;
import techguns.capabilities.TGExtendedPlayer;
import techguns.items.additionalslots.TGSlotType;

/** The Techguns inventory tab (1.12 TGPlayerInventoryContainer): the 15 extra slots, armor, inventory and offhand. */
public class TGPlayerInventoryMenu extends AbstractContainerMenu {
	public static final int ARMOR_START = TGExtendedPlayer.NUMSLOTS;
	public static final int INV_START = ARMOR_START + 4;
	public static final int HOTBAR_START = INV_START + 27;
	public static final int OFFHAND = HOTBAR_START + 9;

	private static final EquipmentSlot[] ARMOR = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
	private static final ResourceLocation[] ARMOR_ICONS = {InventoryMenu.EMPTY_ARMOR_SLOT_HELMET, InventoryMenu.EMPTY_ARMOR_SLOT_CHESTPLATE,
			InventoryMenu.EMPTY_ARMOR_SLOT_LEGGINGS, InventoryMenu.EMPTY_ARMOR_SLOT_BOOTS};

	public TGPlayerInventoryMenu(int id, Player player) {
		super(TGMenus.PLAYER_INVENTORY.get(), id);
		Container tg = TGExtendedPlayer.get(player).inventory;
		Inventory inv = player.getInventory();

		addSlot(new TGSlot(tg, TGExtendedPlayer.SLOT_FACE, 77, 8, TGSlotType.FACE));
		addSlot(new TGSlot(tg, TGExtendedPlayer.SLOT_BACK, 77, 26, TGSlotType.BACK));
		addSlot(new TGSlot(tg, TGExtendedPlayer.SLOT_HAND, 77, 44, TGSlotType.HAND));
		for (int i = 0; i < 3; i++) addSlot(new TGSlot(tg, TGExtendedPlayer.SLOTS_AUTOFOOD_START + i, 116 + i * 18, 24, TGSlotType.FOOD));
		addSlot(new TGSlot(tg, TGExtendedPlayer.SLOT_AUTOHEAL, 97, 24, TGSlotType.HEAL));
		for (int i = 0; i < 2; i++) {
			for (int j = 0; j < 4; j++) addSlot(new TGSlot(tg, TGExtendedPlayer.SLOTS_AMMO_START + i * 4 + j, 98 + j * 18, 44 + i * 18, TGSlotType.AMMO));
		}

		for (int k = 0; k < 4; k++) {
			EquipmentSlot equipmentSlot = ARMOR[k];
			ResourceLocation icon = ARMOR_ICONS[k];
			// vanilla ArmorSlot is package private
			addSlot(new Slot(inv, 39 - k, 8, 8 + k * 18) {
				@Override
				public void setByPlayer(ItemStack newStack, ItemStack oldStack) {
					player.onEquipItem(equipmentSlot, oldStack, newStack);
					super.setByPlayer(newStack, oldStack);
				}

				@Override
				public int getMaxStackSize() {
					return 1;
				}

				@Override
				public boolean mayPlace(ItemStack stack) {
					return stack.canEquip(equipmentSlot, player);
				}

				@Override
				public boolean mayPickup(Player p) {
					ItemStack stack = getItem();
					return (stack.isEmpty() || p.isCreative() || !EnchantmentHelper.has(stack, EnchantmentEffectComponents.PREVENT_ARMOR_CHANGE)) && super.mayPickup(p);
				}

				@Override
				public Pair<ResourceLocation, ResourceLocation> getNoItemIcon() {
					return Pair.of(InventoryMenu.BLOCK_ATLAS, icon);
				}
			});
		}

		for (int i = 0; i < 3; i++) {
			for (int j = 0; j < 9; j++) addSlot(new Slot(inv, j + (i + 1) * 9, 8 + j * 18, 84 + i * 18));
		}
		for (int i = 0; i < 9; i++) addSlot(new Slot(inv, i, 8 + i * 18, 142));
		addSlot(new Slot(inv, Inventory.SLOT_OFFHAND, 77, 62) {
			@Override
			public Pair<ResourceLocation, ResourceLocation> getNoItemIcon() {
				return Pair.of(InventoryMenu.BLOCK_ATLAS, InventoryMenu.EMPTY_ARMOR_SLOT_SHIELD);
			}
		});
	}

	public static class TGSlot extends Slot {
		public final TGSlotType type;

		TGSlot(Container container, int slot, int x, int y, TGSlotType type) {
			super(container, slot, x, y);
			this.type = type;
		}

		@Override
		public boolean mayPlace(ItemStack stack) {
			return type.accepts(stack);
		}
	}

	@Override
	public boolean stillValid(Player player) {
		return true;
	}

	/** Shift click: extra slots and armor to the inventory; from the inventory to armor, then the extra slots, then hotbar/main. */
	@Override
	public ItemStack quickMoveStack(Player player, int index) {
		Slot slot = slots.get(index);
		if (!slot.hasItem()) return ItemStack.EMPTY;
		ItemStack stack = slot.getItem();
		ItemStack copy = stack.copy();
		EquipmentSlot equipmentSlot = player.getEquipmentSlotForItem(stack);
		int armorSlot = ARMOR_START + 3 - equipmentSlot.getIndex();
		if (index < INV_START) {
			if (!moveItemStackTo(stack, INV_START, OFFHAND, false)) return ItemStack.EMPTY;
		} else if (equipmentSlot.getType() == EquipmentSlot.Type.HUMANOID_ARMOR && !slots.get(armorSlot).hasItem()) {
			if (!moveItemStackTo(stack, armorSlot, armorSlot + 1, false)) return ItemStack.EMPTY;
		} else if (!moveItemStackTo(stack, 0, ARMOR_START, false)) {
			boolean main = index < HOTBAR_START;
			if (!moveItemStackTo(stack, main ? HOTBAR_START : INV_START, main ? OFFHAND : HOTBAR_START, false)) return ItemStack.EMPTY;
		}
		if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
		else slot.setChanged();
		if (stack.getCount() == copy.getCount()) return ItemStack.EMPTY;
		slot.onTake(player, stack);
		return copy;
	}
}
