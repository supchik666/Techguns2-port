package techguns.items.armors;

import java.util.List;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.phys.Vec3;
import techguns.TGComponents;
import techguns.TGSounds;
import techguns.items.guns.GenericGun;

/**
 * 1.12 PoweredArmor: gives its bonuses only while charged. The chestplate holds the charge, refills from a battery item in
 * the inventory, and tops up the other pieces of its power type; walking drains 1 per piece per second, and jumping,
 * landing and cooling cost extra. (1.12's unpowered bonuses were all 0, so there are none here.)
 */
public class PoweredArmor extends GenericArmor {
	public enum PowerType { STEAM, RF }

	public final PowerType powerType;
	public final int maxPower;
	@Nullable
	private Supplier<? extends Item> battery, emptyBattery;

	public PoweredArmor(TGArmorMaterial material, Type type, PowerType powerType, int maxPower, String... textures) {
		super(material, type, textures);
		this.powerType = powerType;
		this.maxPower = maxPower;
	}

	public PoweredArmor battery(Supplier<? extends Item> battery, Supplier<? extends Item> empty) {
		this.battery = battery;
		this.emptyBattery = empty;
		return this;
	}

	public static int getPower(ItemStack stack) {
		return stack.getOrDefault(TGComponents.POWER, 0);
	}

	static void consumePower(ItemStack stack, int amount) {
		stack.set(TGComponents.POWER, Math.max(getPower(stack) - amount, 0));
	}

	/** Charges {@code stack} by up to {@code max}; returns what it took. */
	private int charge(ItemStack stack, int max) {
		int taken = Math.max(Math.min(maxPower - getPower(stack), max), 0);
		stack.set(TGComponents.POWER, getPower(stack) + taken);
		return taken;
	}

	@Override
	protected float bonus(ItemStack stack, TGArmorBonus bonus, Player player, boolean consume) {
		if (getPower(stack) <= 0) return 0;
		float value = getBonus(bonus);
		if (consume && value > 0 && !player.level().isClientSide) consumePower(stack, actionCost(bonus, player));
		return value;
	}

	/** 1.12 applyPowerConsumptionOnAction. */
	private int actionCost(TGArmorBonus bonus, Player player) {
		if (bonus == TGArmorBonus.JUMP && type == Type.LEGGINGS) {
			player.level().playSound(null, player.getX(), player.getY(), player.getZ(), TGSounds.STEAM_JUMP.get(), SoundSource.PLAYERS, 1f, 1f);
			return 5;
		}
		if (bonus == TGArmorBonus.FALLDMG && type == Type.BOOTS) return 5;
		return bonus == TGArmorBonus.COOLING_SYSTEM && type == Type.CHESTPLATE ? 1 : 0;
	}

	/** 1.12 calculateConsumptionTick, server side every player tick. */
	public static void tick(Player player) {
		Vec3 motion = player.getDeltaMovement();
		boolean drain = player.level().getGameTime() % 20 == 0 && (Math.abs(motion.x) > 0.001 || Math.abs(motion.z) > 0.001);
		ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
		PoweredArmor chestArmor = chest.getItem() instanceof PoweredArmor a ? a : null;
		powerSlots(player, chestArmor, chest, drain);
		if (chestArmor == null || getPower(chest) > 0 || chestArmor.battery == null || !GenericGun.consume(player, chestArmor.battery.get())) return;
		if (chestArmor.emptyBattery != null) GenericGun.give(player, new ItemStack(chestArmor.emptyBattery.get()));
		chest.set(TGComponents.POWER, chestArmor.maxPower);
		powerSlots(player, chestArmor, chest, false);
	}

	private static void powerSlots(Player player, @Nullable PoweredArmor chestArmor, ItemStack chest, boolean drain) {
		for (EquipmentSlot slot : new EquipmentSlot[] {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
			ItemStack stack = player.getItemBySlot(slot);
			if (!(stack.getItem() instanceof PoweredArmor armor)) continue;
			if (slot != EquipmentSlot.CHEST && chestArmor != null && armor.powerType == chestArmor.powerType) {
				consumePower(chest, armor.charge(stack, getPower(chest)));
			}
			if (drain) consumePower(stack, 1);
		}
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
		tooltip.add(Component.translatable("techguns.tooltip.power").append(": " + getPower(stack) + "/" + maxPower).withStyle(ChatFormatting.AQUA));
		var line = Component.translatable("techguns.tooltip.powerType").append(": ").append(Component.translatable("techguns.tooltip.powertype." + (powerType == PowerType.STEAM ? "Steam" : "RF")));
		if (battery != null) line.append(" (").append(battery.get().getDescription()).append(")");
		tooltip.add(line.withStyle(ChatFormatting.AQUA));
		super.appendHoverText(stack, context, tooltip, flag);
	}
}
