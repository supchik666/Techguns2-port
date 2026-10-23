package techguns.client;

import java.util.Map;
import java.util.function.Supplier;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import techguns.TGArmors;
import techguns.TGComponents;
import techguns.TGItems;
import techguns.Techguns;
import techguns.client.models.LegacyGl;
import techguns.client.models.ModelBase;
import techguns.client.models.ModelBiped;
import techguns.client.models.armor.ModelAdvancedShield;
import techguns.client.models.armor.ModelArmorCoat;
import techguns.client.models.armor.ModelBallisticShield;
import techguns.client.models.armor.ModelBeret;
import techguns.client.models.armor.ModelExoSuit;
import techguns.client.models.armor.ModelRiotShield;
import techguns.client.models.armor.ModelSteamArmor;
import techguns.client.models.armor.ModelT3PowerArmor;
import techguns.client.models.armor.ModelT4PowerArmorMk2;
import techguns.client.render.ItemModelRenderer;
import techguns.items.armors.GenericArmor;

/** 1.12 ClientProxy armor parts: 3D armor models on the wearer, render-hack armor items, and the shield renderer. */
@EventBusSubscriber(modid = Techguns.MODID, value = Dist.CLIENT)
public class ArmorRenderers {
	private static final ResourceLocation STEAM = Techguns.id("textures/models/armor/steam_armor.png");
	private static final ResourceLocation POWER = Techguns.id("textures/models/armor/powerarmor.png");
	private static final ResourceLocation POWER_MK2 = Techguns.id("textures/models/armor/powerarmor_mk2_darkgrey.png");

	@SubscribeEvent
	static void registerItemExtensions(RegisterClientExtensionsEvent event) {
		Map<String, ModelBiped> models = Map.ofEntries(
				Map.entry("steam_armor_0", new ModelSteamArmor(0)), Map.entry("steam_armor_1", new ModelSteamArmor(1)),
				Map.entry("steam_armor_2", new ModelSteamArmor(0, 0.01f)),
				Map.entry("power_armor_0", new ModelT3PowerArmor(0)), Map.entry("power_armor_1", new ModelT3PowerArmor(1)),
				Map.entry("power_armor_2", new ModelT3PowerArmor(0, 0.01f)),
				Map.entry("power_armor_mk2_0", new ModelT4PowerArmorMk2(0)), Map.entry("power_armor_mk2_1", new ModelT4PowerArmorMk2(1)),
				Map.entry("power_armor_mk2_2", new ModelT4PowerArmorMk2(0, 0.01f)),
				Map.entry("exo_suit_0", new ModelExoSuit(0, 1.0f)), Map.entry("exo_suit_1", new ModelExoSuit(1, 0.5f)),
				Map.entry("exo_suit_2", new ModelExoSuit(0, 0.75f)),
				Map.entry("beret_0", new ModelBeret()),
				Map.entry("coat_0", new ModelArmorCoat(0, 1.0f)), Map.entry("coat_1", new ModelArmorCoat(1, 0.51f)),
				Map.entry("coat_2", new ModelArmorCoat(2, 0.49f)), Map.entry("coat_3", new ModelArmorCoat(3, 0.75f)));
		for (var entry : TGItems.ITEMS.getEntries()) {
			if (!(entry.get() instanceof GenericArmor armor) || armor.model == null) continue;
			ModelBiped biped = models.get(armor.model);
			if (biped == null) throw new IllegalStateException("No armor model " + armor.model + " for " + entry.getId());
			event.registerItem(armorModel(biped, armor), armor);
		}
		event.registerItem(shield(ModelRiotShield::new, "riot_shield", "riot_shield_black", "riot_shield_darkgreen", "riot_shield_grey"),
				TGArmors.RIOT_SHIELD.get());
		event.registerItem(shield(ModelBallisticShield::new, "ballistic_shield", "ballistic_shield_black", "ballistic_shield_green",
				"ballistic_shield_grey"), TGArmors.BALLISTIC_SHIELD.get());
		event.registerItem(shield(ModelAdvancedShield::new, "advanced_shield", "advanced_shield_silver", "advanced_shield_green"),
				TGArmors.ADVANCED_SHIELD.get());
	}

	/** Shields show their blocking model while raised, as vanilla's. */
	@SubscribeEvent
	static void clientSetup(FMLClientSetupEvent event) {
		event.enqueueWork(() -> {
			for (var shield : new Item[] {TGArmors.RIOT_SHIELD.get(), TGArmors.BALLISTIC_SHIELD.get(), TGArmors.ADVANCED_SHIELD.get()}) {
				ItemProperties.register(shield, ResourceLocation.withDefaultNamespace("blocking"),
						(stack, level, entity, seed) -> entity != null && entity.isUsingItem() && entity.getUseItem() == stack ? 1 : 0);
			}
		});
	}

