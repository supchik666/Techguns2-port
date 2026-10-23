package techguns.world;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1.12 TGSpawnerTileEnt: every {@link #spawnDelay} ticks spawns one of {@link #mobs} (weighted) next to itself, keeping at most
 * {@link #maxActive} alive, until {@link #mobsLeft} of them died; then the spawner block disappears. Spawned mobs carry
 * {@link TGWorldgen#SPAWNER} (their spawner's position), stay within 10 blocks of it, and undead don't burn in daylight.
 * Structures set everything from NBT ({@link #loadAdditional} keys); code uses {@link #configure} and {@link #addMob}.
 */
public class TGSpawnerBlockEntity extends BlockEntity {
	public record Entry(EntityType<?> type, int weight) {}

	public final List<Entry> mobs = new ArrayList<>();
	public int mobsLeft = 5, maxActive = 3, spawnDelay = 200, spawnRange = 2, delay = 200;
	public double heightOffset;
	/** Replaces the main hand of every spawned mob when set. */
	public ItemStack weapon = ItemStack.EMPTY;
	private final List<UUID> active = new ArrayList<>();

	public TGSpawnerBlockEntity(BlockPos pos, BlockState state) {
		super(TGWorldgen.TG_SPAWNER.get(), pos, state);
	}

	/** 1.12 setParams. */
	public TGSpawnerBlockEntity configure(int mobsLeft, int maxActive, int spawnDelay, int spawnRange) {
		this.mobsLeft = mobsLeft;
		this.maxActive = maxActive;
		this.spawnDelay = this.delay = spawnDelay;
		this.spawnRange = spawnRange;
		setChanged();
		return this;
	}

	public TGSpawnerBlockEntity addMob(EntityType<?> type, int weight) {
		mobs.add(new Entry(type, weight));
		setChanged();
		return this;
	}

	public int activeCount() {
		return active.size();
	}

	public static void serverTick(Level level, BlockPos pos, BlockState state, TGSpawnerBlockEntity be) {
		if (--be.delay > 0) return;
		be.delay = be.spawnDelay;
		ServerLevel server = (ServerLevel) level;
		// ponytail: mobs in unloaded chunks are forgotten too, so the spawner may refill while they're away
		be.active.removeIf(id -> !(server.getEntity(id) instanceof LivingEntity e) || !e.isAlive());
		if (be.active.size() < Math.min(be.maxActive, be.mobsLeft) && !be.mobs.isEmpty() && level.getDifficulty() != Difficulty.PEACEFUL) be.spawn(server);
		be.setChanged();
	}

	private void spawn(ServerLevel level) {
		var random = level.random;
		int roll = random.nextInt(mobs.stream().mapToInt(Entry::weight).sum());
		Entry entry = mobs.getFirst();
		for (Entry e : mobs) if ((roll -= e.weight) < 0) { entry = e; break; }
		if (!(entry.type.create(level) instanceof Mob mob)) return;
		BlockPos pos = getBlockPos();
		mob.moveTo(pos.getX() + 0.5 + (random.nextDouble() - random.nextDouble()) * spawnRange, pos.getY() + 1 + heightOffset,
				pos.getZ() + 0.5 + (random.nextDouble() - random.nextDouble()) * spawnRange, random.nextFloat() * 360, 0);
		mob.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.SPAWNER, null);
		if (!weapon.isEmpty()) mob.setItemSlot(EquipmentSlot.MAINHAND, weapon.copy());
		// ponytail: the 10 block leash isn't saved, so mobs reloaded from disk roam freely (1.12 didn't save it either)
		if (mob instanceof PathfinderMob pathfinder) pathfinder.restrictTo(pos, 10);
		mob.setData(TGWorldgen.SPAWNER, pos);
		level.addFreshEntityWithPassengers(mob);
		level.levelEvent(2004, pos, 0);
		mob.spawnAnim();
		active.add(mob.getUUID());
	}

	/** A mob of this spawner died: one less to go, and the spawner goes when none are left. */
	void killed(LivingEntity mob) {
		active.remove(mob.getUUID());
		if (--mobsLeft <= 0) level.removeBlock(getBlockPos(), false);
		else setChanged();
	}

	@Override
	protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.saveAdditional(tag, registries);
		tag.putInt("mobsLeft", mobsLeft);
		tag.putInt("maxActive", maxActive);
		tag.putInt("spawnDelay", spawnDelay);
		tag.putInt("spawnRange", spawnRange);
		tag.putInt("delay", delay);
		tag.putDouble("spawnHeightOffset", heightOffset);
		ListTag list = new ListTag();
		for (Entry e : mobs) {
			CompoundTag mob = new CompoundTag();
			mob.putString("id", EntityType.getKey(e.type).toString());
			mob.putInt("weight", e.weight);
			list.add(mob);
		}
		tag.put("mobtypes", list);
		if (!weapon.isEmpty()) tag.put("weapon", weapon.save(registries));
		ListTag ids = new ListTag();
		for (UUID id : active) ids.add(NbtUtils.createUUID(id));
		tag.put("active", ids);
	}

	@Override
	protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.loadAdditional(tag, registries);
		if (tag.contains("mobsLeft")) mobsLeft = tag.getInt("mobsLeft");
		if (tag.contains("maxActive")) maxActive = tag.getInt("maxActive");
		if (tag.contains("spawnDelay")) spawnDelay = tag.getInt("spawnDelay");
		if (tag.contains("spawnRange")) spawnRange = tag.getInt("spawnRange");
		delay = tag.contains("delay") ? tag.getInt("delay") : spawnDelay;
		heightOffset = tag.getDouble("spawnHeightOffset");
		mobs.clear();
		for (Tag t : tag.getList("mobtypes", Tag.TAG_COMPOUND)) {
			CompoundTag mob = (CompoundTag) t;
			BuiltInRegistries.ENTITY_TYPE.getOptional(ResourceLocation.parse(mob.getString("id")))
					.ifPresent(type -> mobs.add(new Entry(type, Math.max(1, mob.getInt("weight")))));
		}
		weapon = ItemStack.parseOptional(registries, tag.getCompound("weapon"));
		active.clear();
		for (Tag t : tag.getList("active", Tag.TAG_INT_ARRAY)) active.add(NbtUtils.loadUUID(t));
	}
}
