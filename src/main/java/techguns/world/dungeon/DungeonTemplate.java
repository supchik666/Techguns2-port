package techguns.world.dungeon;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A dungeon block set, converted from the 1.12 .ser files by tools/port/port_dungeons.py into
 * data/techguns/dungeon/&lt;name&gt;.json: one block grid per {@link Type}, '.' = air, ' ' = leave as is.
 */
public class DungeonTemplate {
	/** Every template, indexed as stored in {@link DungeonPiece}. */
	public static final List<String> NAMES = List.of("ncdung1", "nclower1", "ncmid1", "ncupper1", "nctop1", "ncroof1",
			"nether0", "nether1", "nether_b0", "nether_b1", "tfmain", "tfupper", "netherdungeon_entrance");
	private static final Map<String, DungeonTemplate> LOADED = new ConcurrentHashMap<>();

	public final int sizeXZ, sizeY;
	private final Map<Type, Grid> grids = new EnumMap<>(Type.class);

	/** A segment's blocks, dense [y][z][x]; null entries are left untouched. */
	public record Grid(int sizeX, int sizeY, int sizeZ, BlockState[] blocks) {
		public BlockState get(int x, int y, int z) {
			return blocks[(y * sizeZ + z) * sizeX + x];
		}
	}

	/**
	 * 1.12 TemplateSegment: connection pattern (0 1 2 / 7 X 3 / 6 5 4, -Z on top) and how many rotations to try.
	 * Non-matching types are placed explicitly by the maze path.
	 */
	public enum Type {
		STRAIGHT("00010001", 2), CURVE("00010100", 4), FORK("00010101", 4), CROSS("01010101", 1), END("00010000", 4),
		RAMP(null, 0), ENTRANCE(null, 0),
		ROOM_WALL("00011111", 4), ROOM_CORNER("00011100", 4), ROOM_INNER("11111111", 1), ROOM_DOOR("01011111", 4),
		ROOM_DOOR_CORNER1("01011100", 4), ROOM_DOOR_CORNER2("00011101", 4), ROOM_DOOR_CORNER_DOUBLE("01011101", 4),
		FOUNDATION(null, 0), PILLARS(null, 0),
		/** The single grid of a plain structure (netherdungeon_entrance). */
		STRUCTURE(null, 0);

		final boolean[] pattern;
		final int rotations;

		Type(String pattern, int rotations) {
			this.rotations = rotations;
			this.pattern = new boolean[8];
			for (int i = 0; pattern != null && i < 8; i++) this.pattern[i] = pattern.charAt(i) == '1';
		}

		boolean match() {
			return rotations > 0;
		}
	}

	private DungeonTemplate(int sizeXZ, int sizeY) {
		this.sizeXZ = sizeXZ;
		this.sizeY = sizeY;
	}

	public Grid grid(Type type) {
		return grids.get(type);
	}

	// ponytail: read from the mod jar, not the data pack, so data packs can't override templates; move to a reload listener if they should
	public static DungeonTemplate get(String name) {
		return LOADED.computeIfAbsent(name, DungeonTemplate::load);
	}

	private static DungeonTemplate load(String name) {
		String path = "/data/techguns/dungeon/" + name + ".json";
		try (Reader in = new InputStreamReader(DungeonTemplate.class.getResourceAsStream(path), StandardCharsets.UTF_8)) {
			JsonObject json = JsonParser.parseReader(in).getAsJsonObject();
			DungeonTemplate t = new DungeonTemplate(json.get("size_xz").getAsInt(), json.get("size_y").getAsInt());
			for (Map.Entry<String, com.google.gson.JsonElement> e : json.getAsJsonObject("segments").entrySet()) {
				JsonObject seg = e.getValue().getAsJsonObject();
				Map<Character, BlockState> palette = new HashMap<>();
				palette.put('.', net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
				for (Map.Entry<String, com.google.gson.JsonElement> p : seg.getAsJsonObject("palette").entrySet()) {
					palette.put(p.getKey().charAt(0), BlockStateParser.parseForBlock(BuiltInRegistries.BLOCK.asLookup(), p.getValue().getAsString(), false).blockState());
				}
				JsonArray layers = seg.getAsJsonArray("layers");
				int sy = layers.size(), sz = layers.get(0).getAsJsonArray().size(), sx = layers.get(0).getAsJsonArray().get(0).getAsString().length();
				BlockState[] blocks = new BlockState[sx * sy * sz];
				for (int y = 0; y < sy; y++) {
					JsonArray rows = layers.get(y).getAsJsonArray();
					for (int z = 0; z < sz; z++) {
						String row = rows.get(z).getAsString();
						for (int x = 0; x < sx; x++) blocks[(y * sz + z) * sx + x] = palette.get(row.charAt(x));
					}
				}
				t.grids.put(Type.valueOf(e.getKey()), new Grid(sx, sy, sz, blocks));
			}
			return t;
		} catch (java.io.IOException | CommandSyntaxException | NullPointerException e) {
			throw new IllegalStateException("Broken dungeon template " + path, e);
		}
	}
}
