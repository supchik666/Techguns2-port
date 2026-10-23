package techguns.world.camp;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import techguns.world.dungeon.Canvas;

/** 1.12 WorldgenStructure as the military camp and the city use it: something that fills a segment of a given size. */
public abstract class Part {
	/** Size limits, -1 = none. */
	public final int minX, minY, minZ, maxX, maxY, maxZ;
	public boolean canSwapXZ;

	protected Part(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
		this.minX = minX;
		this.minY = minY;
		this.minZ = minZ;
		this.maxX = maxX;
		this.maxY = maxY;
		this.maxZ = maxZ;
	}

	public Part swapXZ() {
		canSwapXZ = true;
		return this;
	}

	/** @param direction where it points: 0 = west (-x), 1 = north (-z), 2 = east (+x), 3 = south (+z) */
	public abstract void place(Canvas c, int x, int y, int z, int sizeX, int sizeY, int sizeZ, int direction, Random rnd);

	/** 1.12 directionToFacing. */
	public static Direction facing(int direction) {
		return switch (direction) {
			case 0 -> Direction.WEST;
			case 2 -> Direction.EAST;
			case 3 -> Direction.SOUTH;
			default -> Direction.NORTH;
		};
	}

	/** A height between minY and maxY. */
	public int height(Random rand) {
		return maxY == -1 || maxY < minY ? minY : minY + rand.nextInt(maxY + 1 - minY);
	}

	/** 1.12 getMatchingStructure: a random part that fits sizeX x sizeZ, or a cobblestone floor. */
	public static Part pick(List<Part> parts, int sizeX, int sizeZ, Random rand) {
		List<Part> matching = new ArrayList<>();
		for (Part p : parts) {
			if (p.canSwapXZ ? p.fitsSwapped(sizeX, sizeZ) : p.fits(sizeX, sizeZ)) matching.add(p);
		}
		return matching.isEmpty() ? plane(Blocks.COBBLESTONE.defaultBlockState()) : matching.get(rand.nextInt(matching.size()));
	}

	private boolean fits(int sizeX, int sizeZ) {
		return sizeX >= minX && sizeZ >= minZ && (maxX == -1 || sizeX <= maxX) && (maxZ == -1 || sizeZ <= maxZ);
	}

	/** 1.12's swapped check mixes both orientations for the min and max tests, kept as is. */
	private boolean fitsSwapped(int sizeX, int sizeZ) {
		return (sizeX >= minX && sizeZ >= minZ || sizeZ >= minX && sizeX >= minZ)
				&& ((maxX == -1 || sizeX <= maxX) && (maxZ == -1 || sizeZ <= maxZ) || (maxX == -1 || sizeZ <= maxX) && (maxZ == -1 || sizeX <= maxZ));
	}

	/** 1.12 EmptyPlane: a floor of {@code state}. */
	public static Part plane(BlockState state) {
		return new Part(1, 1, 1, -1, -1, -1) {
			@Override
			public void place(Canvas c, int x, int y, int z, int sizeX, int sizeY, int sizeZ, int direction, Random rnd) {
				c.fill(x, y, z, sizeX, 1, sizeZ, state);
			}
		};
	}
}
