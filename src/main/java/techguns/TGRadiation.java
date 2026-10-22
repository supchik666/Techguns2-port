package techguns;

import java.util.Set;
import java.util.function.Supplier;

import com.mojang.serialization.Codec;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionBrewing;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.common.EffectCure;
import net.neoforged.neoforge.common.EffectCures;
import net.neoforged.neoforge.event.brewing.RegisterBrewingRecipesEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.datamaps.DataMapType;
import net.neoforged.neoforge.registries.datamaps.RegisterDataMapTypesEvent;

/**
 * 1.12 TGRadiationSystem. Players have a radiation level (0 to {@link #MAX_RADIATION}) that the radiation effect raises
 * every second, less their {@link TGAttributes#RADIATION_RESISTANCE}; from {@link #MINOR_POISONING} up it poisons them.
 * Radioactive items carried ({@link #ITEM_RADIATION}) and radiation zones ({@link techguns.entities.special.RadiationEntity})
 * give the effect. ponytail: always on; 1.12 had a WIP config to turn it off (off by default), wait for the M8 config.
 */
@EventBusSubscriber(modid = Techguns.MODID)
public class TGRadiation {
	public static final int MAX_RADIATION = 1000;
	public static final int MINOR_POISONING = 500;
	public static final int SEVERE_POISONING = 750;
	public static final int LETHAL_POISONING = 1000;
	static final int RADLOST_ON_DEATH = 200;
	static final int INVENTORY_RADIATION_INTERVAL = 60;

	public static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(Registries.MOB_EFFECT, Techguns.MODID);
	public static final DeferredRegister<Potion> POTIONS = DeferredRegister.create(Registries.POTION, Techguns.MODID);

	public static final DeferredHolder<MobEffect, MobEffect> RADIATION = EFFECTS.register("radiation", RadiationEffect::new);
	public static final DeferredHolder<MobEffect, MobEffect> RAD_REGENERATION = EFFECTS.register("radregeneration", RadRegenerationEffect::new);
	public static final DeferredHolder<MobEffect, MobEffect> RAD_RESISTANCE = EFFECTS.register("radresistance",
			() -> new MobEffect(MobEffectCategory.BENEFICIAL, 0x99ac2d) {}.addAttributeModifier(TGAttributes.RADIATION_RESISTANCE,
					Techguns.id("effect.radresistance"), 1, AttributeModifier.Operation.ADD_VALUE));

	public static final DeferredHolder<Potion, Potion> RAD_POTION = POTIONS.register("radpotion",
			() -> new Potion("radpotion", new MobEffectInstance(RADIATION, 300, 4)));
	public static final DeferredHolder<Potion, Potion> RAD_POTION_SEVERE = POTIONS.register("radpotion_severe",
			() -> new Potion("radpotion", new MobEffectInstance(RADIATION, 300, 24)));
	public static final DeferredHolder<Potion, Potion> RAD_REGENERATION_POTION = POTIONS.register("radregenerationpotion",
			() -> new Potion("radregenerationpotion", new MobEffectInstance(RAD_REGENERATION, 400, 9)));
	public static final DeferredHolder<Potion, Potion> RAD_RESISTANCE_POTION = POTIONS.register("radresistancepotion",
			() -> new Potion("radresistancepotion", new MobEffectInstance(RAD_RESISTANCE, 900, 1)));

	/** A player's radiation level, kept through death; synced to that player only, for the HUD. */
	public static final Supplier<AttachmentType<Integer>> RAD_LEVEL = TGComponents.ATTACHMENTS.register("radiation",
			() -> AttachmentType.builder(() -> 0).serialize(Codec.INT).copyOnDeath().sync((holder, player) -> holder == player, ByteBufCodecs.VAR_INT)
					.build());

	private static final Codec<Integer> AMOUNT = ExtraCodecs.POSITIVE_INT.fieldOf("amount").codec();
	/** How radioactive an item is: carrying it gives the radiation effect at this level (1.12 ItemRadiationRegistry). */
	public static final DataMapType<Item, Integer> ITEM_RADIATION = DataMapType.builder(Techguns.id("radiation"), Registries.ITEM, AMOUNT)
			.synced(AMOUNT, false).build();

	/** Radiation effect damage to mobs (1.12 tg_rad). */
	public static final ResourceKey<DamageType> RADIATION_DAMAGE = ResourceKey.create(Registries.DAMAGE_TYPE, Techguns.id("radiation"));
	/** Lethal radiation poisoning; armor doesn't help (1.12 tg_rad_poisoning, UNRESISTABLE). */
	public static final ResourceKey<DamageType> POISONING_DAMAGE = ResourceKey.create(Registries.DAMAGE_TYPE, Techguns.id("radiation_poisoning"));

	public static int getRadiation(Player player) {
		return player.getData(RAD_LEVEL);
	}

	/** 1.12 TGExtendedPlayer.addRadiation: changes the level by {@code amount}, kept within 0 to {@link #MAX_RADIATION}. */
	public static void addRadiation(Player player, int amount) {
		int old = getRadiation(player), level = Mth.clamp(old + amount, 0, MAX_RADIATION);
		if (level != old) player.setData(RAD_LEVEL, level);
	}

	/** What the radiation effect at {@code amplifier} gives {@code entity} each second: amplifier+1 less its resistance. */
	public static int radPerSecond(LivingEntity entity, int amplifier) {
		return Mth.clamp(amplifier + 1 - (int) entity.getAttributeValue(TGAttributes.RADIATION_RESISTANCE), 0, MAX_RADIATION);
	}

