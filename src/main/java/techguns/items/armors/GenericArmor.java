package techguns.items.armors;

import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.DoubleFunction;
import java.util.function.Function;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.Level;
import techguns.TGComponents;
import techguns.Techguns;
import techguns.damagesystem.TGDamageType;

/**
 * 1.12 GenericArmor and GenericArmorMultiCamo: per-damage-type armor values (see {@link techguns.damagesystem.DamageSystem}),
 * bonuses summed over the worn pieces, and camos picked by sneak-use. Broken pieces (1 durability left) still absorb
 * but give no bonuses, and the armor bar doesn't count them.
 */
public class GenericArmor extends ArmorItem {
	public final TGArmorMaterial tgMaterial;
	/** Texture per camo, under textures/models/armor/. */
	public final String[] textures;
	private final EnumMap<TGArmorBonus, Float> bonuses = new EnumMap<>(TGArmorBonus.class);
	/** Camo names are techguns.item.[first texture].[suffix]camoname.[i]; the suffix ends in a dot. */
	private String camoSuffix = "";
	/** Added to the wearer's radiation resistance, see {@link ArmorBonuses}. */
	public float radResistance;
	/** Which extra slot items this piece hides on the player model (helmet: face; chestplate: back and gloves). */
	public boolean hideFace, hideBack, hideGloves;
	/** Client armor model key (see TGClient), or null for the flat vanilla layer. */
	@Nullable
	public String model;
	/** Separate _layer_1/_layer_2 textures, as the flat armors; 3D models mostly use one. */
	public boolean doubleTex = true;
	/** The item is drawn from its 3D armor model (1.12 setUseRenderHack). */
	public boolean renderHack;

	public GenericArmor(TGArmorMaterial material, Type type, String... textures) {
		super(material.material, type, new Item.Properties().durability(material.durability));
		this.tgMaterial = material;
		this.textures = textures;
	}

	public GenericArmor set(TGArmorBonus bonus, float value) {
		bonuses.put(bonus, value);
		return this;
	}

	public GenericArmor rad(float resistance) {
		radResistance = resistance;
		return this;
	}

	public GenericArmor hide(boolean face, boolean back, boolean gloves) {
		hideFace = face;
		hideBack = back;
		hideGloves = gloves;
		return this;
	}

	public GenericArmor model(String key, boolean doubleTex, boolean renderHack) {
		this.model = key;
		this.doubleTex = doubleTex;
		this.renderHack = renderHack;
		return this;
	}

	public GenericArmor camoSuffix(String suffix) {
		camoSuffix = suffix;
		return this;
	}

	public float getBonus(TGArmorBonus bonus) {
		return bonuses.getOrDefault(bonus, 0f);
	}

	public float armorValue(TGDamageType damageType) {
		return tgMaterial.armorValue(type, damageType);
	}

	public static boolean isBroken(ItemStack stack) {
		return stack.getDamageValue() >= stack.getMaxDamage() - 1;
	}

	/** 1.12 getArmorDisplay: broken pieces show no armor points; {@link techguns.damagesystem.DamageSystem#armorValue} still counts them. */
	@Override
	public ItemAttributeModifiers getDefaultAttributeModifiers(ItemStack stack) {
		ItemAttributeModifiers modifiers = super.getDefaultAttributeModifiers(stack);
		if (!isBroken(stack)) return modifiers;
		return new ItemAttributeModifiers(modifiers.modifiers().stream().filter(e -> !e.attribute().is(Attributes.ARMOR)).toList(), modifiers.showInTooltip());
	}

	/** The bonus this piece gives right now; powered armor only gives it with power, spending some if {@code consume}. */
	protected float bonus(ItemStack stack, TGArmorBonus bonus, Player player, boolean consume) {
		return getBonus(bonus);
	}

	/**
	 * 1.12 getArmorBonusForPlayer: {@code bonus} summed over the worn TG armor, spending power for it if {@code consume}.
	 * ponytail: B's face/back/hand slot items add theirs here (1.12 ITGSpecialSlot.getBonus).
	 */
	public static float bonus(Player player, TGArmorBonus bonus, boolean consume) {
		float total = 0;
		for (ItemStack stack : player.getArmorSlots()) {
			if (stack.getItem() instanceof GenericArmor armor && !isBroken(stack)) total += armor.bonus(stack, bonus, player, consume);
		}
		return total;
	}

	public int getCamo(ItemStack stack) {
		return Math.min(stack.getOrDefault(TGComponents.CAMO, 0), textures.length - 1);
	}

	public Component camoName(ItemStack stack) {
		return Component.translatable("techguns.item." + textures[0] + "." + camoSuffix + "camoname." + getCamo(stack));
	}

