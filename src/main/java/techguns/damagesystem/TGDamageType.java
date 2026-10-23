package techguns.damagesystem;

import javax.annotation.Nullable;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.damagesource.DamageTypes;
import techguns.Techguns;

/** 1.12 DamageType: what armor and shields resist. The order indexes {@link ShieldStats} and armor values. */
public enum TGDamageType {
	PHYSICAL(null), PROJECTILE("bullet"), FIRE("fire"), EXPLOSION("explosion"), ENERGY("energy"), POISON("poison"), UNRESISTABLE("radiation_poisoning"),
	ICE(null), LIGHTNING("lightning"), RADIATION("radiation"), DARK("dark");

	/** The techguns damage type a {@link TGDamageSource} of this kind uses; null where no weapon deals it. */
	@Nullable
	public final ResourceKey<DamageType> key;

	TGDamageType(@Nullable String key) {
		this.key = key == null ? null : ResourceKey.create(Registries.DAMAGE_TYPE, Techguns.id(key));
	}

	/** 1.12 determineTGDamageType: TG sources carry their type, vanilla ones are sorted by tag. */
	public static TGDamageType of(DamageSource source) {
		if (source instanceof TGDamageSource tg) return tg.type;
		if (source.is(DamageTypeTags.IS_EXPLOSION)) return EXPLOSION;
		if (source.is(DamageTypeTags.WITCH_RESISTANT_TO)) return ENERGY; // 1.12 magic damage
		if (source.is(DamageTypeTags.IS_FIRE) || source.is(DamageTypes.DRAGON_BREATH)) return FIRE;
		if (source.is(DamageTypeTags.IS_PROJECTILE)) return PROJECTILE;
		if (source.is(DamageTypes.WITHER)) return POISON;
		if (source.is(DamageTypeTags.IS_LIGHTNING)) return LIGHTNING;
		if (source.is(DamageTypeTags.BYPASSES_ARMOR)) return UNRESISTABLE;
		return PHYSICAL;
	}
}
