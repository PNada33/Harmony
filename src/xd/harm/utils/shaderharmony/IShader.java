package xd.harm.utils.shaderharmony;

public interface IShader {

    String glsl();

    default String getName() {
        return "SHADERNONAME";
    }

}
