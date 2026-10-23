package techguns.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import techguns.Techguns;

/** Client entry point: NeoForge's generated config screen, from the mod list. */
@Mod(value = Techguns.MODID, dist = Dist.CLIENT)
public class TechgunsClient {
	public TechgunsClient(ModContainer container) {
		container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
	}
}
