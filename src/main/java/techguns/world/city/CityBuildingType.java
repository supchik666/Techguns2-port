package techguns.world.city;

import java.util.List;
import java.util.Random;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import techguns.TGBlocks;
import techguns.blocks.LampBlock;
import techguns.world.camp.Part;
import techguns.world.dungeon.Canvas;
import techguns.world.dungeon.DungeonPiece;

/** 1.12 CityBuildingType, BuildingStyle and Staircase: a building of wall sections stacked floor by floor. */
final class CityBuildingType {
	/** 1.12 BuildingStyle: block options for wall, glass, floor, border and door decoration; chisel blocks use their vanilla fallbacks. */
	enum Style {
		STONE, FACTORY, BRICK, LAB, LIME;

		List<List<Block>> blocks() {
			List<Block> glass = List.of(Blocks.GLASS_PANE);
			List<Block> stoneFloors = List.of(Blocks.POLISHED_ANDESITE, Blocks.STONE_BRICKS, Blocks.POLISHED_ANDESITE, Blocks.DIORITE, Blocks.POLISHED_GRANITE);
			return switch (this) {
				case STONE -> List.of(List.of(Blocks.STONE, Blocks.STONE_BRICKS, Blocks.COBBLESTONE, Blocks.COBBLESTONE, Blocks.COBBLESTONE, Blocks.POLISHED_ANDESITE),
						glass, stoneFloors, List.of(Blocks.POLISHED_ANDESITE, Blocks.POLISHED_ANDESITE, Blocks.STONE_BRICKS, Blocks.STONE_BRICKS, Blocks.POLISHED_ANDESITE));
				case FACTORY -> List.of(List.of(Blocks.BRICKS), List.of(Blocks.LIGHT_GRAY_STAINED_GLASS_PANE), List.of(TGBlocks.CONCRETE_GREY_DARK.get()));
				case BRICK -> List.of(List.of(Blocks.BRICKS), glass, stoneFloors, List.of(Blocks.BRICKS));
				case LAB -> List.of(List.of(Blocks.QUARTZ_BLOCK), List.of(Blocks.WHITE_STAINED_GLASS_PANE), List.of(Blocks.POLISHED_ANDESITE),
						List.of(Blocks.QUARTZ_PILLAR), List.of(Blocks.CHISELED_QUARTZ_BLOCK));
				case LIME -> List.of(List.of(Blocks.CUT_SANDSTONE), glass, List.of(Blocks.SANDSTONE), List.of(Blocks.CHISELED_SANDSTONE), List.of(Blocks.CHISELED_SANDSTONE));
			};
		}

