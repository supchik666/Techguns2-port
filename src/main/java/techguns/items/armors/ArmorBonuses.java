package techguns.items.armors;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingEvent;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import techguns.TGAttributes;
import techguns.Techguns;
import techguns.capabilities.TGExtendedPlayer;
import techguns.items.additionalslots.ItemGasMask;

/**
 * 1.12 TGTickHandler/TGEventHandler armor parts: power drain, attribute bonuses, jump, fall and mining boosts.
 * Step assist and night vision follow the player's toggles; oxygen gear is read by {@link techguns.items.additionalslots.ItemScubaTanks}.
 */
@EventBusSubscriber(modid = Techguns.MODID)
public class ArmorBonuses {
	private static final ResourceLocation SPEED_ID = Techguns.id("armor_speed");
	private static final ResourceLocation HEALTH_ID = Techguns.id("armor_health");
	private static final ResourceLocation KNOCKBACK_ID = Techguns.id("armor_knockback_resistance");
	private static final ResourceLocation STEP_ID = Techguns.id("armor_step_assist");
	private static final ResourceLocation RAD_ID = Techguns.id("armor_radiation_resistance");

	@SubscribeEvent
	static void tick(PlayerTickEvent.Post event) {
		Player player = event.getEntity();
		TGExtendedPlayer tg = TGExtendedPlayer.get(player);
		if (!player.level().isClientSide) {
			PoweredArmor.tick(player);
			if (player.isOnFire() && GenericArmor.bonus(player, TGArmorBonus.COOLING_SYSTEM, false) >= 1) player.clearFire();
			modifier(player, Attributes.MAX_HEALTH, HEALTH_ID, GenericArmor.bonus(player, TGArmorBonus.EXTRA_HEART, false), AttributeModifier.Operation.ADD_VALUE);
			modifier(player, Attributes.KNOCKBACK_RESISTANCE, KNOCKBACK_ID, GenericArmor.bonus(player, TGArmorBonus.KNOCKBACK_RESISTANCE, false),
					AttributeModifier.Operation.ADD_VALUE);
			if (GenericArmor.bonus(player, TGArmorBonus.NIGHTVISION, tg.enableNightVision) > 0 && tg.enableNightVision) {
				player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 220, 0, false, false));
			}
			modifier(player, TGAttributes.RADIATION_RESISTANCE, RAD_ID, radResistance(player, tg), AttributeModifier.Operation.ADD_VALUE);
		}
		float speed = GenericArmor.bonus(player, TGArmorBonus.SPEED, false);
		if (player.isInWater() || player.isInLava()) speed += GenericArmor.bonus(player, TGArmorBonus.SPEED_WATER, false);
		if (player.isSprinting()) speed *= 2;
		modifier(player, Attributes.MOVEMENT_SPEED, SPEED_ID, speed, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
		modifier(player, Attributes.STEP_HEIGHT, STEP_ID, tg.enableStepAssist && GenericArmor.bonus(player, TGArmorBonus.STEPASSIST, false) > 0 ? 0.5f : 0,
				AttributeModifier.Operation.ADD_VALUE);
	}

	/** 1.12 armor attribute modifiers plus the gas mask's face slot one; broken pieces count too, as in 1.12. */
	private static float radResistance(Player player, TGExtendedPlayer tg) {
		float total = 0;
		for (ItemStack stack : player.getArmorSlots()) {
			if (stack.getItem() instanceof GenericArmor armor) total += armor.radResistance;
		}
		if (tg.getSlot(TGExtendedPlayer.SLOT_FACE).getItem() instanceof ItemGasMask mask) total += mask.radresist;
		return total;
	}

	private static void modifier(Player player, Holder<Attribute> attribute, ResourceLocation id, float value, AttributeModifier.Operation operation) {
		AttributeInstance instance = player.getAttribute(attribute);
		if (instance == null) return;
		AttributeModifier old = instance.getModifier(id);
		if (value <= 0) {
			if (old != null) instance.removeModifier(id);
		} else if (old == null || old.amount() != value) {
			instance.addOrUpdateTransientModifier(new AttributeModifier(id, value, operation));
		}
	}

	@SubscribeEvent
	static void jump(LivingEvent.LivingJumpEvent event) {
		if (!(event.getEntity() instanceof Player player)) return;
		float jump = GenericArmor.bonus(player, TGArmorBonus.JUMP, true);
		if (jump > 0) player.setDeltaMovement(player.getDeltaMovement().add(0, jump, 0));
	}

	@SubscribeEvent
	static void fall(LivingFallEvent event) {
		if (!(event.getEntity() instanceof Player player)) return;
		float reduction = GenericArmor.bonus(player, TGArmorBonus.FALLDMG, event.getDistance() > 3);
		float freeHeight = GenericArmor.bonus(player, TGArmorBonus.FREEHEIGHT, false);
		event.setDistance(Math.max(event.getDistance() - freeHeight, 0) * Math.max(1 - reduction, 0));
	}

	@SubscribeEvent
	static void breakSpeed(PlayerEvent.BreakSpeed event) {
		Player player = event.getEntity();
		float bonus = 1 + GenericArmor.bonus(player, TGArmorBonus.BREAKSPEED, true);
		if (player.isEyeInFluid(FluidTags.WATER) || player.isEyeInFluid(FluidTags.LAVA)) bonus *= 1 + GenericArmor.bonus(player, TGArmorBonus.BREAKSPEED_WATER, true);
		event.setNewSpeed(event.getNewSpeed() * bonus);
	}
}
