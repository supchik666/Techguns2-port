package techguns.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import techguns.client.ShooterValues;
import techguns.client.TGClient;
import techguns.client.models.ModelMultipart;
import techguns.client.particle.TGParticleManager;
import techguns.items.guns.GenericGun;
import techguns.items.guns.GenericGunCharge;

/**
 * A gun: recoil, reload and muzzle flash on top of {@link ItemModelRenderer}, and the scope overlay when zoomed
 * (1.12 RenderGunBase). A charge gun pulls back while charging and eases forward again through the recoil.
 */
public class GunRenderer extends ItemModelRenderer {
	private GunAnimation recoilAnim = GunAnimation.genericRecoil;
	private float[] recoilParams = {0.15f, 5.0f};
	private GunAnimation reloadAnim = GunAnimation.genericReload;
	private float[] reloadParams = {1.0f, 40.0f};
	/** Degrees the view kicks up per shot through the scope, see {@link ScopeLens}. */
	private float scopeKick;
	private GunAnimation recoilAnim3p = GunAnimation.genericRecoil;
	private float[] recoilParams3p = {0f, 5.0f};
	private GunAnimation reloadAnim3p = GunAnimation.genericReload;
	private float[] reloadParams3p = {0f, 40.0f};

	private ScreenEffect muzzleFX, idleFX;
	private float muzzleFX_x_r, muzzleFX_x_l, muzzleFX_y, muzzleFX_z, muzzleFX_scale = 1.0f;
	private float muzzleFX_3p_y, muzzleFX_3p_z, muzzleFX_3p_scale = 0.5f;
	private float jitterX, jitterY, jitterScale;
	private ScreenEffect scope;
	private float scopescale = 3.0f;
	private float chargeTranslation = 0.25f;
	/** Scope lens centers (x, y, z triples in model pixels) and radius; they glow. */
	private float[] lenses = {};
	private float lensRadius;
	private boolean lensesAlongX;
	private float lensRoll;
	/** Holder of the gun being drawn in hand, whose item-attached particles go on the model; null elsewhere. */
	private LivingEntity holder;

	public GunRenderer(ModelMultipart model, int parts) {
		super(model, parts, null);
		scaleGround = scaleThirdp;
	}

	public GunRenderer setRecoilAnim(GunAnimation anim, float... params) {
		recoilAnim = anim;
		recoilParams = params;
		return this;
	}

	public GunRenderer setReloadAnim(GunAnimation anim, float... params) {
		reloadAnim = anim;
		reloadParams = params;
		return this;
	}

	public GunRenderer setReloadAnim3p(GunAnimation anim, float... params) {
		reloadAnim3p = anim;
		reloadParams3p = params;
		return this;
	}

	public GunRenderer setScopeKick(float degrees) {
		scopeKick = degrees;
		return this;
	}

	public GunRenderer setMuzzleFx(ScreenEffect fx, float x, float y, float z, float scale, float x_l) {
		muzzleFX = fx;
		muzzleFX_x_r = x;
		muzzleFX_x_l = x_l;
		muzzleFX_y = y;
		muzzleFX_z = z;
		muzzleFX_scale = scale;
		return this;
	}

	/** Looping effect at the muzzle while not firing or reloading (the flamethrower pilot flame, 1.12 RenderGunFlamethrower). */
	public GunRenderer setIdleFx(ScreenEffect fx) {
		idleFX = fx;
		return this;
	}

	public GunRenderer setMuzzleFXPos3P(float y, float z) {
		muzzleFX_3p_y = y;
		muzzleFX_3p_z = z;
		return this;
	}

	/** Angle jitter isn't used by the 1.12 renderer either; kept so registrations read the same. */
	public GunRenderer setMuzzleFlashJitter(float x, float y, float angle, float scale) {
		jitterX = x;
		jitterY = y;
		jitterScale = scale;
		return this;
	}

	public GunRenderer setScope(ScreenEffect scope) {
		this.scope = scope;
		return this;
	}

	public GunRenderer setScope(ScreenEffect scope, float scale) {
		this.scope = scope;
		scopescale = scale;
		return this;
	}

