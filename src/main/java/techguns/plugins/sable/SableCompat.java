package techguns.plugins.sable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;

import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.companion.SableCompanion;
import dev.ryanhcode.sable.companion.SubLevelAccess;
import dev.ryanhcode.sable.companion.math.BoundingBox3d;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.ModList;

/**
 * Sable (Create Aeronautics) physics ships. Their blocks live in a plot grid far from where the ship flies, and Sable's
 * patched {@code Level.clip} returns hits there; these put hits back in the world and let our explosions reach ships.
 * Sable is compileOnly; nothing here touches its classes unless it's loaded, and its types stay in this class.
 */
public final class SableCompat {
	private static final boolean LOADED = ModList.get().isLoaded("sable");
	/** Impulse per ship block a blast ray reaches; Sable gives vanilla explosions the same. */
	private static final double BLAST_IMPULSE = 5;

	/** {@code pos} in world coordinates if it lies on a ship. */
	public static Vec3 toWorld(Level level, Vec3 pos) {
		return LOADED ? SableCompanion.INSTANCE.projectOutOfSubLevel(level, pos) : pos;
	}

	/** A block hit with its location in world coordinates; the block pos stays on the ship's plot, where the block is. */
	public static HitResult toWorld(Level level, HitResult hit) {
		if (!LOADED || !(hit instanceof BlockHitResult b) || hit.getType() == HitResult.Type.MISS) return hit;
		Vec3 pos = toWorld(level, b.getLocation());
		return pos == b.getLocation() ? hit : new BlockHitResult(pos, b.getDirection(), b.getBlockPos(), b.isInside()); // ponytail: side stays ship-local, so impact FX face the wrong way on a tilted ship
	}

	/** The ships within {@code radius} of a blast at {@code center}, or null if none (or no Sable). */
	@Nullable
	public static Ships ships(ServerLevel level, Vec3 center, double radius) {
		return LOADED ? Ships.find(level, center, radius) : null;
	}

	/** Ships near a blast, by index, so callers don't see Sable types. Only loaded with Sable: the verifier would load its types. */
	public static final class Ships {
		private final List<SubLevelAccess> ships;
		private final Vec3 center;
		private final Set<BlockPos> pushed = new HashSet<>();

		private Ships(List<SubLevelAccess> ships, Vec3 center) {
			this.ships = ships;
			this.center = center;
		}

		@Nullable
		private static Ships find(ServerLevel level, Vec3 center, double radius) {
			List<SubLevelAccess> found = new ArrayList<>();
			SableCompanion.INSTANCE.getAllIntersecting(level, new BoundingBox3d(new AABB(center, center).inflate(radius))).forEach(found::add);
			return found.isEmpty() ? null : new Ships(found, center);
		}

		public int size() {
			return ships.size();
		}

		/** The plot pos of ship {@code i}'s block at world point {@code p}. */
		public BlockPos blockAt(int i, Vec3 p) {
			return BlockPos.containing(ships.get(i).logicalPose().transformPositionInverse(p));
		}

		/** Pushes ship {@code i} at its block {@code pos}, away from the blast; once per block. */
		public void push(int i, BlockPos pos) {
			if (!pushed.add(pos) || !(ships.get(i) instanceof ServerSubLevel ship)) return;
			RigidBodyHandle body = RigidBodyHandle.of(ship);
			if (body == null || !body.isValid()) return;
			Vec3 at = pos.getCenter();
			body.applyImpulseAtPoint(at, at.subtract(ship.logicalPose().transformPositionInverse(center)).normalize().scale(BLAST_IMPULSE));
		}
	}
}
