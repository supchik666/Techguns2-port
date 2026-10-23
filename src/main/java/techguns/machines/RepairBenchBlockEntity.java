package techguns.machines;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import com.mojang.datafixers.util.Pair;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import techguns.TGBlockEntities;
import techguns.TGItems;
import techguns.items.armors.GenericArmor;
import techguns.items.armors.GenericShield;

/**
 * 1.12 RepairBenchTileEnt: materials in slots 0-8 fully repair the worn armor, the offhand item or the TG armor or
 * shield in slot 9. Each button repairs one; the materials needed scale with the damage (1.12 getRepairMats).
 * Guns have no durability, so there's nothing to repair on them.
 */
public class RepairBenchBlockEntity extends TGBlockEntity {
	public static final int SLOT_ITEM = 9;
	/** Buttons 3-6 repair the worn helmet..boots, 7 the offhand item, 8 slot 9. */
	public static final int BUTTON_ARMOR = 3, BUTTON_OFFHAND = 7, BUTTON_SLOT = 8;

	private static final EquipmentSlot[] ARMOR = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
	private static final ResourceLocation[] ARMOR_ICONS = {InventoryMenu.EMPTY_ARMOR_SLOT_HELMET, InventoryMenu.EMPTY_ARMOR_SLOT_CHESTPLATE,
			InventoryMenu.EMPTY_ARMOR_SLOT_LEGGINGS, InventoryMenu.EMPTY_ARMOR_SLOT_BOOTS};

	/** 1.12 TGArmors setRepairMats per armor material: metal, cloth, the metal share. Legs use a third metal where the set uses half. */
	private record Mats(Supplier<? extends Item> metal, Supplier<? extends Item> cloth, float ratio) {}

	private static final Map<String, Mats> MATS = Map.ofEntries(
			Map.entry("t1_combat", new Mats(() -> Items.IRON_INGOT, TGItems.HEAVY_CLOTH, 0.5f)),
			Map.entry("t1_scout", new Mats(null, TGItems.HEAVY_CLOTH, 0)),
			Map.entry("t1_miner", new Mats(() -> Items.IRON_INGOT, TGItems.HEAVY_CLOTH, 0.5f)),
			Map.entry("t1_steam", new Mats(TGItems.STEAMARMOR_PLATE, TGItems.STEAMARMOR_PLATE, 1)),
			Map.entry("t2_hazmat", new Mats(null, TGItems.PROTECTIVE_FIBER, 0)),
			Map.entry("t2_combat", new Mats(TGItems.INGOT_OBSIDIAN_STEEL, TGItems.HEAVY_CLOTH, 0.5f)),
			Map.entry("t2_commando", new Mats(TGItems.INGOT_OBSIDIAN_STEEL, TGItems.RUBBER_BAR, 0.5f)),
			Map.entry("t2_riot", new Mats(TGItems.INGOT_OBSIDIAN_STEEL, TGItems.TREATED_LEATHER, 0.5f)),
			Map.entry("t3_combat", new Mats(TGItems.PLATE_CARBON, TGItems.CARBON_FIBERS, 0.5f)),
			Map.entry("t3_power", new Mats(TGItems.POWER_ARMOR_PLATING, TGItems.WIRE_GOLD, 0.5f)),
			Map.entry("t3_miner", new Mats(TGItems.PLATE_CARBON, TGItems.CIRCUIT_BOARD_ELITE, 0.5f)),
			Map.entry("t3_exo", new Mats(TGItems.PLATE_CARBON, TGItems.CARBON_FIBERS, 0.5f)),
			Map.entry("t2_beret", new Mats(TGItems.HEAVY_CLOTH, TGItems.HEAVY_CLOTH, 1)),
			Map.entry("t4_praetor", new Mats(TGItems.PLATE_CARBON, TGItems.CIRCUIT_BOARD_ELITE, 0.5f)),
			Map.entry("t4_power", new Mats(TGItems.POWER_ARMOR_PLATING, TGItems.WIRE_GOLD, 0.5f)));

	public RepairBenchBlockEntity(BlockPos pos, BlockState state) {
		super(TGBlockEntities.REPAIR_BENCH.get(), pos, state, "repairbench", slots());
	}

	private static int[][] slots() {
		int[][] slots = new int[10][];
		for (int i = 0; i < 9; i++) slots[i] = new int[] {8 + i * 18, 57};
		slots[SLOT_ITEM] = new int[] {8, 18};
		return slots;
	}

	@Override
	public boolean canInsert(int slot, ItemStack stack) {
		return slot != SLOT_ITEM || stack.getItem() instanceof GenericArmor || stack.getItem() instanceof GenericShield;
	}

