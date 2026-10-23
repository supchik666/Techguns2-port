package techguns.entities.npcs;

import java.util.List;
import java.util.function.Supplier;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import techguns.TGConfig;
import techguns.Techguns;

/**
 * 1.12 TGSpawnManager: the biome modifiers in data/techguns/neoforge/biome_modifier spawn {@link Dummy}s like any monster;
 * each is replaced on joining the world by an NPC picked by weight from the entries up to the spot's danger level:
 * distance from world spawn (0 within 500 blocks, 1 within 1000, 2 within 2500, else 3; half that in the nether) plus 1-2 for harsh biomes.
 * Unlike 1.12, gun soldiers spawn at any danger and twice as often, so guns show up near spawn too. Weights, distances
 * and a biome blacklist are in {@link TGConfig}.
 */
@EventBusSubscriber(modid = Techguns.MODID)
public class TGSpawnManager {
	record Spawn(Supplier<? extends EntityType<? extends GenericNPC>> type, ModConfigSpec.IntValue weight, int danger) {}

	static final List<Spawn> OVERWORLD = List.of(new Spawn(TGNpcs.ZOMBIE_SOLDIER, TGConfig.SPAWN_ZOMBIE_SOLDIER, 0),
			new Spawn(TGNpcs.ZOMBIE_FARMER, TGConfig.SPAWN_ZOMBIE_FARMER, 0), new Spawn(TGNpcs.ZOMBIE_MINER, TGConfig.SPAWN_ZOMBIE_MINER, 0),
			new Spawn(TGNpcs.SKELETON_SOLDIER, TGConfig.SPAWN_SKELETON_SOLDIER, 0), new Spawn(TGNpcs.PSYCHO_STEVE, TGConfig.SPAWN_PSYCHO_STEVE, 1),
			new Spawn(TGNpcs.BANDIT, TGConfig.SPAWN_BANDIT, 2));
	static final List<Spawn> NETHER = List.of(new Spawn(TGNpcs.ZOMBIE_PIGMAN_SOLDIER, TGConfig.SPAWN_ZOMBIE_PIGMAN_SOLDIER, 0),
			new Spawn(TGNpcs.CYBER_DEMON, TGConfig.SPAWN_CYBER_DEMON, 0));

	/** The placeholder the biome modifiers spawn. */
	public static class Dummy extends Monster {
		public Dummy(EntityType<? extends Dummy> type, Level level) {
			super(type, level);
		}
	}

	@SubscribeEvent
	static void join(EntityJoinLevelEvent event) {
		if (!(event.getEntity() instanceof Dummy dummy) || !(event.getLevel() instanceof ServerLevel level)) return;
		event.setCanceled(true);
		if (!event.loadedFromDisk()) replace(level, dummy);
	}

	private static void replace(ServerLevel level, Entity dummy) {
		if (level.getBiome(dummy.blockPosition()).unwrapKey().map(k -> TGConfig.NPC_BIOME_BLACKLIST.get().contains(k.location().toString())).orElse(false)) return;
		int danger = danger(level, dummy.blockPosition());
		List<Spawn> table = level.dimension() == Level.NETHER ? NETHER : OVERWORLD;
		int total = table.stream().filter(s -> s.danger <= danger).mapToInt(s -> s.weight.get()).sum();
		if (total <= 0) return;
		int roll = level.random.nextInt(total);
		for (Spawn spawn : table) {
			if (spawn.danger > danger || (roll -= spawn.weight.get()) >= 0) continue;
			GenericNPC npc = spawn.type.get().create(level);
			if (npc == null) return;
			npc.moveTo(dummy.getX(), dummy.getY(), dummy.getZ(), dummy.getYRot(), 0);
			npc.onSpawnByManager(danger);
			level.addFreshEntity(npc);
			return;
		}
	}

	public static int danger(ServerLevel level, BlockPos pos) {
		float factor = level.dimension() == Level.NETHER ? 0.5f : 1;
		BlockPos spawn = level.getSharedSpawnPos();
		double distance = Math.sqrt(Math.pow(pos.getX() - spawn.getX(), 2) + Math.pow(pos.getZ() - spawn.getZ(), 2));
		int danger = distance < TGConfig.SPAWN_DISTANCE_0.get() * factor ? 0 : distance < TGConfig.SPAWN_DISTANCE_1.get() * factor ? 1
				: distance < TGConfig.SPAWN_DISTANCE_2.get() * factor ? 2 : 3;
		return danger + biomeDanger(level.getBiome(pos));
	}

	private static int biomeDanger(Holder<Biome> biome) {
		if (biome.is(Tags.Biomes.IS_NETHER) || biome.is(Tags.Biomes.IS_END)) return 0;
		if (biome.is(Tags.Biomes.IS_WASTELAND) || biome.is(Tags.Biomes.IS_SANDY) || biome.is(Tags.Biomes.IS_SNOWY) || biome.is(Tags.Biomes.IS_SWAMP)
				|| biome.is(Tags.Biomes.IS_SPOOKY) || biome.is(Tags.Biomes.IS_DEAD)) return 2;
		if (biome.is(Tags.Biomes.IS_COLD) || biome.is(Tags.Biomes.IS_SPARSE_VEGETATION) || biome.is(Tags.Biomes.IS_SAVANNA)
				|| biome.is(Tags.Biomes.IS_DRY)) return 1;
		return 0;
	}
}
