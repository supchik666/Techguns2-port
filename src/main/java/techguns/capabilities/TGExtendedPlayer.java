package techguns.capabilities;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.attachment.AttachmentSyncHandler;
import net.neoforged.neoforge.attachment.IAttachmentHolder;
import net.neoforged.neoforge.common.util.INBTSerializable;
import techguns.TGComponents;
import techguns.items.additionalslots.TGSlotType;

/**
 * The 1.12 TGExtendedPlayer capability as a data attachment ({@link TGComponents#EXTENDED_PLAYER}): the 15 extra
 * slots (1.12 TGPlayerInventory), the keybind toggles and the auto feeder's leftovers. Saved, copied on death (the
 * slots are emptied into the drops first unless keepInventory) and synced to everyone who sees the player.
 * ponytail: radiation (1.12 radlevel) and the gun timers live in their own attachments.
 */
public class TGExtendedPlayer implements INBTSerializable<CompoundTag> {
	public static final int NUMSLOTS = 15;
	public static final int SLOT_FACE = 0;
	public static final int SLOT_BACK = 1;
	public static final int SLOT_HAND = 2;
	public static final int SLOTS_AUTOFOOD_START = 3;
	public static final int SLOTS_AUTOFOOD_END = 5;
	public static final int SLOT_AUTOHEAL = 6;
	public static final int SLOTS_AMMO_START = 7;
	public static final int SLOTS_AMMO_END = 14;

	public final SimpleContainer inventory = new SimpleContainer(NUMSLOTS);

	public boolean enableJetpack = true;
	public boolean enableStepAssist = true;
	public boolean enableNightVision;
	public boolean enableSafemode;
	/** Armor jump bonus; stored inverted (bit 256) so saves from before it default to on. */
	public boolean enableJumpBoost = true;
	/** Saved but unused, as in 1.12. */
	public boolean enableHovermode;
	/** Read by the client, saved on the server. */
	public boolean showTGHudElements = true;

	public short foodleft;
	public float lastSaturation = 1.0f;

	/** Jetpack boosting; the owning client sets it and tells the server (synced to others). */
	public boolean isJumpkeyPressed;
	/** Set each tick by the glider or jetpack (synced to others, for the model and sound). */
	public boolean isGliding;
	/** Server: was flying with the anti-grav pack last tick, for the start effect. */
	public boolean wasFlying;
	/** Client: the loop sounds playing for this player (TGPlayerClient). */
	public Object jetpackSound, gliderSound;

	/** Server: what the clients last got. */
	private final NonNullList<ItemStack> synced = NonNullList.withSize(NUMSLOTS, ItemStack.EMPTY);
	private int syncedStates;

	public static TGExtendedPlayer get(Player player) {
		return player.getData(TGComponents.EXTENDED_PLAYER);
	}

	public ItemStack getSlot(int slot) {
		return inventory.getItem(slot);
	}

	public static boolean isSafemode(Player player) {
		return get(player).enableSafemode;
	}

	public static boolean isNightVision(Player player) {
		return get(player).enableNightVision;
	}

	public static boolean isStepAssist(Player player) {
		return get(player).enableStepAssist;
	}

	public static boolean isJetpack(Player player) {
		return get(player).enableJetpack;
	}

	public static boolean isHud(Player player) {
		return get(player).showTGHudElements;
	}

	/** Bits as in 1.12 (jetpack, nv, safe, step, hud, hover); 6 and 7 are the synced jump key and gliding, 8 jump boost off. */
	private int states(boolean withTransient) {
		int b = (enableJetpack ? 1 : 0) | (enableNightVision ? 2 : 0) | (enableSafemode ? 4 : 0) | (enableStepAssist ? 8 : 0)
				| (showTGHudElements ? 16 : 0) | (enableHovermode ? 32 : 0) | (enableJumpBoost ? 0 : 256);
		if (withTransient) b |= (isJumpkeyPressed ? 64 : 0) | (isGliding ? 128 : 0);
		return b;
	}

	private void setStates(int b, boolean withTransient) {
		enableJetpack = (b & 1) != 0;
		enableNightVision = (b & 2) != 0;
		enableSafemode = (b & 4) != 0;
		enableStepAssist = (b & 8) != 0;
		showTGHudElements = (b & 16) != 0;
		enableHovermode = (b & 32) != 0;
		enableJumpBoost = (b & 256) == 0;
		if (withTransient) {
			isJumpkeyPressed = (b & 64) != 0;
			isGliding = (b & 128) != 0;
		}
	}

