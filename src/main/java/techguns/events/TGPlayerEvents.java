package techguns.events;

import io.netty.buffer.ByteBuf;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingEvent;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import techguns.TGConfig;
import techguns.TGEffects;
import techguns.TGItems;
import techguns.TGPermissions;
import techguns.TGSounds;
import techguns.Techguns;
import techguns.capabilities.TGExtendedPlayer;
import techguns.gui.TGPlayerInventoryMenu;
import techguns.items.additionalslots.ItemAntiGravPack;
import techguns.items.additionalslots.ItemGlider;
import techguns.items.additionalslots.ItemJetpack;
import techguns.items.additionalslots.ItemJumpPack;
import techguns.items.additionalslots.ItemNightVisionGoggles;
import techguns.items.additionalslots.ItemTGSpecialSlot;
import techguns.items.armors.GenericArmor;
import techguns.items.armors.TGArmorBonus;

/**
 * What the extra slots do (1.12 TGTickHandler and TGEventHandler, slot parts) and the toggle keys' packet.
 * ponytail: the slot items apply their own effects here instead of through the armor bonus sum (1.12
 * GenericArmor.getArmorBonusForPlayer), so armor bonuses must not count them again.
 */
@EventBusSubscriber(modid = Techguns.MODID)
public class TGPlayerEvents {
	private static final ResourceLocation ANTIGRAV_FLIGHT = Techguns.id("antigrav_flight");
	private static final double GLOVES_BREAKSPEED = 0.10;

	/** 0-4 are the inventory screen's toggle buttons, in order. */
	public enum Action {
		HUD, NIGHTVISION, SAFEMODE, STEP_ASSIST, JETPACK, BOOST_START, BOOST_STOP, OPEN_INVENTORY, JUMP_BOOST
	}

	/** Client pressed a toggle key or button (1.12 PacketTGKeybindPress); {@code showMsg} confirms it in the action bar. */
	public record PlayerAction(Action action, boolean showMsg) implements CustomPacketPayload {
		public static final Type<PlayerAction> TYPE = new Type<>(Techguns.id("player_action"));
		static final StreamCodec<ByteBuf, PlayerAction> CODEC = StreamCodec.composite(ByteBufCodecs.idMapper(i -> Action.values()[i], Action::ordinal),
				PlayerAction::action, ByteBufCodecs.BOOL, PlayerAction::showMsg, PlayerAction::new);

		@Override
		public Type<PlayerAction> type() {
			return TYPE;
		}
	}

