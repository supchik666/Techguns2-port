package techguns.client;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import org.lwjgl.glfw.GLFW;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Axis;

import net.minecraft.ChatFormatting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.util.FastColor;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ComputeFovModifierEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.event.RenderLivingEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.registries.DeferredItem;
import net.minecraft.Util;
import net.minecraft.world.entity.Entity;
import techguns.TGComponents;
import techguns.TGItems;
import techguns.TGEntities;
import techguns.TGuns;
import techguns.Techguns;
import techguns.client.models.ModelMultipart;
import techguns.capabilities.TGExtendedPlayer;
import techguns.client.models.ModelObj;
import techguns.client.models.guns.*;
import techguns.client.models.items.ModelARMagazine;
import techguns.client.models.items.ModelAS50Mag;
import techguns.client.models.items.ModelLmgMag;
import techguns.client.models.projectiles.ModelRocket;
import techguns.client.render.GenericProjectileRenderer;
import techguns.client.render.GunAnimation;
import techguns.client.render.GrenadeRenderer;
import techguns.client.particle.TGParticleManager;
import techguns.client.render.GunRenderer;
import techguns.client.render.ItemModelRenderer;
import techguns.client.render.ScreenEffect;
import techguns.items.guns.GenericGun;
import techguns.items.guns.GenericGun.Crosshair;
import techguns.items.guns.GenericGunMeleeCharge;
import techguns.items.guns.GuidedMissileLauncher;
import techguns.items.guns.ammo.AmmoType;
import techguns.network.TGPackets.GunAction;

/** Client setup and per-frame hooks: gun input and zoom, held-item rendering, crosshairs and ammo HUD. */
@EventBusSubscriber(modid = Techguns.MODID, value = Dist.CLIENT)
public class TGClient {
	static final KeyMapping FORCE_RELOAD = new KeyMapping("techguns.key.forceReload", GLFW.GLFW_KEY_R, "techguns.key.categories.techguns");
	private static final ResourceLocation CROSSHAIRS = Techguns.id("textures/gui/tg_crosshairs.png");
	private static final ResourceLocation LOCK_ON = Techguns.id("textures/gui/crosshairs.png");

	/** The living entity currently being rendered, so the gun renderer knows who holds it in third person. */
	public static LivingEntity renderedEntity;
	/** Attack was clicked; kept until the gun is ready so semi-auto clicks during the fire delay aren't lost. */
	private static boolean firePressed;
	/** Use was clicked while dual-wielding: the off-hand gun's {@link #firePressed}. */
	private static boolean offFirePressed;
	/** Players whose ambient effect is running. */
	private static final Set<Entity> AMBIENT = Collections.newSetFromMap(new WeakHashMap<>());
	/** Where the shishkebap's flames start on its blade, in its model frame (blocks at model scale). */
	private static final Vec3 AMBIENT_FX_OFFSET = new Vec3(0, 0.75, 0);
	/** Holding attack on a block re-sends the click event every tick; only the first one is a semi-auto shot. */
	private static boolean attackHeld;
	private static boolean useHeld;
	/** 0 not zoomed, 1 zoomed, 2 zoomed in further (scoped guns, as in CS2), with {@link #zoomedGun}. */
	private static int zoomLevel;
	private static GenericGun zoomedGun;
	/** FOV of the second zoom, relative to the first. */
	private static final float SECOND_ZOOM = 0.4f;

	@SubscribeEvent
	static void registerKeys(RegisterKeyMappingsEvent event) {
		event.register(FORCE_RELOAD);
	}

