package techguns.machines.multiblock;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.random.Weight;
import net.minecraft.util.random.WeightedEntry;
import net.minecraft.util.random.WeightedRandom;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.registries.DeferredItem;
import techguns.TGBlocks;
import techguns.TGConfig;
import techguns.TGItems;
import techguns.TGSounds;

/**
 * 1.12 OreDrillTileEntMaster, OreDrillDefinition and TGOreClusters: a drill from the controller toward an ore cluster.
 * The smallest is the controller, one rod and the cluster behind it. Bigger ones have engines then rods in a line, a
 * square of engines {@link #radius} wide around the engine line, and a frame of frame blocks (edges) and scaffolds
 * (faces) around everything; the inside of the rod part is air. Each run mines one ore of the cluster; bigger drills
 * and better heads mine faster. Runs on furnace fuel, lava from the tank, or FE.
 */
public class OreDrillBlockEntity extends MultiBlockMachine {
	public static final int SLOT_DRILL = 0, SLOT_FUEL = 1;
	public static final int ROD = 0, ENGINE = 1, FRAME = 2, SCAFFOLD = 3;
	public static final int MAX_RADIUS = 3, MAX_SIZE = 16;

	public int engines, rods, radius;
	public Direction drillDirection = Direction.NORTH;
	public int fuelBuffer, fuelBufferMax;
	private ItemStack lastDrill = ItemStack.EMPTY;

	/** Fuel: lava, 20 buffer per mB like a lava bucket. ponytail: the output tank for oil clusters isn't ported */
	public final FluidTank tank = new FluidTank(16000, f -> f.is(FluidTags.LAVA)) {
		@Override
		protected void onContentsChanged() {
			needUpdate();
		}
	};

	public record Ore(ItemStack stack, int weight) implements WeightedEntry {
		@Override
		public Weight getWeight() {
			return Weight.of(weight);
		}
	}

	/** 1.12 OreCluster: the head level it needs, the ore and power multipliers, and the ores it gives. */
	public record Cluster(int level, double amount, double power, List<Ore> ores) {}

	private static Map<Block, Cluster> clusters;

	private static Ore ore(ItemLike item, int weight) {
		return new Ore(new ItemStack(item), weight);
	}

	/** 1.12 TGOreClusters defaults. ponytail: hard-coded, a datapack format if anyone wants to change them */
	public static Map<Block, Cluster> clusters() {
		if (clusters == null) {
			clusters = Map.of(
					TGBlocks.ORE_CLUSTER_COAL.get(), new Cluster(0, 10, 0.1, List.of(ore(Blocks.COAL_ORE, 99), ore(Items.DIAMOND, 1))),
					TGBlocks.ORE_CLUSTER_COMMON_METAL.get(), new Cluster(0, 5, 0.2,
							List.of(ore(Blocks.IRON_ORE, 45), ore(Blocks.COPPER_ORE, 30), ore(TGBlocks.ORE_TIN.get(), 25))),
					TGBlocks.ORE_CLUSTER_RARE_METAL.get(), new Cluster(1, 2.5, 0.4, List.of(ore(TGBlocks.ORE_LEAD.get(), 50))),
					TGBlocks.ORE_CLUSTER_SHINY_METAL.get(), new Cluster(2, 1, 1, List.of(ore(TGBlocks.ORE_TITANIUM.get(), 20), ore(Blocks.GOLD_ORE, 40))),
					TGBlocks.ORE_CLUSTER_URANIUM.get(), new Cluster(3, 0.5, 1, List.of(ore(TGBlocks.ORE_URANIUM.get(), 50))),
					TGBlocks.ORE_CLUSTER_COMMON_GEM.get(), new Cluster(1, 5, 0.2, List.of(ore(Blocks.REDSTONE_ORE, 40), ore(Blocks.LAPIS_ORE, 20))),
					TGBlocks.ORE_CLUSTER_SHINY_GEM.get(), new Cluster(3, 0.2, 1, List.of(ore(Blocks.DIAMOND_ORE, 40), ore(Blocks.EMERALD_ORE, 20))),
					TGBlocks.ORE_CLUSTER_NETHER_CRYSTAL.get(), new Cluster(2, 4, 0.5,
							List.of(ore(Blocks.NETHER_QUARTZ_ORE, 50), ore(Blocks.GLOWSTONE, 40), ore(Items.BLAZE_ROD, 10))),
					// ponytail: 1.12 pumped oil into the output tank; mines cobblestone until fluids are ported
					TGBlocks.ORE_CLUSTER_OIL.get(), new Cluster(2, 4, 1, List.of()));
		}
		return clusters;
	}

