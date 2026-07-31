package xd.harm.modules.impl.player;

import xd.harm.Harmony;
import com.google.common.eventbus.Subscribe;
import xd.harm.events.network.EventPacket;
import xd.harm.events.world.EventUpdate;
import xd.harm.modules.api.Category;
import xd.harm.modules.api.Module;
import xd.harm.modules.api.ModuleRegister;
import xd.harm.modules.settings.impl.BooleanSetting;
import xd.harm.modules.settings.impl.CategorySetting;
import xd.harm.modules.settings.impl.ModeSetting;
import xd.harm.modules.settings.impl.SliderSetting;
import xd.harm.modules.settings.impl.ModeListSetting;
import xd.harm.utils.math.SensUtils;
import xd.harm.utils.player.InventoryUtil;
import xd.harm.config.FriendStorage;
import xd.harm.utils.render.rect.RenderUtility;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.AirItem;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.ArmorMaterial;
import net.minecraft.network.play.client.*;
import net.minecraft.network.play.server.*;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector2f;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.math.vector.Vector4f;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.SoundEvents;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.HashMap;
import java.util.Map;

@ModuleRegister(name = "AutoThrow", category = Category.Player, desc = "Автоматические расходники")
public class AutoThrow extends Module {

    private static final float FIXED_RANGE = 10.0f;

    private final BooleanSetting disorientationEnabled = new BooleanSetting("Дезориентация", false);
    public final ModeListSetting disorientationTargets = (new ModeListSetting("Дезориентация цели:", new BooleanSetting[]{
            new BooleanSetting("Незеритовая броня", false),
            new BooleanSetting("Алмазная броня", false),
            new BooleanSetting("Элитры", false),
    })).setVisible(() -> disorientationEnabled.get());
    public final ModeListSetting disorientationTriggers = (new ModeListSetting("Дезориентация триггеры:", new BooleanSetting[]{
            new BooleanSetting("Мало ХП у меня", false),
            new BooleanSetting("Мало ХП у противника", false),
            new BooleanSetting("Снесли Тотем мне", false),
            new BooleanSetting("Снёс Тотем я", false),
    })).setVisible(() -> disorientationEnabled.get());

    private final BooleanSetting trapEnabled = new BooleanSetting("Трапка", false);
    public final ModeListSetting trapTargets = (new ModeListSetting("Трапка цели:", new BooleanSetting[]{
            new BooleanSetting("Незеритовая броня", false),
            new BooleanSetting("Алмазная броня", false),
            new BooleanSetting("Элитры", false),
    })).setVisible(() -> trapEnabled.get());
    public final ModeListSetting trapTriggers = (new ModeListSetting("Трапка триггеры:", new BooleanSetting[]{
            new BooleanSetting("Мало ХП у меня", false),
            new BooleanSetting("Мало ХП у противника", false),
            new BooleanSetting("Снесли Тотем мне", false),
            new BooleanSetting("Снёс Тотем я", false),
    })).setVisible(() -> trapEnabled.get());

    private final BooleanSetting blatantEnabled = new BooleanSetting("Явная пыль", true);
    public final ModeListSetting blatantTargets = (new ModeListSetting("Явная пыль цели:", new BooleanSetting[]{
            new BooleanSetting("Незеритовая броня", true),
            new BooleanSetting("Алмазная броня", false),
            new BooleanSetting("Элитры", false),
    })).setVisible(() -> blatantEnabled.get());
    public final ModeListSetting blatantTriggers = (new ModeListSetting("Явная пыль триггеры:", new BooleanSetting[]{
            new BooleanSetting("Мало ХП у меня", false),
            new BooleanSetting("Мало ХП у противника", false),
            new BooleanSetting("Снесли Тотем мне", false),
            new BooleanSetting("Снёс Тотем я", false),
    })).setVisible(() -> blatantEnabled.get());

    private final BooleanSetting flameEnabled = new BooleanSetting("Огненный смерч", false);
    public final ModeListSetting flameTargets = (new ModeListSetting("Огненный смерч цели:", new BooleanSetting[]{
            new BooleanSetting("Незеритовая броня", false),
            new BooleanSetting("Алмазная броня", false),
            new BooleanSetting("Элитры", false),
    })).setVisible(() -> flameEnabled.get());
    public final ModeListSetting flameTriggers = (new ModeListSetting("Огненный смерч триггеры:", new BooleanSetting[]{
            new BooleanSetting("Мало ХП у меня", false),
            new BooleanSetting("Мало ХП у противника", false),
            new BooleanSetting("Снесли Тотем мне", false),
            new BooleanSetting("Снёс Тотем я", false),
    })).setVisible(() -> flameEnabled.get());

    private final BooleanSetting auraEnabled = new BooleanSetting("Божья аура", false);
    public final ModeListSetting auraTriggers = (new ModeListSetting("Божья аура триггеры:", new BooleanSetting[]{
            new BooleanSetting("Мало ХП у меня", false),
            new BooleanSetting("Мало ХП у противника", false),
            new BooleanSetting("Снесли Тотем мне", false),
            new BooleanSetting("Снёс Тотем я", false),
    })).setVisible(() -> auraEnabled.get());

    private final BooleanSetting otrigaEnabled = new BooleanSetting("Зелье отрыжки", false);
    public final ModeListSetting otrigaTargets = (new ModeListSetting("Отрыжка цели:", new BooleanSetting[]{
            new BooleanSetting("Незеритовая броня", false),
            new BooleanSetting("Алмазная броня", false),
            new BooleanSetting("Элитры", false),
    })).setVisible(() -> otrigaEnabled.get());
    public final ModeListSetting otrigaTriggers = (new ModeListSetting("Отрыжка триггеры:", new BooleanSetting[]{
            new BooleanSetting("Мало ХП у меня", false),
            new BooleanSetting("Мало ХП у противника", false),
            new BooleanSetting("Снесли Тотем мне", false),
            new BooleanSetting("Снёс Тотем я", false),
    })).setVisible(() -> otrigaEnabled.get());

    private final BooleanSetting serkaEnabled = new BooleanSetting("Серная кислота", false);
    public final ModeListSetting serkaTargets = (new ModeListSetting("Серка цели:", new BooleanSetting[]{
            new BooleanSetting("Незеритовая броня", false),
            new BooleanSetting("Алмазная броня", false),
            new BooleanSetting("Элитры", false),
    })).setVisible(() -> serkaEnabled.get());
    public final ModeListSetting serkaTriggers = (new ModeListSetting("Серка триггеры:", new BooleanSetting[]{
            new BooleanSetting("Мало ХП у меня", false),
            new BooleanSetting("Мало ХП у противника", false),
            new BooleanSetting("Снесли Тотем мне", false),
            new BooleanSetting("Снёс Тотем я", false),
    })).setVisible(() -> serkaEnabled.get());

    private final BooleanSetting snowballEnabled = new BooleanSetting("Снежок заморозка", false);
    public final ModeListSetting snowballTargets = (new ModeListSetting("Снежок цели:", new BooleanSetting[]{
            new BooleanSetting("Незеритовая броня", false),
            new BooleanSetting("Алмазная броня", false),
            new BooleanSetting("Элитры", false),
    })).setVisible(() -> snowballEnabled.get());
    public final ModeListSetting snowballTriggers = (new ModeListSetting("Снежок триггеры:", new BooleanSetting[]{
            new BooleanSetting("Мало ХП у меня", false),
            new BooleanSetting("Мало ХП у противника", false),
            new BooleanSetting("Снесли Тотем мне", false),
            new BooleanSetting("Снёс Тотем я", false),
    })).setVisible(() -> snowballEnabled.get());

    private final BooleanSetting killerEnabled = new BooleanSetting("Зелье Киллера", false);
    public final ModeListSetting killerTargets = (new ModeListSetting("Киллер цели:", new BooleanSetting[]{
            new BooleanSetting("Незеритовая броня", false),
            new BooleanSetting("Алмазная броня", false),
            new BooleanSetting("Элитры", false),
    })).setVisible(() -> killerEnabled.get());
    public final ModeListSetting killerTriggers = (new ModeListSetting("Киллер триггеры:", new BooleanSetting[]{
            new BooleanSetting("Мало ХП у меня", false),
            new BooleanSetting("Мало ХП у противника", false),
            new BooleanSetting("Снесли Тотем мне", false),
            new BooleanSetting("Снёс Тотем я", false),
    })).setVisible(() -> killerEnabled.get());

    private final BooleanSetting medicEnabled = new BooleanSetting("Зелье Медика", false);
    public final ModeListSetting medicTargets = (new ModeListSetting("Медик цели:", new BooleanSetting[]{
            new BooleanSetting("Незеритовая броня", false),
            new BooleanSetting("Алмазная броня", false),
            new BooleanSetting("Элитры", false),
    })).setVisible(() -> medicEnabled.get());
    public final ModeListSetting medicTriggers = (new ModeListSetting("Медик триггеры:", new BooleanSetting[]{
            new BooleanSetting("Мало ХП у меня", false),
            new BooleanSetting("Мало ХП у противника", false),
            new BooleanSetting("Снесли Тотем мне", false),
            new BooleanSetting("Снёс Тотем я", false),
    })).setVisible(() -> medicEnabled.get());

    private final BooleanSetting winnerEnabled = new BooleanSetting("Зелье Победителя", false);
    public final ModeListSetting winnerTargets = (new ModeListSetting("Победитель цели:", new BooleanSetting[]{
            new BooleanSetting("Незеритовая броня", false),
            new BooleanSetting("Алмазная броня", false),
            new BooleanSetting("Элитры", false),
    })).setVisible(() -> winnerEnabled.get());
    public final ModeListSetting winnerTriggers = (new ModeListSetting("Победитель триггеры:", new BooleanSetting[]{
            new BooleanSetting("Мало ХП у меня", false),
            new BooleanSetting("Мало ХП у противника", false),
            new BooleanSetting("Снесли Тотем мне", false),
            new BooleanSetting("Снёс Тотем я", false),
    })).setVisible(() -> winnerEnabled.get());

    private final BooleanSetting agentEnabled = new BooleanSetting("Зелье Агента", false);
    public final ModeListSetting agentTargets = (new ModeListSetting("Агент цели:", new BooleanSetting[]{
            new BooleanSetting("Незеритовая броня", false),
            new BooleanSetting("Алмазная броня", false),
            new BooleanSetting("Элитры", false),
    })).setVisible(() -> agentEnabled.get());
    public final ModeListSetting agentTriggers = (new ModeListSetting("Агент триггеры:", new BooleanSetting[]{
            new BooleanSetting("Мало ХП у меня", false),
            new BooleanSetting("Мало ХП у противника", false),
            new BooleanSetting("Снесли Тотем мне", false),
            new BooleanSetting("Снёс Тотем я", false),
    })).setVisible(() -> agentEnabled.get());

