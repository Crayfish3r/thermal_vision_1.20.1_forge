#version 150

uniform sampler2D DiffuseSampler;
uniform sampler2D ThermalMaskSampler;

in vec2 texCoord;

out vec4 fragColor;

vec3 thermalPalette(float heat) {
    vec3 orange = vec3(1.00, 0.20, 0.015);
    vec3 yellow = vec3(1.00, 0.82, 0.08);
    vec3 whiteHot = vec3(1.00, 0.98, 0.78);

    vec3 warm = mix(orange, yellow, smoothstep(0.35, 0.82, heat));
    return mix(warm, whiteHot, smoothstep(0.78, 1.0, heat));
}

void main() {
    vec4 scene = texture(DiffuseSampler, texCoord);
    vec4 thermalMask = texture(ThermalMaskSampler, texCoord);

    float coverage = clamp(thermalMask.a, 0.0, 1.0);
    float heat = clamp(thermalMask.r, 0.0, 1.0);

    vec3 entityColor = thermalPalette(heat);
    vec3 finalColor = mix(scene.rgb, entityColor, coverage);

    fragColor = vec4(finalColor, 1.0);
}
