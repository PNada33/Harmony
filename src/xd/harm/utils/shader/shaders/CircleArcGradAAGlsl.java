package xd.harm.utils.shader.shaders;

import xd.harm.utils.shader.IShader;

public class CircleArcGradAAGlsl implements IShader {
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
                
                        const float PI  = 3.14159265359;
                        const float PI2 = 6.28318530718;
                
                        float mod2pi(float a){ return a - floor(a / PI2) * PI2; }
                
                        float sdRing(vec2 p, float R, float t){
                            float halfT = max(0.0001, t * 0.5);
                            return abs(length(p) - (R - halfT)) - halfT;
                        }
                
                        void main() {
                            vec2 uv    = gl_TexCoord[0].st;
                            vec2 px    = uv * size;
                            vec2 p     = px - center;
                
                            float a0 = mod2pi(startAngle);
                            float a1 = mod2pi(endAngle);
                            float len = a1 - a0;
                            if (len < 0.0) len += PI2;
                
                            float ang  = atan(p.y, p.x);
                            float angN = mod2pi(ang - a0);
                
                            float inside = step(0.0, angN) * step(angN, len);
                
                            float fAng = max(0.5, feather) / max(radius, 1.0);
                            float mS = smoothstep(0.0, fAng, angN);
                            float mE = 1.0 - smoothstep(len - fAng, len, angN);
                            float aA = inside * mS * mE;
                
                            float d = sdRing(p, radius, max(thickness, 0.0001));
                            float aR = 1.0 - smoothstep(0.0, feather, d);
                
                            float t = len <= 1e-5 ? 0.0 : clamp(angN / len, 0.0, 1.0);
                            t = pow(t, max(0.001, gradExp));
                            vec4 col = mix(colorA, colorB, t);
                
                            float a = aR * aA;
                            gl_FragColor = vec4(col.rgb, col.a * a);
                        }
                """;
    }
}