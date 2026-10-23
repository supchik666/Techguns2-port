package techguns.entities.npcs;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import techguns.TGuns;
import techguns.damagesystem.DamageSystem;
import techguns.damagesystem.TGDamageType;

/** 1.12 StormTrooper: a white HEV suit and a blaster or laser. */
public class StormTrooper extends GenericNPC {
	public StormTrooper(EntityType<? extends StormTrooper> type, Level level) {
		super(type, level);
		sounds(SoundEvents.VILLAGER_AMBIENT, SoundEvents.VILLAGER_HURT, SoundEvents.VILLAGER_DEATH, SoundEvents.ZOMBIE_STEP);
	}

	@Override
	protected void addRandomArmor(int difficulty) {
		for (String piece : new String[] {"helmet", "chestplate", "leggings", "boots"}) wear("t3_miner_" + piece, 1);
		hold(TGuns.BLASTERRIFLE, TGuns.LASERGUN);
	}

	/** Armor from the worn pieces (1.12 GenericNPCGearSpecificStats). */
	@Override
	public float getTotalArmorAgainstType(TGDamageType type) {
		return DamageSystem.armorValue(this, type);
	}
}
