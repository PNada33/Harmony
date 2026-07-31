package xd.harm.utils.shader.shaders;

import xd.harm.utils.shader.IShader;

public class ExitShaderGlsl implements IShader {
    @Override
    public String glsl() {
        return """
                #version 120
                
                uniform vec2 resolution;
                uniform float progress;
                uniform sampler2D texture;
                

                float hash(vec2 p) {
                    return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453);
                }
                

                float voronoi(vec2 x) {
                    vec2 p = floor(x);
                    vec2 f = fract(x);
                    
                    float res = 8.0;
                    for(int j = -1; j <= 1; j++) {
                        for(int i = -1; i <= 1; i++) {
                            vec2 b = vec2(i, j);
                            vec2 r = b - f + hash(p + b);
                            float d = dot(r, r);
                            res = min(res, d);
                        }
                    }
                    return sqrt(res);
                }
                

                float easeInOutQuad(float t) {
                    return t < 0.5 ? 2.0 * t * t : 1.0 - pow(-2.0 * t + 2.0, 2.0) * 0.5;
                }
                
                void main() {
                    vec2 uv = gl_TexCoord[0].st;
                    vec2 p = uv * resolution / min(resolution.x, resolution.y);
                    
                    vec4 texColor = texture2D(texture, uv);
                    
                    
                    float prog = easeInOutQuad(progress);
                    
                    
                    float noise1 = voronoi(p * 8.0 + prog * 2.0);
                    float dissolve1 = smoothstep(prog - 0.1, prog + 0.1, noise1);
                    
                  
                    float noise2 = hash(floor(p * 40.0 + prog * 5.0));
                    float dissolve2 = smoothstep(prog - 0.05, prog + 0.05, noise2);
                    
                  
                    float noise3 = hash(floor(p * 3.0 + prog));
                    float dissolve3 = smoothstep(prog - 0.2, prog + 0.1, noise3);
                    
                    
                    float finalDissolve = dissolve1 * dissolve2 * dissolve3;
                    
                   
                    float edge = smoothstep(0.0, 0.1, finalDissolve) * (1.0 - smoothstep(0.9, 1.0, finalDissolve));
                    vec3 glowColor = vec3(0.3, 0.6, 1.0); 
                    

                    float vignette = 1.0 - prog * 0.7;
                    float distFromCenter = length(uv - 0.5);
                    vignette *= 1.0 - smoothstep(0.3, 1.2, distFromCenter + prog * 0.5);
                    
                 
                    vec3 finalColor = texColor.rgb * vignette;
                    finalColor += glowColor * edge * 0.5 * (1.0 - prog);
                    
                 
                    float finalAlpha = texColor.a * finalDissolve * (1.0 - prog * 0.3);
                    
                    gl_FragColor = vec4(finalColor, finalAlpha);
                }
                """;
    }
}