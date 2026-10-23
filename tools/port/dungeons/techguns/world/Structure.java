package techguns.world;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;

/** Read-only stub of the 1.12 class. */
public class Structure implements Serializable {
	private static final long serialVersionUID = 2L;
	public int sizeX, sizeY, sizeZ;
	public List<techguns.util.MBlock> blocks;
	public HashMap<Integer, List<BlockEntry>> blockEntries;

	public static class BlockEntry implements Serializable {
		private static final long serialVersionUID = 1L;
		public int x, y, z, blockIndex;
	}
}
