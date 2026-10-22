#version 150

uniform sampler2D DepthSampler;
// clip space -> world space relative to the camera
uniform mat4 InvViewProj;
uniform int LightCount;
// per light: x, y, z relative to the camera, radius, r, g, b, unused
uniform float Lights[128];

in vec2 texCoord;

out vec4 fragColor;

void main() {
    float depth = texture(DepthSampler, texCoord).r;
    if (depth >= 1.0) {
        fragColor = vec4(0.0); // sky
        return;
    }
    vec4 world = InvViewProj * vec4(vec3(texCoord, depth) * 2.0 - 1.0, 1.0);
    vec3 pos = world.xyz / world.w;
    vec3 light = vec3(0.0);
    for (int i = 0; i < LightCount; i++) {
        int o = i * 8;
        float f = max(0.0, 1.0 - distance(pos, vec3(Lights[o], Lights[o + 1], Lights[o + 2])) / Lights[o + 3]);
        light += vec3(Lights[o + 4], Lights[o + 5], Lights[o + 6]) * f * f;
    }
    fragColor = vec4(light, 1.0);
}
