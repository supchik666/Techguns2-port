package techguns.entities.npcs;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import techguns.TGuns;
import techguns.damagesystem.DamageSystem;
import techguns.damagesystem.TGDamageType;

/** 1.12 Outcast: power armor in random camos and a heavy weapon. */
public class Outcast extends GenericNPC {
	public Outcast(EntityType<? extends Outcast> type, Level level) {
		super(type, level);
	}

	@Override
	protected void addRandomArmor(int difficulty) {
		for (String piece : new String[] {"helmet", "chestplate", "leggings", "boots"}) wear("t3_power_" + piece, 1 + random.nextInt(2));
		hold(TGuns.LASERGUN, TGuns.GRIMREAPER, TGuns.MINIGUN, TGuns.BIOGUN, TGuns.FLAMETHROWER, TGuns.TESLAGUN, TGuns.LMG);
	}

	/** Armor from the worn pieces (1.12 GenericNPCGearSpecificStats). */
	@Override
	public float getTotalArmorAgainstType(TGDamageType type) {
		return DamageSystem.armorValue(this, type);
	}
}
