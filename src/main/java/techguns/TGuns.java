package techguns;

import net.neoforged.neoforge.registries.DeferredItem;
import techguns.items.guns.GenericGun;
import techguns.items.guns.ammo.AmmoTypes;

/** Gun definitions, numbers as in 1.12 TGuns. */
public class TGuns {
	public static final int MAX_RANGE_PISTOL = 40;

	public static final DeferredItem<GenericGun> PISTOL = TGItems.ITEMS.register("pistol",
			() -> new GenericGun(AmmoTypes.PISTOL_MAGAZINE, true, 4, 18, 35, 8.0f, TGSounds.PISTOL_FIRE, TGSounds.PISTOL_RELOAD,
					MAX_RANGE_PISTOL, 0.025f).setBulletSpeed(2.0f).setDamageDrop(18, 25, 5.0f).setRecoiltime(3));

	/** Loads the class so the guns above get registered. */
	static void init() {}
}
