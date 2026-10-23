package techguns.entities.npcs;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import techguns.TGuns;

/** 1.12 SkeletonSoldier: an undead gunner that burns in daylight. */
public class SkeletonSoldier extends GenericNPC {
	public SkeletonSoldier(EntityType<? extends SkeletonSoldier> type, Level level) {
		super(type, level);
		burnsInDaylight = true;
		sounds(SoundEvents.SKELETON_AMBIENT, SoundEvents.SKELETON_HURT, SoundEvents.SKELETON_DEATH, SoundEvents.SKELETON_STEP);
	}

	@Override
	protected void addRandomArmor(int difficulty) {
		wear(random.nextBoolean() ? "t1_scout_helmet" : "t1_combat_helmet");
		wear(random.nextBoolean() ? "t1_scout_boots" : "t1_combat_boots");
		hold(TGuns.REVOLVER, TGuns.THOMPSON, TGuns.HANDCANNON);
	}
}
