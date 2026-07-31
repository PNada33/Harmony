package net.minecraft.client.gui.widget.list;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MainWindow;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.client.gui.AnimatedBackgroundRenderer;
import net.minecraft.client.gui.FocusableGui;
import net.minecraft.client.gui.IGuiEventListener;
import net.minecraft.client.gui.IRenderable;
import net.minecraft.client.gui.screen.MultiplayerScreen;
import net.minecraft.client.gui.screen.WorldSelectionScreen;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector4f;
import org.lwjgl.opengl.GL11;
import xd.harm.utils.render.color.ColorUtils;
import xd.harm.utils.render.rect.RenderUtility;

public abstract class AbstractList<E extends AbstractList.AbstractListEntry<E>> extends FocusableGui implements IRenderable
{
    protected final Minecraft minecraft;
    protected final int itemHeight;
    private final List<E> children = new SimpleArrayList();
    protected int width;
    protected int height;
    protected int y0;
    protected int y1;
    protected int x1;
    protected int x0;
    protected boolean centerListVertically = true;
    private double scrollAmount;
    private boolean renderSelection = true;
    private boolean renderHeader;
    protected int headerHeight;
    private boolean scrolling;
    private E selected;
    private boolean field_244603_t = true;
    private boolean field_244604_u = true;
    private float styledScrollbarHoverAnimation;
    private float styledScrollbarTravelAnimation;
    private float styledScrollbarThumbY = Float.NaN;
    private float styledScrollbarThumbHeight = Float.NaN;

    public AbstractList(Minecraft mcIn, int widthIn, int heightIn, int topIn, int bottomIn, int itemHeightIn)
    {
        this.minecraft = mcIn;
        this.width = widthIn;
        this.height = heightIn;
        this.y0 = topIn;
        this.y1 = bottomIn;
        this.itemHeight = itemHeightIn;
        this.x0 = 0;
        this.x1 = widthIn;
    }

    public void setRenderSelection(boolean value)
    {
        this.renderSelection = value;
    }

    protected void setRenderHeader(boolean value, int height)
    {
        this.renderHeader = value;
        this.headerHeight = height;

        if (!value)
        {
            this.headerHeight = 0;
        }
    }

    public int getRowWidth()
    {
        return 220;
    }

    @Nullable
    public E getSelected()
    {
        return this.selected;
    }

    public void setSelected(@Nullable E entry)
    {
        this.selected = entry;
    }

    public void func_244605_b(boolean p_244605_1_)
    {
        this.field_244603_t = p_244605_1_;
    }

    public void func_244606_c(boolean p_244606_1_)
    {
        this.field_244604_u = p_244606_1_;
    }

    @Nullable
    public E getListener()
    {
        return (E)(super.getListener());
    }

    public final List<E> getEventListeners()
    {
        return this.children;
    }

    protected final void clearEntries()
    {
        this.children.clear();
    }

    protected void replaceEntries(Collection<E> entries)
    {
        this.children.clear();
        this.children.addAll(entries);
    }

    protected E getEntry(int index)
    {
        return this.getEventListeners().get(index);
    }

    protected int addEntry(E entry)
    {
        this.children.add(entry);
        return this.children.size() - 1;
    }

    protected int getItemCount()
    {
        return this.getEventListeners().size();
    }

    protected boolean isSelectedItem(int index)
    {
        return Objects.equals(this.getSelected(), this.getEventListeners().get(index));
    }

    @Nullable
    protected final E getEntryAtPosition(double p_230933_1_, double p_230933_3_)
    {
        int i = this.getRowWidth() / 2;
        int j = this.x0 + this.width / 2;
        int k = j - i;
        int l = j + i;
        int i1 = MathHelper.floor(p_230933_3_ - (double)this.y0) - this.headerHeight + (int)this.getScrollAmount() - 4;
        int j1 = i1 / this.itemHeight;
        return (E)(p_230933_1_ < (double)this.getScrollbarPosition() && p_230933_1_ >= (double)k && p_230933_1_ <= (double)l && j1 >= 0 && i1 >= 0 && j1 < this.getItemCount() ? this.getEventListeners().get(j1) : null);
    }

