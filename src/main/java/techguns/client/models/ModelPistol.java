package techguns.client.models;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

public class ModelPistol extends ModelMultipart {
	public ModelPistol() {
		box("top", 0, 17, 3, 1, 14, -2F, 2F, -2F, 0F, 0F, 0F);
		box("grip3", 3, 0, 2, 7, 3, -1.5F, 3F, 7F, 0.2181662F, 0F, 0F);
		box("barrel", 0, 21, 1, 1, 6, -0.5F, 0.25F, -3F, 0F, 0F, 0.7853982F);
		box("is2", 0, 21, 1, 1, 1, -1F, -0.5333334F, -0.5F, 0F, 0F, 0F);
		box("grip2", 20, 5, 1, 1, 3, -1F, 5F, 5F, 0F, 0F, 0F);
		box("is1", 0, 24, 1, 1, 1, -1F, -0.5F, 9.5F, 0F, 0F, 0F);
		box("trigger", 25, 0, 1, 2, 1, -1F, 2.5F, 6.5F, -0.4089647F, 0F, 0F);
		box("grip1", 20, 0, 1, 3, 1, -1F, 3F, 4F, 0F, 0F, 0F);
		box("slide", 0, 0, 3, 2, 14, -2F, 0F, -2F, 0F, 0F, 0F);
		bake(64, 32);
	}

	@Override
	public void render(PoseStack pose, VertexConsumer consumer, int light, int overlay, int part, float fireProgress, float reloadProgress, int ammoLeft) {
		if (part == 0) {
			render(pose, consumer, light, overlay, "top", "grip3", "barrel", "grip2", "trigger", "grip1");
		} else {
			pose.pushPose();
			float progress = fireProgress * 2.0f;
			if (progress < 0.5f) pose.translate(0, 0, 0.15f * progress * 2.0f); // slide kicks back
			render(pose, consumer, light, overlay, "is1", "is2", "slide");
			pose.popPose();
		}
	}
}
