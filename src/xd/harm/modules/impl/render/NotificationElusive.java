package xd.harm.modules.impl.render;

import com.google.common.eventbus.Subscribe;
import com.mojang.blaze3d.matrix.MatrixStack;
import xd.harm.Harmony;
import xd.harm.events.render.EventDisplay;
import xd.harm.modules.api.Category;
import xd.harm.modules.api.Module;
import xd.harm.modules.api.ModuleRegister;
import xd.harm.modules.settings.impl.BooleanSetting;
import xd.harm.modules.settings.impl.ModeListSetting;
import xd.harm.modules.settings.impl.ModeSetting;
import xd.harm.modules.settings.impl.SliderSetting;
import xd.harm.utils.render.color.ColorUtils;
import xd.harm.utils.render.rect.RenderUtility;
import xd.harm.utils.text.font.ClientFonts;
import xd.harm.utils.text.font.styled.StyledFont;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

@ModuleRegister(name = "NotificationElusive", category = Category.Render, desc = "Elusive уведомления модулей")
public class NotificationElusive extends Module {

    public final ModeListSetting events = new ModeListSetting("События",
            new BooleanSetting("Включение", true),
            new BooleanSetting("Выключение", true));
    public final ModeSetting position = new ModeSetting("Позиция", "Справа снизу",
            "Справа снизу", "Слева снизу");
    public final SliderSetting duration = new SliderSetting("Длительность", 2.0f, 1.0f, 6.0f, 0.5f);
    public final SliderSetting fontSize = new SliderSetting("Размер шрифта", 14.0f, 10.0f, 18.0f, 1.0f);
    public final SliderSetting backgroundAlpha = new SliderSetting("Прозрачность фона", 120.0f, 40.0f, 255.0f, 5.0f);
    public final SliderSetting radius = new SliderSetting("Скругление", 2.0f, 0.0f, 8.0f, 0.5f);
    public final SliderSetting maxVisible = new SliderSetting("Максимум", 5.0f, 1.0f, 8.0f, 1.0f);

    private final Map<Module, Boolean> states = new IdentityHashMap<>();
    private final List<Notice> notices = new ArrayList<>();
    private Module previousNotifications;
    private boolean previousNotificationsEnabled;

    public NotificationElusive() {
        addSettings(events, position, duration, fontSize, backgroundAlpha, radius, maxVisible);
    }

    @Override
    public boolean onEnable() {
        super.onEnable();
        states.clear();
        notices.clear();
        previousNotifications = null;
        previousNotificationsEnabled = false;
        if (Harmony.getInstance() != null && Harmony.getInstance().getModuleManager() != null) {
            for (Module module : Harmony.getInstance().getModuleManager().getModules()) {
                if (module != this && module.getName().equalsIgnoreCase("Notifications")) {
                    previousNotifications = module;
                    previousNotificationsEnabled = module.isState();
                    if (previousNotificationsEnabled) module.setState(false, false);
                    break;
                }
            }
        }
        if (Harmony.getInstance() != null && Harmony.getInstance().getModuleManager() != null) {
            for (Module module : Harmony.getInstance().getModuleManager().getModules()) {
                states.put(module, module.isState());
            }
        }
        return false;
    }

    @Override
    public boolean onDisable() {
        states.clear();
        notices.clear();
        if (previousNotifications != null && previousNotificationsEnabled && !previousNotifications.isState()) {
            previousNotifications.setState(true, false);
        }
        previousNotifications = null;
        previousNotificationsEnabled = false;
        super.onDisable();
        return false;
    }

    @Subscribe
    public void onDisplay(EventDisplay event) {
        if (event.getType() != EventDisplay.Type.HIGH || mc.player == null
                || mc.gameSettings.showDebugInfo || Boolean.getBoolean("bot.mode")) {
            return;
        }
        detectChanges();
        renderNotices(event.getMatrixStack());
    }