	/**
	 * Glowing scope lenses: {@code radius}, then the x, y, z of each lens center in model pixels (an OBJ's units * 16).
	 * {@code alongX} for models whose barrel runs along x instead of z; {@code roll} turns the glint around the barrel
	 * axis, in degrees (45 for scope tubes turned on their edge).
	 */
	public GunRenderer setScopeLenses(boolean alongX, float roll, float radius, float... lenses) {
		lensesAlongX = alongX;
		lensRoll = roll;
		lensRadius = radius;
		this.lenses = lenses;
		return this;
	}

	public GunRenderer setChargeTranslationAmount(float value) {
		chargeTranslation = value;
		return this;
	}

	public boolean hasScopeTexture() {
		return scope != null;
	}

	/** The scope overlay, or null. */
	ScreenEffect getScope() {
		return scope;
	}

	float getScopeScale() {
		return scopescale;
	}

	float getScopeKick() {
		return scopeKick;
	}

	/** Degrees of camera shake per unscoped shot. ponytail: read off the recoil animation's tilt; add a per-gun setter if one feels off. */
	float getShake() {
		return 0.25f + (recoilParams.length > 1 ? recoilParams[1] : 0) * 0.04f;
	}

	// Covariant overrides so the 1.12 setter chains compile in any order.
	@Override
	public GunRenderer setBaseRotation(BaseRotation rotation) {
		super.setBaseRotation(rotation);
		return this;
	}

	@Override
	public GunRenderer setBaseTranslation(float x, float y, float z) {
		super.setBaseTranslation(x, y, z);
		return this;
	}

	@Override
	public GunRenderer setBaseScale(float baseScale) {
		super.setBaseScale(baseScale);
		return this;
	}

	@Override
	public GunRenderer setGUIScale(float scale) {
		super.setGUIScale(scale);
		return this;
	}

	@Override
	public GunRenderer setTransformTranslations(float[][] translations) {
		super.setTransformTranslations(translations);
		return this;
	}

	@Override
	protected ResourceLocation getTexture(ItemStack stack) {
		return ((GenericGun) stack.getItem()).getTexture(stack);
	}

