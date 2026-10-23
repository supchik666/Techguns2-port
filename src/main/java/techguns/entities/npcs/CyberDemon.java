package techguns.entities.npcs;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import techguns.TGSounds;
import techguns.TGuns;

/** 1.12 CyberDemon: a fire immune nether demon with the nether blaster. */
public class CyberDemon extends GenericNPC {
	public CyberDemon(EntityType<? extends CyberDemon> type, Level level) {
		super(type, level);
		armor = 10;
		sounds(TGSounds.CYBERDEMON_IDLE.get(), TGSounds.CYBERDEMON_HURT.get(), TGSounds.CYBERDEMON_DEATH.get(), TGSounds.CYBERDEMON_STEP.get());
	}

	@Override
	protected void addRandomArmor(int difficulty) {
		hold(TGuns.NETHERBLASTER);
	}
}