    private void detectChanges() {
        for (Module module : Harmony.getInstance().getModuleManager().getModules()) {
            if (module == this) {
                continue;
            }
            boolean current = module.isState();
            Boolean previous = states.put(module, current);
            if (previous == null || previous == current) {
                continue;
            }
            if (current && events.getValueByName("Включение").get()) {
                addNotice(module.getName(), true);
            } else if (!current && events.getValueByName("Выключение").get()) {
                addNotice(module.getName(), false);
            }
        }
    }

    private void addNotice(String moduleName, boolean enabled) {
        notices.add(new Notice(moduleName, enabled, System.currentTimeMillis()));
        int limit = Math.max(1, Math.round(maxVisible.get()));
        while (notices.size() > limit) {
            notices.remove(0);
        }
    }

    private void renderNotices(MatrixStack matrix) {
        int size=Math.max(10,Math.min(18,Math.round(fontSize.get())));
        StyledFont regular=ClientFonts.elusiveText[size];
        StyledFont bold=ClientFonts.elusiveText[Math.min(24,size+4)];
        StyledFont iconFont=ClientFonts.elusiveNotify[Math.min(28,size+6)];
        if (regular==null || bold==null || iconFont==null) return;
        long now=System.currentTimeMillis();
        long lifetime=Math.max(1000L,(long)(duration.get()*1000.0f));
        float screenWidth=mc.getMainWindow().getScaledWidth();
        float y=mc.getMainWindow().getScaledHeight()-25.0f;
        Iterator<Notice> iterator=notices.iterator();
        while (iterator.hasNext()) {
            Notice notice=iterator.next();
            long age=now-notice.createdAt;
            if (age>=lifetime+260L) { iterator.remove(); continue; }
            float animation=age<220L?easeOutCubic(age/220.0f):(age>lifetime?1.0f-easeInCubic((age-lifetime)/260.0f):1.0f);
            animation=Math.max(0.0f,Math.min(1.0f,animation));
            String title="Notification";
            String content=notice.moduleName+(notice.enabled?" was enabled!":" was disabled!");
            float contentWidth=Math.max(regular.getWidth(content)+5.0f,bold.getWidth(title)+5.0f);
            float cardWidth=contentWidth+21.0f;
            boolean right=position.is("Справа снизу");
            float visibleX=right?screenWidth-cardWidth-3.5f:3.5f;
            float hiddenX=right?screenWidth+4.0f:-cardWidth-4.0f;
            float x=hiddenX+(visibleX-hiddenX)*animation;
            int bg=ColorUtils.rgba(0,0,0,Math.round(backgroundAlpha.get()*animation));
            int iconColor=ColorUtils.rgba(notice.enabled?0:255,notice.enabled?255:0,0,Math.round(255.0f*animation));
            int textColor=ColorUtils.rgba(255,255,255,Math.round(255.0f*animation));
            RenderUtility.drawRoundedRect(x,y-4.0f,cardWidth,25.0f,radius.get(),bg);
            iconFont.drawString(matrix,notice.enabled?"s":"e",x+4.5f,y+7.0f-iconFont.getFontHeight()/2.0f,iconColor);
            float textX=x+17.5f;
            bold.drawString(matrix,title,textX,y-1.0f,textColor);
            regular.drawString(matrix,content,textX+1.0f,y+11.5f,textColor);
            y-=30.0f;
        }
    }

    private static float easeOutCubic(float value) {
        float t = Math.max(0.0f, Math.min(1.0f, value));
        return 1.0f - (float) Math.pow(1.0f - t, 3.0);
    }

    private static float easeInCubic(float value) {
        float t = Math.max(0.0f, Math.min(1.0f, value));
        return t * t * t;
    }

    private static final class Notice {
        private final String moduleName;
        private final boolean enabled;
        private final long createdAt;

        private Notice(String moduleName, boolean enabled, long createdAt) {
            this.moduleName = moduleName;
            this.enabled = enabled;
            this.createdAt = createdAt;
        }
    }
}
