package techguns.gametest;

import java.util.List;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import techguns.TGComponents;
import techguns.TGEntities;
import techguns.TGItems;
import techguns.TGuns;
import techguns.Techguns;
import techguns.items.guns.GenericGun;
import techguns.items.guns.ammo.AmmoType;

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

		gun.shootGunPrimary(stack, helper.getLevel(), player, InteractionHand.MAIN_HAND, false);
		helper.assertTrue(gun.getAmmo(stack) == 0, "shot didn't use a round");
		helper.startSequence()
				.thenWaitUntil(() -> helper.assertTrue(zombie.getHealth() < zombie.getMaxHealth(), "zombie wasn't hit"))
				.thenExecuteAfter(5, () -> gun.shootGunPrimary(stack, helper.getLevel(), player, InteractionHand.MAIN_HAND, false))
				.thenExecute(() -> {
					helper.assertTrue(gun.getAmmo(stack) == gun.getClipsize(), "didn't reload");
					helper.assertTrue(player.getInventory().countItem(TGItems.PISTOL_MAGAZINE_EMPTY.get()) == 1, "no empty magazine back");
					helper.assertTrue(player.getInventory().countItem(TGItems.PISTOL_MAGAZINE.get()) == 0, "magazine not used");
				})
				.thenSucceed();
	}

	/** One trigger pull fires the slug and 7 pellets; the reload key tops the tube up from loose shells. */
	@GameTest(template = "empty")
	public static void shotgunFiresPelletsAndLoadsShells(GameTestHelper helper) {
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		Vec3 pos = helper.absoluteVec(new Vec3(1.5, 1, 0.5));
		player.moveTo(pos.x, pos.y, pos.z, 0, 0);
		ItemStack stack = new ItemStack(TGuns.COMBATSHOTGUN.get());
		stack.set(TGComponents.AMMO, 2);
		player.setItemInHand(InteractionHand.MAIN_HAND, stack);
		player.getInventory().add(new ItemStack(TGItems.SHOTGUN_ROUNDS.get(), 10));
		GenericGun gun = TGuns.COMBATSHOTGUN.get();

		gun.shootGunPrimary(stack, helper.getLevel(), player, InteractionHand.MAIN_HAND, false);
		helper.assertTrue(helper.getEntities(TGEntities.GENERIC_PROJECTILE.get()).size() == 8, "expected 8 pellets");
		helper.runAfterDelay(15, () -> {
			gun.tryForcedReload(stack, helper.getLevel(), player, InteractionHand.MAIN_HAND);
			helper.assertTrue(gun.getAmmo(stack) == 8, "tube not full");
			helper.assertTrue(player.getInventory().countItem(TGItems.SHOTGUN_ROUNDS.get()) == 3, "should use 7 shells");
			helper.succeed();
		});
	}

	/** Gun + incendiary shells in the crafting grid gives the gun switched to incendiary, with one shell loaded. */
	@GameTest(template = "empty")
	public static void ammoChangeRecipeSetsVariant(GameTestHelper helper) {
		CraftingInput input = CraftingInput.of(2, 1,
				List.of(new ItemStack(TGuns.COMBATSHOTGUN.get()), new ItemStack(TGItems.SHOTGUN_ROUNDS_INCENDIARY.get())));
		ItemStack out = helper.getLevel().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, helper.getLevel())
				.orElseThrow(() -> new GameTestAssertException("no ammo change recipe"))
				.value().assemble(input, helper.getLevel().registryAccess());
		helper.assertTrue(AmmoType.INCENDIARY.equals(out.get(TGComponents.AMMO_VARIANT)), "variant not set");
		helper.assertTrue(out.get(TGComponents.AMMO) == 1, "should load one shell");
		helper.succeed();
	}
}
