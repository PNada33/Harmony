package xd.harm.utils.shader.shaders;

import xd.harm.utils.shader.IShader;

public class HudHoloGlsl implements IShader {
    @Override public String glsl() {
        return """
        #version 120
        uniform sampler2D texture;

        uniform vec4  color;    
        uniform vec2  size;      
        uniform float time;      
        uniform float progress;  

        float edgeMask(vec2 p, vec2 s) {
            vec2 d = min(p, s - p);
            float m = min(min(d.x, d.y), 6.0);
            return smoothstep(0.0, 6.0, m);
        }

        void main() {
            vec2 uv  = gl_TexCoord[0].st;
            vec2 px  = uv * size;
            vec2 cen = size * 0.5;

            float dist = length(px - cen);
            float maxR = length(size * 0.5) * 1.4; 
            float ringR = progress * maxR;

            float thickness = 3.0 + progress * 2.0;

            float innerEdge = smoothstep(ringR - thickness - 2.0, ringR - thickness, dist);
            float outerEdge = 1.0 - smoothstep(ringR + thickness, ringR + thickness + 2.0, dist);
            float ring = innerEdge * outerEdge;

            float pulse = 0.7 + 0.3 * sin(progress * 3.14159);
            ring *= pulse;

            float fade = 1.0 - progress * 0.7;
            ring *= fade;

            float mask = edgeMask(px, size);

            float a = ring * mask;

            a *= smoothstep(0.0, 0.05, progress) * (1.0 - smoothstep(0.9, 1.0, progress));

            gl_FragColor = vec4(color.rgb, color.a * a);
        }
        """;
    }
}