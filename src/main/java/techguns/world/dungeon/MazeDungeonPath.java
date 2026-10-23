package techguns.world.dungeon;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.util.RandomSource;
import techguns.world.dungeon.DungeonTemplate.Type;

/**
 * The 1.12 maze generator, ported as is: grows a path of segments through an sX*sY*sZ grid (ramps, rooms, forks),
 * then matches every cell's connection pattern to a segment type. Output: {@link #placements} and {@link #spawners}.
 */
public class MazeDungeonPath {
	private final RandomSource rand;
	private PathSegment[][][] volume;
	private final List<PathSegment> list = new ArrayList<>();
	private int nextRoomID = 1;
	private final int sX, sZ;
	private int sY;

	int startHeightLevel = 0;
	float chanceStraight = 0.5f, chanceRamp = 0.25f, chanceRoom = 0.25f, chanceFork = 0.2f, chanceUp = 0.5f;
	int minRoomArea = 4, maxRoomArea = 16, minRoomWidth = 2, maxRoomWidth = 5;
	boolean usePillars = true, useFoundations = true, useRoof = false, useBottomLayer = false;
	int entranceRampLength = 0;
	private int rampPlaced = 0;
	private int numSegments = 0;

	Direction startFacing;
	/** Start cell in grid coordinates. */
	Vec3i startPos;

	/** {template index, type ordinal, 1.12 rotation, grid x, grid y, grid z}; y counts in segments and may exceed sY by one (roofs). */
	final List<int[]> placements = new ArrayList<>();
	/** Grid cells that get a spawner, {x, y, z}. */
	final List<int[]> spawners = new ArrayList<>();

	public MazeDungeonPath(int sX, int sY, int sZ, RandomSource rand) {
		this.sX = sX;
		this.sY = sY;
		this.sZ = sZ;
		this.rand = rand;
		this.volume = new PathSegment[sX][sY][sZ];
	}

	/** 1.12 Dungeon.generate: five paths, keep the biggest, then lay out segments and spawners. */
	public static MazeDungeonPath generate(DungeonPreset preset, int sizeX, int sizeY, int sizeZ, RandomSource rand) {
		MazeDungeonPath best = null;
		for (int i = 0; i < 5; i++) {
			MazeDungeonPath p = new MazeDungeonPath(sizeX / DungeonPreset.SIZE_XZ, sizeY / DungeonPreset.SIZE_Y, sizeZ / DungeonPreset.SIZE_XZ, rand);
			preset.init(p);
			p.generatePath();
			if (best == null || p.numSegments > best.numSegments) best = p;
		}
		best.generateDungeon(preset);
		best.generateSpawners(0.1f);
		return best;
	}

	public int getNumSegments() {
		return numSegments;
	}

	void generatePath() {
		if (useBottomLayer) sY -= 1;
		int startX, startZ;
		int startY = (sY + startHeightLevel) % sY;
		switch (rand.nextInt(4)) {
			case 0 -> {
				startX = 0;
				startZ = sZ / 2;
				startFacing = Direction.EAST;
			}
			case 1 -> {
				startX = sX - 1;
				startZ = sZ / 2;
				startFacing = Direction.WEST;
			}
			case 2 -> {
				startX = sX / 2;
				startZ = 0;
				startFacing = Direction.SOUTH;
			}
			default -> {
				startX = sX / 2;
				startZ = sZ - 1;
				startFacing = Direction.NORTH;
			}
		}
		entranceRampLength = Math.min(entranceRampLength, startY);
		startPos = new Vec3i(startX, startY, startZ);
		generateSegment(startX, startY, startZ, startFacing.get2DDataValue(), null);
	}

