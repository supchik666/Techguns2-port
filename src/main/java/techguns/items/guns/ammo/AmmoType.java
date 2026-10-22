package techguns.items.guns.ammo;

import java.util.function.Supplier;

import net.minecraft.world.item.Item;

/**
 * What a gun reloads from. {@code bulletsPerMag} is how many {@code bullet} items one magazine is crafted from;
 * a forced reload hands the leftover shots back as bullets.
 * ponytail: 1.12 also had ammo variants (incendiary, explosive...); add a variant map with ammo switching.
 */
public record AmmoType(Supplier<? extends Item> ammo, Supplier<? extends Item> emptyMag, Supplier<? extends Item> bullet, int bulletsPerMag) {
	public float getShotsPerBullet(int clipsize, int ammoCount) {
		if (ammoCount == 1 && bulletsPerMag == 0) return clipsize;
		float bpm = bulletsPerMag == 0 ? (float) clipsize / ammoCount : bulletsPerMag;
		return clipsize / bpm;
	}
}
