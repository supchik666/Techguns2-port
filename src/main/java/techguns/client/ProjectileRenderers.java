package techguns.client;

import net.minecraft.client.renderer.entity.NoopRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import techguns.TGEntities;
import techguns.Techguns;
import techguns.client.render.BioGunProjectileRenderer;
import techguns.client.render.BlasterProjectileRenderer;
import techguns.client.render.GenericProjectileRenderer;
import techguns.client.render.LaserProjectileRenderer;
import techguns.client.render.ModelProjectileRenderer;
import techguns.client.render.NDRProjectileRenderer;
import techguns.client.render.SonicShotgunProjectileRenderer;
import techguns.client.render.TeslaProjectileRenderer;
import techguns.client.render.ThrownGrenadeRenderer;

/** Renderers of the gun projectiles; the trail-only ones are drawn by their TGFX trail alone. */
@EventBusSubscriber(modid = Techguns.MODID, value = Dist.CLIENT)
public final class ProjectileRenderers {
	private ProjectileRenderers() {}

	@SubscribeEvent
	static void register(EntityRenderersEvent.RegisterRenderers event) {
		event.registerEntityRenderer(TGEntities.LASER_PROJECTILE.get(), LaserProjectileRenderer::new);
		event.registerEntityRenderer(TGEntities.TESLA_PROJECTILE.get(), TeslaProjectileRenderer::new);
		event.registerEntityRenderer(TGEntities.NDR_PROJECTILE.get(), NDRProjectileRenderer::new);
		event.registerEntityRenderer(TGEntities.BLASTER_PROJECTILE.get(), BlasterProjectileRenderer::new);
		event.registerEntityRenderer(TGEntities.SONIC_SHOTGUN_PROJECTILE.get(), SonicShotgunProjectileRenderer::new);
		event.registerEntityRenderer(TGEntities.GAUSS_PROJECTILE.get(), GenericProjectileRenderer::new); // Kind.ADVANCED: blue tracer
		event.registerEntityRenderer(TGEntities.ALIEN_BLASTER_PROJECTILE.get(), NoopRenderer::new);
		event.registerEntityRenderer(TGEntities.DEATOMIZER_PROJECTILE.get(), NoopRenderer::new);
		event.registerEntityRenderer(TGEntities.CYBERDEMON_BLASTER_PROJECTILE.get(), NoopRenderer::new);
		event.registerEntityRenderer(TGEntities.FLAMETHROWER_PROJECTILE.get(), NoopRenderer::new);
		event.registerEntityRenderer(TGEntities.ROCKET_PROJECTILE.get(), ModelProjectileRenderer::rocket);
		event.registerEntityRenderer(TGEntities.GUIDED_MISSILE_PROJECTILE.get(), ModelProjectileRenderer::rocket);
		event.registerEntityRenderer(TGEntities.GRENADE_40MM_PROJECTILE.get(), ModelProjectileRenderer::grenade40mm);
		event.registerEntityRenderer(TGEntities.GRENADE_PROJECTILE.get(), context -> new ThrownGrenadeRenderer(context, "stielgranate"));
		event.registerEntityRenderer(TGEntities.FRAG_GRENADE_PROJECTILE.get(), context -> new ThrownGrenadeRenderer(context, "fraggrenade"));
		event.registerEntityRenderer(TGEntities.TFG_PROJECTILE.get(), NoopRenderer::new);
		event.registerEntityRenderer(TGEntities.BIOGUN_PROJECTILE.get(), BioGunProjectileRenderer::new);
	}
}
