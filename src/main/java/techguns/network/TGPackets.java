package techguns.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import techguns.Techguns;
import techguns.client.ShooterValues;
import techguns.items.guns.GenericGun;

@EventBusSubscriber(modid = Techguns.MODID)
public class TGPackets {
	private static final StreamCodec<ByteBuf, InteractionHand> HAND = ByteBufCodecs.BOOL.map(
			offhand -> offhand ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND, hand -> hand == InteractionHand.OFF_HAND);

	/**
	 * Client pressed fire (or reload) with a gun in {@code hand}; the server runs the same gun logic the client predicted.
	 * {@code zooming} is client state the server trusts, as in 1.12; it only tightens spread.
	 */
	public record GunAction(InteractionHand hand, boolean reload, boolean zooming) implements CustomPacketPayload {
		public static final Type<GunAction> TYPE = new Type<>(Techguns.id("gun_action"));
		static final StreamCodec<ByteBuf, GunAction> CODEC = StreamCodec.composite(HAND, GunAction::hand, ByteBufCodecs.BOOL, GunAction::reload,
				ByteBufCodecs.BOOL, GunAction::zooming, GunAction::new);

		@Override
		public Type<GunAction> type() {
			return TYPE;
		}
	}

	/** Tells other players to play a shooter's recoil (or reload) animation. */
	public record GunAnimation(int entityId, InteractionHand hand, boolean reload) implements CustomPacketPayload {
		public static final Type<GunAnimation> TYPE = new Type<>(Techguns.id("gun_animation"));
		static final StreamCodec<ByteBuf, GunAnimation> CODEC = StreamCodec.composite(ByteBufCodecs.VAR_INT, GunAnimation::entityId,
				HAND, GunAnimation::hand, ByteBufCodecs.BOOL, GunAnimation::reload, GunAnimation::new);

		@Override
		public Type<GunAnimation> type() {
			return TYPE;
		}
	}

	@SubscribeEvent
	static void register(RegisterPayloadHandlersEvent event) {
		var registrar = event.registrar("1");
		registrar.playToServer(GunAction.TYPE, GunAction.CODEC, (payload, context) -> {
			Player player = context.player();
			ItemStack stack = player.getItemInHand(payload.hand());
			if (!(stack.getItem() instanceof GenericGun gun)) return;
			if (payload.reload()) {
				gun.tryForcedReload(stack, player.level(), player, payload.hand());
			} else {
				gun.shootGunPrimary(stack, player.level(), player, payload.hand(), payload.zooming());
			}
		});
		registrar.playToClient(GunAnimation.TYPE, GunAnimation.CODEC, (payload, context) -> {
			if (context.player().level().getEntity(payload.entityId()) instanceof LivingEntity shooter
					&& shooter.getItemInHand(payload.hand()).getItem() instanceof GenericGun gun) {
				if (payload.reload()) {
					ShooterValues.reloaded(shooter, payload.hand(), gun.getReloadtime());
				} else {
					ShooterValues.fired(shooter, payload.hand(), gun);
				}
			}
		});
	}
}