    public void updateSize(int p_230940_1_, int p_230940_2_, int p_230940_3_, int p_230940_4_)
    {
        this.width = p_230940_1_;
        this.height = p_230940_2_;
        this.y0 = p_230940_3_;
        this.y1 = p_230940_4_;
        this.x0 = 0;
        this.x1 = p_230940_1_;
    }

    public void setLeftPos(int p_230959_1_)
    {
        this.x0 = p_230959_1_;
        this.x1 = p_230959_1_ + this.width;
    }

    protected int getMaxPosition()
    {
        return this.getItemCount() * this.itemHeight + this.headerHeight;
    }

    protected void clickedHeader(int p_230938_1_, int p_230938_2_)
    {
    }

    protected void renderHeader(MatrixStack p_230448_1_, int p_230448_2_, int p_230448_3_, Tessellator p_230448_4_)
    {
    }

    protected void renderBackground(MatrixStack p_230433_1_)
    {
    }

    protected void renderDecorations(MatrixStack p_230447_1_, int p_230447_2_, int p_230447_3_)
    {
    }

    public void render(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks)
    {
        this.renderBackground(matrixStack);
        int i = this.getScrollbarPosition();
        int j = i + 6;
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bufferbuilder = tessellator.getBuffer();
        boolean useMainScreenBackground = this.shouldUseMainScreenBackground();
        boolean useCenteredMainScreenBackground = this.shouldUseCenteredMainScreenBackground();
        boolean clipListEntries = this.shouldClipListEntries();
        int scrollAmount = (int)this.getScrollAmount();

        if (this.field_244603_t)
        {
            if (useMainScreenBackground)
            {
                float time = System.currentTimeMillis() / 1000.0f;
                if (useCenteredMainScreenBackground)
                {
                    this.enableListScissor();
                }
                AnimatedBackgroundRenderer.drawMainMenuStyle(this.minecraft, this.width, this.height, 1.0f, time);
                if (useCenteredMainScreenBackground)
                {
                    GL11.glDisable(GL11.GL_SCISSOR_TEST);
                }
            }
            else
            {
                this.minecraft.getTextureManager().bindTexture(AbstractGui.BACKGROUND_LOCATION);
                RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
                float f = 32.0F;
                bufferbuilder.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
                bufferbuilder.pos((double)this.x0, (double)this.y1, 0.0D).tex((float)this.x0 / 32.0F, (float)(this.y1 + scrollAmount) / 32.0F).color(32, 32, 32, 255).endVertex();
                bufferbuilder.pos((double)this.x1, (double)this.y1, 0.0D).tex((float)this.x1 / 32.0F, (float)(this.y1 + scrollAmount) / 32.0F).color(32, 32, 32, 255).endVertex();
                bufferbuilder.pos((double)this.x1, (double)this.y0, 0.0D).tex((float)this.x1 / 32.0F, (float)(this.y0 + scrollAmount) / 32.0F).color(32, 32, 32, 255).endVertex();
                bufferbuilder.pos((double)this.x0, (double)this.y0, 0.0D).tex((float)this.x0 / 32.0F, (float)(this.y0 + scrollAmount) / 32.0F).color(32, 32, 32, 255).endVertex();
                tessellator.draw();
            }
        }

        int j1 = this.getRowLeft();
        int k = this.y0 + 4 - scrollAmount;

        if (this.renderHeader)
        {
            this.renderHeader(matrixStack, j1, k, tessellator);
        }

        if (clipListEntries)
        {
            this.enableListScissor(0, 1, 0, 8);
        }

        this.renderList(matrixStack, j1, k, mouseX, mouseY, partialTicks);

        if (clipListEntries)
        {
            GL11.glDisable(GL11.GL_SCISSOR_TEST);
        }

        if (this.field_244604_u)
        {
            if (!useMainScreenBackground || useCenteredMainScreenBackground)
            {
                this.minecraft.getTextureManager().bindTexture(AbstractGui.BACKGROUND_LOCATION);
                RenderSystem.enableDepthTest();
                RenderSystem.depthFunc(519);
                float f1 = 32.0F;
                int l = -100;
                bufferbuilder.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
                bufferbuilder.pos((double)this.x0, (double)this.y0, -100.0D).tex(0.0F, (float)this.y0 / 32.0F).color(64, 64, 64, 255).endVertex();
                bufferbuilder.pos((double)(this.x0 + this.width), (double)this.y0, -100.0D).tex((float)this.width / 32.0F, (float)this.y0 / 32.0F).color(64, 64, 64, 255).endVertex();
                bufferbuilder.pos((double)(this.x0 + this.width), 0.0D, -100.0D).tex((float)this.width / 32.0F, 0.0F).color(64, 64, 64, 255).endVertex();
                bufferbuilder.pos((double)this.x0, 0.0D, -100.0D).tex(0.0F, 0.0F).color(64, 64, 64, 255).endVertex();
                bufferbuilder.pos((double)this.x0, (double)this.height, -100.0D).tex(0.0F, (float)this.height / 32.0F).color(64, 64, 64, 255).endVertex();
                bufferbuilder.pos((double)(this.x0 + this.width), (double)this.height, -100.0D).tex((float)this.width / 32.0F, (float)this.height / 32.0F).color(64, 64, 64, 255).endVertex();
                bufferbuilder.pos((double)(this.x0 + this.width), (double)this.y1, -100.0D).tex((float)this.width / 32.0F, (float)this.y1 / 32.0F).color(64, 64, 64, 255).endVertex();
                bufferbuilder.pos((double)this.x0, (double)this.y1, -100.0D).tex(0.0F, (float)this.y1 / 32.0F).color(64, 64, 64, 255).endVertex();
                tessellator.draw();
                RenderSystem.depthFunc(515);
                RenderSystem.disableDepthTest();
                RenderSystem.enableBlend();
                RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ZERO, GlStateManager.DestFactor.ONE);
                RenderSystem.disableAlphaTest();
                RenderSystem.shadeModel(7425);
                RenderSystem.disableTexture();
                int i1 = 4;
                bufferbuilder.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
                bufferbuilder.pos((double)this.x0, (double)(this.y0 + 4), 0.0D).tex(0.0F, 1.0F).color(0, 0, 0, 0).endVertex();
                bufferbuilder.pos((double)this.x1, (double)(this.y0 + 4), 0.0D).tex(1.0F, 1.0F).color(0, 0, 0, 0).endVertex();
                bufferbuilder.pos((double)this.x1, (double)this.y0, 0.0D).tex(1.0F, 0.0F).color(0, 0, 0, 255).endVertex();
                bufferbuilder.pos((double)this.x0, (double)this.y0, 0.0D).tex(0.0F, 0.0F).color(0, 0, 0, 255).endVertex();
                bufferbuilder.pos((double)this.x0, (double)this.y1, 0.0D).tex(0.0F, 1.0F).color(0, 0, 0, 255).endVertex();
                bufferbuilder.pos((double)this.x1, (double)this.y1, 0.0D).tex(1.0F, 1.0F).color(0, 0, 0, 255).endVertex();
                bufferbuilder.pos((double)this.x1, (double)(this.y1 - 4), 0.0D).tex(1.0F, 0.0F).color(0, 0, 0, 0).endVertex();
                bufferbuilder.pos((double)this.x0, (double)(this.y1 - 4), 0.0D).tex(0.0F, 0.0F).color(0, 0, 0, 0).endVertex();
                tessellator.draw();
            }
        }

