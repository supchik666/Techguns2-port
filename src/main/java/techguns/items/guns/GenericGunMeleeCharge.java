package techguns.items.guns;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Supplier;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.common.ItemAbility;
import net.neoforged.neoforge.event.entity.player.SweepAttackEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import techguns.TGComponents;
import techguns.TGEffects;
import techguns.Techguns;
import techguns.client.ShooterValues;
import techguns.items.guns.ammo.AmmoType;

/**
 * A powered melee tool (1.12 GenericGunMeleeCharge and its PowerHammer, Chainsaw, MiningDrill and Shishkebap
 * subclasses, here set up by setters): left click hits and mines like a tool, faster and harder with ammo, using one
 * round per block or hit. Upgrade heads ({@link TGComponents#MINING_HEAD}) add harvest level and speed. Right click
 * charges a blow, fires a short hit, or does nothing ({@link RightClick}).
 * ponytail: the TG damage system's armor penetration and knockback, and the drill's sneak silk touch, wait for M4.
 */
@EventBusSubscriber(modid = Techguns.MODID)
public class GenericGunMeleeCharge extends GenericGunCharge {
	public enum RightClick {
		CHARGE, FIRE, NONE
	}

	private static final float MELEE_SOUND_VOLUME = 0.65f;
	/** Server game time from which each player's swing sound may play again (1.12 TGExtendedPlayer.swingSoundDelay). */
	private static final Map<Player, Long> NEXT_SWING_SOUND = new WeakHashMap<>();
	/** Set while the 3x3 around a block is broken, so those blocks cost no ammo and don't break their own 3x3. */
	private static boolean breakingArea;

	/** Tool class name (for the tooltip) to the blocks it mines. */
	final Map<String, TagKey<Block>> tools = new LinkedHashMap<>();
	/** 1.12 harvest level: 0 wood, 1 stone, 2 iron, 3 diamond. ponytail: one level for all tool classes, as every 1.12 tool had. */
	int harvestLevel = -1;
	float digSpeed = 1.0f;
	float meleeDamagePwr, meleeDamageEmpty;
	/** 0 mines one block, 1 a 3x3. */
	int miningRadius;
	int swingSoundDelay = 5;
	List<Supplier<? extends Item>> miningHeads = List.of();
	float digSpeedPerHead = 1.0f;
	float chargedDamagePerHead;
	/** Cancels the vanilla arm swing; the swing plays the tool's own animation instead. */
	boolean hasCustomAnim = true;
	RightClick rightClick = RightClick.CHARGE;
	Supplier<SoundEvent> swingSound, breakSound, sweepSound;
	boolean sweepFX;
	int fireSeconds;
	/** Chance a powered hit's kill bursts into gore (1.12 getMeleeDamageSource). */
	float goreChance = 1;

	public GenericGunMeleeCharge(AmmoType ammoType, boolean semiAuto, int minFiretime, int clipsize, int reloadtime, float damage,
			Supplier<SoundEvent> fireSound, Supplier<SoundEvent> reloadSound, int ttl, float accuracy, float fullChargeTime,
			int ammoConsumedOnFullCharge) {
		super(ammoType, semiAuto, minFiretime, clipsize, reloadtime, damage, fireSound, reloadSound, ttl, accuracy, fullChargeTime,
				ammoConsumedOnFullCharge);
		setShootWithLeftClick(false);
		setNoMuzzleLight();
		setCrossHair(Crosshair.VANILLA);
	}

	public GenericGunMeleeCharge setMeleeDmg(float powered, float unpowered) {
		meleeDamagePwr = powered;
		meleeDamageEmpty = unpowered;
		return this;
	}

