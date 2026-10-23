package techguns.machines;

import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import techguns.TGBlockEntities;
import techguns.TGComponents;
import techguns.TGItems;
import techguns.TGEntities;
import techguns.entities.npcs.NPCTurret;
import techguns.items.guns.GenericGun;

/**
 * 1.12 TurretTileEnt: the base of an {@link NPCTurret}. Holds its gun, armor plate, ammo (inputs) and the empty magazines
 * it hands back (outputs). Costs 5 FE/t while on; heals the turret 1 HP per 3 s for 500 FE, and after it dies rebuilds
 * it in 10 s at 50 FE/t, back at 10 HP. Redstone off or no power switches the turret's AI off.
 */
public class TurretBlockEntity extends MachineBlockEntity implements GenericGun.AmmoSource {
	public static final int SLOT_WEAPON = 18, SLOT_ARMOR = 19;
	public static final int BUTTON_TARGET_ANIMALS = 3, BUTTON_PVP = 4;
	public static final int REPAIR_TIME = 200, POWER = 5, REPAIR_POWER = 50, HEAL_POWER = 500, HEAL_DELAY = 60;
	/** Ticks a saved turret may take to load with its chunk before it counts as lost. */
	private static final int LINK_TIMEOUT = 100;

	@Nullable
	public UUID turretId;
	public boolean turretDeath, attackAnimals;
	/** 0: no PvP, 3: shoots players and turrets outside the owner's team, 4: everyone but the owner (1.12 TGNpcFactions.shouldAttack). */
	public byte pvp;
	public int repairTime;
	private int healTime, unlinked;

	public TurretBlockEntity(BlockPos pos, BlockState state) {
		super(TGBlockEntities.TURRET.get(), pos, state, "turretbase", slots(), null, 5000, POWER, 9, false, false);
	}

	private static int[][] slots() {
		int[][] slots = new int[20][];
		for (int i = 0; i < 3; i++) {
			for (int j = 0; j < 3; j++) {
				slots[i * 3 + j] = new int[] {18 + 18 * i, 17 + 18 * j};
				slots[9 + i * 3 + j] = new int[] {116 + 18 * i, 17 + 18 * j};
			}
		}
		slots[SLOT_WEAPON] = new int[] {85, 19};
		slots[SLOT_ARMOR] = new int[] {85, 42};
		return slots;
	}

	public boolean hanging() {
		return getBlockState().getValue(TurretBlock.HANGING);
	}

	public ItemStack weapon() {
		return inventory.getStackInSlot(SLOT_WEAPON);
	}

	@Override
	public boolean isOutput(int slot) {
		return slot >= 9 && slot < 18;
	}

	@Override
	public boolean canInsert(int slot, ItemStack stack) {
		if (slot == SLOT_WEAPON) return stack.getItem() instanceof GenericGun;
		if (slot == SLOT_ARMOR) return armorTier(stack) > 0;
		return slot < 9;
	}

	/** 1-4 for the turret armor plates, else 0. Picks the base texture. */
	public static int armorTier(ItemStack stack) {
		if (stack.is(TGItems.TURRET_ARMOR_IRON)) return 1;
		if (stack.is(TGItems.TURRET_ARMOR_STEEL)) return 2;
		if (stack.is(TGItems.TURRET_ARMOR_OBSIDIAN_STEEL)) return 3;
		return stack.is(TGItems.TURRET_ARMOR_CARBON) ? 4 : 0;
	}

	public int tier() {
		return armorTier(inventory.getStackInSlot(SLOT_ARMOR));
	}

	/** The turret's armor: 5 per tier. */
	public float armorValue() {
		return 5 * tier();
	}

	@Override
	protected void contentsChanged(int slot) {
		super.contentsChanged(slot);
		if (slot >= SLOT_WEAPON) needUpdate(); // the gun and the plate show on clients
	}

	@Nullable
	public NPCTurret turret() {
		return turretId != null && level instanceof ServerLevel server && server.getEntity(turretId) instanceof NPCTurret turret ? turret : null;
	}

	@Override
	public void tick() {
		if (level.isClientSide) return;
		NPCTurret turret = turret();
		if (turretDeath) {
			if (redstoneEnabled() && energy.consume(REPAIR_POWER) && --repairTime <= 0) spawnTurret(10.0f);
			return;
		}
		if (turret == null) {
			// a new block spawns at once; a saved turret gets time to load. ponytail: one lost for good just counts as killed
			if (turretId == null) spawnTurret(-1);
			else if (++unlinked > LINK_TIMEOUT) onTurretDeath();
			return;
		}
		unlinked = 0;
		boolean on = redstoneEnabled() && energy.consume(POWER);
		turret.setNoAi(!on);
		if (!on) return;
		if (healTime > 0) {
			healTime--;
		} else if (turret.getHealth() < turret.getMaxHealth() && energy.consume(HEAL_POWER)) {
			turret.heal(1.0f);
			healTime = HEAL_DELAY;
		}
	}