	private void generateSegment(int x, int y, int z, int dir, PathSegment prev) {
		Direction facing = Direction.from2DDataValue(dir);
		PathSegment segment = new PathSegment(x, y, z);
		if (prev != null) {
			segment.setConnection(facing.getOpposite());
			prev.setConnection(facing);
		} else {
			segment.isEntrance = true;
		}
		addSegment(segment);

		int roll = 0;
		boolean success = false, canRollAgain = true, fork = false;

		//downwards entrance ramp
		if (rampPlaced < entranceRampLength) {
			Vec3i nextPos = segment.next(facing);
			Vec3i elevPos = new Vec3i(x, y - 1, z);
			Vec3i elevNextPos = new Vec3i(x + facing.getStepX(), y - 1, z + facing.getStepZ());
			if (isWithinBounds(nextPos) && isWithinBounds(elevNextPos)) {
				PathSegment rampDummy = new PathSegment(elevPos.getX(), elevPos.getY(), elevPos.getZ());
				addSegment(rampDummy);
				segment.isRamp = true;
				rampDummy.isRamp = true;
				segment.elevation = -1;
				rampDummy.elevation = 1;
				rampDummy.rampRotation = facing.getOpposite().get2DDataValue();
				generateSegment(elevNextPos.getX(), elevNextPos.getY(), elevNextPos.getZ(), dir, rampDummy);
				success = true;
			}
			rampPlaced++;
		}

		while (!success && roll++ < 3) {
			int nextDir;
			if (segment.isEntrance) {
				nextDir = dir;
				canRollAgain = false;
			} else {
				nextDir = getRandomDir(dir);
			}
			Direction nextFacing = Direction.from2DDataValue(nextDir);
			Vec3i nextPos = segment.next(nextFacing);
			if (isWithinBounds(nextPos)) {
				if (dir == nextDir && !segment.isEntrance && !fork && rand.nextFloat() < chanceRamp) {
					int dy = rand.nextFloat() < chanceUp ? 1 : -1;
					Vec3i elevPos = new Vec3i(x, y + dy, z);
					if (isWithinBounds(elevPos) && !isOccupied(elevPos)) {
						Vec3i elevNextPos = new Vec3i(x + nextFacing.getStepX(), y + dy, z + nextFacing.getStepZ());
						boolean cont = false;
						if (!isOccupied(elevNextPos)) {
							success = true;
							cont = true;
						} else {
							PathSegment seg = get(elevNextPos);
							if (seg != null && !seg.isRamp && !seg.isEntrance) {
								seg.setConnection(nextFacing.getOpposite());
								success = true;
							}
						}
						if (success) {
							canRollAgain = false;
							PathSegment rampDummy = new PathSegment(elevPos.getX(), elevPos.getY(), elevPos.getZ());
							addSegment(rampDummy);
							segment.isRamp = true;
							rampDummy.isRamp = true;
							if (dy == 1) {
								segment.elevation = 1;
								rampDummy.elevation = -1;
								segment.rampRotation = nextDir;
							} else {
								segment.elevation = -1;
								rampDummy.elevation = 1;
								rampDummy.rampRotation = nextFacing.getOpposite().get2DDataValue();
							}
							if (cont) generateSegment(elevNextPos.getX(), elevNextPos.getY(), elevNextPos.getZ(), nextDir, rampDummy);
						}
					}
				} else if (!isOccupied(nextPos)) {
					if (rand.nextFloat() < chanceRoom) {
						int area = minRoomArea + rand.nextInt(maxRoomArea - minRoomArea);
						int w = minRoomWidth + rand.nextInt(maxRoomWidth - minRoomWidth);
						int l = minRoomWidth + rand.nextInt(maxRoomWidth - minRoomWidth);
						if (!tryGrowRoom(nextPos, segment, nextFacing, w, l, area)) {
							generateSegment(nextPos.getX(), nextPos.getY(), nextPos.getZ(), nextDir, segment);
						}
					} else {
						generateSegment(nextPos.getX(), nextPos.getY(), nextPos.getZ(), nextDir, segment);
					}
					success = true;
				} else {
					PathSegment seg = get(nextPos);
					if (seg != null && !seg.isRamp && !seg.isEntrance) {
						segment.setConnection(nextFacing);
						seg.setConnection(nextFacing.getOpposite());
						success = true;
					}
				}
			}
			if (success && canRollAgain && rand.nextFloat() < chanceFork) {
				success = false;
				fork = true;
			}
		}
	}

	private boolean tryGrowRoom(Vec3i pos, PathSegment prev, Direction dir, int maxWidth, int maxLength, int preferredArea) {
		int x = pos.getX(), y = pos.getY(), z = pos.getZ();
		Direction right = dir.getClockWise(), left = dir.getCounterClockWise();
		int bestl = 0, bestwL = 0, bestwR = 0;
		float bestValue = Float.MAX_VALUE;
		int maxwL = Integer.MAX_VALUE, maxwR = Integer.MAX_VALUE;

		for (int l = 0; l < maxLength; l++) {
			int fx = x + dir.getStepX() * l, fz = z + dir.getStepZ() * l;
			Vec3i posF = new Vec3i(fx, y, fz);
			if (!isWithinBounds(posF) || isOccupied(posF)) break;
			int wL = 0, wR = 0;
			while (wL < maxWidth && wL < maxwL) {
				Vec3i p = new Vec3i(fx + left.getStepX() * (wL + 1), y, fz + left.getStepZ() * (wL + 1));
				if (!isWithinBounds(p) || isOccupied(p)) {
					maxwL = wL;
					break;
				}
				wL++;
			}
			while (wR < maxWidth && wR < maxwR) {
				Vec3i p = new Vec3i(fx + right.getStepX() * (wR + 1), y, fz + right.getStepZ() * (wR + 1));
				if (!isWithinBounds(p) || isOccupied(p)) {
					maxwR = wR;
					break;
				}
				wR++;
			}
			int area = (wL + wR + 1) * (l + 1);
			float value = (float) Math.max(area, preferredArea) / Math.min(area, preferredArea)
					+ (float) Math.max(wL + wR + 1, l + 1) / Math.min(wL + wR + 1, l + 1);
			if (value < bestValue) {
				bestValue = value;
				bestl = l;
				bestwL = wL;
				bestwR = wR;
			}
		}

		if (bestl < 2 || bestwL + bestwR + 1 < 2) return false;
		int x1 = x + left.getStepX() * bestwL, z1 = z + left.getStepZ() * bestwL;
		int x2 = x + right.getStepX() * bestwR + dir.getStepX() * bestl, z2 = z + right.getStepZ() * bestwR + dir.getStepZ() * bestl;
		placeRoomSegments(new Vec3i(Math.min(x1, x2), y, Math.min(z1, z2)), new Vec3i(Math.max(x1, x2), y, Math.max(z1, z2)));
		prev.setConnection(dir);
		PathSegment segment = get(pos);
		if (segment != null) segment.setConnection(dir.getOpposite());
		return true;
	}

