package techguns.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import techguns.items.guns.GenericGunMeleeCharge;

/** Powered melee tools play their own swing, so the hand doesn't drop with the attack cooldown (1.12 ItemRenderHack). */
@Mixin(ItemInHandRenderer.class)
abstract class ItemInHandRendererMixin {
	@Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;getAttackStrengthScale(F)F"))
	private float techguns$noMeleeCooldownDip(LocalPlayer player, float adjustTicks) {
		return player.getMainHandItem().getItem() instanceof GenericGunMeleeCharge ? 1.0f : player.getAttackStrengthScale(adjustTicks);
	}
}
