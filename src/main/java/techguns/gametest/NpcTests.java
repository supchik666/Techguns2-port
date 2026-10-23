package techguns.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.phys.Vec3;
import net.minecraft.resources.ResourceKey;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import techguns.TGItems;
import techguns.TGuns;
import techguns.Techguns;
import techguns.entities.npcs.ArmySoldier;
import techguns.entities.npcs.GenericNPC;
import techguns.entities.npcs.TGNpcs;
import techguns.entities.npcs.ZombieFarmer;
import techguns.entities.projectiles.GenericProjectile;

/** The GenericNPC mobs: gun AI, sun burn, spawn eggs and natural spawns. */
@GameTestHolder(Techguns.MODID)
@PrefixGameTestTemplate(false)
public class NpcTests {
	/** A soldier given an M4 and a target shoots it with gun projectiles. */
	@GameTest(template = "empty", timeoutTicks = 400)
	public static void soldierShootsTarget(GameTestHelper helper) {
		Pig pig = helper.spawnWithNoFreeWill(EntityType.PIG, new Vec3(1.5, 1, 4.5));
		ArmySoldier soldier = helper.spawn(TGNpcs.ARMY_SOLDIER.get(), new Vec3(1.5, 1, 0.5));
		soldier.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(TGuns.M4.get()));
		soldier.setTarget(pig);
		helper.succeedWhen(() -> helper.assertTrue(pig.getLastDamageSource() != null
				&& pig.getLastDamageSource().getDirectEntity() instanceof GenericProjectile, "pig not shot"));
	}

	/** Zombie NPCs are undead and burn in daylight. */
	@GameTest(template = "empty", timeoutTicks = 400, skyAccess = true)
	public static void zombieBurnsInDaylight(GameTestHelper helper) {
		helper.getLevel().setDayTime(6000);
		ZombieFarmer zombie = helper.spawnWithNoFreeWill(TGNpcs.ZOMBIE_FARMER.get(), new Vec3(1.5, 1, 1.5));
		helper.assertTrue(zombie.getType().is(EntityTypeTags.UNDEAD), "not undead");
		helper.succeedWhen(() -> helper.assertTrue(zombie.isOnFire(), "not burning"));
	}

	/** Every spawn egg spawns its own NPC, armed. */
	@GameTest(template = "empty")
	public static void spawnEggsSpawnArmedNpcs(GameTestHelper helper) {
		BlockPos pos = helper.absolutePos(new BlockPos(1, 1, 1));
		int eggs = 0;
		for (var holder : TGItems.ITEMS.getEntries()) {
			if (!(holder.get() instanceof DeferredSpawnEggItem egg)) continue;
			ItemStack stack = new ItemStack(egg);
			Entity entity = egg.getType(stack).spawn(helper.getLevel(), stack, null, pos, MobSpawnType.SPAWN_EGG, true, false);
			String id = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).getPath();
			helper.assertTrue((id + "_spawn_egg").equals(holder.getId().getPath()), holder.getId() + " spawned " + id);
			helper.assertTrue(!(entity instanceof GenericNPC npc) || !npc.getMainHandItem().isEmpty(), id + " is unarmed"); // the special mobs carry no gun
			entity.discard();
			eggs++;
		}
		helper.assertTrue(eggs == 20, "expected 20 eggs, found " + eggs);
		helper.succeed();
	}

	/** The biome modifiers add the placeholder spawn, which turns into an armed NPC on joining the world. */
	@GameTest(template = "empty")
	public static void naturalSpawnsBecomeNpcs(GameTestHelper helper) {
		assertDummySpawn(helper, Biomes.PLAINS, 1000);
		helper.assertTrue(net.minecraft.world.entity.MobCategory.MONSTER.getMaxInstancesPerChunk() == 105, "monster cap isn't raised");
		assertDummySpawn(helper, Biomes.NETHER_WASTES, 500);
		helper.spawn(TGNpcs.DUMMY_SPAWN.get(), new Vec3(1.5, 1, 1.5));
		helper.assertEntityNotPresent(TGNpcs.DUMMY_SPAWN.get());
		var npcs = helper.getLevel().getEntitiesOfClass(GenericNPC.class, helper.getBounds());
		helper.assertTrue(npcs.size() == 1 && !npcs.get(0).getMainHandItem().isEmpty(), "expected one armed NPC, found " + npcs);
		helper.succeed();
	}

	private static void assertDummySpawn(GameTestHelper helper, ResourceKey<Biome> key, int weight) {
		Biome biome = helper.getLevel().registryAccess().registryOrThrow(Registries.BIOME).getOrThrow(key);
		helper.assertTrue(biome.getMobSettings().getMobs(MobCategory.MONSTER).unwrap().stream()
				.anyMatch(s -> s.type == TGNpcs.DUMMY_SPAWN.get() && s.getWeight().asInt() == weight), key.location() + " lacks the NPC spawn");
	}
}
