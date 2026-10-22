package techguns;

import java.util.function.Predicate;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/**
 * Common entry point for TGFX particle effects (1.12 proxy.createFX / PacketSpawnParticle). Effects are named systems
 * from {@code assets/techguns/particles/*.txt}. The create calls do nothing on the server; {@link #sendFX} sends one to
 * nearby players. The client sets {@link #client} at startup.
 */
@EventBusSubscriber(modid = Techguns.MODID)
public final class TGEffects {
	public interface Handler {
		/** {@code pitch}/{@code yaw} rotate the system; {@code scale} 1 is normal size. */
		void createFX(Level level, String name, double x, double y, double z, double motionX, double motionY, double motionZ, float pitch,
				float yaw, float scale);

		/**
		 * Follows {@code entity} while it's alive (trails). {@code offset} is in the entity's rotated frame; with
		 * {@code attachToHead} it's from a living entity's eyes and turns with its head (muzzle and charge effects). Ends when
		 * {@code condition} turns false (1.12 EntityCondition).
		 */
		void createFXOnEntity(Entity entity, String name, float scale, Vec3 offset, boolean attachToHead, Predicate<Entity> condition);
	}

	public static Handler client = new Handler() {
		@Override
		public void createFX(Level level, String name, double x, double y, double z, double mx, double my, double mz, float pitch, float yaw, float scale) {}

		@Override
		public void createFXOnEntity(Entity entity, String name, float scale, Vec3 offset, boolean attachToHead, Predicate<Entity> condition) {}
	};

	private TGEffects() {}

	public static void createFX(Level level, String name, double x, double y, double z, double motionX, double motionY, double motionZ) {
		createFX(level, name, x, y, z, motionX, motionY, motionZ, 0, 0, 1.0f);
	}

	public static void createFX(Level level, String name, double x, double y, double z, double motionX, double motionY, double motionZ, float pitch,
			float yaw, float scale) {
		if (level.isClientSide) client.createFX(level, name, x, y, z, motionX, motionY, motionZ, pitch, yaw, scale);
	}

	public static void createFXOnEntity(Entity entity, String name) {
		createFXOnEntity(entity, name, 1.0f);
	}

	public static void createFXOnEntity(Entity entity, String name, float scale) {
		if (entity.level().isClientSide) client.createFXOnEntity(entity, name, scale, Vec3.ZERO, false, Entity::isAlive);
	}

	/** 1.12 createFXOnEntityWithOffset, see {@link Handler#createFXOnEntity}. */
	public static void createFXOnEntity(Entity entity, String name, Vec3 offset, boolean attachToHead) {
		createFXOnEntity(entity, name, offset, attachToHead, Entity::isAlive);
	}

	/** As above, ending when {@code condition} turns false (e.g. the entity stops charging). */
	public static void createFXOnEntity(Entity entity, String name, Vec3 offset, boolean attachToHead, Predicate<Entity> condition) {
		if (entity.level().isClientSide) client.createFXOnEntity(entity, name, 1.0f, offset, attachToHead, condition);
	}

	/** Server side: plays effect {@code name} at x/y/z for players within {@code range} blocks. */
	public static void sendFX(ServerLevel level, String name, double x, double y, double z, float scale, double range) {
		sendFX(level, name, x, y, z, 0, 0, scale, range);
	}

	/** As above, rotated by {@code pitch}/{@code yaw} (impacts facing out of the hit side). */
	public static void sendFX(ServerLevel level, String name, double x, double y, double z, float pitch, float yaw, float scale, double range) {
		PacketDistributor.sendToPlayersNear(level, null, x, y, z, range, new SpawnFX(name, x, y, z, pitch, yaw, scale));
	}

	public record SpawnFX(String name, double x, double y, double z, float pitch, float yaw, float scale) implements CustomPacketPayload {
		public static final Type<SpawnFX> TYPE = new Type<>(Techguns.id("spawn_fx"));
		static final StreamCodec<ByteBuf, SpawnFX> CODEC = StreamCodec.of((buf, p) -> {
			ByteBufCodecs.STRING_UTF8.encode(buf, p.name);
			buf.writeDouble(p.x).writeDouble(p.y).writeDouble(p.z).writeFloat(p.pitch).writeFloat(p.yaw).writeFloat(p.scale);
		}, buf -> new SpawnFX(ByteBufCodecs.STRING_UTF8.decode(buf), buf.readDouble(), buf.readDouble(), buf.readDouble(), buf.readFloat(),
				buf.readFloat(), buf.readFloat()));

		@Override
		public Type<SpawnFX> type() {
			return TYPE;
		}
	}

	@SubscribeEvent
	static void register(RegisterPayloadHandlersEvent event) {
		event.registrar("1").playToClient(SpawnFX.TYPE, SpawnFX.CODEC,
				(p, context) -> createFX(context.player().level(), p.name(), p.x(), p.y(), p.z(), 0, 0, 0, p.pitch(), p.yaw(), p.scale()));
	}
}
