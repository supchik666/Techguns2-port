package techguns.machines;

import java.util.UUID;

import com.mojang.authlib.GameProfile;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.level.BlockEvent;
import techguns.TGBlockEntities;
import techguns.TGEffects;
import techguns.TGSounds;
import techguns.blocks.ExplosiveChargeBlock;

/**
 * 1.12 ExplosiveChargeTileEnt and ExplosiveChargeAdvTileEnt: once armed from its GUI, counts down its fuse, then breaks
 * the blocks behind it, a (radius / 2) square wide and {@code length} deep, each line stopping at the first block it can't
 * break. No explosion: 1.12 drops the blocks one by one, so there's no TGExplosion and nothing for safe mode to stop.
 */
public class ExplosiveChargeBlockEntity extends TGBlockEntity {
	/** 3 arms it, 4/5 radius +/-, 6/7 length +/-, 8/9 fuse +/- 1 s. */
	public static final int BUTTON_DETONATE = 3, BUTTON_RADIUS = 4, BUTTON_LENGTH = 6, BUTTON_FUSE = 8;
	/** 1.12 TGConfig explosiveChargeMaxBlockHardness / explosiveChargeAdvancedMaxBlockHardness. ponytail: not configurable. */
	private static final float MAX_HARDNESS = 30, MAX_HARDNESS_ADVANCED = 100;
	private static final GameProfile FAKE_PLAYER = new GameProfile(UUID.fromString("5b0d9c5e-3b52-4d8e-9c7f-6a8b1f3c2e10"), "[Techguns charge]");

	public final boolean advanced;
	private final int maxRadius, minRadius = 3, minLength = 2, maxLength, minFuse = 60, maxFuse = 200;
	public int radius, length, fuse = 100;
	public boolean armed;

	public ExplosiveChargeBlockEntity(BlockPos pos, BlockState state) {
		super(TGBlockEntities.EXPLOSIVE_CHARGE.get(), pos, state, "explosivecharge", new int[0][]);
		advanced = ((ExplosiveChargeBlock) state.getBlock()).advanced;
		maxRadius = advanced ? 7 : 3;
		maxLength = advanced ? 10 : 5;
		radius = advanced ? 5 : 3;
		length = advanced ? 10 : 3;
	}

	@Override
	public boolean playerInventory() {
		return false;
	}

	@Override
	public void buttonClicked(int id, Player player) {
		if (armed) return;
		switch (id) {
			case BUTTON_DETONATE -> arm();
			case BUTTON_RADIUS -> radius = Math.min(radius + 1, maxRadius);
			case BUTTON_RADIUS + 1 -> radius = Math.max(radius - 1, minRadius);
			case BUTTON_LENGTH -> length = Math.min(length + 1, maxLength);
			case BUTTON_LENGTH + 1 -> length = Math.max(length - 1, minLength);
			case BUTTON_FUSE -> fuse = Math.min(fuse + 20, maxFuse);
			case BUTTON_FUSE + 1 -> fuse = Math.max(fuse - 20, minFuse);
			default -> super.buttonClicked(id, player);
		}
		needUpdate();
	}

	public void arm() {
		armed = true;
		level.playSound(null, worldPosition, sound(TGSounds.TNT_INIT.get(), TGSounds.C4_INIT.get()), SoundSource.BLOCKS, 4, 1);
		level.setBlock(worldPosition, getBlockState().setValue(ExplosiveChargeBlock.ARMED, true), 3);
	}

	private SoundEvent sound(SoundEvent tnt, SoundEvent c4) {
		return advanced ? c4 : tnt;
	}

	@Override
	public void tick() {
		if (!armed || level.isClientSide) return;
		fuse--;
		if (fuse > 0 && fuse % 20 == 0) level.playSound(null, worldPosition, sound(TGSounds.TNT_TICK.get(), TGSounds.C4_TICK.get()), SoundSource.BLOCKS, 4, 1);
		if (fuse <= 0) explode((ServerLevel) level);
	}

	private void explode(ServerLevel level) {
		level.playSound(null, worldPosition, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.BLOCKS, 4,
				(1 + (level.random.nextFloat() - level.random.nextFloat()) * 0.2f) * 0.7f);
		Direction facing = getBlockState().getValue(ExplosiveChargeBlock.FACING);
		Direction side1 = facing.getAxis() == Direction.Axis.Z ? Direction.EAST : Direction.NORTH;
		Direction side2 = facing.getAxis() == Direction.Axis.Y ? Direction.EAST : Direction.UP;
		BlockPos pos = worldPosition;
		level.removeBlock(pos, false);
		// ponytail: 1.12 skipped breaking when the owner was offline; a fake player stands in, so protection mods still get a say
		Player player = owner != null ? level.getPlayerByUUID(owner) : null;
		if (player == null) player = FakePlayerFactory.get(level, FAKE_PLAYER);
		int rad = radius / 2;
		for (BlockPos start : BlockPos.betweenClosed(pos.relative(side1, rad).relative(side2, rad), pos.relative(side1, -rad).relative(side2, -rad))) {
			BlockPos.MutableBlockPos p = start.mutable();
			for (int i = 0; i < length; i++) {
				if (!breakBlock(level, p.move(facing), player)) break;
			}
		}
	}

	/** False if the block stops the blast. */
	private boolean breakBlock(ServerLevel level, BlockPos pos, Player player) {
		BlockState state = level.getBlockState(pos);
		float hardness = state.getDestroySpeed(level, pos);
		if (hardness < 0 || (advanced ? hardness > MAX_HARDNESS_ADVANCED : hardness >= MAX_HARDNESS)) return false;
		if (NeoForge.EVENT_BUS.post(new BlockEvent.BreakEvent(level, pos, state, player)).isCanceled()) return false;
		Block.dropResources(state, level, pos, level.getBlockEntity(pos));
		level.removeBlock(pos, false);
		if (level.random.nextFloat() > 0.5f) TGEffects.sendFX(level, "MiningChargeBlockExplosion", pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 1, 50);
		return true;
	}

	@Override
	protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.saveAdditional(tag, registries);
		tag.putShort("blastradius", (short) radius);
		tag.putShort("blastlength", (short) length);
		tag.putShort("fusetime", (short) fuse);
		tag.putBoolean("armed", armed);
	}

	@Override
	protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.loadAdditional(tag, registries);
		radius = tag.getShort("blastradius");
		length = tag.getShort("blastlength");
		fuse = tag.getShort("fusetime");
		armed = tag.getBoolean("armed");
	}
}
