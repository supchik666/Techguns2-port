package techguns;

import static techguns.items.armors.TGArmorBonus.*;

import java.util.function.Function;
import java.util.function.Supplier;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem.Type;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import techguns.damagesystem.ShieldStats;
import techguns.damagesystem.TGDamageType;
import techguns.items.armors.GenericArmor;
import techguns.items.armors.GenericShield;
import techguns.items.armors.PoweredArmor;
import techguns.items.armors.PoweredArmor.PowerType;
import techguns.items.armors.TGArmorMaterial;

/** 1.12 TGArmors: the armor sets (ids [set]_helmet/_chestplate/_leggings/_boots) and shields, with their 1.12 stats. */
public class TGArmors {
	static final TGArmorMaterial T1_COMBAT = new TGArmorMaterial("t1_combat", 60, 15, SoundEvents.ARMOR_EQUIP_LEATHER, 0.5f, () -> Items.IRON_INGOT);
	static final TGArmorMaterial T1_SCOUT = new TGArmorMaterial("t1_scout", 60, 13, SoundEvents.ARMOR_EQUIP_LEATHER, 0, TGItems.HEAVY_CLOTH);
	static final TGArmorMaterial T1_MINER = new TGArmorMaterial("t1_miner", 60, 13, SoundEvents.ARMOR_EQUIP_LEATHER, 0, () -> Items.IRON_INGOT);
	static final TGArmorMaterial T1_STEAM = new TGArmorMaterial("t1_steam", 200, 19, SoundEvents.ARMOR_EQUIP_IRON, 1, TGItems.STEAMARMOR_PLATE)
			.elemental(17).set(TGDamageType.EXPLOSION, 18).set(TGDamageType.POISON, 10).set(TGDamageType.RADIATION, 6);
	static final TGArmorMaterial T2_HAZMAT = new TGArmorMaterial("t2_hazmat", 80, 10, SoundEvents.ARMOR_EQUIP_LEATHER, 0, TGItems.PROTECTIVE_FIBER)
			.elemental(16).set(TGDamageType.EXPLOSION, 10).set(TGDamageType.POISON, 20).set(TGDamageType.RADIATION, 20);
	static final TGArmorMaterial T2_COMBAT = new TGArmorMaterial("t2_combat", 72, 18, SoundEvents.ARMOR_EQUIP_LEATHER, 1, TGItems.INGOT_OBSIDIAN_STEEL);
	static final TGArmorMaterial T2_COMMANDO = new TGArmorMaterial("t2_commando", 72, 18, SoundEvents.ARMOR_EQUIP_LEATHER, 1, TGItems.INGOT_OBSIDIAN_STEEL)
			.elemental(16).set(TGDamageType.EXPLOSION, 16).set(TGDamageType.POISON, 10).set(TGDamageType.RADIATION, 5);
	static final TGArmorMaterial T2_RIOT = new TGArmorMaterial("t2_riot", 96, 18.5f, SoundEvents.ARMOR_EQUIP_LEATHER, 1.5f, TGItems.INGOT_OBSIDIAN_STEEL)
			.resist(18, 18, 18, 17, 17, 16, 16);
	static final TGArmorMaterial T3_COMBAT = new TGArmorMaterial("t3_combat", 240, 21, SoundEvents.ARMOR_EQUIP_CHAIN, 2, TGItems.PLATE_CARBON)
			.resist(18, 18, 19, 18, 18, 10, 12);
	static final TGArmorMaterial T3_POWER = new TGArmorMaterial("t3_power", 360, 22, SoundEvents.ARMOR_EQUIP_IRON, 2.5f, TGItems.POWER_ARMOR_PLATING)
			.resist(19, 21, 20, 19, 19, 15, 17);
	static final TGArmorMaterial T3_MINER = new TGArmorMaterial("t3_miner", 300, 20, SoundEvents.ARMOR_EQUIP_IRON, 2, TGItems.PLATE_CARBON)
			.resist(20, 20, 20, 20, 20, 20, 20);
	static final TGArmorMaterial T3_EXO = new TGArmorMaterial("t3_exo", 300, 21, SoundEvents.ARMOR_EQUIP_CHAIN, 2, TGItems.PLATE_CARBON)
			.resist(18.5f, 18.5f, 19, 18.5f, 18.5f, 10, 12);
	static final TGArmorMaterial T2_BERET = new TGArmorMaterial("t2_beret", 60, 8, SoundEvents.ARMOR_EQUIP_LEATHER, 0, TGItems.HEAVY_CLOTH);
	static final TGArmorMaterial T4_PRAETOR = new TGArmorMaterial("t4_praetor", 360, 22, SoundEvents.ARMOR_EQUIP_IRON, 3, TGItems.PLATE_CARBON)
			.resist(21, 21, 21, 21, 21, 21, 20);
	static final TGArmorMaterial T4_POWER = new TGArmorMaterial("t4_power", 460, 23, SoundEvents.ARMOR_EQUIP_IRON, 3.5f, TGItems.POWER_ARMOR_PLATING)
			.resist(21, 22, 21, 21, 21, 19, 20);

