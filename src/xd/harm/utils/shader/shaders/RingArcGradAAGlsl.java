package xd.harm.utils.shader.shaders;

import xd.harm.utils.shader.IShader;

public class RingArcGradAAGlsl implements IShader {
    @Override
    public String glsl() {
        return """
                #version 120
                uniform sampler2D texture;
                
                        uniform vec4  colorA;      
                        uniform vec4  colorB;     
                        uniform vec2  size;        
                        uniform vec2  center;      
                        uniform float radius;      
                        uniform float thickness;   
                        uniform float feather;    
                        uniform float startAngle;  
                        uniform float endAngle;    
                        uniform float gradExp;     
                
                        const float PI2 = 6.28318530718;
                
                        float mod2pi(float a) {
                            return a - floor(a / PI2) * PI2;
                        }
                
                        float sdRing(vec2 p, float R, float t) {
                            float halfT = max(0.0001, t * 0.5);
                            return abs(length(p) - (R - halfT)) - halfT;
                        }
                
                        void main() {
                            vec2 uv    = gl_TexCoord[0].st;
                            vec2 pixel = uv * size;
                            vec2 p     = pixel - center;
                
                            float a0   = mod2pi(startAngle);
                            float a1   = mod2pi(endAngle);
                            float aLen = a1 - a0; if (aLen < 0.0) aLen += PI2;
                
                            float ang  = atan(p.y, p.x);
                            float angN = mod2pi(ang - a0);
                
                            float inside = step(0.0, angN) * step(angN, aLen);
                            float featherAng = max(feather, 0.5) / max(radius, 1.0);
                            float mStart = smoothstep(0.0, featherAng, angN);
                            float mEnd   = 1.0 - smoothstep(aLen - featherAng, aLen, angN);
                            float angularAlpha = inside * mStart * mEnd;
                
                            float d = sdRing(p, radius, max(thickness, 0.0001));
                            float radialAlpha = 1.0 - smoothstep(0.0, feather, d);
                
                            float t = (aLen > 0.0) ? clamp(angN / aLen, 0.0, 1.0) : 0.0;
                            t = pow(t, max(0.001, gradExp));
                            vec4 col = mix(colorA, colorB, t);
                
                            float a = radialAlpha * angularAlpha;
                            gl_FragColor = vec4(col.rgb, col.a * a);
                        }
                """;
    }
}