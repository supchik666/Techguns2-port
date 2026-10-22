package techguns.gametest;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import techguns.Techguns;
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
}
