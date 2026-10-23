package techguns.recipes;

import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.crafting.SizedIngredient;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import techguns.Techguns;

/**
 * One recipe of a single-block machine (1.12 MetalPressRecipes, ChemLabRecipes, GrinderRecipes...), one json format for all
 * of them; the recipe type says which machine. See PORTING.md for the format.
 * {@code inputs} go into the machine's input slots in order (slots past them must be empty; {@code swap} also accepts the
 * first two the other way round), {@code bottle} is the chem lab's container slot, {@code fluid} its input tank.
 * {@code chances} (grinder) scale each result's count; {@code power} is FE/t (0: the machine's default), {@code time} ticks.
 */
public record MachineRecipe(Kind kind, List<SizedIngredient> inputs, Optional<SizedIngredient> bottle, Optional<SizedFluidIngredient> fluid,
		List<ItemStack> results, List<Float> chances, Optional<FluidStack> fluidResult, int power, int time, boolean swap)
		implements Recipe<MachineRecipe.Input> {

	public static final DeferredRegister<RecipeType<?>> TYPES = DeferredRegister.create(Registries.RECIPE_TYPE, Techguns.MODID);

	public static final Kind AMMO_PRESS = new Kind("ammo_press");
	public static final Kind METAL_PRESS = new Kind("metal_press");
	public static final Kind GRINDER = new Kind("grinder");
	public static final Kind BLAST_FURNACE = new Kind("blast_furnace");
	public static final Kind CHARGING_STATION = new Kind("charging_station");
	public static final Kind CHEM_LAB = new Kind("chem_lab");

	/** A machine's recipe type and serializer, both named after it. */
	public static final class Kind {
		public final DeferredHolder<RecipeType<?>, RecipeType<MachineRecipe>> type;
		public final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<MachineRecipe>> serializer;

		Kind(String name) {
			type = TYPES.register(name, () -> RecipeType.simple(Techguns.id(name)));
			MapCodec<MachineRecipe> codec = codec(this);
			StreamCodec<RegistryFriendlyByteBuf, MachineRecipe> stream = ByteBufCodecs.fromCodecWithRegistries(codec.codec());
			serializer = AmmoChangeRecipe.SERIALIZERS.register(name, () -> new RecipeSerializer<>() {
				@Override
				public MapCodec<MachineRecipe> codec() {
					return codec;
				}

				@Override
				public StreamCodec<RegistryFriendlyByteBuf, MachineRecipe> streamCodec() {
					return stream;
				}
			});
		}
	}

	private static MapCodec<MachineRecipe> codec(Kind kind) {
		return RecordCodecBuilder.mapCodec(i -> i.group(
				SizedIngredient.FLAT_CODEC.listOf().optionalFieldOf("inputs", List.of()).forGetter(MachineRecipe::inputs),
				SizedIngredient.FLAT_CODEC.optionalFieldOf("bottle").forGetter(MachineRecipe::bottle),
				SizedFluidIngredient.FLAT_CODEC.optionalFieldOf("fluid").forGetter(MachineRecipe::fluid),
				ItemStack.CODEC.listOf().optionalFieldOf("results", List.of()).forGetter(MachineRecipe::results),
				Codec.FLOAT.listOf().optionalFieldOf("chances", List.of()).forGetter(MachineRecipe::chances),
				FluidStack.CODEC.optionalFieldOf("fluid_result").forGetter(MachineRecipe::fluidResult),
				Codec.INT.optionalFieldOf("power", 0).forGetter(MachineRecipe::power),
				Codec.INT.optionalFieldOf("time", 100).forGetter(MachineRecipe::time),
				Codec.BOOL.optionalFieldOf("swap", false).forGetter(MachineRecipe::swap))
				.apply(i, (in, b, f, r, c, fr, p, t, s) -> new MachineRecipe(kind, in, b, f, r, c, fr, p, t, s)));
	}

	/** The machine's input slots, its bottle slot (or empty) and input tank (or empty). */
	public record Input(List<ItemStack> items, ItemStack bottle, FluidStack fluid) implements RecipeInput {
		@Override
		public ItemStack getItem(int index) {
			return items.get(index);
		}

		@Override
		public int size() {
			return items.size();
		}
	}

	/** -1 if {@code in} doesn't hold {@code mult} times this recipe, 1 if it does with the first two slots swapped, else 0. */
	public int match(Input in, int mult) {
		if (!bottle.map(b -> test(b, in.bottle, mult)).orElse(in.bottle.isEmpty())) return -1;
		if (fluid.isPresent() && !(fluid.get().ingredient().test(in.fluid) && in.fluid.getAmount() >= fluid.get().amount() * mult)) return -1;
		for (int swapped = 0; swapped <= (swap ? 1 : 0); swapped++) {
			if (matchItems(in.items, swapped == 1, mult)) return swapped;
		}
		return -1;
	}

	private boolean matchItems(List<ItemStack> items, boolean swapped, int mult) {
		for (int i = 0; i < items.size(); i++) {
			ItemStack stack = items.get(swapped && i < 2 ? 1 - i : i);
			if (i < inputs.size() ? !test(inputs.get(i), stack, mult) : !stack.isEmpty()) return false;
		}
		return true;
	}

	private static boolean test(SizedIngredient ingredient, ItemStack stack, int mult) {
		return ingredient.ingredient().test(stack) && stack.getCount() >= ingredient.count() * mult;
	}

	/** The results of {@code mult} runs; a result with a chance gets count * chance * mult, the fraction as a random extra one. */
	public List<ItemStack> rollResults(int mult, RandomSource random) {
		return IntStream.range(0, results.size()).mapToObj(i -> {
			ItemStack stack = results.get(i);
			if (i >= chances.size()) return stack.copyWithCount(stack.getCount() * mult);
			float amount = stack.getCount() * chances.get(i) * mult;
			int count = Mth.floor(amount);
			return stack.copyWithCount(count + (random.nextFloat() < amount - count ? 1 : 0));
		}).filter(s -> !s.isEmpty()).toList();
	}

	@Override
	public boolean matches(Input input, Level level) {
		return match(input, 1) >= 0;
	}

	@Override
	public ItemStack assemble(Input input, HolderLookup.Provider registries) {
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

	/** Keeps them out of the recipe book. */
	@Override
	public boolean isSpecial() {
		return true;
	}

	@Override
	public RecipeSerializer<?> getSerializer() {
		return kind.serializer.get();
	}

	@Override
	public RecipeType<?> getType() {
		return kind.type.get();
	}
}
