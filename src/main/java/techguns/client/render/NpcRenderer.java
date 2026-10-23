package techguns.client.render;

import java.util.function.Supplier;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.model.HumanoidArmorModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PiglinModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.NoopRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import techguns.Techguns;
import techguns.client.models.LegacyGl;
import techguns.client.models.ModelBiped;
import techguns.client.models.npcs.ModelCyberDemon;
import techguns.client.models.npcs.ModelSuperMutant;
import techguns.entities.npcs.GenericNPC;
import techguns.entities.npcs.SuperMutant;
import techguns.entities.npcs.TGNpcs;
import techguns.items.guns.GenericGun;

/**
 * The 1.12 RenderGenericNPC family: a biped holding its gun up like a bow (1.12 ModelGenericNPC), in vanilla armor.
 * ponytail: guns sit in the plain vanilla hand spot; 1.12's per-mob weapon offsets (getWeaponPos) come back if a gun clips.
 */
public class NpcRenderer<T extends GenericNPC, M extends HumanoidModel<T>> extends HumanoidMobRenderer<T, M> {
	private final ResourceLocation texture;
	private final float scale;

	NpcRenderer(EntityRendererProvider.Context ctx, M model, ResourceLocation texture, float scale, ModelLayerLocation inner, ModelLayerLocation outer) {
		super(ctx, model, 0.5f * scale);
		this.texture = texture;
		this.scale = scale;
		addLayer(new HumanoidArmorLayer<>(this, new HumanoidArmorModel<>(ctx.bakeLayer(inner)), new HumanoidArmorModel<>(ctx.bakeLayer(outer)),
				ctx.getModelManager()));
	}

	public static void register(EntityRenderersEvent.RegisterRenderers event) {
		humanoid(event, TGNpcs.ARMY_SOLDIER.get(), "army_soldier");
		humanoid(event, TGNpcs.BANDIT.get(), "bandit");
		humanoid(event, TGNpcs.COMMANDO.get(), "army_soldier");
		humanoid(event, TGNpcs.DICTATOR_DAVE.get(), "dictator");
		humanoid(event, TGNpcs.OUTCAST.get(), "army_soldier");
		humanoid(event, TGNpcs.PSYCHO_STEVE.get(), "army_soldier");
		humanoid(event, TGNpcs.STORM_TROOPER.get(), "army_soldier");
		humanoid(event, TGNpcs.ZOMBIE_FARMER.get(), "zombie_soldier");
		humanoid(event, TGNpcs.ZOMBIE_MINER.get(), "zombie_soldier");
		humanoid(event, TGNpcs.ZOMBIE_POLICEMAN.get(), "zombie_policeman");
		humanoid(event, TGNpcs.ZOMBIE_SOLDIER.get(), "zombie_soldier");
		event.registerEntityRenderer(TGNpcs.SKELETON_SOLDIER.get(), ctx -> new NpcRenderer<>(ctx, new HumanoidModel<>(ctx.bakeLayer(ModelLayers.SKELETON)),
				ResourceLocation.withDefaultNamespace("textures/entity/skeleton/skeleton.png"), 1, ModelLayers.SKELETON_INNER_ARMOR,
				ModelLayers.SKELETON_OUTER_ARMOR));
		event.registerEntityRenderer(TGNpcs.ZOMBIE_PIGMAN_SOLDIER.get(), ctx -> new NpcRenderer<>(ctx,
				new PiglinModel<>(ctx.bakeLayer(ModelLayers.ZOMBIFIED_PIGLIN)),
				ResourceLocation.withDefaultNamespace("textures/entity/piglin/zombified_piglin.png"), 1, ModelLayers.ZOMBIFIED_PIGLIN_INNER_ARMOR,
				ModelLayers.ZOMBIFIED_PIGLIN_OUTER_ARMOR));
		legacy(event, TGNpcs.CYBER_DEMON.get(), "cyberdemon", 1, ModelCyberDemon::new);
		legacy(event, TGNpcs.SUPER_MUTANT_BASIC.get(), "supermutant_texture_1", SuperMutant.SCALE[0], ModelSuperMutant::new);
		legacy(event, TGNpcs.SUPER_MUTANT_HEAVY.get(), "supermutant_texture_2", SuperMutant.SCALE[1], ModelSuperMutant::new);
		legacy(event, TGNpcs.SUPER_MUTANT_ELITE.get(), "supermutant_texture_3", SuperMutant.SCALE[2], ModelSuperMutant::new);
		event.registerEntityRenderer(TGNpcs.DUMMY_SPAWN.get(), NoopRenderer::new);
	}

	private static <T extends GenericNPC> void humanoid(EntityRenderersEvent.RegisterRenderers event, EntityType<T> type, String texture) {
		event.registerEntityRenderer(type, ctx -> new NpcRenderer<>(ctx, new HumanoidModel<>(ctx.bakeLayer(ModelLayers.ZOMBIE)),
				Techguns.id("textures/entity/" + texture + ".png"), 1, ModelLayers.ZOMBIE_INNER_ARMOR, ModelLayers.ZOMBIE_OUTER_ARMOR));
	}

	private static <T extends GenericNPC> void legacy(EntityRenderersEvent.RegisterRenderers event, EntityType<T> type, String texture, float scale,
			Supplier<ModelBiped> model) {
		event.registerEntityRenderer(type, ctx -> new NpcRenderer<>(ctx, new Legacy<T>(ctx.bakeLayer(ModelLayers.ZOMBIE), model.get()),
				Techguns.id("textures/entity/" + texture + ".png"), scale, ModelLayers.ZOMBIE_INNER_ARMOR, ModelLayers.ZOMBIE_OUTER_ARMOR));
	}

	@Override
	public void render(T npc, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
		ItemStack held = npc.getMainHandItem();
		HumanoidModel.ArmPose arm = held.isEmpty() ? HumanoidModel.ArmPose.EMPTY
				: held.getItem() instanceof GenericGun ? HumanoidModel.ArmPose.BOW_AND_ARROW : HumanoidModel.ArmPose.ITEM;
		boolean right = npc.getMainArm() == HumanoidArm.RIGHT;
		model.rightArmPose = right ? arm : HumanoidModel.ArmPose.EMPTY;
		model.leftArmPose = right ? HumanoidModel.ArmPose.EMPTY : arm;
		super.render(npc, yaw, partialTick, pose, buffers, light);
	}

	@Override
	protected void scale(T npc, PoseStack pose, float partialTick) {
		pose.scale(scale, scale, scale);
	}

	@Override
	public ResourceLocation getTextureLocation(T npc) {
		return texture;
	}

	/** A ported 1.12 biped, drawn in the pose of the vanilla humanoid it stands in for (which also places the held item). */
	static class Legacy<T extends GenericNPC> extends HumanoidModel<T> {
		private final ModelBiped legacy;

		Legacy(ModelPart root, ModelBiped legacy) {
			super(root);
			this.legacy = legacy;
		}

		@Override
		public void renderToBuffer(PoseStack pose, VertexConsumer consumer, int light, int overlay, int color) {
			legacy.copyFrom(this);
			LegacyGl.begin(pose, consumer, light, overlay);
			legacy.render(ItemModelRenderer.SCALE);
		}
	}
}
