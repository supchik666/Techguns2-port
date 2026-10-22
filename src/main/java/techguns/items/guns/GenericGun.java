package techguns.items.guns;

import java.util.List;
import java.util.function.Supplier;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.network.PacketDistributor;
import techguns.TGComponents;
import techguns.client.ShooterValues;
import techguns.entities.projectiles.GenericProjectile;
import techguns.items.guns.ammo.AmmoType;
import techguns.network.TGPackets.GunAnimation;

/**
 * A gun that fires {@link GenericProjectile}s and reloads magazines from the player's inventory.
 * Firing runs on both sides: the client predicts sound, ammo and animation, the server spawns the bullet.
 * ponytail: 1.12 also had zoom, shotgun/burst modes, melee, mining, NPC use, ammo variants and armor accuracy bonuses;
 * they come back with the guns that need them.
 */
public class GenericGun extends Item {
	final AmmoType ammoType;
	final boolean semiAuto;
	final int minFiretime;
	final int clipsize;
	final int reloadtime;
	final float damage;
	final Supplier<SoundEvent> fireSound;
	final Supplier<SoundEvent> reloadSound;
	final int ttl;
	final float accuracy;

	float speed = 2.0f;
	float damageDropStart;
	float damageDropEnd;
	float damageMin;
	int recoiltime = 5;
	int muzzleFlashtime = 5;

	public GenericGun(AmmoType ammoType, boolean semiAuto, int minFiretime, int clipsize, int reloadtime, float damage,
			Supplier<SoundEvent> fireSound, Supplier<SoundEvent> reloadSound, int ttl, float accuracy) {
		super(new Properties().stacksTo(1));
		this.ammoType = ammoType;
		this.semiAuto = semiAuto;
		this.minFiretime = minFiretime;
		this.clipsize = clipsize;
		this.reloadtime = reloadtime;
		this.damage = damage;
		this.fireSound = fireSound;
		this.reloadSound = reloadSound;
		this.ttl = ttl;
		this.accuracy = accuracy;
		this.damageDropStart = ttl;
		this.damageDropEnd = ttl;
		this.damageMin = damage;
	}

	public GenericGun setBulletSpeed(float speed) {
		this.speed = speed;
		return this;
	}

	public GenericGun setDamageDrop(float start, float end, float minDamage) {
		this.damageDropStart = start;
		this.damageDropEnd = end;
		this.damageMin = minDamage;
		return this;
	}

	public GenericGun setRecoiltime(int recoiltime) {
		this.recoiltime = recoiltime;
		return this;
	}

	public boolean isSemiAuto() {
		return semiAuto;
	}

	public float getSpread() {
		return accuracy;
	}

	public int getClipsize() {
		return clipsize;
	}

	public AmmoType getAmmoType() {
		return ammoType;
	}

	public int getAmmo(ItemStack stack) {
		return stack.getOrDefault(TGComponents.AMMO, clipsize);
	}

	public int getRecoiltime() {
		return recoiltime;
	}

	public int getMuzzleFlashtime() {
		return muzzleFlashtime;
	}

	public int getReloadtime() {
		return reloadtime;
	}

	private int getScaledTTL() {
		return (int) Math.ceil(ttl / speed);
	}

	public static boolean isReady(Player player, InteractionHand hand) {
		return player.level().getGameTime() >= player.getData(TGComponents.NEXT_FIRE)[hand.ordinal()];
	}

	private static void setFireDelay(Player player, InteractionHand hand, int ticks) {
		player.getData(TGComponents.NEXT_FIRE)[hand.ordinal()] = player.level().getGameTime() + ticks;
	}