    private final BooleanSetting xlopyshkaEnabled = new BooleanSetting("Хлопушка", false);
    public final ModeListSetting xlopyshkaTargets = (new ModeListSetting("Хлопушка цели:", new BooleanSetting[]{
            new BooleanSetting("Незеритовая броня", false),
            new BooleanSetting("Алмазная броня", false),
            new BooleanSetting("Элитры", false),
    })).setVisible(() -> xlopyshkaEnabled.get());
    public final ModeListSetting xlopyshkaTriggers = (new ModeListSetting("Хлопушка триггеры:", new BooleanSetting[]{
            new BooleanSetting("Мало ХП у меня", false),
            new BooleanSetting("Мало ХП у противника", false),
            new BooleanSetting("Снесли Тотем мне", false),
            new BooleanSetting("Снёс Тотем я", false),
    })).setVisible(() -> xlopyshkaEnabled.get());

    private final BooleanSetting svatvodaEnabled = new BooleanSetting("Святая вода", false);
    public final ModeListSetting svatvodaTargets = (new ModeListSetting("Святая вода цели:", new BooleanSetting[]{
            new BooleanSetting("Незеритовая броня", false),
            new BooleanSetting("Алмазная броня", false),
            new BooleanSetting("Элитры", false),
    })).setVisible(() -> svatvodaEnabled.get());
    public final ModeListSetting svatvodaTriggers = (new ModeListSetting("Святая вода триггеры:", new BooleanSetting[]{
            new BooleanSetting("Мало ХП у меня", false),
            new BooleanSetting("Мало ХП у противника", false),
            new BooleanSetting("Снесли Тотем мне", false),
            new BooleanSetting("Снёс Тотем я", false),
    })).setVisible(() -> svatvodaEnabled.get());

    private final BooleanSetting gnevkaEnabled = new BooleanSetting("Зелье гнева", false);
    public final ModeListSetting gnevkaTargets = (new ModeListSetting("Гнев цели:", new BooleanSetting[]{
            new BooleanSetting("Незеритовая броня", false),
            new BooleanSetting("Алмазная броня", false),
            new BooleanSetting("Элитры", false),
    })).setVisible(() -> gnevkaEnabled.get());
    public final ModeListSetting gnevkaTriggers = (new ModeListSetting("Гнев триггеры:", new BooleanSetting[]{
            new BooleanSetting("Мало ХП у меня", false),
            new BooleanSetting("Мало ХП у противника", false),
            new BooleanSetting("Снесли Тотем мне", false),
            new BooleanSetting("Снёс Тотем я", false),
    })).setVisible(() -> gnevkaEnabled.get());

    private final BooleanSetting paladinEnabled = new BooleanSetting("Зелье паладина", false);
    public final ModeListSetting paladinTargets = (new ModeListSetting("Паладин цели:", new BooleanSetting[]{
            new BooleanSetting("Незеритовая броня", false),
            new BooleanSetting("Алмазная броня", false),
            new BooleanSetting("Элитры", false),
    })).setVisible(() -> paladinEnabled.get());
    public final ModeListSetting paladinTriggers = (new ModeListSetting("Паладин триггеры:", new BooleanSetting[]{
            new BooleanSetting("Мало ХП у меня", false),
            new BooleanSetting("Мало ХП у противника", false),
            new BooleanSetting("Снесли Тотем мне", false),
            new BooleanSetting("Снёс Тотем я", false),
    })).setVisible(() -> paladinEnabled.get());

    private final BooleanSetting assasinEnabled = new BooleanSetting("Зелье ассасина", false);
    public final ModeListSetting assasinTargets = (new ModeListSetting("Ассасин цели:", new BooleanSetting[]{
            new BooleanSetting("Незеритовая броня", false),
            new BooleanSetting("Алмазная броня", false),
            new BooleanSetting("Элитры", false),
    })).setVisible(() -> assasinEnabled.get());
    public final ModeListSetting assasinTriggers = (new ModeListSetting("Ассасин триггеры:", new BooleanSetting[]{
            new BooleanSetting("Мало ХП у меня", false),
            new BooleanSetting("Мало ХП у противника", false),
            new BooleanSetting("Снесли Тотем мне", false),
            new BooleanSetting("Снёс Тотем я", false),
    })).setVisible(() -> assasinEnabled.get());

    private final BooleanSetting radiaciaEnabled = new BooleanSetting("Зелье радиации", false);
    public final ModeListSetting radiaciaTargets = (new ModeListSetting("Радиация цели:", new BooleanSetting[]{
            new BooleanSetting("Незеритовая броня", false),
            new BooleanSetting("Алмазная броня", false),
            new BooleanSetting("Элитры", false),
    })).setVisible(() -> radiaciaEnabled.get());
    public final ModeListSetting radiaciaTriggers = (new ModeListSetting("Радиация триггеры:", new BooleanSetting[]{
            new BooleanSetting("Мало ХП у меня", false),
            new BooleanSetting("Мало ХП у противника", false),
            new BooleanSetting("Снесли Тотем мне", false),
            new BooleanSetting("Снёс Тотем я", false),
    })).setVisible(() -> radiaciaEnabled.get());

    private final BooleanSetting snotvornoeEnabled = new BooleanSetting("Снотворное", false);
    public final ModeListSetting snotvornoeTargets = (new ModeListSetting("Снотворное цели:", new BooleanSetting[]{
            new BooleanSetting("Незеритовая броня", false),
            new BooleanSetting("Алмазная броня", false),
            new BooleanSetting("Элитры", false),
    })).setVisible(() -> snotvornoeEnabled.get());
    public final ModeListSetting snotvornoeTriggers = (new ModeListSetting("Снотворное триггеры:", new BooleanSetting[]{
            new BooleanSetting("Мало ХП у меня", false),
            new BooleanSetting("Мало ХП у противника", false),
            new BooleanSetting("Снесли Тотем мне", false),
            new BooleanSetting("Снёс Тотем я", false),
    })).setVisible(() -> snotvornoeEnabled.get());

    private final BooleanSetting flashEnabled = new BooleanSetting("Сопли флеша", false);
    public final ModeListSetting flashTargets = (new ModeListSetting("Флеш цели:", new BooleanSetting[]{
            new BooleanSetting("Незеритовая броня", false),
            new BooleanSetting("Алмазная броня", false),
            new BooleanSetting("Элитры", false),
    })).setVisible(() -> flashEnabled.get());
    public final ModeListSetting flashTriggers = (new ModeListSetting("Флеш триггеры:", new BooleanSetting[]{
            new BooleanSetting("Мало ХП у меня", false),
            new BooleanSetting("Мало ХП у противника", false),
            new BooleanSetting("Снесли Тотем мне", false),
            new BooleanSetting("Снёс Тотем я", false),
    })).setVisible(() -> flashEnabled.get());

    private final BooleanSetting plastEnabled = new BooleanSetting("Пласт", false);
    public final ModeListSetting plastTargets = (new ModeListSetting("Пласт цели:", new BooleanSetting[]{
            new BooleanSetting("Незеритовая броня", false),
            new BooleanSetting("Алмазная броня", false),
            new BooleanSetting("Элитры", false),
    })).setVisible(() -> plastEnabled.get());
    public final ModeListSetting plastTriggers = (new ModeListSetting("Пласт триггеры:", new BooleanSetting[]{
            new BooleanSetting("Мало ХП у меня", false),
            new BooleanSetting("Мало ХП у противника", false),
            new BooleanSetting("Снесли Тотем мне", false),
            new BooleanSetting("Снёс Тотем я", false),
    })).setVisible(() -> plastEnabled.get());

    private final SliderSetting myLowHpThreshold = (SliderSetting) new SliderSetting("Порог ХП", 6.0f, 1.0f, 20.0f, 0.5f).setVisible(this::hasAnyLowHpTrigger);
    private final SliderSetting enemyLowHpThreshold = (SliderSetting) new SliderSetting("Порог ХП противника", 6.0f, 1.0f, 20.0f, 0.5f).setVisible(this::hasAnyLowHpTrigger);

    private final ModeSetting checktouse = new ModeSetting("Поиск предмета", "Фантайм", "Фантайм", "Копии");
    private final ModeSetting itemsMode = new ModeSetting("Предметы", "Новые", "Новые", "Старые");
    private final CategorySetting commonItemsCategory = new CategorySetting("Основное");
    private final CategorySetting oldItemsCategory = (new CategorySetting("Старые предметы")).setVisible(this::isOldItemsMode);
    private final CategorySetting newItemsCategory = (new CategorySetting("Новые предметы")).setVisible(this::isNewItemsMode);

    InventoryUtil.Hand handUtil = new InventoryUtil.Hand();
    long delay;
    private boolean myTotemTriggered = false;
    private long myTotemTriggerTime = 0;
    private long lastTotemAutoMs = 0L;
    private boolean isTotemAnimating = false;

    private Map<String, PlayerTotemData> playersTotemData = new HashMap<>();
    private Map<String, Long> enemyTotemTriggers = new HashMap<>();
    private PlayerEntity lastTotemTarget = null;
    private PlayerEntity throwTarget = null;

    private long lastMyLowHpCheck = 0;
    private long lastEnemyLowHpCheck = 0;

    private static class PlayerTotemData {
        long lastTotemTime = 0L;
    }

    public AutoThrow() {
        configureItemModeVisibility();

        addSettings(checktouse, itemsMode,
                commonItemsCategory,
                disorientationEnabled, disorientationTargets, disorientationTriggers,
                trapEnabled, trapTargets, trapTriggers,
                blatantEnabled, blatantTargets, blatantTriggers,
                flameEnabled, flameTargets, flameTriggers,
                auraEnabled, auraTriggers,
                snowballEnabled, snowballTargets, snowballTriggers,
                plastEnabled, plastTargets, plastTriggers,
                oldItemsCategory,
                otrigaEnabled, otrigaTargets, otrigaTriggers,
                serkaEnabled, serkaTargets, serkaTriggers,
                killerEnabled, killerTargets, killerTriggers,
                medicEnabled, medicTargets, medicTriggers,
                winnerEnabled, winnerTargets, winnerTriggers,
                agentEnabled, agentTargets, agentTriggers,
                flashEnabled, flashTargets, flashTriggers,
                newItemsCategory,
                xlopyshkaEnabled, xlopyshkaTargets, xlopyshkaTriggers,
                svatvodaEnabled, svatvodaTargets, svatvodaTriggers,
                gnevkaEnabled, gnevkaTargets, gnevkaTriggers,
                paladinEnabled, paladinTargets, paladinTriggers,
                assasinEnabled, assasinTargets, assasinTriggers,
                radiaciaEnabled, radiaciaTargets, radiaciaTriggers,
                snotvornoeEnabled, snotvornoeTargets, snotvornoeTriggers,
                myLowHpThreshold, enemyLowHpThreshold);
    }

