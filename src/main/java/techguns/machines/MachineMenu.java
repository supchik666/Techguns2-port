package techguns.machines;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.SlotItemHandler;
import techguns.TGMenus;

/**
 * The menu of every {@link TGBlockEntity}: its slots at {@link TGBlockEntity#slots}, the player inventory below, and
 * {@link TGBlockEntity#menuData}. Buttons go to {@link TGBlockEntity#buttonClicked}.
 */
public class MachineMenu extends AbstractContainerMenu {
	public final TGBlockEntity be;
	public final ContainerData data;
	private final int size;

	public MachineMenu(int id, Inventory inventory, TGBlockEntity be) {
		this(id, inventory, be, be.menuData());
	}

	/** Client side; the block entity is the client's copy, synced by block updates. */
	public MachineMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buf) {
		this(id, inventory, (TGBlockEntity) inventory.player.level().getBlockEntity(buf.readBlockPos()), null);
	}

	private MachineMenu(int id, Inventory inventory, TGBlockEntity be, ContainerData data) {
		super(TGMenus.MACHINE.get(), id);
		this.be = be;
		this.data = data != null ? data : new SimpleContainerData(be.menuData().getCount());
		this.size = be.slots.length;
		for (int i = 0; i < size; i++) addSlot(new SlotItemHandler(be.inventory, i, be.slots[i][0], be.slots[i][1]));
		for (int row = 0; row < 3; row++) {
			for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
		}
		for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col, 8 + col * 18, 142));
		addDataSlots(this.data);
	}

	@Override
	public boolean clickMenuButton(Player player, int id) {
		if (be.canUse(player)) be.buttonClicked(id, player);
		return true;
	}

	@Override
	public boolean stillValid(Player player) {
		return Container.stillValidBlockEntity(be, player);
	}

	@Override
	public ItemStack quickMoveStack(Player player, int index) {
		Slot slot = slots.get(index);
		if (!slot.hasItem()) return ItemStack.EMPTY;
		ItemStack stack = slot.getItem();
		ItemStack copy = stack.copy();
		if (index < size) {
			if (!moveItemStackTo(stack, size, size + 36, true)) return ItemStack.EMPTY;
		} else if (!moveItemStackTo(stack, 0, size, false)) {
			boolean main = index < size + 27;
			if (!moveItemStackTo(stack, main ? size + 27 : size, main ? size + 36 : size + 27, false)) return ItemStack.EMPTY;
		}
		if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
		else slot.setChanged();
		if (stack.getCount() == copy.getCount()) return ItemStack.EMPTY;
		slot.onTake(player, stack);
		return copy;
	}

	/** Synced {@link MachineBlockEntity} values. */
	public int energy() {
		return (data.get(0) & 0xFFFF) | (data.get(1) << 16);
	}

	public int progress() {
		return data.get(2);
	}

	public int totaltime() {
		return data.get(3);
	}

	/** Progress scaled to 0..limit. */
	public int progressScaled(int limit) {
		return totaltime() > 0 ? progress() * limit / totaltime() : 0;
	}
}
