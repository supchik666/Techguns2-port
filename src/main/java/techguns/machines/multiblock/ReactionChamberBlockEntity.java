package techguns.machines.multiblock;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import techguns.TGBlocks;
import techguns.TGEffects;
import techguns.TGItems;
import techguns.TGRadiation;
import techguns.TGSounds;
import techguns.recipes.ReactionChamberRecipe;

/**
 * 1.12 ReactionChamberTileEntMaster, ReactionChamberOperation and ReactionChamberDefinition: a 3x3x4 chamber behind the
 * controller, housings at the bottom and top, glass in between. The player sets the liquid level (buckets the tank holds)
 * and the beam intensity; a {@link ReactionChamberRecipe} starts when both and the fluid match it, then every
 * {@link #TICKRATE} ticks it takes power and counts a completion if they still match, while the wanted intensity drifts.
 * Missed reaction ticks irradiate the area; running out of time loses the input or blows the chamber up.
 * The side connectors (front and sides at the bottom) take items and fluid, the top center takes power.
 */
public class ReactionChamberBlockEntity extends MultiBlockMachine {
	public static final int BUTTON_LEVEL_UP = 3, BUTTON_LEVEL_DOWN = 4, BUTTON_INTENSITY_UP = 5, BUTTON_INTENSITY_DOWN = 6, BUTTON_DUMP = 7;
	public static final int TICKRATE = 60;
	public static final int SLOT_INPUT = 0, SLOT_FOCUS = 1;
	public static final int HOUSING = 1, SIDE_CONNECTOR = 2, ENERGY_CONNECTOR = 3;

	public int liquidLevel, intensity;
	/** The running reaction, or null. */
	@Nullable
	public ResourceLocation reaction;
	public int completion, requiredIntensity, nextTick;

	/** Fills up to the liquid level and drains only what's above it (1.12 ReactionChamberFluidTank). */
	public final FluidTank tank = new FluidTank(10000) {
		@Override
		public int fill(FluidStack resource, FluidAction action) {
			int space = liquidLevel * 1000 - getFluidAmount();
			return space <= 0 ? 0 : super.fill(resource.copyWithAmount(Math.min(resource.getAmount(), space)), action);
		}

		@Override
		public FluidStack drain(int maxDrain, FluidAction action) {
			return super.drain(Math.min(maxDrain, Math.max(getFluidAmount() - liquidLevel * 1000, 0)), action);
		}

		@Override
		protected void onContentsChanged() {
			contentsChanged = true;
			needUpdate();
		}
	};

	public ReactionChamberBlockEntity(BlockPos pos, BlockState state) {
		super(MultiBlocks.REACTION_CHAMBER.get(), pos, state, "reactionchamber", new int[][] {{35, 17}, {93, 17}, {134, 17}, {152, 17}, {134, 35}, {152, 35}},
				null, 1000000, 0, 2, false);
	}

	public static boolean isFocus(ItemStack stack) {
		return stack.is(TGItems.RC_HEAT_RAY) || stack.is(TGItems.RC_UV_EMITTER);
	}

	@Override
	public boolean canInsert(int slot, ItemStack stack) {
		return slot == SLOT_FOCUS ? isFocus(stack) : super.canInsert(slot, stack);
	}

	@Nullable
	public ReactionChamberRecipe recipe() {
		if (reaction == null || level == null) return null;
		return level.getRecipeManager().byKey(reaction).map(h -> h.value() instanceof ReactionChamberRecipe r ? r : null).orElse(null);
	}

	@Override
	public boolean isWorking() {
		return reaction != null;
	}

