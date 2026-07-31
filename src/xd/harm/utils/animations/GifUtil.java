package xd.harm.utils.animations;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.NativeImage;
import net.minecraft.util.ResourceLocation;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.stream.ImageInputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class GifUtil {
    final List<ResourceLocation> frames = new ArrayList<>();
    final List<Integer> frameDelays = new ArrayList<>();

    @Getter
    int currentFrame = 0;
    @Getter
    int gifWidth = 0;
    @Getter
    int gifHeight = 0;
    long lastFrameTime = 0;
    boolean loaded = false;
    boolean triedToLoad = false;
    final String gifPath;

    public GifUtil(String gifPath) {
        this.gifPath = gifPath;
    }

    private void loadGif() {
        if (triedToLoad) return;
        triedToLoad = true;

        try {
            ResourceLocation gifLocation = new ResourceLocation(gifPath);
            InputStream inputStream = Minecraft.getInstance().getResourceManager().getResource(gifLocation).getInputStream();
            ImageInputStream stream = ImageIO.createImageInputStream(inputStream);
            ImageReader reader = ImageIO.getImageReadersByFormatName("gif").next();
            reader.setInput(stream);

            int frameCount = reader.getNumImages(true);

            BufferedImage firstFrame = reader.read(0);
            int width = firstFrame.getWidth();
            int height = firstFrame.getHeight();
            this.gifWidth = width;
            this.gifHeight = height;

            reader.dispose();
            stream.close();
            inputStream.close();

            inputStream = Minecraft.getInstance().getResourceManager().getResource(gifLocation).getInputStream();
            stream = ImageIO.createImageInputStream(inputStream);
            reader = ImageIO.getImageReadersByFormatName("gif").next();
            reader.setInput(stream);

            BufferedImage masterImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            Graphics2D masterGraphics = masterImage.createGraphics();
            masterGraphics.setBackground(new Color(0, 0, 0, 0));

            BufferedImage previousImage = null;

            for (int i = 0; i < frameCount; i++) {
                BufferedImage frameImage = reader.read(i);
                IIOMetadata metadata = reader.getImageMetadata(i);

                String disposalMethod = "none";
                int delay = 100;
                int frameX = 0;
                int frameY = 0;
                int frameW = frameImage.getWidth();
                int frameH = frameImage.getHeight();

                String metaFormatName = metadata.getNativeMetadataFormatName();
                org.w3c.dom.Node root = metadata.getAsTree(metaFormatName);
                org.w3c.dom.NodeList children = root.getChildNodes();

                for (int j = 0; j < children.getLength(); j++) {
                    org.w3c.dom.Node node = children.item(j);

                    if (node.getNodeName().equals("ImageDescriptor")) {
                        org.w3c.dom.NamedNodeMap attrs = node.getAttributes();
                        if (attrs.getNamedItem("imageLeftPosition") != null)
                            frameX = Integer.parseInt(attrs.getNamedItem("imageLeftPosition").getNodeValue());
                        if (attrs.getNamedItem("imageTopPosition") != null)
                            frameY = Integer.parseInt(attrs.getNamedItem("imageTopPosition").getNodeValue());
                    }

                    if (node.getNodeName().equals("GraphicControlExtension")) {
                        org.w3c.dom.NamedNodeMap attrs = node.getAttributes();
                        if (attrs.getNamedItem("disposalMethod") != null)
                            disposalMethod = attrs.getNamedItem("disposalMethod").getNodeValue();
                        if (attrs.getNamedItem("delayTime") != null)
                            delay = Integer.parseInt(attrs.getNamedItem("delayTime").getNodeValue()) * 10;
                    }
                }

                if (delay <= 0) delay = 100;

                if (disposalMethod.equals("restoreToPrevious")) {
                    previousImage = copyImage(masterImage);
                }

                masterGraphics.drawImage(frameImage, frameX, frameY, null);

                BufferedImage copy = copyImage(masterImage);
                frameDelays.add(delay);

                NativeImage nativeImage = convertToNativeImage(copy);
                DynamicTexture dynamicTexture = new DynamicTexture(nativeImage);
                ResourceLocation frameLocation = Minecraft.getInstance().getTextureManager()
                        .getDynamicTextureLocation("gif_" + gifPath.hashCode() + "_" + i, dynamicTexture);
                frames.add(frameLocation);

                if (disposalMethod.equals("restoreToBackgroundColor")) {
                    masterGraphics.setComposite(AlphaComposite.Clear);
                    masterGraphics.fillRect(frameX, frameY, frameW, frameH);
                    masterGraphics.setComposite(AlphaComposite.SrcOver);
                } else if (disposalMethod.equals("restoreToPrevious")) {
                    if (previousImage != null) {
                        masterGraphics.setComposite(AlphaComposite.Src);
                        masterGraphics.drawImage(previousImage, 0, 0, null);
                        masterGraphics.setComposite(AlphaComposite.SrcOver);
                    }
                }
            }

            masterGraphics.dispose();
            reader.dispose();
            stream.close();
            inputStream.close();

            loaded = true;

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private BufferedImage copyImage(BufferedImage source) {
        BufferedImage copy = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = copy.createGraphics();
        g.drawImage(source, 0, 0, null);
        g.dispose();
        return copy;
    }

    private NativeImage convertToNativeImage(BufferedImage bufferedImage) {
        int width = bufferedImage.getWidth();
        int height = bufferedImage.getHeight();

        NativeImage nativeImage = new NativeImage(NativeImage.PixelFormat.RGBA, width, height, false);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int argb = bufferedImage.getRGB(x, y);
                int a = (argb >> 24) & 0xFF;
                int r = (argb >> 16) & 0xFF;
                int g = (argb >> 8) & 0xFF;
                int b = argb & 0xFF;
                int abgr = (a << 24) | (b << 16) | (g << 8) | r;
                nativeImage.setPixelRGBA(x, y, abgr);
            }
        }

        return nativeImage;
    }

    public void update() {
        if (!triedToLoad) {
            loadGif();
        }

        if (frames.isEmpty()) return;

        long currentTime = System.currentTimeMillis();
        int delay = frameDelays.isEmpty() ? 100 : frameDelays.get(currentFrame);

        if (currentTime - lastFrameTime >= delay) {
            currentFrame = (currentFrame + 1) % frames.size();
            lastFrameTime = currentTime;
        }
    }

    public ResourceLocation getCurrentFrame() {
        if (frames.isEmpty()) return null;
        return frames.get(currentFrame);
    }

    public void reset() {
        currentFrame = 0;
        lastFrameTime = System.currentTimeMillis();
    }

    public boolean isLoaded() {
        return loaded && !frames.isEmpty();
    }

    public int getLoadedFramesCount() {
        return frames.size();
    }
}