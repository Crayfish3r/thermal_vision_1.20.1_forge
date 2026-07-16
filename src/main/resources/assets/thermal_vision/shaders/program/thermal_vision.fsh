#version 150

uniform sampler2D DiffuseSampler;
uniform vec2 InSize;
uniform vec2 OutSize;
uniform float Time;
uniform float Fade;

in vec2 texCoord;

out vec4 fragColor;

float luminance(vec3 color) {
    return dot(color, vec3(0.299, 0.587, 0.114));
}

float rand(vec2 value) {
    return fract(sin(dot(value, vec2(12.9898, 78.233))) * 43758.5453);
}

float warmThermalMask(vec3 color, float lum) {
    float redDominance = color.r - max(color.b + 0.18, color.g * 0.55);
    float yellowHeat = min(color.r, color.g) - color.b * 1.55;
    float saturation = max(color.r, max(color.g, color.b)) - min(color.r, min(color.g, color.b));

    float orangeMask = smoothstep(0.10, 0.36, redDominance);
    float yellowMask = smoothstep(0.18, 0.52, yellowHeat);
    float brightMask = smoothstep(0.30, 0.58, lum);
    float saturatedMask = smoothstep(0.12, 0.42, saturation);

    return clamp(max(orangeMask, yellowMask) * brightMask * saturatedMask, 0.0, 1.0);
}

void main() {
    vec2 pixel = 1.0 / max(InSize, vec2(1.0));
    vec4 source = texture(DiffuseSampler, texCoord);

    float centerLum = luminance(source.rgb);
    float leftLum = luminance(texture(DiffuseSampler, texCoord + vec2(-pixel.x, 0.0)).rgb);
    float rightLum = luminance(texture(DiffuseSampler, texCoord + vec2(pixel.x, 0.0)).rgb);
    float upLum = luminance(texture(DiffuseSampler, texCoord + vec2(0.0, -pixel.y)).rgb);
    float downLum = luminance(texture(DiffuseSampler, texCoord + vec2(0.0, pixel.y)).rgb);

    float horizontalEdge = abs(leftLum - rightLum);
    float verticalEdge = abs(upLum - downLum);
    float edge = smoothstep(0.035, 0.20, max(horizontalEdge, verticalEdge));

    float compressedLum = pow(clamp(centerLum, 0.0, 1.0), 0.72);
    compressedLum = floor(compressedLum * 18.0) / 18.0;

    vec3 coldBlack = vec3(0.002, 0.008, 0.030);
    vec3 coldBlue = vec3(0.018, 0.100, 0.310);
    vec3 coldBright = vec3(0.050, 0.260, 0.620);
    vec3 thermalCold = mix(coldBlack, coldBlue, compressedLum);
    thermalCold = mix(thermalCold, coldBright, smoothstep(0.72, 1.0, compressedLum) * 0.30);

    vec3 edgeColor = vec3(0.14, 0.62, 1.0) * edge * 0.50;

    float heatMask = warmThermalMask(source.rgb, centerLum);
    float heatBoost = smoothstep(0.25, 0.95, centerLum);
    vec3 heatOrange = vec3(1.00, 0.28, 0.015);
    vec3 heatYellow = vec3(1.00, 0.88, 0.10);
    vec3 thermalHeat = mix(heatOrange, heatYellow, heatBoost);
    thermalHeat += vec3(0.62, 0.18, 0.015) * heatMask;

    float scanline = sin((texCoord.y * OutSize.y + Time * 90.0) * 3.14159265);
    float scanlineMask = 0.92 + 0.08 * smoothstep(-0.25, 1.0, scanline);

    vec2 rounded = texCoord * 2.0 - 1.0;
    rounded.x *= OutSize.x / max(OutSize.y, 1.0);
    float roundedDistance = length(rounded);
    float vignette = smoothstep(0.72, 1.38, roundedDistance);

    float grain = rand(texCoord * OutSize + Time * 27.0) - 0.5;
    vec3 coldColor = (thermalCold + edgeColor) * scanlineMask;
    coldColor += grain * 0.018;
    coldColor *= 1.0 - vignette * 0.62;

    float heatCut = smoothstep(0.08, 0.44, heatMask);
    vec3 thermalColor = mix(coldColor, thermalHeat, heatCut);
    thermalColor = clamp(thermalColor, 0.0, 1.0);

    float effectFade = clamp(Fade, 0.0, 1.0);
    vec3 finalColor = mix(source.rgb, thermalColor, effectFade);

    fragColor = vec4(finalColor, source.a);
}
