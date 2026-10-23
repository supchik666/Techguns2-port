package techguns.entities.npcs;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import techguns.TGuns;

/** 1.12 ZombieSoldier: a zombie in combat armor that doesn't burn, with a gun or shovel. */
public class ZombieSoldier extends GenericNPC {
	public ZombieSoldier(EntityType<? extends ZombieSoldier> type, Level level) {
		super(type, level);
		armor = 5;
		sounds(SoundEvents.ZOMBIE_AMBIENT, SoundEvents.ZOMBIE_HURT, SoundEvents.ZOMBIE_DEATH, SoundEvents.ZOMBIE_STEP);
	}

	@Override
	protected void addRandomArmor(int difficulty) {
		maybeWear("t1_combat_helmet", 0);
		maybeWear("t1_combat_chestplate", 0);
		maybeWear("t1_combat_leggings", 0);
		maybeWear("t1_combat_boots", 0);
		hold(TGuns.REVOLVER, TGuns.THOMPSON, Items.IRON_SHOVEL, Items.STONE_SHOVEL);
	}
}
