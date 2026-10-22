package techguns.client.models;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;

/**
 * Techne-style gun model rendered in parts (part 0 is the static body, higher parts move with the animation).
 * Boxes keep their 1.12 ModelRenderer numbers: texture offset, size, rotation point and rotation.
 */
public abstract class ModelMultipart {
	private final MeshDefinition mesh = new MeshDefinition();
	private ModelPart root;

	protected void box(String name, int u, int v, float w, float h, float d, float x, float y, float z, float rx, float ry, float rz) {
		mesh.getRoot().addOrReplaceChild(name, CubeListBuilder.create().texOffs(u, v).addBox(0, 0, 0, w, h, d), PartPose.offsetAndRotation(x, y, z, rx, ry, rz));
	}

	/** Call once all boxes are added. */
	protected void bake(int textureWidth, int textureHeight) {
		root = LayerDefinition.create(mesh, textureWidth, textureHeight).bakeRoot();
	}

	protected void render(PoseStack pose, VertexConsumer consumer, int light, int overlay, String... names) {
		for (String name : names) root.getChild(name).render(pose, consumer, light, overlay);
	}

	public abstract void render(PoseStack pose, VertexConsumer consumer, int light, int overlay, int part, float fireProgress, float reloadProgress, int ammoLeft);
}
