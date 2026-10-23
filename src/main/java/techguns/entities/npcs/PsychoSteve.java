package techguns.entities.npcs;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import techguns.TGuns;

/** 1.12 PsychoSteve: a fast miner with a chainsaw. */
public class PsychoSteve extends GenericNPC {
	public PsychoSteve(EntityType<? extends PsychoSteve> type, Level level) {
		super(type, level);
		armor = 5;
		xpReward = 25;
		sounds(SoundEvents.VILLAGER_AMBIENT, SoundEvents.VILLAGER_HURT, SoundEvents.VILLAGER_DEATH, SoundEvents.ZOMBIE_STEP);
	}

	@Override
	protected void addRandomArmor(int difficulty) {
		int camo = randomCamo("t1_miner_chestplate");
		for (String piece : new String[] {"helmet", "chestplate", "leggings", "boots"}) wear("t1_miner_" + piece, camo);
		hold(TGuns.CHAINSAW);
	}
}