	private void placeRoomSegments(Vec3i min, Vec3i max) {
		int roomID = nextRoomID++;
		int y = min.getY();
		for (int x = min.getX(); x <= max.getX(); x++) {
			for (int z = min.getZ(); z <= max.getZ(); z++) {
				PathSegment s = new PathSegment(x, y, z);
				s.roomID = roomID;
				addSegment(s);
				if (x > min.getX()) s.pattern[7] = true;
				if (x < max.getX()) s.pattern[3] = true;
				if (z > min.getZ()) s.pattern[1] = true;
				if (z < max.getZ()) s.pattern[5] = true;
				if (x != min.getX() && z != min.getZ()) s.pattern[0] = true;
				if (x != min.getX() && z != max.getZ()) s.pattern[6] = true;
				if (x != max.getX() && z != min.getZ()) s.pattern[2] = true;
				if (x != max.getX() && z != max.getZ()) s.pattern[4] = true;
			}
		}
		//doors on each side
		int length = max.getZ() - min.getZ() + 1;
		for (int i = rand.nextInt(length); i > 0; i--) placeDoor(new Vec3i(min.getX(), y, min.getZ() + rand.nextInt(length)), Direction.WEST);
		for (int i = rand.nextInt(length); i > 0; i--) placeDoor(new Vec3i(max.getX(), y, min.getZ() + rand.nextInt(length)), Direction.EAST);
		length = max.getX() - min.getX() + 1;
		for (int i = rand.nextInt(length); i > 0; i--) placeDoor(new Vec3i(min.getX() + rand.nextInt(length), y, min.getZ()), Direction.NORTH);
		for (int i = rand.nextInt(length); i > 0; i--) placeDoor(new Vec3i(min.getX() + rand.nextInt(length), y, max.getZ()), Direction.SOUTH);
	}

	private void placeDoor(Vec3i pos, Direction facing) {
		PathSegment segment = get(pos);
		Vec3i nextPos = pos.relative(facing);
		if (!isWithinBounds(nextPos)) return;
		if (!isOccupied(nextPos)) {
			generateSegment(nextPos.getX(), nextPos.getY(), nextPos.getZ(), facing.get2DDataValue(), segment);
		} else {
			PathSegment seg = get(nextPos);
			if (seg != null && !seg.isRamp && !seg.isEntrance) {
				segment.setConnection(facing);
				seg.setConnection(facing.getOpposite());
			}
		}
	}

	private void addSegment(PathSegment seg) {
		if (seg.x >= 0 && seg.x < sX && seg.y >= 0 && seg.y < sY && seg.z >= 0 && seg.z < sZ) {
			volume[seg.x][seg.y][seg.z] = seg;
			list.add(seg);
			numSegments++;
		}
	}

	private boolean isOccupied(Vec3i pos) {
		return volume[pos.getX()][pos.getY()][pos.getZ()] != null;
	}

	private PathSegment get(Vec3i pos) {
		return volume[pos.getX()][pos.getY()][pos.getZ()];
	}

	private int getRandomDir(int dir) {
		float f = rand.nextFloat();
		if (f < chanceStraight) return dir;
		Direction d = Direction.from2DDataValue(dir);
		return (f < chanceStraight + (1.0f - chanceStraight) * 0.5f ? d.getClockWise() : d.getCounterClockWise()).get2DDataValue();
	}

