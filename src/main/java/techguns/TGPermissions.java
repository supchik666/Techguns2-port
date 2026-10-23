package techguns;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.server.permission.PermissionAPI;
import net.neoforged.neoforge.server.permission.events.PermissionGatherEvent;
import net.neoforged.neoforge.server.permission.nodes.PermissionNode;
import net.neoforged.neoforge.server.permission.nodes.PermissionTypes;
import techguns.capabilities.TGExtendedPlayer;

/** 1.12 TGPermissions: who may turn off safe mode (guns breaking blocks). Everyone by default. */
@EventBusSubscriber(modid = Techguns.MODID)
public final class TGPermissions {
	public static final PermissionNode<Boolean> ALLOW_UNSAFE_MODE = new PermissionNode<>(Techguns.MODID, "allowunsafemode", PermissionTypes.BOOLEAN,
			(player, uuid, context) -> true);

	private TGPermissions() {}

	@SubscribeEvent
	static void nodes(PermissionGatherEvent.Nodes event) {
		event.addNodes(ALLOW_UNSAFE_MODE);
	}

	/** The restrictUnsafeModeToOP config, else the permission node. */
	public static boolean canUseUnsafeMode(Player player) {
		if (!(player instanceof ServerPlayer sp)) return true;
		return TGConfig.RESTRICT_UNSAFE_MODE_TO_OP.getAsBoolean() ? sp.server.getPlayerList().isOp(sp.getGameProfile())
				: PermissionAPI.getPermission(sp, ALLOW_UNSAFE_MODE);
	}

	/** 1.12 forced safe mode back on at load for players who may not use unsafe mode. */
	@SubscribeEvent
	static void login(PlayerEvent.PlayerLoggedInEvent event) {
		if (!canUseUnsafeMode(event.getEntity())) TGExtendedPlayer.get(event.getEntity()).enableSafemode = true;
	}
}
