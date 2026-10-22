package techguns.client.particle;

import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Camera;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.phys.Vec3;

/** A segment of a ribbon: a quad from the previous particle of its system to this one, facing the camera. Drawn in spawn order, unsorted. */
public class TGParticleStreak extends TGParticle {
	TGParticleStreak prev, next;
	/** This end's two vertices (camera-relative) this frame, shared with the next segment so the ribbon has no gaps. */
	private Vec3 edge1, edge2;

	TGParticleStreak(TGParticleSystem system, Vec3 pos, Vec3 motion) {
		super(system, pos, motion);
	}

	@Override
	void render(MultiBufferSource buffers, Camera camera, float partialTick) {
		prepare(partialTick);
		if (next == null) alpha = 0; // the newest end fades in
		edge1 = edge2 = null;
		// 1.12 kept drawing to a dead previous segment, which had faded out by then; dropping it also frees the chain
		if (prev != null && prev.dead) prev = null;
		if (prev == null) return;
		float[] uv = uv(partialTick);
		Vec3 cam = camera.getPosition();
		Vec3 here = prevPos.lerp(pos, partialTick).subtract(cam);
		Vec3 there = prev.prevPos.lerp(prev.pos, partialTick).subtract(cam);
		Vec3 side = new Vec3(camera.getLookVector()).cross(there.subtract(here).normalize()).normalize();
		edge1 = here.add(side.scale(0.1f * renderSize));
		edge2 = here.subtract(side.scale(0.1f * renderSize));
		if (prev.edge1 == null) {
			prev.edge1 = there.add(side.scale(0.1f * prev.renderSize));
			prev.edge2 = there.subtract(side.scale(0.1f * prev.renderSize));
		}
		VertexConsumer consumer = buffers.getBuffer(renderType());
		vertex(consumer, edge1.x, edge1.y, edge1.z, uv[2], uv[3], r, g, b, alpha);
		vertex(consumer, edge2.x, edge2.y, edge2.z, uv[2], uv[1], r, g, b, alpha);
		vertex(consumer, prev.edge2.x, prev.edge2.y, prev.edge2.z, uv[0], uv[1], prev.r, prev.g, prev.b, prev.alpha);
		vertex(consumer, prev.edge1.x, prev.edge1.y, prev.edge1.z, uv[0], uv[3], prev.r, prev.g, prev.b, prev.alpha);
	}
}