	/** Spawns the turret on the base; {@code health} -1 for full. */
	private void spawnTurret(float health) {
		NPCTurret turret = TGEntities.TURRET.get().create(level);
		if (turret == null) return;
		turret.link(worldPosition, hanging());
		if (health > 0) turret.setHealth(health);
		level.addFreshEntity(turret);
		turretId = turret.getUUID();
		turretDeath = false;
		repairTime = 0;
		needUpdate();
	}

	/** From the turret's death; the base starts rebuilding it. */
	public void onTurretDeath() {
		turretId = null;
		turretDeath = true;
		repairTime = REPAIR_TIME;
		unlinked = 0;
		needUpdate();
	}

	/** The block is gone: so is its turret. */
	public void removeTurret() {
		NPCTurret turret = turret();
		if (turret != null) turret.discard();
	}

	/** Takes a round for the next shot, reloading from the input slots when empty (1.12 consumeAmmo). */
	public boolean consumeAmmo() {
		ItemStack stack = weapon();
		if (!(stack.getItem() instanceof GenericGun gun)) return false;
		if (gun.getAmmo(stack) <= 0 && !gun.load(stack, this, 0)) return false;
		stack.set(TGComponents.AMMO, gun.getAmmo(stack) - 1);
		setChanged();
		return true;
	}

	@Override
	public boolean consume(Item item) {
		for (int i = 0; i < 9; i++) {
			if (inventory.getStackInSlot(i).is(item)) {
				inventory.extractItem(i, 1, false);
				return true;
			}
		}
		return false;
	}

	@Override
	public void give(ItemStack stack) {
		ItemStack left = stack.copy();
		for (int i = 9; i < 18 && !left.isEmpty(); i++) {
			ItemStack out = inventory.getStackInSlot(i);
			if (out.isEmpty()) {
				inventory.setStackInSlot(i, left);
				return;
			}
			if (ItemStack.isSameItemSameComponents(out, left)) {
				int n = Math.min(left.getCount(), out.getMaxStackSize() - out.getCount());
				out.grow(n);
				left.shrink(n);
				setChanged();
			}
		}
		if (!left.isEmpty()) Containers.dropItemStack(level, worldPosition.getX() + 0.5, worldPosition.getY() + 1, worldPosition.getZ() + 0.5, left);
	}

	@Override
	public int count(Item item) {
		int count = 0;
		for (int i = 0; i < 9; i++) {
			if (inventory.getStackInSlot(i).is(item)) count += inventory.getStackInSlot(i).getCount();
		}
		return count;
	}

	@Override
	public void buttonClicked(int id, Player player) {
		if (id == BUTTON_TARGET_ANIMALS) {
			attackAnimals = !attackAnimals;
		} else if (id == BUTTON_PVP && player.getUUID().equals(owner)) {
			pvp = (byte) (pvp == 0 ? 3 : pvp == 3 ? 4 : 0);
		} else {
			super.buttonClicked(id, player);
			return;
		}
		needUpdate();
	}

	/** Energy (two shorts), repair ticks left, {@link #REPAIR_TIME} while dead, turret health and max health. */
	@Override
	public ContainerData menuData() {
		ContainerData base = super.menuData();
		return new ContainerData() {
			@Override
			public int get(int index) {
				NPCTurret turret = turret();
				return switch (index) {
					case 2 -> repairTime;
					case 3 -> turretDeath ? REPAIR_TIME : 0;
					case 4 -> turret == null ? 0 : (int) Math.ceil(turret.getHealth());
					case 5 -> turret == null ? 0 : (int) turret.getMaxHealth();
					default -> base.get(index);
				};
			}

			@Override
			public void set(int index, int value) {}

			@Override
			public int getCount() {
				return 6;
			}
		};
	}

	@Override
	protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.saveAdditional(tag, registries);
		if (turretId != null) tag.putUUID("turret", turretId);
		tag.putBoolean("turretDeath", turretDeath);
		tag.putInt("repairTime", repairTime);
		tag.putBoolean("attackAnimals", attackAnimals);
		tag.putByte("pvp", pvp);
	}

	@Override
	protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.loadAdditional(tag, registries);
		turretId = tag.hasUUID("turret") ? tag.getUUID("turret") : null;
		turretDeath = tag.getBoolean("turretDeath");
		repairTime = tag.getInt("repairTime");
		attackAnimals = tag.getBoolean("attackAnimals");
		pvp = tag.getByte("pvp");
	}
}
