package xd.harm.utils.shader.shaders;

import xd.harm.utils.shader.IShader;

public class RoundedGlsl implements IShader {

    @Override
    public String glsl() {
        return """
                #version 120

                uniform vec2 size;
                uniform vec4 round;
                uniform vec2 smoothness;
                uniform vec4 color1;
                uniform vec4 color2;
                uniform vec4 color3;
                uniform vec4 color4;

                float roundedRectDistance(vec2 point, vec2 size, vec4 radius) {

                    radius.xy = (point.x > 0.0) ? radius.xy : radius.zw;
                    radius.x = (point.y > 0.0) ? radius.x : radius.y;

                    vec2 q = abs(point) - size + radius.x;
                    return min(max(q.x, q.y), 0.0) + length(max(q, 0.0)) - radius.x;
                }

                float aaStep(float edge, float x) {
                    float width = 0.7 * length(vec2(dFdx(x), dFdy(x)));
                    return smoothstep(edge - width, edge + width, x);
                }

                void main() {

                    vec2 texCoord = gl_TexCoord[0].st;

                    vec2 center = texCoord * size - size * 0.5;

                    float dist = roundedRectDistance(center, size * 0.5, round);

                    float alpha = 1.0 - aaStep(0.0, dist);

                    vec4 colorTop = mix(color1, color4, texCoord.x);
                    vec4 colorBottom = mix(color2, color3, texCoord.x);
                    vec4 finalColor = mix(colorTop, colorBottom, texCoord.y);

                    gl_FragColor = vec4(finalColor.rgb, finalColor.a * alpha);
                }
                """;
    }
}