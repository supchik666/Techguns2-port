package techguns.recipes;

import java.util.List;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.crafting.FluidIngredient;
import techguns.machines.multiblock.MultiBlocks;

/**
 * A reaction chamber recipe (1.12 ReactionChamberRecipe); see PORTING.md for the json. It starts with one {@code input}
 * item, the {@code focus} in the focus slot, exactly {@code liquid_level} buckets of {@code fluid} with the level set to
 * match, and the intensity at {@code intensity}. Every reaction tick (60 ticks) takes {@code power} FE and, if the focus,
 * intensity and level still match, adds one completion; {@code required_completion} of them within {@code ticks} reaction
 * ticks gives the results and uses up {@code liquid_consumption} mB. Each reaction tick the wanted intensity may drift
 * (chance {@code instability}, by 1 + up to {@code margin} - 1). Running out of time fails with {@code risk}.
 */
public record ReactionChamberRecipe(Ingredient input, Item focus, FluidIngredient fluid, List<ItemStack> results, int ticks, int requiredCompletion,
		int intensity, int margin, int liquidLevel, int liquidConsumption, float instability, Risk risk, int power) implements Recipe<SingleRecipeInput> {

	/** What a failed reaction does: nothing besides losing the input, or blowing up the chamber. */
	public enum Risk implements StringRepresentable {
		NONE, BREAK_ITEM, EXPLOSION_LOW, EXPLOSION_MEDIUM;

		public static final Codec<Risk> CODEC = StringRepresentable.fromEnum(Risk::values);

		@Override
		public String getSerializedName() {
			return name().toLowerCase();
		}

		/** Whether the chamber stops after failing; it starts the next reaction otherwise. */
		public boolean fatal() {
			return this == BREAK_ITEM;
		}
	}

	public static final MapCodec<ReactionChamberRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Ingredient.CODEC_NONEMPTY.fieldOf("input").forGetter(ReactionChamberRecipe::input),
			BuiltInRegistries.ITEM.byNameCodec().fieldOf("focus").forGetter(ReactionChamberRecipe::focus),
			FluidIngredient.CODEC_NON_EMPTY.fieldOf("fluid").forGetter(ReactionChamberRecipe::fluid),
			ItemStack.CODEC.listOf().fieldOf("results").forGetter(ReactionChamberRecipe::results),
			Codec.INT.fieldOf("ticks").forGetter(ReactionChamberRecipe::ticks),
			Codec.INT.fieldOf("required_completion").forGetter(ReactionChamberRecipe::requiredCompletion),
			Codec.intRange(0, 10).fieldOf("intensity").forGetter(ReactionChamberRecipe::intensity),
			Codec.INT.optionalFieldOf("margin", 0).forGetter(ReactionChamberRecipe::margin),
			Codec.intRange(0, 10).fieldOf("liquid_level").forGetter(ReactionChamberRecipe::liquidLevel),
			Codec.INT.optionalFieldOf("liquid_consumption", 0).forGetter(ReactionChamberRecipe::liquidConsumption),
			Codec.FLOAT.optionalFieldOf("instability", 0f).forGetter(ReactionChamberRecipe::instability),
			Risk.CODEC.optionalFieldOf("risk", Risk.NONE).forGetter(ReactionChamberRecipe::risk),
			Codec.INT.fieldOf("power").forGetter(ReactionChamberRecipe::power)).apply(i, ReactionChamberRecipe::new));
	public static final StreamCodec<RegistryFriendlyByteBuf, ReactionChamberRecipe> STREAM_CODEC = ByteBufCodecs.fromCodecWithRegistries(CODEC.codec());

	@Override
	public boolean matches(SingleRecipeInput input, Level level) {
		return this.input.test(input.item());
	}

	@Override
	public ItemStack assemble(SingleRecipeInput input, HolderLookup.Provider registries) {
		return getResultItem(registries).copy();
	}

	@Override
	public boolean canCraftInDimensions(int width, int height) {
		return true;
	}

	@Override
	public ItemStack getResultItem(HolderLookup.Provider registries) {
		return results.isEmpty() ? ItemStack.EMPTY : results.getFirst();
	}

	@Override
	public boolean isSpecial() {
		return true;
	}

	@Override
	public RecipeSerializer<?> getSerializer() {
		return MultiBlocks.REACTION_CHAMBER_SERIALIZER.get();
	}

	@Override
	public RecipeType<?> getType() {
		return MultiBlocks.REACTION_CHAMBER_RECIPE.get();
	}
}
