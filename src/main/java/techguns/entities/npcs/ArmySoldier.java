package techguns.entities.npcs;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import techguns.TGuns;

/** 1.12 ArmySoldier: combat armor or a beret, rifle, shotgun or bolt action. */
public class ArmySoldier extends GenericNPC {
	public ArmySoldier(EntityType<? extends ArmySoldier> type, Level level) {
		super(type, level);
		armor = 8;
		sounds(SoundEvents.VILLAGER_AMBIENT, SoundEvents.VILLAGER_HURT, SoundEvents.VILLAGER_DEATH, SoundEvents.ZOMBIE_VILLAGER_STEP);
	}

	@Override
	protected void addRandomArmor(int difficulty) {
		wear(random.nextBoolean() ? "t2_combat_helmet" : "t2_beret");
		maybeWear("t2_combat_chestplate", 0);
		maybeWear("t2_combat_leggings", 0);
		maybeWear("t2_combat_boots", 0);
		hold(TGuns.M4, TGuns.COMBATSHOTGUN, TGuns.BOLTACTION);
	}
}
