package techguns.damagesystem;

import java.util.function.Supplier;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import techguns.TGSounds;
import techguns.items.armors.GenericShield;

/** 1.12 ShieldStats: how much of each damage type a blocking shield stops, and the sound of a blocked hit. */
public class ShieldStats {
	public static final ShieldStats VANILLA = new ShieldStats(TGSounds.BULLET_IMPACT_WOOD, 0, 1, .35f, .35f, .35f, .35f, .35f, 0, .35f, .35f, 0, 0);
	public static final ShieldStats DEFAULT = new ShieldStats(TGSounds.BULLET_IMPACT_METAL, 0, 1, .5f, .5f, .5f, .5f, .5f, 0, .5f, .5f, .25f, .25f);
	public static final ShieldStats RIOT = new ShieldStats(TGSounds.BULLET_IMPACT_GLASS, 0, 1, .65f, .6f, .6f, .6f, .6f, 0, .6f, .6f, .4f, .4f);
	public static final ShieldStats BALLISTIC = new ShieldStats(TGSounds.BULLET_IMPACT_METAL, 1.5f, 1, .8f, .7f, .7f, .7f, .7f, 0, .7f, .7f, .6f, .6f);
	public static final ShieldStats ADVANCED = new ShieldStats(TGSounds.BULLET_IMPACT_METAL, 3, 1, .9f, .8f, .8f, .8f, .8f, 0, .8f, .8f, .75f, .75f);

	public final Supplier<SoundEvent> sound;
	/** Flat damage threshold, lowered by twice the penetration. */
	private final float dt;
	/** Fraction stopped, by {@link TGDamageType} ordinal. */
	private final float[] rates;

	private ShieldStats(Supplier<SoundEvent> sound, float dt, float... rates) {
		this.sound = sound;
		this.dt = dt;
		this.rates = rates;
	}

	public static ShieldStats of(ItemStack shield) {
		if (shield.getItem() instanceof GenericShield s) return s.stats;
		return shield.is(Items.SHIELD) ? VANILLA : DEFAULT;
	}

	/** The damage that passes the shield. */
	public float passThrough(float amount, TGDamageSource source) {
		return Math.max(amount * (1 - rate(source.type)) - Math.max(dt - source.penetration * 2, 0), 0);
	}

	public float rate(TGDamageType type) {
		return rates[type.ordinal()];
	}
}
