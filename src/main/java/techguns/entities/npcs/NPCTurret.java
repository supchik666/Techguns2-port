package techguns.entities.npcs;

import java.util.UUID;

import javax.annotation.Nullable;

import com.mojang.authlib.GameProfile;

import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.ambient.AmbientCreature;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.PlayerTeam;
import techguns.damagesystem.DamageSystem;
import techguns.damagesystem.INpcTGDamageSystem;
import techguns.damagesystem.TGDamageType;
import techguns.entities.projectiles.GenericProjectile;
import techguns.plugins.sable.SableCompat;
import techguns.items.guns.GenericGun;
import techguns.machines.TurretBlockEntity;

/**
 * 1.12 NPCTurret: sits on (or hangs under) a {@link TurretBlockEntity} and shoots its gun with the base's ammo. Targets
 * hostile mobs within 20 blocks up or down, animals if the base says so, and players and other turrets per the base's PvP
 * setting; never its owner. Lives only while its base does; the base heals it, powers it and rebuilds it when it dies.
 */
public class NPCTurret extends PathfinderMob implements RangedAttackMob, INpcTGDamageSystem, TGNpcFaction.Member {
	private static final EntityDataAccessor<BlockPos> BASE = SynchedEntityData.defineId(NPCTurret.class, EntityDataSerializers.BLOCK_POS);
	private static final EntityDataAccessor<Boolean> HANGING = SynchedEntityData.defineId(NPCTurret.class, EntityDataSerializers.BOOLEAN);
	@Nullable
	private Item gun;
	@Nullable
	private Goal attackGoal;

