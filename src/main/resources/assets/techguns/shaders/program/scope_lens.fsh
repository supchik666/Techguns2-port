#version 150

uniform sampler2D DiffuseSampler;
uniform vec2 OutSize;
// the scope's window: half width and half height in half screen heights, then the shape exponent (2 round, higher squarer)
uniform vec3 Window;
// 0 sharp, 1 fully blurred (moving or just fired)
uniform float Blur;

in vec2 texCoord;

out vec4 fragColor;

const float DISTORTION = 0.2;
const float ABERRATION = 0.022;
const float VIGNETTE = 0.65;
// blur radius at Blur = 1, in half screen heights
const float BLUR_RADIUS = 0.035;
const int TAPS = 16;

// p in half screen heights from the center -> texture coordinate
vec2 uv(vec2 p) {
    return p / vec2(OutSize.x / OutSize.y, 1.0) * 0.5 + 0.5;
}

vec3 split(vec2 bent, vec2 fringe) {
    return vec3(texture(DiffuseSampler, uv(bent + fringe)).r, texture(DiffuseSampler, uv(bent)).g, texture(DiffuseSampler, uv(bent - fringe)).b);
}

void main() {
    vec2 p = (texCoord - 0.5) * 2.0 * vec2(OutSize.x / OutSize.y, 1.0);
    vec2 q = abs(p) / Window.xy;
    // 0 at the center, 1 on the window's edge
    float r = min(pow(pow(q.x, Window.z) + pow(q.y, Window.z), 1.0 / Window.z), 1.2);
    float edge = r * r * r;
    // the glass bends the view toward the rim, splitting colors there
    vec2 bent = p * (1.0 + DISTORTION * edge);
    vec2 fringe = p * ABERRATION * edge * r;
    vec3 color;
    if (Blur <= 0.001) {
        color = split(bent, fringe);
    } else {
        // a disk of taps on a golden-angle spiral
        color = vec3(0.0);
        for (int i = 0; i < TAPS; i++) {
            float t = (float(i) + 0.5) / float(TAPS);
            float a = float(i) * 2.39996;
            color += split(bent + vec2(cos(a), sin(a)) * sqrt(t) * BLUR_RADIUS * Blur, fringe);
        }
        color /= float(TAPS);
    }
    color *= 1.0 - VIGNETTE * smoothstep(0.45, 1.0, r);
    fragColor = vec4(color, 1.0);
}
