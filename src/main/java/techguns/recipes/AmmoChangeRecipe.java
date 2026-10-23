package techguns.recipes;

import java.util.function.Function;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import techguns.TGComponents;
import techguns.Techguns;
import techguns.items.guns.GenericGun;
import techguns.items.guns.ammo.AmmoType;

/**
 * Gun + magazine (or rounds) of another variant: the same gun, set to that variant and loaded (1.12 AmmoSwitchRecipe).
 * The json is a plain shapeless recipe.
 */
public class AmmoChangeRecipe extends ShapelessRecipe {
	public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS = DeferredRegister.create(Registries.RECIPE_SERIALIZER, Techguns.MODID);
	public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<AmmoChangeRecipe>> SERIALIZER = SERIALIZERS.register("ammo_change",
			() -> shapeless(AmmoChangeRecipe::new));
	public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<MiningToolUpgradeRecipe>> MININGTOOL_UPGRADE = SERIALIZERS.register(
			"miningtool_upgrade", () -> shapeless(MiningToolUpgradeRecipe::new));

	/** A serializer reading a plain shapeless recipe json into a subclass that only changes matching or the result. */
	static <T extends ShapelessRecipe> RecipeSerializer<T> shapeless(Function<ShapelessRecipe, T> factory) {
		MapCodec<T> codec = RecipeSerializer.SHAPELESS_RECIPE.codec().xmap(factory, r -> r);
		StreamCodec<RegistryFriendlyByteBuf, T> streamCodec = RecipeSerializer.SHAPELESS_RECIPE.streamCodec().map(factory, r -> r);
		return new RecipeSerializer<>() {
			@Override
			public MapCodec<T> codec() {
				return codec;
			}

			@Override
			public StreamCodec<RegistryFriendlyByteBuf, T> streamCodec() {
				return streamCodec;
			}
		};
	}

	AmmoChangeRecipe(ShapelessRecipe recipe) {
		super(recipe.getGroup(), recipe.category(), recipe.getResultItem(null), recipe.getIngredients());
	}

	@Override
	public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
		ItemStack gunStack = ItemStack.EMPTY;
		ItemStack ammo = ItemStack.EMPTY;
		for (ItemStack stack : input.items()) {
			if (stack.getItem() instanceof GenericGun) {
				gunStack = stack;
			} else if (!stack.isEmpty()) {
				ammo = stack;
			}
		}
		if (!(gunStack.getItem() instanceof GenericGun gun)) return super.assemble(input, registries);
		AmmoType type = gun.getAmmoType();
		String variant = AmmoType.DEFAULT;
		for (String key : type.keys()) {
			if (type.get(key).ammo().get() == ammo.getItem()) variant = key;
		}
		ItemStack out = gunStack.copyWithCount(1);
		out.set(TGComponents.AMMO_VARIANT, variant);
		out.set(TGComponents.AMMO, gun.getAmmoCount() > 1 ? 1 : gun.getClipsize());
		return out;
	}

	@Override
	public RecipeSerializer<?> getSerializer() {
		return SERIALIZER.get();
	}
}
