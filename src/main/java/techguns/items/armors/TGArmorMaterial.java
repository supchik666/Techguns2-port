package techguns.items.armors;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.function.Supplier;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.registries.DeferredRegister;
import techguns.Techguns;
import techguns.damagesystem.TGDamageType;

/**
 * 1.12 TGArmorMaterial: full-set armor values per damage type, split .25/.30/.25/.20 over the pieces. The registered
 * vanilla material carries the rounded physical values (the armor bar), toughness, equip sound and anvil repair item.
 */
public class TGArmorMaterial {
	public static final DeferredRegister<ArmorMaterial> MATERIALS = DeferredRegister.create(Registries.ARMOR_MATERIAL, Techguns.MODID);
	private static final EnumMap<ArmorItem.Type, Float> SPLIT = new EnumMap<>(ArmorItem.Type.class);

	static {
		SPLIT.put(ArmorItem.Type.HELMET, 0.25f);
		SPLIT.put(ArmorItem.Type.CHESTPLATE, 0.30f);
		SPLIT.put(ArmorItem.Type.LEGGINGS, 0.25f);
		SPLIT.put(ArmorItem.Type.BOOTS, 0.20f);
	}

	/** Full-set values by {@link TGDamageType} ordinal. */
	private final float[] armor = new float[TGDamageType.values().length];
	/** Per piece, as in 1.12 (0.25 x the vanilla 55 x base durability). */
	public final int durability;
	public final float toughness;
	public final Holder<ArmorMaterial> material;

	public TGArmorMaterial(String name, int baseDurability, float armor, Holder<SoundEvent> sound, float toughness, Supplier<? extends Item> repair) {
		Arrays.fill(this.armor, armor * 0.75f);
		set(TGDamageType.PHYSICAL, armor).set(TGDamageType.PROJECTILE, armor).set(TGDamageType.RADIATION, 0).set(TGDamageType.UNRESISTABLE, 0);
		this.durability = Math.round(0.25f * 55 * baseDurability);
		this.toughness = toughness;
		EnumMap<ArmorItem.Type, Integer> defense = new EnumMap<>(ArmorItem.Type.class);
		SPLIT.forEach((type, split) -> defense.put(type, Math.round(armor * split)));
		material = MATERIALS.register(name, () -> new ArmorMaterial(defense, 0, sound, () -> Ingredient.of(repair.get()),
				List.of(new ArmorMaterial.Layer(Techguns.id(name))), toughness, 0));
	}

	public TGArmorMaterial set(TGDamageType type, float value) {
		armor[type.ordinal()] = value;
		return this;
	}

	/** 1.12 setArmorElemental. */
	public TGArmorMaterial elemental(float value) {
		return set(TGDamageType.ENERGY, value).set(TGDamageType.FIRE, value).set(TGDamageType.ICE, value).set(TGDamageType.LIGHTNING, value);
	}

	/** Fire, explosion, energy, ice, lightning, poison and radiation, as the 1.12 setter chains list them. */
	public TGArmorMaterial resist(float fire, float explosion, float energy, float ice, float lightning, float poison, float radiation) {
		return set(TGDamageType.FIRE, fire).set(TGDamageType.EXPLOSION, explosion).set(TGDamageType.ENERGY, energy).set(TGDamageType.ICE, ice)
				.set(TGDamageType.LIGHTNING, lightning).set(TGDamageType.POISON, poison).set(TGDamageType.RADIATION, radiation);
	}

	public float armorValue(ArmorItem.Type slot, TGDamageType type) {
		return armor[type.ordinal()] * SPLIT.getOrDefault(slot, 0f);
	}
}
