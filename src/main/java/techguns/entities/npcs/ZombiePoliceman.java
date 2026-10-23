package techguns.entities.npcs;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import techguns.TGuns;

/** 1.12 ZombiePoliceman: a zombie in police armor with a sidearm. */
public class ZombiePoliceman extends GenericNPC {
	public ZombiePoliceman(EntityType<? extends ZombiePoliceman> type, Level level) {
		super(type, level);
		armor = 5;
		burnsInDaylight = true;
		sounds(SoundEvents.ZOMBIE_AMBIENT, SoundEvents.ZOMBIE_HURT, SoundEvents.ZOMBIE_DEATH, SoundEvents.ZOMBIE_STEP);
	}

	@Override
	protected void addRandomArmor(int difficulty) {
		maybeWear("t2_combat_helmet", 5);
		maybeWear("t2_combat_chestplate", 5);
		maybeWear("t2_combat_leggings", 5);
		maybeWear("t2_combat_boots", 5);
		hold(TGuns.REVOLVER, TGuns.PISTOL);
	}
}
