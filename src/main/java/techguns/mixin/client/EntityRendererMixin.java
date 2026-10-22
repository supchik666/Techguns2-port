package techguns.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import techguns.client.LightPulses;

/** Block light from {@link LightPulses} on entities and held items. */
@Mixin(EntityRenderer.class)
abstract class EntityRendererMixin {
	@Inject(method = "getBlockLightLevel", at = @At("RETURN"), cancellable = true)
	private void techguns$lightPulses(Entity entity, BlockPos pos, CallbackInfoReturnable<Integer> cir) {
		int light = LightPulses.blockLight(pos);
		if (light > cir.getReturnValueI()) cir.setReturnValue(light);
	}
}
