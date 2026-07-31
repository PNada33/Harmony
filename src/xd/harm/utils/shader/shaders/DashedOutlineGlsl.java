package xd.harm.utils.shader.shaders;

import xd.harm.utils.shader.IShader;

public class DashedOutlineGlsl implements IShader {
    @Override
    public String glsl() {
        return """
                #version 120

                uniform vec2 quadSize;
                uniform vec2 innerSize;
                uniform vec4 round;
                uniform vec2 smoothness;
                uniform vec4 outlineColor;
                uniform float outlineThickness;
                uniform float time;
                uniform float globalAlpha; // Новая переменная для прозрачности при наводке

                float test(vec2 vec_1, vec2 vec_2, vec4 vec_4) {
                    vec_4.xy = (vec_1.x > 0.0) ? vec_4.xy : vec_4.zw;
                    vec_4.x = (vec_1.y > 0.0) ? vec_4.x : vec_4.y;
                    vec2 coords = abs(vec_1) - vec_2 + vec_4.x;
                    return min(max(coords.x, coords.y), 0.0) + length(max(coords, vec2(0.0f))) - vec_4.x;
                }

                void main() {
                    vec2 st = gl_TexCoord[0].st * quadSize;
                    vec2 halfQuad = 0.5 * quadSize;
                    vec2 halfInner = 0.5 * innerSize;
                    vec2 center = halfQuad;
                    
                    float dist = test(center - st, halfInner - outlineThickness/2.0, round);
                    float dLine = abs(dist) - outlineThickness/2.0;
                    float sdfAlpha = 1.0 - smoothstep(smoothness.x, smoothness.y, dLine);
                    
                    if (sdfAlpha <= 0.0) discard;

                    // Вычисляем длину по периметру
                    vec2 toCenter = st - center;
                    float angle = atan(toCenter.y, toCenter.x) + 3.14159265;
                    float maxR = max(max(round.x, round.y), max(round.z, round.w));
                    float perimeter = 2.0 * innerSize.x + 2.0 * innerSize.y - 8.0 * maxR + 2.0 * 3.14159265 * maxR;
                    float pathDist = (angle / 6.28318530) * perimeter;

                    // Одна плавная линия-комета
                    float speed = 1.0;
                    float pos = mod(time * perimeter * speed, perimeter);
                    
                    // Вычисляем расстояние за летящей головой
                    float distBehind = mod(pos - pathDist, perimeter);
                    
                    // Сама голова более яркая
                    float head = max(0.0, 1.0 - min(abs(pos - pathDist), perimeter - abs(pos - pathDist)) / 30.0);
                    
                    // Длинный плавный хвост
                    float tail = exp(-distBehind * 0.007); 

                    // Базовый цвет (очень слабое свечение рамки)
                    float visibility = 0.05; 
                    
                    // Добавляем эффект кометы (голова + хвост)
                    visibility += tail * 1.5 + head * 2.0;

                    // Внешнее свечение вокруг кометы
                    float outerGlow = max(0.0, 1.0 - (dLine / (outlineThickness * 4.0)));
                    visibility += outerGlow * 0.5 * (tail + head);

                    vec4 finalColor = outlineColor;
                    
                    // ОЧЕНЬ ВАЖНО: Учитываем globalAlpha для плавного появления/исчезновения при наводке
                    finalColor.a *= sdfAlpha * min(visibility, 1.0) * globalAlpha;
                    
                    // Чем ярче точка, тем белее цвет (ослепительное ядро)
                    finalColor.rgb = mix(finalColor.rgb, vec3(1.0, 1.0, 1.0), clamp(head + tail*0.5, 0.0, 1.0));

                    gl_FragColor = finalColor;
                }
                """;
    }
}
