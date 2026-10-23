package techguns.recipes;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.registries.DeferredHolder;
import techguns.Techguns;

/**
 * 1.12 UpgradeBenchRecipe: the upgrade item that puts {@code enchantment} at {@code level} on TG armor in the upgrade
 * bench. The input is the upgrade item; the bench checks the armor and works out the result and its XP cost.
 * Json: {@code {"type": "techguns:upgrade_bench", "upgrade": <ingredient>, "enchantment": "minecraft:protection", "level": 1}}.
 */
public record UpgradeBenchRecipe(Ingredient upgrade, Holder<Enchantment> enchantment, int level) implements Recipe<SingleRecipeInput> {
	public static final DeferredHolder<RecipeType<?>, RecipeType<UpgradeBenchRecipe>> TYPE = MachineRecipe.TYPES.register("upgrade_bench",
			() -> RecipeType.simple(Techguns.id("upgrade_bench")));
	private static final MapCodec<UpgradeBenchRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Ingredient.CODEC_NONEMPTY.fieldOf("upgrade").forGetter(UpgradeBenchRecipe::upgrade),
			Enchantment.CODEC.fieldOf("enchantment").forGetter(UpgradeBenchRecipe::enchantment),
			Codec.intRange(1, 255).fieldOf("level").forGetter(UpgradeBenchRecipe::level))
			.apply(i, UpgradeBenchRecipe::new));
	private static final StreamCodec<RegistryFriendlyByteBuf, UpgradeBenchRecipe> STREAM_CODEC = ByteBufCodecs.fromCodecWithRegistries(CODEC.codec());
	public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<UpgradeBenchRecipe>> SERIALIZER = AmmoChangeRecipe.SERIALIZERS.register(
			"upgrade_bench", () -> new RecipeSerializer<>() {
				@Override
				public MapCodec<UpgradeBenchRecipe> codec() {
					return CODEC;
				}

				@Override
				public StreamCodec<RegistryFriendlyByteBuf, UpgradeBenchRecipe> streamCodec() {
					return STREAM_CODEC;
				}
			});

	/** Called from the mod constructor so the type and serializer register. */
	public static void init() {}

	@Override
	public boolean matches(SingleRecipeInput input, Level level) {
		return upgrade.test(input.item());
	}

	@Override
	public ItemStack assemble(SingleRecipeInput input, HolderLookup.Provider registries) {
		return ItemStack.EMPTY;
	}

	@Override
	public boolean canCraftInDimensions(int width, int height) {
		return true;
	}

	@Override
	public ItemStack getResultItem(HolderLookup.Provider registries) {
		return ItemStack.EMPTY;
	}

	@Override
	public boolean isSpecial() {
		return true;
	}

	@Override
	public RecipeSerializer<?> getSerializer() {
		return SERIALIZER.get();
	}

	@Override
	public RecipeType<?> getType() {
		return TYPE.get();
	}
}
