package techguns.world.camp;

import java.util.Random;

import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootTable;
import techguns.TGBlocks;
import techguns.Techguns;
import techguns.blocks.LampBlock;
import techguns.world.dungeon.Canvas;

/** The 1.12 military camp buildings (world/structures/Tent, Containers, CampProps, Bunker, Barracks, Helipad, Tanks, WatchTowerSmall). */
final class CampParts {
	static final ResourceKey<LootTable> BUNKER_LOOT = ResourceKey.create(Registries.LOOT_TABLE, Techguns.id("chests/militarybase_bunker"));
	static final ResourceKey<LootTable> BARRACKS_LOOT = ResourceKey.create(Registries.LOOT_TABLE, Techguns.id("chests/militarybase_barracks"));

	private CampParts() {
	}

	/** ponytail: a barrel stands in for the 9 military crate variants until the military crate block is ported. */
	static BlockState crate() {
		return Blocks.BARREL.defaultBlockState().setValue(BarrelBlock.FACING, Direction.UP);
	}

	static BlockState lamp(Direction facing) {
		return TGBlocks.LAMP_YELLOW.get().defaultBlockState().setValue(LampBlock.FACING, facing);
	}

	static class Tent extends Part {
		final int type;

		Tent(int minX, int minY, int minZ, int maxX, int maxY, int maxZ, int type) {
			super(minX, minY, minZ, maxX, maxY, maxZ);
			this.type = type;
		}

		@Override
		public void place(Canvas c, int posX, int posY, int posZ, int sizeX, int sizeY, int sizeZ, int direction, Random rnd) {
			if (type == 0) {
				posX++;
				posZ++;
				sizeX -= 2;
				sizeZ -= 2;
			}
			//1.12 always used the wood net here (its camo switch was unused)
			BlockState roof = c.camo.net(true), wall = c.camo.net(false), ground = Blocks.GRAVEL.defaultBlockState();
			for (int i = 0; i < sizeX; i++) {
				for (int j = 0; j < sizeZ; j++) {
					for (int k = 0; k < sizeY; k++) {
						if (k == 0) {
							c.set(posX + i, posY + k, posZ + j, ground);
						} else if (k < sizeY - 1) {
							if (j == 0 || j == sizeZ - 1 || i == 0 || i == sizeX - 1) c.set(posX + i, posY + k, posZ + j, wall);
						} else {
							c.set(posX + i, posY + k, posZ + j, roof);
						}
					}
				}
			}
			if (type == 0 || type == 1) {
				int x, z, gx, gz;
				switch (direction) {
					case 0 -> { x = posX; z = posZ + sizeZ / 2; gx = x - 1; gz = z; }
					case 1 -> { x = posX + sizeX / 2; z = posZ; gx = x; gz = z - 1; }
					case 2 -> { x = posX + sizeX - 1; z = posZ + sizeZ / 2; gx = x + 1; gz = z; }
					default -> { x = posX + sizeX / 2; z = posZ + sizeZ - 1; gx = x; gz = z + 1; }
				}
				c.air(x, posY + 1, z);
				c.air(x, posY + 2, z);
				if (type == 0) c.set(gx, posY, gz, ground);
			}
			if (type == 2) {
				for (int y = 1; y < sizeY - 1; y++) {
					if (direction == 0 || direction == 2) {
						for (int z = 1; z < sizeZ - 1; z++) {
							if (z % 3 != 0) c.air(posX + (direction == 0 ? 0 : sizeX - 1), posY + y, posZ + z);
						}
					} else {
						for (int x = 1; x < sizeX - 1; x++) {
							if (x % 3 != 1) c.air(posX + x, posY + y, posZ + (direction == 1 ? 0 : sizeZ - 1));
						}
					}
				}
			}
		}
	}

	static class Containers extends Part {
		static final int WIDTH = 2, HEIGHT = 2;

		Containers(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
			super(minX, minY, minZ, maxX, maxY, maxZ);
		}

