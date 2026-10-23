package techguns.entities.npcs;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import techguns.TGuns;

/** 1.12 Commando: commando armor and the silenced M4. */
public class Commando extends GenericNPC {
	public Commando(EntityType<? extends Commando> type, Level level) {
		super(type, level);
		armor = 8;
		sounds(SoundEvents.VILLAGER_AMBIENT, SoundEvents.VILLAGER_HURT, SoundEvents.VILLAGER_DEATH, SoundEvents.ZOMBIE_VILLAGER_STEP);
	}

	@Override
	protected void addRandomArmor(int difficulty) {
		wear("t2_commando_helmet");
		wear("t2_commando_chestplate");
		wear("t2_commando_leggings");
		wear("t2_commando_boots");
		hold(TGuns.M4_INFILTRATOR);
	}
}