	/** Sneak-use switches camo; plain use equips as usual. */
	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (!player.isShiftKeyDown() || textures.length < 2) return super.use(level, player, hand);
		if (!level.isClientSide) {
			stack.set(TGComponents.CAMO, (getCamo(stack) + 1) % textures.length);
			player.displayClientMessage(Component.translatable("techguns.message.camoswitch").append(" ").append(camoName(stack)), true);
		}
		return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
	}

	@Override
	public ResourceLocation getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, ArmorMaterial.Layer layer, boolean innerModel) {
		String suffix = doubleTex ? (slot == EquipmentSlot.LEGS ? "_layer_2" : "_layer_1") : "";
		return Techguns.id("textures/models/armor/" + textures[getCamo(stack)] + suffix + ".png");
	}

	/** Armor never breaks: it stops at 1 durability. */
	@Override
	public <T extends LivingEntity> int damageItem(ItemStack stack, int amount, @Nullable T entity, Consumer<Item> onBroken) {
		return Math.max(Math.min(amount, stack.getMaxDamage() - 1 - stack.getDamageValue()), 0);
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
		if (textures.length > 1) tooltip.add(Component.translatable("techguns.tooltip.currentcamo").append(": ").append(camoName(stack)).withStyle(ChatFormatting.GRAY));
		tooltip.add(line("durability", (stack.getMaxDamage() - stack.getDamageValue()) + "/" + stack.getMaxDamage()));
		if (!Screen.hasShiftDown()) {
			shiftHint(tooltip);
			return;
		}
		if (tgMaterial.toughness > 0) tooltip.add(line("toughness", tgMaterial.toughness).withStyle(ChatFormatting.GRAY));
		resistances(tooltip, this::armorValue, "");
		bonusLine(tooltip, TGArmorBonus.EXTRA_HEART, "healthbonus", v -> "+" + (int) v + " ", "hearts");
		bonusLine(tooltip, TGArmorBonus.SPEED, "movespeed", v -> "+" + v * 100 + "%", null);
		bonusLine(tooltip, TGArmorBonus.JUMP, "jumpheight", v -> "+" + v, null);
		bonusLine(tooltip, TGArmorBonus.FALLDMG, "falldamage", v -> "-" + v * 100 + "%", null);
		bonusLine(tooltip, TGArmorBonus.FREEHEIGHT, "fallheight", v -> "-" + v, null);
		bonusLine(tooltip, TGArmorBonus.BREAKSPEED, "miningspeed", v -> "+" + v * 100 + "%", null);
		bonusLine(tooltip, TGArmorBonus.BREAKSPEED_WATER, "underwatermining", v -> "+" + v * 100 + "%", null);
		bonusLine(tooltip, TGArmorBonus.KNOCKBACK_RESISTANCE, "knockbackresistance", v -> "+" + v * 100 + "%", null);
		for (String flagBonus : new String[] {"NIGHTVISION:nightvision", "STEPASSIST:stepassist", "OXYGEN_GEAR:oxygengear", "COOLING_SYSTEM:coolingsystem"}) {
			String[] parts = flagBonus.split(":");
			if (getBonus(TGArmorBonus.valueOf(parts[0])) > 0) tooltip.add(Component.translatable("techguns.armorTooltip." + parts[1]));
		}
	}

	private void bonusLine(List<Component> tooltip, TGArmorBonus bonus, String key, DoubleFunction<String> format, @Nullable String unit) {
		float value = getBonus(bonus);
		if (value <= 0) return;
		var line = Component.translatable("techguns.armorTooltip." + key).append(": " + format.apply(value));
		tooltip.add(unit == null ? line : line.append(Component.translatable("techguns.armorTooltip." + unit)));
	}

	static void shiftHint(List<Component> tooltip) {
		tooltip.add(Component.translatable("techguns.gun.tooltip.shift1").append(" ")
				.append(Component.translatable("techguns.gun.tooltip.shift2").withStyle(ChatFormatting.GREEN)).append(" ")
				.append(Component.translatable("techguns.gun.tooltip.shift3")).withStyle(ChatFormatting.GRAY));
	}

	static MutableComponent line(String key, Object value) {
		return Component.translatable("techguns.armorTooltip." + key).append(": " + value);
	}

	/** The AR/PR/EX... lines, shared with the shield tooltip. */
	public static void resistances(List<Component> tooltip, Function<TGDamageType, Float> value, String unit) {
		tooltip.add(Component.translatable("techguns.armorTooltip.resistances").append(":").withStyle(ChatFormatting.BLUE));
		Object[][] rows = {{ChatFormatting.DARK_GRAY, "AR", TGDamageType.PHYSICAL}, {ChatFormatting.GRAY, "PR", TGDamageType.PROJECTILE},
				{ChatFormatting.DARK_RED, "EX", TGDamageType.EXPLOSION}, {ChatFormatting.DARK_AQUA, "E", TGDamageType.ENERGY},
				{ChatFormatting.RED, "F", TGDamageType.FIRE}, {ChatFormatting.AQUA, "I", TGDamageType.ICE}, {ChatFormatting.YELLOW, "L", TGDamageType.LIGHTNING},
				{ChatFormatting.DARK_GREEN, "P", TGDamageType.POISON}, {ChatFormatting.DARK_GRAY, "D", TGDamageType.DARK},
				{ChatFormatting.GREEN, "RAD", TGDamageType.RADIATION}};
		for (Object[] row : rows) {
			tooltip.add(Component.literal(" " + row[1] + ": " + String.format(Locale.ROOT, "%.2f", value.apply((TGDamageType) row[2])).replaceAll("\\.?0+$", "") + unit)
					.withStyle((ChatFormatting) row[0]));
		}
	}
}
