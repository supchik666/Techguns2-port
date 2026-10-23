package techguns.entities.npcs;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import techguns.TGuns;

/** 1.12 Bandit: scout armor and an old gun. */
public class Bandit extends GenericNPC {
	public Bandit(EntityType<? extends Bandit> type, Level level) {
		super(type, level);
		armor = 5;
	}

	@Override
	protected void addRandomArmor(int difficulty) {
		maybeWear("t1_scout_helmet", 0);
		wear("t1_scout_chestplate");
		wear("t1_scout_leggings");
		wear("t1_scout_boots");
		hold(TGuns.PISTOL, TGuns.AK47, TGuns.SAWEDOFF, TGuns.THOMPSON, TGuns.REVOLVER, TGuns.BOLTACTION);
	}
}
