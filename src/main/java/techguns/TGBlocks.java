package techguns;

import java.util.function.Function;
import java.util.function.Supplier;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;
import techguns.blocks.CamoNetBlock;
import techguns.blocks.LampBlock;
import techguns.blocks.SandbagsBlock;
import techguns.blocks.WallLanternBlock;
import techguns.machines.MachineBlock;
import techguns.machines.TGBlockEntity;
import techguns.machines.multiblock.MultiBlockController;
import techguns.machines.multiblock.MultiBlockPart;
import techguns.machines.multiblock.MultiBlocks;

/**
 * Simple blocks, flattened from the 1.12 meta blocks. Ids match tools/port/flattening.csv.
 * Copper ore is dropped in favor of vanilla's.
 */
public class TGBlocks {
	public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Techguns.MODID);

	//Ores
	public static final DeferredBlock<Block> ORE_TIN = block("ore_tin", ore(4.0f));
	public static final DeferredBlock<Block> ORE_LEAD = block("ore_lead", ore(6.0f));
	public static final DeferredBlock<Block> ORE_TITANIUM = block("ore_titanium", ore(8.0f));
	public static final DeferredBlock<Block> ORE_URANIUM = block("ore_uranium", ore(7.0f).lightLevel(s -> 4));

	//Metal panels
	public static final DeferredBlock<Block> CONTAINER_RED = block("container_red", metal());
	public static final DeferredBlock<Block> CONTAINER_GREEN = block("container_green", metal());
	public static final DeferredBlock<Block> CONTAINER_BLUE = block("container_blue", metal());
	public static final DeferredBlock<Block> CONTAINER_ORANGE = block("container_orange", metal());
	public static final DeferredBlock<Block> PANEL_LARGE_BORDER = block("panel_large_border", metal());
	public static final DeferredBlock<Block> STEELFRAME_BLUE = block("steelframe_blue", metal());
	public static final DeferredBlock<Block> STEELFRAME_DARK = block("steelframe_dark", metal());
	public static final DeferredBlock<Block> STEELFRAME_SCAFFOLD = block("steelframe_scaffold", metal());

	//Nether metal
	public static final DeferredBlock<Block> NETHERMETAL_PANEL = block("nethermetal_panel", metal());
	public static final DeferredBlock<Block> NETHERMETAL_GRATE1 = block("nethermetal_grate1", metal());
	public static final DeferredBlock<Block> NETHERMETAL_GRATE2 = block("nethermetal_grate2", metal());
	public static final DeferredBlock<Block> NETHERMETAL_GREY_DARK = block("nethermetal_grey_dark", metal());
	public static final DeferredBlock<Block> NETHERMETAL_GREY = block("nethermetal_grey", metal());
	public static final DeferredBlock<Block> NETHERMETAL_GREY_TILES = block("nethermetal_grey_tiles", metal());
	public static final DeferredBlock<Block> NETHERMETAL_BORDER_RED = block("nethermetal_border_red", metal());
	public static final DeferredBlock<Block> NETHERMETAL_PLATE_BLACK = block("nethermetal_plate_black", metal());
	public static final DeferredBlock<Block> NETHERMETAL_PLATE_RED = block("nethermetal_plate_red", metal());
	public static final DeferredBlock<Block> NETHERMETAL_BORDER_LAVA = block("nethermetal_border_lava", metal().lightLevel(s -> 15));

	//Concrete
	public static final DeferredBlock<Block> CONCRETE_BROWN = block("concrete_brown", concrete());
	public static final DeferredBlock<Block> CONCRETE_BROWN_LIGHT = block("concrete_brown_light", concrete());
	public static final DeferredBlock<Block> CONCRETE_GREY = block("concrete_grey", concrete());
	public static final DeferredBlock<Block> CONCRETE_GREY_DARK = block("concrete_grey_dark", concrete());
	public static final DeferredBlock<Block> CONCRETE_BROWN_PIPES = block("concrete_brown_pipes", concrete());
	public static final DeferredBlock<Block> CONCRETE_BROWN_LIGHT_SCAFF = block("concrete_brown_light_scaff", concrete());

	//Neon lights
	public static final DeferredBlock<Block> NEONTUBES2 = block("neontubes2", neon());
	public static final DeferredBlock<Block> NEONTUBES2_ROTATED = block("neontubes2_rotated", neon());
	public static final DeferredBlock<Block> NEONTUBES4 = block("neontubes4", neon());
	public static final DeferredBlock<Block> NEONTUBES4_ROTATED = block("neontubes4_rotated", neon());
	public static final DeferredBlock<Block> NEONSQUARE_WHITE = block("neonsquare_white", neon());

	//Alien bug nest
	public static final DeferredBlock<Block> BUGNEST_SAND = block("bugnest_sand", Properties.of().mapColor(MapColor.SAND).strength(3.0f).sound(SoundType.SAND));
	public static final DeferredBlock<Block> BUGNEST_EGGS = block("bugnest_eggs", Properties.of().mapColor(MapColor.COLOR_GREEN).strength(4.0f).sound(SoundType.SLIME_BLOCK));

	//Ladders
	public static final DeferredBlock<LadderBlock> LADDER_METAL = ladder("ladder_metal");
	public static final DeferredBlock<LadderBlock> LADDER_SHINY = ladder("ladder_shiny");
	public static final DeferredBlock<LadderBlock> LADDER_RUSTY = ladder("ladder_rusty");
	public static final DeferredBlock<LadderBlock> LADDER_CARBON = ladder("ladder_carbon");

	//Stairs
	public static final DeferredBlock<StairBlock> STAIRS_METAL = stairs("stairs_metal", PANEL_LARGE_BORDER, metal());
	public static final DeferredBlock<StairBlock> STAIRS_METAL_CARBON = stairs("stairs_metal_carbon", STEELFRAME_DARK, metal());
	public static final DeferredBlock<StairBlock> STAIRS_CONCRETE = stairs("stairs_concrete", CONCRETE_GREY_DARK, concrete().strength(6.0f));
	public static final DeferredBlock<StairBlock> STAIRS_CONCRETE_LIGHT = stairs("stairs_concrete_light", CONCRETE_BROWN_LIGHT, concrete().strength(6.0f));

	//Lamps
	public static final DeferredBlock<LampBlock> LAMP_YELLOW = withItem("lamp_yellow", LampBlock::new, lamp());
	public static final DeferredBlock<LampBlock> LAMP_WHITE = withItem("lamp_white", LampBlock::new, lamp());
	public static final DeferredBlock<WallLanternBlock> LANTERN_YELLOW = withItem("lantern_yellow", WallLanternBlock::new, lamp());
	public static final DeferredBlock<WallLanternBlock> LANTERN_WHITE = withItem("lantern_white", WallLanternBlock::new, lamp());

	//Camo nets
	public static final DeferredBlock<CamoNetBlock> CAMONET_WOOD = camonet("camonet_wood", false);
	public static final DeferredBlock<CamoNetBlock> CAMONET_DESERT = camonet("camonet_desert", false);
	public static final DeferredBlock<CamoNetBlock> CAMONET_SNOW = camonet("camonet_snow", false);
	public static final DeferredBlock<CamoNetBlock> CAMONET_TOP_WOOD = camonet("camonet_top_wood", true);
	public static final DeferredBlock<CamoNetBlock> CAMONET_TOP_DESERT = camonet("camonet_top_desert", true);
	public static final DeferredBlock<CamoNetBlock> CAMONET_TOP_SNOW = camonet("camonet_top_snow", true);

	public static final DeferredBlock<SandbagsBlock> SANDBAGS = withItem("sandbags", SandbagsBlock::new,
			Properties.of().mapColor(MapColor.COLOR_BROWN).strength(3.0f, 15.0f).sound(SoundType.WOOL));
	public static final DeferredBlock<LadderBlock> SLIMYLADDER = withItem("slimyladder", LadderBlock::new,
			Properties.ofFullCopy(Blocks.LADDER).sound(SoundType.SLIME_BLOCK));

	//Machines
	public static final DeferredBlock<MachineBlock> AMMO_PRESS = machine("ammo_press", () -> TGBlockEntities.AMMO_PRESS.get());
	public static final DeferredBlock<MachineBlock> METAL_PRESS = machine("metal_press", () -> TGBlockEntities.METAL_PRESS.get());
	public static final DeferredBlock<MachineBlock> CHEM_LAB = machine("chem_lab", () -> TGBlockEntities.CHEM_LAB.get());
	public static final DeferredBlock<MachineBlock> CHARGING_STATION = machine("charging_station", () -> TGBlockEntities.CHARGING_STATION.get());
	public static final DeferredBlock<MachineBlock> BLAST_FURNACE = machine("blast_furnace", () -> TGBlockEntities.BLAST_FURNACE.get());
	public static final DeferredBlock<MachineBlock> GRINDER = machine("grinder", () -> TGBlockEntities.GRINDER.get());

	//Multiblock machines (M5.2): controllers, parts and the ore clusters the ore drill mines
	public static final DeferredBlock<MultiBlockController> FABRICATOR_CONTROLLER = withItem("fabricator_controller",
			p -> new MultiBlockController(() -> MultiBlocks.FABRICATOR.get(), p), multiBlock());
	public static final DeferredBlock<MultiBlockPart> FABRICATOR_HOUSING = withItem("fabricator_housing", MultiBlockPart::new, multiBlock());
	public static final DeferredBlock<MultiBlockPart> FABRICATOR_GLASS = withItem("fabricator_glass", MultiBlockPart::new, multiBlock().sound(SoundType.GLASS));
	public static final DeferredBlock<MultiBlockController> REACTION_CHAMBER_CONTROLLER = withItem("reaction_chamber_controller",
			p -> new MultiBlockController(() -> MultiBlocks.REACTION_CHAMBER.get(), p), multiBlock());
	public static final DeferredBlock<MultiBlockPart> REACTION_CHAMBER_HOUSING = withItem("reaction_chamber_housing", MultiBlockPart::new, multiBlock());
	public static final DeferredBlock<MultiBlockPart> REACTION_CHAMBER_GLASS = withItem("reaction_chamber_glass", MultiBlockPart::new,
			multiBlock().sound(SoundType.GLASS));
	public static final DeferredBlock<MultiBlockController> ORE_DRILL_CONTROLLER = withItem("ore_drill_controller",
			p -> new MultiBlockController(() -> MultiBlocks.ORE_DRILL.get(), p), multiBlock());
	public static final DeferredBlock<MultiBlockPart> ORE_DRILL_FRAME = withItem("ore_drill_frame", MultiBlockPart::new, multiBlock());
	public static final DeferredBlock<MultiBlockPart> ORE_DRILL_SCAFFOLD = withItem("ore_drill_scaffold", MultiBlockPart::new, multiBlock());
	public static final DeferredBlock<MultiBlockPart> ORE_DRILL_ROD = withItem("ore_drill_rod", MultiBlockPart::new, multiBlock());
	public static final DeferredBlock<MultiBlockPart> ORE_DRILL_ENGINE = withItem("ore_drill_engine", MultiBlockPart::new, multiBlock());
	public static final DeferredBlock<Block> ORE_CLUSTER_COAL = block("ore_cluster_coal", oreCluster());
	public static final DeferredBlock<Block> ORE_CLUSTER_COMMON_METAL = block("ore_cluster_common_metal", oreCluster());
	public static final DeferredBlock<Block> ORE_CLUSTER_RARE_METAL = block("ore_cluster_rare_metal", oreCluster());
	public static final DeferredBlock<Block> ORE_CLUSTER_SHINY_METAL = block("ore_cluster_shiny_metal", oreCluster());
	public static final DeferredBlock<Block> ORE_CLUSTER_URANIUM = block("ore_cluster_uranium", oreCluster());
	public static final DeferredBlock<Block> ORE_CLUSTER_COMMON_GEM = block("ore_cluster_common_gem", oreCluster());
	public static final DeferredBlock<Block> ORE_CLUSTER_SHINY_GEM = block("ore_cluster_shiny_gem", oreCluster());
	public static final DeferredBlock<Block> ORE_CLUSTER_NETHER_CRYSTAL = block("ore_cluster_nether_crystal", oreCluster());
	public static final DeferredBlock<Block> ORE_CLUSTER_OIL = block("ore_cluster_oil", oreCluster());

	private static Properties multiBlock() {
		return Properties.of().mapColor(MapColor.METAL).strength(4.0f).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion()
				.pushReaction(PushReaction.BLOCK);
	}

	/** Unbreakable, drops nothing (1.12 BlockOreCluster). */
	private static Properties oreCluster() {
		return Properties.of().mapColor(MapColor.STONE).strength(-1.0f, 3600000.0f).noLootTable().noOcclusion();
	}

	private static Properties ore(float hardness) {
		return Properties.of().mapColor(MapColor.STONE).strength(hardness, 3.0f).requiresCorrectToolForDrops();
	}

	private static Properties metal() {
		return Properties.of().mapColor(MapColor.METAL).strength(8.0f).sound(SoundType.METAL).requiresCorrectToolForDrops();
	}

	private static Properties concrete() {
		return Properties.of().mapColor(MapColor.STONE).strength(8.0f).requiresCorrectToolForDrops();
	}

	private static Properties neon() {
		return Properties.of().mapColor(MapColor.COLOR_YELLOW).strength(4.0f).sound(SoundType.GLASS).lightLevel(s -> 15);
	}

	private static Properties lamp() {
		return neon().noOcclusion().pushReaction(PushReaction.DESTROY);
	}

	private static DeferredBlock<CamoNetBlock> camonet(String name, boolean top) {
		return withItem(name, p -> new CamoNetBlock(top, p), Properties.of().mapColor(MapColor.COLOR_GREEN).sound(SoundType.WOOL).noOcclusion());
	}

	private static DeferredBlock<MachineBlock> machine(String name, Supplier<BlockEntityType<? extends TGBlockEntity>> type) {
		return withItem(name, p -> new MachineBlock(type, p),
				Properties.of().mapColor(MapColor.METAL).strength(4.0f).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion());
	}

	private static DeferredBlock<Block> block(String name, Properties props) {
		return withItem(name, p -> new Block(p), props);
	}

	private static DeferredBlock<LadderBlock> ladder(String name) {
		return withItem(name, LadderBlock::new, metal().strength(6.0f).noOcclusion());
	}

	private static DeferredBlock<StairBlock> stairs(String name, DeferredBlock<Block> base, Properties props) {
		return withItem(name, p -> new StairBlock(base.get().defaultBlockState(), p), props);
	}

	private static <B extends Block> DeferredBlock<B> withItem(String name, Function<Properties, B> factory, Properties props) {
		DeferredBlock<B> block = BLOCKS.registerBlock(name, factory, props);
		TGItems.ITEMS.registerSimpleBlockItem(block);
		return block;
	}
}
