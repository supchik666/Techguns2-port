package techguns.items.guns;

import java.util.List;
import java.util.function.Supplier;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import techguns.entities.projectiles.GenericProjectile;

/**
 * A hand grenade (1.12 GenericGrenade): hold right click to wind up, release to throw; a longer wind-up (up to
 * {@code fullChargeTime} ticks) throws further. The projectile factory gets the max/min radius as damage drop
 * start/end and the bounce count in the ammo slot.
 */
public class GenericGrenade extends Item {
	float damage = 20.0f;
	float radius = 3.0f;
	int maxBounces = 3;
	float damageMin = 10.0f;
	float radiusMin = 5.0f;
	float fullChargeTime = 30.0f;
	int ttl = 200;
	float speed = 0.75f;
	float spread = 0.1f;
	Supplier<SoundEvent> startSound;
	ChargedProjectileFactory projectile;

	public GenericGrenade() {
		super(new Properties().stacksTo(16));
	}

	public GenericGrenade setStartSound(Supplier<SoundEvent> sound) {
		this.startSound = sound;
		return this;
	}

	public GenericGrenade setDamageAndRadius(float damage, float radiusMax, float damageMin, float radiusMin) {
		this.damage = damage;
		this.radius = radiusMax;
		this.damageMin = damageMin;
		this.radiusMin = radiusMin;
		return this;
	}

	public GenericGrenade setChargedProjectile(ChargedProjectileFactory factory) {
		this.projectile = factory;
		return this;
	}

	public float getFullChargeTime() {
		return fullChargeTime;
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		player.startUsingItem(hand);
		if (!level.isClientSide && startSound != null) level.playSound(null, player, startSound.get(), SoundSource.PLAYERS, 1.0f, 1.0f);
		return InteractionResultHolder.consume(player.getItemInHand(hand));
	}

	@Override
	public int getUseDuration(ItemStack stack, LivingEntity entity) {
		return 72000;
	}

	@Override
	public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
		float charge = Math.min((getUseDuration(stack, entity) - timeLeft) / fullChargeTime, 1.0f);
		InteractionHand hand = entity.getUsedItemHand();
		boolean left = (hand == InteractionHand.MAIN_HAND) == (entity.getMainArm() == HumanoidArm.LEFT);
		if (!level.isClientSide) {
			// 1.12 divided by the raw charge; a click without wind-up would get infinite gravity.
			float gravity = 0.015f / Math.max(charge, 0.01f);
			level.addFreshEntity(projectile.create(level, entity, GenericProjectile.Kind.BULLET, damage, speed, ttl, spread, radius, radiusMin,
					damageMin, left ? GenericProjectile.LEFT : GenericProjectile.RIGHT, gravity, charge, maxBounces));
		}
		if (!(entity instanceof Player player && player.getAbilities().instabuild)) stack.shrink(1);
		entity.swing(hand);
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
		tooltip.add(GenericGun.stat("damage", damage));
		if (Screen.hasShiftDown()) {
			tooltip.add(GenericGun.stat("radius", radius));
			tooltip.add(GenericGun.stat("bounces", maxBounces));
		} else {
			tooltip.add(Component.translatable("techguns.gun.tooltip.shift1").append(" ")
					.append(Component.translatable("techguns.gun.tooltip.shift2").withStyle(ChatFormatting.GREEN)).append(" ")
					.append(Component.translatable("techguns.gun.tooltip.shift3")).withStyle(ChatFormatting.GRAY));
		}
	}
}
