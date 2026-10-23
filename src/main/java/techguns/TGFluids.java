package techguns;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/** 1.12 TGFluids: creeper acid, made and used by the chem lab. ponytail: acid doesn't hurt what swims in it. Milk is NeoForge's. */
public class TGFluids {
	public static final DeferredRegister<FluidType> FLUID_TYPES = DeferredRegister.create(NeoForgeRegistries.Keys.FLUID_TYPES, Techguns.MODID);
	public static final DeferredRegister<Fluid> FLUIDS = DeferredRegister.create(Registries.FLUID, Techguns.MODID);

	public static final DeferredHolder<FluidType, FluidType> CREEPER_ACID_TYPE = FLUID_TYPES.register("creeper_acid",
			() -> new FluidType(FluidType.Properties.create().descriptionId("block.techguns.block_creeper_acid")));
	public static final DeferredHolder<Fluid, FlowingFluid> CREEPER_ACID = FLUIDS.register("creeper_acid", () -> new BaseFlowingFluid.Source(acid()));
	public static final DeferredHolder<Fluid, FlowingFluid> CREEPER_ACID_FLOWING = FLUIDS.register("creeper_acid_flowing",
			() -> new BaseFlowingFluid.Flowing(acid()));
	public static final DeferredBlock<LiquidBlock> CREEPER_ACID_BLOCK = TGBlocks.BLOCKS.registerBlock("block_creeper_acid",
			p -> new LiquidBlock(CREEPER_ACID.get(), p), BlockBehaviour.Properties.ofFullCopy(Blocks.WATER));
	public static final DeferredItem<BucketItem> CREEPER_ACID_BUCKET = TGItems.ITEMS.register("creeper_acid_bucket",
			() -> new BucketItem(CREEPER_ACID.get(), new Item.Properties().craftRemainder(Items.BUCKET).stacksTo(1)));

	private static BaseFlowingFluid.Properties acid() {
		return new BaseFlowingFluid.Properties(CREEPER_ACID_TYPE, CREEPER_ACID, CREEPER_ACID_FLOWING).block(CREEPER_ACID_BLOCK).bucket(CREEPER_ACID_BUCKET);
	}
}