	public OreDrillBlockEntity(BlockPos pos, BlockState state) {
		super(MultiBlocks.ORE_DRILL.get(), pos, state, "oredrill", slots(), null, 500000, 0, 2, false);
	}

	private static int[][] slots() {
		int[][] slots = new int[11][];
		slots[SLOT_DRILL] = new int[] {30, 17};
		slots[SLOT_FUEL] = new int[] {30, 51};
		for (int i = 0; i < 9; i++) slots[2 + i] = new int[] {102 + 18 * (i % 3), 17 + 18 * (i / 3)};
		return slots;
	}

	public static final List<DeferredItem<Item>> HEADS = List.of(TGItems.OREDRILLHEAD_STEEL, TGItems.OREDRILLHEAD_OBSIDIANSTEEL,
			TGItems.OREDRILLHEAD_CARBON, TGItems.OREDRILLHEAD_MEDIUM_STEEL, TGItems.OREDRILLHEAD_MEDIUM_OBSIDIANSTEEL, TGItems.OREDRILLHEAD_MEDIUM_CARBON,
			TGItems.OREDRILLHEAD_LARGE_STEEL, TGItems.OREDRILLHEAD_LARGE_OBSIDIANSTEEL, TGItems.OREDRILLHEAD_LARGE_CARBON);

	/** Index in {@link #HEADS}: size * 3 + material, or -1. */
	private static int head(ItemStack stack) {
		for (int i = 0; i < HEADS.size(); i++) if (stack.is(HEADS.get(i))) return i;
		return -1;
	}

	@Override
	public boolean canInsert(int slot, ItemStack stack) {
		if (slot == SLOT_DRILL) return head(stack) >= 0;
		if (slot == SLOT_FUEL) return stack.getBurnTime(RecipeType.SMELTING) > 0;
		return super.canInsert(slot, stack);
	}

	@Override
	public float menuRange() {
		return 7.5f;
	}

	/** 1 for the controller and a rod, 2 for 3x3, 3 for 5x5, 4 for 7x7. */
	public int drillRadius() {
		return engines == 0 ? 0 : radius + 1;
	}

	/** The head's material level (1 steel, 2 obsidian steel, 3 carbon) if it's the size this drill takes, else 0. */
	private int headLevel() {
		int head = head(inventory.getStackInSlot(SLOT_DRILL));
		int r = drillRadius(), size = r == 0 ? 0 : r <= 2 ? 1 : 2;
		return head >= 0 && head / 3 == size ? head % 3 + 1 : 0;
	}

