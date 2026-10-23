package techguns;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import techguns.recipes.AmmoChangeRecipe;

@Mod(Techguns.MODID)
public class Techguns {
	public static final String MODID = "techguns";
	public static final Logger LOGGER = LogUtils.getLogger();

	public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

	public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB = CREATIVE_TABS.register(MODID, () -> CreativeModeTab.builder()
			.title(Component.translatable("itemGroup.techguns"))
			.icon(() -> TGItems.PISTOL_ROUNDS.get().getDefaultInstance())
			.displayItems((params, output) -> TGItems.ITEMS.getEntries().forEach(item -> output.accept(item.get())))
			.build());

	public Techguns(IEventBus modEventBus, ModContainer modContainer) {
		TGBlocks.BLOCKS.register(modEventBus);
		TGItems.ITEMS.register(modEventBus);
		TGuns.init();
		TGArmors.init(modEventBus);
		TGComponents.COMPONENTS.register(modEventBus);
		TGComponents.ATTACHMENTS.register(modEventBus);
		TGSounds.SOUNDS.register(modEventBus);
		TGAttributes.ATTRIBUTES.register(modEventBus);
		TGRadiation.EFFECTS.register(modEventBus);
		TGRadiation.POTIONS.register(modEventBus);
		TGEntities.ENTITIES.register(modEventBus);
		CREATIVE_TABS.register(modEventBus);
		AmmoChangeRecipe.SERIALIZERS.register(modEventBus);
		modContainer.registerConfig(ModConfig.Type.CLIENT, TGConfig.CLIENT_SPEC);
	}

	public static ResourceLocation id(String path) {
		return ResourceLocation.fromNamespaceAndPath(MODID, path);
	}
}
