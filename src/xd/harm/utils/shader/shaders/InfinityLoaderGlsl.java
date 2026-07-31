package xd.harm.utils.shader.shaders;

import xd.harm.utils.shader.IShader;

public class InfinityLoaderGlsl implements IShader {
    @Override
    public String glsl() {
        return """
                #version 120
                
                uniform vec2  size;       
                uniform float thickness;  
                uniform float feather;    
                uniform float time;       
                uniform float speed;      
                uniform float alpha;      
                uniform vec4  colorA;     
                uniform vec4  colorB;     
                uniform vec4  colorGlow;

                #define PI 3.14159265359
                #define TWO_PI 6.28318530718

                vec2 lemniscate(float t) {
                    float a = 0.35;
                    float sinT = sin(t);
                    float cosT = cos(t);
                    float denom = 1.0 + sinT * sinT;
                    return vec2(
                        a * cosT / denom,
                        a * sin(t * 2.0) / (2.0 * denom)
                    );
                }

           
                float easeInOutCubic(float x) {
                    return x < 0.5 ? 4.0 * x * x * x : 1.0 - pow(-2.0 * x + 2.0, 3.0) * 0.5;
                }

                
                float distanceToSegment(vec2 p, vec2 a, vec2 b) {
                    vec2 pa = p - a;
                    vec2 ba = b - a;
                    float h = clamp(dot(pa, ba) / dot(ba, ba), 0.0, 1.0);
                    return length(pa - ba * h);
                }

                void main() {
                    vec2 uv = gl_TexCoord[0].st;
                    vec2 p = (uv - 0.5) * size;
                    float s = min(size.x, size.y);
                    p /= s;
                    p.x *= 1.5;
                    
                    float px = 1.0 / s;
                    
                   
                    float a = 0.35;
                    float a2 = a * a;
                    float x = p.x, y = p.y;
                    float x2 = x * x, y2 = y * y;
                    float r2 = x2 + y2;
                    float f = r2 * r2 - a2 * (x2 - y2);
                    float gx = 4.0 * r2 * x - 2.0 * a2 * x;
                    float gy = 4.0 * r2 * y + 2.0 * a2 * y;
                    float d = abs(f) / (sqrt(gx * gx + gy * gy) + 0.0001);
                    
                   
                    float distToCenter = length(p);
                    float centerMask = smoothstep(0.0, 0.08, distToCenter);
                    
                   
                    float rawBallPos = fract(time * speed * 0.15);
                    float ballPos = easeInOutCubic(rawBallPos);
                    float ballT = ballPos * TWO_PI;
                    vec2 ballWorldPos = lemniscate(ballT);
                    float distToBall = distance(p, ballWorldPos);
                    
                   
                    float minDistToTail = 10000.0;
                    const float tailSamples = 35.0;
                    const float invTailSamples = 1.0 / tailSamples;
                    
                    vec2 prevPoint = ballWorldPos;
                    for(float i = 1.0; i < tailSamples; i++) {
                        float tailOffset = (i * invTailSamples) * 0.35;
                        float tailT = (ballPos - tailOffset) * TWO_PI;
                        vec2 currentPoint = lemniscate(tailT);
                        
                
                        float segDist = distanceToSegment(p, prevPoint, currentPoint);
                        
            
                        float fade = 1.0 - (i * invTailSamples);
                        fade = pow(fade, 0.8);
                        
                        minDistToTail = min(minDistToTail, segDist / (fade * 0.5 + 0.5));
                        prevPoint = currentPoint;
                    }
                    
          
                    float tN = thickness * px;
                    float feath = max(px * 3.0, feather * px);
                    float lineThreshold = tN * 0.4;
                    
        
                    float line = 1.0 - smoothstep(lineThreshold, lineThreshold + feath, d);
                    
                    float ballIntensity = pow(1.0 - smoothstep(0.0, 0.03, distToBall), 1.2);
                    
                    float tailIntensity = 1.0 - smoothstep(0.0, 0.025, minDistToTail);
                    tailIntensity = pow(tailIntensity, 0.6);
                    tailIntensity *= smoothstep(tN + feath, tN * 0.3, d) * centerMask;
                    
                    float colorPhase = ballPos;
                    float colorMix = easeInOutCubic(fract(colorPhase)) * 0.85 + 0.15;
                    vec3 activeColor = mix(colorA.rgb, colorB.rgb, colorMix);
                    
                    vec3 baseColor = vec3(0.16, 0.16, 0.19);
                    
                    vec3 finalColor = baseColor * line * 0.45;
                    finalColor += activeColor * tailIntensity * 2.0;
                    
                    vec3 ballColor = mix(activeColor, colorGlow.rgb, 0.35);
                    finalColor += ballColor * ballIntensity * 2.8;
                    

                    float tN3 = tN * 3.0;
                    float ballGlow = exp(-distToBall * 25.0) * smoothstep(tN3, 0.0, d) * 0.9;
                    float tailGlow = exp(-minDistToTail * 18.0) * smoothstep(tN * 2.5, 0.0, d) * centerMask * 0.45;
                    
                    finalColor += activeColor * (ballGlow + tailGlow);
                    

                    float centerGlow = (1.0 - centerMask) * line;
                    finalColor += baseColor * (centerGlow * 1.2 + exp(-distToCenter * 15.0) * (1.0 - centerMask) * 0.4);
                    
 
                    finalColor += baseColor * exp(-(d / (tN * 2.5 + 0.0001)) * 4.5) * line * 0.25;
                    
          
                    float finalAlpha = clamp(
                        line * 0.85 + 
                        ballIntensity * 0.6 +
                        tailIntensity * 0.5 +
                        ballGlow * 0.25 +
                        centerGlow * 0.3,
                        0.0, 1.0
                    ) * alpha;
                    
                    gl_FragColor = vec4(finalColor, finalAlpha);
                }
                """;
    }
}