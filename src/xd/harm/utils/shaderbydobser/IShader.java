package xd.harm.utils.shaderbydobser;

public interface IShader {

    String glsl();

    default String getName() {
        return "SHADERNONAME";
    }

}
