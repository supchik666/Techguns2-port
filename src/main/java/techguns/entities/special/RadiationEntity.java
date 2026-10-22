package techguns.entities.special;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import techguns.TGEntities;
import techguns.TGRadiation;

/**
 * An invisible radiation zone, server only (1.12 EntityRadiation): every second it gives living entities within
 * {@code radius} the radiation effect ({@link TGRadiation#applyRadToEntities}); over the second half of its life the
 * strength fades to 0.
 */
public class RadiationEntity extends Entity {
	static final int RAD_INTERVAL = 20;

	private int duration;
	private float innerRadius, radius;
	private int strength, strengthOuter;

	public RadiationEntity(EntityType<? extends RadiationEntity> type, Level level) {
		super(type, level);
	}

	public RadiationEntity(Level level, Vec3 pos, int duration, float innerRadius, int strength, float radius, int strengthOuter) {
		this(TGEntities.RADIATION.get(), level);
		setPos(pos);
		this.duration = duration;
		this.innerRadius = innerRadius;
		this.strength = strength;
		this.radius = radius;
		this.strengthOuter = strengthOuter;
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {}

	@Override
	public void tick() {
		super.tick();
		if (level().isClientSide) return;
		if (tickCount > duration) {
			discard();
		} else if (tickCount % RAD_INTERVAL == 0) {
			double fade = tickCount * 2 >= duration ? 1 - (tickCount - duration * 0.5) / (duration * 0.5) : 1;
			TGRadiation.applyRadToEntities(level(), position(), radius, RAD_INTERVAL + 2, (int) Math.round(strength * fade), innerRadius,
					(int) Math.round(strengthOuter * fade));
		}
	}

	@Override
	protected void readAdditionalSaveData(CompoundTag tag) {
		tickCount = tag.getInt("Age");
		duration = tag.getInt("Duration");
		innerRadius = tag.getFloat("InnerRadius");
		radius = tag.getFloat("Radius");
		strength = tag.getInt("Strength");
		strengthOuter = tag.getInt("StrengthOuter");
	}

	@Override
	protected void addAdditionalSaveData(CompoundTag tag) {
		tag.putInt("Age", tickCount);
		tag.putInt("Duration", duration);
		tag.putFloat("InnerRadius", innerRadius);
		tag.putFloat("Radius", radius);
		tag.putInt("Strength", strength);
		tag.putInt("StrengthOuter", strengthOuter);
	}
}
