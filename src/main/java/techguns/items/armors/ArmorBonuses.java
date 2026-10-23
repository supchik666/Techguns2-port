package techguns.items.armors;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingEvent;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import techguns.Techguns;

/**
 * 1.12 TGTickHandler/TGEventHandler armor parts: power drain, attribute bonuses, jump, fall and mining boosts.
 * ponytail: step assist is always on (B's keybind toggles it), night vision and oxygen gear wait for B's player data.
 */
@EventBusSubscriber(modid = Techguns.MODID)
public class ArmorBonuses {
	private static final ResourceLocation SPEED_ID = Techguns.id("armor_speed");
	private static final ResourceLocation HEALTH_ID = Techguns.id("armor_health");
	private static final ResourceLocation KNOCKBACK_ID = Techguns.id("armor_knockback_resistance");
	private static final ResourceLocation STEP_ID = Techguns.id("armor_step_assist");

	@SubscribeEvent
	static void tick(PlayerTickEvent.Post event) {
		Player player = event.getEntity();
		if (!player.level().isClientSide) {
			PoweredArmor.tick(player);
			if (player.isOnFire() && GenericArmor.bonus(player, TGArmorBonus.COOLING_SYSTEM, false) >= 1) player.clearFire();
			modifier(player, Attributes.MAX_HEALTH, HEALTH_ID, GenericArmor.bonus(player, TGArmorBonus.EXTRA_HEART, false), AttributeModifier.Operation.ADD_VALUE);
			modifier(player, Attributes.KNOCKBACK_RESISTANCE, KNOCKBACK_ID, GenericArmor.bonus(player, TGArmorBonus.KNOCKBACK_RESISTANCE, false),
					AttributeModifier.Operation.ADD_VALUE);
		}
		float speed = GenericArmor.bonus(player, TGArmorBonus.SPEED, false);
		if (player.isInWater() || player.isInLava()) speed += GenericArmor.bonus(player, TGArmorBonus.SPEED_WATER, false);
		if (player.isSprinting()) speed *= 2;
		modifier(player, Attributes.MOVEMENT_SPEED, SPEED_ID, speed, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
		modifier(player, Attributes.STEP_HEIGHT, STEP_ID, GenericArmor.bonus(player, TGArmorBonus.STEPASSIST, false) > 0 ? 0.5f : 0,
				AttributeModifier.Operation.ADD_VALUE);
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