	static {
		set("t1_combat", t -> new GenericArmor(T1_COMBAT, t, "t1_combat").set(KNOCKBACK_RESISTANCE, of(t, .05f, .2f, .1f, .05f)));
		set("t1_scout", t -> fall(t, .2f, 1, new GenericArmor(T1_SCOUT, t, "t1_scout", "t1_scout_forest", "t1_scout_snow", "t1_scout_black")
				.set(SPEED, .125f).set(JUMP, of(t, .02f, .02f, .02f, .1f))));
		set("t1_miner", t -> fall(t, .2f, 1, new GenericArmor(T1_MINER, t, "t1_miner", "t1_miner_red", "t1_miner_green", "t1_miner_black")
				.camoSuffix(t == Type.HELMET ? "helmet." : "").set(SPEED, .08f).set(JUMP, of(t, 0, 0, 0, .1f)).set(BREAKSPEED, .05f)));
		set("steam", t -> fall(t, .2f, 1, step(t, powered(T1_STEAM, t, PowerType.STEAM, 3600, TGItems.COMPRESSED_AIR_TANK, TGItems.COMPRESSED_AIR_TANK_EMPTY,
				"steam_armor", "steam_armor_rusty", "steam_armor_iron", "steam_armor_hazard")
				.set(BREAKSPEED, .05f).set(EXTRA_HEART, of(t, 1, 2, 1, 1)).set(SPEED, .05f).set(JUMP, of(t, .03f, .03f, .03f, .15f))
				.set(KNOCKBACK_RESISTANCE, of(t, .15f, .3f, .2f, .15f)).rad(.5f).hide(t == Type.HELMET, t == Type.CHESTPLATE, t == Type.CHESTPLATE)
				.model("steam_armor_" + (int) of(t, 2, 0, 1, 0), false, true))));
		set("hazmat", t -> fall(t, .1f, .5f, new GenericArmor(T2_HAZMAT, t, "hazmatsuit", "hazmatsuit_grey", "hazmatsuit_orange", "hazmatsuit_blue").rad(1)));
		set("t2_combat", t -> new GenericArmor(T2_COMBAT, t, "t2_combat", "t2_combat_wood", "t2_combat_desert", "t2_combat_arctic", "t2_combat_swat", "t2_combat_security")
				.set(SPEED, .1f).set(JUMP, of(t, 0, 0, 0, .1f)).set(KNOCKBACK_RESISTANCE, of(t, .1f, .25f, .15f, .1f)));
		set("t2_commando", t -> fall(t, .2f, 1, new GenericArmor(T2_COMMANDO, t, "t2_commando").set(SPEED, .1f).set(JUMP, of(t, 0, 0, 0, .1f))
				.set(BREAKSPEED_WATER, 1.25f).set(GUN_ACCURACY, .05f).set(KNOCKBACK_RESISTANCE, of(t, .05f, .2f, .1f, .05f))
				.set(OXYGEN_GEAR, of(t, 1, 0, 0, 0)).rad(.25f)));
		set("t2_riot", t -> new GenericArmor(T2_RIOT, t, "t2_riot").set(SPEED, of(t, .12f, .12f, .13f, .13f)).set(JUMP, of(t, 0, 0, 0, .15f))
				.set(KNOCKBACK_RESISTANCE, of(t, .125f, .275f, .175f, .125f)).rad(.5f).model("coat_" + t.ordinal(), true, false));
		set("t3_combat", t -> new GenericArmor(T3_COMBAT, t, "t3_combat", "t3_combat_silver", "t3_combat_green").set(SPEED, .1f).set(JUMP, of(t, 0, 0, 0, .1f))
				.set(KNOCKBACK_RESISTANCE, of(t, .15f, .3f, .2f, .15f)).rad(.25f));
		set("t3_power", t -> fall(t, .2f, 1, step(t, powered(T3_POWER, t, PowerType.RF, 3600, TGItems.ENERGY_CELL, TGItems.ENERGY_CELL_EMPTY,
				"powerarmor", "powerarmor_dark", "powerarmor_dark2")
				.set(BREAKSPEED, .05f).set(EXTRA_HEART, of(t, 1, 2, 1, 1)).set(SPEED, .05f).set(JUMP, of(t, .03f, .03f, .03f, .15f))
				.set(KNOCKBACK_RESISTANCE, of(t, .2f, .35f, .25f, .2f)).rad(.75f).hide(t == Type.HELMET, false, t == Type.CHESTPLATE)
				.model("power_armor_" + (int) of(t, 2, 0, 1, 0), false, true))));
		set("t3_miner", t -> fall(t, .5f, 3, step(t, powered(T3_MINER, t, PowerType.RF, 10000, TGItems.ENERGY_CELL, TGItems.ENERGY_CELL_EMPTY,
				"hevsuit", "hevsuit_silver", "hevsuit_black")
				.set(SPEED, .1f).set(JUMP, of(t, .3f, .03f, .03f, .15f)).set(SPEED_WATER, 1.25f).set(BREAKSPEED, .1f).set(BREAKSPEED_WATER, 1.25f)
				.set(OXYGEN_GEAR, of(t, 1, 0, 0, 0)).set(COOLING_SYSTEM, of(t, 0, 1, 0, 0)).set(KNOCKBACK_RESISTANCE, of(t, .1f, .25f, .15f, .1f))
				.rad(1).hide(t == Type.HELMET, false, t == Type.CHESTPLATE))));
		set("t3_exo", t -> fall(t, .5f, 5, step(t, powered(T3_EXO, t, PowerType.RF, 7200, TGItems.ENERGY_CELL, TGItems.ENERGY_CELL_EMPTY,
				"t3_exo", "t3_exo_silver", "t3_exo_green")
				.set(SPEED, .2f).set(JUMP, of(t, .1f, .1f, .1f, .3f)).set(BREAKSPEED, .075f).set(KNOCKBACK_RESISTANCE, of(t, .15f, .3f, .2f, .15f))
				.rad(.5f).model("exo_suit_" + (int) of(t, 0, 2, 1, 2), true, false))));
		TGItems.ITEMS.register("t2_beret", () -> new GenericArmor(T2_BERET, Type.HELMET, "beret_texture", "beret_texture_black", "beret_texture_green")
				.set(SPEED, .1f).model("beret_0", false, false));
		set("t4_praetor", t -> fall(t, .5f, 3, step(t, powered(T4_PRAETOR, t, PowerType.RF, 10000, TGItems.ENERGY_CELL, TGItems.ENERGY_CELL_EMPTY, "t4_praetor")
				.set(SPEED, .15f).set(JUMP, of(t, .3f, .03f, .03f, .15f)).set(SPEED_WATER, 1.25f).set(BREAKSPEED, .15f).set(BREAKSPEED_WATER, 1.25f)
				.set(OXYGEN_GEAR, of(t, 1, 0, 0, 0)).set(COOLING_SYSTEM, of(t, 0, 1, 0, 0)).set(KNOCKBACK_RESISTANCE, of(t, .2f, .35f, .25f, .2f))
				.rad(1).hide(t == Type.HELMET, false, t == Type.CHESTPLATE))));
		set("t4_power", t -> fall(t, .4f, 1, step(t, powered(T4_POWER, t, PowerType.RF, 3600, TGItems.ENERGY_CELL, TGItems.ENERGY_CELL_EMPTY,
				"powerarmor_mk2_darkgrey", "powerarmor_mk2", "powerarmor_mk2_black", "powerarmor_mk2_white")
				.set(BREAKSPEED, .1f).set(EXTRA_HEART, of(t, 2, 3, 2, 2)).set(SPEED, .05f).set(JUMP, of(t, .03f, .03f, .03f, .15f))
				.set(KNOCKBACK_RESISTANCE, of(t, .2f, .35f, .25f, .2f)).rad(1.25f).hide(t == Type.HELMET, false, t == Type.CHESTPLATE)
				.model("power_armor_mk2_" + (int) of(t, 2, 0, 1, 0), false, true))));
	}

