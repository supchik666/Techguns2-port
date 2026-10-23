package techguns.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import techguns.Techguns;
import techguns.capabilities.TGExtendedPlayer;
import techguns.entities.projectiles.FlamethrowerProjectile;
import techguns.entities.projectiles.GenericProjectile;
import techguns.entities.projectiles.LaserProjectile;
import techguns.entities.projectiles.TeslaProjectile;

/** Beam projectiles hit on the tick they're made. */
@GameTestHolder(Techguns.MODID)
@PrefixGameTestTemplate(false)
public class EnergyProjectileTests {
	private static Player shooter(GameTestHelper helper) {
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		Vec3 pos = helper.absoluteVec(new Vec3(1.5, 1, 0.5));
		player.moveTo(pos.x, pos.y, pos.z, 0, 0); // yaw 0 faces +z
		player.setYHeadRot(0);
		return player;
	}

	@GameTest(template = "empty")
	public static void laserHitsZombie(GameTestHelper helper) {
		Zombie zombie = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new Vec3(1.5, 1, 3.5));
		Player player = shooter(helper);
		helper.getLevel().addFreshEntity(new LaserProjectile(helper.getLevel(), player, GenericProjectile.Kind.BULLET, 5, 100, 20, 0, 100, 100, 5,
				GenericProjectile.CENTER, 0));
		helper.assertTrue(zombie.getHealth() < zombie.getMaxHealth(), "laser didn't hit");
		helper.succeed();
	}

	/** The bolt jumps from the zombie it hits to the one beside it. */
	@GameTest(template = "empty")
	public static void teslaChains(GameTestHelper helper) {
		Zombie first = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new Vec3(1.5, 1, 3.5));
		Zombie second = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new Vec3(0.5, 1, 4.5));
		Player player = shooter(helper);
		helper.getLevel().addFreshEntity(new TeslaProjectile(helper.getLevel(), player, GenericProjectile.Kind.BULLET, 5, 100, 20, 0, 100, 100, 5,
				GenericProjectile.CENTER, 0));
		helper.assertTrue(first.getHealth() < first.getMaxHealth(), "bolt didn't hit");
		helper.assertTrue(second.getHealth() < second.getMaxHealth(), "bolt didn't chain");
		helper.succeed();
	}

	/** Flames at a plank wall: fire appears in front of it unless the shooter is in safe mode. */
	private static void flames(GameTestHelper helper, boolean safemode) {
		for (int x = 0; x < 3; x++) for (int y = 1; y < 4; y++) helper.setBlock(new BlockPos(x, y, 3), Blocks.OAK_PLANKS);
		Player player = shooter(helper);
		TGExtendedPlayer.get(player).enableSafemode = safemode;
		for (int i = 0; i < 20; i++) {
			helper.getLevel().addFreshEntity(new FlamethrowerProjectile(helper.getLevel(), player, GenericProjectile.Kind.BULLET, 1, 1, 20, 0, 10, 10, 1,
					GenericProjectile.CENTER, 0));
		}
		helper.runAfterDelay(10, () -> {
			boolean fire = false;
			for (int x = 0; x < 3; x++) for (int y = 1; y < 4; y++) fire |= helper.getBlockState(new BlockPos(x, y, 2)).is(Blocks.FIRE);
			helper.assertTrue(fire != safemode, safemode ? "flames burned blocks in safe mode" : "flames lit no fire");
			helper.succeed();
		});
	}

	@GameTest(template = "empty")
	public static void flamesIgniteBlocks(GameTestHelper helper) {
		flames(helper, false);
	}

	@GameTest(template = "empty")
	public static void flamesSpareBlocksInSafemode(GameTestHelper helper) {
		flames(helper, true);
	}
}
