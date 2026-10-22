package techguns;

import java.util.function.Supplier;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.util.ExtraCodecs;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/** Item data components (the 1.12 stack NBT) and entity data attachments (the 1.12 capabilities). */
public class TGComponents {
	public static final DeferredRegister.DataComponents COMPONENTS = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, Techguns.MODID);
	public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS = DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, Techguns.MODID);

	/** Shots left in a gun. Missing means a full clip, so new guns come loaded as in 1.12. */
	public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> AMMO = COMPONENTS.registerComponentType("ammo",
			builder -> builder.persistent(ExtraCodecs.NON_NEGATIVE_INT).networkSynchronized(ByteBufCodecs.VAR_INT));

	/** Game time from which each hand (indexed by InteractionHand ordinal) may fire again. Not saved, like the 1.12 fire delay. */
	public static final Supplier<AttachmentType<long[]>> NEXT_FIRE = ATTACHMENTS.register("next_fire", () -> AttachmentType.builder(() -> new long[2]).build());
}