	@Override
	protected void start() {
		contentsChanged = false;
		BlockPos target = worldPosition.relative(drillDirection, engines + rods + 1);
		BlockState state = level.getBlockState(target);
		Cluster cluster = clusters().get(state.getBlock());
		if (cluster == null) {
			unform();
			return;
		}
		int head = headLevel();
		if (head <= 0) return;
		int eff = head + drillRadius() - cluster.level();
		List<Ore> ores = cluster.ores();
		if (eff >= 0 && !ores.isEmpty()) {
			int size = Math.min(clusterSize(target, state, engines + rods), engines + rods);
			double perHour = (size * 3 + size * eff * 0.5) * cluster.amount() * TGConfig.ORE_DRILL_ORES.get();
			ItemStack ore = WeightedRandom.getRandomItem(level.random, ores).map(o -> o.stack().copy()).orElse(new ItemStack(Blocks.COBBLESTONE));
			operation = new Operation(List.of(), List.of(ore), FluidStack.EMPTY, (int) (8 * perHour * (1 + Math.max(drillRadius() - 1, 0) * 0.2) * cluster.power() * TGConfig.ORE_DRILL_POWER.get()));
			totaltime = (int) (60 / perHour * 1200);
		} else {
			operation = new Operation(List.of(), List.of(new ItemStack(Blocks.COBBLESTONE)), FluidStack.EMPTY, (int) (24 * TGConfig.ORE_DRILL_POWER.get()));
			totaltime = 20 * 20;
		}
		progress = 0;
		needUpdate();
	}

	/** Connected blocks of the cluster's state, counting up to {@code max}. */
	private int clusterSize(BlockPos start, BlockState state, int max) {
		Set<BlockPos> seen = new HashSet<>(List.of(start));
		ArrayDeque<BlockPos> todo = new ArrayDeque<>(seen);
		while (!todo.isEmpty() && seen.size() < max) {
			BlockPos p = todo.poll();
			for (Direction d : Direction.values()) {
				BlockPos n = p.relative(d);
				if (level.getBlockState(n) == state && seen.add(n)) todo.add(n);
			}
		}
		return seen.size();
	}

	@Override
	public void tick() {
		if (!level.isClientSide && formed && operation != null && redstoneEnabled() && !checkDrill()) return;
		super.tick();
	}

	/** 1.12 checkDrill: every second, a removed or swapped head cancels the run. */
	private boolean checkDrill() {
		if (level.getGameTime() % 20 != 0) return true;
		ItemStack drill = inventory.getStackInSlot(SLOT_DRILL);
		if (lastDrill.isEmpty()) {
			lastDrill = drill.copy();
			return !drill.isEmpty();
		}
		if (drill.isEmpty() || !ItemStack.isSameItem(lastDrill, drill)) {
			lastDrill = drill.copy();
			operation = null;
			progress = totaltime = 0;
			contentsChanged = true;
			needUpdate();
			return false;
		}
		return true;
	}

	@Override
	protected boolean consumePower(int amount) {
		if (TGConfig.MACHINES_NEED_NO_POWER.getAsBoolean()) return true;
		float fePerFuel = TGConfig.ORE_DRILL_FUEL.get().floatValue();
		if (fuelBuffer * fePerFuel >= amount) {
			fuelBuffer -= (int) Math.max(amount / fePerFuel, 1);
			return true;
		}
		amount -= (int) (fuelBuffer * fePerFuel);
		fuelBuffer = fuelBufferMax = 0;
		ItemStack fuel = inventory.getStackInSlot(SLOT_FUEL);
		int burn = fuel.isEmpty() ? 0 : fuel.getBurnTime(RecipeType.SMELTING);
		if (burn > 0) {
			ItemStack remainder = fuel.getCraftingRemainingItem();
			inventory.extractItem(SLOT_FUEL, 1, false);
			if (!remainder.isEmpty() && !output(List.of(remainder), false)) dropAtController(remainder);
			fuelBuffer = fuelBufferMax = burn;
		} else if (!tank.isEmpty()) {
			fuelBuffer = fuelBufferMax = tank.drain(1000, IFluidHandler.FluidAction.EXECUTE).getAmount() * 20;
		}
		if (fuelBuffer * fePerFuel >= amount) {
			fuelBuffer -= (int) Math.max(amount / fePerFuel, 1);
			return true;
		}
		return amount == 0 || energy.consume(amount);
	}

	private void dropAtController(ItemStack stack) {
		Containers.dropItemStack(level, worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5, stack);
	}

