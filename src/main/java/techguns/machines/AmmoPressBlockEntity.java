package techguns.machines;

import java.util.Comparator;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.state.BlockState;
import techguns.TGBlockEntities;
import techguns.TGSounds;
import techguns.recipes.MachineRecipe;

/** 1.12 AmmoPressTileEnt: makes the rounds of the chosen build plan, one ammo_press recipe each (cycled in id order). */
public class AmmoPressBlockEntity extends MachineBlockEntity {
	public static final int BUTTON_NEXT = 3;
	public static final int BUTTON_PREV = 4;

	public int plan;

	public AmmoPressBlockEntity(BlockPos pos, BlockState state) {
		super(TGBlockEntities.AMMO_PRESS.get(), pos, state, "ammopress", new int[][] {{100, 17}, {120, 17}, {140, 17}, {120, 60}, {152, 60}},
				MachineRecipe.AMMO_PRESS, 20000, 5, 3, false);
	}

	public List<RecipeHolder<MachineRecipe>> plans() {
		return level.getRecipeManager().getAllRecipesFor(kind.type.get()).stream().sorted(Comparator.comparing(RecipeHolder::id)).toList();
	}

	@Nullable
	public RecipeHolder<MachineRecipe> selectedPlan() {
		List<RecipeHolder<MachineRecipe>> plans = plans();
		return plans.isEmpty() ? null : plans.get(Math.floorMod(plan, plans.size()));
	}

	@Override
	protected List<RecipeHolder<MachineRecipe>> recipes() {
		RecipeHolder<MachineRecipe> selected = selectedPlan();
		return selected == null ? List.of() : List.of(selected);
	}

	@Override
	public void buttonClicked(int id, Player player) {
		if (id == BUTTON_NEXT || id == BUTTON_PREV) {
			plan = Math.floorMod(plan + (id == BUTTON_NEXT ? 1 : -1), Math.max(plans().size(), 1));
			contentsChanged = true;
			needUpdate();
		}
		super.buttonClicked(id, player);
	}

	@Override
	protected void ambient() {
		if (atFraction(0.05f)) playSound(TGSounds.AMMO_PRESS_WORK1.get(), 0.5f, 1);
		else if (atFraction(0.30f)) playSound(TGSounds.AMMO_PRESS_WORK2.get(), 0.5f, 1);
	}

	@Override
	protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.saveAdditional(tag, registries);
		tag.putInt("plan", plan);
	}

	@Override
	protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.loadAdditional(tag, registries);
		plan = tag.getInt("plan");
	}
}
