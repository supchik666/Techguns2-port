package techguns.world.dungeon;

import java.io.Serializable;
import java.util.HashMap;

/** Read-only stub of the 1.12 class. */
public class DungeonTemplate implements Serializable {
	private static final long serialVersionUID = 2L;
	public int sizeXZ, sizeY;
	public HashMap<TemplateSegment.SegmentType, DungeonSegment> segments;
	public String name;
}
