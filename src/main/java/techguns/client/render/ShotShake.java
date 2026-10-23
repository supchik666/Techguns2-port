package techguns.client.render;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import techguns.TGConfig;
import techguns.Techguns;
import techguns.client.ShooterValues;
import techguns.client.TGClient;
import techguns.items.guns.GenericGun;

/**
 * The camera moves with the local player's shots; only the camera, never the aim. Through a scope, as in CS2, the scope
 * stays still and the view kicks up or sideways; otherwise a quicker shake, rolling too. Rapid shots add up smoothly.
 */
@EventBusSubscriber(modid = Techguns.MODID, value = Dist.CLIENT)
public final class ShotShake {
	/** Scales every gun's {@link GunRenderer#getScopeKick()}. */
	private static final float KICK_SCALE = 1.15f;
	/** Degrees of shake per unscoped shot, by gun id; anything else (pistols, SMGs, energy guns) is {@link #LIGHT}. */
	private static final float LIGHT = 0.75f;
	private static final Map<String, Float> SHAKE = new HashMap<>();
	static {
		for (String id : List.of("thompson", "ak47", "m4", "m4_infiltrator", "scar", "aug", "lmg", "handcannon")) SHAKE.put(id, 1.5f);
		for (String id : List.of("combatshotgun", "sawedoff", "boltaction", "as50", "gaussrifle")) SHAKE.put(id, 2.5f);
		for (String id : List.of("rocketlauncher", "grenadelauncher", "guidedmissilelauncher", "grimreaper", "tfg")) SHAKE.put(id, 4.5f);
		SHAKE.put("minigun", 1.75f);
	}
	/** Standing shakes more, sneaking steadies the gun. */
	private static final float STAND = 1.5f, SNEAK = 0.5f;

	/** Critically damped: peaks 1/rate after a kick and settles in about 4/rate. */
	private static final class Spring {
		final float rate;
		float x, v;

		Spring(float rate) {
			this.rate = rate;
		}

		/** An impulse that peaks at exactly {@code peak}. */
		void kick(float peak) {
			v += peak * rate * (float) Math.E;
		}

		void step(float dt) {
			for (; dt > 0; dt -= 0.004f) {
				float h = Math.min(dt, 0.004f);
				v -= (rate * rate * x + 2 * rate * v) * h;
				x += v * h;
			}
		}
	}

	/** Scope kick: peaks 70 ms after the shot, settled in about 300. */
	private static final Spring kickPitch = new Spring(14), kickYaw = new Spring(14);
	/** Shake: peaks 55 ms after the shot, settled in about 220. */
	private static final Spring shakePitch = new Spring(18), shakeYaw = new Spring(18), shakeRoll = new Spring(18);
	private static final int[] lastShots = new int[2];
	private static long lastFrame;

	private ShotShake() {}

	@SubscribeEvent
	static void computeCameraAngles(ViewportEvent.ComputeCameraAngles event) {
		Minecraft mc = Minecraft.getInstance();
		long now = System.nanoTime();
		float dt = lastFrame == 0 ? 0 : Math.min(now - lastFrame, 100_000_000L) / 1e9f;
		lastFrame = now;
		if (mc.player != null) {
			for (InteractionHand hand : InteractionHand.values()) {
				int shots = ShooterValues.get(mc.player, hand).shots;
				int fired = shots - lastShots[hand.ordinal()];
				lastShots[hand.ordinal()] = shots;
				if (fired > 0 && dt > 0 && TGConfig.SCREEN_SHAKE.getAsBoolean()) {
					shot(mc.player, hand, fired * (mc.player.isShiftKeyDown() ? SNEAK : STAND));
				}
			}
		}
		for (Spring s : new Spring[] {kickPitch, kickYaw, shakePitch, shakeYaw, shakeRoll}) s.step(dt);
		event.setPitch(event.getPitch() - kickPitch.x - shakePitch.x);
		event.setYaw(event.getYaw() + kickYaw.x + shakeYaw.x);
		event.setRoll(event.getRoll() + shakeRoll.x);
	}

	private static void shot(LocalPlayer player, InteractionHand hand, float strength) {
		ItemStack stack = player.getItemInHand(hand);
		if (!(stack.getItem() instanceof GenericGun gun) || !(IClientItemExtensions.of(stack).getCustomRenderer() instanceof GunRenderer renderer)) {
			return;
		}
		// any direction from straight left through up to straight right
		double angle = (Math.random() - 0.5) * Math.PI;
		float up = (float) Math.cos(angle), side = (float) Math.sin(angle);
		if (hand == InteractionHand.MAIN_HAND && renderer.getScope() != null && TGClient.isZooming(gun)) {
			float k = strength * renderer.getScopeKick() * KICK_SCALE;
			kickPitch.kick(k * up);
			kickYaw.kick(k * side);
		} else {
			// mostly a punch upward, so the gun's power reads as recoil
			float k = strength * SHAKE.getOrDefault(BuiltInRegistries.ITEM.getKey(gun).getPath(), LIGHT);
			shakePitch.kick(k * (0.6f + 0.4f * up));
			shakeYaw.kick(k * side * 0.5f);
			shakeRoll.kick(k * (float) (Math.random() * 2 - 1));
		}
	}
}
