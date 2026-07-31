package xd.harm.utils.shader;

import xd.harm.utils.shader.shaders.*;

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
    private IShader round = new RoundGlsl();
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
    @Getter
    private IShader head = new HeadGlsl();
    @Getter
    private IShader ringArc = new RingArcAAGlsl();
    @Getter
    private IShader ringArcGradient = new RingArcGradientGlsl();
    @Getter
    private IShader ringArcGrad = new RingArcGradAAGlsl();
    @Getter
    private IShader circleArc = new xd.harm.utils.shader.shaders.CircleArcAAGlsl();
    @Getter
    private IShader circleArcGrad = new xd.harm.utils.shader.shaders.CircleArcGradAAGlsl();
    @Getter
    private IShader hudHolo = new xd.harm.utils.shader.shaders.HudHoloGlsl();
    @Getter
    private IShader hudRays = new xd.harm.utils.shader.shaders.HudRaysGlsl();
    @Getter
    private IShader InfinityLoader = new InfinityLoaderGlsl();
    @Getter
    private IShader ExitShader = new ExitShaderGlsl();
    @Getter
    private IShader RingArcGradientGlsl = new RingArcGradientGlsl();
    @Getter
    private IShader GradientBar = new GradientBarGlsl();
    @Getter
    private IShader dashedOutline = new DashedOutlineGlsl();
    @Getter
    private IShader menuButton = new MenuButtonGlsl();
    @Getter
    private IShader mainMenuGrid = new MainMenuGridGlsl();
    @Getter
    private IShader radialFill = new RadialFillGlsl();

    public static Shaders getInstance() {
        return Instance;
    }

    public IShader getFont() { return font; }
    public IShader getVertex() { return vertex; }
    public IShader getRounded() { return rounded; }
    public IShader getRoundedout() { return roundedout; }
    public IShader getSmooth() { return smooth; }
    public IShader getWhite() { return white; }
    public IShader getRound() { return round; }
    public IShader getAlpha() { return alpha; }
    public IShader getGaussianbloom() { return gaussianbloom; }
    public IShader getKawaseUp() { return kawaseUp; }
    public IShader getKawaseDown() { return kawaseDown; }
    public IShader getOutline() { return outline; }
    public IShader getContrast() { return contrast; }
    public IShader getMask() { return mask; }
    public IShader getMainMenuShader() { return MainMenuShader; }
    public IShader getMainMenu2Shader() { return MainMenu2Shader; }
    public IShader getGradient() { return gradient; }
    public IShader getHead() { return head; }
    public IShader getRingArc() { return ringArc; }
    public IShader getRingArcGradient() { return ringArcGradient; }
    public IShader getRingArcGrad() { return ringArcGrad; }
    public IShader getCircleArc() { return circleArc; }
    public IShader getCircleArcGrad() { return circleArcGrad; }
    public IShader getHudHolo() { return hudHolo; }
    public IShader getHudRays() { return hudRays; }
    public IShader getInfinityLoader() { return InfinityLoader; }
    public IShader getExitShader() { return ExitShader; }
    public IShader getRingArcGradientGlsl() { return RingArcGradientGlsl; }
    public IShader getGradientBar() { return GradientBar; }
    public IShader getDashedOutline() { return dashedOutline; }
    public IShader getMenuButton() { return menuButton; }
    public IShader getMainMenuGrid() { return mainMenuGrid; }
    public IShader getRadialFill() { return radialFill; }
}
