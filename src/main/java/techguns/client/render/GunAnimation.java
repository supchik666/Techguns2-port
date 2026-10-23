package techguns.client.render;

import java.util.ArrayList;
import java.util.List;

import org.joml.Quaternionf;
import org.joml.Vector3f;

import com.mojang.blaze3d.vertex.PoseStack;

/** Keyframed gun movement, a straight port of the 1.12 GunAnimation with PoseStack in place of GL calls. */
public class GunAnimation {
	interface Curve {
		float f(float x);
	}

	static final Curve LINEAR = x -> x;
	static final Curve FAST = x -> 1.0f - (1.0f - x) * (1.0f - x);
	static final Curve SMOOTH = x -> {
		double d = Math.sin(Math.PI * x * 0.5);
		return (float) (d * d);
	};
	static final Curve SINUS = x -> (float) Math.sin(Math.PI * x * 2.0);

	public static final GunAnimation genericRecoil = new GunAnimation().addTranslate(0, 0, 1.0f).addRotate(1.0f, 1.0f, 0, 0)
			.addSegment(0, 0.25f, FAST, 0f, 1f).addSegment(0.25f, 1f, SMOOTH, 1f, 0f);
	public static final GunAnimation swayRecoil = new GunAnimation().addTranslate(1.0f, 0.75f, 0.50f).addRotate(1.0f, 1.0f, -0.75f, 0.25f)
			.addSegment(0, 1.0f, SINUS, 0.0f, 1.0f);
	public static final GunAnimation pulseRifleRecoil = new GunAnimation().addTranslate(0, 0, 1.0f).addRotate(1.0f, 1.0f, 0, 0)
			.addSegment(0, 0.1f, FAST, 0f, 0.4f).addSegment(0.1f, 0.2f, SMOOTH, 0.4f, 0.3f).addSegment(0.2f, 0.40f, FAST, 0.3f, 1f)
			.addSegment(0.40f, 1f, SMOOTH, 1f, 0f);
	public static final GunAnimation breechReload = new GunAnimation().addTranslate(0, 1.0f, -1.0f).addRotate(1.0f, -1.0f, 0f, 0f)
			.addSegment(0, 0.25f, SMOOTH, 0f, 1f).addSegment(0.25f, 0.5f, LINEAR, 1f, 1f).addSegment(0.5f, 0.85f, SMOOTH, 1.0f, -0.25f)
			.addSegment(0.85f, 1f, SMOOTH, -0.25f, 0f);
	public static final GunAnimation genericReload = new GunAnimation().addTranslate(-0.15f, -0.05f, 0.15f).addRotate(1.0f, -0.5f, 1.0f, -0.25f)
			.addSegment(0, 0.2f, SMOOTH, 0f, 1f).addSegment(0.2f, 0.8f, LINEAR, 1.0f, 1.0f).addSegment(0.8f, 1.0f, SMOOTH, 1.0f, 0.0f);

	private record Segment(float start, float end, Curve curve, float val1, float val2) {}

	/** A translate (angle == null) or a rotation of {@code angle} degrees about x/y/z, scaled by the animation value. */
	private record Transformation(Float angle, float x, float y, float z) {}

	private final List<Segment> segments = new ArrayList<>();
	private final List<Transformation> transformations = new ArrayList<>();

	public GunAnimation addSegment(float start, float end, Curve curve, float val1, float val2) {
		segments.add(new Segment(start, end, curve, val1, val2));
		return this;
	}

	public GunAnimation addTranslate(float x, float y, float z) {
		transformations.add(new Transformation(null, x, y, z));
		return this;
	}

	public GunAnimation addRotate(float angle, float x, float y, float z) {
		transformations.add(new Transformation(angle, x, y, z));
		return this;
	}

	/** Each magnitude scales the transformation at the same index; mirror flips it for the left hand. */
	public void play(PoseStack pose, float progress, boolean mirror, float... magnitudes) {
		float prev = 0.0f;
		float value = 0.0f;
		for (Segment s : segments) {
			if (progress > s.start && progress <= s.end) {
				value += s.val1 + s.curve.f((progress - prev) / (s.end - s.start)) * (s.val2 - s.val1);
			}
			prev = s.end;
		}
		for (int i = 0; i < transformations.size(); i++) {
			Transformation t = transformations.get(i);
			float v = i < magnitudes.length ? value * magnitudes[i] : value;
			if (t.angle == null) {
				pose.translate(mirror ? -t.x * v : t.x * v, t.y * v, t.z * v);
			} else {
				// glRotate normalizes the axis; JOML's rotationAxis expects a unit vector
				Vector3f axis = new Vector3f(t.x, mirror ? -t.y : t.y, mirror ? -t.z : t.z).normalize();
				pose.mulPose(new Quaternionf().rotationAxis(t.angle * v * ((float) Math.PI / 180f), axis));
			}
		}
	}
}
