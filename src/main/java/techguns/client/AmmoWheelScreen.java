package techguns.client;

import java.util.List;

import org.joml.Matrix4f;

import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import techguns.TGComponents;
import techguns.capabilities.TGExtendedPlayer;
import techguns.items.guns.GenericGun;
import techguns.items.guns.ammo.AmmoType;
import techguns.network.TGPackets;

/**
 * The ammo wheel (not in 1.12, as in Rust): opened by holding R on a gun with more than one ammo type. Point at a type
 * and release R, or click, to load it; release in the middle to cancel. Types the player has none of can't be picked.
 */
public class AmmoWheelScreen extends Screen {
	private static final float INNER = 28, OUTER = 78, GAP = 0.03f;
	private static final int STEPS = 16;
	/** The open animation: the wheel grows from this scale and fades in over OPEN_MILLIS, the background darkens. */
	private static final float START_SCALE = 0.6f, OPEN_MILLIS = 150;
	private static final int BACKGROUND_ALPHA = 0x70;

	private final long opened = Util.getMillis();

	private final GenericGun gun;
	private final List<String> keys;
	private int hovered = -1;

	AmmoWheelScreen(GenericGun gun) {
		super(Component.empty());
		this.gun = gun;
		this.keys = List.copyOf(gun.getAmmoType().keys());
	}

	private Item ammo(int i) {
		return gun.getAmmoType().get(keys.get(i)).ammo().get();
	}

	private int count(int i) {
		return TGExtendedPlayer.countAmmo(minecraft.player, ammo(i));
	}

	/** Middle of segment {@code i}, clockwise from the top. */
	private double angle(int i) {
		return 2 * Math.PI * i / keys.size() - Math.PI / 2;
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		ItemStack stack = minecraft.player.getMainHandItem();
		if (!stack.is(gun)) {
			onClose();
			return;
		}
		float open = Math.min(1, (Util.getMillis() - opened) / OPEN_MILLIS);
		open = 1 - (1 - open) * (1 - open) * (1 - open); // ease out
		g.fill(0, 0, width, height, Math.round(BACKGROUND_ALPHA * open) << 24);
		int n = keys.size();
		float cx = width / 2f, cy = height / 2f;
		double dx = mouseX - cx, dy = mouseY - cy;
		hovered = dx * dx + dy * dy < INNER * INNER ? -1 : (int) Math.floorMod(Math.round((Math.atan2(dy, dx) + Math.PI / 2) * n / (2 * Math.PI)), n);
		int loaded = keys.indexOf(stack.getOrDefault(TGComponents.AMMO_VARIANT, AmmoType.DEFAULT));

		float scale = START_SCALE + (1 - START_SCALE) * open;
		g.pose().pushPose();
		g.pose().translate(cx, cy, 0);
		g.pose().scale(scale, scale, 1);
		g.pose().translate(-cx, -cy, 0);
		VertexConsumer consumer = g.bufferSource().getBuffer(RenderType.gui());
		Matrix4f pose = g.pose().last().pose();
		double half = Math.PI / n - GAP;
		for (int i = 0; i < n; i++) {
			int color = fade(count(i) == 0 ? 0x60303030 : i == hovered ? 0xC0C87A1E : 0xA0181818, open);
			for (int k = 0; k < STEPS; k++) {
				double a0 = angle(i) - half + 2 * half * k / STEPS, a1 = angle(i) - half + 2 * half * (k + 1) / STEPS;
				vertex(consumer, pose, cx, cy, OUTER, a0, color);
				vertex(consumer, pose, cx, cy, INNER, a0, color);
				vertex(consumer, pose, cx, cy, INNER, a1, color);
				vertex(consumer, pose, cx, cy, OUTER, a1, color);
			}
		}
		g.flush();

		float mid = (INNER + OUTER) / 2;
		for (int i = 0; i < n; i++) {
			int x = Math.round(cx + (float) Math.cos(angle(i)) * mid), y = Math.round(cy + (float) Math.sin(angle(i)) * mid);
			g.renderItem(new ItemStack(ammo(i)), x - 8, y - 12);
			int count = count(i);
			g.drawCenteredString(font, String.valueOf(count), x, y + 6, fade(count == 0 ? 0xFF808080 : i == loaded ? 0xFFFFD040 : 0xFFFFFFFF, open));
		}
		int shown = hovered >= 0 ? hovered : loaded;
		if (shown >= 0) g.drawCenteredString(font, ammo(shown).getDescription(), (int) cx, (int) cy + (int) OUTER + 6, fade(0xFFFFFFFF, open));
		if (hovered < 0 && loaded >= 0) {
			g.drawCenteredString(font, Component.translatable("techguns.ammowheel.loaded").withStyle(ChatFormatting.YELLOW), (int) cx, (int) cy - 4,
					fade(0xFFFFFFFF, open));
		}
		g.pose().popPose();
	}

	/** {@code argb} with its alpha times {@code f}; at least 4, since text treats alpha under 4 as opaque. */
	private static int fade(int argb, float f) {
		return Math.max(4, Math.round((argb >>> 24) * f)) << 24 | argb & 0xFFFFFF;
	}

	private static void vertex(VertexConsumer consumer, Matrix4f pose, float cx, float cy, float radius, double angle, int color) {
		consumer.addVertex(pose, cx + (float) Math.cos(angle) * radius, cy + (float) Math.sin(angle) * radius, 0).setColor(color);
	}

	/** No blur: the game stays visible, as in Rust; render darkens it. */
	@Override
	public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {}

	@Override
	public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
		if (TGClient.FORCE_RELOAD.matches(keyCode, scanCode)) {
			select();
			return true;
		}
		return super.keyReleased(keyCode, scanCode, modifiers);
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		select();
		return true;
	}

	private void select() {
		ItemStack stack = minecraft.player.getMainHandItem();
		if (hovered >= 0 && count(hovered) > 0 && stack.is(gun)) {
			String key = keys.get(hovered);
			PacketDistributor.sendToServer(new TGPackets.SwitchAmmo(key));
			gun.switchAmmo(stack, minecraft.level, minecraft.player, InteractionHand.MAIN_HAND, key);
		}
		onClose();
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
