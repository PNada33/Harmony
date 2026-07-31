package xd.harm.utils.shaderharmony.shaders;

import xd.harm.utils.shaderharmony.IShader;

public class FontGlsl implements IShader {


    @Override
    public String glsl() {
        return """
                    #version 120
                           
                            
                uniform sampler2D Sampler;
                uniform vec2 TextureSize;
                uniform float Range;
                uniform float EdgeStrength;
                uniform float Thickness;
                uniform vec4 color;
                uniform bool Outline;
                uniform float OutlineThickness;
                uniform vec4 OutlineColor;
                           
                            
                float median(float red, float green, float blue) {
                  return max(min(red, green), min(max(red, green), blue));
                }
                            
                void main() {
                    vec4 texColor = texture2D(Sampler, gl_TexCoord[0].st);
                            
                    float dx = dFdx(gl_TexCoord[0].x) * TextureSize.x;
                    float dy = dFdy(gl_TexCoord[0].y) * TextureSize.y;
                    float toPixels = Range * inversesqrt(dx * dx + dy * dy);
                            
                    float sigDist = median(texColor.r, texColor.g, texColor.b) - 0.5 + Thickness;
                   

                    float alpha = smoothstep(-EdgeStrength, EdgeStrength, sigDist * toPixels);
                    if (Outline) {
                        float outlineAlpha = smoothstep(-EdgeStrength, EdgeStrength, (sigDist + OutlineThickness) * toPixels) - alpha;
                        float finalAlpha = alpha * color.a + outlineAlpha * color.a;
                     
                        gl_FragColor = vec4(mix(OutlineColor.rgb, color.rgb, alpha), finalAlpha);
                        return;
                    }
                    gl_FragColor = vec4(color.rgb, color.a * alpha);
                }
                    """;
    }
}
