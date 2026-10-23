package techguns.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.world.entity.MobCategory;

/** More hostile mobs with the mod: the monster cap (vanilla 70) is raised by half. ponytail: constant until the M8 config. */
@Mixin(MobCategory.class)
abstract class MobCategoryMixin {
	private static final float MONSTER_CAP = 1.5f;

	@Inject(method = "getMaxInstancesPerChunk", at = @At("RETURN"), cancellable = true)
	private void techguns$monsterCap(CallbackInfoReturnable<Integer> cir) {
		if ((Object) this == MobCategory.MONSTER) cir.setReturnValue(Math.round(cir.getReturnValueI() * MONSTER_CAP));
	}
}