	@SubscribeEvent
	static void onPlayerTick(PlayerTickEvent.Post event) {
		Player player = event.getEntity();
		TGExtendedPlayer tg = TGExtendedPlayer.get(player);
		if (player.level().isClientSide && !player.isLocalPlayer()) {
			// other players: guessed from how they move, for the glider model and sound
			Object back = tg.getSlot(TGExtendedPlayer.SLOT_BACK).getItem();
			tg.isGliding = (back instanceof ItemGlider || back instanceof ItemJetpack) && !player.onGround() && player.isCrouching() && player.getY() < player.yo;
			return;
		}
		tickSlots(player, tg);
		if (player.level().isClientSide) return;
		if (!TGConfig.DISABLE_AUTOFEEDER.getAsBoolean()) autofeed(player, tg);
		if (tg.enableNightVision && ItemNightVisionGoggles.givesNightVision(tg.getSlot(TGExtendedPlayer.SLOT_FACE))) {
			player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 220, 0, false, false));
		}
		antiGrav(player, tg);
		tg.syncIfChanged(player);
	}

	/** The face, back and hand items' own ticks. */
	public static void tickSlots(Player player, TGExtendedPlayer tg) {
		tg.isGliding = false;
		for (int i = TGExtendedPlayer.SLOT_FACE; i <= TGExtendedPlayer.SLOT_HAND; i++) {
			ItemStack stack = tg.getSlot(i);
			if (stack.getItem() instanceof ItemTGSpecialSlot item) item.onPlayerTick(stack, player, tg);
		}
	}

	/** Tops up hunger from what's left of the last meal, else eats from the food slots, keeping what doesn't fit for later. */
	public static void autofeed(Player player, TGExtendedPlayer tg) {
		int needed = 20 - player.getFoodData().getFoodLevel();
		if (needed <= 0) return;
		if (tg.foodleft > 0) {
			int eat = Math.min(needed, tg.foodleft);
			player.getFoodData().eat(eat, tg.lastSaturation);
			tg.foodleft -= (short) eat;
			if (tg.foodleft == 0) tg.lastSaturation = 0;
			return;
		}
		for (int i = TGExtendedPlayer.SLOTS_AUTOFOOD_START; i <= TGExtendedPlayer.SLOTS_AUTOFOOD_END; i++) {
			ItemStack stack = tg.getSlot(i);
			FoodProperties food = stack.getFoodProperties(player);
			if (food == null) continue;
			float modifier = food.nutrition() > 0 ? food.saturation() / (2.0f * food.nutrition()) : 0;
			// eat() for the effects, sounds, stats and leftover bowl; the nutrition goes in below
			ItemStack rest = player.eat(player.level(), stack, new FoodProperties(0, 0, food.canAlwaysEat(), food.eatSeconds(), food.usingConvertsTo(), food.effects()));
			if (rest != stack) {
				tg.inventory.setItem(i, ItemStack.EMPTY);
				player.getInventory().placeItemBackInInventory(rest);
			}
			player.getFoodData().eat(Math.min(needed, food.nutrition()), modifier);
			tg.foodleft = (short) Math.max(0, food.nutrition() - needed);
			tg.lastSaturation = tg.foodleft > 0 ? modifier : 0;
			return;
		}
	}

	/** Creative flight while the pack has charge; the ring and sound when taking off. */
	static void antiGrav(Player player, TGExtendedPlayer tg) {
		AttributeInstance flight = player.getAttribute(NeoForgeMod.CREATIVE_FLIGHT);
		boolean gives = ItemAntiGravPack.givesFlight(tg.getSlot(TGExtendedPlayer.SLOT_BACK));
		if (flight != null && gives != flight.hasModifier(ANTIGRAV_FLIGHT)) {
			if (gives) flight.addTransientModifier(new AttributeModifier(ANTIGRAV_FLIGHT, 1, AttributeModifier.Operation.ADD_VALUE));
			else flight.removeModifier(ANTIGRAV_FLIGHT);
		}
		boolean flying = gives && player.getAbilities().flying;
		if (flying && !tg.wasFlying) {
			playSound(player, TGSounds.ANTI_GRAV_START.get());
			TGEffects.sendFX((ServerLevel) player.level(), "AntiGravRing", player.getX(), player.getY(), player.getZ(), 1.0f, 50);
		}
		tg.wasFlying = flying;
	}

	private static void playSound(Player player, SoundEvent sound) {
		player.level().playSound(null, player.getX(), player.getEyeY(), player.getZ(), sound, SoundSource.PLAYERS, 1.0f, 1.0f);
	}

	/** Jump pack: extra jump height, both sides (the client's motion is what counts). */
	@SubscribeEvent
	static void onJump(LivingEvent.LivingJumpEvent event) {
		if (!(event.getEntity() instanceof Player player)) return;
		TGExtendedPlayer tg = TGExtendedPlayer.get(player);
		ItemStack back = tg.getSlot(TGExtendedPlayer.SLOT_BACK);
		if (!(back.getItem() instanceof ItemJumpPack) || !tg.enableJetpack || !ItemTGSpecialSlot.hasCharge(back)) return;
		ItemTGSpecialSlot.drain(back, 1);
		player.setDeltaMovement(player.getDeltaMovement().add(0, ItemJumpPack.JUMPBOOST, 0));
		if (player.level() instanceof ServerLevel level) {
			TGEffects.sendFX(level, "JumpPackBoost", player.getX(), player.getY(), player.getZ(), 1.0f, 50);
			playSound(player, TGSounds.STEAM_JUMP_2.get());
		}
	}

	/** Jump pack: the first blocks of a fall are free and the rest hurt less. */
	@SubscribeEvent
	static void onFall(LivingFallEvent event) {
		if (!(event.getEntity() instanceof Player player)) return;
		TGExtendedPlayer tg = TGExtendedPlayer.get(player);
		ItemStack back = tg.getSlot(TGExtendedPlayer.SLOT_BACK);
		if (!(back.getItem() instanceof ItemJumpPack) || !ItemTGSpecialSlot.hasCharge(back)) return;
		float reduction = 1;
		if (event.getDistance() > 3.0f && tg.enableJetpack) {
			ItemTGSpecialSlot.drain(back, 1);
			reduction -= ItemJumpPack.FALLDMG;
			if (player.level() instanceof ServerLevel level) TGEffects.sendFX(level, "JumpPackFall", player.getX(), player.getY(), player.getZ(), 1.0f, 50);
		}
		event.setDistance(Math.max(0, event.getDistance() - ItemJumpPack.FREEHEIGHT) * reduction);
	}

	@SubscribeEvent
	static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
		if (TGExtendedPlayer.get(event.getEntity()).getSlot(TGExtendedPlayer.SLOT_HAND).is(TGItems.WORKING_GLOVES)) {
			event.setNewSpeed((float) (event.getNewSpeed() * (1 + GLOVES_BREAKSPEED)));
		}
	}

	/** The slots drop with the inventory; the emptied attachment then carries over to the respawned player. */
	@SubscribeEvent
	static void onDrops(LivingDropsEvent event) {
		if (!(event.getEntity() instanceof Player player) || player.level().getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY)) return;
		TGExtendedPlayer tg = TGExtendedPlayer.get(player);
		for (int i = 0; i < TGExtendedPlayer.NUMSLOTS; i++) {
			ItemStack stack = tg.inventory.removeItemNoUpdate(i);
			if (stack.isEmpty()) continue;
			ItemEntity item = new ItemEntity(player.level(), player.getX(), player.getEyeY() - 0.3, player.getZ(), stack);
			item.setDefaultPickUpDelay();
			event.getDrops().add(item);
		}
	}

	@SubscribeEvent
	static void onDeath(LivingDeathEvent event) {
		if (event.getEntity() instanceof Player player && !player.level().isClientSide) {
			TGExtendedPlayer tg = TGExtendedPlayer.get(player);
			tg.foodleft = 0;
			tg.lastSaturation = 0;
		}
	}

	@SubscribeEvent
	static void register(RegisterPayloadHandlersEvent event) {
		event.registrar("1").playToServer(PlayerAction.TYPE, PlayerAction.CODEC, (p, context) -> handle(context.player(), p));
	}

	static void handle(Player player, PlayerAction p) {
		TGExtendedPlayer tg = TGExtendedPlayer.get(player);
		switch (p.action()) {
			case HUD -> tg.showTGHudElements = !tg.showTGHudElements;
			case NIGHTVISION -> {
				tg.enableNightVision = !tg.enableNightVision;
				if (!tg.enableNightVision) {
					if (player.removeEffect(MobEffects.NIGHT_VISION)) playSound(player, TGSounds.NIGHTVISION_OFF.get());
				} else if (ItemNightVisionGoggles.givesNightVision(tg.getSlot(TGExtendedPlayer.SLOT_FACE))
						|| GenericArmor.bonus(player, TGArmorBonus.NIGHTVISION, false) > 0) {
					playSound(player, TGSounds.NIGHTVISION_ON.get());
				}
				confirm(player, p, "techguns.armorTooltip.nightvision", tg.enableNightVision);
			}
			case SAFEMODE -> {
				tg.enableSafemode = !tg.enableSafemode || !TGPermissions.canUseUnsafeMode(player);
				confirm(player, p, "techguns.msg.safemode", tg.enableSafemode);
			}
			case STEP_ASSIST -> {
				tg.enableStepAssist = !tg.enableStepAssist;
				confirm(player, p, "techguns.armorTooltip.stepassist", tg.enableStepAssist);
			}
			case JETPACK -> {
				if (tg.isJumpkeyPressed) return;
				tg.enableJetpack = !tg.enableJetpack;
				confirm(player, p, "techguns.msg.enablejetpack", tg.enableJetpack);
			}
			case JUMP_BOOST -> {
				tg.enableJumpBoost = !tg.enableJumpBoost;
				confirm(player, p, "techguns.msg.jumpboost", tg.enableJumpBoost);
			}
			case BOOST_START -> tg.isJumpkeyPressed = true;
			case BOOST_STOP -> tg.isJumpkeyPressed = false;
			case OPEN_INVENTORY -> player.openMenu(new SimpleMenuProvider((id, inventory, p2) -> new TGPlayerInventoryMenu(id, p2),
					Component.translatable("techguns.extendedinventory")));
		}
	}

	/** 1.12 PacketShowKeybindConfirmMessage: "[Techguns]: Toggled [Night Vision] to [On]". */
	private static void confirm(Player player, PlayerAction p, String name, boolean state) {
		if (!p.showMsg()) return;
		player.displayClientMessage(Component.literal("[Techguns]: ").withStyle(ChatFormatting.YELLOW)
				.append(Component.translatable("techguns.msg.keybindtogglechange").withStyle(ChatFormatting.WHITE))
				.append(Component.literal(" [").append(Component.translatable(name)).append("] ").withStyle(ChatFormatting.YELLOW))
				.append(Component.translatable("techguns.msg.keybindtogglechange2").withStyle(ChatFormatting.WHITE))
				.append(Component.literal(" [").append(Component.translatable("techguns.container.info." + (state ? "on" : "off"))).append("]")
						.withStyle(state ? ChatFormatting.GREEN : ChatFormatting.DARK_RED)), true);
	}
}