    private void configureItemModeVisibility() {
        otrigaEnabled.setVisible(this::isOldItemsMode);
        otrigaTargets.setVisible(() -> otrigaEnabled.get() && isOldItemsMode());
        otrigaTriggers.setVisible(() -> otrigaEnabled.get() && isOldItemsMode());

        serkaEnabled.setVisible(this::isOldItemsMode);
        serkaTargets.setVisible(() -> serkaEnabled.get() && isOldItemsMode());
        serkaTriggers.setVisible(() -> serkaEnabled.get() && isOldItemsMode());

        killerEnabled.setVisible(this::isOldItemsMode);
        killerTargets.setVisible(() -> killerEnabled.get() && isOldItemsMode());
        killerTriggers.setVisible(() -> killerEnabled.get() && isOldItemsMode());

        medicEnabled.setVisible(this::isOldItemsMode);
        medicTargets.setVisible(() -> medicEnabled.get() && isOldItemsMode());
        medicTriggers.setVisible(() -> medicEnabled.get() && isOldItemsMode());

        winnerEnabled.setVisible(this::isOldItemsMode);
        winnerTargets.setVisible(() -> winnerEnabled.get() && isOldItemsMode());
        winnerTriggers.setVisible(() -> winnerEnabled.get() && isOldItemsMode());

        agentEnabled.setVisible(this::isOldItemsMode);
        agentTargets.setVisible(() -> agentEnabled.get() && isOldItemsMode());
        agentTriggers.setVisible(() -> agentEnabled.get() && isOldItemsMode());

        flashEnabled.setVisible(this::isOldItemsMode);
        flashTargets.setVisible(() -> flashEnabled.get() && isOldItemsMode());
        flashTriggers.setVisible(() -> flashEnabled.get() && isOldItemsMode());

        xlopyshkaEnabled.setVisible(this::isNewItemsMode);
        xlopyshkaTargets.setVisible(() -> xlopyshkaEnabled.get() && isNewItemsMode());
        xlopyshkaTriggers.setVisible(() -> xlopyshkaEnabled.get() && isNewItemsMode());

        svatvodaEnabled.setVisible(this::isNewItemsMode);
        svatvodaTargets.setVisible(() -> svatvodaEnabled.get() && isNewItemsMode());
        svatvodaTriggers.setVisible(() -> svatvodaEnabled.get() && isNewItemsMode());

        gnevkaEnabled.setVisible(this::isNewItemsMode);
        gnevkaTargets.setVisible(() -> gnevkaEnabled.get() && isNewItemsMode());
        gnevkaTriggers.setVisible(() -> gnevkaEnabled.get() && isNewItemsMode());

        paladinEnabled.setVisible(this::isNewItemsMode);
        paladinTargets.setVisible(() -> paladinEnabled.get() && isNewItemsMode());
        paladinTriggers.setVisible(() -> paladinEnabled.get() && isNewItemsMode());

        assasinEnabled.setVisible(this::isNewItemsMode);
        assasinTargets.setVisible(() -> assasinEnabled.get() && isNewItemsMode());
        assasinTriggers.setVisible(() -> assasinEnabled.get() && isNewItemsMode());

        radiaciaEnabled.setVisible(this::isNewItemsMode);
        radiaciaTargets.setVisible(() -> radiaciaEnabled.get() && isNewItemsMode());
        radiaciaTriggers.setVisible(() -> radiaciaEnabled.get() && isNewItemsMode());

        snotvornoeEnabled.setVisible(this::isNewItemsMode);
        snotvornoeTargets.setVisible(() -> snotvornoeEnabled.get() && isNewItemsMode());
        snotvornoeTriggers.setVisible(() -> snotvornoeEnabled.get() && isNewItemsMode());
    }

    private boolean isOldItemsMode() {
        String mode = itemsMode.get();
        return mode != null && (mode.equalsIgnoreCase("Старые") || mode.equalsIgnoreCase("Old"));
    }

    private boolean isNewItemsMode() {
        return !isOldItemsMode();
    }

    private boolean isOldItemTriggersSetting(ModeListSetting settings) {
        return settings == otrigaTriggers || settings == serkaTriggers || settings == killerTriggers
                || settings == medicTriggers || settings == winnerTriggers || settings == agentTriggers
                || settings == flashTriggers;
    }

    private boolean isNewItemTriggersSetting(ModeListSetting settings) {
        return settings == xlopyshkaTriggers || settings == svatvodaTriggers || settings == gnevkaTriggers
                || settings == paladinTriggers || settings == assasinTriggers || settings == radiaciaTriggers
                || settings == snotvornoeTriggers;
    }

    private boolean isTriggersVisibleForCurrentItemsMode(ModeListSetting settings) {
        if (isOldItemTriggersSetting(settings)) {
            return isOldItemsMode();
        }
        if (isNewItemTriggersSetting(settings)) {
            return isNewItemsMode();
        }
        return true;
    }


    @Subscribe
    private void onUpdate(EventUpdate e) {
        if (myTotemTriggered) {
            handleMyTotemTrigger();
        }

        handleEnemyTotemTriggers();

        if (hasAnyLowHpTrigger()) {
            handleMyLowHp();
            handleEnemyLowHp();
        }

        if (disorientationEnabled.get()) handleDisorientation();
        if (trapEnabled.get()) handleTrap();
        if (blatantEnabled.get()) handleBlatant();
        if (flameEnabled.get()) handleFlame();
        if (auraEnabled.get()) handleAura();
        if (otrigaEnabled.get() && isOldItemsMode()) handleOtriga();
        if (serkaEnabled.get() && isOldItemsMode()) handleSerka();
        if (snowballEnabled.get()) handleSnowball();
        if (killerEnabled.get() && isOldItemsMode()) handleKiller();
        if (medicEnabled.get() && isOldItemsMode()) handleMedic();
        if (winnerEnabled.get() && isOldItemsMode()) handleWinner();
        if (agentEnabled.get() && isOldItemsMode()) handleAgent();
        if (xlopyshkaEnabled.get() && isNewItemsMode()) handleXlopyshka();
        if (svatvodaEnabled.get() && isNewItemsMode()) handleSvatvoda();
        if (gnevkaEnabled.get() && isNewItemsMode()) handleGnevka();
        if (paladinEnabled.get() && isNewItemsMode()) handlePaladin();
        if (assasinEnabled.get() && isNewItemsMode()) handleAssasin();
        if (radiaciaEnabled.get() && isNewItemsMode()) handleRadiacia();
        if (snotvornoeEnabled.get() && isNewItemsMode()) handleSnotvornoe();
        if (flashEnabled.get() && isOldItemsMode()) handleFlash();
        if (plastEnabled.get()) handlePlast();

        this.handUtil.handleItemChange(System.currentTimeMillis() - this.delay > 200L);
    }

    private boolean hasAnyTotemTrigger() {
        return hasTotemTrigger(disorientationTriggers) || hasTotemTrigger(trapTriggers) ||
                hasTotemTrigger(blatantTriggers) || hasTotemTrigger(flameTriggers) ||
                hasTotemTrigger(auraTriggers) || hasTotemTrigger(otrigaTriggers) ||
                hasTotemTrigger(serkaTriggers) || hasTotemTrigger(snowballTriggers) ||
                hasTotemTrigger(killerTriggers) || hasTotemTrigger(medicTriggers) ||
                hasTotemTrigger(winnerTriggers) || hasTotemTrigger(agentTriggers) ||
                hasTotemTrigger(xlopyshkaTriggers) || hasTotemTrigger(svatvodaTriggers) ||
                hasTotemTrigger(gnevkaTriggers) || hasTotemTrigger(paladinTriggers) ||
                hasTotemTrigger(assasinTriggers) || hasTotemTrigger(radiaciaTriggers) ||
                hasTotemTrigger(snotvornoeTriggers) || hasTotemTrigger(flashTriggers) ||
                hasTotemTrigger(plastTriggers);
    }

    private boolean hasTotemTrigger(ModeListSetting settings) {
        if (!isTriggersVisibleForCurrentItemsMode(settings)) {
            return false;
        }
        return (Boolean) settings.getValueByName("Снесли Тотем мне").get() ||
                (Boolean) settings.getValueByName("Снёс Тотем я").get();
    }

    private boolean hasAnyLowHpTrigger() {
        return hasLowHpTrigger(disorientationTriggers) || hasLowHpTrigger(trapTriggers) ||
                hasLowHpTrigger(blatantTriggers) || hasLowHpTrigger(flameTriggers) ||
                hasLowHpTrigger(auraTriggers) || hasLowHpTrigger(otrigaTriggers) ||
                hasLowHpTrigger(serkaTriggers) || hasLowHpTrigger(snowballTriggers) ||
                hasLowHpTrigger(killerTriggers) || hasLowHpTrigger(medicTriggers) ||
                hasLowHpTrigger(winnerTriggers) || hasLowHpTrigger(agentTriggers) ||
                hasLowHpTrigger(xlopyshkaTriggers) || hasLowHpTrigger(svatvodaTriggers) ||
                hasLowHpTrigger(gnevkaTriggers) || hasLowHpTrigger(paladinTriggers) ||
                hasLowHpTrigger(assasinTriggers) || hasLowHpTrigger(radiaciaTriggers) ||
                hasLowHpTrigger(snotvornoeTriggers) || hasLowHpTrigger(flashTriggers) ||
                hasLowHpTrigger(plastTriggers);
    }

    private boolean hasLowHpTrigger(ModeListSetting settings) {
        if (!isTriggersVisibleForCurrentItemsMode(settings)) {
            return false;
        }
        return (Boolean) settings.getValueByName("Мало ХП у меня").get() ||
                (Boolean) settings.getValueByName("Мало ХП у противника").get();
    }

    @Subscribe
    private void onPacketReceive(EventPacket e) {
        if (mc.world == null || mc.player == null) return;

        if (e.getPacket() instanceof SEntityStatusPacket) {
            SEntityStatusPacket packet = (SEntityStatusPacket) e.getPacket();
            if (packet.getOpCode() == 35) {
                Entity entity = packet.getEntity(mc.world);
                if (entity != null) {
                    if (entity == mc.player) {
                        triggerTotemEffect();
                    } else if (entity instanceof PlayerEntity) {
                        PlayerEntity player = (PlayerEntity) entity;
                        if (!FriendStorage.isFriend(player.getName().getString())) {
                            if (mc.player.getDistanceSq(player) <= FIXED_RANGE * FIXED_RANGE) {
                                triggerEnemyTotemEffect(player);
                            }
                        }
                    }
                }
            }
        }
    }

