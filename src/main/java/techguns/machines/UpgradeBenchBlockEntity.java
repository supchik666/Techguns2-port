package techguns.machines;

import java.util.Optional;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.block.state.BlockState;
import techguns.TGBlockEntities;
import techguns.TGConfig;
import techguns.items.armors.GenericArmor;
import techguns.recipes.UpgradeBenchRecipe;

/**
 * 1.12 UpgradeBenchTileEnt and UpgradeBenchContainer: TG armor and an upgrade item ({@link UpgradeBenchRecipe}) make the
 * armor with that enchantment level, for {@link TGConfig#UPGRADE_XP_COST} XP per level it gains. Taking the result uses up both.
 * ponytail: 1.12 kept the inputs in the menu and gave them back on close; here they stay in the bench (and drop when it breaks).
 * ponytail: any TG armor takes the three protection upgrades, as in 1.12; TG armor isn't in the vanilla enchantable tags.
 */
public class UpgradeBenchBlockEntity extends TGBlockEntity {
	public static final int SLOT_ARMOR = 0, SLOT_UPGRADE = 1;

	public final SimpleContainer output = new SimpleContainer(1);
	public int cost;

	public UpgradeBenchBlockEntity(BlockPos pos, BlockState state) {
		super(TGBlockEntities.UPGRADE_BENCH.get(), pos, state, "armorbench", new int[][] {{17, 47}, {53, 47}});
	}

	@Override
	public boolean canInsert(int slot, ItemStack stack) {
		return slot != SLOT_ARMOR || stack.getItem() instanceof GenericArmor;
	}

	@Override
	protected void contentsChanged(int slot) {
		super.contentsChanged(slot);
		updateOutput();
	}

	private Optional<UpgradeBenchRecipe> recipe() {
		ItemStack upgrade = inventory.getStackInSlot(SLOT_UPGRADE);
		if (level == null || upgrade.isEmpty()) return Optional.empty();
		return level.getRecipeManager().getRecipeFor(UpgradeBenchRecipe.TYPE.get(), new SingleRecipeInput(upgrade), level).map(r -> r.value());
	}

	/** 1.12 updateOutput: the armor with the recipe's level when that's more than it has, and what it costs. */
	public void updateOutput() {
		ItemStack armor = inventory.getStackInSlot(SLOT_ARMOR);
		Optional<UpgradeBenchRecipe> recipe = armor.getItem() instanceof GenericArmor ? recipe() : Optional.empty();
		ItemStack result = ItemStack.EMPTY;
		cost = 0;
		if (recipe.isPresent()) {
			UpgradeBenchRecipe r = recipe.get();
			int has = EnchantmentHelper.getItemEnchantmentLevel(r.enchantment(), armor);
			if (r.level() > has) {
				cost = TGConfig.UPGRADE_XP_COST.get() * (r.level() - has);
				result = armor.copy();
				EnchantmentHelper.updateEnchantments(result, e -> e.set(r.enchantment(), r.level()));
			}
		}
		output.setItem(0, result);
	}

	/** The result slot, then the worn armor, as 1.12's container. */
	@Override
	public void addMenuSlots(MachineMenu menu, Inventory inventory) {
		updateOutput();
		menu.addMenuSlot(new Slot(output, 0, 116, 47) {
			@Override
			public boolean mayPlace(ItemStack stack) {
				return false;
			}

			@Override
			public boolean mayPickup(Player player) {
				return hasItem() && (player.getAbilities().instabuild || player.totalExperience >= cost);
			}

			@Override
			public void onTake(Player player, ItemStack stack) {
				int xp = cost;
				UpgradeBenchBlockEntity.this.inventory.setStackInSlot(SLOT_ARMOR, ItemStack.EMPTY);
				UpgradeBenchBlockEntity.this.inventory.extractItem(SLOT_UPGRADE, 1, false);
				player.level().playSound(null, player.blockPosition(), SoundEvents.ARMOR_EQUIP_DIAMOND.value(), SoundSource.PLAYERS, 1, 1);
				if (!player.getAbilities().instabuild) player.giveExperiencePoints(-xp);
				super.onTake(player, stack);
			}
		});
		for (int k = 0; k < 4; k++) menu.addMenuSlot(RepairBenchBlockEntity.armorSlot(inventory, k, 17 + k * 18, 18));
	}

	@Override
	public ContainerData menuData() {
		return new ContainerData() {
			@Override
			public int get(int index) {
				return cost;
			}

			@Override
			public void set(int index, int value) {
				cost = value;
			}

			@Override
			public int getCount() {
				return 1;
			}
		};
	}
}
