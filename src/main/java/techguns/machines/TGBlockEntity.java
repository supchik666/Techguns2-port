package techguns.machines;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * Base of Techguns block entities with a GUI (1.12 BasicTGTileEntity, BasicInventoryTileEnt, BasicOwnedTileEnt,
 * BasicRedstoneTileEnt): an inventory laid out by {@link #slots}, an owner and security level, and a redstone mode.
 * Synced to clients whole by {@link #needUpdate}; buttons arrive through {@link MachineMenu#clickMenuButton}.
 */
public abstract class TGBlockEntity extends BlockEntity implements MenuProvider {
	public static final int BUTTON_SECURITY = 1;
	public static final int BUTTON_REDSTONE = 2;

	/** GUI position of each inventory slot. */
	public final int[][] slots;
	public final ItemStackHandler inventory;
	private final String name;
	@Nullable
	public UUID owner;
	/** 0: anyone may use it, 3: only the owner. ponytail: 1.12's 1 and 2 (FTB team allies/members) aren't ported. */
	public byte security;
	/** 0: ignores redstone, 1: works with a signal, 2: works without one. */
	public byte redstoneMode;
	public boolean hasSignal;

	/** {@code name} is the GUI title key, techguns.container.name. */
	protected TGBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, String name, int[][] slots) {
		super(type, pos, state);
		this.name = name;
		this.slots = slots;
		inventory = new ItemStackHandler(slots.length) {
			@Override
			protected void onContentsChanged(int slot) {
				contentsChanged(slot);
			}

			@Override
			public boolean isItemValid(int slot, ItemStack stack) {
				return canInsert(slot, stack);
			}
		};
	}

	/** Item handler capability: automation inserts where players may and takes only from output slots. ponytail: no per-side config. */
	public final IItemHandler automation = new IItemHandler() {
		@Override
		public int getSlots() {
			return inventory.getSlots();
		}

		@Override
		public ItemStack getStackInSlot(int slot) {
			return inventory.getStackInSlot(slot);
		}

		@Override
		public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
			return inventory.insertItem(slot, stack, simulate);
		}

		@Override
		public ItemStack extractItem(int slot, int amount, boolean simulate) {
			return isOutput(slot) ? inventory.extractItem(slot, amount, simulate) : ItemStack.EMPTY;
		}

		@Override
		public int getSlotLimit(int slot) {
			return inventory.getSlotLimit(slot);
		}

		@Override
		public boolean isItemValid(int slot, ItemStack stack) {
			return inventory.isItemValid(slot, stack);
		}
	};

	protected void contentsChanged(int slot) {
		setChanged();
	}

	/** Whether players and automation may put {@code stack} into {@code slot}. */
	public boolean canInsert(int slot, ItemStack stack) {
		return !isOutput(slot);
	}

	public boolean isOutput(int slot) {
		return false;
	}

	public boolean canUse(Player player) {
		return owner == null || security == 0 || owner.equals(player.getUUID());
	}

	public boolean redstoneEnabled() {
		return redstoneMode == 0 || hasSignal == (redstoneMode == 1);
	}

	public void updateSignal() {
		boolean signal = level.hasNeighborSignal(worldPosition);
		if (signal != hasSignal) {
			hasSignal = signal;
			needUpdate();
		}
	}

	/** Server side, from a menu button; the player passed {@link #canUse}. */
	public void buttonClicked(int id, Player player) {
		if (id == BUTTON_SECURITY && player.getUUID().equals(owner)) {
			security = (byte) (security == 0 ? 3 : 0);
		} else if (id == BUTTON_REDSTONE) {
			redstoneMode = (byte) ((redstoneMode + 1) % 3);
		} else {
			return;
		}
		needUpdate();
	}

	/** Values the menu syncs every tick (ints sent as shorts). */
	public ContainerData menuData() {
		return new SimpleContainerData(0);
	}

	/** Called every tick on both sides. */
	public void tick() {}

	/** What the block drops when broken, besides itself. */
	public List<ItemStack> drops() {
		List<ItemStack> drops = new ArrayList<>();
		for (int i = 0; i < inventory.getSlots(); i++) drops.add(inventory.getStackInSlot(i));
		return drops;
	}

	/** Saves and sends the whole block entity to clients. */
	public void needUpdate() {
		setChanged();
		if (level != null && !level.isClientSide) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
	}

	@Override
	protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.saveAdditional(tag, registries);
		tag.put("inventory", inventory.serializeNBT(registries));
		if (owner != null) tag.putUUID("owner", owner);
		tag.putByte("security", security);
		tag.putByte("redstoneMode", redstoneMode);
		tag.putBoolean("hasSignal", hasSignal);
	}

	@Override
	protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.loadAdditional(tag, registries);
		inventory.deserializeNBT(registries, tag.getCompound("inventory"));
		owner = tag.hasUUID("owner") ? tag.getUUID("owner") : null;
		security = tag.getByte("security");
		redstoneMode = tag.getByte("redstoneMode");
		hasSignal = tag.getBoolean("hasSignal");
	}

	@Override
	public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
		return saveCustomOnly(registries);
	}

	@Override
	public Packet<ClientGamePacketListener> getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}

	@Override
	public Component getDisplayName() {
		return Component.translatable("techguns.container." + name);
	}

	@Override
	public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
		return new MachineMenu(id, inventory, this);
	}
}
