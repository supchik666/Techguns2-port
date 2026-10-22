package techguns.gametest;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import techguns.TGComponents;
import techguns.TGItems;
import techguns.TGuns;
import techguns.Techguns;
import techguns.items.guns.GenericGun;

/** Run with ./gradlew runGameTestServer. "empty" is a 3x3x5 air structure in data/techguns/structure. */
@GameTestHolder(Techguns.MODID)
@PrefixGameTestTemplate(false)
public class GunTests {
	/** Last round hits a zombie in front; the next trigger pull swaps the magazine. */
	@GameTest(template = "empty")
	public static void pistolShootsAndReloads(GameTestHelper helper) {
		Zombie zombie = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new Vec3(1.5, 1, 3.5));
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		Vec3 pos = helper.absoluteVec(new Vec3(1.5, 1, 0.5));
		player.moveTo(pos.x, pos.y, pos.z, 0, 0); // yaw 0 faces +z, toward the zombie
		player.setYHeadRot(0);
		ItemStack stack = new ItemStack(TGuns.PISTOL.get());
		stack.set(TGComponents.AMMO, 1);
		player.setItemInHand(InteractionHand.MAIN_HAND, stack);
		player.getInventory().add(new ItemStack(TGItems.PISTOL_MAGAZINE.get()));
		GenericGun gun = TGuns.PISTOL.get();

		gun.shootGunPrimary(stack, helper.getLevel(), player, InteractionHand.MAIN_HAND);
		helper.assertTrue(gun.getAmmo(stack) == 0, "shot didn't use a round");
		helper.startSequence()
				.thenWaitUntil(() -> helper.assertTrue(zombie.getHealth() < zombie.getMaxHealth(), "zombie wasn't hit"))
				.thenExecuteAfter(5, () -> gun.shootGunPrimary(stack, helper.getLevel(), player, InteractionHand.MAIN_HAND))
				.thenExecute(() -> {
					helper.assertTrue(gun.getAmmo(stack) == gun.getClipsize(), "didn't reload");
					helper.assertTrue(player.getInventory().countItem(TGItems.PISTOL_MAGAZINE_EMPTY.get()) == 1, "no empty magazine back");
					helper.assertTrue(player.getInventory().countItem(TGItems.PISTOL_MAGAZINE.get()) == 0, "magazine not used");
				})
				.thenSucceed();
	}
}