	private boolean isWithinBounds(Vec3i pos) {
		int maxY = entranceRampLength > rampPlaced ? sY : sY - entranceRampLength;
		return pos.getX() >= 0 && pos.getX() < sX && pos.getY() >= 0 && pos.getY() < maxY && pos.getZ() >= 0 && pos.getZ() < sZ;
	}

	private void place(DungeonPreset preset, Type type, int templateY, boolean above, int rot, int x, int y, int z) {
		placements.add(new int[]{DungeonTemplate.NAMES.indexOf(preset.template(templateY, sY, above)), type.ordinal(), rot, x, y, z});
	}

	/** 1.12 generateDungeon: pick a segment type and rotation for every cell, plus roofs, foundations and pillars. */
	void generateDungeon(DungeonPreset preset) {
		if (useBottomLayer) cloneBottomLayer();
		for (int i = 0; i < sX; i++) {
			for (int j = 0; j < sY; j++) {
				for (int k = 0; k < sZ; k++) {
					boolean above = false, below = false, directlyBelow = false;
					for (int y = j + 1; y < sY && !above; y++) above = volume[i][y][k] != null;
					for (int y = j - 1; y >= 0; y--) {
						if (volume[i][y][k] != null) {
							below = true;
							directlyBelow = y == j - 1;
							break;
						}
					}
					PathSegment segment = volume[i][j][k];
					if (segment == null) {
						if (useFoundations && above && !below) place(preset, Type.FOUNDATION, j, above, 0, i, j, k);
						else if (usePillars && above) place(preset, Type.PILLARS, j, above, 0, i, j, k);
						continue;
					}
					if (segment.isRamp) {
						if (segment.elevation == 1) {
							int r = segment.rampRotation;
							if (r == 0 || r == 2) r = (r + 2) % 4; //1.12 workaround
							place(preset, Type.RAMP, segment.y, above, (r + 1) % 4, i, j, k);
						} else if (useRoof && !above && segment.y + 1 < sY) {
							int r = volume[i][j - 1][k].rampRotation;
							if (r == 0 || r == 2) r = (r + 2) % 4;
							place(preset, Type.END, -1, above, (r + 1) % 4, i, j + 1, k);
						}
						continue;
					}
					for (Type t : Type.values()) {
						if (!t.match()) continue;
						int r = 0;
						while (r < t.rotations && !segment.match(t.pattern, r)) r++;
						if (r == t.rotations) continue;
						boolean entrance = t == Type.END && j == (sY + startHeightLevel) % sY;
						place(preset, entrance ? Type.ENTRANCE : t, segment.y, above, r, i, j, k);
						if (useRoof && !above && segment.y >= startHeightLevel && segment.y + 1 < sY) place(preset, t, -1, above, r, i, j + 1, k);
						break;
					}
				}
			}
		}
	}

	/** Grows the grid by one level and copies the bottom layer below it (shifting everything up). */
	private void cloneBottomLayer() {
		PathSegment[][][] v = new PathSegment[sX][sY + 1][sZ];
		for (int i = 0; i < sX; i++) {
			for (int j = 0; j < sY; j++) {
				for (int k = 0; k < sZ; k++) {
					PathSegment s = volume[i][j][k];
					if (s == null) continue;
					v[i][j + 1][k] = s;
					s.y++;
					if (j == 0) {
						PathSegment copy = new PathSegment(i, 0, k);
						copy.pattern = s.pattern;
						v[i][0][k] = copy;
					}
				}
			}
		}
		volume = v;
		sY++;
		startPos = startPos.above();
	}

	private void generateSpawners(float density) {
		int numSpawns = (int) (numSegments * density);
		for (int tries = 0; numSpawns > 0 && tries < 5; ) {
			PathSegment seg = list.get(rand.nextInt(list.size()));
			int[] cell = {seg.x, seg.y, seg.z};
			if (!seg.isEntrance && !seg.isRamp && spawners.stream().noneMatch(c -> java.util.Arrays.equals(c, cell))) {
				spawners.add(cell);
				numSpawns--;
				tries = 0;
			} else {
				tries++;
			}
		}
	}

	private static class PathSegment {
		boolean isEntrance, isRamp;
		int x, y, z;
		int elevation = 0, rampRotation = -1, roomID = -1;
		boolean[] pattern = new boolean[8];

		PathSegment(int x, int y, int z) {
			this.x = x;
			this.y = y;
			this.z = z;
		}

		Vec3i next(Direction facing) {
			return new Vec3i(x + facing.getStepX(), y, z + facing.getStepZ());
		}

		void setConnection(Direction facing) {
			pattern[((facing.get2DDataValue() + 2) % 4) * 2 + 1] = true;
		}

		boolean match(boolean[] o, int rotations) {
			for (int k = 0; k < 8; k++) if (pattern[k] != o[(rotations * 2 + k) % 8]) return false;
			return true;
		}
	}
}
