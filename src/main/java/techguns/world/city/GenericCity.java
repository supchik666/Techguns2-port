package techguns.world.city;

import java.util.List;
import java.util.Random;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import techguns.TGBlocks;
import techguns.world.camp.Part;
import techguns.world.dungeon.Canvas;

/**
 * 1.12 GenericCity.testCity with GenericCitySegment and CityRoad: the area is split recursively into blocks with
 * roads between them, each block gets a building. The ground is flat at posY (1.12 used maxHeightDiff -1 and let the
 * roads ramp; flat ground makes every ramp zero).
 */
public class GenericCity {
	static final int SET_AIR_HEIGHT = 3;
	static final int[] ROAD_WIDTH = {6, 4, 4, 2, 1};
	static final float FUZZY_BORDER = 0.5f;
	static final List<Part> STRUCTURES = List.of(
			new CityBuildingType.Building(CityBuildingType.BUILDING1, 3, 5, 5, -1, 16, -1).swapXZ(),
			new CityBuildingType.Building(CityBuildingType.BUILDING2, 4, 5, 4, -1, 16, -1),
			new CityBuildingType.Building(CityBuildingType.BUILDING3, 4, 5, 4, -1, 16, -1),
			new CityBuildingType.Building(CityBuildingType.BUILDING4, 4, 5, 4, -1, 16, -1),
			new CityBuildingType.Building(CityBuildingType.BUILDING5, 4, 5, 4, -1, 16, -1),
			new CityBuildingType.Building(CityBuildingType.BUILDING6, 4, 5, 4, -1, 16, -1));

	final Random rand;
	final int posY;
	final int minSubSegmentSize, maxSubSegmentSize;
	final Segment root;

	/** 1.12 createCity up to the segment layout. */
	public GenericCity(Random rand, int posX, int posY, int posZ, int sizeX, int sizeZ) {
		this.rand = rand;
		this.posY = posY;
		minSubSegmentSize = Math.min(sizeX, sizeZ) < 14 ? 7 : 10;
		maxSubSegmentSize = Math.min(sizeX, sizeZ) <= 45 ? 15 : Math.min(sizeX, sizeZ) / 3;
		root = new Segment(null, posX, posZ, sizeX, sizeZ, ROAD_WIDTH[0], 0);
		root.isCenter = true;
		root.generateSubSegments();
	}

	public void draw(Canvas c) {
		root.draw(c);
	}

	/** 1.12 CityRoad.setRoadBlocks with no height difference. */
	void road(Canvas c, int x, int z, int roadWidth, int roadLength, boolean alongX) {
		BlockState main = TGBlocks.CONCRETE_GREY_DARK.get().defaultBlockState(), side = TGBlocks.CONCRETE_GREY.get().defaultBlockState();
		for (int a = -1; a < roadLength + 2; a++) {
			for (int b = 0; b < roadWidth; b++) {
				BlockState block = roadWidth >= 4 && b > 0 && b < roadWidth - 1 ? main : side;
				int px = alongX ? x + a : x + b, pz = alongX ? z + b : z + a;
				for (int i = -1; i < 1; i++) {
					if (c.get(px, posY + i, pz) != main) c.set(px, posY + i, pz, block);
				}
				for (int i = 1; i < (alongX ? 4 : SET_AIR_HEIGHT); i++) c.air(px, posY + i, pz);
			}
		}
	}

	class Segment {
		final Segment rootSegment;
		final int x, z, sizeX, sizeZ, roadWidth, direction;
		final float rootRadiusSquared;
		boolean isCenter;
		int splitStep;
		Segment[] subSegments;
		boolean subAlignment;

		Segment(Segment root, int x, int z, int sizeX, int sizeZ, int roadWidth, int direction) {
			this.rootSegment = root == null ? this : root;
			this.x = x;
			this.z = z;
			this.sizeX = sizeX;
			this.sizeZ = sizeZ;
			this.roadWidth = roadWidth;
			this.direction = direction;
			rootRadiusSquared = sizeX / 2.0f * (sizeX / 2.0f) + sizeZ / 2.0f * (sizeZ / 2.0f);
		}

		void draw(Canvas c) {
			if (subSegments == null || subSegments.length < 2) {
				placeStructure(c);
				return;
			}
			for (int i = 0; i < subSegments.length; i++) {
				Segment s = subSegments[i];
				s.draw(c);
				if (i == subSegments.length - 1) continue;
				if (subAlignment) road(c, x, s.z + s.sizeZ, roadWidth, sizeX, true);
				else road(c, s.x + s.sizeX, z, roadWidth, sizeZ, false);
			}
		}

		/** 1.12 GenericCity.placeStructure. */
		void placeStructure(Canvas c) {
			if (isBorderSegment() && splitStep > 2 && rand.nextFloat() < FUZZY_BORDER) return;
			c.fill(x, posY + 1, z, sizeX, SET_AIR_HEIGHT, sizeZ, Blocks.AIR.defaultBlockState());
			Part part = Part.pick(STRUCTURES, sizeX, sizeZ, rand);
			part.place(c, x, posY, z, sizeX, part.height(rand), sizeZ, direction, rand);
		}

		void generateSubSegments() {
			subAlignment = sizeX < sizeZ;
			int size = subAlignment ? sizeZ : sizeX, size2 = subAlignment ? sizeX : sizeZ;
			if (size <= 0) return;
			//always split long segments
			double s = Math.sqrt((double) (size - minSubSegmentSize) / (maxSubSegmentSize - minSubSegmentSize));
			if (size2 < minSubSegmentSize || size < maxSubSegmentSize && rand.nextFloat() > s * 0.75f + getCenterFactor() * 0.25f) return;
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
			int nextRoadWidth = ROAD_WIDTH[Math.min(ROAD_WIDTH.length - 1, splitStep + 1)];
			for (int i = 0; i < count; i++) {
				boolean last = i >= count - 1;
				int splitSize = last ? size - sizeTotal : (int) Math.round(parts[i] / sum * (size - roadWidth * count));
				int dir = last ? (subAlignment ? 1 : 0) : (subAlignment ? 3 : 2);
				subSegments[i] = subAlignment ? new Segment(rootSegment, x, z + sizeTotal, sizeX, splitSize, nextRoadWidth, dir)
						: new Segment(rootSegment, x + sizeTotal, z, splitSize, sizeZ, nextRoadWidth, dir);
				sizeTotal += splitSize + roadWidth;
				if (isCenter && (splitStep <= 1 && i == (count - 1) / 2 || i == count - 1)) subSegments[i].isCenter = true;
				subSegments[i].splitStep = splitStep + 1;
				subSegments[i].generateSubSegments();
			}
		}

		float getCenterFactor() {
			float dx = rootSegment.x + rootSegment.sizeX / 2.0f - (x + sizeX / 2.0f), dz = rootSegment.z + rootSegment.sizeZ / 2.0f - (z + sizeZ / 2.0f);
			return 1.0f - (dx * dx + dz * dz) / rootSegment.rootRadiusSquared;
		}

		boolean isBorderSegment() {
			return x <= rootSegment.x || x + sizeX >= rootSegment.x + rootSegment.sizeX
					|| z <= rootSegment.z || z + sizeZ >= rootSegment.z + rootSegment.sizeZ;
		}
	}
}