	@Override
	protected void renderModel(ItemStack stack, ItemDisplayContext context, PoseStack pose, MultiBufferSource buffers, int light, int overlay,
			int ammo, float reloadProgress, float fireProgress, float chargeProgress) {
		super.renderModel(stack, context, pose, buffers, light, overlay, ammo, reloadProgress, fireProgress, chargeProgress);
		for (int i = 0; i < lenses.length; i += 3) {
			pose.pushPose();
			pose.translate(lenses[i] * SCALE, lenses[i + 1] * SCALE, lenses[i + 2] * SCALE);
			if (lensesAlongX) pose.mulPose(Axis.YP.rotationDegrees(90f));
			pose.mulPose(Axis.ZP.rotationDegrees(lensRoll));
			ScreenEffect.scopeLens.render(pose, buffers, 0, 0, 0, 0, lensRadius * 2 * SCALE, false);
			pose.popPose();
		}
		if (holder != null) {
			pose.pushPose();
			pose.mulPose(Axis.XP.rotationDegrees(180f)); // model up is -y
			TGParticleManager.renderAttached(holder, pose, buffers, Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false));
			pose.popPose();
		}
	}

	@Override
	public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
		GenericGun gun = (GenericGun) stack.getItem();
		boolean firstPerson = context.firstPerson();
		boolean thirdPerson = context == ItemDisplayContext.THIRD_PERSON_LEFT_HAND || context == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND;
		boolean leftHand = context == ItemDisplayContext.FIRST_PERSON_LEFT_HAND || context == ItemDisplayContext.THIRD_PERSON_LEFT_HAND;
		LivingEntity entity = firstPerson ? Minecraft.getInstance().player : thirdPerson ? TGClient.renderedEntity : null;

		float fireProgress = 0, reloadProgress = 0, muzzleProgress = 0, chargeProgress = 0;
		boolean offhand = false;
		ShooterValues.AttackTime time = null;
		if (entity != null) {
			offhand = leftHand == (entity.getMainArm() == HumanoidArm.RIGHT);
			time = ShooterValues.get(entity, offhand ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
			if (gun instanceof GenericGunCharge charge && !offhand && entity.isUsingItem()) {
				chargeProgress = Math.min(entity.getTicksUsingItem() / charge.getFullChargeTime(), 1.0f);
			} else {
				reloadProgress = ShooterValues.progress(time.reloadEnd, time.reloadTotal);
				if (reloadProgress == 0) fireProgress = ShooterValues.progress(time.recoilEnd, time.recoilTotal);
				if (fireProgress > 0 && gun instanceof GenericGunCharge) chargeProgress = (1.0f - fireProgress) * time.recoilCharge;
			}
			muzzleProgress = ShooterValues.progress(time.muzzleFlashEnd, time.muzzleFlashTotal);
		}

		pose.pushPose();
		pose.translate(0.5f, 0.5f, 0.5f); // ItemRenderer shifts custom renderers by -0.5; 1.12 drew from the origin
		if (firstPerson && !offhand && scope != null && TGClient.isZooming(gun)) {
			scope.render(pose, buffers, fireProgress, leftHand ? 0.56f : -0.56f, 0.52f, 0f, scopescale, false);
			pose.popPose();
			return;
		}
		pose.pushPose();
		applyTranslation(pose, context);
		if (firstPerson) {
			if (chargeProgress > 0) pose.translate(0, 0, chargeTranslation * chargeProgress);
			if (fireProgress > 0) {
				recoilAnim.play(pose, fireProgress, leftHand, recoilParams);
			} else if (reloadProgress > 0) {
				reloadAnim.play(pose, reloadProgress, leftHand, reloadParams);
			} else if (offhand && entity.isCrouching()) {
				pose.mulPose(Axis.XP.rotationDegrees(-35f)); // lower the off-hand gun while sneaking
			}
		} else if (thirdPerson) {
			if (fireProgress > 0) {
				recoilAnim3p.play(pose, fireProgress, leftHand, recoilParams3p);
			} else if (reloadProgress > 0) {
				reloadAnim3p.play(pose, reloadProgress, leftHand, reloadParams3p);
			}
		} else if (context == ItemDisplayContext.GUI) {
			pose.mulPose(Axis.YP.rotationDegrees(40.0f));
			pose.mulPose(Axis.XP.rotationDegrees(20.0f));
		} else if (context == ItemDisplayContext.FIXED) {
			pose.mulPose(Axis.YP.rotationDegrees(-90.0f));
		}
		holder = offhand ? null : entity;
		renderModel(stack, context, pose, buffers, light, overlay, gun.getAmmo(stack), reloadProgress, fireProgress, chargeProgress);
		holder = null;
		pose.popPose();

		if (muzzleProgress > 0 && muzzleFX != null) {
			if (firstPerson) {
				float x = (leftHand ? muzzleFX_x_l : muzzleFX_x_r) + jitterX * time.jitterX;
				muzzleFX.render(pose, buffers, muzzleProgress, x, muzzleFX_y + jitterY * time.jitterY, muzzleFX_z,
						muzzleFX_scale + jitterScale * time.jitterScale, false);
			} else {
				muzzleFX.render(pose, buffers, muzzleProgress, 0, muzzleFX_3p_y, muzzleFX_3p_z, muzzleFX_scale * muzzleFX_3p_scale, true);
			}
		} else if (idleFX != null && reloadProgress <= 0 && entity != null && !entity.isUnderWater()) {
			float p = (entity.level().getGameTime() % 30) / 30.0f;
			if (firstPerson) {
				idleFX.render(pose, buffers, p, leftHand ? muzzleFX_x_l : muzzleFX_x_r, muzzleFX_y, muzzleFX_z, muzzleFX_scale * 0.5f, false);
			} else {
				idleFX.render(pose, buffers, p, 0, muzzleFX_3p_y, muzzleFX_3p_z, muzzleFX_scale * 0.5f * muzzleFX_3p_scale, true);
			}
		}
		pose.popPose();
	}
}