	@Override
	public void tick() {
		if (!formed || !redstoneEnabled()) return;
		ReactionChamberRecipe recipe = recipe();
		if (recipe == null) {
			if (!level.isClientSide && contentsChanged) start();
			return;
		}
		progress++;
		nextTick--;
		if (level.isClientSide) {
			if (nextTick == TICKRATE - 1) reactionTickEffects();
			return;
		}
		if (nextTick <= 0 && progress < totaltime) {
			reactionTick(recipe);
			nextTick = TICKRATE;
		} else if (progress < totaltime && completion < recipe.requiredCompletion()) {
			return;
		}
		if (completion >= recipe.requiredCompletion()) {
			for (ItemStack result : recipe.results()) {
				// ponytail: a result that doesn't fit drops whole, 1.12 dropped only the overflow
				if (!output(List.of(result.copy()), false)) Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), result.copy());
			}
			if (recipe.liquidConsumption() > 0) {
				tank.getFluid().shrink(recipe.liquidConsumption());
				if (tank.getFluid().isEmpty()) tank.setFluid(FluidStack.EMPTY);
			}
			clear();
			start();
		} else if (progress >= totaltime) {
			clear();
			switch (recipe.risk()) {
				case EXPLOSION_LOW -> explode(false);
				case EXPLOSION_MEDIUM -> explode(true);
				default -> {}
			}
			if (formed && !recipe.risk().fatal()) start();
		}
		needUpdate();
	}

	private void clear() {
		reaction = null;
		progress = totaltime = completion = 0;
	}

	/** 1.12 checkAndStartOperation: the first recipe matching the input, focus, fluid, level and intensity, if there's power for a tick. */
	@Override
	protected void start() {
		contentsChanged = false;
		ItemStack input = inventory.getStackInSlot(SLOT_INPUT);
		FluidStack fluid = tank.getFluid();
		if (input.isEmpty()) return;
		for (RecipeHolder<ReactionChamberRecipe> holder : level.getRecipeManager().getAllRecipesFor(MultiBlocks.REACTION_CHAMBER_RECIPE.get())) {
			ReactionChamberRecipe r = holder.value();
			if (r.input().test(input) && inventory.getStackInSlot(SLOT_FOCUS).is(r.focus()) && r.fluid().test(fluid) && liquidLevel == r.liquidLevel()
					&& fluid.getAmount() == r.liquidLevel() * 1000 && intensity == r.intensity()) {
				if (energy.getEnergyStored() < r.power()) {
					contentsChanged = true;
					return;
				}
				inventory.extractItem(SLOT_INPUT, 1, false);
				contentsChanged = false;
				reaction = holder.id();
				totaltime = r.ticks() * TICKRATE;
				progress = completion = 0;
				requiredIntensity = r.intensity();
				nextTick = TICKRATE;
				needUpdate();
				return;
			}
		}
	}

	private void reactionTick(ReactionChamberRecipe recipe) {
		boolean powered = energy.consume(recipe.power());
		ItemStack focus = inventory.getStackInSlot(SLOT_FOCUS);
		boolean focusMatch = focus.is(recipe.focus());
		boolean success = powered && focusMatch && requiredIntensity == intensity && recipe.liquidLevel() == liquidLevel;
		if (success) completion++;
		// both 1.12 foci: radiation factor 0.5 on failure, 0 on success; radius 6, full strength within 4
		int rad = success ? 0 : Mth.ceil(0.5f * intensity);
		if (rad > 0 && isFocus(focus)) TGRadiation.applyRadToEntities(level, Vec3.atCenterOf(worldPosition), 6, 62, rad - 1, 4, 0);
		if (!focusMatch) {
			progress = totaltime;
			completion = 0;
		} else if (recipe.instability() > 0 && level.random.nextFloat() < recipe.instability()) {
			int jump = 1 + (recipe.margin() > 1 ? level.random.nextInt(recipe.margin()) : 0);
			requiredIntensity = Mth.clamp(requiredIntensity + (level.random.nextBoolean() ? -jump : jump), 0, 10);
		}
	}

	/** Client side, at each reaction tick: the focus' sound, a beep on target or a warning, and some particles. */
	private void reactionTickEffects() {
		BlockPos pos = worldPosition.relative(direction);
		ItemStack focus = inventory.getStackInSlot(SLOT_FOCUS);
		if (focus.is(TGItems.RC_HEAT_RAY)) level.playLocalSound(pos, TGSounds.RC_HEATRAY_WORK.get(), SoundSource.BLOCKS, 1, 1, false);
		else if (focus.is(TGItems.RC_UV_EMITTER)) level.playLocalSound(pos, TGSounds.CHEM_LAB_WORK.get(), SoundSource.BLOCKS, 1, 1, false);
		for (int i = 0; i < 4; i++) {
			level.addParticle(ParticleTypes.EFFECT, pos.getX() + level.random.nextFloat(), pos.getY() + level.random.nextFloat(), pos.getZ() + level.random.nextFloat(), 0, 1, 0);
		}
		level.playLocalSound(pos, (requiredIntensity == intensity ? TGSounds.RC_BEEP : TGSounds.RC_WARNING).get(), SoundSource.BLOCKS, 1, 1, false);
	}

	/** 1.12 explode: {@code medium} blows up the whole glass box, else only the center and its four neighbours go. */
	private void explode(boolean medium) {
		BlockState fluidBlock = tank.getFluid().getFluid().defaultFluidState().createLegacyBlock();
		BlockPos center = worldPosition.relative(direction).above();
		List<BlockPos> remove = new ArrayList<>();
		if (medium) {
			Direction left = direction.getClockWise();
			BlockPos.betweenClosed(worldPosition.relative(left).above(), worldPosition.relative(left.getOpposite()).relative(direction, 2).above(2))
					.forEach(p -> remove.add(p.immutable()));
		} else {
			for (Direction d : Direction.Plane.HORIZONTAL) remove.add(center.relative(d));
		}
		unform();
		level.setBlockAndUpdate(center, medium ? Blocks.AIR.defaultBlockState() : fluidBlock);
		remove.forEach(p -> level.setBlockAndUpdate(p, Blocks.AIR.defaultBlockState()));
		double x = center.getX() + 0.5, y = center.getY() + 0.5, z = center.getZ() + 0.5;
		if (medium) {
			level.explode(null, x, y, z, 4, true, Level.ExplosionInteraction.BLOCK);
			if (!fluidBlock.isAir()) {
				level.setBlockAndUpdate(center, fluidBlock);
				level.setBlockAndUpdate(center.above(), Blocks.AIR.defaultBlockState());
				level.setBlockAndUpdate(center.below(), Blocks.GRAVEL.defaultBlockState());
			}
		} else {
			TGEffects.sendFX((ServerLevel) level, "FragGrenadeExplosion", x, y, z, 1, 50);
			level.playSound(null, x, y, z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.BLOCKS, 4,
					(1 + (level.random.nextFloat() - level.random.nextFloat()) * 0.2f) * 0.7f);
		}
		energy.set(0);
		tank.setFluid(FluidStack.EMPTY);
		ItemStack input = inventory.getStackInSlot(SLOT_INPUT);
		if (!input.isEmpty()) {
			inventory.setStackInSlot(SLOT_INPUT, ItemStack.EMPTY);
			Containers.dropItemStack(level, x, y, z, input);
		}
	}

	@Override
	public void buttonClicked(int id, Player player) {
		switch (id) {
			case BUTTON_LEVEL_UP -> liquidLevel = Math.min(liquidLevel + 1, 10);
			case BUTTON_LEVEL_DOWN -> liquidLevel = Math.max(liquidLevel - 1, 0);
			case BUTTON_INTENSITY_UP -> intensity = Math.min(intensity + 1, 10);
			case BUTTON_INTENSITY_DOWN -> intensity = Math.max(intensity - 1, 0);
			case BUTTON_DUMP -> tank.setFluid(FluidStack.EMPTY);
			default -> {
				super.buttonClicked(id, player);
				return;
			}
		}
		contentsChanged = true;
		needUpdate();
	}

	@Nullable
	@Override
	protected Map<BlockPos, Integer> check(Player player, Direction face) {
		if (face.getAxis().isVertical()) return invalid(player, "techguns.msg.error.multiblockinvalid");
		Direction dir = face.getOpposite(), left = dir.getClockWise(), right = left.getOpposite();
		BlockPos pos = worldPosition, center = pos.relative(dir), top = center.above(3);
		List<BlockPos> glass = new ArrayList<>();
		BlockPos.betweenClosed(pos.relative(left).above(), pos.relative(right).relative(dir, 2).above(2)).forEach(p -> glass.add(p.immutable()));
		Map<BlockPos, Integer> housings = new HashMap<>();
		for (BlockPos p : List.of(pos.relative(left), pos.relative(right), pos.relative(left).relative(dir, 2), pos.relative(right).relative(dir, 2), center,
				pos.relative(left).above(3), pos.relative(right).above(3), pos.relative(left).relative(dir, 2).above(3), pos.relative(right).relative(dir, 2).above(3),
				top.relative(dir), top.relative(face), top.relative(left), top.relative(right))) {
			housings.put(p, HOUSING);
		}
		housings.put(top, ENERGY_CONNECTOR);
		for (BlockPos p : List.of(center.relative(dir), center.relative(left), center.relative(right))) housings.put(p, SIDE_CONNECTOR);
		boolean glassOk = allParts(glass, TGBlocks.REACTION_CHAMBER_GLASS.get());
		if (!(allParts(housings.keySet(), TGBlocks.REACTION_CHAMBER_HOUSING.get()) & glassOk)) return invalid(player, "techguns.msg.error.multiblockinvalid");
		direction = dir;
		glass.forEach(p -> housings.put(p, HOUSING));
		return housings;
	}

	@Nullable
	@Override
	@SuppressWarnings("unchecked")
	public <T> T partCapability(BlockCapability<T, Direction> cap, int type) {
		if (type == SIDE_CONNECTOR && cap == Capabilities.ItemHandler.BLOCK) return (T) automation;
		if (type == SIDE_CONNECTOR && cap == Capabilities.FluidHandler.BLOCK) return (T) tank;
		if (type == ENERGY_CONNECTOR && cap == Capabilities.EnergyStorage.BLOCK) return (T) energy;
		return null;
	}

	@Override
	protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.saveAdditional(tag, registries);
		tag.put("tank", tank.writeToNBT(registries, new CompoundTag()));
		tag.putByte("liquidLevel", (byte) liquidLevel);
		tag.putByte("intensity", (byte) intensity);
		if (reaction != null) tag.putString("reaction", reaction.toString());
		tag.putByte("completion", (byte) completion);
		tag.putByte("requiredIntensity", (byte) requiredIntensity);
		tag.putByte("nextTick", (byte) nextTick);
	}

	@Override
	protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.loadAdditional(tag, registries);
		tank.readFromNBT(registries, tag.getCompound("tank"));
		liquidLevel = tag.getByte("liquidLevel");
		intensity = tag.getByte("intensity");
		reaction = tag.contains("reaction") ? ResourceLocation.tryParse(tag.getString("reaction")) : null;
		completion = tag.getByte("completion");
		requiredIntensity = tag.getByte("requiredIntensity");
		nextTick = tag.getByte("nextTick");
	}
}
