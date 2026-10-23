package techguns.gametest;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import techguns.TGComponents;
import techguns.TGItems;
import techguns.TGuns;
import techguns.Techguns;
import techguns.capabilities.TGExtendedPlayer;
import techguns.events.TGPlayerEvents;
import techguns.items.guns.GenericGun;

/** The extra equipment slots (M4). */
@GameTestHolder(Techguns.MODID)
@PrefixGameTestTemplate(false)
public class PlayerSlotTests {
	/** Slots and toggles survive a save and load. */
	@GameTest(template = "empty")
	public static void slotsSaveAndLoad(GameTestHelper helper) {
		TGExtendedPlayer tg = new TGExtendedPlayer();
		tg.inventory.setItem(TGExtendedPlayer.SLOT_BACK, new ItemStack(TGItems.JETPACK.get()));
		tg.inventory.setItem(TGExtendedPlayer.SLOTS_AMMO_START, new ItemStack(TGItems.PISTOL_MAGAZINE.get(), 3));
		tg.enableSafemode = true;
		tg.enableJetpack = false;
		tg.foodleft = 4;

		TGExtendedPlayer loaded = new TGExtendedPlayer();
		loaded.deserializeNBT(helper.getLevel().registryAccess(), tg.serializeNBT(helper.getLevel().registryAccess()));
		helper.assertTrue(loaded.getSlot(TGExtendedPlayer.SLOT_BACK).is(TGItems.JETPACK.get()), "back slot lost");
		helper.assertTrue(loaded.getSlot(TGExtendedPlayer.SLOTS_AMMO_START).getCount() == 3, "ammo slot lost");
		helper.assertTrue(loaded.enableSafemode && !loaded.enableJetpack && loaded.foodleft == 4, "toggles lost");
		helper.succeed();
	}

	/** A worn gas mask filters poison, using a charge. */
	@GameTest(template = "empty")
	public static void gasMaskFiltersPoison(GameTestHelper helper) {
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		TGExtendedPlayer tg = TGExtendedPlayer.get(player);
		tg.inventory.setItem(TGExtendedPlayer.SLOT_FACE, new ItemStack(TGItems.GAS_MASK.get()));
		player.addEffect(new MobEffectInstance(MobEffects.POISON, 200));

		TGPlayerEvents.tickSlots(player, tg);
		helper.assertFalse(player.hasEffect(MobEffects.POISON), "still poisoned");
		helper.assertTrue(tg.getSlot(TGExtendedPlayer.SLOT_FACE).getDamageValue() == 1, "mask not used");
		helper.succeed();
	}

	/** The food slots top up hunger; what the meal had beyond that is kept for later. */
	@GameTest(template = "empty")
	public static void autofeedKeepsLeftovers(GameTestHelper helper) {
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		TGExtendedPlayer tg = TGExtendedPlayer.get(player);
		tg.inventory.setItem(TGExtendedPlayer.SLOTS_AUTOFOOD_START, new ItemStack(Items.COOKED_BEEF)); // 8 nutrition
		player.getFoodData().setFoodLevel(15);

		TGPlayerEvents.autofeed(player, tg);
		helper.assertTrue(player.getFoodData().getFoodLevel() == 20, "not fed");
		helper.assertTrue(tg.getSlot(TGExtendedPlayer.SLOTS_AUTOFOOD_START).isEmpty(), "steak not eaten");
		helper.assertTrue(tg.foodleft == 3, "leftover " + tg.foodleft);
		player.getFoodData().setFoodLevel(18);
		TGPlayerEvents.autofeed(player, tg);
		helper.assertTrue(player.getFoodData().getFoodLevel() == 20 && tg.foodleft == 1, "leftover not used");
		helper.succeed();
	}

	/** Reloading takes magazines from the ammo slots and puts the empty one there. */
	@GameTest(template = "empty")
	public static void reloadUsesAmmoSlots(GameTestHelper helper) {
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		TGExtendedPlayer tg = TGExtendedPlayer.get(player);
		tg.inventory.setItem(TGExtendedPlayer.SLOTS_AMMO_START, new ItemStack(TGItems.PISTOL_MAGAZINE.get()));
		ItemStack stack = new ItemStack(TGuns.PISTOL.get());
		stack.set(TGComponents.AMMO, 0);
		player.setItemInHand(InteractionHand.MAIN_HAND, stack);
		GenericGun gun = TGuns.PISTOL.get();

		gun.tryForcedReload(stack, helper.getLevel(), player, InteractionHand.MAIN_HAND);
		helper.assertTrue(gun.getAmmo(stack) == gun.getClipsize(), "didn't reload");
		helper.assertTrue(TGExtendedPlayer.countAmmo(player, TGItems.PISTOL_MAGAZINE.get()) == 0, "magazine not used");
		helper.assertTrue(tg.getSlot(TGExtendedPlayer.SLOTS_AMMO_START).is(TGItems.PISTOL_MAGAZINE_EMPTY.get()), "empty magazine not in the ammo slot");
		helper.succeed();
	}
}