	public static final DeferredItem<GenericShield> RIOT_SHIELD = TGItems.ITEMS.register("riot_shield", () -> new GenericShield(1200, ShieldStats.RIOT, TGItems.INGOT_STEEL));
	public static final DeferredItem<GenericShield> BALLISTIC_SHIELD = TGItems.ITEMS.register("ballistic_shield",
			() -> new GenericShield(2400, ShieldStats.BALLISTIC, TGItems.PLATE_OBSIDIAN_STEEL));
	public static final DeferredItem<GenericShield> ADVANCED_SHIELD = TGItems.ITEMS.register("advanced_shield",
			() -> new GenericShield(3200, ShieldStats.ADVANCED, TGItems.PLATE_CARBON));

	public static void init(IEventBus modEventBus) {
		TGArmorMaterial.MATERIALS.register(modEventBus);
	}

	private static void set(String name, Function<Type, GenericArmor> piece) {
		for (Type type : new Type[] {Type.HELMET, Type.CHESTPLATE, Type.LEGGINGS, Type.BOOTS}) {
			TGItems.ITEMS.register(name + "_" + type.getName(), () -> piece.apply(type));
		}
	}

	/** The value for {@code type} out of helmet, chestplate, leggings, boots. */
	private static float of(Type type, float... values) {
		return values[type.ordinal()];
	}

	private static PoweredArmor powered(TGArmorMaterial material, Type type, PowerType power, int chestPower,
			Supplier<? extends Item> battery, Supplier<? extends Item> empty, String... textures) {
		PoweredArmor armor = new PoweredArmor(material, type, power, type == Type.CHESTPLATE ? chestPower : 10, textures);
		return type == Type.CHESTPLATE ? armor.battery(battery, empty) : armor;
	}

	/** 1.12 setFallProtection, on the boots. */
	private static GenericArmor fall(Type type, float damage, float freeHeight, GenericArmor armor) {
		return type == Type.BOOTS ? armor.set(FALLDMG, damage).set(FREEHEIGHT, freeHeight) : armor;
	}

	/** 1.12 setStepAssist, on the boots. */
	private static GenericArmor step(Type type, GenericArmor armor) {
		return type == Type.BOOTS ? armor.set(STEPASSIST, 1) : armor;
	}
}
