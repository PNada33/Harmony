package xd.harm.utils.shaderharmony.shaders;

import xd.harm.utils.shaderharmony.IShader;

public class RoundedTextureGlsl implements IShader {
    @Override
    public String glsl() {
        return """
           #version 120

                uniform vec2 size;
                uniform vec4 round;
                uniform vec2 smoothness;
                uniform float value;
                uniform sampler2D textureIn;
                uniform float alpha;

                float test(vec2 vec_1, vec2 vec_2, vec4 vec_4) {
                    vec_4.xy = (vec_1.x > 0.0) ? vec_4.xy : vec_4.zw;
                    vec_4.x = (vec_1.y > 0.0) ? vec_4.x : vec_4.y;
                    vec2 coords = abs(vec_1) - vec_2 + vec_4.x;
                    return min(max(coords.x, coords.y), 0.0) + length(max(coords, vec2(0.0f))) - vec_4.x;
                }

                void main() {
                    vec4 color = texture2D(textureIn, gl_TexCoord[0].st);
                    vec2 st = gl_TexCoord[0].st * size;
                    vec2 halfSize = 0.5 * size;
                    float sa = 1.0 - smoothstep(smoothness.x, smoothness.y, test(halfSize - st, halfSize - value, round));

                    gl_FragColor = mix(vec4(color.rgb, 0.0), vec4(color.rgb, alpha), sa);
                }""";
    }
}
