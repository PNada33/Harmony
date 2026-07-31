package xd.harm.ui.clickgui.components.settings;

import com.mojang.blaze3d.matrix.MatrixStack;
import xd.harm.modules.settings.impl.CategorySetting;
import xd.harm.ui.clickgui.components.builder.Component;
import xd.harm.utils.SoundUtil;
import xd.harm.utils.math.AnimationMath;
import xd.harm.utils.render.color.ColorUtils;
import xd.harm.utils.render.font.Fonts;
import xd.harm.utils.render.rect.RenderUtility;

import java.util.Locale;

public class CategoryComponent extends Component {

    private final CategorySetting setting;

    private boolean collapsed = false;
    // Анимация сворачивания: 0 = полностью развёрнуто, 1 = полностью свёрнуто
    private float collapseAnim = 0.0f;

    private static final float TITLE_SIZE = 7f;
    private static final float SIDE_GAP = 6f;
    private static final float ANIM_SPEED = 15.0f;

    public CategoryComponent(CategorySetting setting) {
        this.setting = setting;
        setHeight(14f);
    }

    public boolean isCollapsed() {
        return collapsed;
    }

    public CategorySetting getSetting() {
        return setting;
    }

    public void setCollapsed(boolean collapsed) {
        this.collapsed = collapsed;
        // При восстановлении состояния сразу выставляем анимацию в целевое значение
        this.collapseAnim = collapsed ? 1.0f : 0.0f;
    }

    /**
     * Возвращает текущее значение анимации сворачивания (0..1).
     * 0 = развёрнуто, 1 = свёрнуто. Используется для плавного скрытия дочерних компонентов.
     */
    public float getCollapseAnim() {
        return collapseAnim;
    }

    /**
     * Обновляет анимацию сворачивания. Должно вызываться каждый кадр рендера.
     */
    public void updateAnim() {
        float target = collapsed ? 1.0f : 0.0f;
        collapseAnim = AnimationMath.fast(collapseAnim, target, ANIM_SPEED);
    }

    @Override
    public void mouseClick(float mouseX, float mouseY, int button) {
        // Клик по заголовку категории — сворачиваем/разворачиваем
        if (button == 0 && isHovered(mouseX, mouseY)) {
            collapsed = !collapsed;
            SoundUtil.playSound(collapsed ? "closemodescreen.wav" : "openmodescreen.wav");
        }
        super.mouseClick(mouseX, mouseY, button);
    }

    @Override
    public void render(MatrixStack stack, float mouseX, float mouseY) {
        super.render(stack, mouseX, mouseY);
        updateAnim();

        String title = setting.getName();
        if (title == null || title.isEmpty()) {
            return;
        }

        title = title.toUpperCase(Locale.ROOT);

        float centerX = getX() + getWidth() / 2f;
        float textY = getY() + 3f;
        float textWidth = Fonts.sfbold.getWidth(title, TITLE_SIZE);

        // Текст заголовка: при сворачивании — тусклее (плавно по анимации)
        int textAlpha = (int) (235 - 105 * collapseAnim);
        Fonts.sfbold.drawCenteredText(
                stack,
                title,
                centerX,
                textY,
                ColorUtils.rgba(210, 210, 210, textAlpha),
                TITLE_SIZE
        );

        float lineY = getY() + getHeight() / 2f + 1.5f;
        float leftStart = getX() + 4f;
        float leftEnd = centerX - textWidth / 2f - SIDE_GAP;
        float rightStart = centerX + textWidth / 2f + SIDE_GAP;
        float rightEnd = getX() + getWidth() - 4f;

        // Линии: при сворачивании — прозрачнее (плавно по анимации)
        int lineAlpha = (int) (55 - 30 * collapseAnim);

        if (leftEnd - leftStart > 2f) {
            RenderUtility.drawRoundedRect(
                    leftStart,
                    lineY,
                    leftEnd - leftStart,
                    1f,
                    0.5f,
                    ColorUtils.rgba(255, 255, 255, lineAlpha)
            );
        }

        if (rightEnd - rightStart > 2f) {
            RenderUtility.drawRoundedRect(
                    rightStart,
                    lineY,
                    rightEnd - rightStart,
                    1f,
                    0.5f,
                    ColorUtils.rgba(255, 255, 255, lineAlpha)
            );
        }
    }

    @Override
    public boolean isVisible() {
        return setting.visible.get();
    }
}
