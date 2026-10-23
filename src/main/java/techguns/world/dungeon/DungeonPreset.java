package techguns.world.dungeon;

import java.util.List;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import com.mojang.serialization.Codec;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.storage.loot.LootTable;
import techguns.Techguns;
import techguns.entities.npcs.TGNpcs;

/** The 1.12 dungeon presets (PresetCastle, PresetNetherDungeonUnderground, PresetTechFortress): templates per level, maze tuning, loot, mobs. */
public enum DungeonPreset implements StringRepresentable {
	CASTLE("chests/castle", List.of(TGNpcs.ZOMBIE_SOLDIER::get, TGNpcs.SKELETON_SOLDIER::get)),
	NETHER("chests/castle", List.of(TGNpcs.ZOMBIE_SOLDIER::get, TGNpcs.SKELETON_SOLDIER::get)),
	TECH_FORTRESS(null, List.of(TGNpcs.STORM_TROOPER::get));

	public static final Codec<DungeonPreset> CODEC = StringRepresentable.fromEnum(DungeonPreset::values);

	@Nullable
	public final ResourceKey<LootTable> lootTable;
	public final List<Supplier<EntityType<?>>> mobs;

	DungeonPreset(@Nullable String lootTable, List<Supplier<EntityType<?>>> mobs) {
		this.lootTable = lootTable == null ? null : ResourceKey.create(Registries.LOOT_TABLE, Techguns.id(lootTable));
		this.mobs = mobs;
	}

	@Override
	public String getSerializedName() {
		return name().toLowerCase();
	}

	/** Segment edge length and height in blocks; every 1.12 template is 5x5x5. */
	public static final int SIZE_XZ = 5, SIZE_Y = 5;

	/** 1.12 initDungeonPath. */
	void init(MazeDungeonPath p) {
		switch (this) {
			case CASTLE, TECH_FORTRESS -> {
				p.startHeightLevel = 1;
				p.chanceStraight = 0.8f;
				p.chanceRamp = 0.5f;
				p.chanceRoom = 0.25f;
				p.chanceFork = 0.4f;
				p.chanceUp = 0.65f;
				p.useRoof = true;
			}
			case NETHER -> {
				p.startHeightLevel = -1;
				p.chanceStraight = 0.7f;
				p.chanceRamp = 0.4f;
				p.chanceRoom = 0.35f;
				p.chanceFork = 0.5f;
				p.chanceUp = 0.6f;
				p.usePillars = false;
				p.useBottomLayer = true;
				p.entranceRampLength = 3;
			}
		}
	}

	/** 1.12 getSegment: the template for a segment at level y (-1 = roof) of a dungeon yMax levels high. */
	String template(int y, int yMax, boolean segmentAbove) {
		return switch (this) {
			case CASTLE -> y == -1 ? "ncroof1" : y == 0 ? "ncdung1" : y <= 1 ? "nclower1" : y < yMax - 2 ? "ncmid1" : y < yMax - 1 ? "ncupper1" : "nctop1";
			case NETHER -> y == 0 ? "nether_b0" : y <= 1 ? "nether_b1" : y <= 2 ? "nether0" : "nether1";
			case TECH_FORTRESS -> !segmentAbove && y > 0 ? "tfupper" : "tfmain";
		};
	}
}
