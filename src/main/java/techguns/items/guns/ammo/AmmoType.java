package techguns.items.guns.ammo;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import net.minecraft.world.item.Item;
import techguns.entities.projectiles.GenericProjectile;

/**
 * What a gun reloads from. Each variant (default, incendiary...) has its own magazine or rounds, the bullet item they're
 * crafted from and the projectile it fires; the empty magazine is shared. {@code bulletsPerMag} is how many bullets one
 * magazine is crafted from, so a forced reload can hand the leftover shots back as bullets.
 */
public class AmmoType {
	public static final String DEFAULT = "default";
	public static final String INCENDIARY = "incendiary";
	public static final String EXPLOSIVE = "explosive";

	public record Variant(Supplier<? extends Item> ammo, Supplier<? extends Item> bullet, GenericProjectile.Kind kind) {}

	private final Map<String, Variant> variants = new LinkedHashMap<>();
	/** Null for loose rounds, which go in one at a time. */
	private final Supplier<? extends Item> emptyMag;
	private final int bulletsPerMag;

	public AmmoType(Supplier<? extends Item> ammo, Supplier<? extends Item> emptyMag, Supplier<? extends Item> bullet, int bulletsPerMag,
			GenericProjectile.Kind kind) {
		this.emptyMag = emptyMag;
		this.bulletsPerMag = bulletsPerMag;
		variants.put(DEFAULT, new Variant(ammo, bullet, kind));
	}

	/** Loose rounds: the ammo item is its own bullet. */
	public AmmoType(Supplier<? extends Item> ammo, GenericProjectile.Kind kind) {
		this(ammo, null, ammo, 0, kind);
	}

	public AmmoType variant(String key, Supplier<? extends Item> ammo, Supplier<? extends Item> bullet, GenericProjectile.Kind kind) {
		variants.put(key, new Variant(ammo, bullet, kind));
		return this;
	}

	public AmmoType variant(String key, Supplier<? extends Item> ammo, GenericProjectile.Kind kind) {
		return variant(key, ammo, ammo, kind);
	}

	/** Unknown keys fall back to the default variant, as in 1.12. */
	public Variant get(String key) {
		return variants.getOrDefault(key, variants.get(DEFAULT));
	}

	public Set<String> keys() {
		return variants.keySet();
	}

	public Supplier<? extends Item> emptyMag() {
		return emptyMag;
	}

	public float getShotsPerBullet(int clipsize, int ammoCount) {
		if (ammoCount == 1 && bulletsPerMag == 0) return clipsize;
		float bpm = bulletsPerMag == 0 ? (float) clipsize / ammoCount : bulletsPerMag;
		return clipsize / bpm;
	}
}