    private void handleMyLowHp() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastMyLowHpCheck < 1000) return;
        lastMyLowHpCheck = currentTime;
        if (mc.player == null) return;

        float currentHealth = mc.player.getHealth() + mc.player.getAbsorptionAmount();
        if (currentHealth <= myLowHpThreshold.get()) {
            throwTarget = findNearestThrowTarget();
            if (disorientationEnabled.get() && (Boolean) disorientationTriggers.getValueByName("Мало ХП у меня").get())
                throwDisorientation("Мало ХП у тебя");
            if (trapEnabled.get() && (Boolean) trapTriggers.getValueByName("Мало ХП у меня").get())
                throwTrap("Мало ХП у тебя");
            if (blatantEnabled.get() && (Boolean) blatantTriggers.getValueByName("Мало ХП у меня").get())
                throwBlatant("Мало ХП у тебя");
            if (flameEnabled.get() && (Boolean) flameTriggers.getValueByName("Мало ХП у меня").get())
                throwFlame("Мало ХП у тебя");
            if (auraEnabled.get() && (Boolean) auraTriggers.getValueByName("Мало ХП у меня").get())
                throwAura("Мало ХП у тебя");
            if (otrigaEnabled.get() && (Boolean) otrigaTriggers.getValueByName("Мало ХП у меня").get())
                throwOtriga("Мало ХП у тебя");
            if (serkaEnabled.get() && (Boolean) serkaTriggers.getValueByName("Мало ХП у меня").get())
                throwSerka("Мало ХП у тебя");
            if (snowballEnabled.get() && (Boolean) snowballTriggers.getValueByName("Мало ХП у меня").get())
                throwSnowball("Мало ХП у тебя");
            if (killerEnabled.get() && (Boolean) killerTriggers.getValueByName("Мало ХП у меня").get())
                throwKiller("Мало ХП у тебя");
            if (medicEnabled.get() && (Boolean) medicTriggers.getValueByName("Мало ХП у меня").get())
                throwMedic("Мало ХП у тебя");
            if (winnerEnabled.get() && (Boolean) winnerTriggers.getValueByName("Мало ХП у меня").get())
                throwWinner("Мало ХП у тебя");
            if (agentEnabled.get() && (Boolean) agentTriggers.getValueByName("Мало ХП у меня").get())
                throwAgent("Мало ХП у тебя");
            if (xlopyshkaEnabled.get() && (Boolean) xlopyshkaTriggers.getValueByName("Мало ХП у меня").get())
                throwXlopyshka("Мало ХП у тебя");
            if (svatvodaEnabled.get() && (Boolean) svatvodaTriggers.getValueByName("Мало ХП у меня").get())
                throwSvatvoda("Мало ХП у тебя");
            if (gnevkaEnabled.get() && (Boolean) gnevkaTriggers.getValueByName("Мало ХП у меня").get())
                throwGnevka("Мало ХП у тебя");
            if (paladinEnabled.get() && (Boolean) paladinTriggers.getValueByName("Мало ХП у меня").get())
                throwPaladin("Мало ХП у тебя");
            if (assasinEnabled.get() && (Boolean) assasinTriggers.getValueByName("Мало ХП у меня").get())
                throwAssasin("Мало ХП у тебя");
            if (radiaciaEnabled.get() && (Boolean) radiaciaTriggers.getValueByName("Мало ХП у меня").get())
                throwRadiacia("Мало ХП у тебя");
            if (snotvornoeEnabled.get() && (Boolean) snotvornoeTriggers.getValueByName("Мало ХП у меня").get())
                throwSnotvornoe("Мало ХП у тебя");
            if (flashEnabled.get() && (Boolean) flashTriggers.getValueByName("Мало ХП у меня").get())
                throwFlash("Мало ХП у тебя");
            if (plastEnabled.get() && (Boolean) plastTriggers.getValueByName("Мало ХП у меня").get())
                throwPlast("Мало ХП у тебя");
        }
    }

    private void handleEnemyLowHp() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastEnemyLowHpCheck < 500) return;
        lastEnemyLowHpCheck = currentTime;
        if (mc.world == null) return;

        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player == mc.player) continue;
            if (FriendStorage.isFriend(player.getName().getString())) continue;
            if (mc.player.getDistanceSq(player) > FIXED_RANGE * FIXED_RANGE) continue;

            float enemyHealth = player.getHealth() + player.getAbsorptionAmount();
            if (enemyHealth <= enemyLowHpThreshold.get()) {
                throwTarget = player;
                if (disorientationEnabled.get() && (Boolean) disorientationTriggers.getValueByName("Мало ХП у противника").get())
                    throwDisorientation("Мало ХП у врага");
                if (trapEnabled.get() && (Boolean) trapTriggers.getValueByName("Мало ХП у противника").get())
                    throwTrap("Мало ХП у врага");
                if (blatantEnabled.get() && (Boolean) blatantTriggers.getValueByName("Мало ХП у противника").get())
                    throwBlatant("Мало ХП у врага");
                if (flameEnabled.get() && (Boolean) flameTriggers.getValueByName("Мало ХП у противника").get())
                    throwFlame("Мало ХП у врага");
                if (auraEnabled.get() && (Boolean) auraTriggers.getValueByName("Мало ХП у противника").get())
                    throwAura("Мало ХП у врага");
                if (otrigaEnabled.get() && (Boolean) otrigaTriggers.getValueByName("Мало ХП у противника").get())
                    throwOtriga("Мало ХП у врага");
                if (serkaEnabled.get() && (Boolean) serkaTriggers.getValueByName("Мало ХП у противника").get())
                    throwSerka("Мало ХП у врага");
                if (snowballEnabled.get() && (Boolean) snowballTriggers.getValueByName("Мало ХП у противника").get())
                    throwSnowball("Мало ХП у врага");
                if (killerEnabled.get() && (Boolean) killerTriggers.getValueByName("Мало ХП у противника").get())
                    throwKiller("Мало ХП у врага");
                if (medicEnabled.get() && (Boolean) medicTriggers.getValueByName("Мало ХП у противника").get())
                    throwMedic("Мало ХП у врага");
                if (winnerEnabled.get() && (Boolean) winnerTriggers.getValueByName("Мало ХП у противника").get())
                    throwWinner("Мало ХП у врага");
                if (agentEnabled.get() && (Boolean) agentTriggers.getValueByName("Мало ХП у противника").get())
                    throwAgent("Мало ХП у врага");
                if (xlopyshkaEnabled.get() && (Boolean) xlopyshkaTriggers.getValueByName("Мало ХП у противника").get())
                    throwXlopyshka("Мало ХП у врага");
                if (svatvodaEnabled.get() && (Boolean) svatvodaTriggers.getValueByName("Мало ХП у противника").get())
                    throwSvatvoda("Мало ХП у врага");
                if (gnevkaEnabled.get() && (Boolean) gnevkaTriggers.getValueByName("Мало ХП у противника").get())
                    throwGnevka("Мало ХП у врага");
                if (paladinEnabled.get() && (Boolean) paladinTriggers.getValueByName("Мало ХП у противника").get())
                    throwPaladin("Мало ХП у врага");
                if (assasinEnabled.get() && (Boolean) assasinTriggers.getValueByName("Мало ХП у противника").get())
                    throwAssasin("Мало ХП у врага");
                if (radiaciaEnabled.get() && (Boolean) radiaciaTriggers.getValueByName("Мало ХП у противника").get())
                    throwRadiacia("Мало ХП у врага");
                if (snotvornoeEnabled.get() && (Boolean) snotvornoeTriggers.getValueByName("Мало ХП у противника").get())
                    throwSnotvornoe("Мало ХП у врага");
                if (flashEnabled.get() && (Boolean) flashTriggers.getValueByName("Мало ХП у противника").get())
                    throwFlash("Мало ХП у врага");
                if (plastEnabled.get() && (Boolean) plastTriggers.getValueByName("Мало ХП у противника").get())
                    throwPlast("Мало ХП у врага");
                break;
            }
        }
    }

    private void triggerEnemyTotemEffect(PlayerEntity target) {
        if (FriendStorage.isFriend(target.getName().getString())) return;
        String playerName = target.getName().getString();
        PlayerTotemData data = playersTotemData.computeIfAbsent(playerName, k -> new PlayerTotemData());
        long now = System.currentTimeMillis();
        if (now - data.lastTotemTime >= 2000) {
            data.lastTotemTime = now;
            enemyTotemTriggers.put(playerName, now);
            lastTotemTarget = target;
        }
    }

    private PlayerEntity findTarget(ModeListSetting settings) {
        if (mc.world == null || mc.player == null) {
            return null;
        }

        PlayerEntity bestTarget = null;
        double bestAngle = Double.MAX_VALUE;

        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player == mc.player) continue;
            if (FriendStorage.isFriend(player.getName().getString())) continue;
            if (mc.player.getDistanceSq(player) > FIXED_RANGE * FIXED_RANGE) continue;

            boolean hasElytra = hasElytra(player);
            ArmorType armorType = getArmorType(player);
            boolean shouldTarget = false;

            if ((Boolean) settings.getValueByName("Элитры").get() && hasElytra) shouldTarget = true;
            if ((Boolean) settings.getValueByName("Незеритовая броня").get() && armorType == ArmorType.NETHERITE && !hasElytra)
                shouldTarget = true;
            if ((Boolean) settings.getValueByName("Алмазная броня").get() && armorType == ArmorType.DIAMOND && !hasElytra)
                shouldTarget = true;

            if (shouldTarget) {
                double angle = getAngleTo(player);
                if (angle < bestAngle) {
                    bestAngle = angle;
                    bestTarget = player;
                }
            }
        }

        if (bestTarget != null) {
            throwTarget = bestTarget;
        }
        return bestTarget;
    }

    private double getAngleTo(PlayerEntity player) {
        Vector3d eyePos = mc.player.getEyePosition(1.0F);
        Vector3d targetPos = getPredictedThrowTargetPos(player);
        Vector3d diff = targetPos.subtract(eyePos);

        float yaw = (float) MathHelper.wrapDegrees(Math.toDegrees(Math.atan2(diff.z, diff.x)) - 90.0D);
        float pitch = (float) (-Math.toDegrees(Math.atan2(diff.y, Math.sqrt(diff.x * diff.x + diff.z * diff.z))));

        float yawDiff = MathHelper.wrapDegrees(yaw - mc.player.rotationYaw);
        float pitchDiff = MathHelper.wrapDegrees(pitch - mc.player.rotationPitch);
        return Math.sqrt(yawDiff * yawDiff + pitchDiff * pitchDiff);
    }

    private PlayerEntity getPlayerByName(String name) {
        if (mc.world == null || name == null) {
            return null;
        }

        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player.getName().getString().equalsIgnoreCase(name) && isValidThrowTarget(player)) {
                return player;
            }
        }
        return null;
    }

    private PlayerEntity findNearestThrowTarget() {
        if (mc.world == null || mc.player == null) {
            return null;
        }

        PlayerEntity nearest = null;
        double nearestDistance = Double.MAX_VALUE;
        for (PlayerEntity player : mc.world.getPlayers()) {
            if (!isValidThrowTarget(player)) {
                continue;
            }

            double distance = mc.player.getDistanceSq(player);
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearest = player;
            }
        }
        return nearest;
    }

    private boolean isValidThrowTarget(PlayerEntity player) {
        if (player == null || mc.player == null) {
            return false;
        }
        if (player == mc.player) {
            return false;
        }
        if (!player.isAlive() || player.isInvulnerable() || player.isCreative()) {
            return false;
        }
        if (FriendStorage.isFriend(player.getName().getString())) {
            return false;
        }
        return mc.player.getDistanceSq(player) <= FIXED_RANGE * FIXED_RANGE;
    }


    private void handleEnemyTotemTriggers() {
        long currentTime = System.currentTimeMillis();

        enemyTotemTriggers.entrySet().removeIf(entry -> {
            if (currentTime >= entry.getValue()) {
                PlayerEntity target = getPlayerByName(entry.getKey());
                if (target != null) {
                    throwTarget = target;
                }
                if (disorientationEnabled.get() && (Boolean) disorientationTriggers.getValueByName("Снёс Тотем я").get())
                    throwDisorientation("Снёс тотем врагу");
                if (trapEnabled.get() && (Boolean) trapTriggers.getValueByName("Снёс Тотем я").get())
                    throwTrap("Снёс тотем врагу");
                if (blatantEnabled.get() && (Boolean) blatantTriggers.getValueByName("Снёс Тотем я").get())
                    throwBlatant("Снёс тотем врагу");
                if (flameEnabled.get() && (Boolean) flameTriggers.getValueByName("Снёс Тотем я").get())
                    throwFlame("Снёс тотем врагу");
                if (auraEnabled.get() && (Boolean) auraTriggers.getValueByName("Снёс Тотем я").get())
                    throwAura("Снёс тотем врагу");
                if (otrigaEnabled.get() && (Boolean) otrigaTriggers.getValueByName("Снёс Тотем я").get())
                    throwOtriga("Снёс тотем врагу");
                if (serkaEnabled.get() && (Boolean) serkaTriggers.getValueByName("Снёс Тотем я").get())
                    throwSerka("Снёс тотем врагу");
                if (snowballEnabled.get() && (Boolean) snowballTriggers.getValueByName("Снёс Тотем я").get())
                    throwSnowball("Снёс тотем врагу");
                if (killerEnabled.get() && (Boolean) killerTriggers.getValueByName("Снёс Тотем я").get())
                    throwKiller("Снёс тотем врагу");
                if (medicEnabled.get() && (Boolean) medicTriggers.getValueByName("Снёс Тотем я").get())
                    throwMedic("Снёс тотем врагу");
                if (winnerEnabled.get() && (Boolean) winnerTriggers.getValueByName("Снёс Тотем я").get())
                    throwWinner("Снёс тотем врагу");
                if (agentEnabled.get() && (Boolean) agentTriggers.getValueByName("Снёс Тотем я").get())
                    throwAgent("Снёс тотем врагу");
                if (xlopyshkaEnabled.get() && (Boolean) xlopyshkaTriggers.getValueByName("Снёс Тотем я").get())
                    throwXlopyshka("Снёс тотем врагу");
                if (svatvodaEnabled.get() && (Boolean) svatvodaTriggers.getValueByName("Снёс Тотем я").get())
                    throwSvatvoda("Снёс тотем врагу");
                if (gnevkaEnabled.get() && (Boolean) gnevkaTriggers.getValueByName("Снёс Тотем я").get())
                    throwGnevka("Снёс тотем врагу");
                if (paladinEnabled.get() && (Boolean) paladinTriggers.getValueByName("Снёс Тотем я").get())
                    throwPaladin("Снёс тотем врагу");
                if (assasinEnabled.get() && (Boolean) assasinTriggers.getValueByName("Снёс Тотем я").get())
                    throwAssasin("Снёс тотем врагу");
                if (radiaciaEnabled.get() && (Boolean) radiaciaTriggers.getValueByName("Снёс Тотем я").get())
                    throwRadiacia("Снёс тотем врагу");
                if (snotvornoeEnabled.get() && (Boolean) snotvornoeTriggers.getValueByName("Снёс Тотем я").get())
                    throwSnotvornoe("Снёс тотем врагу");
                if (flashEnabled.get() && (Boolean) flashTriggers.getValueByName("Снёс Тотем я").get())
                    throwFlash("Снёс тотем врагу");
                if (plastEnabled.get() && (Boolean) plastTriggers.getValueByName("Снёс Тотем я").get())
                    throwPlast("Снёс тотем врагу");
                return true;
            }
            return false;
        });
    }

    private void triggerTotemEffect() {
        if (!myTotemTriggered && !isTotemAnimating) {
            long now = System.currentTimeMillis();
            if (now - lastTotemAutoMs >= 2000) {
                myTotemTriggered = true;
                myTotemTriggerTime = now;
                isTotemAnimating = true;
                lastTotemAutoMs = now;
            }
        }
    }

    private void handleMyTotemTrigger() {
        long currentTime = System.currentTimeMillis();

        if (currentTime >= myTotemTriggerTime) {
            throwTarget = findNearestThrowTarget();
            if (disorientationEnabled.get() && (Boolean) disorientationTriggers.getValueByName("Снесли Тотем мне").get())
                throwDisorientation("Снесли тотем тебе");
            if (trapEnabled.get() && (Boolean) trapTriggers.getValueByName("Снесли Тотем мне").get())
                throwTrap("Снесли тотем тебе");
            if (blatantEnabled.get() && (Boolean) blatantTriggers.getValueByName("Снесли Тотем мне").get())
                throwBlatant("Снесли тотем тебе");
            if (flameEnabled.get() && (Boolean) flameTriggers.getValueByName("Снесли Тотем мне").get())
                throwFlame("Снесли тотем тебе");
            if (auraEnabled.get() && (Boolean) auraTriggers.getValueByName("Снесли Тотем мне").get())
                throwAura("Снесли тотем тебе");
            if (otrigaEnabled.get() && (Boolean) otrigaTriggers.getValueByName("Снесли Тотем мне").get())
                throwOtriga("Снесли тотем тебе");
            if (serkaEnabled.get() && (Boolean) serkaTriggers.getValueByName("Снесли Тотем мне").get())
                throwSerka("Снесли тотем тебе");
            if (snowballEnabled.get() && (Boolean) snowballTriggers.getValueByName("Снесли Тотем мне").get())
                throwSnowball("Снесли тотем тебе");
            if (killerEnabled.get() && (Boolean) killerTriggers.getValueByName("Снесли Тотем мне").get())
                throwKiller("Снесли тотем тебе");
            if (medicEnabled.get() && (Boolean) medicTriggers.getValueByName("Снесли Тотем мне").get())
                throwMedic("Снесли тотем тебе");
            if (winnerEnabled.get() && (Boolean) winnerTriggers.getValueByName("Снесли Тотем мне").get())
                throwWinner("Снесли тотем тебе");
            if (agentEnabled.get() && (Boolean) agentTriggers.getValueByName("Снесли Тотем мне").get())
                throwAgent("Снесли тотем тебе");
            if (xlopyshkaEnabled.get() && (Boolean) xlopyshkaTriggers.getValueByName("Снесли Тотем мне").get())
                throwXlopyshka("Снесли тотем тебе");
            if (svatvodaEnabled.get() && (Boolean) svatvodaTriggers.getValueByName("Снесли Тотем мне").get())
                throwSvatvoda("Снесли тотем тебе");
            if (gnevkaEnabled.get() && (Boolean) gnevkaTriggers.getValueByName("Снесли Тотем мне").get())
                throwGnevka("Снесли тотем тебе");
            if (paladinEnabled.get() && (Boolean) paladinTriggers.getValueByName("Снесли Тотем мне").get())
                throwPaladin("Снесли тотем тебе");
            if (assasinEnabled.get() && (Boolean) assasinTriggers.getValueByName("Снесли Тотем мне").get())
                throwAssasin("Снесли тотем тебе");
            if (radiaciaEnabled.get() && (Boolean) radiaciaTriggers.getValueByName("Снесли Тотем мне").get())
                throwRadiacia("Снесли тотем тебе");
            if (snotvornoeEnabled.get() && (Boolean) snotvornoeTriggers.getValueByName("Снесли Тотем мне").get())
                throwSnotvornoe("Снесли тотем тебе");
            if (flashEnabled.get() && (Boolean) flashTriggers.getValueByName("Снесли Тотем мне").get())
                throwFlash("Снесли тотем тебе");
            if (plastEnabled.get() && (Boolean) plastTriggers.getValueByName("Снесли Тотем мне").get())
                throwPlast("Снесли тотем тебе");

            myTotemTriggered = false;
            isTotemAnimating = false;
        }
    }

    private void handleDisorientation() {
        PlayerEntity target = findTarget(disorientationTargets);
        if (target == null) return;
        throwDisorientation("Цель в радиусе");
    }

    private void handleTrap() {
        PlayerEntity target = findTarget(trapTargets);
        if (target == null) return;
        throwTrap("Цель в радиусе");
    }

    private void handleBlatant() {
        PlayerEntity target = findTarget(blatantTargets);
        if (target == null) return;
        throwBlatant("Цель в радиусе");
    }

    private void handleFlame() {
        PlayerEntity target = findTarget(flameTargets);
        if (target == null) return;
        throwFlame("Цель в радиусе");
    }

    private void handleAura() {
        if (!hasAllAuraEffects()) return;
        throwAura("Негативные эффекты");
    }

    private void handleOtriga() {
        PlayerEntity target = findTarget(otrigaTargets);
        if (target == null) return;
        throwOtriga("Цель в радиусе");
    }

    private void handleSerka() {
        PlayerEntity target = findTarget(serkaTargets);
        if (target == null) return;
        throwSerka("Цель в радиусе");
    }

    private void handleSnowball() {
        PlayerEntity target = findTarget(snowballTargets);
        if (target == null) return;
        throwSnowball("Цель в радиусе");
    }

    private void handleKiller() {
        PlayerEntity target = findTarget(killerTargets);
        if (target == null) return;
        throwKiller("Цель в радиусе");
    }

    private void handleMedic() {
        PlayerEntity target = findTarget(medicTargets);
        if (target == null) return;
        throwMedic("Цель в радиусе");
    }

    private void handleWinner() {
        PlayerEntity target = findTarget(winnerTargets);
        if (target == null) return;
        throwWinner("Цель в радиусе");
    }

    private void handleAgent() {
        PlayerEntity target = findTarget(agentTargets);
        if (target == null) return;
        throwAgent("Цель в радиусе");
    }

    private void handleXlopyshka() {
        PlayerEntity target = findTarget(xlopyshkaTargets);
        if (target == null) return;
        throwXlopyshka("Цель в радиусе");
    }

    private void handleSvatvoda() {
        PlayerEntity target = findTarget(svatvodaTargets);
        if (target == null) return;
        throwSvatvoda("Цель в радиусе");
    }

    private void handleGnevka() {
        PlayerEntity target = findTarget(gnevkaTargets);
        if (target == null) return;
        throwGnevka("Цель в радиусе");
    }

    private void handlePaladin() {
        PlayerEntity target = findTarget(paladinTargets);
        if (target == null) return;
        throwPaladin("Цель в радиусе");
    }

    private void handleAssasin() {
        PlayerEntity target = findTarget(assasinTargets);
        if (target == null) return;
        throwAssasin("Цель в радиусе");
    }

    private void handleRadiacia() {
        PlayerEntity target = findTarget(radiaciaTargets);
        if (target == null) return;
        throwRadiacia("Цель в радиусе");
    }

    private void handleSnotvornoe() {
        PlayerEntity target = findTarget(snotvornoeTargets);
        if (target == null) return;
        throwSnotvornoe("Цель в радиусе");
    }

    private void handleFlash() {
        PlayerEntity target = findTarget(flashTargets);
        if (target == null) return;
        throwFlash("Цель в радиусе");
    }

    private void handlePlast() {
        PlayerEntity target = findTarget(plastTargets);
        if (target == null) return;
        throwPlast("Цель в радиусе");
    }

    private boolean hasAllAuraEffects() {
        net.minecraft.potion.EffectInstance blindness = mc.player.getActivePotionEffect(net.minecraft.potion.Effects.BLINDNESS);
        net.minecraft.potion.EffectInstance glowing = mc.player.getActivePotionEffect(net.minecraft.potion.Effects.GLOWING);
        net.minecraft.potion.EffectInstance hunger = mc.player.getActivePotionEffect(net.minecraft.potion.Effects.HUNGER);
        net.minecraft.potion.EffectInstance slowness = mc.player.getActivePotionEffect(net.minecraft.potion.Effects.SLOWNESS);
        net.minecraft.potion.EffectInstance wither = mc.player.getActivePotionEffect(net.minecraft.potion.Effects.WITHER);

        boolean hasBlindness = blindness != null;
        boolean hasGlowing = glowing != null;
        boolean hasHunger = hunger != null && hunger.getAmplifier() >= 9;
        boolean hasSlowness = slowness != null && slowness.getAmplifier() >= 2;
        boolean hasWither = wither != null && wither.getAmplifier() >= 4;

        if (hasBlindness && hasGlowing && hasHunger && hasSlowness && hasWither) return true;

        net.minecraft.potion.EffectInstance poison = mc.player.getActivePotionEffect(net.minecraft.potion.Effects.POISON);
        net.minecraft.potion.EffectInstance weakness = mc.player.getActivePotionEffect(net.minecraft.potion.Effects.WEAKNESS);

        boolean hasPoison = poison != null && poison.getAmplifier() >= 1;
        boolean hasSlowness2 = slowness != null && slowness.getAmplifier() >= 3;
        boolean hasWeakness = weakness != null && weakness.getAmplifier() >= 2;
        boolean hasWither2 = wither != null && wither.getAmplifier() >= 4;

        return hasPoison && hasSlowness2 && hasWeakness && hasWither2;
    }

    private enum ArmorType {NONE, OTHER, DIAMOND, NETHERITE}

    private ArmorType getArmorType(PlayerEntity player) {
        int diamondPieces = 0;
        int netheritePieces = 0;
        int totalPieces = 0;

        for (EquipmentSlotType slot : new EquipmentSlotType[]{EquipmentSlotType.HEAD, EquipmentSlotType.CHEST, EquipmentSlotType.LEGS, EquipmentSlotType.FEET}) {
            ItemStack stack = player.getItemStackFromSlot(slot);
            if (slot == EquipmentSlotType.CHEST && stack.getItem() == Items.ELYTRA) continue;
            if (!stack.isEmpty() && stack.getItem() instanceof ArmorItem) {
                totalPieces++;
                ArmorItem armorItem = (ArmorItem) stack.getItem();
                ArmorMaterial material = (ArmorMaterial) armorItem.getArmorMaterial();
                if (material == ArmorMaterial.DIAMOND) diamondPieces++;
                else if (material == ArmorMaterial.NETHERITE) netheritePieces++;
            }
        }

        if (totalPieces == 0) return ArmorType.NONE;
        if (netheritePieces >= 2) return ArmorType.NETHERITE;
        if (diamondPieces >= 2) return ArmorType.DIAMOND;
        return ArmorType.OTHER;
    }

    private boolean hasElytra(PlayerEntity player) {
        ItemStack chestStack = player.getItemStackFromSlot(EquipmentSlotType.CHEST);
        return chestStack.getItem() == Items.ELYTRA;
    }

    private boolean throwDisorientation(String reason) {
        int hbSlot = -1;
        int invSlot = -1;

        if (checktouse.is("Фантайм")) {
            hbSlot = getItemForNBTContains("desorientation", true);
            invSlot = getItemForNBTContains("desorientation", false);
        } else if (checktouse.is("Копии")) {
            hbSlot = getItemForName("дезориентация", true);
            invSlot = getItemForName("дезориентация", false);
        }

        if (invSlot == -1 && hbSlot == -1) return false;

        if (!mc.player.getCooldownTracker().hasCooldown(Items.ENDER_EYE)) {
            int resultSlot = findAndTrowItem(hbSlot, invSlot);
            if (resultSlot > 8) mc.playerController.pickItem(resultSlot);
            return true;
        }
        return false;
    }

    private boolean throwTrap(String reason) {
        int hbSlot = -1;
        int invSlot = -1;

        if (checktouse.is("Фантайм")) {
            hbSlot = getItemForNBTContains("trap", true);
            invSlot = getItemForNBTContains("trap", false);
        } else if (checktouse.is("Копии")) {
            hbSlot = getTrapItem(true);
            invSlot = getTrapItem(false);
        }

        if (invSlot == -1 && hbSlot == -1) return false;

        if (!mc.player.getCooldownTracker().hasCooldown(Items.NETHERITE_SCRAP)) {
            int old = mc.player.inventory.currentItem;
            int resultSlot = findAndTrowItem(hbSlot, invSlot);
            if (resultSlot > 8) mc.playerController.pickItem(resultSlot);
            if (InventoryUtil.findEmptySlot(true) != -1 && mc.player.inventory.currentItem != old)
                mc.player.inventory.currentItem = old;
            return true;
        }
        return false;
    }

    private boolean throwBlatant(String reason) {
        int hbSlot = -1;
        int invSlot = -1;

        if (checktouse.is("Фантайм")) {
            hbSlot = getItemForNBTContains("sheerdust", true);
            invSlot = getItemForNBTContains("sheerdust", false);
        } else if (checktouse.is("Копии")) {
            hbSlot = getItemForName("явная", true);
            invSlot = getItemForName("явная", false);
        }

        if (invSlot == -1 && hbSlot == -1) return false;

        if (!mc.player.getCooldownTracker().hasCooldown(Items.SUGAR)) {
            int old = mc.player.inventory.currentItem;
            int resultSlot = findAndTrowItem(hbSlot, invSlot);
            if (resultSlot > 8) mc.playerController.pickItem(resultSlot);
            if (InventoryUtil.findEmptySlot(true) != -1 && mc.player.inventory.currentItem != old)
                mc.player.inventory.currentItem = old;
            return true;
        }
        return false;
    }

    private boolean throwFlame(String reason) {
        int hbSlot = -1;
        int invSlot = -1;

        if (checktouse.is("Фантайм")) {
            hbSlot = getItemForNBTContains("fierytornado", true);
            invSlot = getItemForNBTContains("fierytornado", false);
        } else if (checktouse.is("Копии")) {
            hbSlot = getItemForName("огненный", true);
            invSlot = getItemForName("огненный", false);
        }

        if (invSlot == -1 && hbSlot == -1) return false;

        if (!mc.player.getCooldownTracker().hasCooldown(Items.FIRE_CHARGE)) {
            int old = mc.player.inventory.currentItem;
            int resultSlot = findAndTrowItem(hbSlot, invSlot);
            if (resultSlot > 8) mc.playerController.pickItem(resultSlot);
            if (InventoryUtil.findEmptySlot(true) != -1 && mc.player.inventory.currentItem != old)
                mc.player.inventory.currentItem = old;
            return true;
        }
        return false;
    }

    private boolean throwAura(String reason) {
        int hbSlot = -1;
        int invSlot = -1;

        if (checktouse.is("Фантайм")) {
            hbSlot = getItemForNBTContains("godsaura", true);
            invSlot = getItemForNBTContains("godsaura", false);
        } else if (checktouse.is("Копии")) {
            hbSlot = getItemForName("аура", true);
            invSlot = getItemForName("аура", false);
        }

        if (invSlot == -1 && hbSlot == -1) return false;

        if (!mc.player.getCooldownTracker().hasCooldown(Items.PHANTOM_MEMBRANE)) {
            int resultSlot = findAndTrowItem(hbSlot, invSlot);
            if (resultSlot > 8) mc.playerController.pickItem(resultSlot);
            return true;
        }
        return false;
    }

    private boolean throwOtriga(String reason) {
        if (!isOldItemsMode()) return false;

        int hbSlot = -1;
        int invSlot = -1;

        if (checktouse.is("Фантайм")) {
            hbSlot = getItemForNBTContains("burp", true);
            invSlot = getItemForNBTContains("burp", false);
        } else if (checktouse.is("Копии")) {
            hbSlot = getItemForName("отрыжки", true);
            invSlot = getItemForName("отрыжки", false);
        }

        if (invSlot == -1 && hbSlot == -1) return false;

        if (!mc.player.getCooldownTracker().hasCooldown(Items.SPLASH_POTION)) {
            int old = mc.player.inventory.currentItem;
            int resultSlot = findAndTrowItem(hbSlot, invSlot);
            if (resultSlot > 8) mc.playerController.pickItem(resultSlot);
            if (InventoryUtil.findEmptySlot(true) != -1 && mc.player.inventory.currentItem != old)
                mc.player.inventory.currentItem = old;
            return true;
        }
        return false;
    }

    private boolean throwSerka(String reason) {
        if (!isOldItemsMode()) return false;

        int hbSlot = -1;
        int invSlot = -1;

        if (checktouse.is("Фантайм")) {
            hbSlot = getItemForNBTContains("acid", true);
            invSlot = getItemForNBTContains("acid", false);
        } else if (checktouse.is("Копии")) {
            hbSlot = getItemForName("серная", true);
            invSlot = getItemForName("серная", false);
        }

        if (invSlot == -1 && hbSlot == -1) return false;

        if (!mc.player.getCooldownTracker().hasCooldown(Items.SPLASH_POTION)) {
            int old = mc.player.inventory.currentItem;
            int resultSlot = findAndTrowItem(hbSlot, invSlot);
            if (resultSlot > 8) mc.playerController.pickItem(resultSlot);
            if (InventoryUtil.findEmptySlot(true) != -1 && mc.player.inventory.currentItem != old)
                mc.player.inventory.currentItem = old;
            return true;
        }
        return false;
    }

    private boolean throwSnowball(String reason) {
        int hbSlot = -1;
        int invSlot = -1;

        if (checktouse.is("Фантайм")) {
            hbSlot = getItem(Items.SNOWBALL, true);
            invSlot = getItem(Items.SNOWBALL, false);
        } else if (checktouse.is("Копии")) {
            hbSlot = getItemForName("снежок", true);
            invSlot = getItemForName("снежок", false);
            if (hbSlot == -1 && invSlot == -1) {
                hbSlot = getItem(Items.SNOWBALL, true);
                invSlot = getItem(Items.SNOWBALL, false);
            }
        }

        if (invSlot == -1 && hbSlot == -1) return false;

        if (!mc.player.getCooldownTracker().hasCooldown(Items.SNOWBALL)) {
            int old = mc.player.inventory.currentItem;
            int resultSlot = findAndTrowItem(hbSlot, invSlot);
            if (resultSlot > 8) mc.playerController.pickItem(resultSlot);
            if (InventoryUtil.findEmptySlot(true) != -1 && mc.player.inventory.currentItem != old)
                mc.player.inventory.currentItem = old;
            return true;
        }
        return false;
    }

    private boolean throwKiller(String reason) {
        if (!isOldItemsMode()) return false;

        int hbSlot = -1;
        int invSlot = -1;

        if (checktouse.is("Фантайм")) {
            hbSlot = getItemForNBTContains("potion-killer", true);
            invSlot = getItemForNBTContains("potion-killer", false);
        } else if (checktouse.is("Копии")) {
            hbSlot = getItemForName("киллера", true);
            invSlot = getItemForName("киллера", false);
        }

        if (invSlot == -1 && hbSlot == -1) return false;

        if (!mc.player.getCooldownTracker().hasCooldown(Items.SPLASH_POTION)) {
            int old = mc.player.inventory.currentItem;
            int resultSlot = findAndTrowItem(hbSlot, invSlot);
            if (resultSlot > 8) mc.playerController.pickItem(resultSlot);
            if (InventoryUtil.findEmptySlot(true) != -1 && mc.player.inventory.currentItem != old)
                mc.player.inventory.currentItem = old;
            return true;
        }
        return false;
    }

    private boolean throwMedic(String reason) {
        if (!isOldItemsMode()) return false;

        int hbSlot = -1;
        int invSlot = -1;

        if (checktouse.is("Фантайм")) {
            hbSlot = getItemForNBTContains("potion-medic", true);
            invSlot = getItemForNBTContains("potion-medic", false);
        } else if (checktouse.is("Копии")) {
            hbSlot = getItemForName("медика", true);
            invSlot = getItemForName("медика", false);
        }

        if (invSlot == -1 && hbSlot == -1) return false;

        if (!mc.player.getCooldownTracker().hasCooldown(Items.SPLASH_POTION)) {
            int old = mc.player.inventory.currentItem;
            int resultSlot = findAndTrowItem(hbSlot, invSlot);
            if (resultSlot > 8) mc.playerController.pickItem(resultSlot);
            if (InventoryUtil.findEmptySlot(true) != -1 && mc.player.inventory.currentItem != old)
                mc.player.inventory.currentItem = old;
            return true;
        }
        return false;
    }

    private boolean throwWinner(String reason) {
        if (!isOldItemsMode()) return false;

        int hbSlot = -1;
        int invSlot = -1;

        if (checktouse.is("Фантайм")) {
            hbSlot = getItemForNBTContains("potion-winner", true);
            invSlot = getItemForNBTContains("potion-winner", false);
        } else if (checktouse.is("Копии")) {
            hbSlot = getItemForName("победителя", true);
            invSlot = getItemForName("победителя", false);
        }

        if (invSlot == -1 && hbSlot == -1) return false;

        if (!mc.player.getCooldownTracker().hasCooldown(Items.SPLASH_POTION)) {
            int old = mc.player.inventory.currentItem;
            int resultSlot = findAndTrowItem(hbSlot, invSlot);
            if (resultSlot > 8) mc.playerController.pickItem(resultSlot);
            if (InventoryUtil.findEmptySlot(true) != -1 && mc.player.inventory.currentItem != old)
                mc.player.inventory.currentItem = old;
            return true;
        }
        return false;
    }

    private boolean throwAgent(String reason) {
        if (!isOldItemsMode()) return false;

        int hbSlot = -1;
        int invSlot = -1;

        if (checktouse.is("Фантайм")) {
            hbSlot = getItemForNBTContains("potion-agent", true);
            invSlot = getItemForNBTContains("potion-agent", false);
        } else if (checktouse.is("Копии")) {
            hbSlot = getItemForName("агента", true);
            invSlot = getItemForName("агента", false);
        }

        if (invSlot == -1 && hbSlot == -1) return false;

        if (!mc.player.getCooldownTracker().hasCooldown(Items.SPLASH_POTION)) {
            int old = mc.player.inventory.currentItem;
            int resultSlot = findAndTrowItem(hbSlot, invSlot);
            if (resultSlot > 8) mc.playerController.pickItem(resultSlot);
            if (InventoryUtil.findEmptySlot(true) != -1 && mc.player.inventory.currentItem != old)
                mc.player.inventory.currentItem = old;
            return true;
        }
        return false;
    }

    private boolean throwXlopyshka(String reason) {
        if (!isNewItemsMode()) return false;

        int hbSlot = getItemForName("хлопушка", true);
        int invSlot = getItemForName("хлопушка", false);

        if (invSlot == -1 && hbSlot == -1) return false;

        if (!mc.player.getCooldownTracker().hasCooldown(Items.SPLASH_POTION)) {
            int old = mc.player.inventory.currentItem;
            int resultSlot = findAndTrowItem(hbSlot, invSlot);
            if (resultSlot > 8) mc.playerController.pickItem(resultSlot);
            if (InventoryUtil.findEmptySlot(true) != -1 && mc.player.inventory.currentItem != old)
                mc.player.inventory.currentItem = old;
            return true;
        }
        return false;
    }

    private boolean throwSvatvoda(String reason) {
        if (!isNewItemsMode()) return false;

        int hbSlot = getItemForName("святая вода", true);
        int invSlot = getItemForName("святая вода", false);

        if (invSlot == -1 && hbSlot == -1) return false;

        if (!mc.player.getCooldownTracker().hasCooldown(Items.SPLASH_POTION)) {
            int old = mc.player.inventory.currentItem;
            int resultSlot = findAndTrowItem(hbSlot, invSlot);
            if (resultSlot > 8) mc.playerController.pickItem(resultSlot);
            if (InventoryUtil.findEmptySlot(true) != -1 && mc.player.inventory.currentItem != old)
                mc.player.inventory.currentItem = old;
            return true;
        }
        return false;
    }

    private boolean throwGnevka(String reason) {
        if (!isNewItemsMode()) return false;

        int hbSlot = getItemForName("зелье гнева", true);
        int invSlot = getItemForName("зелье гнева", false);

        if (invSlot == -1 && hbSlot == -1) return false;

        if (!mc.player.getCooldownTracker().hasCooldown(Items.SPLASH_POTION)) {
            int old = mc.player.inventory.currentItem;
            int resultSlot = findAndTrowItem(hbSlot, invSlot);
            if (resultSlot > 8) mc.playerController.pickItem(resultSlot);
            if (InventoryUtil.findEmptySlot(true) != -1 && mc.player.inventory.currentItem != old)
                mc.player.inventory.currentItem = old;
            return true;
        }
        return false;
    }

    private boolean throwPaladin(String reason) {
        if (!isNewItemsMode()) return false;

        int hbSlot = getItemForName("палладина", true);
        int invSlot = getItemForName("палладина", false);

        if (hbSlot == -1 && invSlot == -1) {
            hbSlot = getItemForName("паладина", true);
            invSlot = getItemForName("паладина", false);
        }

        if (invSlot == -1 && hbSlot == -1) return false;

        if (!mc.player.getCooldownTracker().hasCooldown(Items.SPLASH_POTION)) {
            int old = mc.player.inventory.currentItem;
            int resultSlot = findAndTrowItem(hbSlot, invSlot);
            if (resultSlot > 8) mc.playerController.pickItem(resultSlot);
            if (InventoryUtil.findEmptySlot(true) != -1 && mc.player.inventory.currentItem != old)
                mc.player.inventory.currentItem = old;
            return true;
        }
        return false;
    }

    private boolean throwAssasin(String reason) {
        if (!isNewItemsMode()) return false;

        int hbSlot = getItemForName("ассасина", true);
        int invSlot = getItemForName("ассасина", false);

        if (invSlot == -1 && hbSlot == -1) return false;

        if (!mc.player.getCooldownTracker().hasCooldown(Items.SPLASH_POTION)) {
            int old = mc.player.inventory.currentItem;
            int resultSlot = findAndTrowItem(hbSlot, invSlot);
            if (resultSlot > 8) mc.playerController.pickItem(resultSlot);
            if (InventoryUtil.findEmptySlot(true) != -1 && mc.player.inventory.currentItem != old)
                mc.player.inventory.currentItem = old;
            return true;
        }
        return false;
    }

    private boolean throwRadiacia(String reason) {
        if (!isNewItemsMode()) return false;

        int hbSlot = getItemForName("радиации", true);
        int invSlot = getItemForName("радиации", false);

        if (invSlot == -1 && hbSlot == -1) return false;

        if (!mc.player.getCooldownTracker().hasCooldown(Items.SPLASH_POTION)) {
            int old = mc.player.inventory.currentItem;
            int resultSlot = findAndTrowItem(hbSlot, invSlot);
            if (resultSlot > 8) mc.playerController.pickItem(resultSlot);
            if (InventoryUtil.findEmptySlot(true) != -1 && mc.player.inventory.currentItem != old)
                mc.player.inventory.currentItem = old;
            return true;
        }
        return false;
    }

    private boolean throwSnotvornoe(String reason) {
        if (!isNewItemsMode()) return false;

        int hbSlot = getItemForName("снотворное", true);
        int invSlot = getItemForName("снотворное", false);

        if (invSlot == -1 && hbSlot == -1) return false;

        if (!mc.player.getCooldownTracker().hasCooldown(Items.SPLASH_POTION)) {
            int old = mc.player.inventory.currentItem;
            int resultSlot = findAndTrowItem(hbSlot, invSlot);
            if (resultSlot > 8) mc.playerController.pickItem(resultSlot);
            if (InventoryUtil.findEmptySlot(true) != -1 && mc.player.inventory.currentItem != old)
                mc.player.inventory.currentItem = old;
            return true;
        }
        return false;
    }

    private boolean throwFlash(String reason) {
        if (!isOldItemsMode()) return false;

        int hbSlot = -1;
        int invSlot = -1;

        if (checktouse.is("Фантайм")) {
            hbSlot = getItemForNBTContains("flash", true);
            invSlot = getItemForNBTContains("flash", false);
        } else if (checktouse.is("Копии")) {
            hbSlot = getItemForName("флеш", true);
            invSlot = getItemForName("флеш", false);
            if (hbSlot == -1 && invSlot == -1) {
                hbSlot = getItemForName("сопли", true);
                invSlot = getItemForName("сопли", false);
            }
        }

        if (invSlot == -1 && hbSlot == -1) return false;

        if (!mc.player.getCooldownTracker().hasCooldown(Items.SPLASH_POTION)) {
            int old = mc.player.inventory.currentItem;
            int resultSlot = findAndTrowItem(hbSlot, invSlot);
            if (resultSlot > 8) mc.playerController.pickItem(resultSlot);
            if (InventoryUtil.findEmptySlot(true) != -1 && mc.player.inventory.currentItem != old)
                mc.player.inventory.currentItem = old;
            return true;
        }
        return false;
    }

    private boolean throwPlast(String reason) {
        int hbSlot = -1;
        int invSlot = -1;

        if (checktouse.is("Фантайм")) {
            hbSlot = getItemForNBTContains("stratum", true);
            invSlot = getItemForNBTContains("stratum", false);
        } else if (checktouse.is("Копии")) {
            hbSlot = getItemForName("пласт", true);
            invSlot = getItemForName("пласт", false);
        }

        if (invSlot == -1 && hbSlot == -1) return false;

        if (!mc.player.getCooldownTracker().hasCooldown(Items.DRIED_KELP)) {
            int old = mc.player.inventory.currentItem;
            int resultSlot = findAndTrowItem(hbSlot, invSlot);
            if (resultSlot > 8) mc.playerController.pickItem(resultSlot);
            if (InventoryUtil.findEmptySlot(true) != -1 && mc.player.inventory.currentItem != old)
                mc.player.inventory.currentItem = old;
            return true;
        }
        return false;
    }

    private int findAndTrowItem(int hbSlot, int invSlot) {
        int originalSlot = mc.player.inventory.currentItem;
        boolean isFreakTime = isConnectedToSkyTime();
        Vector2f throwRotation = getThrowRotation(resolveThrowTarget());

        if (hbSlot != -1) {
            mc.player.connection.sendPacket(new CHeldItemChangePacket(hbSlot));
            applyThrowRotation(throwRotation);
            mc.player.connection.sendPacket(new CPlayerTryUseItemPacket(Hand.MAIN_HAND));
            if (isFreakTime && trapEnabled.get()) {
                float rayYaw = throwRotation != null ? throwRotation.x : mc.player.rotationYaw;
                float rayPitch = throwRotation != null ? throwRotation.y : mc.player.rotationPitch;
                BlockRayTraceResult airplace = (BlockRayTraceResult) rayTrace(4.0F, rayYaw, rayPitch, mc.player);
                mc.player.connection.sendPacket(new CPlayerTryUseItemOnBlockPacket(Hand.MAIN_HAND, airplace));
            }
            mc.player.swingArm(Hand.MAIN_HAND);
            this.delay = System.currentTimeMillis();
            mc.player.connection.sendPacket(new CHeldItemChangePacket(originalSlot));
            return hbSlot;
        } else if (invSlot == -1) {
            return -1;
        } else {
            this.handUtil.setOriginalSlot(mc.player.inventory.currentItem);
            mc.playerController.pickItem(invSlot);
            mc.player.connection.sendPacket(new CCloseWindowPacket(0));
            applyThrowRotation(throwRotation);
            mc.player.connection.sendPacket(new CPlayerTryUseItemPacket(Hand.MAIN_HAND));
            if (isFreakTime && trapEnabled.get()) {
                float rayYaw = throwRotation != null ? throwRotation.x : mc.player.rotationYaw;
                float rayPitch = throwRotation != null ? throwRotation.y : mc.player.rotationPitch;
                BlockRayTraceResult airplace = (BlockRayTraceResult) rayTrace(4.0F, rayYaw, rayPitch, mc.player);
                mc.player.connection.sendPacket(new CPlayerTryUseItemOnBlockPacket(Hand.MAIN_HAND, airplace));
            }
            mc.player.swingArm(Hand.MAIN_HAND);
            this.delay = System.currentTimeMillis();
            return invSlot;
        }
    }

    private PlayerEntity resolveThrowTarget() {
        if (isValidThrowTarget(throwTarget)) {
            return throwTarget;
        }
        if (isValidThrowTarget(lastTotemTarget)) {
            throwTarget = lastTotemTarget;
            return throwTarget;
        }

        throwTarget = findNearestThrowTarget();
        return throwTarget;
    }

    private Vector2f getThrowRotation(PlayerEntity target) {
        if (!isValidThrowTarget(target)) {
            return null;
        }

        Vector3d eyePos = mc.player.getEyePosition(1.0F);
        Vector3d targetPos = getPredictedThrowTargetPos(target);
        Vector3d diff = targetPos.subtract(eyePos);

        float yaw = (float) MathHelper.wrapDegrees(Math.toDegrees(Math.atan2(diff.z, diff.x)) - 90.0D);
        float horizontalDist = (float) Math.sqrt(diff.x * diff.x + diff.z * diff.z);
        float pitch = (float) (-Math.toDegrees(Math.atan2(diff.y, horizontalDist)));

        pitch -= MathHelper.clamp(mc.player.getDistance(target) * 0.35F, 0.0F, 6.0F);
        pitch = MathHelper.clamp(pitch, -89.0F, 89.0F);

        float gcd = SensUtils.getGCDValue();
        if (gcd > 0.0F) {
            yaw -= (yaw - mc.player.rotationYaw) % gcd;
            pitch -= (pitch - mc.player.rotationPitch) % gcd;
        }

        return new Vector2f(yaw, pitch);
    }

    private Vector3d getPredictedThrowTargetPos(PlayerEntity target) {
        double predictionFactor = 2.0D;
        double predX = target.getPosX() + (target.getPosX() - target.prevPosX) * predictionFactor;
        double predY = target.getPosY() + target.getHeight() * 0.55D + (target.getPosY() - target.prevPosY) * 0.5D;
        double predZ = target.getPosZ() + (target.getPosZ() - target.prevPosZ) * predictionFactor;
        return new Vector3d(predX, predY, predZ);
    }

    private void applyThrowRotation(Vector2f rotation) {
        if (rotation == null || mc.player == null || mc.player.connection == null) {
            return;
        }

        mc.player.connection.sendPacket(new CPlayerPacket.RotationPacket(rotation.x, rotation.y, mc.player.isOnGround()));
        mc.player.rotationYawHead = rotation.x;
        mc.player.rotationPitchHead = rotation.y;
        mc.player.renderYawOffset = rotation.x;
    }

    private int getTrapItem(boolean inHotBar) {
        int firstSlot = inHotBar ? 0 : 9;
        int lastSlot = inHotBar ? 9 : 36;
        for (int i = firstSlot; i < lastSlot; i++) {
            ItemStack itemStack = mc.player.inventory.getStackInSlot(i);
            if (itemStack.getItem() instanceof AirItem) continue;

            String displayName = TextFormatting.getTextWithoutFormattingCodes(itemStack.getDisplayName().getString());
            if (displayName != null && displayName.toLowerCase().contains("трапка")) return i;

            List<ITextComponent> tooltip = itemStack.getTooltip(mc.player, mc.gameSettings.advancedItemTooltips ? ITooltipFlag.TooltipFlags.ADVANCED : ITooltipFlag.TooltipFlags.NORMAL);
            for (ITextComponent line : tooltip) {
                String tooltipText = TextFormatting.getTextWithoutFormattingCodes(line.getString()).toLowerCase();
                if (tooltipText.contains("создаёт клетку вокруг 3х3") || tooltipText.contains("нельзя сломать 15 секунд"))
                    return i;
            }
        }
        return -1;
    }

    private int getItemForName(String name, boolean inHotBar) {
        int firstSlot = inHotBar ? 0 : 9;
        int lastSlot = inHotBar ? 9 : 36;
        for (int i = firstSlot; i < lastSlot; i++) {
            ItemStack itemStack = mc.player.inventory.getStackInSlot(i);
            if (itemStack.getItem() instanceof AirItem) continue;

            String displayName = TextFormatting.getTextWithoutFormattingCodes(itemStack.getDisplayName().getString());
            if (displayName != null && displayName.toLowerCase().contains(name)) return i;
        }
        return -1;
    }

    private int getItemForNBTContains(String nbt, boolean hotbar) {
        int start = hotbar ? 0 : 9;
        int end = hotbar ? 9 : 36;
        for (int i = start; i < end; i++) {
            ItemStack stack = mc.player.inventory.getStackInSlot(i);
            if (!(stack.getItem() instanceof AirItem) && stack.hasTag()) {
                String tag = stack.getTag().toString();
                if (tag != null && tag.toLowerCase().contains(nbt.toLowerCase())) return i;
            }
        }
        return -1;
    }

    private int getItem(net.minecraft.item.Item input, boolean inHotBar) {
        int firstSlot = inHotBar ? 0 : 9;
        int lastSlot = inHotBar ? 9 : 36;
        for (int i = firstSlot; i < lastSlot; i++) {
            ItemStack itemStack = mc.player.inventory.getStackInSlot(i);
            if (itemStack.getItem() instanceof AirItem) continue;
            if (itemStack.getItem() == input) return i;
        }
        return -1;
    }

    private boolean isConnectedToSkyTime() {
        return mc.getCurrentServerData() != null && mc.getCurrentServerData().serverIP.toLowerCase().contains("skytime");
    }

    private Object rayTrace(float distance, float yaw, float pitch, Object player) {
        return null;
    }

    @Override
    public boolean onEnable() {
        super.onEnable();
        myTotemTriggered = false;
        myTotemTriggerTime = 0;
        lastTotemAutoMs = 0L;
        isTotemAnimating = false;
        playersTotemData.clear();
        enemyTotemTriggers.clear();
        lastTotemTarget = null;
        lastMyLowHpCheck = 0;
        lastEnemyLowHpCheck = 0;
        return false;
    }

    @Override
    public boolean onDisable() {
        super.onDisable();
        myTotemTriggered = false;
        myTotemTriggerTime = 0;
        lastTotemAutoMs = 0L;
        isTotemAnimating = false;
        playersTotemData.clear();
        enemyTotemTriggers.clear();
        lastTotemTarget = null;
        lastMyLowHpCheck = 0;
        lastEnemyLowHpCheck = 0;
        return false;
    }
}