	/**
	 * Gives living entities within {@code radius} of {@code pos} the radiation effect for {@code duration} ticks at
	 * {@code strength} inside {@code innerRadius}, going to {@code strengthOuter} at {@code radius}. 1.12 had the falloff
	 * backwards (outer strength just past the inner radius, rising to full at the edge); this fades out as meant.
	 */
	public static void applyRadToEntities(Level level, Vec3 pos, double radius, int duration, int strength, double innerRadius, int strengthOuter) {
		for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, new AABB(pos, pos).inflate(radius))) {
			double distance = entity.position().distanceTo(pos);
			if (distance >= radius) continue;
			int amplifier = distance <= innerRadius ? strength
					: (int) Math.round(strength + (strengthOuter - strength) * (distance - innerRadius) / (radius - innerRadius));
			entity.addEffect(new MobEffectInstance(RADIATION, duration, amplifier, true, true));
		}
	}

	/** 1.12 TGTickHandler: hunger from {@link #MINOR_POISONING}, also weakness and mining fatigue from severe, nausea, slowness and damage at lethal. */
	public static void poison(Player player) {
		int level = getRadiation(player);
		if (level >= SEVERE_POISONING) {
			player.addEffect(new MobEffectInstance(MobEffects.HUNGER, 30, 1, false, false));
			player.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 30, 1, false, false));
			player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 30, 1, false, false));
		} else if (level >= MINOR_POISONING) {
			player.addEffect(new MobEffectInstance(MobEffects.HUNGER, 30, 0, false, false));
		}
		if (level >= LETHAL_POISONING) {
			player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 30, 1, false, false));
			player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 1, false, false));
			player.hurt(player.damageSources().source(POISONING_DAMAGE), 2);
		}
	}

	@SubscribeEvent
	static void tick(PlayerTickEvent.Post event) {
		Player player = event.getEntity();
		if (player.level().isClientSide) return;
		long time = player.level().getGameTime();
		if (time % INVENTORY_RADIATION_INTERVAL == 0) {
			int max = 0;
			for (ItemStack stack : player.getInventory().items) {
				Integer amount = stack.getItem().builtInRegistryHolder().getData(ITEM_RADIATION);
				if (amount != null) max = Math.max(max, amount);
			}
			if (max > 0) player.addEffect(new MobEffectInstance(RADIATION, INVENTORY_RADIATION_INTERVAL, max - 1, false, false));
		}
		if (time % 20 == 0) poison(player);
	}

	@SubscribeEvent
	static void onDeath(LivingDeathEvent event) {
		if (event.getEntity() instanceof Player player && !player.level().isClientSide) addRadiation(player, -RADLOST_ON_DEATH);
	}

	@SubscribeEvent
	static void registerDataMaps(RegisterDataMapTypesEvent event) {
		event.register(ITEM_RADIATION);
	}

	/** 1.12 TGMachineRecipes brewing. ponytail: our yellowcake and lead ingot, not the c: tags 1.12 took (dustUranium, ingotLead). */
	@SubscribeEvent
	static void registerBrewing(RegisterBrewingRecipesEvent event) {
		PotionBrewing.Builder builder = event.getBuilder();
		builder.addMix(Potions.AWKWARD, TGItems.YELLOWCAKE.get(), RAD_POTION);
		builder.addMix(Potions.AWKWARD, TGItems.ENRICHED_URANIUM.get(), RAD_POTION_SEVERE);
		builder.addMix(Potions.AWKWARD, TGItems.INGOT_LEAD.get(), RAD_RESISTANCE_POTION);
		builder.addMix(RAD_POTION, Items.GOLDEN_CARROT, RAD_REGENERATION_POTION);
		builder.addMix(RAD_POTION, Items.GLISTERING_MELON_SLICE, RAD_REGENERATION_POTION);
	}

	/** 1.12 RadiationPotion: every second players gain {@link #radPerSecond} with a geiger tick; mobs take half of it as damage. */
	static class RadiationEffect extends MobEffect {
		RadiationEffect() {
			super(MobEffectCategory.HARMFUL, 0xf8ff00);
		}

		@Override
		public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
			return duration % 20 == 0;
		}

		@Override
		public boolean applyEffectTick(LivingEntity entity, int amplifier) {
			int amount = radPerSecond(entity, amplifier);
			if (entity instanceof Player player) {
				if (player.isCreative()) return true;
				if (!player.level().isClientSide) {
					addRadiation(player, amount);
				} else if (amount > 0) {
					player.level().playSound(player, player.getX(), player.getY(), player.getZ(),
							(amplifier >= 2 ? TGSounds.GEIGER_HIGH : TGSounds.GEIGER_LOW).get(), SoundSource.PLAYERS, 1, 1);
				}
			} else if (entity instanceof Mob && amount * 0.5f >= 1) {
				entity.hurt(entity.damageSources().source(RADIATION_DAMAGE), amount * 0.5f);
			}
			return true;
		}

		/** Milk doesn't cure it. */
		@Override
		public void fillEffectCures(Set<EffectCure> cures, MobEffectInstance instance) {
			cures.add(EffectCures.PROTECTED_BY_TOTEM);
		}
	}

	/** 1.12 RadRegenerationPotion: every second players lose amplifier+1 radiation. */
	static class RadRegenerationEffect extends MobEffect {
		RadRegenerationEffect() {
			super(MobEffectCategory.NEUTRAL, 0xfa2000);
		}

		@Override
		public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
			return duration % 20 == 0;
		}

		@Override
		public boolean applyEffectTick(LivingEntity entity, int amplifier) {
			if (entity instanceof Player player && !player.level().isClientSide) addRadiation(player, -(amplifier + 1));
			return true;
		}
	}
}
