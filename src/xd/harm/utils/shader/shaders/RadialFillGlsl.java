package xd.harm.utils.shader.shaders;

import xd.harm.utils.shader.IShader;

public class RadialFillGlsl implements IShader {

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
                uniform vec4 activeColor1;
                uniform vec4 activeColor2;
                uniform vec4 activeColor3;
                uniform vec4 activeColor4;
                uniform vec4 borderColor;
                uniform float borderWidth;
                uniform float glowIntensity;
                uniform float glowRadius;
                uniform vec2 clickPoint;
                uniform float progress;
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

                void main() {
                    vec2 uv = gl_TexCoord[0].st;
                    vec2 center = uv * size - size * 0.5;

                    float dist = roundedSDF(center, size * 0.5, round);
                    float bodyAlpha = 1.0 - aaStep(0.0, dist);

                    float innerEdge = dist + borderWidth;
                    float borderMask = (1.0 - aaStep(0.0, dist)) * aaStep(0.0, innerEdge);

                    float glowDist = abs(dist);
                    float safeGlowRadius = max(glowRadius, 0.001);
                    float glow = exp(-glowDist * glowDist / (safeGlowRadius * safeGlowRadius)) * glowIntensity;
                    glow *= (1.0 - aaStep(0.0, dist));

                    vec4 colTop = mix(bgColor1, bgColor4, uv.x);
                    vec4 colBot = mix(bgColor2, bgColor3, uv.x);
                    vec4 baseBg = mix(colTop, colBot, uv.y);

                    vec4 actTop = mix(activeColor1, activeColor4, uv.x);
                    vec4 actBot = mix(activeColor2, activeColor3, uv.x);
                    vec4 activeBg = mix(actTop, actBot, uv.y);

                    vec2 p1 = vec2(0.0, 0.0);
                    vec2 p2 = vec2(size.x, 0.0);
                    vec2 p3 = vec2(0.0, size.y);
                    vec2 p4 = vec2(size.x, size.y);
                    float maxDist = max(max(distance(clickPoint, p1), distance(clickPoint, p2)), max(distance(clickPoint, p3), distance(clickPoint, p4)));

                    float currentRadius = progress * (maxDist + 16.0);
                    vec2 pixel = uv * size;
                    float d = distance(pixel, clickPoint);

                    float rippleMask = 1.0 - smoothstep(currentRadius - 8.0, currentRadius + 8.0, d);

                    vec4 finalBg = baseBg;
                    if (state > 0.01) {
                        finalBg = mix(baseBg, activeBg, rippleMask);
                    } else {
                        finalBg = mix(activeBg, baseBg, rippleMask);
                    }

                    if (progress > 0.01 && progress < 0.99) {
                        float wave = exp(-abs(d - currentRadius) * abs(d - currentRadius) / 16.0);
                        vec4 waveColor = vec4(activeBg.rgb, 1.0) * wave * (1.0 - progress) * 0.25;
                        finalBg.rgb = mix(finalBg.rgb, waveColor.rgb, waveColor.a);
                        finalBg.a = max(finalBg.a, waveColor.a);
                    }

                    vec4 finalColor = vec4(finalBg.rgb, finalBg.a * bodyAlpha);

                    finalColor.rgb = mix(finalColor.rgb, borderColor.rgb, borderMask * borderColor.a);
                    finalColor.a = max(finalColor.a, borderMask * borderColor.a);

                    finalColor.rgb += borderColor.rgb * glow;
                    finalColor.a = max(finalColor.a, glow * borderColor.a);

                    gl_FragColor = finalColor;
                }
                """;
    }

}
