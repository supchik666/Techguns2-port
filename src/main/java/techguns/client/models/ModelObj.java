package techguns.client.models;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemDisplayContext;

import org.joml.Vector3f;

/**
 * A Wavefront OBJ drawn with the 1.12 model signature (1.12 ModelBaseBaked around Forge's OBJ loader, "flip-v" by default).
 * Reads only v/vt/f; triangles become quads with a repeated corner. Loaded on first render.
 * ponytail: one texture, face normals, no groups; switch to NeoForge's OBJ loader if a model needs more.
 */
public class ModelObj extends ModelMultipart {
	private final ResourceLocation location;
	private final boolean flipV;
	/** Per vertex: x, y, z, u, v, nx, ny, nz; four vertices per quad. */
	private float[] quads;

	public ModelObj(ResourceLocation location) {
		this(location, true);
	}

	/** {@code flipV}: the 1.12 blockstate's "flip-v" custom data. */
	public ModelObj(ResourceLocation location, boolean flipV) {
		this.location = location;
		this.flipV = flipV;
	}

	@Override
	public void render(Entity entityIn, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch,
			float scale, int ammoLeft, float reloadProgress, ItemDisplayContext transformType, int part, float fireProgress, float chargeProgress) {
		if (quads == null) quads = load(location, flipV);
		PoseStack.Pose last = LegacyGl.pose.last();
		for (int i = 0; i < quads.length; i += 8) {
			LegacyGl.consumer.addVertex(last, quads[i], quads[i + 1], quads[i + 2]).setColor(-1).setUv(quads[i + 3], quads[i + 4])
					.setOverlay(LegacyGl.overlay).setLight(LegacyGl.light).setNormal(last, quads[i + 5], quads[i + 6], quads[i + 7]);
		}
	}

	static float[] load(ResourceLocation location, boolean flipV) {
		List<float[]> pos = new ArrayList<>(), uv = new ArrayList<>();
		List<Float> out = new ArrayList<>();
		try (BufferedReader reader = Minecraft.getInstance().getResourceManager().openAsReader(location)) {
			for (String line; (line = reader.readLine()) != null;) {
				String[] t = line.trim().split("\\s+");
				switch (t[0]) {
					case "v" -> pos.add(new float[] {Float.parseFloat(t[1]), Float.parseFloat(t[2]), Float.parseFloat(t[3])});
					case "vt" -> uv.add(new float[] {Float.parseFloat(t[1]), flipV ? 1.0f - Float.parseFloat(t[2]) : Float.parseFloat(t[2])});
					case "f" -> {
						int[][] corners = new int[4][];
						for (int c = 0; c < 4; c++) {
							String[] idx = t[Math.min(c + 1, t.length - 1)].split("/");
							corners[c] = new int[] {Integer.parseInt(idx[0]) - 1, Integer.parseInt(idx[1]) - 1};
						}
						float[] a = pos.get(corners[0][0]), b = pos.get(corners[1][0]), c = pos.get(corners[2][0]);
						Vector3f n = new Vector3f(b[0] - a[0], b[1] - a[1], b[2] - a[2])
								.cross(c[0] - a[0], c[1] - a[1], c[2] - a[2]).normalize();
						for (int[] corner : corners) {
							float[] p = pos.get(corner[0]), tex = uv.get(corner[1]);
							for (float f : new float[] {p[0], p[1], p[2], tex[0], tex[1], n.x, n.y, n.z}) out.add(f);
						}
					}
					default -> {}
				}
			}
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
		float[] quads = new float[out.size()];
		for (int i = 0; i < quads.length; i++) quads[i] = out.get(i);
		return quads;
	}
}
