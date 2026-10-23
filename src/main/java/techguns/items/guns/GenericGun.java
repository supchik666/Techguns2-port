package techguns.items.guns;

import java.util.List;
import java.util.function.Supplier;
import java.util.stream.IntStream;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.network.PacketDistributor;
import techguns.TGComponents;
import techguns.capabilities.TGExtendedPlayer;
import techguns.Techguns;
import techguns.client.ShooterValues;
import techguns.entities.npcs.RangedGunAttackGoal;
import techguns.damagesystem.TGDamageType;
import techguns.entities.projectiles.GenericProjectile;
import techguns.items.armors.GenericArmor;
import techguns.items.armors.TGArmorBonus;
import techguns.items.guns.ammo.AmmoType;
import techguns.network.TGPackets.GunAnimation;

/**
 * A gun that fires {@link GenericProjectile}s and reloads magazines (or loose rounds) from the player's inventory.
 * Firing runs on both sides: the client predicts sound, ammo and animation, the server spawns the bullets.
 * Melee and mining are in {@link GenericGunMeleeCharge}. NPCs fire through {@link #fireWeaponFromNPC} and {@link #getAIAttack}.
 * ponytail: 1.12 also had silencers; they come back with the systems that need them.
 */
public class GenericGun extends Item {
	public enum GunHandType {
		ONE_HANDED, ONE_POINT_FIVE_HANDED, TWO_HANDED
	}

	/** 1.12 EnumCrosshairStyle; the order is the column in tg_crosshairs.png. */
	public enum Crosshair {
		VANILLA, GUN_DYNAMIC, TRI, QUAD_CORNERS_DOT, TRI_INV, FOUR_PARTS, FOUR_PARTS_SPIKED, HORIZONTAL_TWO_PART, HORIZONTAL_TWO_PART_LARGE,
		HORIZONTAL_TWO_PART_E, HORIZONTAL_SMALL, QUAD_NO_CORNERS
	}

	/** How the tooltip shows range: drop start, end and max range, just the range (beams), or the blast radius (explosives). */
	public enum RangeTooltipType {
		DROP, NO_DROP, RADIUS
	}

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
	/** Color of the muzzle flash light; null for none. */
	float[] muzzleLight = {1f, 0.9f, 0.2f};
	/** Rounds loaded per reload for guns fed loose shells; 1 means a full clip from one magazine. */
	int ammoCount = 1;
	/** Every gun aims like the LMG unless it sets its own zoom or {@link #setNoZoom()} (not in 1.12, where most guns couldn't). */
	float zoomMult = 0.75f;
	/** Spread multiplier while zoomed. */
	float zoombonus = 0.75f;
	boolean fireCenteredZoomed;
	boolean shotgun;
	/** Spread of the extra shotgun pellets; the crosshair spacing too, as in 1.12. */
	float spread = 0.015f;
	int bulletcount = 7;
	boolean burst;
	double gravity;
	/** Armor piercing, see {@link techguns.damagesystem.DamageSystem}. */
	float penetration;
	Supplier<SoundEvent> rechamberSound;
	float forwardOffset;
	GunHandType handType = GunHandType.TWO_HANDED;
	Crosshair crosshair = Crosshair.GUN_DYNAMIC;
	List<ResourceLocation> textures = List.of();
	ProjectileFactory projectile = GenericProjectile::new;
	/** Looping guns play this instead of the fire sound when they haven't fired for {@code maxLoopDelay} ticks. */
	Supplier<SoundEvent> firesoundStart;
	int maxLoopDelay;
	/** Rapid-fire guns let the recoil animation or muzzle flash finish instead of restarting it every shot. */
	boolean checkRecoil;
	boolean checkMuzzleFlash;
	RangeTooltipType rangeTooltipType = RangeTooltipType.DROP;
	/** Melee tools attack and mine with left click instead of firing. */
	boolean shootWithLeftClick = true;
	/** Held aimed (the bow pose); off for the one-handed shishkebap. */
	boolean bowAnim = true;
	/** TGFX played on the holder while the gun has ammo, or null (1.12 setHasAmbient + the renderer's ambient FX). */
	String ambientFX;
	/** NPC use (1.12 setAIStats): attack range in blocks, ticks between attacks, shots per burst (0 for none) and ticks between them. */
	float aiRange = 15.0f;
	int aiAttackTime = 60;
	int aiBurstCount;
	int aiBurstDelay;

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

	public GenericGun setMuzzleFlashTime(int time) {
		this.muzzleFlashtime = time;
		return this;
	}

