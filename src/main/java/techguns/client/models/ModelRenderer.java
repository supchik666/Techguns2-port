package techguns.client.models;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

import org.joml.Quaternionf;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.core.Direction;

/**
 * The 1.12 ModelRenderer API over 1.21 {@link ModelPart.Cube}s, so the Techne-exported models port with only import changes.
 * As in 1.12, a box takes the mirror flag and texture size current when it is added. Draws into {@link LegacyGl}.
 */
public class ModelRenderer {
	public float textureWidth;
	public float textureHeight;
	private int textureOffsetX;
	private int textureOffsetY;
	public float rotationPointX, rotationPointY, rotationPointZ;
	public float rotateAngleX, rotateAngleY, rotateAngleZ;
	public float offsetX, offsetY, offsetZ;
	public boolean mirror;
	public boolean showModel = true;
	public boolean isHidden;
	/** Draws this part's own boxes full bright with bloom (not in 1.12; set by port_models.py on lamps and screens). */
	public boolean glow;
	public List<ModelRenderer> childModels;
	private final List<ModelPart.Cube> cubes = new ArrayList<>();
	private ModelPart part;

	public ModelRenderer(ModelBase model) {
		setTextureSize(model.textureWidth, model.textureHeight);
	}

	public ModelRenderer(ModelBase model, int u, int v) {
		this(model);
		setTextureOffset(u, v);
	}

	public ModelRenderer setTextureOffset(int u, int v) {
		textureOffsetX = u;
		textureOffsetY = v;
		return this;
	}

	public ModelRenderer addBox(float x, float y, float z, int width, int height, int depth) {
		return addBox(x, y, z, width, height, depth, 0.0f);
	}

	public ModelRenderer addBox(float x, float y, float z, int width, int height, int depth, float grow) {
		cubes.add(new ModelPart.Cube(textureOffsetX, textureOffsetY, x, y, z, width, height, depth, grow, grow, grow, mirror,
				textureWidth, textureHeight, EnumSet.allOf(Direction.class)));
		part = null;
		return this;
	}

	public void setRotationPoint(float x, float y, float z) {
		rotationPointX = x;
		rotationPointY = y;
		rotationPointZ = z;
	}

	public ModelRenderer setTextureSize(int width, int height) {
		textureWidth = width;
		textureHeight = height;
		return this;
	}

	public void addChild(ModelRenderer child) {
		if (childModels == null) childModels = new ArrayList<>();
		childModels.add(child);
	}

	/** Same transform order as 1.12: offset, rotation point, then Z, Y, X rotation; children inherit it. */
	public void render(float scale) {
		if (isHidden || !showModel) return;
		if (part == null) part = new ModelPart(cubes, Map.of());
		PoseStack pose = LegacyGl.pose;
		pose.pushPose();
		pose.translate(offsetX, offsetY, offsetZ);
		pose.translate(rotationPointX * scale, rotationPointY * scale, rotationPointZ * scale);
		if (rotateAngleX != 0 || rotateAngleY != 0 || rotateAngleZ != 0) {
			pose.mulPose(new Quaternionf().rotationZYX(rotateAngleZ, rotateAngleY, rotateAngleX));
		}
		if (childModels != null) {
			for (ModelRenderer child : childModels) child.render(scale);
		}
		pose.scale(scale * 16, scale * 16, scale * 16); // cubes are in pixels and ModelPart divides by 16
		if (glow) LegacyGl.enableFXLighting();
		part.render(pose, LegacyGl.consumer, LegacyGl.light, LegacyGl.overlay, LegacyGl.color);
		if (glow) LegacyGl.disableFXLighting();
		pose.popPose();
	}
}
