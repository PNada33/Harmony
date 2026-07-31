package xd.harm.utils.shaderbydobser;


import xd.harm.utils.shaderbydobser.shaders.AlphaGlsl;
import xd.harm.utils.shaderbydobser.shaders.ContrastGlsl;
import xd.harm.utils.shaderbydobser.shaders.FontGlsl;
import xd.harm.utils.shaderbydobser.shaders.GaussianBloomGlsl;
import xd.harm.utils.shaderbydobser.shaders.GradientGlsl;
import xd.harm.utils.shaderbydobser.shaders.KawaseDownGlsl;
import xd.harm.utils.shaderbydobser.shaders.KawaseUpGlsl;
import xd.harm.utils.shaderbydobser.shaders.MainMenu2Glsl;
import xd.harm.utils.shaderbydobser.shaders.MainMenuGlsl;
import xd.harm.utils.shaderbydobser.shaders.MaskGlsl;
import xd.harm.utils.shaderbydobser.shaders.OutlineGlsl;
import xd.harm.utils.shaderbydobser.shaders.RoundedGlsl;
import xd.harm.utils.shaderbydobser.shaders.RoundedOutGlsl;
import xd.harm.utils.shaderbydobser.shaders.SmoothGlsl;
import xd.harm.utils.shaderbydobser.shaders.VertexGlsl;
import xd.harm.utils.shaderbydobser.shaders.WhiteGlsl;
import lombok.Getter;

public class Shaders {
    @Getter
    private static Shaders Instance = new Shaders();
    @Getter
    private IShader font = new FontGlsl();
    @Getter
    private IShader vertex = new VertexGlsl();
    @Getter
    private IShader rounded = new RoundedGlsl();
    @Getter
    private IShader roundedout = new RoundedOutGlsl();
    @Getter
    private IShader smooth = new SmoothGlsl();
    @Getter
    private IShader white = new WhiteGlsl();
    @Getter
    private IShader alpha = new AlphaGlsl();
    @Getter
    private IShader gaussianbloom = new GaussianBloomGlsl();
    @Getter
    private IShader kawaseUp = new KawaseUpGlsl();
    @Getter
    private IShader kawaseDown = new KawaseDownGlsl();
    @Getter
    private IShader outline = new OutlineGlsl();
    @Getter
    private IShader contrast = new ContrastGlsl();
    @Getter
    private IShader mask = new MaskGlsl();
    @Getter
    private IShader MainMenuShader = new MainMenuGlsl();
    @Getter
    private IShader MainMenu2Shader = new MainMenu2Glsl();
    @Getter
    private IShader gradient = new GradientGlsl();
}