		/** Stairs (1.12 default: facing east) and the block under them. */
		BlockState[] stairs() {
			return this == LAB ? new BlockState[]{Blocks.QUARTZ_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.EAST), Blocks.QUARTZ_BLOCK.defaultBlockState()}
					: new BlockState[]{Blocks.OAK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.EAST), Blocks.DARK_OAK_PLANKS.defaultBlockState()};
		}
	}

	static final CityBuildingType BUILDING1 = new CityBuildingType(2, 5, Style.STONE, true, 3, 1,
			new int[][]{{0, 0}, {0, 1}, {0, 1}, {0, 0}, {3, 3}},
			new int[][]{{0, 0}, {0, 0}, {0, -1}, {0, -1}, {3, 3}});
	static final CityBuildingType BUILDING2 = new CityBuildingType(3, 4, Style.FACTORY, false, 0, 2,
			new int[][]{{0, 0, 0}, {0, 1, 1}, {0, 0, 0}, {2, 2, 2}},
			new int[][]{{0, 0, 0}, {0, -1, -1}, {0, -1, -1}, {2, 2, 2}});
	static final CityBuildingType BUILDING3 = new CityBuildingType(3, 4, Style.STONE, true, 3, 2,
			new int[][]{{0, 0, 0}, {0, 1, 1}, {0, 0, 0}, {3, 3, 3}},
			new int[][]{{0, 0, 0}, {0, -1, -1}, {0, -1, -1}, {3, 3, 3}});
	static final CityBuildingType BUILDING4 = new CityBuildingType(3, 4, Style.BRICK, true, 3, 2,
			new int[][]{{0, 0, 0}, {0, 1, 1}, {0, 0, 0}, {3, 3, 3}},
			new int[][]{{0, 0, 0}, {0, -1, -1}, {0, -1, -1}, {3, 3, 3}});
	static final CityBuildingType BUILDING5 = new CityBuildingType(3, 5, Style.LAB, true, 3, 2,
			new int[][]{{0, 0, 0}, {0, 1, 1}, {0, 1, 1}, {0, 0, 0}, {3, 3, 3}},
			new int[][]{{0, 0, 0}, {0, 4, 4}, {0, -1, -1}, {0, -1, -1}, {3, 3, 3}});
	static final CityBuildingType BUILDING6 = new CityBuildingType(3, 4, Style.LIME, true, 3, 2,
			new int[][]{{0, 0, 0}, {0, 1, 1}, {0, 0, 0}, {3, 3, 3}},
			new int[][]{{0, 0, 0}, {0, -1, -1}, {0, -1, -1}, {3, 3, 3}});

	static final int WALL_EXTRA_LENGTH = 1, DOOR_OFFSET = 1;

	final int wallX, wallY;
	final Style style;
	final boolean roofLikeFloor;
	final int roofBorderIndex, doorWidth;
	/** Rows top to bottom, indices into the style's blocks; -1 = air. */
	final int[][] wallPattern, doorPattern;

	CityBuildingType(int wallX, int wallY, Style style, boolean roofLikeFloor, int roofBorderIndex, int doorWidth, int[][] wallPattern, int[][] doorPattern) {
		this.wallX = wallX;
		this.wallY = wallY;
		this.style = style;
		this.roofLikeFloor = roofLikeFloor;
		this.roofBorderIndex = roofBorderIndex;
		this.doorWidth = doorWidth;
		this.wallPattern = wallPattern;
		this.doorPattern = doorPattern;
	}

	/** 1.12 CityBuilding. */
	static class Building extends Part {
		final CityBuildingType type;

		Building(CityBuildingType type, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
			super(minX, minY, minZ, maxX, maxY, maxZ);
			this.type = type;
		}

		@Override
		public void place(Canvas c, int x, int y, int z, int sizeX, int sizeY, int sizeZ, int direction, Random rnd) {
			type.place(c, x, y, z, sizeX, sizeY, sizeZ, direction, rnd);
		}
	}

	void place(Canvas c, int posX, int posY, int posZ, int sizeX, int sizeY, int sizeZ, int direction, Random rand) {
		//1.12 BuildingStyle.rollVariation, which used an unseeded Random
		BlockState[] blocks = style.blocks().stream().map(l -> l.get(rand.nextInt(l.size())).defaultBlockState()).toArray(BlockState[]::new);
		BlockState[] stairType = style.stairs();
		int stairsdir = direction;
		if ((direction == 0 || direction == 2) && sizeX - 2 < wallY + 1 || (direction == 1 || direction == 3) && sizeZ - 2 < wallY + 1) {
			stairsdir = (stairsdir + 1) % 4;
		}
		int segX = (sizeX - WALL_EXTRA_LENGTH) / wallX, sx = segX * wallX + WALL_EXTRA_LENGTH;
		int segZ = (sizeZ - WALL_EXTRA_LENGTH) / wallX, sz = segZ * wallX + WALL_EXTRA_LENGTH;
		int segY = sizeY / wallY;
		boolean isSmall = Math.max(sx - 2, sz - 2) < wallY + 1 || Math.min(sx, sz) < 4;
		for (int i = 0; i < segY; i++) {
			int y = posY + i * wallY;
			floor(c, posX + 1, y, posZ + 1, sx - 2, sz - 2, blocks);
			if (i >= 1) lamps(c, posX + 1, y - 1, posZ + 1, sx - 2, sz - 2);
			wall(c, posX, y, posZ, segX, 1, 0, 0, i == 0 && direction == 0, blocks);
			wall(c, posX + sx - 1, y, posZ, segZ, 0, 1, 1, i == 0 && direction == 1, blocks);
			wall(c, posX + sx - 1, y, posZ + sz - 1, segX, -1, 0, 2, i == 0 && direction == 2, blocks);
			wall(c, posX, y, posZ + sz - 1, segZ, 0, -1, 3, i == 0 && direction == 3, blocks);
		}
		int y2 = posY + segY * wallY;
		if (roofLikeFloor) floor(c, posX + 1, y2, posZ + 1, sx - 2, sz - 2, blocks);
		else floor(c, posX, y2, posZ, sx, sz, blocks);
		lamps(c, posX + 1, y2 - 1, posZ + 1, sx - 2, sz - 2);
		BlockState block = blocks[0];
		for (int i = 0; i < sx; i++) {
			for (int j = 0; j < sz; j++) {
				if (i == 0 || i == sx - 1 || j == 0 || j == sz - 1) {
					c.set(posX + i, y2 + 1, posZ + j, block);
					if (roofLikeFloor) c.set(posX + i, y2, posZ + j, blocks[roofBorderIndex]);
				}
			}
		}
		int dx, dz, px, pz;
		switch (stairsdir) {
			case 0 -> { dx = 0; dz = -1; px = posX + sx - 2; pz = posZ + sz - 2; }
			case 1 -> { dx = 1; dz = 0; px = posX + 1; pz = posZ + sz - 2; }
			case 2 -> { dx = 0; dz = 1; px = posX + 1; pz = posZ + 1; }
			default -> { dx = -1; dz = 0; px = posX + sx - 2; pz = posZ + 1; }
		}
		BlockState ladder = DungeonPiece.rotate(TGBlocks.LADDER_METAL.get().defaultBlockState().setValue(LadderBlock.FACING, Direction.SOUTH), (stairsdir - 1) % 4);
		for (int i = 0; i < segY - 1; i++) {
			int y = posY + i * wallY;
			if (!isSmall) {
				if (i % 2 == 1) staircase(c, px + dx, y + 1, pz + dz, wallY, (stairsdir + 2) % 4, true, stairType);
				else staircase(c, px, y + 1, pz, wallY, (stairsdir + 2) % 4, false, stairType);
			} else {
				for (int j = 0; j < wallY; j++) {
					if (j < wallY - 1) c.set(px - dx, y + i + 1, pz - dz, block); //1.12 used i here
					c.set(px, y + j + 1, pz, ladder);
				}
			}
		}
		int y = posY + (segY - 1) * wallY;
		for (int i = 0; i < wallY; i++) {
			if (i < wallY - 1) c.set(px - dx, y + i + 1, pz - dz, block);
			c.set(px, y + i + 1, pz, ladder);
		}
		c.set(px, y2 + 1, pz, Blocks.OAK_TRAPDOOR.defaultBlockState());
	}

	private static void lamps(Canvas c, int posX, int posY, int posZ, int sizeX, int sizeZ) {
		int density = 3, ix = (sizeX - 1) % density, iz = (sizeZ - 1) % density;
		BlockState lamp = TGBlocks.LAMP_YELLOW.get().defaultBlockState().setValue(LampBlock.FACING, Direction.UP);
		for (int x = posX + ix / 2; x < posX + sizeX - (1 - ix / 2); x += density) {
			for (int z = posZ + iz / 2; z < posZ + sizeZ - (1 - iz / 2); z += density) {
				c.set(x, posY, z, lamp);
			}
		}
	}

	private void wall(Canvas c, int posX, int posY, int posZ, int segments, int dx, int dz, int direction, boolean placeDoor, BlockState[] blocks) {
		for (int i = 0; i < segments; i++) {
			boolean door = placeDoor && i == segments / 2;
			for (int j = 0; j < wallX; j++) {
				for (int l = 1; l <= wallY; l++) {
					int b = (door ? doorPattern : wallPattern)[wallY - l][j];
					int offset = i * wallX + j;
					c.set(posX + offset * dx, posY + l - 1, posZ + offset * dz, b >= 0 ? DungeonPiece.rotate(blocks[b], direction) : Blocks.AIR.defaultBlockState());
				}
			}
			if (door) {
				for (int k = 0; k < doorWidth; k++) {
					int offset = i * wallX + DOOR_OFFSET + k;
					c.door(posX + offset * dx, posY + 1, posZ + offset * dz, Blocks.OAK_DOOR, Direction.from2DDataValue((direction + 3) % 4), false);
				}
			}
		}
	}

	private static void floor(Canvas c, int posX, int posY, int posZ, int sizeX, int sizeZ, BlockState[] blocks) {
		c.fill(posX, posY, posZ, sizeX, 1, sizeZ, blocks[2]);
	}

	/** 1.12 Staircase.placeStaircase. */
	private static void staircase(Canvas c, int posX, int posY, int posZ, int size, int direction, boolean inverted, BlockState[] stairType) {
		int dx = direction == 0 ? 1 : direction == 2 ? -1 : 0, dz = direction == 1 ? 1 : direction == 3 ? -1 : 0;
		for (int i = 0; i < size; i++) {
			int step = inverted ? i : i + 1, y = posY + (inverted ? size - 1 - i : i);
			int x = posX + step * dx, z = posZ + step * dz;
			int rot = inverted ? (3 - (direction + 2) % 4 + 1) % 4 : (3 - direction + 1) % 4;
			c.set(x, y, z, DungeonPiece.rotate(stairType[0], rot));
			for (int j = 1; j <= y - posY; j++) c.set(x, y - j, z, stairType[1]);
			for (int j = 1; j <= 3; j++) c.air(x, y + j, z);
		}
	}
}
