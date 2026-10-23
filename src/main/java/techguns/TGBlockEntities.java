package techguns;

import java.util.function.Supplier;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.energy.ComponentEnergyStorage;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import techguns.items.armors.PoweredArmor;
import techguns.machines.AmmoPressBlockEntity;
import techguns.machines.BlastFurnaceBlockEntity;
import techguns.machines.ChargingStationBlockEntity;
import techguns.machines.CamoBenchBlockEntity;
import techguns.machines.ChemLabBlockEntity;
import techguns.machines.Door3x3BlockEntity;
import techguns.machines.ExplosiveChargeBlockEntity;
import techguns.machines.GrinderBlockEntity;
import techguns.machines.MachineBlockEntity;
import techguns.machines.MetalPressBlockEntity;
import techguns.machines.RepairBenchBlockEntity;
import techguns.machines.TGBlockEntity;
import techguns.machines.UpgradeBenchBlockEntity;

/** Block entity types and the capabilities of machines (and of RF powered armor, which the charging station charges). */
public class TGBlockEntities {
	public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Techguns.MODID);

	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<AmmoPressBlockEntity>> AMMO_PRESS = register("ammo_press",
			AmmoPressBlockEntity::new, TGBlocks.AMMO_PRESS);
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MetalPressBlockEntity>> METAL_PRESS = register("metal_press",
			MetalPressBlockEntity::new, TGBlocks.METAL_PRESS);
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<GrinderBlockEntity>> GRINDER = register("grinder",
			GrinderBlockEntity::new, TGBlocks.GRINDER);
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BlastFurnaceBlockEntity>> BLAST_FURNACE = register("blast_furnace",
			BlastFurnaceBlockEntity::new, TGBlocks.BLAST_FURNACE);
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ChargingStationBlockEntity>> CHARGING_STATION = register(
			"charging_station", ChargingStationBlockEntity::new, TGBlocks.CHARGING_STATION);
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ChemLabBlockEntity>> CHEM_LAB = register("chem_lab",
			ChemLabBlockEntity::new, TGBlocks.CHEM_LAB);

	@SuppressWarnings("DataFlowIssue")
	private static <T extends MachineBlockEntity> DeferredHolder<BlockEntityType<?>, BlockEntityType<T>> register(String name,
			BlockEntityType.BlockEntitySupplier<T> factory, Supplier<? extends Block> block) {
		return BLOCK_ENTITIES.register(name, () -> BlockEntityType.Builder.of(factory, block.get()).build(null));
	}

	static void registerCapabilities(RegisterCapabilitiesEvent event) {
		for (var type : BLOCK_ENTITIES.getEntries()) {
			@SuppressWarnings("unchecked")
			BlockEntityType<MachineBlockEntity> machine = (BlockEntityType<MachineBlockEntity>) type.get();
			event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, machine, (be, side) -> be.automation);
			event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, machine, (be, side) -> be.energy);
		}
		event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, CHEM_LAB.get(), (be, side) -> be.fluids);
		for (var item : TGItems.ITEMS.getEntries()) {
			if (item.get() instanceof PoweredArmor armor && armor.powerType == PoweredArmor.PowerType.RF) {
				event.registerItem(Capabilities.EnergyStorage.ITEM, (stack, ctx) -> new ComponentEnergyStorage(stack, TGComponents.POWER.get(), armor.maxPower), armor);
			}
		}
	}

	/** Benches, charges and doors: no item or energy capabilities, so a register of their own. */
	public static final DeferredRegister<BlockEntityType<?>> BENCHES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Techguns.MODID);
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<RepairBenchBlockEntity>> REPAIR_BENCH = bench("repair_bench",
			RepairBenchBlockEntity::new, TGBlocks.REPAIR_BENCH);
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CamoBenchBlockEntity>> CAMO_BENCH = bench("camo_bench",
			CamoBenchBlockEntity::new, TGBlocks.CAMO_BENCH);
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<UpgradeBenchBlockEntity>> UPGRADE_BENCH = bench("upgrade_bench",
			UpgradeBenchBlockEntity::new, TGBlocks.UPGRADE_BENCH);
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ExplosiveChargeBlockEntity>> EXPLOSIVE_CHARGE = bench("explosive_charge",
			ExplosiveChargeBlockEntity::new, TGBlocks.TNT_CHARGE, TGBlocks.C4_CHARGE);
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<Door3x3BlockEntity>> DOOR3X3 = bench("door3x3",
			Door3x3BlockEntity::new, TGBlocks.DOOR3X3);

	@SafeVarargs
	@SuppressWarnings("DataFlowIssue")
	private static <T extends TGBlockEntity> DeferredHolder<BlockEntityType<?>, BlockEntityType<T>> bench(String name,
			BlockEntityType.BlockEntitySupplier<T> factory, Supplier<? extends Block>... blocks) {
		return BENCHES.register(name, () -> BlockEntityType.Builder.of(factory, java.util.Arrays.stream(blocks).map(Supplier::get).toArray(Block[]::new)).build(null));
	}
}
