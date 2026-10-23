package techguns.entities.npcs;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import techguns.TGuns;

/** 1.12 DictatorDave: a tough boss with the golden revolver. */
public class DictatorDave extends GenericNPC {
	public DictatorDave(EntityType<? extends DictatorDave> type, Level level) {
		super(type, level);
		armor = 15;
		xpReward = 35;
		sounds(SoundEvents.VILLAGER_AMBIENT, SoundEvents.VILLAGER_HURT, SoundEvents.VILLAGER_DEATH, SoundEvents.ZOMBIE_VILLAGER_STEP);
	}

	@Override
	protected void addRandomArmor(int difficulty) {
		wear("t2_beret");
		wear("t1_combat_chestplate");
		wear("t1_scout_leggings");
		wear("t1_combat_boots");
		hold(TGuns.GOLDENREVOLVER);
	}
}
