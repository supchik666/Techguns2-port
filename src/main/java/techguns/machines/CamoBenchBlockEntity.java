package techguns.machines;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.state.BlockState;
import techguns.TGBlockEntities;
import techguns.TGBlocks;
import techguns.TGComponents;
import techguns.capabilities.TGExtendedPlayer;
import techguns.items.additionalslots.ItemTGSpecialSlot;
import techguns.items.additionalslots.TGSlotType;
import techguns.items.armors.GenericArmor;
import techguns.items.armors.GenericShield;
import techguns.items.guns.GenericGun;

/**
 * 1.12 CamoBenchTileEnt: cycles the camo (the CAMO component) of the gun, armor, shield or face/back/hand item in its slot,
 * or turns a block into the next of its group (1.12 CamoBenchRecipes: the colors of a vanilla block, TG panels...).
 * Also cycles the worn armor's camo.
 */
public class CamoBenchBlockEntity extends TGBlockEntity {
	/** 3/4: the slot's item forward/back; 5 + 2k / 6 + 2k: worn armor k (0: helmet) forward/back. */
	public static final int BUTTON_NEXT = 3, BUTTON_PREV = 4, BUTTON_ARMOR = 5;

	/** Blocks the bench cycles between. ponytail: the vanilla groups are 1.12's metadata blocks (wool, glass...) by color. */
	private static final List<List<Supplier<? extends ItemLike>>> GROUPS = new ArrayList<>();

	static {
		for (String name : new String[] {"wool", "concrete", "concrete_powder", "terracotta", "stained_glass", "stained_glass_pane", "banner", "carpet"}) {
			List<Supplier<? extends ItemLike>> group = new ArrayList<>();
			for (DyeColor color : DyeColor.values()) {
				ResourceLocation id = ResourceLocation.withDefaultNamespace(color.getName() + "_" + name);
				group.add(() -> BuiltInRegistries.ITEM.get(id));
			}
			GROUPS.add(group);
		}
		GROUPS.add(List.of(TGBlocks.CONTAINER_RED, TGBlocks.CONTAINER_GREEN, TGBlocks.CONTAINER_BLUE, TGBlocks.CONTAINER_ORANGE,
				TGBlocks.PANEL_LARGE_BORDER, TGBlocks.STEELFRAME_BLUE, TGBlocks.STEELFRAME_DARK, TGBlocks.STEELFRAME_SCAFFOLD));
		GROUPS.add(List.of(TGBlocks.NETHERMETAL_PANEL, TGBlocks.NETHERMETAL_GRATE1, TGBlocks.NETHERMETAL_GRATE2, TGBlocks.NETHERMETAL_GREY_DARK,
				TGBlocks.NETHERMETAL_GREY, TGBlocks.NETHERMETAL_GREY_TILES, TGBlocks.NETHERMETAL_BORDER_RED, TGBlocks.NETHERMETAL_PLATE_BLACK,
				TGBlocks.NETHERMETAL_PLATE_RED, TGBlocks.NETHERMETAL_BORDER_LAVA));
		GROUPS.add(List.of(TGBlocks.CONCRETE_BROWN, TGBlocks.CONCRETE_BROWN_LIGHT, TGBlocks.CONCRETE_GREY, TGBlocks.CONCRETE_GREY_DARK,
				TGBlocks.CONCRETE_BROWN_PIPES, TGBlocks.CONCRETE_BROWN_LIGHT_SCAFF));
		GROUPS.add(List.of(TGBlocks.NEONTUBES2, TGBlocks.NEONTUBES2_ROTATED, TGBlocks.NEONTUBES4, TGBlocks.NEONTUBES4_ROTATED, TGBlocks.NEONSQUARE_WHITE));
		GROUPS.add(List.of(TGBlocks.CAMONET_WOOD, TGBlocks.CAMONET_DESERT, TGBlocks.CAMONET_SNOW));
		GROUPS.add(List.of(TGBlocks.CAMONET_TOP_WOOD, TGBlocks.CAMONET_TOP_DESERT, TGBlocks.CAMONET_TOP_SNOW));
		GROUPS.add(List.of(TGBlocks.LADDER_METAL, TGBlocks.LADDER_SHINY, TGBlocks.LADDER_RUSTY, TGBlocks.LADDER_CARBON));
		GROUPS.add(List.of(TGBlocks.LAMP_YELLOW, TGBlocks.LAMP_WHITE));
		GROUPS.add(List.of(TGBlocks.LANTERN_YELLOW, TGBlocks.LANTERN_WHITE));
	}

