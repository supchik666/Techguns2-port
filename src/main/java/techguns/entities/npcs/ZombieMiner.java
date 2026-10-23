package techguns.entities.npcs;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import techguns.TGuns;

/** 1.12 ZombieMiner: a zombie with a mining helmet and a pickaxe or hand cannon. */
public class ZombieMiner extends GenericNPC {
	public ZombieMiner(EntityType<? extends ZombieMiner> type, Level level) {
		super(type, level);
		burnsInDaylight = true;
		sounds(SoundEvents.ZOMBIE_AMBIENT, SoundEvents.ZOMBIE_HURT, SoundEvents.ZOMBIE_DEATH, SoundEvents.ZOMBIE_STEP);
	}

	@Override
	protected void addRandomArmor(int difficulty) {
		int camo = randomCamo("t1_miner_chestplate");
		wear("t1_miner_helmet", camo);
		maybeWear("t1_miner_chestplate", camo);
		maybeWear("t1_miner_leggings", camo);
		maybeWear("t1_miner_boots", camo);
		hold(Items.STONE_PICKAXE, Items.IRON_PICKAXE, TGuns.HANDCANNON);
	}
}
