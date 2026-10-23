package techguns.client.particle;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Scanner;
import java.util.Set;
import java.util.function.Function;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.phys.Vec3;
import techguns.Techguns;
import techguns.client.particle.TGParticleSystemType.AlphaEntry;
import techguns.client.particle.TGParticleSystemType.ColorEntry;
import techguns.client.particle.TGParticleSystemType.RenderMode;
import techguns.client.particle.TGParticleSystemType.VelocityType;
import techguns.client.particle.TGParticleSystemType.VolumeType;

/**
 * Loads the effects in {@code assets/techguns/particles/*.txt}: particle systems ({@code Name { Key = values ... }}) and
 * {@code FXList Name { system... }} lists that spawn several at once. Names are case-insensitive.
 */
public final class TGFX {
	/** In load order: EXTENDS needs its parent loaded first. */
	private static final String[] FILES = { "fxlist.txt", "nuke.txt", "deathfx.txt", "impactfx.txt", "testfx.txt" };
	private static final Map<String, TGParticleSystemType> SYSTEMS = new HashMap<>();
	private static final Map<String, List<String>> LISTS = new HashMap<>();

	private TGFX() {}

	/** The systems of effect {@code name}, made by {@code factory}; none and a warning if there's no such effect. */
	static List<TGParticleSystem> create(String name, Function<TGParticleSystemType, TGParticleSystem> factory) {
		List<TGParticleSystem> systems = new ArrayList<>();
		if (!add(name.toLowerCase(Locale.ROOT), factory, systems)) Techguns.LOGGER.warn("TGFX '{}' not found", name);
		return systems;
	}

	/** System {@code name}, null if there's none (lists aren't systems). */
	static TGParticleSystemType type(String name) {
		return SYSTEMS.get(name.toLowerCase(Locale.ROOT));
	}

	private static boolean add(String name, Function<TGParticleSystemType, TGParticleSystem> factory, List<TGParticleSystem> out) {
		TGParticleSystemType type = SYSTEMS.get(name);
		if (type != null) return out.add(factory.apply(type));
		List<String> list = LISTS.get(name);
		if (list != null) list.forEach(entry -> add(entry, factory, out));
		return list != null;
	}

	/** Resource reload: parses all files, then warns about list entries, attached systems and textures that don't exist. */
	static void load(ResourceManager manager) {
		SYSTEMS.clear();
		LISTS.clear();
		for (String file : FILES) {
			Optional<Resource> resource = manager.getResource(Techguns.id("particles/" + file));
			if (resource.isEmpty()) {
				Techguns.LOGGER.warn("TGFX file {} is missing", file);
				continue;
			}
			try (InputStream in = resource.get().open()) {
				parse(new Scanner(in), file);
			} catch (IOException e) {
				Techguns.LOGGER.warn("Can't read TGFX file {}", file, e);
			}
		}
		LISTS.forEach((name, list) -> list.stream().filter(entry -> !exists(entry))
				.forEach(entry -> Techguns.LOGGER.warn("TGFX list {}: no effect '{}'", name, entry)));
		SYSTEMS.forEach((name, type) -> {
			if (type.attachedSystem != null && !exists(type.attachedSystem.toLowerCase(Locale.ROOT)))
				Techguns.LOGGER.warn("TGFX {}: no attached effect '{}'", name, type.attachedSystem);
			if (manager.getResource(type.texture).isEmpty()) Techguns.LOGGER.warn("TGFX {}: no texture {}", name, type.texture);
		});
		Set<String> inExplosions = new HashSet<>();
		LISTS.forEach((name, list) -> {
			if (name.contains("explosion")) inExplosions.addAll(list);
		});
		SYSTEMS.forEach((name, type) -> type.bloom = bloom(type, inExplosions.contains(name) || name.contains("explosion") || name.contains("nuke"))
				* (name.startsWith("TFG") ? TFG_BLOOM : 1));
		Techguns.LOGGER.debug("Loaded {} TGFX systems and {} lists", SYSTEMS.size(), LISTS.size());
	}

	/**
	 * Glow strength of a system: additive ones glow like a muzzle flash, explosions (systems named so or in an explosion
	 * list) much more; alpha blended fire glows a little, smoke and debris not at all. ponytail: a heuristic on names and
	 * textures; a Bloom key in the .txt files would be exact.
	 */
	/** The TFG's charge ball, trail and blast are huge; they glow less so they don't wash out the screen. */
	private static final float TFG_BLOOM = 0.5f;

	static float bloom(TGParticleSystemType type, boolean explosion) {
		if (type.renderType == RenderMode.ADDITIVE || type.renderType == RenderMode.NO_Z_TEST) return explosion ? 2.5f : 1;
		String texture = type.texture.getPath();
		return texture.contains("fire") || texture.contains("flame") ? explosion ? 1.5f : 0.5f : 0;
	}

	private static boolean exists(String name) {
		return SYSTEMS.containsKey(name) || LISTS.containsKey(name);
	}

	/** The 1.12 tokenizer: "=", " {", comments and whitespace all separate tokens. A broken block is logged and skipped. */
	private static void parse(Scanner sc, String file) {
		sc.useLocale(Locale.ENGLISH);
		sc.useDelimiter("(\\s*=\\s*)|\\s\\{|\\s*//.*|\\r\\n|\\s+");
		while (sc.hasNext()) {
			String name = sc.next();
			if (name.isEmpty()) continue;
			try {
				if (name.equalsIgnoreCase("fxlist")) {
					name = token(sc);
					List<String> list = new ArrayList<>();
					for (String entry; !(entry = token(sc)).equals("}");) list.add(entry.toLowerCase(Locale.ROOT));
					LISTS.put(name.toLowerCase(Locale.ROOT), list);
				} else {
					SYSTEMS.put(name.toLowerCase(Locale.ROOT), parseSystem(sc));
				}
			} catch (RuntimeException e) {
				Techguns.LOGGER.warn("Can't parse TGFX {} in {}: {}", name, file, e.toString());
				while (sc.hasNext() && !sc.next().equals("}")) {}
			}
		}
	}

