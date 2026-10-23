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
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import techguns.Techguns;

/**
 * 1.12 TGSpawnManager: the biome modifiers in data/techguns/neoforge/biome_modifier spawn {@link Dummy}s like any monster;
 * each is replaced on joining the world by an NPC picked by weight from the entries up to the spot's danger level:
 * distance from world spawn (0 within 500 blocks, 1 within 1000, 2 within 2500, else 3; half that in the nether) plus 1-2 for harsh biomes.
 * Unlike 1.12, gun soldiers spawn at any danger and twice as often, so guns show up near spawn too.
 * ponytail: weights, distances and the biome blacklist were 1.12 config; they come back with the M8 config.
 */
@EventBusSubscriber(modid = Techguns.MODID)
public class TGSpawnManager {
	record Spawn(Supplier<? extends EntityType<? extends GenericNPC>> type, int weight, int danger) {}

	static final List<Spawn> OVERWORLD = List.of(new Spawn(TGNpcs.ZOMBIE_SOLDIER, 200, 0), new Spawn(TGNpcs.ZOMBIE_FARMER, 200, 0),
			new Spawn(TGNpcs.ZOMBIE_MINER, 200, 0), new Spawn(TGNpcs.SKELETON_SOLDIER, 200, 0), new Spawn(TGNpcs.PSYCHO_STEVE, 3, 1),
			new Spawn(TGNpcs.BANDIT, 100, 2));
	static final List<Spawn> NETHER = List.of(new Spawn(TGNpcs.ZOMBIE_PIGMAN_SOLDIER, 100, 0), new Spawn(TGNpcs.CYBER_DEMON, 30, 0));

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
		int danger = danger(level, dummy.blockPosition());
		List<Spawn> table = level.dimension() == Level.NETHER ? NETHER : OVERWORLD;
		int roll = level.random.nextInt(table.stream().filter(s -> s.danger <= danger).mapToInt(Spawn::weight).sum());
		for (Spawn spawn : table) {
			if (spawn.danger > danger || (roll -= spawn.weight) >= 0) continue;
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
		int danger = distance < 500 * factor ? 0 : distance < 1000 * factor ? 1 : distance < 2500 * factor ? 2 : 3;
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