	public GenericGun setMuzzleLight(float r, float g, float b) {
		muzzleLight = new float[] {r, g, b};
		return this;
	}

	/** No muzzle flash light: energy guns, whose light was too much. */
	public GenericGun setNoMuzzleLight() {
		muzzleLight = null;
		return this;
	}

	public GenericGun setAmmoCount(int count) {
		this.ammoCount = count;
		return this;
	}

	/** Heavy guns (and charge guns, whose right click charges) don't aim. */
	public GenericGun setNoZoom() {
		this.zoomMult = this.zoombonus = 1.0f;
		return this;
	}

	/** {@code toggle} is always true for the ported guns; hold-to-zoom isn't implemented. */
	public GenericGun setZoom(float mult, boolean toggle, float bonus, boolean fireCenteredWhileZooming) {
		this.zoomMult = mult;
		this.zoombonus = bonus;
		this.fireCenteredZoomed = fireCenteredWhileZooming;
		return this;
	}

	/** Fires {@code count} extra pellets with {@code spread}; a burst staggers them along the flight path. */
	public GenericGun setShotgunSpread(int count, float spread, boolean burst) {
		this.shotgun = true;
		this.spread = spread;
		this.bulletcount = count;
		this.burst = burst;
		return this;
	}

	public GenericGun setGravity(double gravity) {
		this.gravity = gravity;
		return this;
	}

	public GenericGun setPenetration(float penetration) {
		this.penetration = penetration;
		return this;
	}

	public GenericGun setRechamberSound(Supplier<SoundEvent> sound) {
		this.rechamberSound = sound;
		return this;
	}

	public GenericGun setForwardOffset(float offset) {
		this.forwardOffset = offset;
		return this;
	}

	public GenericGun setHandType(GunHandType type) {
		this.handType = type;
		return this;
	}

	/** The projectile class; the ammo variant's {@link GenericProjectile.Kind} is passed to it. */
	public GenericGun setProjectile(ProjectileFactory projectile) {
		this.projectile = projectile;
		return this;
	}

	public GenericGun setFiresoundStart(Supplier<SoundEvent> sound) {
		this.firesoundStart = sound;
		return this;
	}

	public GenericGun setMaxLoopDelay(int ticks) {
		this.maxLoopDelay = ticks;
		return this;
	}

	public GenericGun setCheckRecoil() {
		this.checkRecoil = true;
		return this;
	}

	public GenericGun setCheckMuzzleFlash() {
		this.checkMuzzleFlash = true;
		return this;
	}

	public GenericGun setRangeTooltipType(RangeTooltipType type) {
		this.rangeTooltipType = type;
		return this;
	}

	public GenericGun setShootWithLeftClick(boolean shoot) {
		this.shootWithLeftClick = shoot;
		return this;
	}

	public GenericGun setNoBowAnim() {
		this.bowAnim = false;
		return this;
	}

	public GenericGun setAmbientFX(String fx) {
		this.ambientFX = fx;
		return this;
	}

	public GenericGun setCrossHair(Crosshair crosshair) {
		this.crosshair = crosshair;
		return this;
	}

	public GenericGun setTexture(String path) {
		return setTextures(path, 1);
	}

	/** Camo textures: {@code path.png}, {@code path_1.png}... */
	public GenericGun setTextures(String path, int variations) {
		textures = IntStream.range(0, variations).mapToObj(i -> Techguns.id(path + (i == 0 ? "" : "_" + i) + ".png")).toList();
		return this;
	}

	/** Camos the camo bench cycles through. */
	public GenericGun setAIStats(float range, int attackTime, int burstCount, int burstDelay) {
		this.aiRange = range;
		this.aiAttackTime = attackTime;
		this.aiBurstCount = burstCount;
		this.aiBurstDelay = burstDelay;
		return this;
	}

	/** How far NPCs and turrets holding this gun shoot. */
	public float getAIRange() {
		return aiRange;
	}

	/** The AI goal a mob holding this gun attacks with (1.12 getAIAttack). */
	public <T extends Mob & RangedAttackMob> Goal getAIAttack(T mob) {
		return new RangedGunAttackGoal<>(mob, 1.0, aiAttackTime / 3, aiAttackTime, aiRange, aiBurstCount, aiBurstDelay);
	}

	public int camoCount() {
		return textures.size();
	}

	public ResourceLocation getTexture(ItemStack stack) {
		int camo = stack.getOrDefault(TGComponents.CAMO, 0);
		return textures.get(camo < textures.size() ? camo : 0);
	}

