package techguns;

import net.neoforged.neoforge.common.ModConfigSpec;

/** config/techguns-client.toml; NeoForge reloads it when the file changes. */
public final class TGConfig {
	private static final ModConfigSpec.Builder CLIENT = new ModConfigSpec.Builder();

	public static final ModConfigSpec.BooleanValue BLOOM = CLIENT
			.comment("Glow around muzzle flashes, energy shots, explosions and lit parts of guns and armor. Always off with Iris/Oculus.")
			.define("bloom", true);

	public static final ModConfigSpec.BooleanValue SCOPE_LENS = CLIENT
			.comment("Lens look through scopes: the view bends and splits into colors toward the rim, which is shaded. Always off with Iris/Oculus.")
			.define("scopeLens", true);

	public static final ModConfigSpec.BooleanValue SCREEN_SHAKE = CLIENT
			.comment("Camera shake from your own shots, and the view kick through scopes.")
			.define("screenShake", true);

	public static final ModConfigSpec CLIENT_SPEC = CLIENT.build();

	private TGConfig() {}
}
