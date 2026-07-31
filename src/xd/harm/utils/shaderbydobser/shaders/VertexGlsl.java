package xd.harm.utils.shaderbydobser.shaders;

import xd.harm.utils.shaderbydobser.IShader;

public class VertexGlsl implements IShader {


    @Override
    public String glsl() {
        return """
                #version 120 
                 void main() {

                     gl_TexCoord[0] = gl_MultiTexCoord0;
                     gl_Position = gl_ModelViewProjectionMatrix * gl_Vertex;
                 }
                 """;
    }
}
