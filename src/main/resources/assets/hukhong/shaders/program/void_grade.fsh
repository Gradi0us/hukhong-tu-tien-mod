#version 150

uniform sampler2D DiffuseSampler;

in vec2 texCoord;

uniform float Strength;
uniform float TintR;
uniform float TintG;
uniform float TintB;
uniform float Pulse;

out vec4 fragColor;

// Gentle cinematic grade: desaturate, shadow-weighted tint, soft vignette. Strength 0 = untouched.
void main() {
    vec3 TintColor = vec3(TintR, TintG, TintB);
    vec3 c = texture(DiffuseSampler, texCoord).rgb;
    float luma = dot(c, vec3(0.299, 0.587, 0.114));
    vec3 g = mix(c, vec3(luma), 0.38 * Strength);
    float shadow = 1.0 - smoothstep(0.0, 0.75, luma);
    g = mix(g, g * (TintColor * 1.25), (0.28 + 0.30 * shadow) * Strength);
    g += TintColor * 0.045 * shadow * Strength;
    vec2 q = texCoord - 0.5;
    float v = smoothstep(0.30, 0.95, length(q * vec2(1.0, 0.85)));
    g *= 1.0 - v * (0.50 + 0.15 * Pulse) * Strength;
    g += vec3(0.30, 0.10, 0.45) * Pulse * 0.10 * Strength;
    fragColor = vec4(mix(c, g, clamp(Strength * 1.4, 0.0, 1.0)), 1.0);
}
