        package xd.harm.ui.mainmenu;

        import xd.harm.Harmony;
        import xd.harm.config.AltConfig;
        import xd.harm.modules.impl.render.Theme;
        import xd.harm.utils.animations.Animation;
        import xd.harm.utils.animations.Direction;
        import xd.harm.utils.animations.impl.DecelerateAnimation;
        import xd.harm.utils.animations.impl.ElasticAnimation;
        import xd.harm.utils.client.Vec2i;
        import xd.harm.utils.math.MathUtil;
        import xd.harm.utils.math.Vector4i;
        import xd.harm.utils.render.color.ColorUtils;
        import xd.harm.utils.render.font.Fonts;
        import xd.harm.utils.render.gl.Scissor;
        import xd.harm.utils.render.rect.RenderUtility;
        import xd.harm.utils.text.font.ClientFonts;
        import com.google.common.collect.Lists;
        import com.google.gson.Gson;
        import com.google.gson.JsonObject;
        import com.mojang.authlib.GameProfile;
        import com.mojang.authlib.minecraft.MinecraftProfileTexture;
        import com.mojang.blaze3d.matrix.MatrixStack;
        import net.minecraft.client.Minecraft;
        import net.minecraft.client.gui.AnimatedBackgroundRenderer;
        import net.minecraft.client.gui.IGuiEventListener;
        import net.minecraft.client.gui.screen.Screen;
        import net.minecraft.client.gui.widget.TextFieldWidget;
        import net.minecraft.client.gui.widget.button.Button;
        import net.minecraft.client.resources.DefaultPlayerSkin;
        import net.minecraft.util.ResourceLocation;
        import net.minecraft.util.Session;
        import net.minecraft.util.math.MathHelper;
        import net.minecraft.util.math.vector.Vector4f;
        import net.minecraft.util.text.ITextComponent;
        import net.minecraft.util.text.StringTextComponent;
        import org.lwjgl.glfw.GLFW;
        import org.lwjgl.opengl.GL11;
        import ru.hogoshi.util.Easings;

        import java.io.InputStreamReader;
        import java.net.URL;
        import java.net.URLConnection;
        import java.nio.charset.StandardCharsets;
        import java.util.ArrayList;
        import java.util.ConcurrentModificationException;
        import java.util.List;
        import java.util.UUID;

        import static xd.harm.utils.client.IMinecraft.mc;

        public class AltScreen extends Screen {
            private float guiScale;
            private float screenWidth;
            private float screenHeight;
            private long screenOpenedAt = System.currentTimeMillis();

            private static final float BASE_CARD_WIDTH = 140;
            private static final float BASE_CARD_HEIGHT = 55;
            private static final float BASE_LEFT_PANEL_WIDTH = 200;
            private static final float BASE_RIGHT_PANEL_WIDTH = 350;
            private static final float BASE_PANEL_HEIGHT = 300;

            private final ActionButtonState addButtonState = new ActionButtonState();
            private final ActionButtonState generateButtonState = new ActionButtonState();
            private final ActionButtonState clearAllButtonState = new ActionButtonState();
            private final AltLayout cachedLayout = new AltLayout();
            private final List<Account> filteredAccountsCache = new ArrayList<>();
            private String cachedFilterSearch = null;
            private int cachedFilterAccountCount = -1;
            private boolean accountFilterDirty = true;

            private float searchOutlineAnimation = 0f;
            private float newAccountOutlineAnimation = 0f;

            private static final float ADD_ICON_OFFSET_X = 0f;
            private static final float ADD_ICON_OFFSET_Y = 0f;

            private static final float GENERATE_ICON_OFFSET_X = - 5f;
            private static final float GENERATE_ICON_OFFSET_Y = 2.5f;

            private static final float CLEAR_ICON_OFFSET_X = 0f;
            private static final float CLEAR_ICON_OFFSET_Y = 0f;
            private static final int COPY_ICON_COLOR = ColorUtils.rgba(200, 180, 60, 255);
            private static final int INACTIVE_ICON_COLOR = ColorUtils.rgba(80, 80, 80, 255);
            private static final int DELETE_ICON_COLOR = ColorUtils.rgba(200, 60, 60, 255);

            private boolean isClosing = false;
            private float closeAnimation = 0f;
            private long closeStartTime = 0;
            private static final long CLOSE_ANIMATION_DURATION = 260;
            private Screen targetScreen = null;

            private float openAnimation = 0f;
            private long openStartTime = 0;
            private static final long OPEN_ANIMATION_DURATION = 260;
            private boolean isOpening = true;
            private boolean initialized = false;

            public AltScreen() {
                super(new StringTextComponent(""));
                updateAccountClasses();
                this.currentUsername = mc.getSession().getUsername();
                this.previousUsername = this.currentUsername;
            }

            public final List<Account> accounts = new ArrayList<>();
            public static final ArrayList<Account> accountsToDelete = new ArrayList<>();
            protected final List<IGuiEventListener> children = Lists.newArrayList();

            public float scroll;
            public float scrollAn;

            private String searchText = "";
            private boolean searchFocused = false;
            private String newAccountText = "";
            private boolean newAccountFocused = false;

            private String currentUsername;
            private String previousUsername;
            private float nameChangeAnimation = 1f;
            private long nameChangeTime = 0;
            private boolean isNameChanging = false;

            public void updateAccountClasses() {
            }

            private void calculateGuiScale() {
                screenWidth = mc.getMainWindow().getScaledWidth();
                screenHeight = mc.getMainWindow().getScaledHeight();

                float base = Math.min(screenWidth / 1280f, screenHeight / 720f);
                guiScale = MathHelper.clamp(base, 0.58f, 1.15f);
                if (screenWidth > 1920 || screenHeight > 1080) {
                    guiScale = MathHelper.clamp(guiScale * 1.08f, 0.58f, 1.35f);
                }
            }

            private float scale(float value) {
                return value * guiScale;
            }

            private int alphaColor(int color, float alpha) {
                return ColorUtils.setAlpha(color, MathHelper.clamp((int) (ColorUtils.getAlpha(color) * alpha), 0, 255));
            }

            private int scaleInt(float value) {
                return Math.round(value * guiScale);
            }

            private int scaleFontSize(int baseSize) {
                int scaled = Math.round(baseSize * guiScale);
                return Math.max(scaled, baseSize >= 12 ? 10 : 8);
            }

            private static class AltLayout {
                float leftX;
                float leftY;
                float leftWidth;
                float leftHeight;
                float rightX;
                float rightY;
                float rightWidth;
                float rightHeight;
                float panelPadding;
                float fieldHeight;
                float fieldGap;
                float actionButtonHeight;
                float buttonGap;
            }

            private static class ActionButtonState {
                float hover;
                float scale = 1f;
            }

            private AltLayout createLayout() {
                AltLayout layout = cachedLayout;
                float outerMargin = scale(8);

                layout.panelPadding = scale(10);
                layout.fieldHeight = scale(22);
                layout.fieldGap = scale(10);
                layout.actionButtonHeight = scale(26);
                layout.buttonGap = scale(8);

                float leftWidth = scale(BASE_LEFT_PANEL_WIDTH);
                float rightWidth = scale(BASE_RIGHT_PANEL_WIDTH);
                float panelHeight = scale(BASE_PANEL_HEIGHT);
                float panelGap = scale(15);

                float availableWidth = Math.max(120f, screenWidth - outerMargin * 2f);
                float availableHeight = Math.max(120f, screenHeight - outerMargin * 2f);

                float totalWidth = leftWidth + rightWidth + panelGap;
                boolean stacked = totalWidth > availableWidth * 0.98f;

                if (stacked) {
                    float panelWidth = Math.min(availableWidth, Math.max(leftWidth, rightWidth));
                    float verticalGap = scale(10);
                    float totalHeight = panelHeight * 2f + verticalGap;

                    if (totalHeight > availableHeight) {
                        float fit = availableHeight / totalHeight;
                        panelHeight *= fit;
                        layout.panelPadding = Math.max(5f, layout.panelPadding * fit);
                        layout.fieldHeight = Math.max(14f, layout.fieldHeight * fit);
                        layout.fieldGap = Math.max(5f, layout.fieldGap * fit);
                        layout.actionButtonHeight = Math.max(16f, layout.actionButtonHeight * fit);
                        layout.buttonGap = Math.max(4f, layout.buttonGap * fit);
                        verticalGap = Math.max(4f, verticalGap * fit);
                    }

                    layout.leftWidth = panelWidth;
                    layout.rightWidth = panelWidth;
                    layout.leftHeight = panelHeight;
                    layout.rightHeight = panelHeight;
                    layout.leftX = (screenWidth - panelWidth) / 2f;
                    layout.leftY = (screenHeight - (panelHeight * 2f + verticalGap)) / 2f;
                    layout.rightX = layout.leftX;
                    layout.rightY = layout.leftY + panelHeight + verticalGap;
                } else {
                    if (totalWidth > availableWidth) {
                        float fit = availableWidth / totalWidth;
                        leftWidth *= fit;
                        rightWidth *= fit;
                        panelGap *= fit;
                        layout.panelPadding = Math.max(5f, layout.panelPadding * fit);
                        layout.fieldHeight = Math.max(14f, layout.fieldHeight * fit);
                        layout.fieldGap = Math.max(5f, layout.fieldGap * fit);
                        layout.actionButtonHeight = Math.max(16f, layout.actionButtonHeight * fit);
                        layout.buttonGap = Math.max(4f, layout.buttonGap * fit);
                    }

                    if (panelHeight > availableHeight) {
                        float fitH = availableHeight / panelHeight;
                        panelHeight *= fitH;
                        layout.panelPadding = Math.max(5f, layout.panelPadding * fitH);
                        layout.fieldHeight = Math.max(14f, layout.fieldHeight * fitH);
                        layout.fieldGap = Math.max(5f, layout.fieldGap * fitH);
                        layout.actionButtonHeight = Math.max(16f, layout.actionButtonHeight * fitH);
                        layout.buttonGap = Math.max(4f, layout.buttonGap * fitH);
                    }

                    layout.leftWidth = leftWidth;
                    layout.rightWidth = rightWidth;
                    layout.leftHeight = panelHeight;
                    layout.rightHeight = panelHeight;

                    float contentWidth = leftWidth + panelGap + rightWidth;
                    layout.leftX = (screenWidth - contentWidth) / 2f;
                    layout.rightX = layout.leftX + leftWidth + panelGap;
                    float panelY = (screenHeight - panelHeight) / 2f;
                    layout.leftY = panelY;
                    layout.rightY = panelY;
                }

                layout.leftX = MathHelper.clamp(layout.leftX, outerMargin, Math.max(outerMargin, screenWidth - layout.leftWidth - outerMargin));
                layout.leftY = MathHelper.clamp(layout.leftY, outerMargin, Math.max(outerMargin, screenHeight - layout.leftHeight - outerMargin));
                layout.rightX = MathHelper.clamp(layout.rightX, outerMargin, Math.max(outerMargin, screenWidth - layout.rightWidth - outerMargin));
                layout.rightY = MathHelper.clamp(layout.rightY, outerMargin, Math.max(outerMargin, screenHeight - layout.rightHeight - outerMargin));

                return layout;
            }

            private float getSearchFieldY(AltLayout layout) {
                return layout.leftY + layout.panelPadding;
            }

            private float getNewAccountFieldY(AltLayout layout) {
                return getSearchFieldY(layout) + layout.fieldHeight + layout.fieldGap;
            }

            private float getAddButtonY(AltLayout layout) {
                return getNewAccountFieldY(layout) + layout.fieldHeight + layout.fieldGap;
            }

            private float getGenerateButtonY(AltLayout layout) {
                return getAddButtonY(layout) + layout.actionButtonHeight + layout.buttonGap;
            }

            private float getClearAllButtonY(AltLayout layout) {
                float minClearY = getGenerateButtonY(layout) + layout.actionButtonHeight + layout.buttonGap;
                float bottomClearY = layout.leftY + layout.leftHeight - layout.panelPadding - layout.actionButtonHeight;
                float clearY = Math.max(minClearY, bottomClearY);
                float maxClearY = layout.leftY + layout.leftHeight - layout.panelPadding - layout.actionButtonHeight;
                return MathHelper.clamp(clearY, layout.leftY + layout.panelPadding, maxClearY);
            }

            private void resetAnimations() {
                isClosing = false;
                closeAnimation = 0f;
                closeStartTime = 0;
                targetScreen = null;
                isOpening = true;
                openAnimation = 0f;
                openStartTime = System.currentTimeMillis();
                screenOpenedAt = openStartTime;
                initialized = true;
            }

            @Override
            protected void init() {
                updateAccountClasses();
                calculateGuiScale();
                resetAnimations();
                super.init();
            }

            @Override
            public boolean mouseReleased(double mouseX, double mouseY, int button) {
                return super.mouseReleased(mouseX, mouseY, button);
            }

            @Override
            public void closeScreen() {
                if (!isClosing) {
                    startCloseAnimation(null);
                }
            }

            private void startCloseAnimation(Screen target) {
                if (!isClosing) {
                    isClosing = true;
                    closeStartTime = System.currentTimeMillis();
                    closeAnimation = 0f;
                    targetScreen = target;
                }
            }

            private void finishClose() {
                isClosing = false;
                closeAnimation = 0f;
                mc.displayGuiScreen(targetScreen);
            }

            private float easeOutBack(float x) {
                float c1 = 1.70158f;
                float c3 = c1 + 1;
                return (float) (1 + c3 * Math.pow(x - 1, 3) + c1 * Math.pow(x - 1, 2));
            }

            private float easeInBack(float x) {
                float c1 = 1.70158f;
                float c3 = c1 + 1;
                return c3 * x * x * x - c1 * x * x;
            }

            private float easeOutCubic(float x) {
                return (float) (1 - Math.pow(1 - x, 3));
            }

            private float easeInCubic(float x) {
                return x * x * x;
            }

            private float easeInOutCubic(float x) {
                return x < 0.5f ? 4f * x * x * x : 1f - (float) Math.pow(-2f * x + 2f, 3) / 2f;
            }

            private float easeOutElastic(float x) {
                if (x == 0) return 0;
                if (x == 1) return 1;
                float c4 = (float) ((2 * Math.PI) / 3);
                return (float) (Math.pow(2, -10 * x) * Math.sin((x * 10 - 0.75) * c4) + 1);
            }

            @Override
            public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
                if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                    if (searchFocused) {
                        searchFocused = false;
                        return true;
                    }
                    if (newAccountFocused) {
                        newAccountFocused = false;
                        return true;
                    }
                    if (!isClosing) {
                        startCloseAnimation(null);
                    }
                    return true;
                }

                if (isOpening || isClosing) return false;

                if (searchFocused) {
                    if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
                        if (!searchText.isEmpty()) {
                            searchText = searchText.substring(0, searchText.length() - 1);
                        }
                        return true;
                    }
                    if (keyCode == GLFW.GLFW_KEY_ENTER) {
                        searchFocused = false;
                        return true;
                    }
                    boolean isControlPressed = (modifiers & GLFW.GLFW_MOD_CONTROL) != 0;
                    if (keyCode == GLFW.GLFW_KEY_V && isControlPressed) {
                        searchText += mc.keyboardListener.getClipboardString();
                        return true;
                    }
                }
                if (newAccountFocused) {
                    if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
                        if (!newAccountText.isEmpty()) {
                            newAccountText = newAccountText.substring(0, newAccountText.length() - 1);
                        }
                        return true;
                    }
                    if (keyCode == GLFW.GLFW_KEY_ENTER) {
                        if (!newAccountText.trim().isEmpty()) {
                            if (tryAddAccount(newAccountText)) {
                                newAccountText = "";
                                newAccountFocused = false;
                            }
                        }
                        return true;
                    }
                    boolean isControlPressed = (modifiers & GLFW.GLFW_MOD_CONTROL) != 0;
                    if (keyCode == GLFW.GLFW_KEY_V && isControlPressed) {
                        newAccountText += mc.keyboardListener.getClipboardString();
                        return true;
                    }
                }
                return super.keyPressed(keyCode, scanCode, modifiers);
            }

            @Override
            public boolean charTyped(char codePoint, int modifiers) {
                if (isOpening || isClosing) return false;
                if (searchFocused) {
                    searchText += codePoint;
                    return true;
                }
                if (newAccountFocused) {
                    newAccountText += codePoint;
                    return true;
                }
                return super.charTyped(codePoint, modifiers);
            }

            @Override
            public boolean mouseClicked(double mouseX, double mouseY, int button) {
                if (isOpening || isClosing) return false;

                Vec2i fixed = MathUtil.getMouse2i((int) mouseX, (int) mouseY);
                mouseX = fixed.getX();
                mouseY = fixed.getY();

                calculateGuiScale();
                AltLayout layout = createLayout();
                float leftPanelX = layout.leftX;
                float leftPanelWidth = layout.leftWidth;
                float fieldX = leftPanelX + layout.panelPadding;
                float fieldWidth = leftPanelWidth - layout.panelPadding * 2f;

                float searchFieldY = getSearchFieldY(layout);
                float searchFieldHeight = layout.fieldHeight;

                if (isInRegion(mouseX, mouseY, fieldX, searchFieldY, fieldWidth, searchFieldHeight)) {
                    searchFocused = true;
                    newAccountFocused = false;
                } else {
                    searchFocused = false;
                }

                float newAccountFieldY = getNewAccountFieldY(layout);
                if (isInRegion(mouseX, mouseY, fieldX, newAccountFieldY, fieldWidth, searchFieldHeight)) {
                    newAccountFocused = true;
                    searchFocused = false;
                } else if (!searchFocused) {
                    newAccountFocused = false;
                }

                float addButtonY = getAddButtonY(layout);
                float generateButtonY = getGenerateButtonY(layout);
                float clearAllButtonY = getClearAllButtonY(layout);
                float actionButtonHeight = layout.actionButtonHeight;

                if (isInRegion(mouseX, mouseY, fieldX, addButtonY, fieldWidth, actionButtonHeight)) {
                    if (!newAccountText.trim().isEmpty()) {
                        if (tryAddAccount(newAccountText)) {
                            newAccountText = "";
                            newAccountFocused = false;
                        }
                    }
                }

                if (isInRegion(mouseX, mouseY, fieldX, generateButtonY, fieldWidth, actionButtonHeight)) {
                    tryAddGeneratedAccount();
                }

                if (isInRegion(mouseX, mouseY, fieldX, clearAllButtonY, fieldWidth, actionButtonHeight)) {
                    accounts.clear();
                    markAccountFilterDirty();
                    new Thread(AltConfig::updateFile).start();
                }

                for (int i = 0; i < accounts.size(); i++) {
                    accounts.get(i).onClick(mouseX, mouseY, button);
                }
                if (!accountsToDelete.isEmpty()) {
                    accounts.removeAll(accountsToDelete);
                    accountsToDelete.clear();
                    markAccountFilterDirty();
                }
                return super.mouseClicked(mouseX, mouseY, button);
            }

            private boolean isInRegion(double mouseX, double mouseY, float x, float y, float width, float height) {
                return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
            }

            private boolean hasAccount(String name) {
                String normalized = name == null ? "" : name.trim();
                if (normalized.isEmpty()) {
                    return false;
                }

                for (Account account : accounts) {
                    if (account.name != null && account.name.equalsIgnoreCase(normalized)) {
                        return true;
                    }
                }
                return false;
            }

            private void markAccountFilterDirty() {
                accountFilterDirty = true;
            }

            private boolean tryAddAccount(String name) {
                String normalized = name == null ? "" : name.trim();
                if (normalized.isEmpty() || hasAccount(normalized)) {
                    return false;
                }

                accounts.add(new Account(normalized, System.currentTimeMillis()));
                markAccountFilterDirty();
                new Thread(AltConfig::updateFile).start();
                return true;
            }

            private boolean tryAddGeneratedAccount() {
                for (int attempt = 0; attempt < 20; attempt++) {
                    String nickname = Harmony.getInstance().randomNickname();
                    if (tryAddAccount(nickname)) {
                        return true;
                    }
                }
                return false;
            }

            private Vector4i quadColors(int x, int y, int z, int w) {
                try {
                    return Vector4i.class.getDeclaredConstructor(int.class, int.class, int.class, int.class).newInstance(x, y, z, w);
                } catch (Exception ignored) {
                    try {
                        Vector4i colors = Vector4i.class.getDeclaredConstructor().newInstance();
                        colors.x = x;
                        colors.y = y;
                        colors.z = z;
                        colors.w = w;
                        return colors;
                    } catch (Exception exception) {
                        throw new IllegalStateException("Cannot create Vector4i", exception);
                    }
                }
            }

            @Override
            public void tick() {
                super.tick();

                String sessionName = mc.getSession().getUsername();
                if (!sessionName.equals(currentUsername)) {
                    previousUsername = currentUsername;
                    currentUsername = sessionName;
                    nameChangeTime = System.currentTimeMillis();
                    isNameChanging = true;
                    nameChangeAnimation = 0;
                }
            }

            @Override
            public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
                if (isOpening || isClosing) return false;
                scroll += (float) (delta * scale(30));
                return super.mouseScrolled(mouseX, mouseY, delta);
            }

            private List<Account> getFilteredAccounts() {
                String search = searchText.trim();
                if (!accountFilterDirty
                        && accounts.size() == cachedFilterAccountCount
                        && search.equals(cachedFilterSearch)) {
                    return search.isEmpty() ? accounts : filteredAccountsCache;
                }

                accountFilterDirty = false;
                cachedFilterSearch = search;
                cachedFilterAccountCount = accounts.size();
                filteredAccountsCache.clear();

                if (search.isEmpty()) {
                    return accounts;
                }

                for (Account account : accounts) {
                    if (containsIgnoreCase(account.name, search)) {
                        filteredAccountsCache.add(account);
                    }
                }
                return filteredAccountsCache;
            }

            private boolean containsIgnoreCase(String value, String search) {
                if (value == null || search == null) {
                    return false;
                }

                int searchLength = search.length();
                int max = value.length() - searchLength;
                if (searchLength == 0) {
                    return true;
                }
                if (max < 0) {
                    return false;
                }

                for (int i = 0; i <= max; i++) {
                    if (value.regionMatches(true, i, search, 0, searchLength)) {
                        return true;
                    }
                }
                return false;
            }

            private void renderThemeGlows(MatrixStack matrixStack, float alpha) {
                float time = (System.currentTimeMillis() - screenOpenedAt) / 1000.0f;
                AnimatedBackgroundRenderer.drawMainMenuStyle(mc, Math.round(screenWidth), Math.round(screenHeight), 1.0f, time);
            }

            @Override
            public void render(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
                calculateGuiScale();

                if (isOpening) {
                    long elapsed = System.currentTimeMillis() - openStartTime;
                    float progress = Math.min(1f, (float) elapsed / OPEN_ANIMATION_DURATION);
                    openAnimation = easeInOutCubic(progress);
                    if (progress >= 1f) {
                        isOpening = false;
                        openAnimation = 1f;
                    }
                }

                if (isClosing) {
                    long elapsed = System.currentTimeMillis() - closeStartTime;
                    float progress = Math.min(1f, (float) elapsed / CLOSE_ANIMATION_DURATION);
                    closeAnimation = easeInOutCubic(progress);
                    if (progress >= 1f) {
                        finishClose();
                        return;
                    }
                }

                float animationAlpha = isClosing ? (1f - closeAnimation) : openAnimation;

                scrollAn = MathUtil.lerp(scrollAn, scroll, 5.5f);
                AltLayout layout = createLayout();

                GL11.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
                GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);

                renderThemeGlows(matrixStack, animationAlpha);

                GL11.glPushMatrix();

                float leftPanelWidth = layout.leftWidth;
                float leftPanelHeight = layout.leftHeight;
                float leftPanelX = layout.leftX;
                float leftPanelY = layout.leftY;

                int themeColor = Theme.MainColor(0);

                int leftPanelAlpha = (int) (150 * animationAlpha);
                int leftPanelColor = ColorUtils.rgba(10, 10, 10, leftPanelAlpha);

                RenderUtility.drawRoundedRect(leftPanelX, leftPanelY, leftPanelWidth, leftPanelHeight,
                        scale(8), leftPanelColor);

                float searchFieldY = getSearchFieldY(layout);
                float searchFieldHeight = layout.fieldHeight;
                float fieldRadius = Math.max(3f, scale(5));
                float fieldWidth = leftPanelWidth - layout.panelPadding * 2f;
                float fieldX = leftPanelX + layout.panelPadding;

                searchOutlineAnimation = MathUtil.lerp(searchOutlineAnimation, searchFocused ? 1f : 0f, 8f);

                int searchBgAlpha = (int) (220 * animationAlpha);
                int searchBgColor = ColorUtils.rgba(15, 15, 18, searchBgAlpha);

                if (searchOutlineAnimation > 0.01f) {
                    int glowAlpha = (int) (searchOutlineAnimation * 35 * animationAlpha);
                    float glowRadius = fieldRadius + 2;

                    RenderUtility.drawRoundedRect(fieldX - 2, searchFieldY - 2, fieldWidth + 4, searchFieldHeight + 4, glowRadius,
                            ColorUtils.setAlpha(themeColor, (int) (glowAlpha * 0.3f)));
                    RenderUtility.drawRoundedRect(fieldX - 1.5f, searchFieldY - 1.5f, fieldWidth + 3, searchFieldHeight + 3, glowRadius,
                            ColorUtils.setAlpha(themeColor, (int) (glowAlpha * 0.5f)));
                    RenderUtility.drawRoundedRect(fieldX - 1, searchFieldY - 1, fieldWidth + 2, searchFieldHeight + 2, fieldRadius + 1,
                            ColorUtils.setAlpha(themeColor, glowAlpha));

                    int outlineAlpha = (int) (searchOutlineAnimation * 220 * animationAlpha);

                    RenderUtility.drawRoundedRect(fieldX - 0.5f, searchFieldY - 0.5f, fieldWidth + 1, searchFieldHeight + 1, fieldRadius + 0.5f,
                            ColorUtils.setAlpha(themeColor, outlineAlpha));
                }

                RenderUtility.drawRoundedRect(fieldX, searchFieldY, fieldWidth, searchFieldHeight,
                        fieldRadius, searchBgColor);

                int iconAlpha = (int) (255 * animationAlpha);
                int textAlpha = (int) (255 * animationAlpha);
                int placeholderAlpha = (int) (180 * animationAlpha);

                ClientFonts.AltManager[18].drawString(matrixStack, "B",
                        fieldX + scale(6),
                        searchFieldY + searchFieldHeight / 2 - ClientFonts.AltManager[18].getFontHeight() / 2 + scale(4.5F),
                        searchText.isEmpty() && !searchFocused ? ColorUtils.rgba(150, 150, 150, placeholderAlpha) : ColorUtils.rgba(255, 255, 255, textAlpha));

                String searchDisplayText = searchText.isEmpty() && !searchFocused ? "Поиск..." : searchText;
                int searchTextColor = searchText.isEmpty() && !searchFocused ?
                        ColorUtils.rgba(150, 150, 150, placeholderAlpha) :
                        ColorUtils.rgba(255, 255, 255, textAlpha);

                Fonts.sfuy.drawText(matrixStack, searchDisplayText,
                        fieldX + scale(20),
                        searchFieldY + searchFieldHeight / 2 - Fonts.sfuy.getHeight(scaleFontSize(10)) / 2,
                        searchTextColor, scaleFontSize(10));

                if (searchFocused && System.currentTimeMillis() % 1000 > 500) {
                    String cursorText = searchText + "_";
                    Fonts.sfuy.drawText(matrixStack, cursorText,
                            fieldX + scale(20),
                            searchFieldY + searchFieldHeight / 2 - Fonts.sfuy.getHeight(scaleFontSize(10)) / 2,
                            ColorUtils.rgba(255, 255, 255, textAlpha), scaleFontSize(10));
                }

                float newAccountFieldY = getNewAccountFieldY(layout);

                newAccountOutlineAnimation = MathUtil.lerp(newAccountOutlineAnimation, newAccountFocused ? 1f : 0f, 8f);

                int newAccountBgColor = ColorUtils.rgba(15, 15, 18, searchBgAlpha);

                if (newAccountOutlineAnimation > 0.01f) {
                    int glowAlpha = (int) (newAccountOutlineAnimation * 35 * animationAlpha);
                    float glowRadius = fieldRadius + 2;

                    RenderUtility.drawRoundedRect(fieldX - 2, newAccountFieldY - 2, fieldWidth + 4, searchFieldHeight + 4, glowRadius,
                            ColorUtils.setAlpha(themeColor, (int) (glowAlpha * 0.3f)));
                    RenderUtility.drawRoundedRect(fieldX - 1.5f, newAccountFieldY - 1.5f, fieldWidth + 3, searchFieldHeight + 3, glowRadius,
                            ColorUtils.setAlpha(themeColor, (int) (glowAlpha * 0.5f)));
                    RenderUtility.drawRoundedRect(fieldX - 1, newAccountFieldY - 1, fieldWidth + 2, searchFieldHeight + 2, fieldRadius + 1,
                            ColorUtils.setAlpha(themeColor, glowAlpha));

                    int outlineAlpha = (int) (newAccountOutlineAnimation * 220 * animationAlpha);

                    RenderUtility.drawRoundedRect(fieldX - 0.5f, newAccountFieldY - 0.5f, fieldWidth + 1, searchFieldHeight + 1, fieldRadius + 0.5f,
                            ColorUtils.setAlpha(themeColor, outlineAlpha));
                }

                RenderUtility.drawRoundedRect(fieldX, newAccountFieldY, fieldWidth, searchFieldHeight,
                        fieldRadius, newAccountBgColor);

                ClientFonts.AltManager[17].drawString(matrixStack, "A",
                        fieldX + scale(6),
                        newAccountFieldY + searchFieldHeight / 2 - ClientFonts.AltManager[20].getFontHeight() / 2 + scale(7),
                        newAccountText.isEmpty() && !newAccountFocused ? ColorUtils.rgba(150, 150, 150, placeholderAlpha) : ColorUtils.rgba(255, 255, 255, textAlpha));

                String newAccountDisplayText = newAccountText.isEmpty() && !newAccountFocused ? "Введите никнейм..." : newAccountText;
                int newAccountTextColor = newAccountText.isEmpty() && !newAccountFocused ?
                        ColorUtils.rgba(150, 150, 150, placeholderAlpha) :
                        ColorUtils.rgba(255, 255, 255, textAlpha);

                Fonts.sfuy.drawText(matrixStack, newAccountDisplayText,
                        fieldX + scale(20),
                        newAccountFieldY + searchFieldHeight / 2 - Fonts.sfuy.getHeight(scaleFontSize(10)) / 2,
                        newAccountTextColor, scaleFontSize(10));

                if (newAccountFocused && System.currentTimeMillis() % 1000 > 500) {
                    String cursorText = newAccountText + "_";
                    Fonts.sfuy.drawText(matrixStack, cursorText,
                            fieldX + scale(20),
                            newAccountFieldY + searchFieldHeight / 2 - Fonts.sfuy.getHeight(scaleFontSize(10)) / 2,
                            ColorUtils.rgba(255, 255, 255, textAlpha), scaleFontSize(10));
                }

                float addButtonY = getAddButtonY(layout);
                float generateButtonY = getGenerateButtonY(layout);
                float clearAllButtonY = getClearAllButtonY(layout);
                float actionButtonHeight = layout.actionButtonHeight;

                renderActionButton(matrixStack, addButtonState, fieldX, addButtonY,
                        fieldWidth, actionButtonHeight, "Добавить", "G", ClientFonts.AltManager[16],
                        ADD_ICON_OFFSET_X, ADD_ICON_OFFSET_Y, mouseX, mouseY, false, partialTicks, animationAlpha);
                renderActionButton(matrixStack, generateButtonState, fieldX, generateButtonY,
                        fieldWidth, actionButtonHeight, "Рандом", "C", ClientFonts.AltManager[30],
                        GENERATE_ICON_OFFSET_X, GENERATE_ICON_OFFSET_Y, mouseX, mouseY, false, partialTicks, animationAlpha);
                renderActionButton(matrixStack, clearAllButtonState, fieldX, clearAllButtonY,
                        fieldWidth, actionButtonHeight, "Очисить всех", "D", ClientFonts.AltManager[21],
                        CLEAR_ICON_OFFSET_X, CLEAR_ICON_OFFSET_Y, mouseX, mouseY, true, partialTicks, animationAlpha);

                float rightPanelWidth = layout.rightWidth;
                float rightPanelHeight = layout.rightHeight;
                float rightPanelX = layout.rightX;
                float rightPanelY = layout.rightY;

                int rightPanelAlpha = (int) (150 * animationAlpha);
                int rightPanelColor = ColorUtils.rgba(10, 10, 10, rightPanelAlpha);

                RenderUtility.drawRoundedRect(rightPanelX, rightPanelY, rightPanelWidth, rightPanelHeight,
                        scale(8), rightPanelColor);

                Scissor.push();
                Scissor.setFromComponentCoordinates(
                        rightPanelX + scale(5),
                        rightPanelY + scale(5),
                        Math.max(scale(8), rightPanelWidth - scale(10)),
                        Math.max(scale(8), rightPanelHeight - scale(10))
                );

                if (accounts.isEmpty()) {
                    int emptyFontSize = scaleFontSize(14);
                    int emptyTextAlpha = (int) (200 * animationAlpha);
                    Fonts.sfuy.drawCenteredText(matrixStack, "Нет аккаунтов",
                            rightPanelX + rightPanelWidth / 2,
                            rightPanelY + rightPanelHeight / 2,
                            ColorUtils.rgba(255, 255, 255, emptyTextAlpha), emptyFontSize);
                }

                List<Account> filteredAccounts = getFilteredAccounts();

                try {
                    float cardSpacingX = scale(8);
                    float cardSpacingY = scale(8);
                    int columns = rightPanelWidth >= scale(290) ? 2 : 1;
                    float cardWidth = (rightPanelWidth - scale(20) - cardSpacingX * (columns - 1)) / columns;
                    float cardHeight = scale(BASE_CARD_HEIGHT);
                    float startY = rightPanelY + scale(10);
                    float startX = rightPanelX + scale(10);
                    float roundedScrollAn = Math.round(scrollAn);

                    for (int i = 0; i < filteredAccounts.size(); i++) {
                        Account account = filteredAccounts.get(i);

                        int row = i / columns;
                        int col = i % columns;

                        account.width = cardWidth;
                        account.height = cardHeight;
                        account.guiScale = guiScale;
                        account.globalAlpha = animationAlpha;

                        account.x = startX + col * (cardWidth + cardSpacingX);
                        account.y = startY + row * (cardHeight + cardSpacingY) + roundedScrollAn;

                        if (account.y + account.height > rightPanelY && account.y < rightPanelY + rightPanelHeight) {
                            GL11.glPushMatrix();

                            if (account.animate.getOutput() < 0.99) {
                                GL11.glTranslatef(account.x + account.width / 2f, account.y + account.height / 2f, 0);
                                GL11.glScalef((float) account.animate.getOutput(), (float) account.animate.getOutput(), 0);
                                GL11.glTranslatef(-(account.x + account.width / 2f), -(account.y + account.height / 2f), 0);
                            }

                            account.render(matrixStack, mouseX, mouseY);

                            GL11.glPopMatrix();
                        }
                    }

                    int totalRows = (int) Math.ceil((double) filteredAccounts.size() / columns);
                    float contentHeight = totalRows <= 0
                            ? 0
                            : totalRows * cardHeight + Math.max(0, totalRows - 1) * cardSpacingY;
                    float maxScrollHeight = Math.max(0, contentHeight - (rightPanelHeight - scale(20)));

                    scroll = MathHelper.clamp(scroll, -maxScrollHeight, 0);

                } catch (ConcurrentModificationException e) {
                }

                Scissor.unset();
                Scissor.pop();

                GL11.glPopMatrix();

                super.render(matrixStack, mouseX, mouseY, partialTicks);
            }

            private void renderActionButton(MatrixStack matrixStack, ActionButtonState state, float x, float y, float width, float height,
                                            String text, String icon, xd.harm.utils.text.font.styled.StyledFont iconFont,
                                            float iconOffsetX, float iconOffsetY,
                                            int mouseX, int mouseY, boolean isDestructive, float partialTicks, float globalAlpha) {
                boolean hovered = isInRegion(mouseX, mouseY, x, y, width, height) && !isClosing;
                float currentHover = state.hover;
                float currentScale = state.scale;

                float delta = MathUtil.clamp(partialTicks * 0.016f * 60.0f, 0.0f, 2.0f);
                float targetHover = hovered ? 1.0f : 0.0f;
                float hoverSpeed = 0.25f;
                float scaleSpeed = 0.25f;

                currentHover += (targetHover - currentHover) * hoverSpeed * delta;
                currentHover = MathUtil.clamp(currentHover, 0.0f, 1.0f);
                currentScale += ((hovered ? 1.02f : 1.0f) - currentScale) * scaleSpeed * delta;
                currentScale = MathUtil.clamp(currentScale, 1.0f, 1.02f);

                state.hover = currentHover;
                state.scale = currentScale;

                int themeCol = Theme.MainColor(0);
                int tr = ColorUtils.getRed(themeCol);
                int tg = ColorUtils.getGreen(themeCol);
                int tb = ColorUtils.getBlue(themeCol);

                int bg = isDestructive
                        ? ColorUtils.rgba(14 + (int) (currentHover * 20), 13 + (int) (currentHover * 6), 16 + (int) (currentHover * 7), 106 + (int) (currentHover * 50))
                        : ColorUtils.rgba(14 + (int) (currentHover * (tr / 12 + 6)), 14 + (int) (currentHover * (tg / 12 + 4)), 17 + (int) (currentHover * (tb / 12 + 4)), 106 + (int) (currentHover * 52));
                int bgTopRight = ColorUtils.rgba(16 + (int) (currentHover * (tr / 14)), 16 + (int) (currentHover * (tg / 14)), 18 + (int) (currentHover * (tb / 14)), 116 + (int) (currentHover * 46));
                int bgBottomLeft = ColorUtils.rgba(9, 9, 11, 106 + (int) (currentHover * 38));
                int bgBottomRight = ColorUtils.rgba(14 + (int) (currentHover * (tr / 16)), 14 + (int) (currentHover * (tg / 16)), 16 + (int) (currentHover * (tb / 16)), 112 + (int) (currentHover * 42));
                int border = isDestructive
                        ? ColorUtils.rgba(155, 70, 84, 45 + (int) (currentHover * 96))
                        : ColorUtils.rgba(
                        (int) (80 + currentHover * (tr - 80)),
                        (int) (80 + currentHover * (tg - 80)),
                        (int) (85 + currentHover * (tb - 85)),
                        50 + (int) (currentHover * 102));

                int textColor = ColorUtils.rgba(
                        Math.min(255, (int) (178 + currentHover * (tr / 2.5f))),
                        Math.min(255, (int) (178 + currentHover * (tg / 2.5f))),
                        Math.min(255, (int) (186 + currentHover * (tb / 2.5f))),
                        228);
                int iconColor = isDestructive
                        ? ColorUtils.rgba(196 + (int) (currentHover * 42), 84, 100, 228)
                        : ColorUtils.rgba(
                        Math.min(255, (int) (170 + currentHover * (tr / 2f))),
                        Math.min(255, (int) (170 + currentHover * (tg / 2f))),
                        Math.min(255, (int) (180 + currentHover * (tb / 2f))),
                        228);

                RenderUtility.drawMenuButton(x, y, width, height, scale(5),
                        alphaColor(bg, globalAlpha),
                        alphaColor(bgTopRight, globalAlpha),
                        alphaColor(bgBottomLeft, globalAlpha),
                        alphaColor(bgBottomRight, globalAlpha),
                        alphaColor(border, globalAlpha),
                        0.8f + currentHover * 0.32f,
                        currentHover * currentHover * 0.16f * globalAlpha,
                        scale(18));

                float textWidth = Fonts.sfuy.getWidth(text, 8f);
                float iconWidth = iconFont.getWidth(icon);
                float iconHeight = iconFont.getFontHeight();
                float spacing = scale(6);
                float totalWidth = iconWidth + spacing + textWidth;

                float startX = x + (width - totalWidth) / 2f;

                float iconX = startX - scale(2) + iconOffsetX;
                float iconY = y + height / 2f - iconHeight / 2f + 3f + iconOffsetY;

                iconFont.drawString(matrixStack, icon, iconX, iconY, alphaColor(iconColor, globalAlpha));

                float textX = iconX + iconWidth + spacing;
                float textY = y + height / 2f - Fonts.sfuy.getHeight(8f) / 2f;

                Fonts.sfuy.drawText(matrixStack, text, textX, textY, alphaColor(textColor, globalAlpha), 8f);
            }

            public class Account {
                public String name;
                public ResourceLocation skin;
                public String uuid;
                public long creationTime;
                private final String formattedDate;
                private String cachedDisplayName = "";
                private float cachedDisplayMaxWidth = Float.NaN;
                private int cachedDisplayFontSize = Integer.MIN_VALUE;

                public float x;
                public float y;
                public float width;
                public float height;
                public float guiScale = 1.0f;
                public float globalAlpha = 1.0f;

                public Animation animate = new DecelerateAnimation(50, 1);
                private float selectAnimation = 0;
                private float hoverAnimation = 0;
                private float outlineAnimation = 0;
                private float copyHoverAnim = 0;
                private float deleteHoverAnim = 0;
                private float copyClickAnim = 0;
                private float deleteClickAnim = 0;
                private long copyClickTime = 0;
                private long deleteClickTime = 0;

                private float accountScale(float value) {
                    return value * guiScale;
                }

                public Account(String accountName) {
                    this(accountName, System.currentTimeMillis());
                }

                public Account(String accountName, long creationTime) {
                    this.name = accountName;
                    this.creationTime = creationTime;
                    this.formattedDate = formatDate(creationTime);
                    UUID playerUUID = null;

                    try {
                        playerUUID = resolveUUID(accountName);
                        if (playerUUID != null) {
                            this.uuid = playerUUID.toString();
                        } else {
                            this.uuid = UUID.nameUUIDFromBytes(("OfflinePlayer:" + accountName).getBytes()).toString();
                        }
                    } catch (Exception e) {
                        this.uuid = UUID.nameUUIDFromBytes(("OfflinePlayer:" + accountName).getBytes()).toString();
                    }

                    this.skin = DefaultPlayerSkin.getDefaultSkin(UUID.fromString(this.uuid));
                    loadSkin(UUID.fromString(this.uuid), accountName);

                    animate.reset();
                    animate.setDirection(Direction.FORWARDS);
                }

                private void loadSkin(UUID uuid, String accountName) {
                    GameProfile profile = new GameProfile(uuid, accountName);
                    Minecraft.getInstance().getSkinManager().loadProfileTextures(profile, (type, loc, tex) -> {
                        if (type == MinecraftProfileTexture.Type.SKIN) {
                            this.skin = loc;
                        }
                    }, true);
                }

                public static UUID resolveUUID(String name) {
                    try {
                        URLConnection connection = new URL("https://api.mojang.com/users/profiles/minecraft/" + name).openConnection();
                        connection.setConnectTimeout(5000);
                        connection.setReadTimeout(5000);

                        InputStreamReader in = new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8);
                        try {
                            JsonObject jsonObject = new Gson().fromJson(in, JsonObject.class);
                            if (jsonObject != null && jsonObject.has("id")) {
                                String id = jsonObject.get("id").getAsString();
                                return UUID.fromString(id.replaceFirst("(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})", "$1-$2-$3-$4-$5"));
                            }
                        } finally {
                            in.close();
                        }
                    } catch (Exception e) {
                    }
                    return null;
                }

                public void render(MatrixStack matrixStack, double mouseX, double mouseY) {
                    boolean isCurrentAccount = mc.getSession().getUsername().equals(name);
                    boolean hoveringCopy = isHoveringCopy(mouseX, mouseY);
                    boolean hoveringDelete = isHoveringDelete(mouseX, mouseY);
                    boolean isHoveringCard = RenderUtility.isInRegion(mouseX, mouseY, x, y, width, height)
                            && !hoveringCopy && !hoveringDelete && !isClosing;

                    selectAnimation = MathUtil.lerp(selectAnimation, isCurrentAccount ? 1 : 0, 3.4f);
                    hoverAnimation = MathUtil.lerp(hoverAnimation, isHoveringCard ? 1 : 0, 5.5f);

                    float targetOutline = isCurrentAccount ? 1f : (isHoveringCard ? 0.4f : 0f);
                    outlineAnimation = MathUtil.lerp(outlineAnimation, targetOutline, 6f);

                    copyHoverAnim = MathUtil.lerp(copyHoverAnim, hoveringCopy && !isClosing ? 1 : 0, 7.9f);
                    deleteHoverAnim = MathUtil.lerp(deleteHoverAnim, hoveringDelete && !isClosing ? 1 : 0, 7.9f);
                    copyClickAnim = MathUtil.lerp(copyClickAnim, 0, 8f);
                    deleteClickAnim = MathUtil.lerp(deleteClickAnim, 0, 8f);

                    int themeColor = Theme.MainColor(0);

                    int cardAlpha = (int) (200 * globalAlpha);
                    int cardColor = ColorUtils.rgba(15, 15, 18, cardAlpha);

                    float cornerRadius = accountScale(6);

                    if (outlineAnimation > 0.01f) {
                        int glowAlpha = (int)(outlineAnimation * 35 * globalAlpha);
                        float glowRadius = cornerRadius + 2;

                        RenderUtility.drawRoundedRect(x - 2, y - 2, width + 4, height + 4, glowRadius,
                                ColorUtils.setAlpha(themeColor, (int)(glowAlpha * 0.3f)));
                        RenderUtility.drawRoundedRect(x - 1.5f, y - 1.5f, width + 3, height + 3, glowRadius,
                                ColorUtils.setAlpha(themeColor, (int)(glowAlpha * 0.5f)));
                        RenderUtility.drawRoundedRect(x - 1, y - 1, width + 2, height + 2, cornerRadius + 1,
                                ColorUtils.setAlpha(themeColor, glowAlpha));

                        int outlineAlpha;
                        if (isCurrentAccount) {
                            outlineAlpha = (int)(outlineAnimation * 220 * globalAlpha);
                        } else {
                            outlineAlpha = (int)(outlineAnimation * 120 * globalAlpha);
                        }

                        RenderUtility.drawRoundedRect(x - 0.5f, y - 0.5f, width + 1, height + 1, cornerRadius + 0.5f,
                                ColorUtils.setAlpha(themeColor, outlineAlpha));
                    }

                    RenderUtility.drawRoundedRect(x, y, width, height, cornerRadius, cardColor);

                    float headSize = accountScale(35);
                    float headX = x + accountScale(5);
                    float headY = y - 2.5f + (height - headSize) / 2;

                    RenderUtility.drawHead(skin, headX, headY, headSize, headSize, accountScale(4), globalAlpha, 0.0f);

                    float maxTextWidth = width - headSize - accountScale(50);
                    int nameFontSize = scaleFontSize(10);
                    String displayName = getDisplayName(maxTextWidth, nameFontSize);

                    int nameAlpha = (int) (255 * globalAlpha);
                    int nameColor = ColorUtils.rgba(255, 255, 255, nameAlpha);

                    float textX = headX + headSize + accountScale(8);

                    Fonts.sfuy.drawText(matrixStack, displayName,
                            textX, y + accountScale(12),
                            nameColor, nameFontSize);

                    int dateFontSize = scaleFontSize(8);
                    int dateAlpha = (int) (180 * globalAlpha);
                    Fonts.sfuy.drawText(matrixStack, formattedDate,
                            textX, y + accountScale(26),
                            ColorUtils.rgba(160, 160, 160, dateAlpha), dateFontSize);

                    float iconSize = accountScale(18);
                    float iconSpacing = accountScale(4);
                    float iconsX = x + width - iconSize - accountScale(6);
                    float iconsStartY = y + (height - (iconSize * 2 + iconSpacing)) / 2;

                    int copyIconAlpha = (int)(selectAnimation * 255 * globalAlpha);
                    int inactiveAlpha = (int)((1 - selectAnimation) * 180 * globalAlpha);

                    if (selectAnimation > 0.01f) {
                        int finalCopyAlpha = (int)(copyIconAlpha * (0.7f + copyHoverAnim * 0.3f));
                        renderVerticalIconAltWithAlpha(matrixStack, iconsX, iconsStartY, iconSize, copyHoverAnim, copyClickAnim,
                                COPY_ICON_COLOR, "F", 20, finalCopyAlpha);
                    }

                    if (selectAnimation < 0.99f) {
                        renderVerticalIconAltWithAlpha(matrixStack, iconsX, iconsStartY, iconSize, 0, 0,
                                INACTIVE_ICON_COLOR, "E", 24, inactiveAlpha);
                    }

                    renderVerticalIconAltWithGlobalAlpha(matrixStack, iconsX, iconsStartY + iconSize + iconSpacing, iconSize, deleteHoverAnim, deleteClickAnim,
                            DELETE_ICON_COLOR, "D", 21, globalAlpha);
                }

                private void renderVerticalIconAlt(MatrixStack matrixStack, float x, float y, float size,
                                                   float hoverAnim, float clickAnim, int baseColor, String icon, int fontIndex) {
                    renderVerticalIconAltWithAlpha(matrixStack, x, y, size, hoverAnim, clickAnim, baseColor, icon, fontIndex, 255);
                }

                private void renderVerticalIconAltWithGlobalAlpha(MatrixStack matrixStack, float x, float y, float size,
                                                                  float hoverAnim, float clickAnim, int baseColor, String icon, int fontIndex, float globalAlpha) {
                    renderVerticalIconAltWithAlpha(matrixStack, x, y, size, hoverAnim, clickAnim, baseColor, icon, fontIndex, (int)(255 * globalAlpha));
                }

                private void renderVerticalIconAltWithAlpha(MatrixStack matrixStack, float x, float y, float size,
                                                            float hoverAnim, float clickAnim, int baseColor, String icon, int fontIndex, int alpha) {
                    if (alpha <= 0) return;

                    float scale = 1.0f + (float)(Easings.CUBIC_OUT.ease(hoverAnim) * 0.15);

                    GL11.glPushMatrix();
                    GL11.glTranslatef(x + size/2, y + size/2, 0);
                    float iconScale = size / 14f;
                    GL11.glScalef(scale * iconScale, scale * iconScale, 1.0f);
                    GL11.glTranslatef(-(x + size/2), -(y + size/2), 0);

                    int baseAlpha = (int)(180 * (alpha / 255f));
                    int hoverAlpha = (int)(255 * (alpha / 255f));

                    int iconColor = ColorUtils.interpolateColor(
                            ColorUtils.setAlpha(baseColor, baseAlpha),
                            ColorUtils.setAlpha(baseColor, hoverAlpha),
                            hoverAnim
                    );

                    if (clickAnim > 0.01f) {
                        iconColor = ColorUtils.interpolateColor(
                                iconColor,
                                ColorUtils.rgba(255, 255, 255, alpha),
                                clickAnim * 0.7f
                        );
                    }

                    ClientFonts.AltManager[fontIndex].drawString(matrixStack, icon,
                            x + size/2 - ClientFonts.AltManager[fontIndex].getWidth(icon)/2,
                            y + size/2 - ClientFonts.AltManager[fontIndex].getFontHeight()/2 + 3,
                            iconColor);

                    GL11.glPopMatrix();
                }

                private boolean isHoveringCopy(double mouseX, double mouseY) {
                    boolean isCurrentAccount = mc.getSession().getUsername().equals(name);
                    if (!isCurrentAccount) {
                        return false;
                    }
                    float iconSize = accountScale(14);
                    float iconsX = x + width - iconSize - accountScale(6);
                    float iconsStartY = y + (height - (iconSize * 2 + accountScale(4))) / 2;
                    return RenderUtility.isInRegion(mouseX, mouseY, iconsX, iconsStartY, iconSize, iconSize);
                }

                private boolean isHoveringDelete(double mouseX, double mouseY) {
                    float iconSize = accountScale(14);
                    float iconSpacing = accountScale(4);
                    float iconsX = x + width - iconSize - accountScale(6);
                    float iconsStartY = y + (height - (iconSize * 2 + iconSpacing)) / 2;
                    float deleteY = iconsStartY + iconSize + iconSpacing;
                    return RenderUtility.isInRegion(mouseX, mouseY, iconsX, deleteY, iconSize, iconSize);
                }

                private String getDisplayName(float maxTextWidth, int nameFontSize) {
                    if (Float.compare(maxTextWidth, cachedDisplayMaxWidth) == 0 && nameFontSize == cachedDisplayFontSize) {
                        return cachedDisplayName;
                    }

                    cachedDisplayMaxWidth = maxTextWidth;
                    cachedDisplayFontSize = nameFontSize;
                    if (Fonts.sfuy.getWidth(name, nameFontSize) <= maxTextWidth) {
                        cachedDisplayName = name;
                        return cachedDisplayName;
                    }

                    int low = 0;
                    int high = name.length();
                    while (low < high) {
                        int mid = (low + high + 1) >>> 1;
                        if (Fonts.sfuy.getWidth(name.substring(0, mid) + "...", nameFontSize) <= maxTextWidth) {
                            low = mid;
                        } else {
                            high = mid - 1;
                        }
                    }
                    cachedDisplayName = name.substring(0, low) + "...";
                    return cachedDisplayName;
                }

                private String formatDate(long timestamp) {
                    java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd.MM.yyyy");
                    return sdf.format(new java.util.Date(timestamp));
                }

                public void onClick(double mouseX, double mouseY, int button) {
                    if (isOpening || isClosing) return;
                    if (RenderUtility.isInRegion(mouseX, mouseY, x, y, width, height)) {
                        if (button == 0) {
                            if (isHoveringDelete(mouseX, mouseY)) {
                                deleteClickAnim = 1.0f;
                                deleteClickTime = System.currentTimeMillis();
                                accountsToDelete.add(this);
                                markAccountFilterDirty();
                                animate.setDirection(Direction.BACKWARDS);
                                new Thread(AltConfig::updateFile).start();
                            } else if (isHoveringCopy(mouseX, mouseY)) {
                                copyClickAnim = 1.0f;
                                copyClickTime = System.currentTimeMillis();
                                mc.keyboardListener.setClipboardString(name);
                            } else {
                                mc.session = new Session(name, uuid, "", "mojang");
                            }
                        }
                    }
                }
            }
        }