	@SubscribeEvent
	static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
		event.registerEntityRenderer(TGEntities.GENERIC_PROJECTILE.get(), GenericProjectileRenderer::new);
		techguns.client.render.NpcRenderer.register(event);
	}

	@SubscribeEvent
	static void registerGuiLayers(RegisterGuiLayersEvent event) {
		event.registerAboveAll(Techguns.id("ammo"), TGClient::renderAmmoCount);
	}

	@SubscribeEvent
	static void registerItemExtensions(RegisterClientExtensionsEvent event) {
		registerAmmoRenderers(event);
		event.registerItem(gun(() -> new GunRenderer(new ModelHandgun(), 1)
				.setBaseRotation(ItemModelRenderer.GUN90)
				.setBaseTranslation(0, -0.2f, ItemModelRenderer.SCALE-0.1f)
				.setGUIScale(0.45f)
				.setMuzzleFx(ScreenEffect.muzzleFlash_gun, 0, 0.16f, -0.75f, 0.9f, 0)
				.setReloadAnim(GunAnimation.breechReload, -0.15f, 55.0f)
				.setReloadAnim3p(GunAnimation.breechReload, 0f, 55.0f)
				.setTransformTranslations(new float[][]{{0, 0.03f, -0.12f}, {0.0f, -0.05f, -0.09f}, {0.0f, 0.0f, 0}, {0.0f, 0.0f, 0}, {0, 0, 0f}})
				.setMuzzleFXPos3P(0.03f, -0.59f)
				.setRecoilAnim(GunAnimation.genericRecoil, 0.2f, 25.0f)), TGuns.HANDCANNON.get());
		event.registerItem(gun(() -> new GunRenderer(new ModelSawedOff(), 1)
				.setBaseRotation(ItemModelRenderer.GUN90)
				.setBaseTranslation(0, -0.2f, ItemModelRenderer.SCALE-0.1f)
				.setGUIScale(0.45f)
				.setMuzzleFx(ScreenEffect.muzzleFlash_gun, 0, 0.16f, -0.75f, 1.05f, 0)
				.setReloadAnim(GunAnimation.breechReload, -0.15f, 55.0f)
				.setReloadAnim3p(GunAnimation.breechReload, 0f, 55.0f)
				.setTransformTranslations(new float[][]{{0f, 0f, 0f}, {0f, -0.04f, 0f}, {0f, 0f, 0f}, {0f, 0f, 0f}, {0f, 0f, -0.05f}})
				.setMuzzleFXPos3P(0.06f, -0.49f)
				.setRecoilAnim(GunAnimation.genericRecoil, 0.2f, 20.0f)), TGuns.SAWEDOFF.get());
		event.registerItem(gun(() -> new GunRenderer(new ModelRevolver(), 1)
				.setBaseRotation(ItemModelRenderer.GUN90)
				.setBaseTranslation(-0.35f, -0.2f, ItemModelRenderer.SCALE*0.5f-0.1f)
				.setBaseScale(0.75f)
				.setGUIScale(0.75f)
				.setMuzzleFx(ScreenEffect.muzzleFlash_gun, 0, 0.25f, -0.41f, 0.5f, 0)
				.setTransformTranslations(new float[][]{{0f, 0.14f, 0.01f}, {0f, 0.0f, 0.0f}, {0.05f, 0.01f, 0f}, {0f, 0f, 0f}, {0f, 0f, 0f}})
				.setMuzzleFXPos3P(0.09f, -0.30f)
				.setRecoilAnim(GunAnimation.genericRecoil, 0.1f, 10.0f)), TGuns.REVOLVER.get());
		event.registerItem(gun(() -> new GunRenderer(new ModelGoldenRevolver(), 1)
				.setBaseRotation(ItemModelRenderer.GUN90)
				.setBaseTranslation(-0.35f, -0.2f, ItemModelRenderer.SCALE*0.5f-0.1f)
				.setBaseScale(0.75f)
				.setGUIScale(0.75f)
				.setMuzzleFx(ScreenEffect.muzzleFlash_gun, 0, 0.25f, -0.41f, 0.5f, 0)
				.setTransformTranslations(new float[][]{{0f, 0.14f, 0.01f}, {0f, 0.0f, 0.0f}, {0.05f, 0.01f, 0f}, {0f, 0f, 0f}, {0f, 0f, 0f}})
				.setMuzzleFXPos3P(0.09f, -0.30f)
				.setRecoilAnim(GunAnimation.genericRecoil, 0.1f, 15.0f)), TGuns.GOLDENREVOLVER.get());
		event.registerItem(gun(() -> new GunRenderer(new ModelThompson(), 1)
				.setBaseRotation(ItemModelRenderer.GUN90)
				.setBaseTranslation(0, -0.2f, ItemModelRenderer.SCALE*0.5f-0.1f)
				.setGUIScale(0.45f)
				.setMuzzleFx(ScreenEffect.muzzleFlash_rifle, 0, 0.14f, -0.75f, 0.55f, 0)
				.setMuzzleFXPos3P(0.1f, -0.59f)
				.setMuzzleFlashJitter(0.02f, 0.02f, 5.0f, 0.1f)), TGuns.THOMPSON.get());
		event.registerItem(gun(() -> new GunRenderer(new ModelAK(), 1)
				.setBaseTranslation(ItemModelRenderer.SCALE*0.5f, -0.1f, 0)
				.setBaseScale(0.75f)
				.setGUIScale(0.35f)
				.setMuzzleFx(ScreenEffect.muzzleFlash_rifle, 0, 0.18f, -1.36f, 0.8f, 0)
				.setRecoilAnim(GunAnimation.genericRecoil, 0.1f, 4.0f)
				.setTransformTranslations(new float[][]{{0f, 0.06f, -0.02f}, {0f, 0f, -0.08f}, {0.06f, -0.01f, 0f}, {0f, 0f, 0f}, {0f, 0f, -0.05f}})
				.setMuzzleFXPos3P(0.08f, -1.02f)
				.setMuzzleFlashJitter(0.02f, 0.02f, 5.0f, 0.1f)), TGuns.AK47.get());
		event.registerItem(gun(() -> new GunRenderer(new ModelBoltaction(), 1)
				.setBaseRotation(ItemModelRenderer.GUN90)
				.setBaseTranslation(0, -0.2f, ItemModelRenderer.SCALE-0.1f)
				.setBaseScale(1.35f)
				.setGUIScale(0.35f)
				.setMuzzleFx(ScreenEffect.muzzleFlash_gun, 0, 0.21f, -1.48f, 0.9f, 0)
				.setScope(ScreenEffect.sniperScope)
				.setTransformTranslations(new float[][]{{0f, -0.02f, -0.09f}, {0f, -0.04f, -0.11f}, {0.1f, -0.08f, 0f}, {0f, 0f, 0f}, {0f, 0f, -0.025f}})
				.setMuzzleFXPos3P(0.12f, -1.13f)
				.setScopeKick(3f)
				.setScopeLenses(true, 0f, 1.2f, 6.1f, -4.5f, 0.5f, -7.1f, -4.5f, 0.5f)), TGuns.BOLTACTION.get());
		event.registerItem(gun(() -> new GunRenderer(new ModelM4(), 1)
				.setBaseTranslation(ItemModelRenderer.SCALE*0.5f, -0.1f, 0)
				.setGUIScale(0.35f)
				.setMuzzleFx(ScreenEffect.muzzleFlash_rifle, 0, 0.18f, -1.29f, 0.75f, 0)
				.setRecoilAnim(GunAnimation.genericRecoil, 0.1f, 4.0f)
				.setTransformTranslations(new float[][]{{0f, 0f, -0.05f}, {0f, 0.01f, -0.1f}, {0f, 0f, 0f}, {0f, 0f, 0f}, {0f, 0f, -0.05f}})
				.setMuzzleFXPos3P(0.13f, -1f)
				.setMuzzleFlashJitter(0.02f, 0.02f, 5.0f, 0.1f)), TGuns.M4.get());
		event.registerItem(gun(() -> new GunRenderer(new ModelM4Infiltrator(), 1)
				.setBaseTranslation(ItemModelRenderer.SCALE*0.5f, -0.1f, 0)
				.setGUIScale(0.35f)
				.setRecoilAnim(GunAnimation.genericRecoil, 0.1f, 4.0f)
				.setTransformTranslations(new float[][]{{0f, 0f, -0.05f}, {0f, 0.01f, -0.1f}, {0f, 0f, 0f}, {0f, 0f, 0f}, {0f, 0f, -0.05f}})
				.setMuzzleFXPos3P(0.14f, -1.15f)
				.setScope(ScreenEffect.sniperScope)
				.setScopeKick(1.25f)
				.setScopeLenses(false, 45f, 1.4f, -0.5f, -10.17f, -6.1f, -0.5f, -9.88f, 7.1f)), TGuns.M4_INFILTRATOR.get());
		event.registerItem(gun(() -> new GunRenderer(new ModelCombatShotgun(), 2)
				.setBaseRotation(ItemModelRenderer.GUN90)
				.setBaseTranslation(0, -0.2f, ItemModelRenderer.SCALE-0.1f)
				.setGUIScale(0.45f)
				.setMuzzleFx(ScreenEffect.muzzleFlash_gun, 0, 0.21f, -0.91f, 0.75f, 0)
				.setTransformTranslations(new float[][]{{0f, 0.03f, 0f}, {0f, -0.01f, -0.10f}, {0.05f, 0f, 0f}, {0f, 0f, 0f}, {0f, 0f, -0.05f}})
				.setMuzzleFXPos3P(0.12f, -0.81f)
				.setRecoilAnim(GunAnimation.genericRecoil, 0.3f, 15.0f)), TGuns.COMBATSHOTGUN.get());
		event.registerItem(gun(() -> new GunRenderer(new ModelMac10(), 1)
				.setBaseTranslation(ItemModelRenderer.SCALE*0.5f, -0.45f, -0.3f)
				.setBaseScale(1.2f)
				.setGUIScale(0.55f)
				.setMuzzleFx(ScreenEffect.muzzleFlash_rifle, 0, 0.23f, -0.46f, 0.5f, 0)
				.setRecoilAnim(GunAnimation.genericRecoil, 0.06f, 3.0f)
				.setTransformTranslations(new float[][]{{0f, 0f, -0.05f}, {0f, -0.10f, 0.01f}, {-0.02f, -0.02f, 0f}, {0f, 0.03f, 0f}, {0f, -0.05f, -0.05f}})
				.setMuzzleFXPos3P(0.07f, -0.26f)
				.setMuzzleFlashJitter(0.02f, 0.05f, 5.0f, 0.1f)), TGuns.MAC10.get());
		event.registerItem(gun(() -> new GunRenderer(new ModelLMG(), 1)
				.setBaseTranslation(ItemModelRenderer.SCALE*0.5f, -0.1f, 0)
				.setGUIScale(0.35f)
				.setMuzzleFx(ScreenEffect.muzzleFlash_rifle, 0, 0.21f, -1.5f, 0.78f, 0)
				.setMuzzleFXPos3P(0.13f, -0.98f)
				.setRecoilAnim(GunAnimation.genericRecoil, 0.01f, 2.0f)
				.setTransformTranslations(new float[][]{{0f, 0.02f, -0.09f}, {0f, 0f, -0.06f}, {0.05f, -0.03f, 0f}, {0f, 0f, 0f}, {0f, 0f, -0.05f}})
				.setMuzzleFXPos3P(0.14f, -1.02f)
				.setMuzzleFlashJitter(0.03f, 0.03f, 5.0f, 0.1f)), TGuns.LMG.get());
		event.registerItem(gun(() -> new GunRenderer(new ModelMinigun(), 2)
				.setBaseRotation(ItemModelRenderer.GUN90)
				.setBaseTranslation(0, -0.2f, ItemModelRenderer.SCALE*0.5f)
				.setBaseScale(1.2f)
				.setGUIScale(0.3f)
				.setMuzzleFx(ScreenEffect.muzzleFlash_minigun, -0.04f, 0.05f, -1.25f, 0.8f, 0.04f)
				.setTransformTranslations(new float[][]{{0f, -0.16f, 0.1f}, {0f, -0.53f, 0.2f}, {0.12f, -0.02f, 0f}, {0f, 0f, 0f}, {0f, 0f, -0.05f}})
				.setMuzzleFXPos3P(-0.38f, -0.78f)
				.setRecoilAnim(GunAnimation.genericRecoil, 0.05f, 3.0f)), TGuns.MINIGUN.get());
		event.registerItem(gun(() -> new GunRenderer(new ModelAS50(), 1)
				.setBaseTranslation(ItemModelRenderer.SCALE*0.5f, -0.1f, 0)
				.setBaseScale(0.85f)
				.setGUIScale(0.30f)
				.setMuzzleFx(ScreenEffect.muzzleFlash_rifle, 0, 0.29f, -1.82f, 1.15f, 0)
				.setRecoilAnim(GunAnimation.genericRecoil, 0.25f, 4.0f)
				.setTransformTranslations(new float[][]{{0f, 0.06f, -0.1f}, {0f, 0.0f, -0.05f}, {0.13f, -0.09f, -0.05f}, {0f, 0f, 0f}, {0f, -0.2f, -0.05f}})
				.setMuzzleFXPos3P(0.17f, -1.29f)
				.setScope(ScreenEffect.sniperScope)
				.setRecoilAnim(GunAnimation.genericRecoil, 0.25f, 5.0f)
				.setScopeKick(3f)
				.setScopeLenses(false, 45f, 1.8f, -0.5f, -15.46f, -18.1f, -0.5f, -15.42f, 13.1f)), TGuns.AS50.get());
		event.registerItem(gun(() -> new GunRenderer(new ModelVector(), 1)
				.setBaseTranslation(ItemModelRenderer.SCALE*0.5f, -0.57f, -0.2f)
				.setBaseScale(1.1f)
				.setGUIScale(0.45f)
				.setMuzzleFx(ScreenEffect.muzzleFlash_rifle, 0, 0.10f, -0.72f, 0.60f, 0)
				.setRecoilAnim(GunAnimation.genericRecoil, 0.05f, 2.0f)
				.setTransformTranslations(new float[][]{{0f, -0.17f, 0.0f}, {0f, -0.2f, -0.04f}, {-0.08f, -0.09f, 0f}, {0f, 0f, 0f}, {0.05f, -0.17f, -0.05f}})
				.setMuzzleFXPos3P(0.0f, -0.53f)
				.setMuzzleFlashJitter(0.02f, 0.02f, 5.0f, 0.1f)), TGuns.VECTOR.get());
		event.registerItem(gun(() -> new GunRenderer(new ModelScar(), 2)
				.setBaseTranslation(ItemModelRenderer.SCALE*0.5f, -0.1f, 0.1f)
				.setGUIScale(0.35f)
				.setMuzzleFx(ScreenEffect.muzzleFlash_rifle, 0, 0.23f, -1.48f, 0.78f, 0)
				.setRecoilAnim(GunAnimation.genericRecoil, 0.1f, 4.0f)
				.setTransformTranslations(new float[][]{{0f, 0.04f, -0.15f}, {0f, 0.02f, -0.11f}, {0.05f, 0f, 0f}, {0f, 0f, 0f}, {0.02f, -0.09f, -0.05f}})
				.setMuzzleFXPos3P(0.14f, -1.04f)
				.setMuzzleFlashJitter(0.02f, 0.02f, 5.0f, 0.1f)
				.setScope(ScreenEffect.sniperScope)
				.setScopeKick(2.25f)
				.setScopeLenses(false, 45f, 1.1f, -0.5f, -10.2f, -3.1f, -0.5f, -10.2f, 11.1f)), TGuns.SCAR.get());
		event.registerItem(gun(() -> new GunRenderer(new ModelAUG(), 2)
				.setBaseTranslation(ItemModelRenderer.SCALE*0.5f, -0.1f, 0.1f)
				.setGUIScale(0.35f)
				.setMuzzleFx(ScreenEffect.muzzleFlash_rifle, 0, 0.19f, -1.45f, 0.75f, 0)
				.setRecoilAnim(GunAnimation.genericRecoil, 0.1f, 4.0f)
				.setTransformTranslations(new float[][]{{0f, 0.01f, -0.15f}, {0f, 0.0f, -0.01f}, {0f, 0f, 0f}, {0f, 0f, 0f}, {0.02f, 0f, -0.05f}})
				.setMuzzleFXPos3P(0.12f, -0.87f)
				.setMuzzleFlashJitter(0.02f, 0.02f, 5.0f, 0.1f)
				.setScope(ScreenEffect.sniperScope)
				.setScopeKick(1.38f)
				.setScopeLenses(false, 45f, 1.0f, -0.5f, -9.59f, -8.1f, -0.5f, -9.38f, 11.1f)), TGuns.AUG.get());
		event.registerItem(gun(() -> new GunRenderer(new ModelPDW(), 1)
				.setBaseRotation(ItemModelRenderer.GUN90)
				.setBaseTranslation(0, -0.2f, ItemModelRenderer.SCALE*1.5f-0.1f)
				.setGUIScale(0.55f)
				.setMuzzleFx(ScreenEffect.muzzleFlash_blue, 0, 0.13f, -0.58f, 0.55f, 0)
				.setTransformTranslations(new float[][]{{0.0f, 0.09f, -0.04f}, {0f, 0.06f, -0.02f}, {0f, 0f, 0f}, {0f, 0f, 0f}, {0f, 0f, -0.05f}})
				.setMuzzleFXPos3P(0.11f, -0.41f)
				.setMuzzleFlashJitter(0.01f, 0.01f, 5.0f, 0.1f)
				.setRecoilAnim(GunAnimation.genericRecoil, 0.06f, 4.0f)), TGuns.PDW.get());
		event.registerItem(gun(() -> new GunRenderer(new ModelPulseRifle(), 1)
				.setBaseRotation(ItemModelRenderer.GUN90)
				.setBaseTranslation(0, -0.2f, ItemModelRenderer.SCALE*1.5f - 0.09f)
				.setGUIScale(0.45f)
				.setMuzzleFx(ScreenEffect.muzzleFlash_blue, 0, 0.22f, -0.76f, 0.6f, 0)
				.setTransformTranslations(new float[][]{{0f, 0.16f, 0.01f}, {0f, 0.05f, 0.08f}, {0.05f, 0f, 0f}, {0f, 0f, 0f}, {0f, 0f, -0.05f}})
				.setMuzzleFXPos3P(0.1f, -0.50f)
				.setScope(ScreenEffect.techScope, 2.125f)
				.setScopeKick(2f)
				.setRecoilAnim(GunAnimation.pulseRifleRecoil, 0.25f, 10.0f)
				.setScopeLenses(true, 0f, 1.0f, 4.6f, -3.5f, 0f, -5.1f, -3.5f, 0f)), TGuns.PULSERIFLE.get());
		event.registerItem(gun(() -> new GunRenderer(new ModelPistol(), 2)
				.setBaseTranslation(ItemModelRenderer.SCALE*0.5f, -0.3f, -0.4f)
				.setBaseScale(1.2f)
				.setGUIScale(0.9f)
				.setMuzzleFx(ScreenEffect.muzzleFlash_gun, 0.03f, 0.2f, -0.5f, 0.55f, -0.03f)
				.setTransformTranslations(new float[][]{{0, 0.09f, -0.02f}, {0.0f, -0.03f, 0.0f}, {0.02f, -0.08f, 0}, {0.02f, -0.08f, 0}, {0, 0, 0f}})
				.setRecoilAnim(GunAnimation.genericRecoil, 0.025f, 12.0f)
				.setMuzzleFlashJitter(0.01f, 0.01f, 5.0f, 0.05f)
				.setMuzzleFXPos3P(0.07f, -0.26f)), TGuns.PISTOL.get());

		// Energy guns (M3.2)
		event.registerItem(gun(() -> new GunRenderer(new ModelFlamethrower(), 1)
				.setBaseRotation(ItemModelRenderer.GUN90)
				.setBaseTranslation(0, -0.2f, ItemModelRenderer.SCALE-0.1f)
				.setGUIScale(0.45f)
				.setMuzzleFx(ScreenEffect.FlamethrowerMuzzleFlash, 0, 0.16f, -0.9f, 0.45f, 0)
				.setTransformTranslations(new float[][]{{0f, 0f, 0f}, {0f, 0f, -0.08f}, {0.05f, 0f, 0f}, {0f, 0f, 0f}, {0f, 0f, -0.05f}})
				.setMuzzleFXPos3P(0.1f, -0.63f)
				.setRecoilAnim(GunAnimation.swayRecoil, 0.025f, 2.5f)
				.setIdleFx(ScreenEffect.FlamethrowerMuzzleFlame)), TGuns.FLAMETHROWER.get());
		event.registerItem(gun(() -> new GunRenderer(new ModelTeslaGun(), 1)
				.setBaseRotation(ItemModelRenderer.GUN90)
				.setBaseTranslation(0.25f, -0.2f, ItemModelRenderer.SCALE-0.09f)
				.setGUIScale(0.45f)
				.setMuzzleFx(ScreenEffect.muzzleFlashLightning, 0, 0.26f, -0.67f, 0.5f, 0)
				.setTransformTranslations(new float[][]{{0f, 0.15f, 0.04f}, {0f, 0.05f, -0.08f}, {0.03f, 0.01f, 0f}, {0f, 0f, 0f}, {0f, 0f, -0.05f}})
				.setMuzzleFXPos3P(0.12f, -0.65f)
				.setRecoilAnim(GunAnimation.genericRecoil, 0.2f, 5.0f)), TGuns.TESLAGUN.get());
		event.registerItem(gun(() -> new GunRenderer(new ModelNetherBlaster(), 1)
				.setBaseTranslation(0f, -0.2f, ItemModelRenderer.SCALE-0.09f)
				.setGUIScale(0.60f)
				.setMuzzleFx(ScreenEffect.muzzleFlashFireball_alpha, 0, 0.29f, -0.33f, 0.5f, 0)
				.setTransformTranslations(new float[][]{{0f, 0.15f, 0.04f}, {0f, -0.16f, -0.24f}, {-0.10f, 0.01f, 0f}, {0f, 0f, -0.11f}, {0.16f, -0.07f, -0.16f}})
				.setMuzzleFXPos3P(-0.06f, -0.45f)), TGuns.NETHERBLASTER.get());
		event.registerItem(gun(() -> new GunRenderer(new ModelLasergun(), 1)
				.setBaseRotation(ItemModelRenderer.GUN90)
				.setBaseTranslation(0.25f, -0.2f, ItemModelRenderer.SCALE*0.5f-0.10f)
				.setBaseScale(1.1f)
				.setGUIScale(0.40f)
				.setMuzzleFx(ScreenEffect.muzzleFlashLaser, 0, 0.30f, -1.06f, 0.5f, 0)
				.setTransformTranslations(new float[][]{{0f, 0.15f, 0.04f}, {0f, 0.02f, 0.01f}, {0.13f, 0.01f, 0f}, {0f, 0f, 0.15f}, {-0.18f, 0f, -0.05f}})
				.setMuzzleFXPos3P(0.11f, -0.83f)
				.setRecoilAnim(GunAnimation.genericRecoil, 0.2f, 5.0f)), TGuns.LASERGUN.get());
		event.registerItem(gun(() -> new GunRenderer(new ModelAlienBlaster(), 1)
				.setBaseTranslation(ItemModelRenderer.SCALE*0.5f, -0.3f, -0.2f)
				.setBaseScale(0.75f)
				.setGUIScale(0.75f)
				.setMuzzleFx(ScreenEffect.muzzleFlashAlienBlaster, 0f, 0.28f, -0.52f, 0.55f, 0f)
				.setTransformTranslations(new float[][]{{0, 0.06f, -0.02f}, {0.0f, -0.07f, -0.01f}, {0.02f, -0.08f, 0}, {0.02f, -0.08f, 0}, {-0.04f, -0.09f, 0f}})
				.setRecoilAnim(GunAnimation.genericRecoil, 0.025f, 10.0f)
				.setMuzzleFXPos3P(0.07f, -0.36f)
				.setReloadAnim(GunAnimation.breechReload, -0.15f, 55.0f)
				.setReloadAnim3p(GunAnimation.breechReload, 0f, 55.0f)), TGuns.ALIENBLASTER.get());
		event.registerItem(gun(() -> new GunRenderer(new ModelBlasterRifle(), 1)
				.setBaseTranslation(ItemModelRenderer.SCALE*0.5f, -0.1f, 0)
				.setBaseScale(0.9f)
				.setGUIScale(0.45f)
				.setMuzzleFx(ScreenEffect.muzzleFlashLaser, 0, 0.24f, -0.93f, 0.75f, 0)
				.setRecoilAnim(GunAnimation.genericRecoil, 0.1f, 4.0f)
				.setTransformTranslations(new float[][]{{0f, 0.11f, -0.20f}, {0f, 0.0f, -0.04f}, {0f, 0f, 0.03f}, {0f, 0f, 0f}, {0f, 0f, -0.05f}})
				.setMuzzleFXPos3P(0.09f, -0.57f)
				.setScope(ScreenEffect.sniperScope)
				.setScopeKick(1.25f)
				.setScopeLenses(false, 0f, 1.0f, -0.5f, -7.0f, 0.9f, -0.5f, -8.0f, 16.1f)), TGuns.BLASTERRIFLE.get());
		event.registerItem(gun(() -> new GunRenderer(new ModelSonicShotgun(), 1)
				.setBaseRotation(ItemModelRenderer.GUN90)
				.setBaseTranslation(0, -0.2f, ItemModelRenderer.SCALE*0.5f-0.10f)
				.setBaseScale(1.0f)
				.setGUIScale(0.35f)
				.setMuzzleFx(ScreenEffect.muzzleFlashSonic, 0, 0.28f, -0.98f, 0.5f, 0)
				.setTransformTranslations(new float[][]{{0f, 0.10f, 0.04f}, {0f, -0.01f, -0.03f}, {0.08f, 0.0f, 0f}, {0f, 0.05f, 0.15f}, {-0.18f, 0f, -0.05f}})
				.setMuzzleFXPos3P(0.11f, -0.81f)), TGuns.SONICSHOTGUN.get());
		event.registerItem(gun(() -> new GunRenderer(new ModelLasergun2(), 1)
				.setBaseTranslation(ItemModelRenderer.SCALE*0.5f, -0.1f, 0.1f)
				.setGUIScale(0.35f)
				.setMuzzleFx(ScreenEffect.muzzleFlashLaser, 0, 0.22f, -1.09f, 0.75f, 0)
				.setRecoilAnim(GunAnimation.genericRecoil, 0.1f, 4.0f)
				.setTransformTranslations(new float[][]{{0f, 0.04f, -0.05f}, {0f, 0.01f, -0.1f}, {0f, 0f, 0f}, {0f, 0f, 0f}, {0f, 0f, -0.05f}})
				.setMuzzleFXPos3P(0.14f, -0.82f)), TGuns.SCATTERBEAMRIFLE.get());
		event.registerItem(gun(() -> new GunRenderer(new ModelNDR(), 1)
				.setBaseRotation(ItemModelRenderer.GUN90)
				.setBaseTranslation(1f, -0.2f, ItemModelRenderer.SCALE*1.5f-0.09f)
				.setBaseScale(1.2f)
				.setGUIScale(0.40f)
				.setMuzzleFx(ScreenEffect.muzzleFlashNukeBeam, 0, 0.19f, -0.91f, 0.65f, 0)
				.setTransformTranslations(new float[][]{{0f, 0.02f, 0.09f}, {-0.01f, 0.04f, 0.3f}, {0.11f, -0.08f, 0f}, {0f, 0f, 0.15f}, {-0.23f, -0.08f, -0.05f}})
				.setMuzzleFXPos3P(0.11f, -0.83f)
				.setRecoilAnim(GunAnimation.swayRecoil, 0.025f, 0.75f)), TGuns.NUCLEARDEATHRAY.get());
		event.registerItem(gun(() -> new GunRenderer(new ModelMibGun(), 1)
				.setBaseTranslation(0f, -0.56f, -0.02f)
				.setBaseScale(1.2f)
				.setGUIScale(0.75f)
				.setMuzzleFx(ScreenEffect.muzzleFlashMibGun, 0f, 0.26f, -0.42f, 0.55f, 0f)
				.setTransformTranslations(new float[][]{{0, 0.10f, -0.02f}, {0.0f, -0.04f, -0.01f}, {0.02f, -0.08f, 0}, {0.02f, -0.08f, 0}, {-0.04f, -0.09f, 0f}})
				.setRecoilAnim(GunAnimation.genericRecoil, 0.025f, 10.0f)
				.setMuzzleFXPos3P(0.06f, -0.31f)
				.setReloadAnim(GunAnimation.breechReload, -0.15f, 55.0f)
				.setReloadAnim3p(GunAnimation.breechReload, 0f, 55.0f)), TGuns.MIBGUN.get());
		event.registerItem(gun(() -> new GunRenderer(new ModelObj(Techguns.id("models/item/gaussrifle.obj")), 1)
				.setBaseRotation((pose, context) -> pose.mulPose(Axis.YP.rotationDegrees(-90f))) // 1.12 RenderGunBaseObj
				.setBaseTranslation(0.6f, 0f, ItemModelRenderer.SCALE*0.5f-0.09f)
				.setBaseScale(0.9f)
				.setGUIScale(0.25f)
				.setMuzzleFx(ScreenEffect.muzzleFlashSonic, 0, 0.21f, -1.56f, 1.0f, 0)
				.setTransformTranslations(new float[][]{{0f, 0.12f, -0.1f}, {0f, 0.05f, -0.17f}, {0.f, 0.06f, 0.06f}, {0f, 0f, 0.f}, {0f, 0f, -0.05f}})
				.setMuzzleFXPos3P(0.09f, -1.26f)
				.setRecoilAnim(GunAnimation.genericRecoil, 0.25f, 7.5f)
				.setScope(ScreenEffect.techScope, 2.125f)
				.setScopeKick(1.75f)
				.setScopeLenses(true, 0f, 1.2f, -14.6f, 6.5f, 1f, 0.6f, 6.5f, 1f)), TGuns.GAUSSRIFLE.get());
		event.registerItem(gun(() -> new GunRenderer(new ModelLaserPistol(), 1)
				.setBaseTranslation(ItemModelRenderer.SCALE*0.5f, -0.3f, -0.4f)
				.setBaseScale(1.2f)
				.setGUIScale(0.7f)
				.setMuzzleFx(ScreenEffect.muzzleFlashLaser, 0.03f, 0.2f, -0.5f, 0.55f, -0.03f)
				.setTransformTranslations(new float[][]{{0, 0.09f, -0.02f}, {0.0f, -0.03f, 0.0f}, {0.02f, -0.08f, 0}, {0.02f, -0.08f, 0}, {0, 0, 0f}})
				.setRecoilAnim(GunAnimation.genericRecoil, 0.0125f, 6.0f)
				.setMuzzleFXPos3P(0.07f, -0.26f)), TGuns.LASERPISTOL.get());
		event.registerItem(gun(() -> new GunRenderer(new ModelBiogun(), 1)
				.setBaseRotation(ItemModelRenderer.GUN90)
				.setBaseTranslation(0.35f, -0.2f, ItemModelRenderer.SCALE-0.1f)
				.setGUIScale(0.45f)
				.setMuzzleFx(ScreenEffect.muzzleGreenFlare, 0, 0.23f, -0.51f, 0.55f, 0)
				.setTransformTranslations(new float[][]{{0f, 0.16f, 0.05f}, {0f, 0.07f, -0.05f}, {0f, 0f, 0f}, {0f, 0f, 0f}, {0f, 0f, -0.05f}})
				.setMuzzleFXPos3P(0.1f, -0.51f)
				.setChargeTranslationAmount(0.05f)), TGuns.BIOGUN.get());
		event.registerItem(gun(() -> new GunRenderer(new ModelRocketLauncher(), 2) {
					// 1.12 RenderRocketLauncher: part 1 is the loaded rocket, textured by ammo variant.
					@Override
					protected ResourceLocation getTexture(ItemStack stack, int part) {
						if (part == 0) return getTexture(stack);
						String variant = stack.getOrDefault(TGComponents.AMMO_VARIANT, AmmoType.DEFAULT);
						return Techguns.id(switch (variant) {
							case AmmoType.NUKE -> "textures/guns/rocket_nuke.png";
							case AmmoType.HIGH_VELOCITY -> "textures/guns/rocket_hv.png";
							default -> "textures/guns/rocket.png";
						});
					}
				}
				.setBaseRotation(ItemModelRenderer.GUN90)
				.setBaseTranslation(-0.4f, -0.2f, -ItemModelRenderer.SCALE*0.5f)
				.setGUIScale(0.45f)
				.setMuzzleFx(ScreenEffect.muzzleFlash_gun, 0, 0.39f, -0.6f, 0.87f, 0)
				.setTransformTranslations(new float[][]{{-0.13f, 0.3f, 0.32f}, {0, 0.02f, 0.07f}, {-0.06f, 0.0f, 0.0f}, {0.0f, 0.0f, 0}, {0, 0, 0f}})
				.setMuzzleFXPos3P(0.09f, -0.26f)), TGuns.ROCKETLAUNCHER.get());
		event.registerItem(gun(() -> new GunRenderer(new ModelGrimReaper(), 1)
				.setBaseRotation(ItemModelRenderer.GUN90)
				.setBaseTranslation(0.3f, -0.2f, ItemModelRenderer.SCALE*0.5f)
				.setGUIScale(0.3f)
				.setChargeTranslationAmount(0.025f)
				.setMuzzleFx(ScreenEffect.muzzleFlash_gun, 0, 0.39f, -0.61f, 0.75f, 0)
				.setTransformTranslations(new float[][]{{0f, 0.25f, 0.18f}, {0, 0.13f, 0.1f}, {-0.02f, 0.01f, 0.0f}, {0.0f, 0.1f, 0}, {0, 0, 0f}})
				.setMuzzleFXPos3P(0.24f, -0.56f)
				.setBaseScale(1.25f)
				.setFirstPersonScale(0.4f)
				.setGroundAndFrameScale(0.35f)), TGuns.GRIMREAPER.get());
		event.registerItem(gun(() -> new GunRenderer(new ModelGrenadeLauncher(), 1)
				.setBaseRotation((pose, context) -> pose.mulPose(Axis.YP.rotationDegrees(90f))) // 1.12 RenderGunBaseObj
				.setBaseTranslation(0f, 0f, 0f)
				.setBaseScale(0.125f)
				.setGUIScale(0.45f)
				.setMuzzleFx(ScreenEffect.muzzleFlash_gun, 0, 0.22f, -0.63f, 0.5f, 0)
				.setTransformTranslations(new float[][]{{0f, 0.19f, -0.09f}, {0f, 0.06f, -0.20f}, {-0.05f, 0.08f, 0f}, {0f, 0.05f, -0.09f}, {0.11f, 0.01f, -0.05f}})
				.setMuzzleFXPos3P(0.07f, -0.61f)), TGuns.GRENADELAUNCHER.get());
		event.registerItem(gun(() -> new GunRenderer(new ModelGuidedMissileLauncher(), 1)
				.setBaseRotation(ItemModelRenderer.GUN90)
				.setBaseTranslation(-0.4f, -0.2f, ItemModelRenderer.SCALE*0.5f)
				.setGUIScale(0.35f)
				.setChargeTranslationAmount(0)
				.setMuzzleFx(ScreenEffect.muzzleFlash_gun, 0, 0.39f, -0.6f, 0.87f, 0)
				.setTransformTranslations(new float[][]{{-0.13f, 0.3f, 0.62f}, {0, 0.09f, 0.28f}, {0.0f, 0.03f, 0.0f}, {0.0f, 0.0f, 0}, {0, 0, -0.04f}})
				.setMuzzleFXPos3P(0.09f, -0.26f)), TGuns.GUIDEDMISSILELAUNCHER.get());
		event.registerItem(gun(() -> new GunRenderer(new ModelTFG(), 1)
				.setBaseRotation(ItemModelRenderer.GUN90)
				.setBaseTranslation(-0.46f, -0.38f, ItemModelRenderer.SCALE-0.125f)
				.setBaseScale(1.20f)
				.setGUIScale(0.30f)
				.setMuzzleFx(ScreenEffect.muzzleFlashTFG, 0.0f, 0.18f, -0.87f, 0.9f, 0)
				.setTransformTranslations(new float[][]{{0f, -0.03f, 0.16f}, {0f, -0.09f, -0.26f}, {0.04f, -0.04f, 0f}, {0f, 0f, 0f}, {-0.07f, 0f, -0.05f}})
				.setMuzzleFXPos3P(0.09f, -1.14f)
				.setChargeTranslationAmount(0.05f)
				.setFirstPersonScale(0.45f)), TGuns.TFG.get());

		// Powered melee tools (M3.4)
		event.registerItem(gun(() -> miningTool(new ModelPowerHammer(), "powerhammer", "powerhammer_obsidian", "powerhammer_carbon")
				.setBaseRotation(ItemModelRenderer.GUN90)
				.setBaseTranslation(0.15f, -0.2f, ItemModelRenderer.SCALE-0.09f)
				.setBaseScale(1.25f)
				.setGUIScale(0.45f)
				.setTransformTranslations(new float[][]{{0f, 0.18f, 0.09f}, {0f, 0.04f, 0.04f}, {0.03f, 0.01f, 0f}, {0f, 0f, 0f}, {-0.07f, -0.03f, -0.05f}})
				.setMuzzleFXPos3P(0.12f, -0.65f)
				.setChargeTranslationAmount(0.125f)), TGuns.POWERHAMMER.get());
		event.registerItem(gun(() -> new GunRenderer(new ModelChainsaw(), 2) {
					// 1.12 RenderGunChainsaw: the blades take the upgrade head's color.
					@Override
					protected int getColor(ItemStack stack, int part) {
						if (part != 1) return -1;
						return switch (((GenericGunMeleeCharge) stack.getItem()).getMiningHead(stack)) {
							case 1 -> FastColor.ARGB32.colorFromFloat(1.0f, 0.9f, 0.55f, 1.0f);
							case 2 -> FastColor.ARGB32.colorFromFloat(1.0f, 0.4f, 0.4f, 0.4f);
							default -> -1;
						};
					}
				}
				.setBaseRotation(ItemModelRenderer.GUN90)
				.setBaseTranslation(-0.4f, -0.2f, ItemModelRenderer.SCALE-0.09f)
				.setBaseScale(0.95f)
				.setGUIScale(0.45f)
				.setTransformTranslations(new float[][]{{0f, -0.08f, 0.15f}, {0f, -0.5f, 0.04f}, {0.03f, 0.01f, 0f}, {0f, 0f, 0f}, {-0.07f, -0.03f, -0.11f}})),
				TGuns.CHAINSAW.get());
		event.registerItem(gun(() -> miningTool(new ModelMiningDrill(), "miningdrill_obsidian", "miningdrill_carbon")
				.setBaseRotation(ItemModelRenderer.GUN90)
				.setBaseTranslation(0, -0.2f, -ItemModelRenderer.SCALE*0.5f)
				.setBaseScale(2.0f)
				.setGUIScale(0.35f)
				.setTransformTranslations(new float[][]{{0f, -0.03f, 0.0f}, {0f, -0.57f, 0.08f}, {0.01f, -0.01f, 0f}, {0f, 0f, 0f}, {0f, -0.08f, -0.05f}})
				.setRecoilAnim(GunAnimation.genericRecoil, 0.05f, 1.0f)), TGuns.MININGDRILL.get());
		event.registerItem(gun(() -> new GunRenderer(new ModelShishkebap(), 1)
				.setBaseTranslation(0, 0, 0)
				.setBaseScale(1.0f)
				.setGUIScale(0.3f)
				.setTransformTranslations(new float[][]{{0f, 0f, 0f}, {0f, 0f, 0f}, {0f, -0.25f, 0f}, {0f, 0f, 0f}, {0f, 0f, 0f}})
				.setReloadAnim(GunAnimation.breechReload, -0.15f, 55.0f)
				.setReloadAnim3p(GunAnimation.breechReload, 0f, 55.0f)), TGuns.SHISHKEBAP.get());

		event.registerItem(gun(() -> new GrenadeRenderer(new ModelStielgranate(), Techguns.id("textures/guns/stielgranate.png"), 180f)
				.setBaseScale(1f)
				.setGUIScale(0.8f)
				.setBaseTranslation(-ItemModelRenderer.SCALE*0.5f, 0.48f, -ItemModelRenderer.SCALE)
				.setTransformTranslations(new float[][]{{0f, -0.06f, 0f}, {0f, -0.15f, 0.06f}, {-0.05f, -0.49f, 0f}, {0f, -0.15f, 0f}, {0f, 0f, -0.05f}})),
				TGuns.STIELGRANATE.get());
		event.registerItem(gun(() -> new GrenadeRenderer(new ModelFragGrenade(true), Techguns.id("textures/guns/frag_grenade_texture.png"), 90f)
				.setBaseScale(1.25f)
				.setGUIScale(1.35f)
				.setBaseTranslation(-0.02f, 0.65f, -ItemModelRenderer.SCALE+0.02f)
				.setTransformTranslations(new float[][]{{0f, -0.06f, 0f}, {0f, -0.11f, -0.01f}, {-0.05f, -0.49f, 0f}, {0f, 0.f, 0f}, {0f, 0f, -0.05f}})),
				TGuns.FRAGGRENADE.get());
	}

	private static void registerAmmoRenderers(RegisterClientExtensionsEvent event) {
		float[][] arMag = {{0, 0.25f, 0}, {0, 0.25f, 0.05f}, {0.1f, 0.25f, 0}, {0, -0.1f, 0}, {0, 0, -0.05f}};
		float[][] lmgMag = {{0, 0.35f, 0}, {0, 0.15f, 0}, {0.1f, 0.25f, 0}, {0, 0, 0}, {0, 0.25f, -0.05f}};
		float[][] as50Mag = {{0, 0, 0}, {0, 0, 0}, {0, 0.05f, 0}, {0, -0.1f, 0}, {0, 0, 0}};
		float[][] rocket = {{0, 0, 0}, {0, -0.1f, 0.02f}, {0, 0, 0}, {0, 0, 0}, {0, 0, 0}};
		BiConsumer<DeferredItem<?>, Supplier<ItemModelRenderer>> reg = (item, factory) -> event.registerItem(gun(factory), item.get());
		reg.accept(TGItems.ASSAULTRIFLE_MAGAZINE, () -> arMag(false, "ar_mag").setTransformTranslations(arMag));
		reg.accept(TGItems.ASSAULTRIFLE_MAGAZINE_EMPTY, () -> arMag(true, "ar_mag").setTransformTranslations(arMag));
		reg.accept(TGItems.ASSAULTRIFLE_MAGAZINE_INCENDIARY, () -> arMag(false, "ar_mag_inc").setTransformTranslations(arMag));
		reg.accept(TGItems.LMG_MAGAZINE, () -> lmgMag(false, "lmg_mag").setTransformTranslations(lmgMag));
		reg.accept(TGItems.LMG_MAGAZINE_EMPTY, () -> lmgMag(true, "lmg_mag").setTransformTranslations(lmgMag));
		reg.accept(TGItems.LMG_MAGAZINE_INCENDIARY, () -> lmgMag(false, "lmg_mag_inc").setTransformTranslations(lmgMag));
		reg.accept(TGItems.AS50_MAGAZINE, () -> as50Mag(false, "as50_mag").setTransformTranslations(as50Mag));
		reg.accept(TGItems.AS50_MAGAZINE_EMPTY, () -> as50Mag(true, "as50_mag").setTransformTranslations(as50Mag));
		reg.accept(TGItems.AS50_MAGAZINE_INCENDIARY, () -> as50Mag(false, "as50_mag_inc").setTransformTranslations(as50Mag));
		reg.accept(TGItems.AS50_MAGAZINE_EXPLOSIVE, () -> as50Mag(false, "as50_mag_exp").setTransformTranslations(as50Mag));
		reg.accept(TGItems.ROCKET, () -> rocket("rocket").setTransformTranslations(rocket));
		reg.accept(TGItems.ROCKET_NUKE, () -> rocket("rocket_nuke").setTransformTranslations(rocket));
		reg.accept(TGItems.ROCKET_HIGH_VELOCITY, () -> rocket("rocket_hv").setTransformTranslations(rocket));
	}

	private static ItemModelRenderer arMag(boolean empty, String texture) {
		return new ItemModelRenderer(new ModelARMagazine(empty), 1, Techguns.id("textures/guns/" + texture + ".png"))
				.setBaseScale(1.25f).setGUIScale(0.85f).setBaseTranslation(0, -0.2f, 0);
	}

	private static ItemModelRenderer lmgMag(boolean empty, String texture) {
		return new ItemModelRenderer(new ModelLmgMag(empty), 1, Techguns.id("textures/guns/" + texture + ".png"))
				.setBaseRotation(ItemModelRenderer.LMG_MAG).setBaseScale(1.25f).setGUIScale(0.75f).setBaseTranslation(0, 0, 0.2f);
	}

	private static ItemModelRenderer as50Mag(boolean empty, String texture) {
		return new ItemModelRenderer(new ModelAS50Mag(empty), 1, Techguns.id("textures/guns/" + texture + ".png"))
				.setBaseScale(1.5f).setGUIScale(0.75f).setBaseTranslation(0.0325f, -0.2f, 0.33f);
	}

	private static ItemModelRenderer rocket(String texture) {
		return new ItemModelRenderer(new ModelRocket(), 1, Techguns.id("textures/guns/" + texture + ".png"))
				.setBaseRotation(ItemModelRenderer.ROCKET).setBaseScale(1.5f).setGUIScale(0.5f).setBaseTranslation(0, 0, 0.1f).setFirstPersonScale(0.35f);
	}

	/** 1.12 RenderMiningToolMultiTexture: part 1, the head, shows the upgrade head fitted (textures/guns/{name}.png by head level). */
	private static GunRenderer miningTool(ModelMultipart model, String... headTextures) {
		return new GunRenderer(model, 2) {
			@Override
			protected ResourceLocation getTexture(ItemStack stack, int part) {
				int head = ((GenericGunMeleeCharge) stack.getItem()).getMiningHead(stack);
				return part == 1 && head < headTextures.length ? Techguns.id("textures/guns/" + headTextures[head] + ".png") : getTexture(stack);
			}
		};
	}

	/** Renderer is built on first use: the BEWLR constructor needs Minecraft's renderers to exist. Guns are held aimed. */
	private static IClientItemExtensions gun(Supplier<? extends BlockEntityWithoutLevelRenderer> factory) {
		return new IClientItemExtensions() {
			private BlockEntityWithoutLevelRenderer renderer;

			@Override
			public BlockEntityWithoutLevelRenderer getCustomRenderer() {
				if (renderer == null) renderer = factory.get();
				return renderer;
			}

			@Override
			public HumanoidModel.ArmPose getArmPose(LivingEntity entity, InteractionHand hand, ItemStack stack) {
				return hand == InteractionHand.MAIN_HAND && stack.getItem() instanceof GenericGun gun && gun.hasBowAnim() ? HumanoidModel.ArmPose.BOW_AND_ARROW : null;
			}
		};
	}

	public static boolean isZooming(GenericGun gun) {
		return gun.canZoom() && zoomLevel > 0 && zoomedGun == gun;
	}

	/** 1.12 TGGuiEvents lock-on reticle: four corners close in as the lock builds, then a blinking frame. */
	private static void renderLockOn(GuiGraphics g, Minecraft mc, GuidedMissileLauncher launcher) {
		float progress = launcher.lockProgress();
		int x = g.guiWidth() / 2;
		int y = g.guiHeight() / 2;
		int offset = (int) Math.max(0.0f, (1.0f - progress) * 16.0f) + 5;
		g.blit(LOCK_ON, x - offset - 3, y - offset - 3, 0, 0, 7, 7);
		g.blit(LOCK_ON, x + offset - 3, y - offset - 3, 7, 0, 7, 7);
		g.blit(LOCK_ON, x - offset - 3, y + offset - 3, 14, 0, 7, 7);
		g.blit(LOCK_ON, x + offset - 3, y + offset - 3, 21, 0, 7, 7);
		g.blit(LOCK_ON, x, y, 0, 0, 1, 1);
		String name = GuidedMissileLauncher.lockTarget().getName().getString();
		if (progress < 1.0f) {
			g.drawString(mc.font, "Locking... : " + name, x + 2, y + 10, 0xFFFFFFFF, false);
		} else {
			g.blit(LOCK_ON, x - 6, y - 6, 28, 0, 13, 13);
			if (Util.getMillis() / 250 % 2 == 0) g.blit(LOCK_ON, x - 9, y - 9, 41, 0, 19, 19);
			g.drawString(mc.font, "Locked On: " + name, x + 2, y + 10, 0xFFFF0000, false);
		}
	}

	private static boolean isReloading(LocalPlayer player) {
		ShooterValues.AttackTime time = ShooterValues.get(player, InteractionHand.MAIN_HAND);
		return ShooterValues.progress(time.reloadEnd, time.reloadTotal) > 0;
	}

	/** Left click with a gun shoots instead of attacking or mining; melee tools attack and mine. */
	@SubscribeEvent
	static void onClickInput(InputEvent.InteractionKeyMappingTriggered event) {
		LocalPlayer player = Minecraft.getInstance().player;
		if (event.isAttack() && player != null && player.getMainHandItem().getItem() instanceof GenericGun gun && gun.isShootWithLeftClick()) {
			event.setCanceled(true);
			event.setSwingHand(false);
			if (!attackHeld) firePressed = true;
			attackHeld = true;
		} else if (event.isUseItem() && player != null && GenericGun.isDualWielding(player)) {
			event.setCanceled(true);
			event.setSwingHand(false);
			if (!useHeld) offFirePressed = true;
			useHeld = true;
		} else if (event.isUseItem() && event.getHand() == InteractionHand.MAIN_HAND && player != null
				&& player.getMainHandItem().getItem() instanceof GenericGun gun && gun.canZoom()) {
			// Right click zooms in, with a scope once more, then out; the click still goes on to blocks and the off hand (shield) as in 1.12.
			if (!useHeld && !isReloading(player)) {
				boolean scoped = IClientItemExtensions.of(player.getMainHandItem()).getCustomRenderer() instanceof GunRenderer r && r.hasScopeTexture();
				zoomLevel = isZooming(gun) ? (zoomLevel + 1) % (scoped ? 3 : 2) : 1;
				zoomedGun = gun;
			}
			useHeld = true;
		}
	}

	@SubscribeEvent
	static void onComputeFov(ComputeFovModifierEvent event) {
		if (zoomLevel > 0) event.setNewFovModifier(event.getNewFovModifier() * zoomedGun.getZoomMult() * (zoomLevel == 2 ? SECOND_ZOOM : 1));
	}

	@SubscribeEvent
	static void onClientTick(ClientTickEvent.Post event) {
		Minecraft mc = Minecraft.getInstance();
		LocalPlayer player = mc.player;
		if (!mc.options.keyAttack.isDown()) attackHeld = false;
		if (!mc.options.keyUse.isDown()) useHeld = false;
		if (player == null || !(player.getMainHandItem().getItem() instanceof GenericGun gun)) {
			firePressed = offFirePressed = false;
			zoomLevel = 0;
			while (FORCE_RELOAD.consumeClick()) {}
			return;
		}
		ItemStack stack = player.getMainHandItem();
		boolean dual = GenericGun.isDualWielding(player);
		if (!isZooming(gun) || isReloading(player) || dual) zoomLevel = 0;
		// Vanilla swallows attack clicks while an item is in use; the guided missile launcher fires while locking on.
		if (player.isUsingItem() && gun.isShootWithLeftClick() && mc.screen == null && mc.options.keyAttack.isDown() && !attackHeld) {
			firePressed = true;
			attackHeld = true;
		}
		boolean holding = !gun.isSemiAuto() && gun.isShootWithLeftClick() && mc.screen == null && mc.options.keyAttack.isDown();
		if ((firePressed || holding) && GenericGun.isReady(player, InteractionHand.MAIN_HAND)) {
			firePressed = false;
			boolean zooming = isZooming(gun);
			Entity target = gun instanceof GuidedMissileLauncher launcher ? launcher.lockedTarget() : null;
			PacketDistributor.sendToServer(new GunAction(InteractionHand.MAIN_HAND, false, zooming, target == null ? -1 : target.getId()));
			gun.shootGunPrimary(stack, player.level(), player, InteractionHand.MAIN_HAND, zooming, target);
		}
		ItemStack offStack = player.getOffhandItem();
		if (!dual) offFirePressed = false;
		else if ((offFirePressed || !((GenericGun) offStack.getItem()).isSemiAuto() && mc.screen == null && mc.options.keyUse.isDown())
				&& GenericGun.isReady(player, InteractionHand.OFF_HAND)) {
			offFirePressed = false;
			PacketDistributor.sendToServer(new GunAction(InteractionHand.OFF_HAND, false, false));
			((GenericGun) offStack.getItem()).shootGunPrimary(offStack, player.level(), player, InteractionHand.OFF_HAND, false);
		}
		while (FORCE_RELOAD.consumeClick()) {
			PacketDistributor.sendToServer(new GunAction(InteractionHand.MAIN_HAND, true, false));
			gun.tryForcedReload(stack, player.level(), player, InteractionHand.MAIN_HAND);
			if (dual) {
				PacketDistributor.sendToServer(new GunAction(InteractionHand.OFF_HAND, true, false));
				((GenericGun) offStack.getItem()).tryForcedReload(offStack, player.level(), player, InteractionHand.OFF_HAND);
			}
		}
	}

	/**
	 * 1.12 ambient effects (the shishkebap's flames): started on each player holding such a gun with ammo, ending when
	 * that stops. Drawn on the held model by its {@link GunRenderer}.
	 */
	@SubscribeEvent
	static void startAmbientFX(ClientTickEvent.Post event) {
		ClientLevel level = Minecraft.getInstance().level;
		if (level == null) return;
		for (Player player : level.players()) {
			if (AMBIENT.contains(player) || !hasAmbientFX(player)) continue;
			AMBIENT.add(player);
			TGParticleManager.createItemAttached(player, ((GenericGun) player.getMainHandItem().getItem()).getAmbientFX(), 1.2f, AMBIENT_FX_OFFSET, entity -> {
				if (hasAmbientFX(entity)) return true;
				AMBIENT.remove(entity);
				return false;
			});
		}
	}

	private static boolean hasAmbientFX(Entity entity) {
		return entity.isAlive() && entity instanceof Player player && player.getMainHandItem().getItem() instanceof GenericGun gun
				&& gun.getAmbientFX() != null && gun.getAmmo(player.getMainHandItem()) > 0;
	}

	@SubscribeEvent
	static void onRenderLivingPre(RenderLivingEvent.Pre<?, ?> event) {
		renderedEntity = event.getEntity();
	}

	@SubscribeEvent
	static void onRenderLivingPost(RenderLivingEvent.Post<?, ?> event) {
		renderedEntity = null;
	}

	/** Gun crosshairs (1.12 TGGuiEvents): parts pushed apart by the gun's spread; none while looking through a scope. */
	@SubscribeEvent
	static void onRenderCrosshair(RenderGuiLayerEvent.Pre event) {
		Minecraft mc = Minecraft.getInstance();
		if (!event.getName().equals(VanillaGuiLayers.CROSSHAIR) || mc.player == null || !mc.options.getCameraType().isFirstPerson()
				|| mc.getDebugOverlay().showDebugScreen() || !(mc.player.getMainHandItem().getItem() instanceof GenericGun gun)) {
			return;
		}
		ItemStack stack = mc.player.getMainHandItem();
		if (isZooming(gun) && IClientItemExtensions.of(stack).getCustomRenderer() instanceof GunRenderer r && r.hasScopeTexture()) {
			event.setCanceled(true);
			return;
		}
		if (gun instanceof GuidedMissileLauncher launcher && GuidedMissileLauncher.lockTarget() != null && launcher.lockProgress() > 0) {
			event.setCanceled(true);
			renderLockOn(event.getGuiGraphics(), mc, launcher);
			return;
		}
		Crosshair crosshair = gun.getCrosshair();
		if (crosshair == Crosshair.VANILLA) return;
		event.setCanceled(true);
		float spread = gun.getSpread();
		if (mc.player.isBlocking()) spread *= gun.getHandType() == GenericGun.GunHandType.ONE_HANDED ? 4.0f : 8.0f;
		spread *= gun.stanceSpread(mc.player, isZooming(gun));
		int s = Mth.clamp(Math.round(100 * spread), 1, 10);
		GuiGraphics g = event.getGuiGraphics();
		int x = g.guiWidth() / 2;
		int y = g.guiHeight() / 2;
		int u = (crosshair.ordinal() - 1) % 16 * 16;
		RenderSystem.enableBlend();
		RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.ONE_MINUS_DST_COLOR, GlStateManager.DestFactor.ONE_MINUS_SRC_COLOR,
				GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
		switch (crosshair) {
			case GUN_DYNAMIC -> {
				g.blit(CROSSHAIRS, x - (4 + s), y, 3, 7, 4, 1);
				g.blit(CROSSHAIRS, x + (1 + s), y, 8, 7, 4, 1);
				g.blit(CROSSHAIRS, x, y, 7, 7, 1, 1);
				g.blit(CROSSHAIRS, x, y - (4 + s), 7, 3, 1, 4);
				g.blit(CROSSHAIRS, x, y + (1 + s), 7, 8, 1, 4);
			}
			case FOUR_PARTS, FOUR_PARTS_SPIKED, QUAD_NO_CORNERS -> {
				g.blit(CROSSHAIRS, x - 1, y - 1, u + 6, 6, 3, 3);
				g.blit(CROSSHAIRS, x - 6 - s, y - 2, u, 5, 6, 5);
				g.blit(CROSSHAIRS, x + 1 + s, y - 2, u + 9, 5, 6, 5);
				g.blit(CROSSHAIRS, x - 2, y - 6 - s, u + 5, 0, 5, 6);
				g.blit(CROSSHAIRS, x - 2, y + 1 + s, u + 5, 9, 5, 6);
			}
			case HORIZONTAL_TWO_PART, HORIZONTAL_TWO_PART_LARGE, HORIZONTAL_TWO_PART_E, HORIZONTAL_SMALL -> {
				g.blit(CROSSHAIRS, x - 1, y - 1, u + 6, 6, 3, 3);
				g.blit(CROSSHAIRS, x - 6 - s, y - 7, u, 0, 6, 16);
				g.blit(CROSSHAIRS, x + 1 + s, y - 7, u + 9, 0, 6, 16);
			}
			case QUAD_CORNERS_DOT -> {
				g.blit(CROSSHAIRS, x - 1, y - 1, u + 6, 6, 3, 3);
				g.blit(CROSSHAIRS, x - 6 - s, y - 6 - s, u, 0, 6, 6);
				g.blit(CROSSHAIRS, x + 1 + s, y - 6 - s, u + 9, 0, 6, 6);
				g.blit(CROSSHAIRS, x - 6 - s, y + 1 + s, u, 9, 6, 6);
				g.blit(CROSSHAIRS, x + 1 + s, y + 1 + s, u + 9, 9, 6, 6);
			}
			case TRI -> {
				g.blit(CROSSHAIRS, x - 1, y - 1, u + 6, 6, 3, 3);
				g.blit(CROSSHAIRS, x - 7, y - 6 - s, u, 0, 16, 6);
				g.blit(CROSSHAIRS, x - 6 - s, y + 1 + s, u, 9, 6, 6);
				g.blit(CROSSHAIRS, x + 1 + s, y + 1 + s, u + 9, 9, 6, 6);
			}
			case TRI_INV -> {
				g.blit(CROSSHAIRS, x - 1, y - 1, u + 6, 6, 3, 3);
				g.blit(CROSSHAIRS, x - 6 - s, y - 6 - s, u, 0, 6, 6);
				g.blit(CROSSHAIRS, x + 1 + s, y - 6 - s, u + 9, 0, 6, 6);
				g.blit(CROSSHAIRS, x - 7, y + 1 + s, u, 9, 16, 6);
			}
			default -> g.blit(CROSSHAIRS, x - 7, y - 7, u, 0, 16, 16);
		}
		RenderSystem.defaultBlendFunc();
		RenderSystem.disableBlend();
	}

	/** "ammo/clipsize xMagazines" in the bottom right corner. */
	private static void renderAmmoCount(GuiGraphics graphics, DeltaTracker delta) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.options.hideGui || mc.player == null || !(mc.player.getMainHandItem().getItem() instanceof GenericGun gun)) return;
		ItemStack stack = mc.player.getMainHandItem();
		int mags = TGExtendedPlayer.countAmmo(mc.player, gun.getAmmoVariant(stack).ammo().get());
		String text = gun.getAmmo(stack) + "/" + gun.getClipsize() + ChatFormatting.YELLOW + "x" + mags;
		graphics.drawString(mc.font, text, graphics.guiWidth() - 1 - mc.font.width(text), graphics.guiHeight() - mc.font.lineHeight - 2, 0xFFFFFFFF, false);
	}
}
