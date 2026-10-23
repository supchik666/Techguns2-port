package techguns.entities.npcs;

/** NPC sides (1.12 TGNpcFaction/TGNpcFactions): hostile mobs fight turrets and neutrals; members of one faction never fight. */
public enum TGNpcFaction {
	HOSTILE, TURRET, NEUTRAL;

	public boolean isHostile(TGNpcFaction other) {
		return this != other && (this == HOSTILE || other == HOSTILE);
	}

	/** An entity on a side (1.12 ITGNpcTeam). */
	public interface Member {
		TGNpcFaction getTGFaction();
	}
}
