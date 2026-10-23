import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import techguns.util.MBlock;
import techguns.world.Structure;
import techguns.world.dungeon.DungeonSegment;
import techguns.world.dungeon.DungeonTemplate;

/**
 * Prints 1.12 dungeon templates (.ser, read through the stubs next to this file) as JSON: per segment
 * its size, palette ("name meta") and a dense y/z/x array of palette indices (-1 where nothing was scanned).
 */
public class DumpDungeons {
	public static void main(String[] args) throws Exception {
		StringBuilder sb = new StringBuilder("{");
		for (int a = 0; a < args.length; a++) {
			String path = args[a];
			DungeonTemplate t;
			try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(path))) {
				t = (DungeonTemplate) in.readObject();
			}
			String name = path.substring(path.lastIndexOf('/') + 1).replace(".ser", "");
			sb.append(a > 0 ? "," : "").append('"').append(name).append("\":{\"size_xz\":").append(t.sizeXZ)
					.append(",\"size_y\":").append(t.sizeY).append(",\"segments\":{");
			boolean first = true;
			for (Map.Entry<?, DungeonSegment> e : t.segments.entrySet()) {
				Structure s = e.getValue().structure;
				int[] idx = new int[s.sizeX * s.sizeY * s.sizeZ];
				Arrays.fill(idx, -1);
				for (List<Structure.BlockEntry> l : s.blockEntries.values())
					for (Structure.BlockEntry b : l) idx[(b.y * s.sizeZ + b.z) * s.sizeX + b.x] = b.blockIndex;
				sb.append(first ? "" : ",").append('"').append(e.getKey()).append("\":{\"size\":[")
						.append(s.sizeX).append(',').append(s.sizeY).append(',').append(s.sizeZ).append("],\"palette\":[");
				first = false;
				for (int i = 0; i < s.blocks.size(); i++) {
					MBlock m = s.blocks.get(i);
					sb.append(i > 0 ? "," : "").append('"').append(m.name).append(' ').append(m.meta).append('"');
				}
				sb.append("],\"blocks\":").append(Arrays.toString(idx).replace(" ", "")).append('}');
			}
			sb.append("}}");
		}
		System.out.println(sb.append('}'));
	}
}