	public boolean isShootWithLeftClick() {
		return shootWithLeftClick;
	}

	public boolean hasBowAnim() {
		return bowAnim;
	}

	public String getAmbientFX() {
		return ambientFX;
	}

	public boolean isSemiAuto() {
		return semiAuto;
	}

	public float getSpread() {
		return spread;
	}

	public float getZoomMult() {
		return zoomMult;
	}

	public boolean canZoom() {
		return zoomMult != 1.0f;
	}

	public GunHandType getHandType() {
		return handType;
	}

	/** Two one- or 1.5-handed guns (not the shishkebap): left click fires the main hand, right click the off hand, and neither aims. */
	public static boolean isDualWielding(Player player) {
		return isDualGun(player.getMainHandItem()) && isDualGun(player.getOffhandItem());
	}

	private static boolean isDualGun(ItemStack stack) {
		return stack.getItem() instanceof GenericGun gun && gun.handType != GunHandType.TWO_HANDED && !(gun instanceof GenericGunCharge);
	}

	public Crosshair getCrosshair() {
		return crosshair;
	}

	public int getAmmoCount() {
		return ammoCount;
	}

	public AmmoType.Variant getAmmoVariant(ItemStack stack) {
		return ammoType.get(stack.getOrDefault(TGComponents.AMMO_VARIANT, AmmoType.DEFAULT));
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

	public float[] getMuzzleLight() {
		return muzzleLight;
	}

	public boolean isCheckRecoil() {
		return checkRecoil;
	}

	public boolean isCheckMuzzleFlash() {
		return checkMuzzleFlash;
	}

	public int getReloadtime() {
		return reloadtime;
	}

	int getScaledTTL() {
		return (int) Math.ceil(ttl / speed);
	}

	public static boolean isReady(Player player, InteractionHand hand) {
		return player.level().getGameTime() >= player.getData(TGComponents.NEXT_FIRE)[hand.ordinal()];
	}

	static void setFireDelay(Player player, InteractionHand hand, int ticks) {
		player.getData(TGComponents.NEXT_FIRE)[hand.ordinal()] = player.level().getGameTime() + ticks;
	}

	/** Spread multiplier (not in 1.12): aiming while sneaking cuts it to a tenth; otherwise twice the spread unless sneaking, times the zoom bonus. */
	public float stanceSpread(Player player, boolean zooming) {
		if (!player.isCrouching()) return zooming ? 2.0f * zoombonus : 2.0f;
		return zooming ? Math.min(zoombonus, 0.1f) : 1.0f;
	}

	/**
	 * Fires one shot (plus shotgun pellets), or reloads when empty. {@code zooming} is the shooter's client zoom state:
	 * it tightens the spread and may fire from the eyes.
	 */
	public void shootGunPrimary(ItemStack stack, Level level, Player player, InteractionHand hand, boolean zooming) {
		shootGunPrimary(stack, level, player, hand, zooming, null);
	}

	/** As above; {@code target} is the guided missile launcher's lock-on target, null for other guns. */
	public void shootGunPrimary(ItemStack stack, Level level, Player player, InteractionHand hand, boolean zooming, Entity target) {
		if (!isReady(player, hand)) return;
		if (isDualWielding(player)) zooming = false;
		int ammo = getAmmo(stack);
		if (ammo <= 0) {
			reload(stack, level, player, hand, 0);
			return;
		}
		// ponytail: 1.12 counted a loop sound delay down per tick; ticks since the last shot (from the fire delay) do the same
		long sinceLastShot = level.getGameTime() - player.getData(TGComponents.NEXT_FIRE)[hand.ordinal()] + minFiretime;
		boolean loopStart = maxLoopDelay > 0 && sinceLastShot >= maxLoopDelay;
		setFireDelay(player, hand, minFiretime);
		if (!player.getAbilities().instabuild) stack.set(TGComponents.AMMO, ammo - 1);
		if (level.isClientSide) {
			ShooterValues.fired(player, hand, this);
		} else {
			PacketDistributor.sendToPlayersTrackingEntity(player, new GunAnimation(player.getId(), hand, false));
			float bonus = Mth.clamp(1 - GenericArmor.bonus(player, TGArmorBonus.GUN_ACCURACY, false), 0, 1) * stanceSpread(player, zooming);
			if (hand == InteractionHand.MAIN_HAND && player.isBlocking()) bonus *= handType == GunHandType.ONE_HANDED ? 4.0f : 8.0f;
			boolean left = (hand == InteractionHand.MAIN_HAND) == (player.getMainArm() == HumanoidArm.LEFT);
			float side = left ? GenericProjectile.LEFT : GenericProjectile.RIGHT;
			if (zooming && fireCenteredZoomed) side = GenericProjectile.CENTER;
			fireProjectiles(level, player, stack, bonus, side, 1.0f, target);
		}
		level.playSound(player, player, (loopStart ? firesoundStart : fireSound).get(), SoundSource.PLAYERS, 4.0f, 1.0f);
		if (rechamberSound != null) level.playSound(player, player, rechamberSound.get(), SoundSource.PLAYERS, 1.0f, 1.0f);
	}

	/** An NPC shot from the shooter's main arm, see below. */
	public void fireWeaponFromNPC(LivingEntity shooter, ItemStack stack, float damageScale, float accuracyScale) {
		fireWeaponFromNPC(shooter, stack, damageScale, accuracyScale,
				shooter.getMainArm() == HumanoidArm.LEFT ? GenericProjectile.LEFT : GenericProjectile.RIGHT);
	}

	/**
	 * A server side NPC or turret shot along the shooter's head rotation (1.12 fireWeaponFromNPC): no ammo or fire delay
	 * (the AI goal paces shots), damage times {@code damageScale}, spread times the zoom bonus and {@code accuracyScale}.
	 * {@code side} places the muzzle: {@link GenericProjectile#CENTER} for turrets.
	 */
	public void fireWeaponFromNPC(LivingEntity shooter, ItemStack stack, float damageScale, float accuracyScale, float side) {
		Level level = shooter.level();
		PacketDistributor.sendToPlayersTrackingEntity(shooter, new GunAnimation(shooter.getId(), InteractionHand.MAIN_HAND, false));
		fireProjectiles(level, shooter, stack, zoombonus * accuracyScale, side, damageScale, null);
		level.playSound(null, shooter, fireSound.get(), SoundSource.HOSTILE, 4.0f, 1.0f);
		if (rechamberSound != null) level.playSound(null, shooter, rechamberSound.get(), SoundSource.HOSTILE, 1.0f, 1.0f);
	}

	/** Spawns the shot and any shotgun pellets, with spread times {@code bonus}. */
	void fireProjectiles(Level level, LivingEntity shooter, ItemStack stack, float bonus, float side, float damageScale, Entity target) {
		GenericProjectile.Kind kind = getAmmoVariant(stack).kind();
		spawnProjectile(level, shooter, kind, accuracy * bonus, forwardOffset, side, damageScale, target);
		if (shotgun) {
			float offset = burst ? speed / bulletcount : 0;
			for (int i = 0; i < bulletcount; i++) {
				spawnProjectile(level, shooter, kind, spread * bonus, forwardOffset + offset * (i + 1.0f), side, damageScale, target);
			}
		}
	}

	protected void spawnProjectile(Level level, LivingEntity shooter, GenericProjectile.Kind kind, float spread, float offset, float side,
			float damageScale, Entity target) {
		level.addFreshEntity(createProjectile(level, shooter, kind, spread, offset, side, damageScale));
	}

	protected GenericProjectile createProjectile(Level level, LivingEntity shooter, GenericProjectile.Kind kind, float spread, float offset,
			float side, float damageScale) {
		GenericProjectile bullet = projectile.create(level, shooter, kind, damage * damageScale, speed, getScaledTTL(), spread,
				damageDropStart, damageDropEnd, damageMin * damageScale, side, gravity);
		if (offset > 0) bullet.shiftForward(offset / speed);
		bullet.setPenetration(penetration);
		return bullet;
	}

	/** The reload key: reloads even if not empty. A magazine hands back the rounds left in it; loose rounds are topped up. */
	public void tryForcedReload(ItemStack stack, Level level, Player player, InteractionHand hand) {
		int ammo = getAmmo(stack);
		if (isReady(player, hand) && ammo < clipsize) reload(stack, level, player, hand, ammo);
	}

	/** Where a reload takes rounds from and puts empties: the player's inventory, or a turret's. */
	public interface AmmoSource {
		boolean consume(Item item);

		void give(ItemStack stack);

		int count(Item item);
	}

	/**
	 * Loads {@code stack} from {@code source} (magazine swap, or loose rounds topped up from {@code oldAmmo}); false, and no
	 * change, if there were no rounds.
	 */
	public boolean load(ItemStack stack, AmmoSource source, int oldAmmo) {
		AmmoType.Variant variant = getAmmoVariant(stack);
		Item extra = ammoType.extraAmmo() == null ? null : ammoType.extraAmmo().get();
		if (extra != null && source.count(extra) == 0) return false;
		if (!source.consume(variant.ammo().get())) return false;
		if (extra != null) {
			source.consume(extra);
			source.give(new ItemStack(ammoType.extraEmpty().get()));
		}
		int newAmmo;
		if (ammoCount > 1) {
			int loaded = 1;
			while (loaded < ammoCount - oldAmmo && source.consume(variant.ammo().get())) loaded++;
			newAmmo = oldAmmo + loaded;
		} else {
			if (ammoType.emptyMag() != null) {
				source.give(new ItemStack(ammoType.emptyMag().get()));
				int bulletsBack = oldAmmo > 0 ? (int) (oldAmmo / ammoType.getShotsPerBullet(clipsize, oldAmmo)) : 0;
				if (bulletsBack > 0 && variant.bullet() != null) source.give(new ItemStack(variant.bullet().get(), bulletsBack));
			}
			newAmmo = clipsize; // loose rounds left in the gun are lost, as in 1.12
		}
		stack.set(TGComponents.AMMO, newAmmo);
		return true;
	}

	void reload(ItemStack stack, Level level, Player player, InteractionHand hand, int oldAmmo) {
		if (!load(stack, new AmmoSource() {
			@Override
			public boolean consume(Item item) {
				return TGExtendedPlayer.consumeAmmo(player, item);
			}

			@Override
			public void give(ItemStack given) {
				TGExtendedPlayer.giveAmmo(player, given);
			}

			@Override
			public int count(Item item) {
				return TGExtendedPlayer.countAmmo(player, item);
			}
		}, oldAmmo)) return;
		setFireDelay(player, hand, reloadtime - minFiretime);
		if (level.isClientSide) {
			ShooterValues.reloaded(player, hand, reloadtime);
		} else {
			PacketDistributor.sendToPlayersTrackingEntity(player, new GunAnimation(player.getId(), hand, true));
		}
		level.playSound(player, player, reloadSound.get(), SoundSource.PLAYERS, 1.0f, 1.0f);
	}

	public static boolean consume(Player player, Item item) {
		Inventory inventory = player.getInventory();
		int slot = inventory.findSlotMatchingItem(new ItemStack(item));
		if (slot < 0) return false;
		inventory.removeItem(slot, 1);
		return true;
	}

	public static void give(Player player, ItemStack stack) {
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
		Component ammo = getAmmoVariant(stack).ammo().get().getDescription().copy().withStyle(ChatFormatting.WHITE);
		tooltip.add(stat("ammo", ammoCount > 1 ? Component.literal(ammoCount + "x ").append(ammo) : ammo));
		tooltip.add(stat("damage", damage + (damageMin < damage ? "-" + damageMin : "") + (shotgun ? " (x" + (bulletcount + 1) + ")" : "")));
		if (Screen.hasShiftDown()) {
			tooltip.add(switch (rangeTooltipType) {
				case DROP -> stat("range", String.format("%.1f,%.1f,%d", damageDropStart, damageDropEnd, ttl));
				case NO_DROP -> stat("range", String.format("%.1f", damageDropStart));
				case RADIUS -> stat("radius", String.format("%.1f-%.1f", damageDropStart, damageDropEnd));
			});
			tooltip.add(stat("velocity", String.format("%.1f", speed)));
			tooltip.add(stat("spread", accuracy));
			tooltip.add(stat("clipsize", clipsize));
			tooltip.add(stat("reloadTime", reloadtime * 0.05f + "s"));
			float pen = techguns.damagesystem.DamageSystem.penetrationPercent(penetration);
			techguns.items.armors.GenericArmor.resistances(tooltip, Component.translatable("techguns.gun.tooltip.armorPen"), t -> t == TGDamageType.RADIATION ? null : pen, "%");
		} else {
			tooltip.add(Component.translatable("techguns.gun.tooltip.shift1").append(" ")
					.append(Component.translatable("techguns.gun.tooltip.shift2").withStyle(ChatFormatting.GREEN)).append(" ")
					.append(Component.translatable("techguns.gun.tooltip.shift3")).withStyle(ChatFormatting.GRAY));
		}
	}

	static Component stat(String key, Object value) {
		MutableComponent line = Component.translatable("techguns.gun.tooltip." + key).append(": ");
		return line.append(value instanceof Component c ? c : Component.literal(String.valueOf(value))).withStyle(ChatFormatting.GRAY);
	}
}
