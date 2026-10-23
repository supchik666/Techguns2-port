package techguns.entities.npcs;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import techguns.TGComponents;
import techguns.Techguns;
import techguns.damagesystem.DamageSystem;
import techguns.damagesystem.INpcTGDamageSystem;
import techguns.damagesystem.TGDamageType;
import techguns.items.armors.GenericArmor;
import techguns.items.guns.GenericGun;

/**
 * 1.12 GenericNPC: a hostile humanoid that shoots the gun in its main hand ({@link GenericGun#getAIAttack}) or melees with anything else.
 * Its TG armor is a flat value per damage type ({@link #armor}), not the pieces it wears. Subclasses pick gear in {@link #addRandomArmor}.
 */
public abstract class GenericNPC extends Monster implements RangedAttackMob, INpcTGDamageSystem, TGNpcFaction.Member {
	/** Flat TG armor, see {@link DamageSystem#armorAgainstTypeDefault}. */
	protected float armor;
	/** Undead that burn in daylight (1.12 GenericNPCUndead.shouldBurnInDay). */
	protected boolean burnsInDaylight;
	private SoundEvent ambient = SoundEvents.VILLAGER_AMBIENT, hurt = SoundEvents.VILLAGER_HURT, death = SoundEvents.VILLAGER_DEATH;
	@Nullable
	private SoundEvent step;
	@Nullable
	private Goal attackGoal;

	protected GenericNPC(EntityType<? extends GenericNPC> type, Level level) {
		super(type, level);
		setCombatTask();
	}

	/** Attributes for {@code EntityAttributeCreationEvent}; add armor toughness where 1.12 set it. */
	public static AttributeSupplier.Builder attributes(double speed, double health, double attack, double followRange) {
		return Monster.createMonsterAttributes().add(Attributes.MOVEMENT_SPEED, speed).add(Attributes.MAX_HEALTH, health)
				.add(Attributes.ATTACK_DAMAGE, attack).add(Attributes.FOLLOW_RANGE, followRange);
	}

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(1, new FloatGoal(this));
		goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 1.0));
		goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0f));
		goalSelector.addGoal(6, new RandomLookAroundGoal(this));
		targetSelector.addGoal(1, new FactionHurtByTargetGoal(this));
		targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
	}

	/** Gear for a mob spawned with {@code difficulty} 0-3 (danger level from the spawn manager, regional difficulty otherwise). */
	protected abstract void addRandomArmor(int difficulty);

	/** Replaces a dummy spawn (see {@link TGSpawnManager}). */
	public void onSpawnByManager(int difficulty) {
		setCanPickUpLoot(false);
		addRandomArmor(difficulty);
	}

	@Override
	public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType spawnType, @Nullable SpawnGroupData data) {
		data = super.finalizeSpawn(level, difficulty, spawnType, data);
		addRandomArmor(Math.round(difficulty.getSpecialMultiplier() * 3));
		setCanPickUpLoot(false);
		return data;
	}

	/** Picks the attack goal for the held item (1.12 setCombatTask): the gun's ranged goal, or melee. */
	private void setCombatTask() {
		if (level() == null || level().isClientSide) return;
		if (attackGoal != null) goalSelector.removeGoal(attackGoal);
		attackGoal = getMainHandItem().getItem() instanceof GenericGun gun ? gun.getAIAttack(this) : new MeleeAttackGoal(this, 1.2, false);
		goalSelector.addGoal(4, attackGoal);
	}

	@Override
	public void setItemSlot(EquipmentSlot slot, ItemStack stack) {
		super.setItemSlot(slot, stack);
		if (slot == EquipmentSlot.MAINHAND) setCombatTask();
	}

	@Override
	public void readAdditionalSaveData(CompoundTag tag) {
		super.readAdditionalSaveData(tag);
		setCombatTask();
	}

	/** 1.12 attackEntityWithRangedAttack: worse aim and damage on easier difficulties. */
	@Override
	public void performRangedAttack(LivingEntity target, float distanceFactor) {
		if (!(getMainHandItem().getItem() instanceof GenericGun gun)) return;
		float accuracy = 1, damage = 1;
		switch (level().getDifficulty()) {
			case EASY -> { accuracy = 1.3f; damage = 0.6f; }
			case NORMAL -> { accuracy = 1.15f; damage = 0.8f; }
			default -> {}
		}
		// shots follow the head; the look control may still be turning
		lookAt(target, 360, 360);
		setYHeadRot(getYRot());
		gun.fireWeaponFromNPC(this, getMainHandItem(), damage, accuracy);
	}

	@Override
	public float getTotalArmorAgainstType(TGDamageType type) {
		return DamageSystem.armorAgainstTypeDefault(this, armor, type);
	}

	@Override
	public TGNpcFaction getTGFaction() {
		return TGNpcFaction.HOSTILE;
	}

	@Override
	public void aiStep() {
		if (burnsInDaylight && isAlive() && isSunBurnTick()) igniteForSeconds(8);
		super.aiStep();
	}

	protected void sounds(SoundEvent ambient, SoundEvent hurt, SoundEvent death, @Nullable SoundEvent step) {
		this.ambient = ambient;
		this.hurt = hurt;
		this.death = death;
		this.step = step;
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return ambient;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return hurt;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return death;
	}

	@Override
	protected void playStepSound(BlockPos pos, BlockState state) {
		if (step != null) playSound(step, 0.15f, 1.0f);
		else super.playStepSound(pos, state);
	}

	/** Wears the TG armor piece {@code id} in {@code camo} (1.12 getNewWithCamo). */
	protected void wear(String id, int camo) {
		ItemStack stack = new ItemStack(BuiltInRegistries.ITEM.get(Techguns.id(id)));
		if (camo > 0) stack.set(TGComponents.CAMO, camo);
		setItemSlot(getEquipmentSlotForItem(stack), stack);
	}

	protected void wear(String id) {
		wear(id, 0);
	}

	/** Wears it half the time, as most 1.12 NPC pieces. */
	protected void maybeWear(String id, int camo) {
		if (random.nextBoolean()) wear(id, camo);
	}

	/** A random camo of the armor piece {@code id}. */
	protected int randomCamo(String id) {
		Item item = BuiltInRegistries.ITEM.get(Techguns.id(id));
		return item instanceof GenericArmor armor ? random.nextInt(armor.textures.length) : 0;
	}

	/** Holds one of {@code choices} at random. */
	protected void hold(ItemLike... choices) {
		setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(choices[random.nextInt(choices.length)]));
	}
}