		@Override
		public void place(Canvas c, int posX, int posY, int posZ, int sizeX, int sizeY, int sizeZ, int direction, Random rnd) {
			BlockState block = TGBlocks.CONTAINER_RED.get().defaultBlockState();
			int offset = 0;
			boolean alongX = sizeX > sizeZ;
			int count = (int) (((alongX ? sizeZ : sizeX) + 1.0f) / (WIDTH + 1.0f));
			for (int i = 0; i < count; i++) {
				if (alongX) c.fill(posX, posY + 1, posZ + offset, sizeX, HEIGHT, WIDTH, block);
				else c.fill(posX + offset, posY + 1, posZ, WIDTH, HEIGHT, sizeZ, block);
				offset += WIDTH + 1;
			}
		}
	}

	static class CampProps extends Part {
		final int variant;

		CampProps(int minX, int minY, int minZ, int maxX, int maxY, int maxZ, int variant) {
			super(minX, minY, minZ, maxX, maxY, maxZ);
			this.variant = variant;
		}

		@Override
		public void place(Canvas c, int posX, int posY, int posZ, int sizeX, int sizeY, int sizeZ, int direction, Random rnd) {
			switch (variant) {
				case 1 -> { //crates + roof
					roof(c, posX, posY, posZ, sizeX, sizeY, sizeZ);
					crates(c, posX + 1, posY, posZ + 1, sizeX - 2, sizeY - 2, sizeZ - 2, rnd);
				}
				case 2 -> sandbags(c, posX, posY, posZ, sizeX, sizeZ, direction);
				case 3 -> {
					roof(c, posX, posY + 1, posZ, sizeX, sizeY - 1, sizeZ);
					sandbags(c, posX, posY, posZ, sizeX, sizeZ, direction);
				}
				case 4 -> sandbagRow(c, posX, posY, posZ, sizeX, sizeY, sizeZ);
				case 5 -> {
					roof(c, posX, posY + 1, posZ, sizeX, sizeY - 1, sizeZ);
					sandbags(c, posX, posY, posZ, sizeX, sizeZ, direction);
					crates(c, posX + 1, posY, posZ + 1, sizeX - 2, sizeY - 2, sizeZ - 2, rnd);
				}
				default -> crates(c, posX, posY, posZ, sizeX, sizeY, sizeZ, rnd);
			}
		}

		private static void sandbagRow(Canvas c, int px, int py, int pz, int sizeX, int sizeY, int sizeZ) {
			BlockState sandbags = TGBlocks.SANDBAGS.get().defaultBlockState();
			int i = 1;
			for (int y = 1; y < sizeY; y++) {
				if (sizeX > sizeZ) {
					for (int x = i; x < sizeX - i; x++) c.set(px + x, py + y, pz + sizeZ / 2, sandbags);
				} else {
					for (int z = i; z < sizeZ - i; z++) c.set(px + sizeX / 2, py + y, pz + z, sandbags);
				}
				i++;
			}
		}

		static void sandbags(Canvas c, int posX, int posY, int posZ, int sizeX, int sizeZ, int direction) {
			BlockState sandbags = TGBlocks.SANDBAGS.get().defaultBlockState();
			for (int x = 0; x < sizeX; x++) {
				for (int z = 0; z < sizeZ; z++) {
					if (x == 0 || x == sizeX - 1 || z == 0 || z == sizeZ - 1) c.set(posX + x, posY + 1, posZ + z, sandbags);
				}
			}
			switch (direction) {
				case 0 -> c.air(posX, posY + 1, posZ + sizeZ / 2);
				case 1 -> c.air(posX + sizeX / 2, posY + 1, posZ);
				case 2 -> c.air(posX + sizeX - 1, posY + 1, posZ + sizeZ / 2);
				default -> c.air(posX + sizeX / 2, posY + 1, posZ + sizeZ - 1);
			}
		}

		private static void roof(Canvas c, int posX, int posY, int posZ, int sizeX, int sizeY, int sizeZ) {
			BlockState fence = Blocks.OAK_FENCE.defaultBlockState();
			c.fill(posX, posY + 1, posZ, 1, sizeY - 2, 1, fence);
			c.fill(posX + sizeX - 1, posY + 1, posZ, 1, sizeY - 2, 1, fence);
			c.fill(posX, posY + 1, posZ + sizeZ - 1, 1, sizeY - 2, 1, fence);
			c.fill(posX + sizeX - 1, posY + 1, posZ + sizeZ - 1, 1, sizeY - 2, 1, fence);
			c.fill(posX, posY + sizeY - 1, posZ, sizeX, 1, sizeZ, c.camo.net(true));
		}

