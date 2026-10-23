package techguns.damagesystem;

import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.damagesource.DamageContainer;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingShieldBlockEvent;
import techguns.Techguns;
import techguns.items.armors.GenericArmor;
import techguns.items.armors.TGArmorBonus;

/**
 * 1.12 DamageSystem: TG armor resists each damage type separately, and penetration pierces armor past its toughness.
 * Applies to TG damage on anything and to all damage on entities wearing TG armor; vanilla armor counts with its
 * vanilla points. NPCs ({@link INpcTGDamageSystem}) use their own per-type armor against everything.
 * ponytail: no PvP/NPC damage factors until the M8 config.
 */
@EventBusSubscriber(modid = Techguns.MODID)
public class DamageSystem {
	/** 1.12 getDamageAfterAbsorb: each point stops 4%, capped at 24; penetration beyond toughness removes points. */
	public static float damageAfterArmor(float damage, float armor, float toughness, float penetration) {
		return damage * (1 - Mth.clamp(armor - Math.max(penetration - toughness, 0), 0, 24) / 25);
	}

	/** Armor points one point of gun penetration removes. */
	public static final float PENETRATION_POINTS = 4;

	/** Tooltip percent of damage {@code armor} points stop, on {@link #damageAfterArmor}'s scale (max 96). */
	public static int protectionPercent(float armor) {
		return Math.round(Mth.clamp(armor, 0, 24) * 4);
	}

	/** Tooltip percent of armor protection a gun's {@code penetration} ignores (before the target's toughness). */
	public static int penetrationPercent(float penetration) {
		return Math.round(penetration * PENETRATION_POINTS * 4);
	}

	/** Armor points against {@code type}: TG pieces by their per-type value, everything else by the armor attribute. */
	public static float armorValue(LivingEntity entity, TGDamageType type) {
		float armor = entity.getArmorValue();
		for (ItemStack stack : entity.getArmorSlots()) {
			// broken pieces aren't in the armor attribute but still absorb
			if (stack.getItem() instanceof GenericArmor piece) armor += piece.armorValue(type) - (GenericArmor.isBroken(stack) ? 0 : piece.getDefense());
		}
		return armor;
	}

	/** 1.12 getArmorAgainstDamageTypeDefault: an NPC's flat armor scaled by how well armor works against {@code type}. */
	public static float armorAgainstTypeDefault(LivingEntity entity, float armor, TGDamageType type) {
		return switch (type) {
			case PHYSICAL, PROJECTILE -> armor;
			case EXPLOSION, ENERGY, ICE, LIGHTNING, DARK -> armor * 0.5f;
			case FIRE -> entity.fireImmune() ? armor * 2 : armor * 0.5f;
			case POISON, RADIATION, UNRESISTABLE -> 0;
		};
	}

	@SubscribeEvent
	static void incoming(LivingIncomingDamageEvent event) {
		LivingEntity entity = event.getEntity();
		DamageSource source = event.getSource();
		// 1.12 ignoreHurtresistTime: bullets and the like neither wait for nor start the hurt cooldown
		if (source instanceof TGDamageSource && source.is(DamageTypeTags.BYPASSES_COOLDOWN)) {
			event.getContainer().setPostAttackInvulnerabilityTicks(entity.invulnerableTime);
		}
		if (entity instanceof Player player && (source.is(DamageTypes.LAVA) || source.is(DamageTypes.ON_FIRE) || source.is(DamageTypes.IN_FIRE))
				&& GenericArmor.bonus(player, TGArmorBonus.COOLING_SYSTEM, player.level().getGameTime() % 5 == 0) >= 1) {
			event.setCanceled(true);
			return;
		}
		if (source.is(DamageTypeTags.BYPASSES_ARMOR)) return;
		float armor, resistance = 0;
		if (entity instanceof INpcTGDamageSystem npc) {
			armor = npc.getTotalArmorAgainstType(TGDamageType.of(source));
			resistance = npc.getPenetrationResistance(TGDamageType.of(source));
		}
		else if (source instanceof TGDamageSource || wearsTGArmor(entity)) armor = armorValue(entity, TGDamageType.of(source));
		else return;
		float toughness = (float) entity.getAttributeValue(Attributes.ARMOR_TOUGHNESS);
		float penetration = source instanceof TGDamageSource tg ? Math.max(tg.penetration - resistance, 0) * PENETRATION_POINTS : 0;
		event.getContainer().addModifier(DamageContainer.Reduction.ARMOR,
				(container, reduction) -> container.getNewDamage() - damageAfterArmor(container.getNewDamage(), armor, toughness, penetration));
	}

	private static boolean wearsTGArmor(LivingEntity entity) {
		for (ItemStack stack : entity.getArmorSlots()) {
			if (stack.getItem() instanceof GenericArmor) return true;
		}
		return false;
	}

	/** TG damage gets through a shield by its {@link ShieldStats}; vanilla damage is blocked as in vanilla. */
	@SubscribeEvent
	static void shieldBlock(LivingShieldBlockEvent event) {
		if (!event.getBlocked() || !(event.getDamageSource() instanceof TGDamageSource source)) return;
		LivingEntity entity = event.getEntity();
		ShieldStats stats = ShieldStats.of(entity.getUseItem());
		float amount = event.getOriginalBlockedDamage();
		event.setBlockedDamage(amount - stats.passThrough(amount, source));
		entity.level().playSound(null, entity.getX(), entity.getY(), entity.getZ(), stats.sound.get(), SoundSource.PLAYERS, 1.0f, 1.0f);
	}
}