	public NPCTurret(EntityType<? extends NPCTurret> type, Level level) {
		super(type, level);
		setNoGravity(true);
		setPersistenceRequired();
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 60).add(Attributes.MOVEMENT_SPEED, 0) // 1.12 had 30 health
				.add(Attributes.KNOCKBACK_RESISTANCE, 1).add(Attributes.FOLLOW_RANGE, 20);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(BASE, BlockPos.ZERO);
		builder.define(HANGING, false);
	}

	/** Mounts it on the base at {@code base}. */
	public void link(BlockPos base, boolean hanging) {
		entityData.set(BASE, base);
		entityData.set(HANGING, hanging);
		refreshDimensions();
		pin();
	}

	public BlockPos basePos() {
		return entityData.get(BASE);
	}

	public boolean isHanging() {
		return entityData.get(HANGING);
	}

	@Nullable
	public TurretBlockEntity base() {
		return level().getBlockEntity(entityData.get(BASE)) instanceof TurretBlockEntity be ? be : null;
	}

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(3, new RandomLookAroundGoal(this));
		targetSelector.addGoal(1, new FactionHurtByTargetGoal(this) {
			@Override
			protected boolean canAttack(@Nullable LivingEntity target, TargetingConditions conditions) {
				TurretBlockEntity be = base();
				return !(target instanceof Player player && be != null && be.owner != null && isFriend(be.owner, player.getUUID()))
						&& super.canAttack(target, conditions);
			}
		});
		targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, LivingEntity.class, 10, true, false, this::isTarget) {
			@Override
			protected AABB getTargetSearchArea(double range) {
				return mob.getBoundingBox().inflate(range, 20.0, range);
			}
		});
	}

	/** 1.12 TurretEntityAINearestAttackableTarget's selector. */
	public boolean isTarget(LivingEntity target) {
		TurretBlockEntity be = base();
		if (be == null) return false;
		if (target instanceof TGNpcFaction.Member member && !getTGFaction().isHostile(member.getTGFaction()) && !(target instanceof NPCTurret)) return false;
		if (target instanceof Enemy) return true;
		if (be.pvp != 0 && be.owner != null) {
			if (target instanceof NPCTurret other) return other.base() != null && pvpTarget(be, other.base().owner);
			if (target instanceof Player player) return pvpTarget(be, player.getUUID());
		}
		if (be.attackAnimals && (target instanceof Animal || target instanceof WaterAnimal || target instanceof AmbientCreature)) {
			return !(target instanceof OwnableEntity pet && pet.getOwnerUUID() != null); // spares tamed pets and horses
		}
		return false;
	}

	private boolean pvpTarget(TurretBlockEntity be, @Nullable UUID other) {
		return other != null && !(be.pvp == 4 ? other.equals(be.owner) : isFriend(be.owner, other));
	}

	/** {@code id} is {@code owner} or on the owner's scoreboard team. ponytail: vanilla teams stand in for 1.12's FTB teams. */
	private boolean isFriend(UUID owner, UUID id) {
		if (owner.equals(id)) return true;
		PlayerTeam team = level().getScoreboard().getPlayersTeam(name(owner));
		return team != null && team.getPlayers().contains(name(id));
	}

	private String name(UUID id) {
		MinecraftServer server = level().getServer();
		if (server == null || server.getProfileCache() == null) return "";
		return server.getProfileCache().get(id).map(GameProfile::getName).orElse("");
	}

	/** The base's gun is the turret's: it renders it, and NPC code reads it as the main hand. */
	@Override
	public ItemStack getItemBySlot(EquipmentSlot slot) {
		TurretBlockEntity be = slot == EquipmentSlot.MAINHAND ? base() : null;
		return be != null ? be.weapon() : super.getItemBySlot(slot);
	}

	@Override
	public void tick() {
		pin();
		if (!level().isClientSide) {
			TurretBlockEntity be = base();
			if (be == null) {
				hurt(damageSources().fellOutOfWorld(), 1.0f); // 1.12: an orphan wastes away
			} else if (!getUUID().equals(be.turretId)) {
				discard(); // its base made another one
				return;
			} else {
				setCombatTask(be.weapon());
			}
		}
		super.tick();
	}

	/** Sits on the base, or hangs below it; on a Sable ship, where the ship is in the world. ponytail: doesn't tilt with the ship */
	private void pin() {
		BlockPos base = entityData.get(BASE);
		setPos(SableCompat.toWorld(level(), new Vec3(base.getX() + 0.5, isHanging() ? base.getY() - getBbHeight() : base.getY() + 1.0, base.getZ() + 0.5)));
		setDeltaMovement(0, 0, 0);
	}

	/** Swaps the attack goal when the base's gun changes (1.12 setCombatTask); follow range is the gun's AI range. */
	private void setCombatTask(ItemStack weapon) {
		if (weapon.getItem() == gun) return;
		gun = weapon.getItem();
		if (attackGoal != null) goalSelector.removeGoal(attackGoal);
		attackGoal = null;
		if (gun instanceof GenericGun g) {
			attackGoal = g.getAIAttack(this);
			goalSelector.addGoal(1, attackGoal);
			getAttribute(Attributes.FOLLOW_RANGE).setBaseValue(g.getAIRange());
		}
	}

	/** Fires from the middle of the turret if the base has a round. */
	@Override
	public void performRangedAttack(LivingEntity target, float distanceFactor) {
		TurretBlockEntity be = base();
		if (be == null || !(be.weapon().getItem() instanceof GenericGun g) || !be.consumeAmmo()) return;
		lookAt(target, 360, 360);
		setYHeadRot(getYRot());
		g.fireWeaponFromNPC(this, be.weapon(), 1.0f, 1.0f, GenericProjectile.CENTER);
	}

	@Override
	public void die(DamageSource source) {
		super.die(source);
		TurretBlockEntity be = base();
		if (!level().isClientSide && be != null && getUUID().equals(be.turretId)) be.onTurretDeath();
	}

	@Override
	protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHit) {} // the gun stays in the base

	@Override
	public float getTotalArmorAgainstType(TGDamageType type) {
		TurretBlockEntity be = base();
		return DamageSystem.armorAgainstTypeDefault(this, be == null ? 0 : be.armorValue(), type);
	}

	@Override
	public TGNpcFaction getTGFaction() {
		return TGNpcFaction.TURRET;
	}

	@Override
	public EntityDimensions getDefaultDimensions(Pose pose) {
		EntityDimensions dimensions = super.getDefaultDimensions(pose);
		return dimensions.withEyeHeight(isHanging() ? 0.15f : 0.75f);
	}

	@Override
	public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
		super.onSyncedDataUpdated(key);
		if (HANGING.equals(key)) refreshDimensions();
	}

	@Override
	public boolean removeWhenFarAway(double distance) {
		return false;
	}

	@Override
	public boolean isPushable() {
		return false;
	}

	@Override
	protected void doPush(Entity entity) {}

	@Override
	public boolean isPushedByFluid() {
		return false;
	}

	@Override
	public void addAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
		super.addAdditionalSaveData(tag);
		tag.putLong("base", entityData.get(BASE).asLong());
		tag.putBoolean("hanging", isHanging());
	}

	@Override
	public void readAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
		super.readAdditionalSaveData(tag);
		link(BlockPos.of(tag.getLong("base")), tag.getBoolean("hanging"));
	}
}
