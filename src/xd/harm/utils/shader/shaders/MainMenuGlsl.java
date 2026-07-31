package xd.harm.utils.shader.shaders;

import xd.harm.utils.shader.IShader;

public class MainMenuGlsl implements IShader {
    @Override
    public String glsl() {
        return """
                #extension GL_OES_standard_derivatives : enable
                
                #ifdef GL_ES
                precision highp float;
                #endif
                
                uniform float width;
                uniform float height;
                uniform float time;
                uniform float alpha;

                vec3 palette(float t, vec3 a, vec3 b, vec3 c, vec3 d) {
                    return a + b * cos(6.28318 * (c * t + d));
                }

                float hash21(vec2 p) {
                    p = fract(p * vec2(123.34, 456.21));
                    p += dot(p, p + 45.32);
                    return fract(p.x * p.y);
                }

                float stars(vec2 uv, float t) {
                    vec2 grid = fract(uv * 15.0) - 0.5;
                    float d = length(grid);

                    float twinkle = sin(t * 5.0 + hash21(floor(uv * 15.0)) * 6.28) * 0.5 + 0.5;
                    
                    return smoothstep(0.5, 0.2, d) * twinkle;
                }
                
                void main(void) {

                    vec2 uv = (gl_FragCoord.xy - 0.5 * vec2(width, height)) / min(width, height);

                    vec3 bgColor = palette(
                        length(uv) * 0.3 + time * 0.01,
                        vec3(0.5, 0.5, 0.5),
                        vec3(0.5, 0.5, 0.5),
                        vec3(1.0, 1.0, 1.0),
                        vec3(0.0, 0.33, 0.67)
                    );

                    float glow = 0.03 / length(uv);
                    glow = min(glow, 1.0);

                    float nebula = 0.0;
                    for (int i = 0; i < 3; i++) {
                        float scale = 1.0 + float(i) * 2.0;
                        float speed = 0.005 * (float(i) * 0.5 + 1.0);
                        
                        vec2 offset = vec2(
                            sin(time * speed) * 0.2,
                            cos(time * speed * 0.7) * 0.2
                        );
                        
                        vec2 p = uv * scale + offset;
                        float noise = hash21(floor(p * 10.0) + time * 0.01);
                        
                        nebula += smoothstep(0.5, 0.8, noise) * (0.5 / scale);
                    }

                    float starField = stars(uv, time);

                    float vignette = 1.0 - dot(uv * 0.8, uv * 0.8);
                    vignette = smoothstep(0.0, 1.0, vignette);

                    vec3 finalColor = bgColor * 0.5;
                    finalColor += vec3(glow * 0.3) * palette(time * 0.01, vec3(0.5), vec3(0.5), vec3(1.0), vec3(0.0, 0.33, 0.67));
                    finalColor += vec3(nebula * 0.3) * palette(time * 0.02, vec3(0.5), vec3(0.5), vec3(1.0), vec3(0.0, 0.1, 0.2));
                    finalColor += vec3(starField * 0.7);
                    finalColor *= vignette;

                    gl_FragColor = vec4(finalColor, alpha);
                }
                """;
    }
}