	/** "pickaxe", "shovel", "axe" or "sword" (blocks swords cut fast). */
	public GenericGunMeleeCharge setTool(String toolClass, int harvestLevel) {
		tools.put(toolClass, switch (toolClass) {
			case "pickaxe" -> BlockTags.MINEABLE_WITH_PICKAXE;
			case "shovel" -> BlockTags.MINEABLE_WITH_SHOVEL;
			case "axe" -> BlockTags.MINEABLE_WITH_AXE;
			case "sword" -> BlockTags.SWORD_EFFICIENT;
			default -> throw new IllegalArgumentException(toolClass);
		});
		if (this.harvestLevel < 0) this.harvestLevel = harvestLevel;
		return this;
	}

	public GenericGunMeleeCharge setDigSpeed(float speed) {
		digSpeed = speed;
		return this;
	}

	public GenericGunMeleeCharge setMiningRadius(int radius) {
		miningRadius = radius;
		return this;
	}

	public GenericGunMeleeCharge setSwingSoundDelay(int ticks) {
		swingSoundDelay = ticks;
		return this;
	}

	/** The upgrade heads in order; head n gives +n harvest level and n * {@code digSpeedPerHead} speed. */
	@SafeVarargs
	public final GenericGunMeleeCharge setMiningHeads(float digSpeedPerHead, Supplier<? extends Item>... heads) {
		this.digSpeedPerHead = digSpeedPerHead;
		miningHeads = List.of(heads);
		return this;
	}

	/** Extra damage per head level on the charged blow (the power hammer). */
	public GenericGunMeleeCharge setChargedDamagePerHead(float damage) {
		chargedDamagePerHead = damage;
		return this;
	}

	public GenericGunMeleeCharge setHasCustomAnim(boolean customAnim) {
		hasCustomAnim = customAnim;
		return this;
	}

	public GenericGunMeleeCharge setRightClick(RightClick rightClick) {
		this.rightClick = rightClick;
		return this;
	}

	/**
	 * Played on a powered swing, on breaking a block (null: none), and with a powered sweep (null: just vanilla's); with
	 * {@code sweepFX} the sweep throws sparks too.
	 */
	public GenericGunMeleeCharge setMeleeSounds(Supplier<SoundEvent> swing, Supplier<SoundEvent> blockBreak, Supplier<SoundEvent> sweep,
			boolean sweepFX) {
		swingSound = swing;
		breakSound = blockBreak;
		sweepSound = sweep;
		this.sweepFX = sweepFX;
		return this;
	}

	/** Powered hits set the target alight (the shishkebap). */
	public GenericGunMeleeCharge setFireOnHit(int seconds) {
		fireSeconds = seconds;
		return this;
	}

	public GenericGunMeleeCharge setGoreChance(float chance) {
		goreChance = chance;
		return this;
	}

	/** Unpowered hits never gore. */
	public float goreChance(ItemStack stack) {
		return getAmmo(stack) > 0 ? goreChance : 0;
	}

	public int getMiningHead(ItemStack stack) {
		return stack.getOrDefault(TGComponents.MINING_HEAD, 0);
	}

	/** 1 + the index of {@code head} in this tool's heads, 0 if it isn't one. */
	public int getMiningHeadLevelForHead(ItemStack head) {
		for (int i = 0; i < miningHeads.size(); i++) {
			if (head.is(miningHeads.get(i).get())) return i + 1;
		}
		return 0;
	}

	public float getEffectiveDigSpeed(ItemStack stack) {
		return digSpeed + digSpeedPerHead * getMiningHead(stack);
	}

	/** With ammo, on a block of one of its tool classes, or one no tool is for (1.12: no harvest tool). */
	public boolean isEffective(ItemStack stack, BlockState state) {
		if (getAmmo(stack) < 1) return false;
		for (TagKey<Block> tag : tools.values()) {
			if (state.is(tag)) return true;
		}
		return !state.is(BlockTags.MINEABLE_WITH_PICKAXE) && !state.is(BlockTags.MINEABLE_WITH_AXE) && !state.is(BlockTags.MINEABLE_WITH_SHOVEL)
				&& !state.is(BlockTags.MINEABLE_WITH_HOE);
	}