	/** The player's armor (helmet first) and offhand, as 1.12's container showed them. */
	@Override
	public void addMenuSlots(MachineMenu menu, Inventory inventory) {
		for (int k = 0; k < 4; k++) menu.addMenuSlot(armorSlot(inventory, k, 89 + k * 20, 18));
		menu.addMenuSlot(offhandSlot(inventory, 64, 18));
	}

	public static Slot offhandSlot(Inventory inventory, int x, int y) {
		return new Slot(inventory, Inventory.SLOT_OFFHAND, x, y) {
			@Override
			public Pair<ResourceLocation, ResourceLocation> getNoItemIcon() {
				return Pair.of(InventoryMenu.BLOCK_ATLAS, InventoryMenu.EMPTY_ARMOR_SLOT_SHIELD);
			}
		};
	}

	/** The player's armor slot {@code k} (0: helmet), as in {@link techguns.gui.TGPlayerInventoryMenu}; vanilla ArmorSlot is package private. */
	public static Slot armorSlot(Inventory inventory, int k, int x, int y) {
		Player player = inventory.player;
		EquipmentSlot equipmentSlot = ARMOR[k];
		return new Slot(inventory, 39 - k, x, y) {
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
			public Pair<ResourceLocation, ResourceLocation> getNoItemIcon() {
				return Pair.of(InventoryMenu.BLOCK_ATLAS, ARMOR_ICONS[k]);
			}
		};
	}

	/** The item button {@code id} repairs. */
	public ItemStack target(int id, Player player) {
		if (id >= BUTTON_ARMOR && id < BUTTON_OFFHAND) return player.getInventory().armor.get(3 - (id - BUTTON_ARMOR));
		if (id == BUTTON_OFFHAND) return player.getOffhandItem();
		if (id == BUTTON_SLOT) return inventory.getStackInSlot(SLOT_ITEM);
		return ItemStack.EMPTY;
	}

	@Override
	public void buttonClicked(int id, Player player) {
		if (id >= BUTTON_ARMOR && id <= BUTTON_SLOT) repair(target(id, player));
		else super.buttonClicked(id, player);
	}

	/** Whether the bench can repair {@code stack} at all. */
	public static boolean canRepair(ItemStack stack) {
		return stack.getItem() instanceof GenericShield || stack.getItem() instanceof GenericArmor armor && MATS.containsKey(material(armor));
	}

	private static String material(GenericArmor armor) {
		return armor.tgMaterial.material.unwrapKey().orElseThrow().location().getPath();
	}

	/** 1.12 getRepairMats: what fully repairing {@code stack} takes; empty if undamaged or not repairable. */
	public static List<ItemStack> repairMaterials(ItemStack stack) {
		List<ItemStack> mats = new ArrayList<>();
		if (!stack.isDamaged() || !canRepair(stack)) return mats;
		float damage = stack.getDamageValue() / (stack.getMaxDamage() - 1f);
		if (stack.getItem() instanceof GenericShield shield) {
			mats.add(new ItemStack(shield.repairItem(), (int) Math.ceil(3 * damage)));
			return mats;
		}
		GenericArmor armor = (GenericArmor) stack.getItem();
		String name = material(armor);
		Mats m = MATS.get(name);
		ArmorItem.Type type = armor.getType();
		int total = switch (type) {
			case CHESTPLATE -> 4;
			case LEGGINGS -> name.equals("t1_miner") ? 2 : 3;
			default -> 2;
		};
		float ratio = type == ArmorItem.Type.LEGGINGS && m.ratio == 0.5f ? 1 / 3f : m.ratio;
		int count = (int) Math.ceil(total * damage);
		int metal = (int) Math.ceil(count * ratio);
		if (m.metal != null && metal > 0) mats.add(new ItemStack(m.metal.get(), metal));
		if (m.cloth != null && count - metal > 0) mats.add(new ItemStack(m.cloth.get(), count - metal));
		return mats;
	}

	/** Takes the materials from slots 0-8 and repairs {@code stack} fully, if they're all there. */
	public boolean repair(ItemStack stack) {
		List<ItemStack> mats = repairMaterials(stack);
		if (mats.isEmpty() || !mats.stream().allMatch(m -> count(m.getItem()) >= m.getCount())) return false;
		for (ItemStack mat : mats) {
			int left = mat.getCount();
			for (int i = 0; i < SLOT_ITEM && left > 0; i++) {
				if (inventory.getStackInSlot(i).is(mat.getItem())) left -= inventory.extractItem(i, left, false).getCount();
			}
		}
		stack.setDamageValue(0);
		setChanged();
		return true;
	}

	public int count(Item item) {
		int count = 0;
		for (int i = 0; i < SLOT_ITEM; i++) {
			if (inventory.getStackInSlot(i).is(item)) count += inventory.getStackInSlot(i).getCount();
		}
		return count;
	}
}
