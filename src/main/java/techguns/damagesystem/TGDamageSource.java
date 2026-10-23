package techguns.damagesystem;

import javax.annotation.Nullable;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import techguns.Techguns;

/**
 * 1.12 TGDamageSource: a damage type carrying its TG type, armor penetration and death effect. Knockback is done by the
 * weapon (all TG damage types are no_knockback). The death type plays on a kill with {@link #goreChance} ({@link DeathEffects}).
 */
public class TGDamageSource extends DamageSource {
	/** The sonic shotgun's own death message on physical damage. */
	public static final ResourceKey<DamageType> SONIC = ResourceKey.create(Registries.DAMAGE_TYPE, Techguns.id("sonic"));

	public enum DeathType {
		DEFAULT, GORE, DISMEMBER, BIO, LASER
	}

	public final TGDamageType type;
	public final DeathType deathType;
	/** Armor points ignored, in 1.12 gun units (x4 against toughness). */
	public float penetration;
	/** Chance that a kill plays {@link #deathType}. */
	public float goreChance = 0.5f;

	public TGDamageSource(TGDamageType type, DeathType deathType, Entity direct, @Nullable Entity causing) {
		this(type.key, type, deathType, direct, causing);
	}

	public TGDamageSource(ResourceKey<DamageType> key, TGDamageType type, DeathType deathType, Entity direct, @Nullable Entity causing) {
		super(direct.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(key), direct, causing);
		this.type = type;
		this.deathType = deathType;
	}
}
