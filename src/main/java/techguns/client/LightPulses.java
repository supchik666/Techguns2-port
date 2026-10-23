package techguns.client;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import techguns.Techguns;

/**
 * Short flashes of block light from muzzle flashes and explosions (1.12 LightPulse, which needed the Albedo mod). The
 * light is mixed into {@code LevelRenderer.getLightColor} and entity light by mixins, and the chunk sections around a
 * pulse rebuild when its block or whole-block radius changes, so a minigun fired standing still rebuilds nothing.
 * Vanilla light has no color, so that light is white and {@link techguns.client.render.LightTint} tints the screen
 * around each pulse with its color.
 */
@EventBusSubscriber(modid = Techguns.MODID, value = Dist.CLIENT)
public final class LightPulses {
	/** Light only reaches this far, so a section rebuild stays bounded. */
	private static final float MAX_RADIUS = 15;

	/** {@code owner} is set for a steady light, which stays full until its last third. */
	private record Pulse(double x, double y, double z, int lifetime, float radiusStart, float radiusEnd, float r, float g, float b, Entity owner,
			int[] age) {
		float fade(float partialTick) {
			float left = 1 - progress(partialTick);
			return owner != null ? Math.min(1, 3 * left) : left;
		}

		float radius() {
			return radius(0);
		}

		float radius(float partialTick) {
			return Math.min(MAX_RADIUS, Mth.lerp(progress(partialTick), radiusStart, radiusEnd));
		}

		float progress(float partialTick) {
			return Math.min(1, (age[0] + partialTick) / lifetime);
		}
	}

	/** A pulse as the chunk builders see it: its block's center and a whole-block radius, so it changes in steps. */
	private record Light(double x, double y, double z, float radius) {}

	private static final List<Pulse> PULSES = new ArrayList<>();
	/** Read by chunk builder threads; replaced, never changed. */
	private static volatile Light[] lights = {};

	private LightPulses() {}

	/** A light at {@code pos} whose radius goes from {@code radiusStart} to {@code radiusEnd} blocks over {@code lifetime} ticks. */
	public static void add(Vec3 pos, int lifetime, float radiusStart, float radiusEnd, float r, float g, float b) {
		PULSES.add(new Pulse(pos.x, pos.y, pos.z, lifetime, radiusStart, radiusEnd, r, g, b, null, new int[1]));
		update();
	}

	/** A muzzle flash in the gun's {@code color}: a brief light a block in front of the shooter's eyes, smaller for dim colors. */
	public static void muzzleFlash(Entity shooter, float[] color) {
		float k = Math.max(color[0], Math.max(color[1], color[2]));
		add(muzzle(shooter), 3, 6 * k, 4 * k, color[0], color[1], color[2]);
	}

	/** A light held while {@code shooter} keeps firing faster than every 4 ticks: each shot moves it and starts its life over. */
	public static void steadyMuzzleFlash(Entity shooter, float[] color) {
		PULSES.removeIf(p -> p.owner == shooter);
		Vec3 pos = muzzle(shooter);
		PULSES.add(new Pulse(pos.x, pos.y, pos.z, 4, 6, 6, color[0], color[1], color[2], shooter, new int[1]));
		update();
	}

	private static Vec3 muzzle(Entity shooter) {
		return shooter.getEyePosition().add(shooter.getLookAngle());
	}

	/** The light of a TGFX effect, by name: explosions flash big, incendiary bullet impacts small; energy impacts, unlike 1.12, not at all. */
	public static void fx(String name, Vec3 pos, float scale) {
		if (name.startsWith("Nuke")) add(pos, 85, MAX_RADIUS, 1, 1, 0.9f, 0.5f);
		else if (name.equals("AlienExplosion") || name.equals("BlueBlasterExplosion")) return; // energy gun impacts
		else if (name.equals("MiningChargeBlockExplosion")) add(pos, 15, 5 * scale, 1, 1, 0.9f, 0.5f); // explosive bullets
		else if (name.contains("Explosion") && !name.startsWith("TFG")) add(pos, 20, 10 * scale, 1, 1, 0.9f, 0.5f);
		else if (name.equals("Impact_IncendiaryBullet")) add(pos, 15, 3 * scale, 1, 1, 0.5f, 0.1f);
	}

	@SubscribeEvent
	static void tick(ClientTickEvent.Post event) {
		if (PULSES.isEmpty()) return;
		PULSES.removeIf(p -> ++p.age[0] >= p.lifetime);
		update();
	}

	/** Publishes the lights and rebuilds around the ones that appeared or went away (a moved or resized light is both). */
	private static void update() {
		Light[] next = new Light[PULSES.size()];
		for (int i = 0; i < next.length; i++) {
			Pulse p = PULSES.get(i);
			next[i] = new Light(Mth.floor(p.x) + 0.5, Mth.floor(p.y) + 0.5, Mth.floor(p.z) + 0.5, Math.round(p.radius()));
		}
		Light[] previous = lights;
		lights = next;
		List<Light> before = List.of(previous), after = List.of(next);
		for (Light l : next) if (!before.contains(l)) setDirty(l);
		for (Light l : previous) if (!after.contains(l)) setDirty(l);
	}

	private static void setDirty(Light l) {
		int r = (int) Math.ceil(l.radius), x = Mth.floor(l.x), y = Mth.floor(l.y), z = Mth.floor(l.z);
		if (r > 0) Minecraft.getInstance().levelRenderer.setBlocksDirty(x - r, y - r, z - r, x + r, y + r, z + r);
	}

	/**
	 * Writes up to {@code out.length / 8} pulses for {@link techguns.client.render.LightTint}: position relative to
	 * {@code camera}, radius, then color faded out over the pulse's life. Returns how many.
	 */
	public static int tints(float[] out, Vec3 camera, float partialTick) {
		int n = Math.min(PULSES.size(), out.length / 8);
		for (int i = 0; i < n; i++) {
			Pulse p = PULSES.get(i);
			float fade = p.fade(partialTick);
			int o = i * 8;
			out[o] = (float) (p.x - camera.x);
			out[o + 1] = (float) (p.y - camera.y);
			out[o + 2] = (float) (p.z - camera.z);
			out[o + 3] = p.radius(partialTick);
			out[o + 4] = p.r * fade;
			out[o + 5] = p.g * fade;
			out[o + 6] = p.b * fade;
		}
		return n;
	}

	/** Block light 0-15 from pulses at {@code pos}: full at the center, fading out to the radius. */
	public static int blockLight(BlockPos pos) {
		Light[] current = lights;
		int best = 0;
		for (Light l : current) {
			double dx = pos.getX() + 0.5 - l.x, dy = pos.getY() + 0.5 - l.y, dz = pos.getZ() + 0.5 - l.z;
			double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
			if (distance < l.radius) best = Math.max(best, (int) (15 * (1 - distance / l.radius) + 0.5));
		}
		return best;
	}

	/** {@code packed} (sky << 20 | block << 4) with its block light raised by the pulses at {@code pos}. */
	public static int apply(int packed, BlockPos pos) {
		if (lights.length == 0) return packed;
		int light = blockLight(pos);
		int block = LightTexture.block(packed);
		return light > block ? LightTexture.pack(light, LightTexture.sky(packed)) : packed;
	}
}