	private Tier tier(ItemStack stack) {
		return switch (harvestLevel + getMiningHead(stack)) {
			case 0 -> Tiers.WOOD;
			case 1 -> Tiers.STONE;
			case 2 -> Tiers.IRON;
			case 3 -> Tiers.DIAMOND;
			default -> Tiers.NETHERITE;
		};
	}

	@Override
	public float getDestroySpeed(ItemStack stack, BlockState state) {
		return isEffective(stack, state) ? getEffectiveDigSpeed(stack) : 1.0f;
	}

	@Override
	public boolean isCorrectToolForDrops(ItemStack stack, BlockState state) {
		return isEffective(stack, state) && !state.is(tier(stack).getIncorrectBlocksForDrops());
	}

	@Override
	public boolean canAttackBlock(BlockState state, Level level, BlockPos pos, Player player) {
		return true;
	}

	@Override
	public boolean mineBlock(ItemStack stack, Level level, BlockState state, BlockPos pos, LivingEntity miningEntity) {
		int ammo = getAmmo(stack);
		if (ammo <= 0 || breakingArea) return false;
		stack.set(TGComponents.AMMO, ammo - 1);
		if (breakSound != null) {
			level.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, breakSound.get(), SoundSource.PLAYERS, MELEE_SOUND_VOLUME, 1.0f);
		}
		return true;
	}

	/** 1.12 onBlockStartBreak: a drill breaking a block breaks the plane around it facing the player too, unless sneaking. */
	@SubscribeEvent
	static void onBreak(BlockEvent.BreakEvent event) {
		if (breakingArea || !(event.getPlayer() instanceof ServerPlayer player) || player.isShiftKeyDown()) return;
		ItemStack stack = player.getMainHandItem();
		if (!(stack.getItem() instanceof GenericGunMeleeCharge tool) || tool.miningRadius <= 0 || !tool.isEffective(stack, event.getState())) return;
		BlockHitResult hit = getPlayerPOVHitResult(player.level(), player, ClipContext.Fluid.NONE);
		if (hit.getType() != HitResult.Type.BLOCK || !hit.getBlockPos().equals(event.getPos())) return;
		ServerLevel level = player.serverLevel();
		Direction.Axis axis = hit.getDirection().getAxis();
		int r = tool.miningRadius;
		breakingArea = true;
		try {
			for (int a = -r; a <= r; a++) {
				for (int b = -r; b <= r; b++) {
					if (a == 0 && b == 0) continue;
					BlockPos pos = switch (axis) {
						case X -> event.getPos().offset(0, a, b);
						case Y -> event.getPos().offset(a, 0, b);
						case Z -> event.getPos().offset(a, b, 0);
					};
					if (!level.isLoaded(pos)) continue;
					BlockState state = level.getBlockState(pos);
					if (!state.isAir() && state.getDestroyProgress(player, level, pos) > 0 && player.hasCorrectToolForDrops(state, level, pos)
							&& tool.isEffective(stack, state)) {
						player.gameMode.destroyBlock(pos);
					}
				}
			}
		} finally {
			breakingArea = false;
		}
	}

	@Override
	public ItemAttributeModifiers getDefaultAttributeModifiers(ItemStack stack) {
		float damage = getAmmo(stack) > 0 ? meleeDamagePwr : meleeDamageEmpty;
		return ItemAttributeModifiers.builder()
				.add(Attributes.ATTACK_DAMAGE, new AttributeModifier(BASE_ATTACK_DAMAGE_ID, damage, AttributeModifier.Operation.ADD_VALUE),
						EquipmentSlotGroup.MAINHAND)
				.add(Attributes.ATTACK_SPEED, new AttributeModifier(BASE_ATTACK_SPEED_ID, -2.4, AttributeModifier.Operation.ADD_VALUE),
						EquipmentSlotGroup.MAINHAND)
				.build();
	}

	@Override
	public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
		int ammo = getAmmo(stack);
		if (ammo <= 0) return true;
		if (!(attacker instanceof Player player && player.getAbilities().instabuild)) stack.set(TGComponents.AMMO, --ammo);
		if (ammo > 0 && fireSeconds > 0) target.igniteForSeconds(fireSeconds);
		return true;
	}

	/** Powered, it sweeps like a sword. */
	@Override
	public boolean canPerformAction(ItemStack stack, ItemAbility ability) {
		return ability == ItemAbilities.SWORD_SWEEP && getAmmo(stack) > 0;
	}

	/** 1.12 doSweepAttackEffect: sparks a block in front of the player and the tool's impact sound, over vanilla's sweep. */
	@SubscribeEvent
	static void onSweep(SweepAttackEvent event) {
		Player player = event.getEntity();
		if (!event.isSweeping() || !(player.level() instanceof ServerLevel level)
				|| !(player.getMainHandItem().getItem() instanceof GenericGunMeleeCharge tool)) return;
		if (tool.sweepSound != null) level.playSound(null, player.getX(), player.getY(), player.getZ(), tool.sweepSound.get(), player.getSoundSource(), 1.0f, 1.0f);
		if (tool.sweepFX) {
			double x = player.getX() - Mth.sin(player.getYRot() * Mth.DEG_TO_RAD);
			double z = player.getZ() + Mth.cos(player.getYRot() * Mth.DEG_TO_RAD);
			TGEffects.sendFX(level, "PowerhammerImpact", x, player.getY() + player.getBbHeight() * 0.8, z, 1.0f, 32);
		}
	}

	/** A powered swing plays the swing sound (at most every swingSoundDelay ticks, so mining loops it) and the tool's animation. */
	@Override
	public boolean onEntitySwing(ItemStack stack, LivingEntity entity, InteractionHand hand) {
		if (getAmmo(stack) <= 0) return false;
		Level level = entity.level();
		if (level.isClientSide) {
			ShooterValues.swung(entity, hand);
		} else if (!(entity instanceof Player player) || player.containerMenu == player.inventoryMenu) {
			long now = level.getGameTime();
			if (now >= NEXT_SWING_SOUND.getOrDefault(entity, 0L)) {
				if (entity instanceof Player swinger) NEXT_SWING_SOUND.put(swinger, now + swingSoundDelay);
				level.playSound(null, entity, swingSound.get(), SoundSource.PLAYERS, MELEE_SOUND_VOLUME, 1.0f);
			}
		}
		return hasCustomAnim;
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		return switch (rightClick) {
			case CHARGE -> super.use(level, player, hand);
			case FIRE -> {
				shootGunPrimary(stack, level, player, hand, false);
				yield InteractionResultHolder.fail(stack);
			}
			case NONE -> InteractionResultHolder.pass(stack);
		};
	}

	@Override
	protected float extraChargedDamage(ItemStack stack) {
		return chargedDamagePerHead * getMiningHead(stack);
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
		super.appendHoverText(stack, context, tooltip, flag);
		if (Screen.hasShiftDown()) {
			if (!miningHeads.isEmpty()) {
				int head = getMiningHead(stack);
				Component name = head > 0 ? miningHeads.get(head - 1).get().getDescription() : Component.translatable("techguns.default");
				tooltip.add(line("techguns.tooltip.mininghead").append(": ").append(name.copy().withStyle(ChatFormatting.WHITE)));
			}
			tooltip.add(line("techguns.tooltip.toolclasses").append(":"));
			int level = harvestLevel + getMiningHead(stack);
			for (String tool : tools.keySet()) tooltip.add(Component.literal(" ").append(line("techguns.toolclass." + tool)).append(": " + level));
			tooltip.add(line("techguns.tooltip.breakspeed").append(": " + getEffectiveDigSpeed(stack)));
			int size = miningRadius * 2 + 1;
			tooltip.add(line("techguns.tooltip.miningradius").append(": " + size + "x" + size));
		} else {
			tooltip.add(line("techguns.tooltip.toolclasses").append(": " + String.join(", ", tools.keySet())));
		}
	}

	private static MutableComponent line(String key) {
		return Component.translatable(key).withStyle(ChatFormatting.GRAY);
	}
}
