package xd.harm.utils.shader.shaders;

import xd.harm.utils.shader.IShader;

public class FractureGlsl implements IShader {

    @Override
    public String glsl() {
        return """
                #version 120

                uniform vec2 size;
                uniform vec4 round;
                uniform vec4 bgColor1;
                uniform vec4 bgColor2;
                uniform vec4 bgColor3;
                uniform vec4 bgColor4;
                uniform vec4 borderColor;
                uniform float borderWidth;
                uniform float glowIntensity;
                uniform float glowRadius;
                uniform vec2 clickPoint;
                uniform float revealAlpha;
                uniform float crackProgress;
                uniform float state;

                float roundedSDF(vec2 p, vec2 b, vec4 r) {
                    r.xy = (p.x > 0.0) ? r.xy : r.zw;
                    r.x  = (p.y > 0.0) ? r.x  : r.y;
                    vec2 q = abs(p) - b + r.x;
                    return min(max(q.x, q.y), 0.0) + length(max(q, 0.0)) - r.x;
                }

                float aaStep(float edge, float x) {
                    float w = 0.7 * length(vec2(dFdx(x), dFdy(x)));
                    return smoothstep(edge - w, edge + w, x);
                }

                float crackDist(vec2 p, vec2 start, vec2 end, float progress) {
                    vec2 dir = end - start;
                    float len = length(dir);
                    vec2 ndir = dir / max(len, 0.001);
                    float proj = dot(p - start, ndir);
                    float t = clamp(proj / max(len * progress, 0.001), 0.0, 1.0);
                    float wave = sin(proj * 0.25) * 1.2 + cos(proj * 0.45) * 0.7 + sin(proj * 0.9) * 0.3;
                    float thickness = 0.35 * (1.0 - t);
                    vec2 targetPos = start + ndir * proj + vec2(-ndir.y, ndir.x) * wave;
                    float dist = distance(p, targetPos) - thickness;
                    float mask = step(0.0, proj) * step(proj, len * progress);
                    return mix(999.0, dist, mask);
                }

                void main() {
                    vec2 uv = gl_TexCoord[0].st;
                    vec2 center = uv * size - size * 0.5;

                    float dist = roundedSDF(center, size * 0.5, round);

                    float bodyAlpha = 1.0 - aaStep(0.0, dist);

                    vec4 baseTop = mix(bgColor1, bgColor4, uv.x);
                    vec4 baseBot = mix(bgColor2, bgColor3, uv.x);
                    vec4 baseBg = mix(baseTop, baseBot, uv.y);

                    vec4 turquoiseTop = vec4(0.0, 0.95, 1.0, 1.0);
                    vec4 turquoiseBot = vec4(0.0, 0.65, 0.75, 1.0);
                    vec4 normalTurq = mix(turquoiseTop, turquoiseBot, uv.y);
                    vec4 brightTurq = vec4(0.3, 1.0, 1.0, 1.0);
                    vec4 turqBg = mix(normalTurq, brightTurq, revealAlpha * 0.4);

                    vec4 bg = mix(baseBg, turqBg, state);

                    if (state > 0.01 && crackProgress > 0.01) {
                        vec2 pixel = uv * size;

                        vec2 c1 = vec2(0.0, 0.0);
                        vec2 c2 = vec2(size.x, 0.0);
                        vec2 c3 = vec2(0.0, size.y);
                        vec2 c4 = vec2(size.x, size.y);

                        float d1 = crackDist(pixel, clickPoint, c1, crackProgress);
                        float d2 = crackDist(pixel, clickPoint, c2, crackProgress);
                        float d3 = crackDist(pixel, clickPoint, c3, crackProgress);
                        float d4 = crackDist(pixel, clickPoint, c4, crackProgress);

                        float minD = min(min(d1, d2), min(d3, d4));

                        float crackSolid = 1.0 - step(0.0, minD);
                        float crackGlow = exp(-max(0.0, minD) * 1.5);
                        float crackAlpha = clamp((crackSolid + crackGlow * 0.4) * revealAlpha, 0.0, 1.0);

                        bg.rgb = mix(bg.rgb, vec3(1.0, 1.0, 1.0), crackAlpha);
                    }

                    float innerEdge = dist + borderWidth;
                    float borderMask = (1.0 - aaStep(0.0, dist)) * aaStep(0.0, innerEdge);

                    float glowDist = max(0.0, dist);
                    float safeGlowRadius = max(glowRadius, 0.001);
                    float glow = exp(-glowDist * glowDist / (safeGlowRadius * safeGlowRadius)) * glowIntensity;
                    glow *= (1.0 - aaStep(0.0, -dist + borderWidth * 0.5));

                    vec4 finalColor = vec4(bg.rgb, bg.a * bodyAlpha);

                    finalColor.rgb = mix(finalColor.rgb, borderColor.rgb, borderMask * borderColor.a);
                    finalColor.a = max(finalColor.a, borderMask * borderColor.a);

                    finalColor.rgb += borderColor.rgb * glow;
                    finalColor.a = max(finalColor.a, glow * borderColor.a);

                    gl_FragColor = finalColor;
                }
                """;
    }

}
