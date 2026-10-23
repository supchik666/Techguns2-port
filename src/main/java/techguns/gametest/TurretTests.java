package techguns.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import techguns.TGBlocks;
import techguns.TGComponents;
import techguns.TGEntities;
import techguns.TGItems;
import techguns.TGuns;
import techguns.Techguns;
import techguns.entities.npcs.NPCTurret;
import techguns.machines.TurretBlockEntity;

/** The NPC turret: shooting with the base's ammo, sparing the owner and pets, and going away with its block. */
@GameTestHolder(Techguns.MODID)
@PrefixGameTestTemplate(false)
public class TurretTests {
	private static final BlockPos POS = new BlockPos(1, 1, 1);

	/** A powered turret base with an empty pistol and two magazines. */
	private static TurretBlockEntity turret(GameTestHelper helper) {
		helper.setBlock(POS, TGBlocks.TURRET.get());
		TurretBlockEntity be = helper.getBlockEntity(POS);
		be.energy.set(be.energy.getMaxEnergyStored());
		ItemStack pistol = new ItemStack(TGuns.PISTOL.get());
		pistol.set(TGComponents.AMMO, 0);
		be.inventory.setStackInSlot(TurretBlockEntity.SLOT_WEAPON, pistol);
		be.inventory.setStackInSlot(0, new ItemStack(TGItems.PISTOL_MAGAZINE.get(), 2));
		return be;
	}

	/** The turret spawns on its base, shoots a husk dead and reloads from the base, handing back the empty magazine. */
	@GameTest(template = "empty8", timeoutTicks = 400)
	public static void turretKillsHostileWithBaseAmmo(GameTestHelper helper) {
		TurretBlockEntity be = turret(helper);
		Husk husk = helper.spawnWithNoFreeWill(EntityType.HUSK, new Vec3(1.5, 1, 6.5));
		helper.succeedWhen(() -> {
			helper.assertEntityPresent(TGEntities.TURRET.get());
			helper.assertTrue(!husk.isAlive(), "husk alive");
			helper.assertTrue(be.inventory.getStackInSlot(0).getCount() == 1, "magazine not used");
			helper.assertTrue(be.inventory.getStackInSlot(9).is(TGItems.PISTOL_MAGAZINE_EMPTY.get()), "no empty magazine in the output");
		});
	}

	/** With PvP on everyone but the owner, it targets a stranger, never its owner; it spares tamed pets and shoots animals only when told to. */
	@GameTest(template = "empty8")
	public static void turretSparesOwnerAndPets(GameTestHelper helper) {
		TurretBlockEntity be = turret(helper);
		Player owner = helper.makeMockPlayer(GameType.SURVIVAL);
		Player stranger = helper.makeMockPlayer(GameType.SURVIVAL);
		be.owner = owner.getUUID();
		Pig pig = helper.spawnWithNoFreeWill(EntityType.PIG, new Vec3(4.5, 1, 4.5));
		Wolf wolf = helper.spawnWithNoFreeWill(EntityType.WOLF, new Vec3(5.5, 1, 4.5));
		wolf.tame(owner);
		helper.runAfterDelay(2, () -> {
			NPCTurret turret = be.turret();
			helper.assertTrue(turret != null, "no turret");
			helper.assertTrue(!turret.isTarget(stranger) && !turret.isTarget(pig), "targets without PvP or animal targeting");
			be.pvp = 4;
			be.attackAnimals = true;
			helper.assertTrue(turret.isTarget(stranger), "spares a stranger with PvP on");
			helper.assertTrue(!turret.isTarget(owner), "targets its owner");
			helper.assertTrue(turret.isTarget(pig), "spares animals with animal targeting on");
			helper.assertTrue(!turret.isTarget(wolf), "targets a tamed wolf");
			helper.succeed();
		});
	}

	/** Breaking the base drops its gun, ammo and armor, and takes the turret with it. */
	@GameTest(template = "empty8")
	public static void breakingBaseDropsContentsAndRemovesTurret(GameTestHelper helper) {
		TurretBlockEntity be = turret(helper);
		be.inventory.setStackInSlot(TurretBlockEntity.SLOT_ARMOR, new ItemStack(TGItems.TURRET_ARMOR_STEEL.get()));
		helper.runAfterDelay(2, () -> {
			NPCTurret turret = be.turret();
			helper.assertTrue(turret != null && turret.isAlive(), "no turret");
			helper.assertTrue(turret.getTotalArmorAgainstType(techguns.damagesystem.TGDamageType.PROJECTILE) == 10, "steel plate isn't 10 armor");
			helper.destroyBlock(POS);
			helper.assertTrue(turret.isRemoved(), "turret outlived its base");
			helper.assertItemEntityPresent(TGuns.PISTOL.get(), POS, 2);
			helper.assertItemEntityPresent(TGItems.PISTOL_MAGAZINE.get(), POS, 2);
			helper.assertItemEntityPresent(TGItems.TURRET_ARMOR_STEEL.get(), POS, 2);
			helper.succeed();
		});
	}
}
