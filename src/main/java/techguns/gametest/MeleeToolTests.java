package techguns.gametest;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import techguns.TGComponents;
import techguns.TGItems;
import techguns.TGuns;
import techguns.Techguns;

/** Run with ./gradlew runGameTestServer. "empty" is a 3x3x5 air structure in data/techguns/structure. */
@GameTestHolder(Techguns.MODID)
@PrefixGameTestTemplate(false)
public class MeleeToolTests {
	/** Heads go on in order: the obsidian head fits a plain hammer, the carbon one only after it. */
	@GameTest(template = "empty")
	public static void upgradeRecipeFitsHeadsInOrder(GameTestHelper helper) {
		ItemStack hammer = new ItemStack(TGuns.POWERHAMMER.get());
		helper.assertTrue(craft(helper, hammer, new ItemStack(TGItems.POWERHAMMERHEAD_CARBON.get())).isEmpty(), "carbon head fit a plain hammer");
		ItemStack obsidian = craft(helper, hammer, new ItemStack(TGItems.POWERHAMMERHEAD_OBSIDIAN.get()));
		helper.assertTrue(obsidian.get(TGComponents.MINING_HEAD) == 1, "obsidian head not fitted");
		ItemStack carbon = craft(helper, obsidian, new ItemStack(TGItems.POWERHAMMERHEAD_CARBON.get()));
		helper.assertTrue(carbon.get(TGComponents.MINING_HEAD) == 2, "carbon head not fitted");
		helper.succeed();
	}

	private static ItemStack craft(GameTestHelper helper, ItemStack tool, ItemStack head) {
		CraftingInput input = CraftingInput.of(2, 1, List.of(tool, head));
		return helper.getLevel().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, helper.getLevel())
				.map(r -> r.value().assemble(input, helper.getLevel().registryAccess())).orElse(ItemStack.EMPTY);
	}

	/** Mining the middle of a stone wall with the drill takes out the whole 3x3 for one shot of fuel. */
	@GameTest(template = "empty")
	public static void drillMines3x3(GameTestHelper helper) {
		for (int x = 0; x < 3; x++) {
			for (int y = 0; y < 3; y++) helper.setBlock(x, y, 3, Blocks.STONE);
		}
		// a FakePlayer drops packets; the mock player can't take the extended player attachment's sync
		ServerPlayer player = FakePlayerFactory.getMinecraft(helper.getLevel());
		player.setGameMode(GameType.SURVIVAL);
		Vec3 pos = helper.absoluteVec(new Vec3(1.5, 0, 0.5));
		player.moveTo(pos.x, pos.y, pos.z, 0, 0); // eyes at y 1.62, looking along +z at the wall's middle
		ItemStack drill = new ItemStack(TGuns.MININGDRILL.get());
		player.setItemInHand(InteractionHand.MAIN_HAND, drill);
		int ammo = TGuns.MININGDRILL.get().getAmmo(drill);

		player.gameMode.destroyBlock(helper.absolutePos(new BlockPos(1, 1, 3)));
		helper.getLevel().getServer().getPlayerList().remove(player);
		for (int x = 0; x < 3; x++) {
			for (int y = 0; y < 3; y++) helper.assertBlockPresent(Blocks.AIR, x, y, 3);
		}
		helper.assertTrue(TGuns.MININGDRILL.get().getAmmo(drill) == ammo - 1, "should use one shot of fuel");
		helper.succeed();
	}
}