	@Override
	protected void finish(Operation operation) {
		for (ItemStack stack : operation.outputs()) if (!output(List.of(stack), false)) dropAtController(stack.copy());
	}

	@Override
	protected void ambient() {
		if (progress % 62 != 1) return;
		SoundEvent sound = (radius == 0 ? TGSounds.ORE_DRILL_SMALL : radius <= 2 ? TGSounds.ORE_DRILL_MEDIUM : TGSounds.ORE_DRILL_LARGE).get();
		level.playLocalSound(worldPosition.relative(drillDirection, engines + rods / 2), sound, SoundSource.BLOCKS, 0.5f, 1, false);
	}

	@Override
	public void unform() {
		super.unform();
		operation = null;
		progress = totaltime = 0;
	}

	private boolean isDrillPart(BlockPos pos) {
		return isPart(pos, TGBlocks.ORE_DRILL_ROD.get()) || isPart(pos, TGBlocks.ORE_DRILL_ENGINE.get());
	}

	private boolean isCluster(BlockPos pos) {
		return clusters().containsKey(level.getBlockState(pos).getBlock());
	}

	private static Direction side1(Direction dir) {
		return dir.getAxis() == Direction.Axis.Y ? Direction.NORTH : Direction.DOWN;
	}

	private static Direction side2(Direction dir) {
		return dir.getAxis() == Direction.Axis.X ? Direction.NORTH : Direction.EAST;
	}

	/** Position {@code h} blocks along the drill (1 = next to the controller), offset {@code r1, r2} across it. */
	private BlockPos at(Direction dir, int h, int r1, int r2) {
		return worldPosition.relative(dir, h).relative(side1(dir), r1).relative(side2(dir), r2);
	}