        int k1 = this.getMaxScroll();

        if (k1 > 0)
        {
            int l1 = (int)((float)((this.y1 - this.y0) * (this.y1 - this.y0)) / (float)this.getMaxPosition());
            l1 = MathHelper.clamp(l1, 32, this.y1 - this.y0 - 8);
            int i2 = (int)this.getScrollAmount() * (this.y1 - this.y0 - l1) / k1 + this.y0;

            if (i2 < this.y0)
            {
                i2 = this.y0;
            }

            if (this.shouldUseStyledMultiplayerScrollbar())
            {
                this.renderStyledMultiplayerScrollbar(i, i2, l1, mouseX, mouseY, partialTicks);
            }
            else
            {
                RenderSystem.disableTexture();
                bufferbuilder.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
                bufferbuilder.pos((double)i, (double)this.y1, 0.0D).tex(0.0F, 1.0F).color(0, 0, 0, 255).endVertex();
                bufferbuilder.pos((double)j, (double)this.y1, 0.0D).tex(1.0F, 1.0F).color(0, 0, 0, 255).endVertex();
                bufferbuilder.pos((double)j, (double)this.y0, 0.0D).tex(1.0F, 0.0F).color(0, 0, 0, 255).endVertex();
                bufferbuilder.pos((double)i, (double)this.y0, 0.0D).tex(0.0F, 0.0F).color(0, 0, 0, 255).endVertex();
                bufferbuilder.pos((double)i, (double)(i2 + l1), 0.0D).tex(0.0F, 1.0F).color(128, 128, 128, 255).endVertex();
                bufferbuilder.pos((double)j, (double)(i2 + l1), 0.0D).tex(1.0F, 1.0F).color(128, 128, 128, 255).endVertex();
                bufferbuilder.pos((double)j, (double)i2, 0.0D).tex(1.0F, 0.0F).color(128, 128, 128, 255).endVertex();
                bufferbuilder.pos((double)i, (double)i2, 0.0D).tex(0.0F, 0.0F).color(128, 128, 128, 255).endVertex();
                bufferbuilder.pos((double)i, (double)(i2 + l1 - 1), 0.0D).tex(0.0F, 1.0F).color(192, 192, 192, 255).endVertex();
                bufferbuilder.pos((double)(j - 1), (double)(i2 + l1 - 1), 0.0D).tex(1.0F, 1.0F).color(192, 192, 192, 255).endVertex();
                bufferbuilder.pos((double)(j - 1), (double)i2, 0.0D).tex(1.0F, 0.0F).color(192, 192, 192, 255).endVertex();
                bufferbuilder.pos((double)i, (double)i2, 0.0D).tex(0.0F, 0.0F).color(192, 192, 192, 255).endVertex();
                tessellator.draw();
            }
        }