		private static void crates(Canvas c, int posX, int posY, int posZ, int sizeX, int sizeY, int sizeZ, Random rnd) {
			for (int x = 0; x < sizeX; x++) {
				for (int z = 0; z < sizeZ; z++) {
					c.set(posX + x, posY, posZ + z, Blocks.GRAVEL.defaultBlockState());
					rnd.nextFloat(); //1.12's unused chest roll
					for (int y = 1; y < sizeY; y++) {
						if (!isFreeSpace(c, posX + x, posY + y - 1, posZ + z) && rnd.nextFloat() > 0.5f) {
							c.set(posX + x, posY + y, posZ + z, crate());
						}
					}
				}
			}
			if ((sizeX + sizeZ) / 2.0f / 6.0f > rnd.nextFloat()) { //lantern
				for (int y = sizeY; y > 1; y--) {
					for (int t = 0; t < 3; t++) {
						int x = posX + rnd.nextInt(sizeX), z = posZ + rnd.nextInt(sizeZ);
						if (c.isAir(x, posY + y, z) && !isFreeSpace(c, x, posY + y - 1, z)) {
							c.set(x, posY + y, z, TGBlocks.LANTERN_YELLOW.get().defaultBlockState());
							return;
						}
					}
				}
			}
		}

		private static boolean isFreeSpace(Canvas c, int x, int y, int z) {
			BlockState state = c.get(x, y, z);
			return state.isAir() || state.is(Blocks.SNOW) || state.is(TGBlocks.LANTERN_YELLOW.get());
		}
	}

	static class Bunker extends Part {
		final int type;

		Bunker(int minX, int minY, int minZ, int maxX, int maxY, int maxZ, int type) {
			super(minX, minY, minZ, maxX, maxY, maxZ);
			this.type = type;
		}