	/** The worn 3D model, posed like the vanilla armor model; render-hack pieces also draw their item from it. */
	private static IClientItemExtensions armorModel(ModelBiped biped, GenericArmor armor) {
		Model model = new Model(RenderType::armorCutoutNoCull) {
			@Override
			public void renderToBuffer(PoseStack pose, VertexConsumer consumer, int light, int overlay, int color) {
				LegacyGl.begin(pose, consumer, light, overlay);
				biped.render(ItemModelRenderer.SCALE);
			}
		};
		return new IClientItemExtensions() {
			private BlockEntityWithoutLevelRenderer renderer;

			@Override
			public Model getGenericArmorModel(LivingEntity entity, ItemStack stack, EquipmentSlot slot, HumanoidModel<?> original) {
				biped.copyFrom(original);
				return model;
			}

			@Override
			public BlockEntityWithoutLevelRenderer getCustomRenderer() {
				if (renderer == null && armor.renderHack) renderer = armorItem(armor);
				return renderer;
			}
		};
	}

	/** 1.12 ClientProxy RenderArmorItem registrations: legs use the model's type 1, the rest type 0. */
	private static ItemModelRenderer armorItem(GenericArmor armor) {
		boolean legs = armor.getType() == ArmorItem.Type.LEGGINGS;
		int type = legs ? 1 : 0;
		String name = armor.model;
		if (name.startsWith("steam_armor")) return new ArmorItemRenderer(new ModelSteamArmor(type), STEAM, armor.getEquipmentSlot());
		if (name.startsWith("power_armor_mk2")) return new ArmorItemRenderer(new ModelT4PowerArmorMk2(type), POWER_MK2, armor.getEquipmentSlot());
		return new ArmorItemRenderer(new ModelT3PowerArmor(type), POWER, armor.getEquipmentSlot());
	}

	/** 1.12 RenderArmorItem: one slot's parts of a 3D armor model as the item. */
	static class ArmorItemRenderer extends ItemModelRenderer {
		private final ModelBiped biped;
		private final ResourceLocation texture;

		ArmorItemRenderer(ModelBiped biped, ResourceLocation texture, EquipmentSlot slot) {
			super(null, 0, texture);
			this.biped = biped;
			this.texture = texture;
			biped.bipedHead.showModel = biped.bipedHeadwear.showModel = slot == EquipmentSlot.HEAD;
			biped.bipedBody.showModel = slot == EquipmentSlot.CHEST || slot == EquipmentSlot.LEGS;
			biped.bipedRightArm.showModel = biped.bipedLeftArm.showModel = slot == EquipmentSlot.CHEST;
			biped.bipedRightLeg.showModel = biped.bipedLeftLeg.showModel = slot == EquipmentSlot.LEGS || slot == EquipmentSlot.FEET;
			scaleThirdp = 1.0f;
			scaleEgo = 1.0f;
			scaleItemframe = 1.5f;
			scaleGround = slot == EquipmentSlot.HEAD ? 1.25f : 1.5f;
			translateBase = new float[] {0, switch (slot) {
				case HEAD -> -0.54f;
				case CHEST -> -0.9f;
				case LEGS -> -1.29f;
				default -> -1.42f;
			}, 0};
			translateType = new float[][] {{0, 0.1f, 0}, {0, 0.04f, 0}, {0, 0, 0}, {0, 0, 0}, {0, 0, 0.07f}};
		}

		@Override
		public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
			// the GUI never gets here: its model is a flat icon (port_assets.py ICON_ARMOR)
			pose.pushPose();
			pose.translate(0.5f, 0.5f, 0.5f);
			applyTranslation(pose, context);
			renderModel(stack, context, pose, buffers, light, overlay, 0, 0, 0, 0);
			LegacyGl.begin(pose, buffers, texture, light, overlay);
			biped.render(SCALE);
			pose.popPose();
		}
	}

	/** 1.12 TileEntityItemRendererTGShield: the shield model with the stack's camo texture. */
	private static IClientItemExtensions shield(Supplier<ModelBase> factory, String... textures) {
		ResourceLocation[] locations = new ResourceLocation[textures.length];
		for (int i = 0; i < textures.length; i++) locations[i] = Techguns.id("textures/armors/" + textures[i] + ".png");
		return new IClientItemExtensions() {
			private BlockEntityWithoutLevelRenderer renderer;

			@Override
			public BlockEntityWithoutLevelRenderer getCustomRenderer() {
				if (renderer == null) renderer = new ItemModelRenderer(null, 0, locations[0]) {
					private final ModelBase model = factory.get();

					@Override
					public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
						int camo = Math.clamp(stack.getOrDefault(TGComponents.CAMO, 0), 0, locations.length - 1);
						pose.pushPose();
						pose.scale(1, -1, -1);
						LegacyGl.begin(pose, buffers, locations[camo], light, overlay);
						LegacyGl.enableBlendMode(LegacyGl.RenderType.ALPHA);
						model.render(null, 0, 0, 0, 0, 0, SCALE);
						pose.popPose();
					}
				};
				return renderer;
			}
		};
	}
}
