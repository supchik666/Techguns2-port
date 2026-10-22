package techguns;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

@Mod(Techguns.MODID)
public class Techguns {
	public static final String MODID = "techguns";
	public static final Logger LOGGER = LogUtils.getLogger();

	public Techguns(IEventBus modEventBus, ModContainer modContainer) {
	}
}
