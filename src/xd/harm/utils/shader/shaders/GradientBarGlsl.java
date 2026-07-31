package xd.harm.utils.shader.shaders;

import xd.harm.utils.shader.IShader;

public class GradientBarGlsl implements IShader {
    @Override
    public String glsl() {
        return "#version 120\n" +
                "\n" +
                "uniform vec2 size;\n" +
                "uniform float radius;\n" +
                "uniform vec4 colorA;\n" +
                "uniform vec4 colorB;\n" +
                "uniform float rotation;\n" +
                "\n" +
                "float roundedBoxSDF(vec2 centerPos, vec2 size, float radius) {\n" +
                "    return length(max(abs(centerPos) - size + radius, 0.0)) - radius;\n" +
                "}\n" +
                "\n" +
                "void main() {\n" +
                "    vec2 uv = gl_TexCoord[0].xy * size;\n" +
                "    vec2 center = size * 0.5;\n" +
                "    \n" +
                "    float distance = roundedBoxSDF(uv - center, size * 0.5, radius);\n" +
                "    \n" +
                "    if (distance > 0.0) {\n" +
                "        discard;\n" +
                "    }\n" +
                "    \n" +
                "    float normalizedX = uv.x / size.x;\n" +
                "    \n" +
                "    float gradientPos = normalizedX + rotation / 6.28318;\n" +
                "    \n" +
                "    float gradient = (sin(gradientPos * 6.28318 * 2.0) + 1.0) * 0.5;\n" +
                "    \n" +
                "    vec4 finalColor = mix(colorA, colorB, gradient);\n" +
                "    \n" +
                "    float edgeSmooth = 1.0 - smoothstep(-1.0, 0.0, distance);\n" +
                "    finalColor.a *= edgeSmooth;\n" +
                "    \n" +
                "    gl_FragColor = finalColor;\n" +
                "}\n";
    }
}