package techguns.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import techguns.Techguns;
import techguns.entities.projectiles.GenericProjectile;
import techguns.entities.projectiles.GrenadeProjectile;
import techguns.entities.projectiles.RocketProjectile;

@GameTestHolder(Techguns.MODID)
@PrefixGameTestTemplate(false)
public class ExplosiveTests {
	/** A survival player at the south end of the template, facing +z, looking {@code pitch} degrees down. */
	private static Player shooter(GameTestHelper helper, float pitch) {
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		Vec3 pos = helper.absoluteVec(new Vec3(1.5, 1, 0.5));
		player.moveTo(pos.x, pos.y, pos.z, 0, pitch);
		player.setYHeadRot(0);
		return player;
	}

	/** The rocket flies past the zombie into a wall; the blast reaches the zombie and breaks the wall. */
	@GameTest(template = "empty")
	public static void rocketBlastHitsZombie(GameTestHelper helper) {
		BlockPos wall = new BlockPos(1, 2, 4);
		helper.setBlock(wall, Blocks.STONE);
		Zombie zombie = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new Vec3(0.5, 1, 3.5));
		helper.getLevel().addFreshEntity(RocketProjectile.create(helper.getLevel(), shooter(helper, 0), GenericProjectile.Kind.BULLET, 20, 0.5f, 100, 0,
				3, 5, 10, GenericProjectile.CENTER, 0));
		helper.succeedWhen(() -> {
			helper.assertTrue(zombie.getHealth() < zombie.getMaxHealth(), "blast didn't hurt the zombie");
			helper.assertBlockPresent(Blocks.AIR, wall);
		});
	}

	/** Thrown at the floor, the grenade bounces three times, then explodes on the fourth impact. */
	@GameTest(template = "empty", timeoutTicks = 200)
	public static void grenadeExplodesAfterBounces(GameTestHelper helper) {
		Zombie zombie = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new Vec3(1.5, 1, 2.5));
		zombie.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 400)); // daylight would burn it, hurting it early
		helper.getLevel().addFreshEntity(new GrenadeProjectile(helper.getLevel(), shooter(helper, 90), GenericProjectile.Kind.BULLET, 10, 0.75f, 200, 0,
				3, 5, 5, GenericProjectile.CENTER, 0, 1.0f, 1));
		helper.runAfterDelay(2, () -> helper.assertTrue(zombie.getHealth() == zombie.getMaxHealth(), "exploded on the first bounce"));
		helper.succeedWhen(() -> helper.assertTrue(zombie.getHealth() < zombie.getMaxHealth(), "grenade didn't explode"));
	}
}
