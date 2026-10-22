package techguns;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityAttributeModificationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

@EventBusSubscriber(modid = Techguns.MODID)
public class TGAttributes {
	public static final DeferredRegister<Attribute> ATTRIBUTES = DeferredRegister.create(Registries.ATTRIBUTE, Techguns.MODID);

	/** 1.12 TGRadiation.RADIATION_RESISTANCE: each point takes one off the radiation the radiation effect gives every second. */
	public static final DeferredHolder<Attribute, Attribute> RADIATION_RESISTANCE = ATTRIBUTES.register("radiation_resistance",
			() -> new RangedAttribute("attribute.name.techguns.radiation_resistance", 0, 0, Float.MAX_VALUE).setSyncable(true));

	/** Every living entity has them, as in 1.12. */
	@SubscribeEvent
	static void addToEntities(EntityAttributeModificationEvent event) {
		event.getTypes().forEach(type -> event.add(type, RADIATION_RESISTANCE));
	}
}
