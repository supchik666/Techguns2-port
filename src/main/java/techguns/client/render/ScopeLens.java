package techguns.client.render;

import java.io.IOException;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.EffectInstance;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import techguns.TGConfig;
import techguns.Techguns;
import techguns.client.TGClient;
import techguns.items.guns.GenericGun;

/**
 * Looking through a scope, the world (not the overlay, drawn later with the hand) is seen through glass: bent and split
 * into colors toward the rim of the scope's window, which is shaded, and blurred while moving. After {@link Bloom} so glows bend too. Off with
 * shader mods, like Bloom.
 */
@EventBusSubscriber(modid = Techguns.MODID, value = Dist.CLIENT)
public final class ScopeLens {
	/** Half the view height at the overlay's depth: the hand is drawn at 70° fov and the overlay 0.72 in front. */
	private static final float VIEW_HALF_HEIGHT = 0.72f * (float) Math.tan(Math.toRadians(35));

	private static TextureTarget target;
	private static EffectInstance lens, copy;
	private static boolean broken;
	/** Eased toward how blurred the view should be. */
	private static float blur;
	private static long lastFrame;

	private ScopeLens() {}

	@SubscribeEvent
	static void registerReloadListener(RegisterClientReloadListenersEvent event) {
		event.registerReloadListener((ResourceManagerReloadListener) manager -> {
			if (lens != null) lens.close();
			if (copy != null) copy.close();
			lens = copy = null;
			if (ModList.get().isLoaded("iris") || ModList.get().isLoaded("oculus")) return;
			try {
				lens = new EffectInstance(manager, Techguns.id("scope_lens").toString());
				copy = new EffectInstance(manager, Techguns.id("bloom_add").toString());
				broken = false;
			} catch (IOException | RuntimeException e) {
				fail("loading its shaders", e);
			}
		});
	}

	@SubscribeEvent(priority = EventPriority.LOW)
	static void afterLevel(RenderLevelStageEvent event) {
		if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL || broken || lens == null || !TGConfig.SCOPE_LENS.getAsBoolean()) return;
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null || !mc.options.getCameraType().isFirstPerson()) return;
		ItemStack stack = mc.player.getMainHandItem();
		if (!(stack.getItem() instanceof GenericGun gun) || !TGClient.isZooming(gun)
				|| !(IClientItemExtensions.of(stack).getCustomRenderer() instanceof GunRenderer renderer) || renderer.getScope() == null) {
			return;
		}
		lens.safeGetUniform("Blur").set(blur(mc));
		// the window's half size as a fraction of the overlay texture, measured off testscope.png / techscope.png
		boolean tech = renderer.getScope() == ScreenEffect.techScope;
		float k = renderer.getScopeScale() / VIEW_HALF_HEIGHT;
		RenderTarget main = mc.getMainRenderTarget();
		RenderSystem.disableDepthTest();
		RenderSystem.depthMask(false);
		try {
			if (target == null || target.width != main.width || target.height != main.height) {
				if (target != null) target.destroyBuffers();
				target = Bloom.target(main.width, main.height);
			}
			lens.safeGetUniform("Window").set(k * (tech ? 0.308f : 0.1585f), k * (tech ? 0.2226f : 0.1585f), tech ? 8f : 2f);
			Bloom.pass(lens, main, target, null);
			copy.safeGetUniform("ColorModulate").set(1f, 1f, 1f, 1f);
			Bloom.pass(copy, target, main, null);
		} catch (RuntimeException e) {
			fail("rendering", e);
		}
		main.bindWrite(true);
		RenderSystem.depthMask(true);
		RenderSystem.enableDepthTest();
	}

	/**
	 * As in CS2: blurred while moving (full at walking speed) or off the ground; eased in and out over about a tenth of
	 * a second.
	 */
	private static float blur(Minecraft mc) {
		LocalPlayer player = mc.player;
		float speed = (float) Math.hypot(player.getX() - player.xo, player.getZ() - player.zo);
		float target = player.onGround() ? Mth.clamp((speed - 0.03f) / 0.15f, 0, 1) : 1;
		long now = System.currentTimeMillis();
		float dt = lastFrame == 0 ? 0 : Math.min(now - lastFrame, 100) / 1000f;
		lastFrame = now;
		blur += (target - blur) * Math.min(1, dt * 10);
		return blur;
	}

	private static void fail(String what, Exception e) {
		broken = true;
		Techguns.LOGGER.error("Scope lens failed {}, turning it off", what, e);
	}
}
