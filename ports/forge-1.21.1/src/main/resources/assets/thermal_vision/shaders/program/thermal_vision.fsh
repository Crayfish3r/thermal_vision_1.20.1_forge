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

    float scanline = sin((texCoord.y * OutSize.y + Time * 90.0) * 3.14159265);
    float scanlineMask = 0.92 + 0.08 * smoothstep(-0.25, 1.0, scanline);

    vec2 rounded = texCoord * 2.0 - 1.0;
    rounded.x *= OutSize.x / max(OutSize.y, 1.0);
    float roundedDistance = length(rounded);
    float vignette = smoothstep(0.72, 1.38, roundedDistance);

    float grain = rand(texCoord * OutSize + Time * 27.0) - 0.5;
    vec3 thermalColor = (thermalCold + edgeColor) * scanlineMask;
    thermalColor += grain * 0.018;
    thermalColor *= 1.0 - vignette * 0.62;
    thermalColor = clamp(thermalColor, 0.0, 1.0);

    float effectFade = clamp(Fade, 0.0, 1.0);
    vec3 finalColor = mix(source.rgb, thermalColor, effectFade);

    fragColor = vec4(finalColor, 1.0);
}
