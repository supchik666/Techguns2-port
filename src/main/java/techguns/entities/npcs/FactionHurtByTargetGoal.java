package techguns.entities.npcs;

import javax.annotation.Nullable;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;

/**
 * Retaliates against whoever hurt the mob, unless both are {@link TGNpcFaction.Member}s on friendly sides (1.12 EntityAIHurtByTargetTGFactions).
 * Turrets extend it to spare their owner's friends.
 */
public class FactionHurtByTargetGoal extends HurtByTargetGoal {
	public FactionHurtByTargetGoal(PathfinderMob mob) {
		super(mob);
	}

	@Override
	protected boolean canAttack(@Nullable LivingEntity target, TargetingConditions conditions) {
		if (mob instanceof TGNpcFaction.Member owner && target instanceof TGNpcFaction.Member other
				&& !owner.getTGFaction().isHostile(other.getTGFaction())) return false;
		return super.canAttack(target, conditions);
	}
}
