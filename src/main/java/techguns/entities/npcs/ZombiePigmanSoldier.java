package techguns.entities.npcs;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import techguns.TGuns;

/** 1.12 ZombiePigmanSoldier: a fire immune nether soldier in desert camo. */
public class ZombiePigmanSoldier extends GenericNPC {
	public ZombiePigmanSoldier(EntityType<? extends ZombiePigmanSoldier> type, Level level) {
		super(type, level);
		armor = 10;
		sounds(SoundEvents.ZOMBIFIED_PIGLIN_AMBIENT, SoundEvents.ZOMBIFIED_PIGLIN_HURT, SoundEvents.ZOMBIFIED_PIGLIN_DEATH, SoundEvents.ZOMBIE_STEP);
	}

	@Override
	protected void addRandomArmor(int difficulty) {
		wear("t2_combat_helmet", 3);
		maybeWear("t2_combat_chestplate", 3);
		maybeWear("t2_combat_leggings", 3);
		maybeWear("t2_combat_boots", 3);
		hold(TGuns.THOMPSON, TGuns.THOMPSON, TGuns.THOMPSON, TGuns.REVOLVER, TGuns.REVOLVER, TGuns.AK47, TGuns.AK47, TGuns.PISTOL, TGuns.PISTOL);
	}
}
