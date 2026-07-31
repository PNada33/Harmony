package xd.harm.utils.shaderharmony;

import xd.harm.utils.shaderharmony.shaders.*;
import lombok.Getter;

@Getter
public class Shaders {
    @Getter
    private final static Shaders Instance = new Shaders();
    private final IShader font = new FontGlsl();
    private final IShader vertex = new VertexGlsl();
    private final IShader rounded = new RoundedGlsl();
    private final IShader roundedout = new RoundedOutGlsl();
    private final IShader smooth = new SmoothGlsl();
    private final IShader white = new WhiteGlsl();
    private final IShader alpha = new AlphaGlsl();
    private final IShader gaussianbloom = new GaussianBloomGlsl();
    private final IShader kawaseUp = new KawaseUpGlsl();
    private final IShader kawaseDown = new KawaseDownGlsl();
    private final IShader outline = new OutlineGlsl();
    private final IShader contrast = new ContrastGlsl();
    private final IShader mask = new MaskGlsl();
    private final IShader MainMenuShader = new MainMenuGlsl();
    private final IShader gradient = new GradientGlsl();
    private final IShader roundedTex = new RoundedTextureGlsl();
    private final IShader outlineEsp = new OutlineESPGlsl();
    private final IShader outlineC = new OutlineCGlsl();
    private final IShader blur = new BlurGlsl();
    private final IShader blurC = new BlurCGlsl();
    private final IShader kawaseUpBloom = new KawaseUpBloom();
    private final IShader kawaseDownBloom = new KawaseDownBloom();
    private final IShader roundedFace = new RoundedFaceGlsl();
    private final IShader smoothGradient = new GradientSmoothGlsl();

    public static Shaders getInstance() {
        return Instance;
    }

    public IShader getFont() {
        return font;
    }

    public IShader getVertex() {
        return vertex;
    }

    public IShader getRounded() {
        return rounded;
    }

    public IShader getRoundedout() {
        return roundedout;
    }

    public IShader getSmooth() {
        return smooth;
    }

    public IShader getWhite() {
        return white;
    }

    public IShader getAlpha() {
        return alpha;
    }

    public IShader getGaussianbloom() {
        return gaussianbloom;
    }

    public IShader getKawaseUp() {
        return kawaseUp;
    }

    public IShader getKawaseDown() {
        return kawaseDown;
    }

    public IShader getOutline() {
        return outline;
    }

    public IShader getContrast() {
        return contrast;
    }

    public IShader getMask() {
        return mask;
    }

    public IShader getMainMenuShader() {
        return MainMenuShader;
    }

    public IShader getGradient() {
        return gradient;
    }

    public IShader getRoundedTex() {
        return roundedTex;
    }

    public IShader getOutlineEsp() {
        return outlineEsp;
    }

    public IShader getOutlineC() {
        return outlineC;
    }

    public IShader getBlur() {
        return blur;
    }

    public IShader getBlurC() {
        return blurC;
    }

    public IShader getKawaseUpBloom() {
        return kawaseUpBloom;
    }

    public IShader getKawaseDownBloom() {
        return kawaseDownBloom;
    }

    public IShader getRoundedFace() {
        return roundedFace;
    }

    public IShader getSmoothGradient() {
        return smoothGradient;
    }

}
