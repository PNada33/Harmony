package xd.harm.utils.shader.shaders;

import xd.harm.utils.shader.IShader;

public class RoundGlsl implements IShader {
    @Override
    public String glsl() {
        return """
            #version 120

            uniform float round;
            uniform vec2 size;
            uniform vec4 color;
            uniform float startAngle;
            uniform float endAngle;
            uniform float strokeWidth;
            uniform float isOutline;

            float alpha(vec2 d, vec2 d1, float radius) {
                vec2 v = abs(d) - d1 + radius;
                return min(max(v.x, v.y), 0.0) + length(max(v, 0.0)) - radius;
            }

            void main() {
                vec2 centre = 0.5 * size;
                vec2 coord = gl_TexCoord[0].st * size;
                vec2 d = centre - coord;

                float dist = alpha(d, centre - 1.0, round);

                float fillAlpha = 1.0 - smoothstep(0.0, 1.5, dist);
                float strokeAlpha = 0.0;
                if (isOutline > 0.5) {
                    strokeAlpha = 1.0 - smoothstep(0.0, 1.5, abs(dist + strokeWidth / 2.0) - strokeWidth / 2.0);
                    fillAlpha = strokeAlpha;
                }

                float angle = atan(d.y, d.x);
                if (angle < 0.0) angle += 6.28318530718;
                float inAngle = step(startAngle, angle) * step(angle, endAngle);
                if (startAngle > endAngle) {
                    inAngle = step(startAngle, angle) + step(0.0, angle) * step(angle, endAngle);
                }

                float finalAlpha = fillAlpha * inAngle * color.a;
                gl_FragColor = vec4(color.rgb, finalAlpha);
            }
            """;
    }
}