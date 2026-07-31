package xd.harm.ui.display.impl;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import xd.harm.Harmony;
import xd.harm.events.render.EventDisplay;
import xd.harm.modules.api.Module;
import xd.harm.modules.impl.render.HUD;
import xd.harm.utils.math.MathUtil;
import xd.harm.utils.render.rect.RenderUtility;
import xd.harm.utils.render.color.ColorUtils;
import xd.harm.utils.render.font.Fonts;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.ChatScreen;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

public class ArrayListRenderer {

    private final Minecraft mc = Minecraft.getInstance();
    private static final String[] EXCLUDE_MATCHES = {
            "nointeract", "procontainer", "respawn",
            "namesecurity", "srpspoofer", "middleclick", "onfalling"
    };

    private final List<Module> enabledModules = new ArrayList<>();
    private final Map<Module, Float> moduleWidthCache = new HashMap<>();
    private long lastUpdateTime = 0;
    
    private float alphaPC = 0;
    private float targetAlpha = 0;

    public ArrayListRenderer() {
    }

    public void render(EventDisplay event) {
        MatrixStack ms = event.getMatrixStack();
        
        HUD hudModule = Harmony.getInstance().getModuleManager().getHud();
        
        if (hudModule == null) return;
        
        targetAlpha = hudModule.isState() ? 1.0f : 0.0f;
        
        if (Math.abs(alphaPC - targetAlpha) > 0.01f) {
            alphaPC = MathUtil.lerp(alphaPC, targetAlpha, 0.05f);
        } else {
            alphaPC = targetAlpha;
        }
        
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastUpdateTime > 300 || mc.currentScreen instanceof ChatScreen) {
            updateModuleList();
            lastUpdateTime = currentTime;
        }

        if (!canRenderList()) {
            if (enabledModules.isEmpty()) {
                updateModuleList();
            }
            return;
        }
        
        GL11.glPushMatrix();
        GL11.glDepthMask(false);
        
        drawArrayList(alphaPC, event, ms, hudModule);
        
        RenderSystem.enableDepthTest();
        GL11.glDepthMask(true);
        GL11.glPopMatrix();
    }

    private void drawArrayList(float alpha, EventDisplay event, MatrixStack ms, HUD hudModule) {
        float x = mc.getMainWindow().getScaledWidth() - 5;
        float y = 5;
        float step = 10.001f;
        
        int index = 0;
        for (Module module : enabledModules) {
            drawModule(x, y, module, step, index, alpha, ms, hudModule);
            y += step * (float) module.getAnimation().getValue();
            index++;
        }
    }

    private void drawModule(float x, float y, Module module, float step, int index, 
                           float alphaPC, MatrixStack ms, HUD hudModule) {
        
        float animProgress = (float) module.getAnimation().getValue();
        if (animProgress < 0.01f) return;
        
        alphaPC *= animProgress / 2.0f + 0.5f;
        
        float gradient = Math.abs((float) index / (float) enabledModules.size());
        gradient = Math.min(Math.max(gradient, 0.0f), 1.0f);
        
        String modName = module.getName();
        float extX = 2.0f;
        float fontSize = 7.0f;
        float width = getModuleNameWidth(module, fontSize) + extX * 2.0f;
        
        int color1 = hudModule.getArrayListColor(index, step);
        int color2 = hudModule.getArrayListColor(index + 1, step);
        int colTex = ColorUtils.interpolate(color1, color2, gradient);
        
        int col1 = ColorUtils.rgba(
            ColorUtils.getRed(colTex),
            ColorUtils.getGreen(colTex),
            ColorUtils.getBlue(colTex),
            (int)(70 * alphaPC)
        );
        
        float drawX = x - width - 1.0f;
        float height = step * animProgress;
        
        RenderUtility.Render2D.drawRect(drawX, y, width, height, col1);
        
        float lineX = drawX + width;
        int lineColor = ColorUtils.setAlpha(colTex, (int)(255 * alphaPC));
        RenderUtility.Render2D.drawRect(lineX, y, 1, height, lineColor);
        
        if (255.0f * alphaPC > 26.0f) {
            float scaleText = Math.min(Math.max(animProgress * 1.005f, 0.1f), 1.0f);
            if (scaleText < 0.1f) return;
            
            int textColor = ColorUtils.setAlpha(colTex, 
                (int) Math.min(Math.max(255.0f * alphaPC, 33.0f), 255.0f));
            
            float textY = y + step / 2.0f - 3.5f - (1.0f - animProgress) * step / 3.0f;
            
            if (scaleText >= 0.99f) {
                Fonts.montserrat.drawText(ms, modName, drawX + extX, textY, textColor, fontSize);
            } else {
                GL11.glPushMatrix();
                float centerX = drawX + width / 2.0f;
                float centerY = y + step / 2.0f;
                GL11.glTranslatef(centerX, centerY, 0);
                GL11.glScalef(scaleText, scaleText, 1.0f);
                GL11.glTranslatef(-centerX, -centerY, 0);
                
                Fonts.montserrat.drawText(ms, modName, drawX + extX, textY, textColor, fontSize);
                
                GL11.glPopMatrix();
            }
        }
    }

    private void updateModuleList() {
        float fontSize = 7.0f;

        HUD hudModule = Harmony.getInstance().getModuleManager().getHud();

        enabledModules.clear();
        for (Module module : Harmony.getInstance().getModuleManager().getModules()) {
            if (!module.isState() || !module.isVisibleInArrayList()) {
                continue;
            }

            String lowerName = module.getName().toLowerCase();
            if (lowerName.contains("helper") || isExcludedName(lowerName)) {
                continue;
            }

            if (hudModule != null && !hudModule.isArrayListCategoryEnabled(module.getCategory())) {
                continue;
            }

            enabledModules.add(module);
        }

        enabledModules.sort(Comparator.comparingDouble(module -> -getModuleNameWidth(module, fontSize)));
    }

    private boolean canRenderList() {
        return !enabledModules.isEmpty() && alphaPC > 0.0f;
    }

    private boolean isExcludedName(String lowerName) {
        for (String match : EXCLUDE_MATCHES) {
            if (lowerName.contains(match)) {
                return true;
            }
        }
        return false;
    }

    private float getModuleNameWidth(Module module, float fontSize) {
        return moduleWidthCache.computeIfAbsent(module, m -> Fonts.montserrat.getWidth(m.getName(), fontSize));
    }
}
