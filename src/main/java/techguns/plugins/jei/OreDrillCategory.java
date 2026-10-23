package techguns.plugins.jei;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.neoforge.NeoForgeTypes;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import techguns.Techguns;
import techguns.machines.multiblock.OreDrillBlockEntity;

/**
 * 1.12 OreDrillJeiRecipeCategory: one recipe per ore of a cluster, with its chance; any drill head, furnace fuel or lava.
 * ponytail: power and duration are for a one-rod drill at efficiency 3 (4.5 ores an hour times the cluster's amount, as
 * 1.12's JEI tooltip); bigger drills and better heads mine faster.
 */
class OreDrillCategory extends TGCategory<OreDrillCategory.Ore> {
	private static final double ORES_PER_HOUR = 4.5;
	private final IDrawable scale;
	private List<ItemStack> fuels;

	record Ore(Block cluster, OreDrillBlockEntity.Cluster stats, ItemStack stack, float chance) {}

	OreDrillCategory(IGuiHelper gui, Block block) {
		super(gui, RecipeType.create(Techguns.MODID, "ore_drill", Ore.class), block.getName(), block, null);
		scale = gui.createDrawable(texture, 177, 41, 10, 50);
	}

	@Override
	public List<Ore> recipes(RecipeManager recipes) {
		List<Ore> out = new ArrayList<>();
		OreDrillBlockEntity.clusters().entrySet().stream().sorted(Comparator.comparing(e -> BuiltInRegistries.BLOCK.getKey(e.getKey()))).forEach(e -> {
			int total = e.getValue().ores().stream().mapToInt(OreDrillBlockEntity.Ore::weight).sum();
			e.getValue().ores().forEach(o -> out.add(new Ore(e.getKey(), e.getValue(), o.stack(), (float) o.weight() / total)));
		});
		return out;
	}

	private double perHour(Ore ore) {
		return ORES_PER_HOUR * ore.stats().amount();
	}

	@Override
	protected int power(Ore ore) {
		return (int) (8 * perHour(ore) * ore.stats().power());
	}

	@Override
	protected int time(Ore ore) {
		return (int) (72000 / perHour(ore));
	}

	@Override
	public void setRecipe(IRecipeLayoutBuilder builder, Ore ore, IFocusGroup focuses) {
		if (fuels == null) fuels = BuiltInRegistries.ITEM.stream().map(ItemStack::new)
				.filter(s -> s.getBurnTime(net.minecraft.world.item.crafting.RecipeType.SMELTING) > 0).toList();
		builder.addSlot(RecipeIngredientRole.INPUT, 30 - X, 17 - Y).addItemStacks(OreDrillBlockEntity.HEADS.stream().map(h -> new ItemStack(h.get())).toList());
		builder.addSlot(RecipeIngredientRole.INPUT, 30 - X, 51 - Y).addItemStacks(fuels);
		builder.addSlot(RecipeIngredientRole.INPUT, 77 - X, 56 - Y).addItemStack(new ItemStack(ore.cluster()));
		builder.addSlot(RecipeIngredientRole.INPUT, 16 - X, 18 - Y).setFluidRenderer(16000, false, 10, 50).setOverlay(scale, 0, 0)
				.addIngredient(NeoForgeTypes.FLUID_STACK, new FluidStack(Fluids.LAVA, 16000));
		builder.addSlot(RecipeIngredientRole.OUTPUT, 102 - X, 17 - Y).addItemStack(ore.stack())
				.addRichTooltipCallback((view, tooltip) -> tooltip.add(Component.translatable("techguns.oredrill.jei.chance")
						.append(": " + String.format("%.2f", ore.chance() * 100) + "%")));
	}

	@Override
	protected void drawGui(Ore ore, GuiGraphics g, float p) {
		g.blit(texture, 73, 17, 177, 1, 25, (int) (p * 36) + 1);
	}
}
