package techguns.world.camp;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import techguns.TGBlocks;
import techguns.TGEntities;
import techguns.entities.npcs.TGNpcs;
import techguns.world.camp.CampParts.Barracks;
import techguns.world.camp.CampParts.Bunker;
import techguns.world.camp.CampParts.CampProps;
import techguns.world.camp.CampParts.Containers;
import techguns.world.camp.CampParts.Helipad;
import techguns.world.camp.CampParts.Tanks;
import techguns.world.camp.CampParts.Tent;
import techguns.world.camp.CampParts.WatchTower;
import techguns.world.dungeon.Canvas;

/**
 * 1.12 MilitaryCamp: a fenced area split recursively into segments with roads between them; each leaf segment gets a
 * building, the central one a helicopter spawner, every road a soldier spawner. The ground is flat at posY.
 */
public class MilitaryCamp {
	static final int FENCE_WIDTH = 1, FENCE_HEIGHT = 2, FENCE_SPACING = 1, ROAD_MAX_WIDTH = 5, ROAD_MIN_WIDTH = 1;
	static final int MIN_BORDER_SIZE = 30, MAX_BORDER_SIZE = 80, MIN_CORNER_SIZE = 20, MAX_CORNER_SIZE = 60;

	static final List<Part> STRUCTURES = List.of(
			new Tent(5, 4, 5, 7, 5, 7, 0),
			new Tent(3, 4, 3, 6, 5, 6, 1),
			new Tent(4, 4, 4, 8, 6, 8, 2),
			new Containers(2, 3, 2, 7, 3, 7),
			new CampProps(1, 3, 1, 5, 5, 5, 0), //crates
			new CampProps(3, 5, 3, 6, 7, 6, 1), //crates + roof
			new Bunker(5, 3, 5, 8, 3, 8, 0), //no roof
			new Barracks(7, 7, 7, -1, -1, -1),
			new Helipad(7, 7, 7, 10, -1, 10),
			new Tanks(4, 5, 4, 10, -1, 10),
			new CampProps(3, 5, 3, 10, 7, 10, 5), //crates + roof + sandbags
			new Bunker(3, 3, 5, 15, 3, 15, 2).swapXZ(), //closed bunker
			new Bunker(4, 3, 6, 15, 3, 15, 3).swapXZ()); //closed bunker with sandbag roof
	static final List<Part> BORDER_STRUCTURES = List.of(
			new Bunker(5, 3, 5, 8, 3, 8, 0),
			new Bunker(5, 4, 5, -1, 4, -1, 1), //roof
			new CampProps(3, 2, 3, 8, 2, 8, 2), //sandbags
			new CampProps(3, 4, 3, 8, 5, 8, 3), //sandbags + roof
			new CampProps(1, 2, 3, -1, 4, -1, 4).swapXZ(), //row of sandbags
			new Bunker(3, 3, 5, 10, 3, 10, 2).swapXZ(),
			new Bunker(4, 3, 6, 10, 3, 10, 3).swapXZ());
	static final List<Part> CORNER_STRUCTURES = List.of(
			new WatchTower(3, 8, 3, 4, 8, -1, 3).swapXZ(),
			new WatchTower(4, 10, 4, -1, 12, -1, 4));

	final int posX, posY, posZ, sizeX, sizeZ;
	final Random rand;
	final float borderChanceX, borderChanceZ, cornerChance;
	final int minSubSegmentSize, maxSubSegmentSize;
	final double campRadiusSquared;
	final Segment rootSegment;
	Segment flagSegment;
	final List<int[]> spawnPositions = new ArrayList<>();

	/** 1.12 init: lays out the segments. posY is the ground block's height. */
	public MilitaryCamp(Random rand, int posX, int posY, int posZ, int sizeX, int sizeZ) {
		this.rand = rand;
		this.posX = posX;
		this.posY = posY;
		this.posZ = posZ;
		this.sizeX = sizeX;
		this.sizeZ = sizeZ;
		campRadiusSquared = sizeX / 2.0 * (sizeX / 2.0) + sizeZ / 2.0 * (sizeZ / 2.0);
		int border = FENCE_WIDTH + FENCE_SPACING;
		int roadWidth = Math.max(ROAD_MIN_WIDTH, Math.min(ROAD_MAX_WIDTH, Math.max(sizeX, sizeZ) / 8));
		borderChanceX = (float) (sizeX - MIN_BORDER_SIZE) / (MAX_BORDER_SIZE - MIN_BORDER_SIZE);
		borderChanceZ = (float) (sizeZ - MIN_BORDER_SIZE) / (MAX_BORDER_SIZE - MIN_BORDER_SIZE);
		cornerChance = (float) ((sizeX + sizeZ) / 2 - MIN_CORNER_SIZE) / (MAX_CORNER_SIZE - MIN_CORNER_SIZE);
		minSubSegmentSize = Math.min(sizeX, sizeZ) < 10 ? 5 : 7;
		maxSubSegmentSize = Math.min(sizeX, sizeZ) <= 45 ? 15 : Math.min(sizeX, sizeZ) / 3;
		rootSegment = new Segment(posX + border, posZ + border, sizeX - 2 * border, sizeZ - 2 * border, roadWidth, 0);
		rootSegment.isCenter = true;
		rootSegment.generateSubSegments();
	}