        this.renderDecorations(matrixStack, mouseX, mouseY);
        RenderSystem.enableTexture();
        RenderSystem.shadeModel(7424);
        RenderSystem.enableAlphaTest();
        RenderSystem.disableBlend();
    }

    private boolean shouldUseMainScreenBackground()
    {
        return this.minecraft.world == null && (this.minecraft.currentScreen instanceof MultiplayerScreen || this.minecraft.currentScreen instanceof WorldSelectionScreen);
    }

    private boolean shouldUseCenteredMainScreenBackground()
    {
        return this.minecraft.world == null && this.minecraft.currentScreen instanceof WorldSelectionScreen;
    }

    private boolean shouldUseStyledMultiplayerScrollbar()
    {
        return this.minecraft.world == null && this.minecraft.currentScreen instanceof MultiplayerScreen;
    }

    private boolean shouldClipListEntries()
    {
        return this.minecraft.world == null && this.minecraft.currentScreen instanceof MultiplayerScreen;
    }

    private void renderStyledMultiplayerScrollbar(int scrollbarX, int thumbY, int thumbHeight, int mouseX, int mouseY, float partialTicks)
    {
        float trackX = (float)scrollbarX + 0.75f;
        float trackY = (float)this.y0 + 6.5f;
        float trackWidth = 2.7f;
        float trackHeight = (float)(this.y1 - this.y0 - 13);
        float edgeInset = 0.55f;
        float targetThumbHeight = MathHelper.clamp((float)thumbHeight + 7.0f, 30.0f, Math.max(30.0f, trackHeight - edgeInset * 2.0f));
        float targetThumbY = MathHelper.clamp((float)thumbY - 2.5f, trackY + edgeInset, trackY + trackHeight - targetThumbHeight - edgeInset);
        float delta = MathHelper.clamp(partialTicks * 0.016f * 60.0f, 0.0f, 2.0f);
        boolean hovered = mouseX >= trackX - 3.0f && mouseX <= trackX + trackWidth + 3.0f && mouseY >= trackY && mouseY <= trackY + trackHeight;

        this.styledScrollbarHoverAnimation += ((hovered ? 1.0f : 0.0f) - this.styledScrollbarHoverAnimation) * 0.22f * delta;
        this.styledScrollbarHoverAnimation = MathHelper.clamp(this.styledScrollbarHoverAnimation, 0.0f, 1.0f);

        if (Float.isNaN(this.styledScrollbarThumbY))
        {
            this.styledScrollbarThumbY = targetThumbY;
        }

        if (Float.isNaN(this.styledScrollbarThumbHeight))
        {
            this.styledScrollbarThumbHeight = targetThumbHeight;
        }

        float rawTravel = Math.abs(targetThumbY - this.styledScrollbarThumbY) + Math.abs(targetThumbHeight - this.styledScrollbarThumbHeight);
        float targetTravelAnim = MathHelper.clamp(rawTravel / 12.0f, 0.0f, 1.0f);
        this.styledScrollbarTravelAnimation += (targetTravelAnim - this.styledScrollbarTravelAnimation) * 0.18f * delta;
        this.styledScrollbarTravelAnimation = MathHelper.clamp(this.styledScrollbarTravelAnimation, 0.0f, 1.0f);

        float smoothSpeed = 0.24f + this.styledScrollbarHoverAnimation * 0.08f + this.styledScrollbarTravelAnimation * 0.1f;
        this.styledScrollbarThumbY += (targetThumbY - this.styledScrollbarThumbY) * smoothSpeed * delta;
        this.styledScrollbarThumbHeight += (targetThumbHeight - this.styledScrollbarThumbHeight) * (smoothSpeed - 0.03f) * delta;

        float hover = this.styledScrollbarHoverAnimation;
        float travel = this.styledScrollbarTravelAnimation;
        float animatedTrackWidth = trackWidth + hover * 0.28f;
        float animatedTrackX = trackX - (animatedTrackWidth - trackWidth) / 2.0f;
        float thumbWidth = 2.0f + hover * 0.28f + travel * 0.14f;
        float thumbX = trackX + (trackWidth - thumbWidth) / 2.0f - hover * 0.03f;
        float thumbHeightF = MathHelper.clamp(this.styledScrollbarThumbHeight + travel * 1.2f, 30.0f, trackHeight - edgeInset * 2.0f);
        float thumbYF = MathHelper.clamp(this.styledScrollbarThumbY, trackY + edgeInset, trackY + trackHeight - thumbHeightF - edgeInset);
        float radius = animatedTrackWidth / 2.0f;
        float thumbRadius = thumbWidth / 2.0f;

        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        RenderUtility.drawRoundedRect(animatedTrackX, trackY, animatedTrackWidth, trackHeight, radius, ColorUtils.rgba(12, 15, 20, (int)(72 + hover * 10)));
        RenderUtility.drawRoundedRect(animatedTrackX + 0.32f, trackY + 1.1f, 0.34f + hover * 0.08f, Math.max(8.0f, trackHeight - 2.2f), 0.17f, ColorUtils.rgba(255, 255, 255, (int)(8 + hover * 10)));
        RenderUtility.drawRoundedRectOutline(animatedTrackX, trackY, animatedTrackWidth, trackHeight, radius, 0.35f, ColorUtils.rgba(255, 255, 255, (int)(14 + hover * 18)));

        RenderUtility.drawShadow(thumbX - 0.35f, thumbYF - 0.35f, thumbWidth + 0.7f, thumbHeightF + 0.7f, 4, ColorUtils.rgba(0, 0, 0, (int)(16 + hover * 10 + travel * 8)));
        RenderUtility.drawRoundedRect(thumbX, thumbYF, thumbWidth, thumbHeightF, thumbRadius, ColorUtils.rgba(226, 235, 250, (int)(62 + hover * 18)));
        RenderUtility.drawRoundedRectOutline(thumbX, thumbYF, thumbWidth, thumbHeightF, thumbRadius, 0.35f, ColorUtils.rgba(255, 255, 255, (int)(42 + hover * 22)));
        if (thumbHeightF > 12.0f)
        {
            float gleamHeight = Math.max(10.0f, thumbHeightF - 10.0f - hover * 2.0f);
            float gleamY = thumbYF + (thumbHeightF - gleamHeight) / 2.0f - travel * 0.25f;
            RenderUtility.drawRoundedRect(thumbX + 0.34f + hover * 0.05f, gleamY, 0.32f, gleamHeight, 0.16f, ColorUtils.rgba(255, 255, 255, (int)(20 + hover * 16)));
        }
        GlStateManager.disableBlend();
    }

    private void enableListScissor()
    {
        this.enableListScissor(0, 0, 0, 0);
    }

    private void enableListScissor(int leftInset, int topInset, int rightInset, int bottomInset)
    {
        MainWindow window = this.minecraft.getMainWindow();
        double scaleX = (double)window.getWidth() / (double)window.getScaledWidth();
        double scaleY = (double)window.getHeight() / (double)window.getScaledHeight();
        int clipX0 = this.x0 + leftInset;
        int clipY0 = this.y0 + topInset;
        int clipX1 = this.x1 - rightInset;
        int clipY1 = this.y1 - bottomInset;
        int scissorX = (int)Math.floor((double)clipX0 * scaleX);
        int scissorY = (int)Math.floor((double)(window.getScaledHeight() - clipY1) * scaleY);
        int scissorWidth = Math.max(0, (int)Math.ceil((double)(clipX1 - clipX0) * scaleX));
        int scissorHeight = Math.max(0, (int)Math.ceil((double)(clipY1 - clipY0) * scaleY));
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(scissorX, scissorY, scissorWidth, scissorHeight);
    }

    protected void centerScrollOn(E p_230951_1_)
    {
        this.setScrollAmount((double)(this.getEventListeners().indexOf(p_230951_1_) * this.itemHeight + this.itemHeight / 2 - (this.y1 - this.y0) / 2));
    }

    protected void ensureVisible(E p_230954_1_)
    {
        int i = this.getRowTop(this.getEventListeners().indexOf(p_230954_1_));
        int j = i - this.y0 - 4 - this.itemHeight;

        if (j < 0)
        {
            this.scroll(j);
        }

        int k = this.y1 - i - this.itemHeight - this.itemHeight;

        if (k < 0)
        {
            this.scroll(-k);
        }
    }

    private void scroll(int p_230937_1_)
    {
        this.setScrollAmount(this.getScrollAmount() + (double)p_230937_1_);
    }

    public double getScrollAmount()
    {
        return this.scrollAmount;
    }

    public void setScrollAmount(double p_230932_1_)
    {
        this.scrollAmount = MathHelper.clamp(p_230932_1_, 0.0D, (double)this.getMaxScroll());
    }

    public int getMaxScroll()
    {
        return Math.max(0, this.getMaxPosition() - (this.y1 - this.y0 - 4));
    }

    protected void updateScrollingState(double p_230947_1_, double p_230947_3_, int p_230947_5_)
    {
        this.scrolling = p_230947_5_ == 0 && p_230947_1_ >= (double)this.getScrollbarPosition() && p_230947_1_ < (double)(this.getScrollbarPosition() + 6);
    }

    protected int getScrollbarPosition()
    {
        return this.width / 2 + 124;
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button)
    {
        this.updateScrollingState(mouseX, mouseY, button);

        if (!this.isMouseOver(mouseX, mouseY))
        {
            return false;
        }
        else
        {
            E e = this.getEntryAtPosition(mouseX, mouseY);

            if (e != null)
            {
                if (e.mouseClicked(mouseX, mouseY, button))
                {
                    this.setListener(e);
                    this.setDragging(true);
                    return true;
                }
            }
            else if (button == 0)
            {
                this.clickedHeader((int)(mouseX - (double)(this.x0 + this.width / 2 - this.getRowWidth() / 2)), (int)(mouseY - (double)this.y0) + (int)this.getScrollAmount() - 4);
                return true;
            }

            return this.scrolling;
        }
    }

    public boolean mouseReleased(double mouseX, double mouseY, int button)
    {
        if (this.getListener() != null)
        {
            this.getListener().mouseReleased(mouseX, mouseY, button);
        }

        return false;
    }

    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY)
    {
        if (super.mouseDragged(mouseX, mouseY, button, dragX, dragY))
        {
            return true;
        }
        else if (button == 0 && this.scrolling)
        {
            if (mouseY < (double)this.y0)
            {
                this.setScrollAmount(0.0D);
            }
            else if (mouseY > (double)this.y1)
            {
                this.setScrollAmount((double)this.getMaxScroll());
            }
            else
            {
                double d0 = (double)Math.max(1, this.getMaxScroll());
                int i = this.y1 - this.y0;
                int j = MathHelper.clamp((int)((float)(i * i) / (float)this.getMaxPosition()), 32, i - 8);
                double d1 = Math.max(1.0D, d0 / (double)(i - j));
                this.setScrollAmount(this.getScrollAmount() + dragY * d1);
            }

            return true;
        }
        else
        {
            return false;
        }
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double delta)
    {
        this.setScrollAmount(this.getScrollAmount() - delta * (double)this.itemHeight / 2.0D);
        return true;
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers)
    {
        if (super.keyPressed(keyCode, scanCode, modifiers))
        {
            return true;
        }
        else if (keyCode == 264)
        {
            this.moveSelection(Ordering.DOWN);
            return true;
        }
        else if (keyCode == 265)
        {
            this.moveSelection(Ordering.UP);
            return true;
        }
        else
        {
            return false;
        }
    }

    protected void moveSelection(Ordering p_241219_1_)
    {
        this.func_241572_a_(p_241219_1_, (p_241573_0_) ->
        {
            return true;
        });
    }

    protected void func_241574_n_()
    {
        E e = this.getSelected();

        if (e != null)
        {
            this.setSelected(e);
            this.ensureVisible(e);
        }
    }

    protected void func_241572_a_(Ordering p_241572_1_, Predicate<E> p_241572_2_)
    {
        int i = p_241572_1_ == Ordering.UP ? -1 : 1;

        if (!this.getEventListeners().isEmpty())
        {
            int j = this.getEventListeners().indexOf(this.getSelected());

            while (true)
            {
                int k = MathHelper.clamp(j + i, 0, this.getItemCount() - 1);

                if (j == k)
                {
                    break;
                }

                E e = this.getEventListeners().get(k);

                if (p_241572_2_.test(e))
                {
                    this.setSelected(e);
                    this.ensureVisible(e);
                    break;
                }

                j = k;
            }
        }
    }

    public boolean isMouseOver(double mouseX, double mouseY)
    {
        return mouseY >= (double)this.y0 && mouseY <= (double)this.y1 && mouseX >= (double)this.x0 && mouseX <= (double)this.x1;
    }

    protected void renderList(MatrixStack p_238478_1_, int p_238478_2_, int p_238478_3_, int p_238478_4_, int p_238478_5_, float p_238478_6_)
    {
        int i = this.getItemCount();
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bufferbuilder = tessellator.getBuffer();
        int rowWidth = this.getRowWidth();
        int rowLeft = this.getRowLeft();
        int renderBaseRowTop = p_238478_3_ + this.headerHeight;
        boolean mouseOverList = this.isMouseOver((double)p_238478_4_, (double)p_238478_5_);
        E hoveredEntry = mouseOverList ? this.getEntryAtPosition((double)p_238478_4_, (double)p_238478_5_) : null;

        for (int j = 0; j < i; ++j)
        {
            int k = this.getRowTop(j);
            int l = k + this.itemHeight;

            if (l >= this.y0 && k <= this.y1)
            {
                int i1 = renderBaseRowTop + j * this.itemHeight;
                int j1 = this.itemHeight - 4;
                E e = this.getEntry(j);
                int k1 = rowWidth;

                if (this.renderSelection && this.isSelectedItem(j))
                {
                    int l1 = this.x0 + this.width / 2 - k1 / 2;
                    int i2 = this.x0 + this.width / 2 + k1 / 2;
                    RenderSystem.disableTexture();
                    float f = this.isFocused() ? 1.0F : 0.5F;
                    RenderSystem.color4f(f, f, f, 1.0F);
                    bufferbuilder.begin(7, DefaultVertexFormats.POSITION);
                    bufferbuilder.pos((double)l1, (double)(i1 + j1 + 2), 0.0D).endVertex();
                    bufferbuilder.pos((double)i2, (double)(i1 + j1 + 2), 0.0D).endVertex();
                    bufferbuilder.pos((double)i2, (double)(i1 - 2), 0.0D).endVertex();
                    bufferbuilder.pos((double)l1, (double)(i1 - 2), 0.0D).endVertex();
                    tessellator.draw();
                    RenderSystem.color4f(0.0F, 0.0F, 0.0F, 1.0F);
                    bufferbuilder.begin(7, DefaultVertexFormats.POSITION);
                    bufferbuilder.pos((double)(l1 + 1), (double)(i1 + j1 + 1), 0.0D).endVertex();
                    bufferbuilder.pos((double)(i2 - 1), (double)(i1 + j1 + 1), 0.0D).endVertex();
                    bufferbuilder.pos((double)(i2 - 1), (double)(i1 - 1), 0.0D).endVertex();
                    bufferbuilder.pos((double)(l1 + 1), (double)(i1 - 1), 0.0D).endVertex();
                    tessellator.draw();
                    RenderSystem.enableTexture();
                }

                e.render(p_238478_1_, j, k, rowLeft, k1, j1, p_238478_4_, p_238478_5_, Objects.equals(hoveredEntry, e), p_238478_6_);
            }
        }
    }

    public int getRowLeft()
    {
        return this.x0 + this.width / 2 - this.getRowWidth() / 2 + 2;
    }

    public int func_244736_r()
    {
        return this.getRowLeft() + this.getRowWidth();
    }

    protected int getRowTop(int p_230962_1_)
    {
        return this.y0 + 4 - (int)this.getScrollAmount() + p_230962_1_ * this.itemHeight + this.headerHeight;
    }

    private int getRowBottom(int p_230948_1_)
    {
        return this.getRowTop(p_230948_1_) + this.itemHeight;
    }

    protected boolean isFocused()
    {
        return false;
    }

    protected E remove(int p_230964_1_)
    {
        E e = this.children.get(p_230964_1_);
        return (E)(this.removeEntry(this.children.get(p_230964_1_)) ? e : null);
    }

    protected boolean removeEntry(E p_230956_1_)
    {
        boolean flag = this.children.remove(p_230956_1_);

        if (flag && p_230956_1_ == this.getSelected())
        {
            this.setSelected((E)null);
        }

        return flag;
    }

    private void func_238480_f_(AbstractListEntry<E> p_238480_1_)
    {
        p_238480_1_.list = this;
    }

    public abstract static class AbstractListEntry<E extends AbstractListEntry<E>> implements IGuiEventListener
    {
        @Deprecated
        private AbstractList<E> list;

        public abstract void render(MatrixStack p_230432_1_, int p_230432_2_, int p_230432_3_, int p_230432_4_, int p_230432_5_, int p_230432_6_, int p_230432_7_, int p_230432_8_, boolean p_230432_9_, float p_230432_10_);

        public boolean isMouseOver(double mouseX, double mouseY)
        {
            return Objects.equals(this.list.getEntryAtPosition(mouseX, mouseY), this);
        }
    }

    public static enum Ordering
    {
        UP,
        DOWN;
    }

    class SimpleArrayList extends java.util.AbstractList<E>
    {
        private final List<E> field_216871_b = Lists.newArrayList();

        private SimpleArrayList()
        {
        }

        public E get(int p_get_1_)
        {
            return this.field_216871_b.get(p_get_1_);
        }

        public int size()
        {
            return this.field_216871_b.size();
        }

        public E set(int p_set_1_, E p_set_2_)
        {
            E e = this.field_216871_b.set(p_set_1_, p_set_2_);
            AbstractList.this.func_238480_f_(p_set_2_);
            return e;
        }

        public void add(int p_add_1_, E p_add_2_)
        {
            this.field_216871_b.add(p_add_1_, p_add_2_);
            AbstractList.this.func_238480_f_(p_add_2_);
        }

        public E remove(int p_remove_1_)
        {
            return this.field_216871_b.remove(p_remove_1_);
        }
    }
}
