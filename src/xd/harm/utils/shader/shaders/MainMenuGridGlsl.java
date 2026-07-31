package xd.harm.utils.shader.shaders;

import xd.harm.utils.shader.IShader;

public class MainMenuGridGlsl implements IShader {
    @Override
    public String glsl() {
        return """
                #version 120

                uniform vec2 size;
                uniform float radius;
                uniform float gridStep;
                uniform float lineWidth;
                uniform float time;
                uniform float alpha;
                uniform vec4 gridColor;
                uniform vec4 bgColor;

                float roundedSDF(vec2 p, vec2 b, float r) {
                    vec2 q = abs(p) - b + r;
                    return min(max(q.x, q.y), 0.0) + length(max(q, 0.0)) - r;
                }

                void main() {
                    vec2 uv = gl_TexCoord[0].st;
                    vec2 p = uv * size;
                    vec2 centered = p - size * 0.5;

                    // Rounded rectangle mask
                    float rect = 1.0 - smoothstep(-0.5, 1.0, roundedSDF(centered, size * 0.5 - vec2(0.5), radius));

                    // Smooth radial fade from center — strongest in center, fades to edges
                    vec2 normPos = centered / (size * 0.5);
                    float radialDist = length(normPos);
                    float radialFade = 1.0 - smoothstep(0.1, 0.85, radialDist);

                    // Directional edge fade — stronger fade on edges
                    float edgeX = smoothstep(0.0, 0.35, uv.x) * (1.0 - smoothstep(0.65, 1.0, uv.x));
                    float edgeY = smoothstep(0.0, 0.35, uv.y) * (1.0 - smoothstep(0.65, 1.0, uv.y));
                    float edgeFade = edgeX * edgeY;

                    // Grid lines
                    vec2 gridUv = p / gridStep;
                    vec2 cell = abs(fract(gridUv - 0.5) - 0.5);
                    float lineDist = min(cell.x, cell.y) * gridStep;
                    float grid = 1.0 - smoothstep(lineWidth * 0.5, lineWidth * 0.5 + 0.8, lineDist);

                    // Center glow — subtle brightness boost
                    float centerGlow = exp(-radialDist * radialDist * 2.2);

                    // Combined mask — no animation
                    float mask = rect * radialFade * edgeFade;

                    // Grid fades with distance from center
                    float gridAlpha = grid * mask * (0.45 + centerGlow * 0.55);

                    // Minimal background
                    float bgAlpha = mask * (0.08 + centerGlow * 0.14);

                    // Color mixing
                    vec3 mono = mix(bgColor.rgb, gridColor.rgb, grid * (0.5 + centerGlow * 0.5));
                    float outAlpha = alpha * max(bgColor.a * bgAlpha, gridColor.a * gridAlpha);

                    gl_FragColor = vec4(mono, outAlpha);
                }
                """;
    }
}