	/** 1.12 setBlocks. */
	public void draw(Canvas c) {
		int border = FENCE_WIDTH + FENCE_SPACING;
		if (sizeX < border * 2 + 1 || sizeZ < border * 2 + 1) return;
		BlockState fence = Blocks.IRON_BARS.defaultBlockState();
		for (int k = 1; k < FENCE_HEIGHT + 1; k++) {
			for (int i = 0; i < sizeX; i++) {
				c.set(posX + i, posY + k, posZ, fence);
				c.set(posX + i, posY + k, posZ + sizeZ - 1, fence);
			}
			for (int j = 1; j < sizeZ - 1; j++) {
				c.set(posX, posY + k, posZ + j, fence);
				c.set(posX + sizeX - 1, posY + k, posZ + j, fence);
			}
		}
		rootSegment.draw(c);
		if (flagSegment == null) return;
		//1.12 initCampFlagEntity
		// ponytail: 1.12 spawned the helicopter 64 blocks up; the vanilla spawner puts it next to the block
		c.spawner(flagSegment.x + flagSegment.sizeX / 2, posY + 1, flagSegment.z + flagSegment.sizeZ / 2, 1, 1, TGEntities.ATTACK_HELICOPTER.get());
		for (int[] p : spawnPositions) {
			c.spawner(p[0], p[1], p[2], 3, 1, TGNpcs.ARMY_SOLDIER.get());
		}
	}

	void road(Canvas c, int x, int z, int roadWidth, int roadLength, boolean alongX) {
		BlockState gravel = Blocks.GRAVEL.defaultBlockState(), concrete = TGBlocks.CONCRETE_GREY.get().defaultBlockState();
		for (int a = 0; a < roadLength; a++) {
			for (int b = 0; b < roadWidth; b++) {
				BlockState block = roadWidth >= 4 && b > 0 && b < roadWidth - 1 ? concrete : gravel;
				if (alongX) c.set(x + a, posY, z + b, block);
				else c.set(x + b, posY, z + a, block);
			}
		}
		spawnPositions.add(alongX ? new int[]{x + roadLength / 2, posY + 1, z + roadWidth / 2} : new int[]{x + roadWidth / 2, posY + 1, z + roadLength / 2});
	}

	/** Cuts the fence where a road leaves the camp. */
	void gate(Canvas c, int x, int z, int roadWidth, int length, boolean alongX) {
		for (int a = 0; a < length; a++) {
			for (int b = 0; b < roadWidth; b++) {
				for (int k = 1; k <= FENCE_HEIGHT; k++) {
					int px = alongX ? x + a : x + b, pz = alongX ? z + b : z + a;
					if (c.get(px, posY + k, pz).is(Blocks.IRON_BARS)) c.air(px, posY + k, pz);
				}
			}
		}
	}

	class Segment {
		final int x, z, sizeX, sizeZ, roadWidth;
		/** 0 = n, 1 = e, 2 = s, 3 = w. */
		final int direction;
		boolean isCenter;
		int splitStep;
		Segment[] subSegments;
		/** false = split along x (roads run along z), true = split along z. */
		boolean subAlignment;

		Segment(int x, int z, int sizeX, int sizeZ, int roadWidth, int direction) {
			this.x = x;
			this.z = z;
			this.sizeX = sizeX;
			this.sizeZ = sizeZ;
			this.roadWidth = Math.max(ROAD_MIN_WIDTH, roadWidth);
			this.direction = direction;
		}

