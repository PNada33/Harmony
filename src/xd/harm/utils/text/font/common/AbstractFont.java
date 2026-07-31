package xd.harm.utils.text.font.common;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

import com.mojang.blaze3d.platform.GlStateManager;

import xd.harm.utils.text.font.ClientFonts;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.Minecraft;
import net.minecraft.util.math.vector.Matrix4f;
import xd.harm.utils.text.font.TextureHelper;
import xd.harm.utils.text.font.Wrapper;

public abstract class AbstractFont implements Wrapper {

    protected final Map<Character, Glyph> glyphs = new HashMap<>();
    protected int texId, imgWidth, imgHeight;
    protected float fontHeight;
    protected String fontName;
    protected boolean antialiasing;

    public abstract float getStretching();

    public abstract float getSpacing();

    public abstract float getLifting();

    public float getFontHeight() {
        return fontHeight;
    }

    public final String getFontName() {
        return fontName;
    }

    protected final void setTexture(BufferedImage img) {
        texId = TextureHelper.loadTexture(img);
    }

    public final void bindTex() {
        GlStateManager.bindTexture(texId);
    }

    public final void unbindTex() {
        GlStateManager.bindTexture(0);
    }

    public static final Font getFont(String fileName, int style, int size) {
        String path = ClientFonts.FONT_DIR.concat(fileName);

        try (InputStream in = ClientFonts.class.getResourceAsStream(path)) {
            if (in != null) {
                return Font.createFont(Font.TRUETYPE_FONT, in).deriveFont(style, size);
            }
        } catch (FontFormatException | IOException e) {
            e.printStackTrace();
        }

        String relative = path.startsWith("/") ? path.substring(1) : path;
        File gameDir = null;
        try {
            gameDir = Minecraft.getInstance().gameDir;
        } catch (Throwable ignored) {
        }

        File[] candidates = gameDir == null ? new File[]{
                new File(relative),
                new File("src", relative),
                new File("out/production/client", relative)
        } : new File[]{
                new File(relative),
                new File(gameDir, relative),
                new File("src", relative),
                new File(gameDir, "src/" + relative),
                new File("out/production/client", relative),
                new File(gameDir, "out/production/client/" + relative)
        };

        for (File candidate : candidates) {
            if (!candidate.isFile()) continue;
            try (InputStream in = Files.newInputStream(candidate.toPath())) {
                return Font.createFont(Font.TRUETYPE_FONT, in).deriveFont(style, size);
            } catch (FontFormatException | IOException e) {
                e.printStackTrace();
            }
        }

        return new Font("SansSerif", style, size);
    }

    public static final Font getFontWindows(String fileName, int style, int size) {
        try (InputStream in = Files.newInputStream(new File("C:/Windows/ClientFonts/" + fileName).toPath())) {
            return Font.createFont(Font.TRUETYPE_FONT, in).deriveFont(style, size);
        } catch (FontFormatException | IOException ignored) {
        }
        try (InputStream in = Files.newInputStream(new File("C:/Windows/Fonts/" + fileName).toPath())) {
            return Font.createFont(Font.TRUETYPE_FONT, in).deriveFont(style, size);
        } catch (FontFormatException | IOException ignored) {
        }
        return getFont(fileName, style, size);
    }


    public final Graphics2D setupGraphics(BufferedImage img, Font font) {
        Graphics2D graphics = img.createGraphics();

        graphics.setFont(font);
        graphics.setColor(new Color(255, 255, 255, 0));
        graphics.fillRect(0, 0, imgWidth, imgHeight);
        graphics.setColor(Color.WHITE);
        if (antialiasing) {
            graphics.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        }

        return graphics;
    }

    public float renderGlyph(Matrix4f matrix, char c, float x, float y, float red, float green, float blue, float alpha) {
        Glyph glyph = glyphs.get(c);
        if (glyph == null)
            return 0;
        float pageX = glyph.x / (float) imgWidth;
        float pageY = glyph.y / (float) imgHeight;
        float pageWidth = glyph.width / (float) imgWidth;
        float pageHeight = glyph.height / (float) imgHeight;
        float width = glyph.width + getStretching();
        float height = glyph.height;

        BUILDER.begin(7, DefaultVertexFormats.POSITION_COLOR_TEX);
        BUILDER.pos(matrix, x, y + height, 0).color(red, green, blue, alpha)
                .tex(pageX, pageY + pageHeight).endVertex();
        BUILDER.pos(matrix, x + width, y + height, 0).color(red, green, blue, alpha)
                .tex(pageX + pageWidth, pageY + pageHeight).endVertex();
        BUILDER.pos(matrix, x + width, y, 0).color(red, green, blue, alpha)
                .tex(pageX + pageWidth, pageY).endVertex();
        BUILDER.pos(matrix, x, y, 0).color(red, green, blue, alpha)
                .tex(pageX, pageY).endVertex();
        TESSELLATOR.draw();

        return width + getSpacing();
    }

    public float getWidth(char ch) {
        Glyph glyph = glyphs.get(ch);
        return (glyph != null) ? (glyph.width + getStretching()) : 0;
    }

    public float getWidth(String text) {
        if (text == null)
            return 0;

        float width = 0.0f;

        float sp = getSpacing();
        for (int i = 0; i < text.length(); i++) {
            width += getWidth(text.charAt(i)) + sp;
        }

        return (width - sp) / 2f;
    }
    public class Glyph {

        public int x;
        public int y;
        public int width;
        public int height;

    }

}
