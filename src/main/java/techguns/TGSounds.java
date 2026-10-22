package techguns;

import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Sound events, named as in 1.12 sounds.json (copied by the asset script). */
public class TGSounds {
	public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, Techguns.MODID);

	public static final DeferredHolder<SoundEvent, SoundEvent> PISTOL_FIRE = register("guns.pistolfire");
	public static final DeferredHolder<SoundEvent, SoundEvent> PISTOL_RELOAD = register("guns.pistolreload");

	public static final DeferredHolder<SoundEvent, SoundEvent> BULLET_IMPACT_STONE = register("guns.impact.rock");
	public static final DeferredHolder<SoundEvent, SoundEvent> BULLET_IMPACT_WOOD = register("guns.impact.wood");
	public static final DeferredHolder<SoundEvent, SoundEvent> BULLET_IMPACT_GLASS = register("guns.impact.glass");
	public static final DeferredHolder<SoundEvent, SoundEvent> BULLET_IMPACT_METAL = register("guns.impact.metal");
	public static final DeferredHolder<SoundEvent, SoundEvent> BULLET_IMPACT_DIRT = register("guns.impact.dirt");

	private static DeferredHolder<SoundEvent, SoundEvent> register(String name) {
		return SOUNDS.register(name, SoundEvent::createVariableRangeEvent);
	}
}
