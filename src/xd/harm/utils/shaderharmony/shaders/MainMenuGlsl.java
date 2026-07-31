package xd.harm.utils.shaderharmony.shaders;

import xd.harm.utils.shaderharmony.IShader;

public class MainMenuGlsl implements IShader {
    @Override
    public String glsl() {
        return """
				#ifdef GL_ES
				precision mediump float;
				#endif
				
				uniform float time;
				uniform float mouseX;
				uniform float mouseY;
				uniform float width;
				uniform float height; 
				
				float interpolate(float x, float min_x, float max_x) {
				    return x * max_x + (1.0 - x) * min_x;
				}
				
				float normsin(float x) {
				    return (sin(x) + 1.0) / 11.0;
				}
				
				void main(void) {
				    vec2 position = (gl_FragCoord.xy / width);
				    
				    float color = normsin(30.0 * position.x + interpolate(normsin(25.0 * position.y + 10.0 * mouseX), 5.0, 25.0) + 
				                          30.0 * position.y + interpolate(normsin(25.0 * position.x + 10.0 * mouseY), 5.0, 25.0) + 2.0 * time);
				    gl_FragColor = vec4(color, color, color, 1.0);
				}
""";
    }
}