	/** The next non-empty token (adjacent separators make empty ones). */
	private static String token(Scanner sc) {
		String token;
		do token = sc.next();
		while (token.isEmpty());
		return token;
	}

	/** Unknown keys (RandomRotation, Rows) and their values are skipped, as in 1.12. */
	private static TGParticleSystemType parseSystem(Scanner sc) {
		TGParticleSystemType t = new TGParticleSystemType();
		for (String key; !(key = token(sc)).equals("}");) {
			switch (key.toLowerCase(Locale.ROOT)) {
				case "extends" -> {
					TGParticleSystemType parent = SYSTEMS.get(token(sc).toLowerCase(Locale.ROOT));
					if (parent == null) throw new IllegalArgumentException("EXTENDS of an unknown system");
					t = parent.copy();
				}
				case "rendertype" -> t.renderType = RenderMode.valueOf(token(sc).toUpperCase(Locale.ROOT));
				case "texture" -> t.texture = ResourceLocation.parse(token(sc).toLowerCase(Locale.ROOT)); // textures were lowercased in the port
				case "columns" -> t.columns = sc.nextInt();
				case "frames" -> t.frames = sc.nextInt();
				case "hasvariations" -> t.hasVariations = sc.nextBoolean();
				case "angle" -> {
					t.angleMin = sc.nextFloat();
					t.angleMax = sc.nextFloat();
				}
				case "anglerate" -> {
					t.angleRateMin = sc.nextFloat();
					t.angleRateMax = sc.nextFloat();
				}
				case "angleratedamping" -> {
					t.angleRateDampingMin = sc.nextFloat();
					t.angleRateDampingMax = sc.nextFloat();
				}
				case "lifetime" -> {
					t.lifetimeMin = sc.nextInt();
					t.lifetimeMax = sc.nextInt();
				}
				case "systemlifetime" -> {
					t.systemLifetimeMin = sc.nextInt();
					t.systemLifetimeMax = sc.nextInt();
				}
				case "animationspeed" -> {
					t.animationSpeedMin = sc.nextFloat();
					t.animationSpeedMax = sc.nextFloat();
				}
				case "size" -> {
					t.sizeMin = sc.nextFloat();
					t.sizeMax = sc.nextFloat();
				}
				case "sizerate" -> {
					t.sizeRateMin = sc.nextFloat();
					t.sizeRateMax = sc.nextFloat();
				}
				case "sizeratedamping" -> {
					t.sizeRateDampingMin = sc.nextFloat();
					t.sizeRateDampingMax = sc.nextFloat();
				}
				case "startsizerate" -> {
					t.startSizeRateMin = sc.nextFloat();
					t.startSizeRateMax = sc.nextFloat();
				}
				case "startsizeratedamping" -> {
					t.startSizeRateDampingMin = sc.nextFloat();
					t.startSizeRateDampingMax = sc.nextFloat();
				}
				case "alpha" -> t.alphaEntries.add(new AlphaEntry(sc.nextFloat(), sc.nextFloat()));
				case "color" -> t.colorEntries.add(new ColorEntry(sc.nextInt() / 255f, sc.nextInt() / 255f, sc.nextInt() / 255f, sc.nextFloat()));
				case "particlecount" -> {
					t.particleCountMin = sc.nextInt();
					t.particleCountMax = sc.nextInt();
				}
				case "initialdelay" -> {
					t.initialDelayMin = sc.nextInt();
					t.initialDelayMax = sc.nextInt();
				}
				case "spawndelay" -> {
					t.spawnDelayMin = sc.nextInt();
					t.spawnDelayMax = sc.nextInt();
				}
				case "velocitytype" -> t.velocityType = VelocityType.valueOf(token(sc).toUpperCase(Locale.ROOT));
				case "velocity1", "velocity2", "velocity3" -> {
					int i = key.charAt(8) - '1';
					t.velocityDataMin[i] = sc.nextFloat();
					t.velocityDataMax[i] = sc.nextFloat();
				}
				case "velocitydamping" -> {
					t.velocityDampingMin = sc.nextFloat();
					t.velocityDampingMax = sc.nextFloat();
				}
				case "volumetype" -> t.volumeType = VolumeType.valueOf(token(sc).toUpperCase(Locale.ROOT));
				case "volume1", "volume2", "volume3" -> t.volumeData[key.charAt(6) - '1'] = sc.nextFloat();
				case "gravity" -> t.gravity = sc.nextFloat();
				case "ishollow" -> t.isHollow = sc.nextBoolean();
				case "systemvelocityfactor" -> {
					t.systemVelocityFactorMin = sc.nextFloat();
					t.systemVelocityFactorMax = sc.nextFloat();
				}
				case "offset" -> t.offset = new Vec3(sc.nextFloat(), sc.nextFloat(), sc.nextFloat());
				case "particlesmovewithsystem" -> t.particlesMoveWithSystem = sc.nextBoolean();
				case "particlessticktosystem" -> t.particlesStickToSystem = sc.nextBoolean();
				case "groundaligned" -> t.groundAligned = sc.nextBoolean();
				case "streak" -> t.streak = sc.nextBoolean();
				case "attachedsystem" -> t.attachedSystem = token(sc);
				// ponytail: RemoveOnGround and VelocityDampingOnGround are skipped; 1.12 never set onGround for these particles
				default -> {}
			}
		}
		return t;
	}
}
