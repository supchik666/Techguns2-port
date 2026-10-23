package techguns.machines.multiblock;

import java.util.Collection;
import java.util.Map;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.BlockCapability;
import techguns.TGEffects;
import techguns.machines.MachineBlock;
import techguns.machines.MachineBlockEntity;
import techguns.recipes.MachineRecipe;

/**
 * The controller of a multiblock machine (1.12 MultiBlockMachineTileEntMaster plus its MultiBlockMachineSchematic): forms
 * from the parts around it when clicked, runs only while formed, and unforms when any part (or itself) is broken.
 * Remembers the parts it linked, to unlink exactly those.
 */
public abstract class MultiBlockMachine extends MachineBlockEntity {
	public boolean formed;
	/** Which way the structure extends from the controller. */
	public Direction direction = Direction.NORTH;
	private long[] parts = new long[0];

	protected MultiBlockMachine(BlockEntityType<?> type, BlockPos pos, BlockState state, String name, int[][] slots, MachineRecipe.Kind kind,
			int capacity, int defaultPower, int inputs, boolean upgrade) {
		super(type, pos, state, name, slots, kind, capacity, defaultPower, inputs, false, upgrade);
	}

	/**
	 * The parts, with their type, of the structure built for a click on {@code face}; sets {@link #direction} and whatever
	 * else the machine needs. Null if it isn't built right, after pinging the wrong blocks and telling {@code player} why.
	 */
	@Nullable
	protected abstract Map<BlockPos, Integer> check(Player player, Direction face);

	/** What a formed part of {@code type} gives for {@code cap}, or null. */
	@Nullable
	public <T> T partCapability(BlockCapability<T, Direction> cap, int type) {
		return null;
	}

	public void tryForm(Player player, Direction face) {
		Map<BlockPos, Integer> found = check(player, face);
		if (found == null) return;
		formed = true;
		parts = found.keySet().stream().mapToLong(BlockPos::asLong).toArray();
		found.forEach((pos, type) -> {
			if (level.getBlockEntity(pos) instanceof MultiBlockSlave part) part.link(worldPosition, type);
		});
		// 1.12 getActualState: the formed model faces the structure, not the way the controller was placed (ore drills can point down)
		BlockState state = getBlockState().setValue(MultiBlockPart.FORMED, true);
		level.setBlock(worldPosition, direction.getAxis().isHorizontal() ? state.setValue(MachineBlock.FACING, direction) : state, 3);
		contentsChanged = true;
		needUpdate();
	}

	public void unform() {
		if (!formed) return;
		formed = false;
		for (long pos : parts) {
			if (level.getBlockEntity(BlockPos.of(pos)) instanceof MultiBlockSlave part && part.isPartOf(worldPosition)) part.unlink();
		}
		parts = new long[0];
		BlockState state = level.getBlockState(worldPosition);
		if (state.getBlock() instanceof MultiBlockController) level.setBlock(worldPosition, state.setValue(MultiBlockPart.FORMED, false), 3);
		needUpdate();
	}

	@Override
	public void tick() {
		if (formed) super.tick();
	}

	/** Whether {@code pos} holds an unformed {@code block}. */
	protected boolean isPart(BlockPos pos, Block block) {
		BlockState state = level.getBlockState(pos);
		return state.is(block) && !state.getValue(MultiBlockPart.FORMED);
	}

	/** Whether all {@code positions} hold an unformed {@code block}; pings the ones that don't. */
	protected boolean allParts(Collection<BlockPos> positions, Block block) {
		boolean valid = true;
		for (BlockPos pos : positions) {
			if (!isPart(pos, block)) {
				ping(pos);
				valid = false;
			}
		}
		return valid;
	}

	/** The red marker 1.12 showed on a block that's in the way. */
	protected void ping(BlockPos pos) {
		TGEffects.sendFX((ServerLevel) level, "MultiblockInvalidPing", pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 1, 32);
	}

	/** Shows {@code key} over the player's hotbar and returns null, for {@link #check}. */
	@Nullable
	protected static Map<BlockPos, Integer> invalid(Player player, String key) {
		player.displayClientMessage(Component.translatable(key), true);
		return null;
	}

	@Override
	protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.saveAdditional(tag, registries);
		tag.putBoolean("formed", formed);
		tag.putByte("direction", (byte) direction.get3DDataValue());
		tag.putLongArray("parts", parts);
	}

	@Override
	protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.loadAdditional(tag, registries);
		formed = tag.getBoolean("formed");
		direction = Direction.from3DDataValue(tag.getByte("direction"));
		parts = tag.getLongArray("parts");
	}
}