	@Override
	public CompoundTag serializeNBT(HolderLookup.Provider provider) {
		CompoundTag tag = ContainerHelper.saveAllItems(new CompoundTag(), inventory.getItems(), provider);
		tag.putInt("states", states(false));
		tag.putShort("foodLeft", foodleft);
		tag.putFloat("lastSaturation", lastSaturation);
		return tag;
	}

	@Override
	public void deserializeNBT(HolderLookup.Provider provider, CompoundTag tag) {
		inventory.clearContent();
		ContainerHelper.loadAllItems(tag, inventory.getItems(), provider);
		setStates(tag.getInt("states"), false);
		foodleft = tag.getShort("foodLeft");
		lastSaturation = tag.getFloat("lastSaturation");
	}

	/**
	 * Server, each tick: syncs when a slot's item or count or a toggle changed; changes to durability alone (fuel
	 * draining every tick) go out once a second. ponytail: other players get the ammo and food slots too.
	 */
	public void syncIfChanged(Player player) {
		if (!(player instanceof ServerPlayer)) return;
		boolean now = states(true) != syncedStates, later = false;
		for (int i = 0; i < NUMSLOTS; i++) {
			ItemStack a = inventory.getItem(i), b = synced.get(i);
			if (!ItemStack.isSameItem(a, b) || a.getCount() != b.getCount()) now = true;
			else if (!ItemStack.isSameItemSameComponents(a, b)) later = true;
		}
		if (now || later && player.tickCount % 20 == 0) {
			for (int i = 0; i < NUMSLOTS; i++) synced.set(i, inventory.getItem(i).copy());
			syncedStates = states(true);
			player.syncData(TGComponents.EXTENDED_PLAYER);
		}
	}

	public static final AttachmentSyncHandler<TGExtendedPlayer> SYNC = new AttachmentSyncHandler<>() {
		@Override
		public void write(RegistryFriendlyByteBuf buf, TGExtendedPlayer tg, boolean initialSync) {
			for (ItemStack stack : tg.inventory.getItems()) ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, stack);
			buf.writeShort(tg.states(true));
		}

		/** In place, so open menus keep pointing at the right container; the local player keeps its own jump key and gliding. */
		@Override
		public TGExtendedPlayer read(IAttachmentHolder holder, RegistryFriendlyByteBuf buf, @Nullable TGExtendedPlayer prev) {
			TGExtendedPlayer tg = prev != null ? prev : new TGExtendedPlayer();
			for (int i = 0; i < NUMSLOTS; i++) tg.inventory.setItem(i, ItemStack.OPTIONAL_STREAM_CODEC.decode(buf));
			tg.setStates(buf.readShort(), !(holder instanceof Player p && p.isLocalPlayer()));
			return tg;
		}
	};

	/** Removes one {@code item}, from the ammo slots first (1.12 InventoryUtil.consumeAmmoPlayer). */
	public static boolean consumeAmmo(Player player, Item item) {
		TGExtendedPlayer tg = get(player);
		for (int i = SLOTS_AMMO_START; i <= SLOTS_AMMO_END; i++) {
			if (tg.inventory.getItem(i).is(item)) {
				tg.inventory.removeItem(i, 1);
				return true;
			}
		}
		int slot = player.getInventory().findSlotMatchingItem(new ItemStack(item));
		if (slot < 0) return false;
		player.getInventory().removeItem(slot, 1);
		return true;
	}

	/** Puts {@code stack} into the ammo slots if it fits there, then the inventory, then drops the rest (1.12 addAmmoToPlayerInventory). */
	public static void giveAmmo(Player player, ItemStack stack) {
		if (TGSlotType.AMMO.accepts(stack)) {
			TGExtendedPlayer tg = get(player);
			for (int i = SLOTS_AMMO_START; i <= SLOTS_AMMO_END && !stack.isEmpty(); i++) {
				ItemStack slot = tg.inventory.getItem(i);
				if (slot.isEmpty()) {
					tg.inventory.setItem(i, stack.copyAndClear());
				} else if (ItemStack.isSameItemSameComponents(slot, stack)) {
					int move = Math.min(stack.getCount(), slot.getMaxStackSize() - slot.getCount());
					slot.grow(move);
					stack.shrink(move);
				}
			}
		}
		if (!stack.isEmpty() && !player.getInventory().add(stack) && !player.level().isClientSide) player.drop(stack, false);
	}

	/** {@code item}s in the inventory and ammo slots. */
	public static int countAmmo(Player player, Item item) {
		int n = player.getInventory().countItem(item);
		TGExtendedPlayer tg = get(player);
		for (int i = SLOTS_AMMO_START; i <= SLOTS_AMMO_END; i++) {
			if (tg.inventory.getItem(i).is(item)) n += tg.inventory.getItem(i).getCount();
		}
		return n;
	}
}
