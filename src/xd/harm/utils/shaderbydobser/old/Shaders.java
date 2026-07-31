package xd.harm.utils.shaderbydobser.old;


import java.util.ArrayList;
import java.util.List;

public class Shaders {

    public List<Shader> shaderList = new ArrayList<>();

    public Shaders() {

        shaderList.addAll(List.of(
                new AlphaShader("")
        ));

    }

}