package techguns.damagesystem;

import org.joml.Vector3f;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import techguns.TGConfig;
import techguns.TGItems;
import techguns.TGRadiation;
import techguns.Techguns;
import techguns.damagesystem.TGDamageSource.DeathType;
import techguns.items.guns.GenericGunMeleeCharge;

/**
 * 1.12 EntityDeathUtils and the death packet: a kill can burst the victim into gibs (GORE, only entities in
 * {@code #techguns:gore}), melt it (BIO) or burn it to ash (LASER). The server rolls it and tells the clients, which
 * draw it ({@code client.particle.DeathEffect}). DISMEMBER was unused in 1.12.
 */
@EventBusSubscriber(modid = Techguns.MODID)
public final class DeathEffects {
	public static final TagKey<EntityType<?>> GORE = TagKey.create(Registries.ENTITY_TYPE, Techguns.id("gore"));

	private DeathEffects() {}

	public record Death(int entityId, DeathType deathType, Vector3f motion) implements CustomPacketPayload {
		public static final Type<Death> TYPE = new Type<>(Techguns.id("death_effect"));
		static final StreamCodec<ByteBuf, Death> CODEC = StreamCodec.composite(ByteBufCodecs.VAR_INT, Death::entityId,
				ByteBufCodecs.idMapper(i -> DeathType.values()[i], DeathType::ordinal), Death::deathType, ByteBufCodecs.VECTOR3F, Death::motion, Death::new);

		@Override
		public Type<Death> type() {
			return TYPE;
		}
	}

	/** The rolled death type of {@code entity} killed by {@code source}. */
	public static DeathType deathType(DamageSource source, LivingEntity entity) {
		DeathType type = DeathType.DEFAULT;
		float chance = 0;
		if (source instanceof TGDamageSource tg) {
			type = tg.deathType;
			chance = tg.goreChance;
		} else if (source.is(DamageTypes.PLAYER_ATTACK)) {
			// 1.12 getMeleeDamageSource; the knife and crowbar were meant to gore at 0.25 but 1.12 never used their source
			ItemStack weapon = source.getWeaponItem();
			if (weapon != null && weapon.getItem() instanceof GenericGunMeleeCharge gun) {
				type = DeathType.GORE;
				chance = gun.goreChance(weapon);
			} else if (weapon != null && (weapon.is(TGItems.COMBAT_KNIFE) || weapon.is(TGItems.CROWBAR))) {
				type = DeathType.GORE;
				chance = 0.25f;
			}
		} else if (source.is(TGRadiation.RADIATION_DAMAGE)) {
			type = DeathType.BIO;
			chance = 0.5f;
		} else if (source.is(TGRadiation.POISONING_DAMAGE)) {
			type = DeathType.LASER;
			chance = 1;
		}
		if (type == DeathType.GORE && !entity.getType().is(GORE)) return DeathType.DEFAULT;
		// gore rolls the config chance instead of 1.12's per-weapon one; a weapon chance of 0 (an unpowered melee gun) still turns it off
		if (type == DeathType.GORE && chance > 0) chance = TGConfig.GORE_CHANCE.get().floatValue();
		return entity.getRandom().nextFloat() < chance ? type : DeathType.DEFAULT;
	}

	@SubscribeEvent(priority = EventPriority.LOWEST)
	static void onDeath(LivingDeathEvent event) {
		LivingEntity entity = event.getEntity();
		if (entity.level().isClientSide) return;
		DeathType type = deathType(event.getSource(), entity);
		if (type != DeathType.DEFAULT) {
			PacketDistributor.sendToPlayersTrackingEntityAndSelf(entity, new Death(entity.getId(), type, entity.getDeltaMovement().toVector3f()));
		}
	}

	@SubscribeEvent
	static void register(RegisterPayloadHandlersEvent event) {
		event.registrar("1").playToClient(Death.TYPE, Death.CODEC, (payload, context) -> techguns.client.particle.DeathEffect.play(payload));
	}
}
