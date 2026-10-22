package techguns.items.guns;

import java.util.function.Supplier;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import techguns.TGComponents;
import techguns.TGEffects;
import techguns.client.ShooterValues;
import techguns.entities.projectiles.GenericProjectile;
import techguns.items.guns.ammo.AmmoType;
import techguns.network.TGPackets.GunAnimation;

/**
 * A gun that charges while right click is held and fires on release (1.12 GenericGunCharge): the longer the charge (up
 * to {@code fullChargeTime} ticks) the more ammo the shot uses and the stronger it is. Left click still fires a plain,
 * uncharged shot. ponytail: shotgun pellets on charged shots are skipped (no charge gun is a shotgun); the charge sound
 * plays on to its end after release.
 */
public class GenericGunCharge extends GenericGun {
	final float fullChargeTime;
	final int ammoConsumedOnFullCharge;
	ChargedProjectileFactory chargedProjectile;
	boolean hasChargedFireAnim = true;
	boolean canFireWhileCharging;
	Supplier<SoundEvent> chargeSound;
	String chargeFX;
	Vec3 chargeFXOffset = Vec3.ZERO;

	public GenericGunCharge(AmmoType ammoType, boolean semiAuto, int minFiretime, int clipsize, int reloadtime, float damage,
			Supplier<SoundEvent> fireSound, Supplier<SoundEvent> reloadSound, int ttl, float accuracy, float fullChargeTime,
			int ammoConsumedOnFullCharge) {
		super(ammoType, semiAuto, minFiretime, clipsize, reloadtime, damage, fireSound, reloadSound, ttl, accuracy);
		this.fullChargeTime = fullChargeTime;
		this.ammoConsumedOnFullCharge = ammoConsumedOnFullCharge;
	}

	/** The charged projectile; an uncharged left-click shot is the same one with charge 0, as in 1.12. */
	public GenericGunCharge setChargedProjectile(ChargedProjectileFactory factory) {
		this.chargedProjectile = factory;
		this.projectile = (level, shooter, kind, damage, speed, ttl, spread, dds, dde, dmin, side, gravity) ->
				factory.create(level, shooter, kind, damage, speed, ttl, spread, dds, dde, dmin, side, gravity, 0.0f, 1);
		return this;
	}

	public GenericGunCharge setChargeFireAnims(boolean hasAnims) {
		this.hasChargedFireAnim = hasAnims;
		return this;
	}

	public GenericGunCharge setFireWhileCharging(boolean canFire) {
		this.canFireWhileCharging = canFire;
		return this;
	}

	public GenericGunCharge setChargeSound(Supplier<SoundEvent> sound) {
		this.chargeSound = sound;
		return this;
	}

	/** An FX started on the shooter when charging begins; the x offset is mirrored for left-handed players. */
	public GenericGunCharge setChargeFX(String fx, float x, float y, float z) {
		this.chargeFX = fx;
		this.chargeFXOffset = new Vec3(x, y, z);
		return this;
	}

	public float getFullChargeTime() {
		return fullChargeTime;
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (!isReady(player, hand)) return InteractionResultHolder.pass(stack);
		if (getAmmo(stack) <= 0) {
			reload(stack, level, player, hand, 0);
			return InteractionResultHolder.pass(stack);
		}
		setFireDelay(player, hand, minFiretime);
		player.startUsingItem(hand);
		startCharge(stack, level, player);
		if (chargeSound != null) level.playSound(player, player, chargeSound.get(), SoundSource.PLAYERS, 4.0f, 1.0f);
		if (chargeFX != null) {
			Vec3 offset = player.getMainArm() == HumanoidArm.RIGHT ? chargeFXOffset : chargeFXOffset.multiply(-1, 1, 1);
			TGEffects.createFXOnEntity(player, chargeFX, offset, true, GenericGunCharge::isCharging);
		}
		return InteractionResultHolder.consume(stack);
	}

	/** 1.12 EntityCondition.CHARGING_WEAPON: the charge FX ends with the charge. */
	static boolean isCharging(Entity entity) {
		return entity instanceof LivingEntity living && living.isAlive() && living.getUseItem().getItem() instanceof GenericGunCharge;
	}

	/** Called on both sides when charging starts. */
	protected void startCharge(ItemStack stack, Level level, Player player) {}

	@Override
	public void shootGunPrimary(ItemStack stack, Level level, Player player, InteractionHand hand, boolean zooming,
			Entity target) {
		if (canFireWhileCharging || player.getUseItem() != stack) super.shootGunPrimary(stack, level, player, hand, zooming, target);
	}

	@Override
	public int getUseDuration(ItemStack stack, LivingEntity entity) {
		return 288000;
	}

	@Override
	public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
		if (!(entity instanceof Player player)) return;
		float charge = Math.min((getUseDuration(stack, entity) - timeLeft) / fullChargeTime, 1.0f);
		int wanted = (int) Math.ceil(charge * ammoConsumedOnFullCharge);
		int ammoConsumed = wanted;
		if (!player.getAbilities().instabuild) {
			int ammo = getAmmo(stack);
			ammoConsumed = Math.min(wanted, ammo);
			stack.set(TGComponents.AMMO, ammo - ammoConsumed);
		}
		if (ammoConsumed < wanted) charge = (float) ammoConsumed / ammoConsumedOnFullCharge;
		InteractionHand hand = player.getUsedItemHand();
		if (!level.isClientSide) {
			// The charged shot always comes from the main arm's side, as in 1.12.
			float side = player.getMainArm() == HumanoidArm.RIGHT ? GenericProjectile.RIGHT : GenericProjectile.LEFT;
			GenericProjectile.Kind kind = getAmmoVariant(stack).kind();
			GenericProjectile proj = chargedProjectile.create(level, player, kind, damage, speed, getScaledTTL(), accuracy, damageDropStart,
					damageDropEnd, damageMin, side, gravity, charge, ammoConsumed);
			if (proj != null) level.addFreshEntity(proj);
			if (hasChargedFireAnim) PacketDistributor.sendToPlayersTrackingEntity(player, new GunAnimation(player.getId(), hand, false));
		} else if (hasChargedFireAnim) {
			ShooterValues.fired(player, hand, this);
			ShooterValues.get(player, hand).recoilCharge = charge;
		}
		if (hasChargedFireAnim) {
			level.playSound(player, player, fireSound.get(), SoundSource.PLAYERS, 4.0f, 1.0f);
			if (rechamberSound != null) level.playSound(player, player, rechamberSound.get(), SoundSource.PLAYERS, 1.0f, 1.0f);
		}
	}
}
