package xd.harm.utils.shader.shaders;

import xd.harm.utils.shader.IShader;

public class MainMenu2Glsl implements IShader {
    @Override
    public String glsl() {
        return """
                #extension GL_OES_standard_derivatives : enable
                
                 precision mediump float;
                 uniform float time;
                 uniform vec2 mouse;
                 uniform float width;
                 uniform float height;
                 uniform float mousex;
                 uniform float mousey;
                
                 void main(void) {
                     vec2 m = vec2(mousex * 2.0 - 1.0, mousey * 2.0 - 1.0);

                     vec2 p = (gl_FragCoord.xy * 2.0 - vec2(width, height)) / min(width, height);
                   
                     float lambda = time * 2.5;

                     float t = 0.02 / abs(tan(lambda) - length(p));
                     float t2 = atan(p.y, p.x) + time;
                
                     vec2 something = vec2(1.0, (sin(time) + 1.0) * 0.5);

                     float dotProduct = dot(vec2(t), something) / length(p);

                     gl_FragColor = vec4(vec3(dotProduct), 1.0);
                 }
                """;
    }
}
