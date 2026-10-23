package techguns;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import techguns.gui.TGPlayerInventoryMenu;
import techguns.machines.MachineMenu;

public class TGMenus {
	public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, Techguns.MODID);

	public static final DeferredHolder<MenuType<?>, MenuType<TGPlayerInventoryMenu>> PLAYER_INVENTORY = MENUS.register("player_inventory",
			() -> new MenuType<>((id, inventory) -> new TGPlayerInventoryMenu(id, inventory.player), FeatureFlags.VANILLA_SET));
	public static final DeferredHolder<MenuType<?>, MenuType<MachineMenu>> MACHINE = MENUS.register("machine", () -> IMenuTypeExtension.create(MachineMenu::new));
}