	@Nullable
	@Override
	protected Map<BlockPos, Integer> check(Player player, Direction face) {
		Direction dir = null;
		for (Direction d : Direction.values()) {
			if (!isDrillPart(worldPosition.relative(d))) continue;
			if (dir != null) {
				ping(worldPosition.relative(d));
				return invalid(player, "techguns.msg.error.oredrill.controlleralone");
			}
			dir = d;
		}
		if (dir == null) return invalid(player, "techguns.msg.error.oredrill.controlleralone");
		Block rod = TGBlocks.ORE_DRILL_ROD.get(), engine = TGBlocks.ORE_DRILL_ENGINE.get();
		Map<BlockPos, Integer> parts = new HashMap<>();
		if (isPart(worldPosition.relative(dir), rod)) {
			if (!isCluster(worldPosition.relative(dir, 2))) {
				ping(worldPosition.relative(dir, 2));
				return null;
			}
			setShape(dir, 0, 1, 0);
			parts.put(worldPosition.relative(dir), ROD);
			return parts;
		}
		int e = 1, r = 0;
		while (e < MAX_SIZE && isPart(worldPosition.relative(dir, e + 1), engine)) e++;
		while (r < MAX_SIZE && isPart(worldPosition.relative(dir, e + r + 1), rod)) r++;
		if (r == 0 || !isCluster(worldPosition.relative(dir, e + r + 1))) {
			ping(worldPosition.relative(dir, e + r + 1));
			return null;
		}
		if (e + r > MAX_SIZE) return null;
		int rad = 0;
		Direction[] around = {side1(dir), side2(dir), side1(dir).getOpposite(), side2(dir).getOpposite()};
		for (Direction d : around) {
			int n = 0;
			while (n < MAX_RADIUS && isPart(worldPosition.relative(dir).relative(d, n + 1), engine)) n++;
			rad = Math.max(rad, n);
		}
		if (!(r >= rad * 2 + 1 && r >= e && e > rad)) return invalid(player, "techguns.msg.error.oredrill.rodsize");
		List<BlockPos> engines = new ArrayList<>(), frame = new ArrayList<>(), scaffold = new ArrayList<>(), air = new ArrayList<>(), hidden = new ArrayList<>();
		int len = e + r, big = rad + 1;
		for (int r1 = -big; r1 <= big; r1++) for (int r2 = -big; r2 <= big; r2++) for (int h = 0; h < len; h++) {
			BlockPos p = at(dir, h + 1, r1, r2);
			boolean inner = Math.abs(r1) <= rad && Math.abs(r2) <= rad, center = r1 == 0 && r2 == 0;
			int edges = (Math.abs(r1) == big ? 1 : 0) + (Math.abs(r2) == big ? 1 : 0);
			if (inner && h < e) engines.add(p);
			else if (inner && h < len - 1 && !center) air.add(p);
			else if (inner && !center) hidden.add(p);
			else if (!inner && edges + (h == 0 || h == len - 1 ? 1 : 0) >= 2) frame.add(p);
			else if (!inner && h > 0) (h == len - 1 ? hidden : scaffold).add(p);
		}
		if (!allParts(engines, engine)) return invalid(player, "techguns.multiblock.oredrill.invalid.engines");
		if (!allParts(frame, TGBlocks.ORE_DRILL_FRAME.get())) return invalid(player, "techguns.multiblock.oredrill.invalid.frame");
		if (!allParts(scaffold, TGBlocks.ORE_DRILL_SCAFFOLD.get())) return invalid(player, "techguns.multiblock.oredrill.invalid.scaffold");
		boolean airOk = true;
		for (BlockPos p : air) {
			if (!level.getBlockState(p).isAir()) {
				ping(p);
				airOk = false;
			}
		}
		if (!airOk) return invalid(player, "techguns.multiblock.oredrill.invalid.air");
		setShape(dir, e, r, rad);
		for (int h = e + 1; h <= len; h++) parts.put(worldPosition.relative(dir, h), ROD);
		engines.forEach(p -> parts.put(p, ENGINE));
		frame.forEach(p -> parts.put(p, FRAME));
		scaffold.forEach(p -> parts.put(p, SCAFFOLD));
		// ponytail: 1.12 hid the last row's scaffolds and drew a drill head there; they're linked if placed, nothing hides them
		hidden.stream().filter(p -> isPart(p, TGBlocks.ORE_DRILL_SCAFFOLD.get())).forEach(p -> parts.put(p, SCAFFOLD));
		return parts;
	}

	private void setShape(Direction dir, int engines, int rods, int radius) {
		this.drillDirection = dir;
		this.direction = dir;
		this.engines = engines;
		this.rods = rods;
		this.radius = radius;
	}

	@Nullable
	@Override
	@SuppressWarnings("unchecked")
	public <T> T partCapability(BlockCapability<T, Direction> cap, int type) {
		if (type == ROD) return null;
		if (cap == Capabilities.ItemHandler.BLOCK) return (T) automation;
		if (cap == Capabilities.EnergyStorage.BLOCK) return (T) energy;
		if (cap == Capabilities.FluidHandler.BLOCK) return (T) tank;
		return null;
	}

	@Override
	protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.saveAdditional(tag, registries);
		tag.put("tank", tank.writeToNBT(registries, new CompoundTag()));
		tag.putByte("engines", (byte) engines);
		tag.putByte("rods", (byte) rods);
		tag.putByte("radius", (byte) radius);
		tag.putByte("drillDirection", (byte) drillDirection.get3DDataValue());
		tag.putInt("fuelBuffer", fuelBuffer);
		tag.putInt("fuelBufferMax", fuelBufferMax);
	}

	@Override
	protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.loadAdditional(tag, registries);
		tank.readFromNBT(registries, tag.getCompound("tank"));
		engines = tag.getByte("engines");
		rods = tag.getByte("rods");
		radius = tag.getByte("radius");
		drillDirection = Direction.from3DDataValue(tag.getByte("drillDirection"));
		fuelBuffer = tag.getInt("fuelBuffer");
		fuelBufferMax = tag.getInt("fuelBufferMax");
	}
}
