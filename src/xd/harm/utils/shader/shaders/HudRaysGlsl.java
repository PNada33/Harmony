package xd.harm.utils.shader.shaders;

import xd.harm.utils.shader.IShader;

public class HudRaysGlsl implements IShader {
    @Override public String glsl() {
        return """
        #version 120
        uniform sampler2D texture;

        uniform vec4  color;
        uniform vec2  size;
        uniform float time;
        uniform float progress; 

        const float PI = 3.14159265;

        void main() {
            vec2 uv  = gl_TexCoord[0].st;
            vec2 px  = uv * size;
            vec2 c   = size * 0.5;

            vec2 dp  = px - c;
            float ang = atan(dp.y, dp.x);
            float r   = length(dp);
            float maxR = length(size * 0.5);

            float rays = 0.0;
            for (int i = 0; i < 6; i++) {
                float a0 = (float(i) / 6.0) * 2.0 * PI + time * 0.5;
                float d = abs(sin((ang - a0) * 3.0));
                float ray = exp(-d * d * 2.0) * smoothstep(0.0, maxR * 0.8, r);
                rays += ray * 0.3;
            }
            rays = clamp(rays, 0.0, 0.8);

            float centerFade = smoothstep(0.0, maxR * 0.3, r);
            rays *= centerFade;

            float a = rays;
            a *= smoothstep(0.0, 0.1, progress);
            a *= 1.0 - smoothstep(0.8, 1.0, progress);

            gl_FragColor = vec4(color.rgb, color.a * a * 0.3);
        }
        """;
    }
}