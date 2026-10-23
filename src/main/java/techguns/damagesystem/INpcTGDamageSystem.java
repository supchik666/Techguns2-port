package techguns.damagesystem;

/** An entity with its own armor per damage type (1.12 INpcTGDamageSystem); {@link DamageSystem} uses it against all damage. */
public interface INpcTGDamageSystem {
	float getTotalArmorAgainstType(TGDamageType type);
}