		@Override
		public void place(Canvas c, int posX, int posY, int posZ, int sizeX, int sizeY, int sizeZ, int direction, Random rnd) {
			BlockState ground = TGBlocks.CONCRETE_BROWN_LIGHT.get().defaultBlockState();
			BlockState wall = TGBlocks.CONCRETE_BROWN.get().defaultBlockState();
			BlockState roof = TGBlocks.CONCRETE_GREY.get().defaultBlockState();
			if (sizeX <= 4 && sizeZ >= 5) {
				if (direction == 0 || direction == 2) direction = (direction + 1) % 4;
			} else if (sizeZ <= 4 && sizeX >= 5) {
				if (direction == 1 || direction == 3) direction = (direction + 1) % 4;
			}
			switch (direction) {
				case 0 -> { posX += 2; sizeX -= 2; }
				case 1 -> { posZ += 2; sizeZ -= 2; }
				case 2 -> sizeX -= 2;
				default -> sizeZ -= 2;
			}
			Direction facing = facing(direction);
			for (int y = -1; y < sizeY; y++) {
				for (int x = 0; x < sizeX; x++) {
					for (int z = 0; z < sizeZ; z++) {
						boolean edge = x == 0 || x == sizeX - 1 || z == 0 || z == sizeZ - 1;
						int px = posX + x, py = posY + y, pz = posZ + z;
						if (y == -1) {
							c.set(px, py, pz, ground);
						} else if (y == 0) {
							if (edge) c.set(px, py, pz, wall);
							else c.air(px, py, pz);
						} else if (y == 1) {
							if (type == 2 || type == 3) {
								if (edge) c.set(px, py, pz, wall);
							} else if ((x == 0 || x == sizeX - 1) && (z == 0 || z == sizeZ - 1 || z % 3 == 0)
									|| (x == 0 || x == sizeX - 1 || x % 3 == 0) && (z == 0 || z == sizeZ - 1)) {
								c.set(px, py, pz, wall);
							}
						} else if (y == 2) {
							c.set(px, py, pz, edge ? wall : roof);
						} else if (y == 3 && (type == 1 || type == 3) && edge) {
							c.set(px, py, pz, TGBlocks.SANDBAGS.get().defaultBlockState());
						}
					}
				}
			}
			int xo, zo, dx, dz; //door position and direction
			switch (direction) {
				case 0 -> { xo = 0; zo = sizeZ / 2; dx = -1; dz = 0; }
				case 1 -> { xo = sizeX / 2; zo = 0; dx = 0; dz = -1; }
				case 2 -> { xo = sizeX - 1; zo = sizeZ / 2; dx = 1; dz = 0; }
				default -> { xo = sizeX / 2; zo = sizeZ - 1; dx = 0; dz = 1; }
			}
			int x = posX + xo, z = posZ + zo;
			//sideways (sx, sz) and outwards (dx, dz) of the door
			int sx = dx == 0 ? 1 : 0, sz = dz == 0 ? 1 : 0;
			// ponytail: copper door instead of 1.12's bunker door until the TG door is ported
			c.door(x, posY, z, Blocks.WAXED_WEATHERED_COPPER_DOOR, facing, true);
			c.set(x + sx, posY, z + sz, wall);
			c.set(x + sx, posY + 1, z + sz, wall);
			c.set(x - sx, posY, z - sz, wall);
			c.set(x - sx, posY + 1, z - sz, wall);
			for (int y = -1; y <= 0; y++) {
				c.set(x + (dx == 0 ? 1 : dx), posY + y, z + (dz == 0 ? 1 : dz), wall);
				c.set(x + (dx == 0 ? -1 : dx), posY + y, z + (dz == 0 ? -1 : dz), wall);
				c.set(x + (dx == 0 ? 1 : dx * 2), posY + y, z + (dz == 0 ? 1 : dz * 2), wall);
				c.set(x + (dx == 0 ? -1 : dx * 2), posY + y, z + (dz == 0 ? -1 : dz * 2), wall);
			}
			c.set(x + (dx == 0 ? 1 : dx), posY + 2, z + (dz == 0 ? 1 : dz), lamp(facing.getOpposite()));
			c.set(x + (dx == 0 ? -1 : dx), posY + 2, z + (dz == 0 ? -1 : dz), lamp(facing.getOpposite()));
			c.set(x, posY - 1, z, ground);
			c.set(x + dx, posY - 1, z + dz, ground);
			c.air(x + dx, posY, z + dz);
			c.air(x + dx, posY + 1, z + dz);
			c.set(x + dx * 2, posY, z + dz * 2, Blocks.COBBLESTONE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, facing));
			c.air(x + dx * 2, posY + 1, z + dz * 2);

			if (type == 2 || type == 3) {
				c.set(x - dx, posY + 1, z - dz, lamp(Direction.UP));
				crates(c, posX + 2, posY, posZ + 2, sizeX - 4, 2, sizeZ - 4, rnd);
			} else {
				c.set(posX + sizeX / 2, posY + 1, posZ + sizeZ / 2, lamp(Direction.UP));
			}

			if (type == 1 || type == 3) { //ladder to the roof
				Direction ladder;
				switch (direction) {
					case 0 -> { xo = sizeX - 2; zo = sizeZ - 2; dx = 1; dz = 0; ladder = Direction.WEST; }
					case 1 -> { xo = 1; zo = sizeZ - 2; dx = 0; dz = 1; ladder = Direction.NORTH; }
					case 2 -> { xo = 1; zo = 1; dx = -1; dz = 0; ladder = Direction.EAST; }
					default -> { xo = sizeX - 2; zo = 1; dx = 0; dz = -1; ladder = Direction.SOUTH; }
				}
				c.air(posX + xo, posY + 2, posZ + zo);
				c.set(posX + xo + dx, posY + 1, posZ + zo + dz, wall);
				BlockState l = TGBlocks.LADDER_METAL.get().defaultBlockState().setValue(LadderBlock.FACING, ladder);
				for (int y = 0; y <= 2; y++) c.set(posX + xo, posY + y, posZ + zo, l);
			}
		}