		void draw(Canvas c) {
			if (subSegments != null && subSegments.length >= 2) {
				for (int i = 0; i < subSegments.length; i++) {
					Segment s = subSegments[i];
					s.draw(c);
					if (i == subSegments.length - 1) continue;
					if (this == rootSegment) { //main road, through the fence
						int extra = FENCE_WIDTH + FENCE_SPACING + 1;
						if (subAlignment) {
							road(c, x - extra, s.z + s.sizeZ, roadWidth, sizeX + 2 * extra, true);
							gate(c, x - (FENCE_SPACING + FENCE_WIDTH), s.z + s.sizeZ, roadWidth, FENCE_WIDTH, true);
							gate(c, x + sizeX + FENCE_SPACING, s.z + s.sizeZ, roadWidth, FENCE_WIDTH, true);
						} else {
							road(c, s.x + s.sizeX, z - extra, roadWidth, sizeZ + 2 * extra, false);
							gate(c, s.x + s.sizeX, z - (FENCE_SPACING + FENCE_WIDTH), roadWidth, FENCE_WIDTH, false);
							gate(c, s.x + s.sizeX, z + sizeZ + FENCE_SPACING, roadWidth, FENCE_WIDTH, false);
						}
					} else if (subAlignment) {
						road(c, x - 1, s.z + s.sizeZ, roadWidth, sizeX + 2, true);
					} else {
						road(c, s.x + s.sizeX, z - 1, roadWidth, sizeZ + 2, false);
					}
				}
			} else if (isCenter && flagSegment == null) {
				flagSegment = this;
			} else {
				List<Part> list = shouldPlaceCornerStructure() ? CORNER_STRUCTURES : shouldPlaceBorderStructure() ? BORDER_STRUCTURES : STRUCTURES;
				Part part = Part.pick(list, sizeX, sizeZ, rand);
				part.place(c, x, posY, z, sizeX, part.height(rand), sizeZ, direction, rand);
			}
		}

		void generateSubSegments() {
			subAlignment = sizeX < sizeZ;
			int size = subAlignment ? sizeZ : sizeX;
			if (size <= 0) return;
			double s = Math.sqrt((double) (size - minSubSegmentSize) / (maxSubSegmentSize - minSubSegmentSize));
			if (size < minSubSegmentSize || size < maxSubSegmentSize && rand.nextFloat() > s * 0.75f + getCenterFactor() * 0.25f) return;
			int count = 2;
			if (this != rootSegment && rand.nextFloat() >= 0.5) {
				count = Math.max(2, Math.round(1.0f + rand.nextFloat() * 0.5f * ((size - (minSubSegmentSize + 1) / 2.0f) / (roadWidth + 2))));
			}
			double[] parts = new double[count];
			double sum = 0;
			for (int i = 0; i < count; i++) {
				parts[i] = 1.0 + rand.nextFloat();
				sum += parts[i];
			}
			int sizeTotal = 0;
			subSegments = new Segment[count];
			for (int i = 0; i < count; i++) {
				boolean last = i >= count - 1;
				int splitSize = last ? size - sizeTotal : (int) Math.round(parts[i] / sum * (size - roadWidth * count));
				int dir = last ? (subAlignment ? 1 : 0) : (subAlignment ? 3 : 2);
				subSegments[i] = subAlignment ? new Segment(x, z + sizeTotal, sizeX, splitSize, roadWidth - 1, dir)
						: new Segment(x + sizeTotal, z, splitSize, sizeZ, roadWidth - 1, dir);
				sizeTotal += splitSize + roadWidth;
				if (isCenter && (splitStep <= 1 && i == (count - 1) / 2 || i == count - 1)) subSegments[i].isCenter = true;
				subSegments[i].splitStep = splitStep + 1;
				subSegments[i].generateSubSegments();
			}
		}

		float getCenterFactor() {
			double dx = posX + MilitaryCamp.this.sizeX / 2.0 - (x + sizeX / 2.0), dz = posZ + MilitaryCamp.this.sizeZ / 2.0 - (z + sizeZ / 2.0);
			return 1.0f - (float) ((dx * dx + dz * dz) / campRadiusSquared);
		}

		boolean shouldPlaceBorderStructure() {
			int border = FENCE_WIDTH + FENCE_SPACING;
			boolean isBorderX = x <= posX + border || x + sizeX >= posX + MilitaryCamp.this.sizeX - border;
			boolean isBorderZ = z <= posZ + border || z + sizeZ >= posZ + MilitaryCamp.this.sizeZ - border;
			return isBorderX && borderChanceX > rand.nextFloat() || isBorderZ && borderChanceZ > rand.nextFloat();
		}

		boolean shouldPlaceCornerStructure() {
			int border = FENCE_WIDTH + FENCE_SPACING;
			boolean minX = x <= posX + border, maxX = x + sizeX >= posX + MilitaryCamp.this.sizeX - border;
			boolean minZ = z <= posZ + border, maxZ = z + sizeZ >= posZ + MilitaryCamp.this.sizeZ - border;
			return (minX || maxX) && (minZ || maxZ) && cornerChance > rand.nextFloat();
		}
	}
}
