#version 150

uniform sampler2D Sampler0;
uniform vec4 ColorModulator;

in vec4 vertexColor;
in vec2 texCoord0;
in vec3 modelNormal;

out vec4 fragColor;

void main() {
    vec4 source = texture(Sampler0, texCoord0);
    float coverage = source.a * vertexColor.a * ColorModulator.a;

    if (source.a < 0.05) {
        discard;
    }

    float heat = clamp(vertexColor.r, 0.0, 1.0);
    float facing = abs(normalize(modelNormal).z);
    float shapedHeat = clamp(heat * mix(0.86, 1.0, facing), 0.0, 1.0);

    fragColor = vec4(shapedHeat, 0.0, 0.0, coverage);
}