	public CamoBenchBlockEntity(BlockPos pos, BlockState state) {
		super(TGBlockEntities.CAMO_BENCH.get(), pos, state, "camobench", new int[][] {{17, 18}});
	}

	/** The worn armor, the face/back/hand slots and the offhand, as 1.12 showed them. */
	@Override
	public void addMenuSlots(MachineMenu menu, Inventory inventory) {
		for (int k = 0; k < 4; k++) menu.addMenuSlot(RepairBenchBlockEntity.armorSlot(inventory, k, 99 + k * 18, 18));
		menu.addMenuSlot(RepairBenchBlockEntity.offhandSlot(inventory, -17, 94));
		TGExtendedPlayer tg = TGExtendedPlayer.get(inventory.player);
		TGSlotType[] types = {TGSlotType.FACE, TGSlotType.BACK, TGSlotType.HAND};
		for (int i = 0; i < 3; i++) {
			TGSlotType type = types[i];
			menu.addMenuSlot(new Slot(tg.inventory, TGExtendedPlayer.SLOT_FACE + i, 44 + i * 18, 18) {
				@Override
				public boolean mayPlace(ItemStack stack) {
					return type.accepts(stack);
				}
			});
		}
	}

	@Override
	public void buttonClicked(int id, Player player) {
		if (id == BUTTON_NEXT || id == BUTTON_PREV) {
			inventory.setStackInSlot(0, cycle(inventory.getStackInSlot(0), id == BUTTON_PREV));
		} else if (id >= BUTTON_ARMOR && id < BUTTON_ARMOR + 8) {
			ItemStack armor = player.getInventory().armor.get(3 - (id - BUTTON_ARMOR) / 2);
			if (camoCount(armor) > 1) cycle(armor, (id - BUTTON_ARMOR) % 2 == 1);
		} else {
			super.buttonClicked(id, player);
		}
	}

	/** Camos of a camo item, 0 for anything else. */
	public static int camoCount(ItemStack stack) {
		return switch (stack.getItem()) {
			case GenericGun gun -> gun.camoCount();
			case GenericArmor armor -> armor.textures.length;
			case GenericShield shield -> shield.camoCount;
			case ItemTGSpecialSlot item -> item.camoCount;
			default -> 0;
		};
	}

	public static int camo(ItemStack stack) {
		return Math.min(stack.getOrDefault(TGComponents.CAMO, 0), Math.max(camoCount(stack) - 1, 0));
	}

	/** The block group {@code stack} is in, or null. */
	private static List<Supplier<? extends ItemLike>> group(ItemStack stack) {
		for (var group : GROUPS) {
			for (var block : group) {
				if (stack.is(block.get().asItem())) return group;
			}
		}
		return null;
	}

	/**
	 * The next (or previous) camo of {@code stack}: camo items change in place, blocks become the next block of their
	 * group. Returns the stack to put back.
	 */
	public static ItemStack cycle(ItemStack stack, boolean back) {
		int count = camoCount(stack);
		if (count > 1) {
			stack.set(TGComponents.CAMO, (camo(stack) + (back ? count - 1 : 1)) % count);
			return stack;
		}
		var group = group(stack);
		if (group == null) return stack;
		int index = index(stack, group);
		Item next = group.get((index + (back ? group.size() - 1 : 1)) % group.size()).get().asItem();
		return stack.transmuteCopy(next, stack.getCount());
	}

	private static int index(ItemStack stack, List<Supplier<? extends ItemLike>> group) {
		for (int i = 0; i < group.size(); i++) {
			if (stack.is(group.get(i).get().asItem())) return i;
		}
		return 0;
	}

	/** "index/count:" for the GUI, empty if {@code stack} has no camo. */
	public static String camoIndex(ItemStack stack) {
		int count = camoCount(stack);
		if (count > 0) return (camo(stack) + 1) + "/" + count + ":";
		var group = group(stack);
		return group == null ? "" : (index(stack, group) + 1) + "/" + group.size();
	}

	/** The name of the current camo; blocks show none. */
	public static Component camoName(ItemStack stack) {
		if (stack.getItem() instanceof GenericArmor armor) return armor.camoName(stack);
		if (camoCount(stack) == 0) return group(stack) != null ? Component.empty() : Component.translatable("techguns.item.invalidcamo");
		int camo = camo(stack);
		return Component.translatable(camo == 0 ? "techguns.item.defaultcamo" : stack.getDescriptionId() + ".camoname." + camo);
	}
}
