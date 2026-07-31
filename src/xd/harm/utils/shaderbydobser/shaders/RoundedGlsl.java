package xd.harm.utils.shaderbydobser.shaders;

import xd.harm.utils.shaderbydobser.IShader;

public class RoundedGlsl implements IShader {

    @Override
    public String glsl() {
        return """
                #version 120

                uniform vec2 size;
                uniform vec4 round;
                uniform vec2 smoothness;
                uniform float value;
                uniform vec4 color1;
                uniform vec4 color2;
                uniform vec4 color3;
                uniform vec4 color4;
                #define NOISE .5/255.0

                float test(vec2 vec_1, vec2 vec_2, vec4 vec_4) {
                    vec_4.xy = (vec_1.x > 0.0) ? vec_4.xy : vec_4.zw;
                    vec_4.x = (vec_1.y > 0.0) ? vec_4.x : vec_4.y;
                    vec2 coords = abs(vec_1) - vec_2 + vec_4.x;
                    return min(max(coords.x, coords.y), 0.0) + length(max(coords, vec2(0.0f))) - vec_4.x;
                }

                vec4 createGradient(vec2 coords, vec4 color1, vec4 color2, vec4 color3, vec4 color4) {
                    vec4 color = mix(mix(color1, color2, coords.y), mix(color3, color4, coords.y), coords.x);


                    color += mix(NOISE, -NOISE, fract(sin(dot(coords.xy, vec2(12.9898, 78.233))) * 43758.5453));
                    return color;
                }

                void main() {
                    vec4 color = createGradient(gl_TexCoord[0].st, color1,color2,color3,color4);
                    vec2 st = gl_TexCoord[0].st * size;
                    vec2 halfSize = 0.5 * size;
                    float sa = 1.0 - smoothstep(smoothness.x, smoothness.y, test(halfSize - st, halfSize - value, round));

                
                    gl_FragColor = mix(vec4(color.rgb, 0.0), vec4(color.rgb, color.a), sa);
                }
                """;
    }
}
