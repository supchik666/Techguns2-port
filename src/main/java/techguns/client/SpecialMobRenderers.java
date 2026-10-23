package techguns.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import techguns.TGEntities;
import techguns.Techguns;
import techguns.client.models.LegacyGl;
import techguns.client.models.ModelBase;
import techguns.client.models.ModelObj;
import techguns.client.models.npcs.ModelAlienBug;
import techguns.client.models.npcs.ModelGhastling;
import techguns.entities.npcs.AlienBug;
import techguns.entities.npcs.AttackHelicopter;
import techguns.entities.npcs.Ghastling;

/** Renderers of the alien bug, ghastling and attack helicopter (1.12 RenderAlienBug, RenderGhastling, RenderAttackHelicopter). */
@EventBusSubscriber(modid = Techguns.MODID, value = Dist.CLIENT)
public final class SpecialMobRenderers {
	private static final ResourceLocation ALIENBUG = Techguns.id("textures/entity/alienbug.png");
	private static final ResourceLocation GHAST = ResourceLocation.withDefaultNamespace("textures/entity/ghast/ghast.png");
	private static final ResourceLocation GHAST_SHOOTING = ResourceLocation.withDefaultNamespace("textures/entity/ghast/ghast_shooting.png");

	private SpecialMobRenderers() {}

	@SubscribeEvent
	static void register(EntityRenderersEvent.RegisterRenderers event) {
		event.registerEntityRenderer(TGEntities.ALIENBUG.get(), context -> new MobRenderer<AlienBug, LegacyModel<AlienBug>>(context,
				new LegacyModel<>(new ModelAlienBug()), 0.8f) {
			@Override
			public ResourceLocation getTextureLocation(AlienBug entity) {
				return ALIENBUG;
			}
		});
		event.registerEntityRenderer(TGEntities.GHASTLING.get(), context -> new MobRenderer<Ghastling, LegacyModel<Ghastling>>(context,
				new LegacyModel<>(new ModelGhastling()), 0.25f) {
			@Override
			public ResourceLocation getTextureLocation(Ghastling entity) {
				return entity.isAttacking() ? GHAST_SHOOTING : GHAST;
			}
		});
		event.registerEntityRenderer(TGEntities.ATTACK_HELICOPTER.get(), HelicopterRenderer::new);
	}

	/** A 1.12 {@link ModelBase} as a vanilla entity model. */
	static class LegacyModel<T extends Mob> extends EntityModel<T> {
		private final ModelBase model;
		private Entity entity;
		private float limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch;

		LegacyModel(ModelBase model) {
			this.model = model;
		}

		@Override
		public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
			this.entity = entity;
			this.limbSwing = limbSwing;
			this.limbSwingAmount = limbSwingAmount;
			this.ageInTicks = ageInTicks;
			this.netHeadYaw = netHeadYaw;
			this.headPitch = headPitch;
		}

		@Override
		public void renderToBuffer(PoseStack pose, VertexConsumer consumer, int light, int overlay, int color) {
			LegacyGl.begin(pose, consumer, light, overlay);
			model.render(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, 0.0625f);
		}
	}

	/** The three OBJ parts: the rotor spins, the gun pitches at the target; dying it sinks and spins faster and faster. */
	static class HelicopterRenderer extends EntityRenderer<AttackHelicopter> {
		private static final ResourceLocation TEXTURE = Techguns.id("textures/entity/apache.png");
		private final ModelObj main = new ModelObj(Techguns.id("models/item/npc/helicopter0.obj"));
		private final ModelObj rotor = new ModelObj(Techguns.id("models/item/npc/helicopter1.obj"));
		private final ModelObj gun = new ModelObj(Techguns.id("models/item/npc/helicopter2.obj"));

		HelicopterRenderer(EntityRendererProvider.Context context) {
			super(context);
			shadowRadius = 5.0f;
		}

		@Override
		public void render(AttackHelicopter entity, float entityYaw, float partialTicks, PoseStack pose, MultiBufferSource buffers, int light) {
			pose.pushPose();
			pose.mulPose(Axis.YP.rotationDegrees(-Mth.rotLerp(partialTicks, entity.yRotO, entity.getYRot()) - 90.0f));
			pose.scale(2.5f, 2.5f, 2.5f);
			LegacyGl.begin(pose, buffers, TEXTURE, light, OverlayTexture.NO_OVERLAY);
			float rotorTicks = (entity.level().getGameTime() % 60) + partialTicks;
			if (entity.deathTime == 0) {
				draw(main);
				pose.pushPose();
				pose.mulPose(Axis.YP.rotationDegrees(rotorTicks * 24.0f));
				draw(rotor);
				pose.popPose();
				pose.pushPose();
				pose.translate(1.2f, 0, 0);
				pose.mulPose(Axis.ZP.rotationDegrees(-Mth.lerp(partialTicks, entity.xRotO, entity.getXRot())));
				pose.translate(-1.2f, 0, 0);
				draw(gun);
				pose.popPose();
			} else {
				float p = (entity.deathTime + partialTicks) / AttackHelicopter.MAX_DEATH_TIME;
				p *= p;
				pose.translate(0, p * -4.0f, 0);
				// 1.12 turned by the angle twice (a GL11 and a GlStateManager call)
				pose.mulPose(Axis.YP.rotationDegrees(p * 720.0f * 2));
				draw(main);
				draw(gun);
				pose.mulPose(Axis.YP.rotationDegrees(rotorTicks * 12.0f));
				draw(rotor);
			}
			pose.popPose();
			super.render(entity, entityYaw, partialTicks, pose, buffers, light);
		}

		private void draw(ModelObj part) {
			part.render(null, 0, 0, 0, 0, 0, 0.0625f, 0, 0, null, 0, 0, 0);
		}

		@Override
		public ResourceLocation getTextureLocation(AttackHelicopter entity) {
			return TEXTURE;
		}
	}
}
