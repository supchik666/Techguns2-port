package techguns.items.guns.ammo;

import static techguns.entities.projectiles.GenericProjectile.Kind.*;
import static techguns.items.guns.ammo.AmmoType.*;

import techguns.TGItems;
import techguns.entities.projectiles.GenericProjectile;

/** As in 1.12 AmmoTypes, with each variant's projectile from the 1.12 TGuns projectile selectors. */
public class AmmoTypes {
	public static final AmmoType STONE_BULLETS = new AmmoType(TGItems.STONE_BULLETS, STONE);
	public static final AmmoType PISTOL_ROUNDS = new AmmoType(TGItems.PISTOL_ROUNDS, BULLET)
			.variant(INCENDIARY, TGItems.PISTOL_ROUNDS_INCENDIARY, INCENDIARY_BULLET);
	public static final AmmoType RIFLE_ROUNDS = new AmmoType(TGItems.RIFLE_ROUNDS, BULLET)
			.variant(INCENDIARY, TGItems.RIFLE_ROUNDS_INCENDIARY, INCENDIARY_BULLET);
	public static final AmmoType SHOTGUN_ROUNDS = new AmmoType(TGItems.SHOTGUN_ROUNDS, BULLET)
			.variant(INCENDIARY, TGItems.SHOTGUN_ROUNDS_INCENDIARY, INCENDIARY_SHELL);

	public static final AmmoType SMG_MAGAZINE = new AmmoType(TGItems.SMG_MAGAZINE, TGItems.SMG_MAGAZINE_EMPTY, TGItems.PISTOL_ROUNDS, 2, BULLET)
			.variant(INCENDIARY, TGItems.SMG_MAGAZINE_INCENDIARY, TGItems.PISTOL_ROUNDS_INCENDIARY, INCENDIARY_BULLET);
	public static final AmmoType ASSAULT_RIFLE_MAGAZINE = new AmmoType(TGItems.ASSAULTRIFLE_MAGAZINE, TGItems.ASSAULTRIFLE_MAGAZINE_EMPTY,
			TGItems.RIFLE_ROUNDS, 3, BULLET)
			.variant(INCENDIARY, TGItems.ASSAULTRIFLE_MAGAZINE_INCENDIARY, TGItems.RIFLE_ROUNDS_INCENDIARY, INCENDIARY_BULLET);
	public static final AmmoType PISTOL_MAGAZINE = new AmmoType(TGItems.PISTOL_MAGAZINE, TGItems.PISTOL_MAGAZINE_EMPTY, TGItems.PISTOL_ROUNDS, 3, BULLET)
			.variant(INCENDIARY, TGItems.PISTOL_MAGAZINE_INCENDIARY, TGItems.PISTOL_ROUNDS_INCENDIARY, INCENDIARY_BULLET);
	public static final AmmoType LMG_MAGAZINE = new AmmoType(TGItems.LMG_MAGAZINE, TGItems.LMG_MAGAZINE_EMPTY, TGItems.RIFLE_ROUNDS, 8, BULLET)
			.variant(INCENDIARY, TGItems.LMG_MAGAZINE_INCENDIARY, TGItems.RIFLE_ROUNDS_INCENDIARY, INCENDIARY_BULLET);
	public static final AmmoType MINIGUN_AMMO_DRUM = new AmmoType(TGItems.MINIGUN_DRUM, TGItems.MINIGUN_DRUM_EMPTY, TGItems.RIFLE_ROUNDS, 16, BULLET)
			.variant(INCENDIARY, TGItems.MINIGUN_DRUM_INCENDIARY, TGItems.RIFLE_ROUNDS_INCENDIARY, INCENDIARY_BULLET);
	public static final AmmoType ADVANCED_MAGAZINE = new AmmoType(TGItems.ADVANCED_MAGAZINE, TGItems.ADVANCED_MAGAZINE_EMPTY,
			TGItems.ADVANCED_ROUNDS, 3, ADVANCED);
	public static final AmmoType AS50_MAGAZINE = new AmmoType(TGItems.AS50_MAGAZINE, TGItems.AS50_MAGAZINE_EMPTY, TGItems.SNIPER_ROUNDS, 2, BULLET)
			.variant(INCENDIARY, TGItems.AS50_MAGAZINE_INCENDIARY, TGItems.SNIPER_ROUNDS_INCENDIARY, INCENDIARY_BULLET)
			.variant(EXPLOSIVE, TGItems.AS50_MAGAZINE_EXPLOSIVE, TGItems.SNIPER_ROUNDS_EXPLOSIVE, EXPLOSIVE_BULLET);

	// Energy and fuel: the empty comes back, nothing else; the gun's projectile class does the rest.
	public static final AmmoType ENERGY_CELL = new AmmoType(TGItems.ENERGY_CELL, TGItems.ENERGY_CELL_EMPTY, null, 0, BULLET);
	public static final AmmoType REDSTONE_BATTERY = new AmmoType(TGItems.REDSTONE_BATTERY, TGItems.REDSTONE_BATTERY_EMPTY, null, 0, BULLET);
	public static final AmmoType NUCLEAR_POWER_CELL = new AmmoType(TGItems.NUCLEAR_POWERCELL, TGItems.NUCLEAR_POWERCELL_EMPTY, null, 0, BULLET);
	public static final AmmoType FUEL_TANK = new AmmoType(TGItems.FUEL_TANK, TGItems.FUEL_TANK_EMPTY, null, 0, BULLET);
	public static final AmmoType NETHER_CHARGE = new AmmoType(TGItems.NETHER_CHARGE, BULLET);
	public static final AmmoType BIO_TANK = new AmmoType(TGItems.BIO_TANK, TGItems.BIO_TANK_EMPTY, null, 0, BULLET);

	// Explosives: the gun's projectile class does the explosion; variants only change its kind.
	public static final AmmoType ROCKETS = new AmmoType(TGItems.ROCKET, BULLET)
			.variant(AmmoType.NUKE, TGItems.ROCKET_NUKE, GenericProjectile.Kind.NUKE)
			.variant(AmmoType.HIGH_VELOCITY, TGItems.ROCKET_HIGH_VELOCITY, GenericProjectile.Kind.HIGH_VELOCITY);
	public static final AmmoType ROCKETS_NO_NUKES = new AmmoType(TGItems.ROCKET, BULLET)
			.variant(AmmoType.HIGH_VELOCITY, TGItems.ROCKET_HIGH_VELOCITY, GenericProjectile.Kind.HIGH_VELOCITY);
	public static final AmmoType GRENADES_40MM = new AmmoType(TGItems.GRENADE_40MM, BULLET);
	public static final AmmoType AMMO_GAUSS_RIFLE = new AmmoType(TGItems.GAUSSRIFLE_SLUGS, BULLET).extra(TGItems.ENERGY_CELL, TGItems.ENERGY_CELL_EMPTY);
}