		private static void crates(Canvas c, int posX, int posY, int posZ, int sizeX, int sizeY, int sizeZ, Random r) {
			for (int x = 0; x < sizeX; x++) {
				for (int z = 0; z < sizeZ; z++) {
					if (r.nextFloat() <= 0.1f) {
						c.chest(posX + x, posY, posZ + z, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.from2DDataValue(r.nextInt(4))), BUNKER_LOOT);
						continue;
					}
					for (int y = 0; y < sizeY; y++) {
						if (!c.isAir(posX + x, posY + y - 1, posZ + z) && r.nextFloat() > 0.5) c.set(posX + x, posY + y, posZ + z, crate());
					}
				}
			}
		}
	}

	static class Barracks extends Part {
		Barracks(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
			super(minX, minY, minZ, maxX, maxY, maxZ);
		}

		@Override
		public void place(Canvas c, int posX, int posY, int posZ, int sizeX, int sizeY, int sizeZ, int direction, Random rnd) {
			BlockState pillar = Blocks.OAK_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y);
			BlockState wall = Blocks.OAK_PLANKS.defaultBlockState(), floor = Blocks.OAK_PLANKS.defaultBlockState();
			BlockState scaffold = Blocks.DARK_OAK_PLANKS.defaultBlockState(), roof = Blocks.SPRUCE_PLANKS.defaultBlockState();
			BlockState roofSlab = Blocks.SPRUCE_SLAB.defaultBlockState(), window = Blocks.GLASS_PANE.defaultBlockState();
			Direction facing = facing(direction);
			switch (direction) {
				case 0 -> { posX += 2; sizeX -= 2; }
				case 1 -> { posZ += 2; sizeZ -= 2; }
				case 2 -> sizeX -= 2;
				default -> sizeZ -= 2;
			}
			sizeX -= (sizeX - 1) % 4;
			sizeZ -= (sizeZ - 1) % 4;
			/*  TRRRRRRRRT   - 6 - roof
			 * SSSSSSSSSSSSS - 5 - roof
			 * SWOWSWOWSWOWS - 4 - wall
			 * SWWWSWWWSWWWS - 3 - wall
			 * SSSSSSSSSSSSS - 2 - floor
			 * P   P   P   P - 1 - pillars
			 * GGGGGGGGGGGGG - 0 - ground
			 */
			for (int x = 0; x < sizeX; x++) {
				for (int z = 0; z < sizeZ; z++) {
					boolean edge = x == 0 || z == 0 || x == sizeX - 1 || z == sizeZ - 1;
					int px = posX + x, pz = posZ + z;
					c.set(px, posY, pz, Blocks.GRAVEL.defaultBlockState());
					if (x % 4 == 0 && z % 4 == 0) c.set(px, posY + 1, pz, pillar);
					c.set(px, posY + 2, pz, edge ? scaffold : floor);
					if (edge) {
						for (int y = 3; y <= 4; y++) {
							c.set(px, posY + y, pz, x % 4 == 0 && z % 4 == 0 ? scaffold : y == 4 && (x % 4 == 2 || z % 4 == 2) ? window : wall);
						}
						c.set(px, posY + 5, pz, scaffold);
					}
					c.set(px, posY + 6, pz, edge ? roofSlab : roof);
				}
			}
			int xo, zo, dx, dz;
			switch (direction) {
				case 0 -> { xo = 0; zo = sizeZ / 8 * 4 + 2; dx = -1; dz = 0; }
				case 1 -> { xo = sizeX / 8 * 4 + 2; zo = 0; dx = 0; dz = -1; }
				case 2 -> { xo = sizeX - 1; zo = sizeZ / 8 * 4 + 2; dx = 1; dz = 0; }
				default -> { xo = sizeX / 8 * 4 + 2; zo = sizeZ - 1; dx = 0; dz = 1; }
			}
			int x0 = posX + xo, z0 = posZ + zo;
			c.door(x0, posY + 3, z0, Blocks.OAK_DOOR, facing.getOpposite(), false);
			c.set(x0 + dx, posY + 5, z0 + dz, lamp(facing.getOpposite()));
			BlockState stairs = Blocks.OAK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, facing.getOpposite());
			c.set(x0 + dx, posY + 2, z0 + dz, stairs);
			c.set(x0 + dx * 2, posY + 1, z0 + dz * 2, stairs);

			int xHalf = sizeX / 2, zHalf = sizeZ / 2;
			for (int x = 1; x < sizeX - 1; x++) {
				for (int z = 1; z < sizeZ - 1; z++) {
					int px = posX + x, pz = posZ + z;
					if (x == 1 || z == 1 || x == sizeX - 2 || z == sizeZ - 2) {
						if (x % 4 == 2 || z % 4 == 2) c.set(px, posY + 5, pz, lamp(Direction.UP));
						if ((x % 4 == 0 || z % 4 == 0) && x != xo && z != zo) {
							Direction rot = Direction.EAST;
							if (z % 4 == 0) {
								if (x > xHalf) rot = rot.getOpposite();
							} else {
								rot = z > zHalf ? Direction.NORTH : Direction.SOUTH;
							}
							c.chest(px, posY + 3, pz, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, rot), BARRACKS_LOOT);
						}
					} else {
						if ((x == 2 || z == 2 || x == sizeX - 3 || z == sizeZ - 3) && (sizeX / 4 > 1 || sizeZ / 4 > 1)
								&& x % 4 == 2 && z % 4 == 2 && x != xo && z != zo) {
							if (x == 2) c.bed(px, posY + 3, pz, Direction.WEST);
							else if (x == sizeX - 3) c.bed(px, posY + 3, pz, Direction.EAST);
							else if (z == 2) c.bed(px, posY + 3, pz, Direction.NORTH);
							else if (z == sizeZ - 3) c.bed(px, posY + 3, pz, Direction.SOUTH);
						}
						if (x % 8 == 0 && z % 8 == 0) c.set(px, posY + 5, pz, lamp(Direction.UP));
					}
				}
			}
		}
	}

	static class Helipad extends Part {
		Helipad(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
			super(minX, minY, minZ, maxX, maxY, maxZ);
		}

		@Override
		public void place(Canvas c, int posX, int posY, int posZ, int sizeX, int sizeY, int sizeZ, int direction, Random rnd) {
			if (sizeX < 7 || sizeZ < 7) return;
			if (sizeX > 9) {
				posX += (sizeX - 9) / 2;
				sizeX = 9;
			}
			if (sizeZ > 9) {
				posZ += (sizeZ - 9) / 2;
				sizeZ = 9;
			}
			BlockState frame = TGBlocks.STEELFRAME_DARK.get().defaultBlockState();
			if (sizeX == 9 && sizeZ == 9) {
				for (int z = 1; z < 8; z++) {
					c.set(posX, posY, posZ + z, frame);
					c.set(posX + 8, posY, posZ + z, frame);
				}
				posX++;
				for (int x = 0; x < 7; x++) {
					c.set(posX + x, posY, posZ, frame);
					c.set(posX + x, posY, posZ + 8, frame);
				}
				posZ++;
			}
			for (int x = 0; x < 7; x++) {
				for (int z = 0; z < 7; z++) {
					boolean edge = x == 0 || z == 0 || x == 6 || z == 6;
					c.set(posX + x, posY + 1, posZ + z, edge ? Blocks.YELLOW_CONCRETE.defaultBlockState() : TGBlocks.CONCRETE_GREY_DARK.get().defaultBlockState());
				}
			}
			BlockState glow = TGBlocks.NEONSQUARE_WHITE.get().defaultBlockState();
			//an H
			for (int i = 2; i <= 4; i++) {
				c.set(posX + i, posY + 1, posZ + 2, glow);
				c.set(posX + i, posY + 1, posZ + 4, glow);
			}
			c.set(posX + 3, posY + 1, posZ + 3, glow);
		}
	}

	static class Tanks extends Part {
		static final int WIDTH = 4;

		Tanks(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
			super(minX, minY, minZ, maxX, maxY, maxZ);
		}

		@Override
		public void place(Canvas c, int posX, int posY, int posZ, int sizeX, int sizeY, int sizeZ, int direction, Random rnd) {
			BlockState base = TGBlocks.STEELFRAME_SCAFFOLD.get().defaultBlockState();
			BlockState wall = TGBlocks.CONTAINER_RED.get().defaultBlockState();
			BlockState border = TGBlocks.CONTAINER_ORANGE.get().defaultBlockState();
			BlockState lava = Blocks.LAVA.defaultBlockState();
			//1.12 wrote the x-long and z-long tanks out separately; here a is along the tank and b across it
			boolean alongX = sizeX > sizeZ;
			int len = alongX ? sizeX : sizeZ;
			int count = (int) (((alongX ? sizeZ : sizeX) + 1.0f) / (WIDTH + 1.0f));
			for (int i = 0, offset = 0; i < count; i++, offset += WIDTH + 1) {
				final int o = offset;
				TankWriter w = (a, y, b, s) -> {
					if (alongX) c.set(posX + a, posY + y, posZ + o + b, s);
					else c.set(posX + o + b, posY + y, posZ + a, s);
				};
				for (int y = 1; y <= 2; y++) {
					w.set(1, y, 0, base);
					w.set(len - 2, y, 0, base);
					w.set(1, y, WIDTH - 1, base);
					w.set(len - 2, y, WIDTH - 1, base);
				}
				for (int a = 1; a < len - 1; a++) {
					w.set(a, 1, 1, base);
					w.set(a, 1, 2, base);
					w.set(a, 2, 1, wall);
					w.set(a, 2, 2, wall);
					for (int y = 3; y <= 4; y++) {
						w.set(a, y, 0, wall);
						w.set(a, y, 3, wall);
						w.set(a, y, 1, lava);
						w.set(a, y, 2, lava);
					}
					w.set(a, 5, 1, wall);
					w.set(a, 5, 2, wall);
				}
				for (int a : new int[]{0, len - 1}) {
					for (int b = 1; b <= 2; b++) {
						w.set(a, 2, b, border);
						w.set(a, 3, b, wall);
						w.set(a, 4, b, wall);
						w.set(a, 5, b, border);
					}
					for (int y = 3; y <= 4; y++) {
						w.set(a, y, 0, border);
						w.set(a, y, 3, border);
					}
				}
			}
		}

		private interface TankWriter {
			void set(int a, int y, int b, BlockState state);
		}
	}

	static class WatchTower extends Part {
		final int towerSize;

		WatchTower(int minX, int minY, int minZ, int maxX, int maxY, int maxZ, int towerSize) {
			super(minX, minY, minZ, maxX, maxY, maxZ);
			this.towerSize = towerSize;
		}

		@Override
		public void place(Canvas c, int posX, int posY, int posZ, int sizeX, int sizeY, int sizeZ, int direction, Random rnd) {
			int t = towerSize;
			if (sizeX >= t + 2) posX += (sizeX - t) / 2;
			if (sizeZ >= t + 2) posZ += (sizeZ - t) / 2;
			BlockState fence = Blocks.OAK_FENCE.defaultBlockState();
			for (int y = 1; y < sizeY - 1; y++) {
				c.set(posX, posY + y, posZ, fence);
				c.set(posX + t - 1, posY + y, posZ, fence);
				c.set(posX, posY + y, posZ + t - 1, fence);
				c.set(posX + t - 1, posY + y, posZ + t - 1, fence);
				if (y < sizeY - 3) c.fill(posX + 1, posY + y, posZ + 1, t - 2, 1, t - 2, Blocks.OAK_PLANKS.defaultBlockState());
			}
			for (int y = sizeY - 4; y < sizeY - 2; y++) { //1.12 fillBlocksHollow
				for (int i = 0; i < t; i++) {
					for (int j = 0; j < t; j++) {
						if (i == 0 || j == 0 || i == t - 1 || j == t - 1) c.set(posX + i, posY + y, posZ + j, fence);
					}
				}
			}
			c.fill(posX, posY + sizeY - 1, posZ, t, 1, t, c.camo.net(true));
			int xo, zo;
			Direction ladder;
			switch (direction) {
				case 0 -> { xo = 0; zo = 1; ladder = Direction.WEST; }
				case 1 -> { xo = 1; zo = 0; ladder = Direction.NORTH; }
				case 2 -> { xo = t - 1; zo = t - 2; ladder = Direction.EAST; }
				default -> { xo = t - 2; zo = t - 1; ladder = Direction.SOUTH; }
			}
			for (int y = 1; y < sizeY - 3; y++) {
				c.set(posX + xo, posY + y, posZ + zo, Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, ladder));
			}
			c.set(posX + xo, posY + sizeY - 3, posZ + zo, Blocks.OAK_FENCE_GATE.defaultBlockState().setValue(FenceGateBlock.FACING, facing(direction)));
		}
	}
}
