package techguns.entities.npcs;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.loot.LootTable;
import techguns.TGSounds;
import techguns.TGuns;
import techguns.damagesystem.TGDamageType;

/** 1.12 SuperMutantBasic/Heavy/Elite: big mutants, tougher by {@link #tier} 0-2, resisting energy and radiation best. */
public class SuperMutant extends GenericNPC {
	/** Armor against physical/projectile, energy-like and poison/radiation damage, by tier. */
	private static final float[][] ARMOR = {{7, 10, 15}, {12, 16, 18}, {10, 14, 16}};
	/** Model scale by tier (the renderer's; the entity type's size matches). */
	public static final float[] SCALE = {1.35f, 1.5f, 1.65f};
	public final int tier;

	public SuperMutant(EntityType<? extends SuperMutant> type, Level level, int tier) {
		super(type, level);
		this.tier = tier;
		sounds(TGSounds.CYBERDEMON_IDLE.get(), TGSounds.CYBERDEMON_HURT.get(), TGSounds.CYBERDEMON_DEATH.get(), TGSounds.CYBERDEMON_STEP.get());
	}

	@Override
	protected void addRandomArmor(int difficulty) {
		hold(TGuns.ROCKETLAUNCHER, TGuns.AK47, TGuns.COMBATSHOTGUN, TGuns.LASERGUN, TGuns.LASERGUN);
	}

	@Override
	public float getTotalArmorAgainstType(TGDamageType type) {
		return switch (type) {
			case PHYSICAL, PROJECTILE -> ARMOR[tier][0];
			case EXPLOSION, LIGHTNING, ENERGY, FIRE, ICE -> ARMOR[tier][1];
			case POISON, RADIATION -> ARMOR[tier][2];
			default -> 0;
		};
	}

	/** All tiers share the basic mutant's loot, as in 1.12. */
	@Override
	protected ResourceKey<LootTable> getDefaultLootTable() {
		return TGNpcs.SUPER_MUTANT_BASIC.get().getDefaultLootTable();
	}
}