	/** Fires one shot, or swaps in a new magazine when empty. */
	public void shootGunPrimary(ItemStack stack, Level level, Player player, InteractionHand hand) {
		if (!isReady(player, hand)) return;
		int ammo = getAmmo(stack);
		if (ammo <= 0) {
			reload(stack, level, player, hand, 0);
			return;
		}
		setFireDelay(player, hand, minFiretime);
		if (!player.getAbilities().instabuild) stack.set(TGComponents.AMMO, ammo - 1);
		if (level.isClientSide) {
			ShooterValues.fired(player, hand, recoiltime, muzzleFlashtime);
		} else {
			PacketDistributor.sendToPlayersTrackingEntity(player, new GunAnimation(player.getId(), hand, false));
			boolean left = (hand == InteractionHand.MAIN_HAND) == (player.getMainArm() == HumanoidArm.LEFT);
			level.addFreshEntity(new GenericProjectile(level, player, damage, speed, getScaledTTL(), accuracy,
					damageDropStart, damageDropEnd, damageMin, left));
		}
		level.playSound(player, player, fireSound.get(), SoundSource.PLAYERS, 4.0f, 1.0f);
	}

	/** The reload key: swaps the magazine even if it isn't empty, handing back the rounds left in it. */
	public void tryForcedReload(ItemStack stack, Level level, Player player, InteractionHand hand) {
		int ammo = getAmmo(stack);
		if (isReady(player, hand) && ammo < clipsize) reload(stack, level, player, hand, ammo);
	}

	private void reload(ItemStack stack, Level level, Player player, InteractionHand hand, int oldAmmo) {
		Inventory inventory = player.getInventory();
		int slot = inventory.findSlotMatchingItem(new ItemStack(ammoType.ammo().get()));
		if (slot < 0) return;
		inventory.removeItem(slot, 1);
		give(player, new ItemStack(ammoType.emptyMag().get()));
		if (oldAmmo > 0) {
			int bulletsBack = (int) (oldAmmo / ammoType.getShotsPerBullet(clipsize, oldAmmo));
			if (bulletsBack > 0) give(player, new ItemStack(ammoType.bullet().get(), bulletsBack));
		}
		setFireDelay(player, hand, reloadtime - minFiretime);
		stack.set(TGComponents.AMMO, clipsize);
		if (level.isClientSide) {
			ShooterValues.reloaded(player, hand, reloadtime);
		} else {
			PacketDistributor.sendToPlayersTrackingEntity(player, new GunAnimation(player.getId(), hand, true));
		}
		level.playSound(player, player, reloadSound.get(), SoundSource.PLAYERS, 1.0f, 1.0f);
	}

	private static void give(Player player, ItemStack stack) {
		if (!player.getInventory().add(stack) && !player.level().isClientSide) player.drop(stack, false);
	}

	@Override
	public boolean canAttackBlock(BlockState state, Level level, BlockPos pos, Player player) {
		return false;
	}

	@Override
	public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
		return slotChanged || !oldStack.is(newStack.getItem());
	}

	@Override
	public boolean isBarVisible(ItemStack stack) {
		return true;
	}

	@Override
	public int getBarWidth(ItemStack stack) {
		return Math.round(13.0f * getAmmo(stack) / clipsize);
	}

	@Override
	public int getBarColor(ItemStack stack) {
		return Mth.hsvToRgb((float) getAmmo(stack) / clipsize / 3.0f, 1.0f, 1.0f);
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
		tooltip.add(stat("ammo", ammoType.ammo().get().getDescription().copy().withStyle(ChatFormatting.WHITE)));
		tooltip.add(stat("damage", damage + (damageMin < damage ? "-" + damageMin : "")));
		if (Screen.hasShiftDown()) {
			tooltip.add(stat("range", String.format("%.1f,%.1f,%d", damageDropStart, damageDropEnd, ttl)));
			tooltip.add(stat("velocity", String.format("%.1f", speed)));
			tooltip.add(stat("spread", accuracy));
			tooltip.add(stat("clipsize", clipsize));
			tooltip.add(stat("reloadTime", reloadtime * 0.05f + "s"));
		} else {
			tooltip.add(Component.translatable("techguns.gun.tooltip.shift1").append(" ")
					.append(Component.translatable("techguns.gun.tooltip.shift2").withStyle(ChatFormatting.GREEN)).append(" ")
					.append(Component.translatable("techguns.gun.tooltip.shift3")).withStyle(ChatFormatting.GRAY));
		}
	}

	private static Component stat(String key, Object value) {
		MutableComponent line = Component.translatable("techguns.gun.tooltip." + key).append(": ");
		return line.append(value instanceof Component c ? c : Component.literal(String.valueOf(value))).withStyle(ChatFormatting.GRAY);
	}
}
