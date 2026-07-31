package xd.harm.modules.impl.player;

import xd.harm.events.input.EventInput;
import xd.harm.events.input.EventKey;
import xd.harm.events.movement.EventMotion;
import xd.harm.events.network.EventPacket;
import xd.harm.events.render.EventDisplay;
import xd.harm.events.world.EventUpdate;
import xd.harm.modules.api.Notify;
import xd.harm.Harmony;
import xd.harm.modules.impl.misc.Notifications;
import xd.harm.ui.notify.NotificationManager;
import com.google.common.eventbus.Subscribe;
import xd.harm.modules.api.Category;
import xd.harm.modules.api.Module;
import xd.harm.modules.api.ModuleRegister;
import xd.harm.modules.settings.impl.BindSetting;
import xd.harm.modules.settings.impl.BooleanSetting;
import xd.harm.modules.settings.impl.CategorySetting;
import xd.harm.modules.settings.impl.ModeSetting;
import xd.harm.modules.settings.impl.ModeListSetting;
import xd.harm.modules.impl.render.Theme;
import xd.harm.utils.projections.ProjectionUtil;
import xd.harm.utils.render.color.ColorUtils;
import xd.harm.utils.render.font.Fonts;
import xd.harm.utils.render.rect.RenderUtility;
import xd.harm.utils.player.InventoryUtil;
import xd.harm.utils.math.StopWatch;
import net.minecraft.block.Blocks;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.monster.ZombieEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.container.ClickType;
import net.minecraft.item.AirItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.SplashPotionItem;
import net.minecraft.network.IPacket;
import net.minecraft.network.play.client.*;
import net.minecraft.network.play.server.*;
import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.potion.PotionUtils;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector2f;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.math.vector.Vector4f;
import net.minecraft.util.text.Color;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@ModuleRegister(name = "FtHelper", category = Category.Player, desc = "Хелпер для Фантайма")
public class FTHelper extends Module {

    private final BooleanSetting smallZombies = new BooleanSetting("Маленькие Зомби", false);
    public final ModeListSetting options = new ModeListSetting("Опции помощника:", new BooleanSetting[]{
            new BooleanSetting("Использование по бинду", true),
            new BooleanSetting("Автоматически забирать ключ", false),
            smallZombies,
    });

    private final ModeSetting checktouse = (new ModeSetting("Поиск предмета", "Фантайм", "Фантайм", "Копии")).setVisible(() -> (Boolean)this.options.getValueByName("Использование по бинду").get());
    private final BooleanSetting onlypvp = (new BooleanSetting("Не юзать если никого нет", false)).setVisible(() -> (Boolean)this.options.getValueByName("Использование по бинду").get());
    private final ModeSetting itemsMode = (new ModeSetting("Предметы", "Новые", "Новые", "Старые")).setVisible(() -> (Boolean)this.options.getValueByName("Использование по бинду").get());
    private final CategorySetting combatItemsCategory = (new CategorySetting("Основное")).setVisible(() -> (Boolean)this.options.getValueByName("Использование по бинду").get());
    private final CategorySetting miscCategory = (new CategorySetting("Прочее")).setVisible(() -> (Boolean)this.options.getValueByName("Использование по бинду").get());
    private final CategorySetting potionsCategory = (new CategorySetting("Зелья")).setVisible(() -> (Boolean)this.options.getValueByName("Использование по бинду").get());
    private final CategorySetting razbafCategory = (new CategorySetting("Разбаф")).setVisible(() -> (Boolean)this.options.getValueByName("Использование по бинду").get());

    public final BindSetting disorientationKey = (new BindSetting("Дезориентация", -1)).setVisible(() -> (Boolean)this.options.getValueByName("Использование по бинду").get());
    public final BindSetting trapKey = (new BindSetting("Трапка", -1)).setVisible(() -> (Boolean)this.options.getValueByName("Использование по бинду").get());
    public final BindSetting blatantKey = (new BindSetting("Явная Пыль", -1)).setVisible(() -> (Boolean)this.options.getValueByName("Использование по бинду").get());
    public final BindSetting shulkerKey = (new BindSetting("Шалкер", -1)).setVisible(() -> (Boolean)this.options.getValueByName("Использование по бинду").get());
    public final BindSetting bowKey = (new BindSetting("Заряженный Арбалет", -1)).setVisible(() -> (Boolean)this.options.getValueByName("Использование по бинду").get());
    public final BindSetting plastKey = (new BindSetting("Пласт", -1)).setVisible(() -> (Boolean)this.options.getValueByName("Использование по бинду").get());
    public final BindSetting flameKey = (new BindSetting("Огненный Смерч", -1)).setVisible(() -> (Boolean)this.options.getValueByName("Использование по бинду").get());
    public final BindSetting auraKey = (new BindSetting("Божья Аура", -1)).setVisible(() -> (Boolean)this.options.getValueByName("Использование по бинду").get());
    public final BindSetting snowballKey = (new BindSetting("Снежок Заморозка", -1)).setVisible(() -> (Boolean)this.options.getValueByName("Использование по бинду").get());

    public final BindSetting otrigaKey = (new BindSetting("Зелье Отрыжки", -1)).setVisible(() -> (Boolean)this.options.getValueByName("Использование по бинду").get() && !this.itemsMode.is("Новые"));
    public final BindSetting flashKey = (new BindSetting("Моча Флеша", -1)).setVisible(() -> (Boolean)this.options.getValueByName("Использование по бинду").get() && !this.itemsMode.is("Новые"));
    public final BindSetting serkaKey = (new BindSetting("Серная кислота", -1)).setVisible(() -> (Boolean)this.options.getValueByName("Использование по бинду").get() && !this.itemsMode.is("Новые"));

    public final BindSetting xlopyshkaKey = (new BindSetting("Хлопушка", -1)).setVisible(() -> (Boolean)this.options.getValueByName("Использование по бинду").get() && !this.itemsMode.is("Старые"));
    public final BindSetting svatvodaKey = (new BindSetting("Святая Вода", -1)).setVisible(() -> (Boolean)this.options.getValueByName("Использование по бинду").get() && !this.itemsMode.is("Старые"));
    public final BindSetting gnevkaKey = (new BindSetting("Зелье Гнева", -1)).setVisible(() -> (Boolean)this.options.getValueByName("Использование по бинду").get() && !this.itemsMode.is("Старые"));
    public final BindSetting paladinKey = (new BindSetting("Зелье Паладина", -1)).setVisible(() -> (Boolean)this.options.getValueByName("Использование по бинду").get() && !this.itemsMode.is("Старые"));
    public final BindSetting assasinKey = (new BindSetting("Зелье Ассасина", -1)).setVisible(() -> (Boolean)this.options.getValueByName("Использование по бинду").get() && !this.itemsMode.is("Старые"));
    public final BindSetting radiaciaKey = (new BindSetting("Зелье Радиации", -1)).setVisible(() -> (Boolean)this.options.getValueByName("Использование по бинду").get() && !this.itemsMode.is("Старые"));
    public final BindSetting snotvornoeKey = (new BindSetting("Снотворное", -1)).setVisible(() -> (Boolean)this.options.getValueByName("Использование по бинду").get() && !this.itemsMode.is("Старые"));

    public final BindSetting killerKey = (new BindSetting("Зелье Киллера", -1)).setVisible(() -> (Boolean)this.options.getValueByName("Использование по бинду").get() && !this.itemsMode.is("Новые"));
    public final BindSetting medicKey = (new BindSetting("Зелье Медика", -1)).setVisible(() -> (Boolean)this.options.getValueByName("Использование по бинду").get() && !this.itemsMode.is("Новые"));
    public final BindSetting winnerKey = (new BindSetting("Зелье Победителя", -1)).setVisible(() -> (Boolean)this.options.getValueByName("Использование по бинду").get() && !this.itemsMode.is("Новые"));
    public final BindSetting agentKey = (new BindSetting("Зелье Агента", -1)).setVisible(() -> (Boolean)this.options.getValueByName("Использование по бинду").get() && !this.itemsMode.is("Новые"));

    public final BindSetting razbafKey = (new BindSetting("Разбаф", -1)).setVisible(() -> (Boolean)this.options.getValueByName("Использование по бинду").get());
    private final ModeSetting razbafFirstPotion = (new ModeSetting("Разбаф первое", "Слабость", "Слабость", "Снотворное")).setVisible(() -> (Boolean)this.options.getValueByName("Использование по бинду").get());
    private final ModeSetting razbafFinalPotion = (new ModeSetting("Разбаф баф", "Ассасин", "Ассасин", "Гнев")).setVisible(() -> (Boolean)this.options.getValueByName("Использование по бинду").get());

    private final BooleanSetting trapTimer = new BooleanSetting("Время Трапки", false);
    private final BooleanSetting onlyDonateItems = new BooleanSetting("Подбор Донат предметов", false);

    final StopWatch stopWatch = new StopWatch();
    InventoryUtil.Hand handUtil = new InventoryUtil.Hand();
    long delay;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private final List<TrapData> consumables = new ArrayList<>();
    private final Set<Integer> changedZombieIds = new HashSet<>();
    private TrapData currentTrap = null;
    private int updateCounter = 0;
    private static final double MAX_DISTANCE = 10.0;
    boolean disorientationThrow, trapThrow, blatantThrow, serkaThrow, otrigaThrow, bowThrow, plastThrow, flameThrow, flashThrow, auraThrow, snowballThrow, killerThrow, medicThrow, winnerThrow, agentThrow;
    boolean shulkerThrow, xlopyshkaThrow, svatvodaThrow, gnevkaThrow, paladinThrow, assasinThrow, radiaciaThrow, snotvornoeThrow;
    private static final float RAZBAF_PITCH = 90.0F;
    private static final long RAZBAF_DELAY_MS = 250L;
    private boolean razbafActive;
    private int razbafStage;
    private long razbafNextAction;
    private float razbafYaw;
    public static int stop;

    public FTHelper() {
        addSettings(
                options,
                checktouse,
                onlypvp,
                itemsMode,
                trapTimer,
                onlyDonateItems,
                combatItemsCategory,
                disorientationKey,
                trapKey,
                blatantKey,
                plastKey,
                flameKey,
                auraKey,
                snowballKey,
                miscCategory,
                bowKey,
                shulkerKey,
                potionsCategory,
                otrigaKey,
                flashKey,
                serkaKey,
                xlopyshkaKey,
                svatvodaKey,
                gnevkaKey,
                paladinKey,
                assasinKey,
                radiaciaKey,
                snotvornoeKey,
                killerKey,
                medicKey,
                winnerKey,
                agentKey,
                razbafCategory,
                razbafKey,
                razbafFirstPotion,
                razbafFinalPotion
        );
    }

    @Subscribe
    private void onKey(EventKey e) {
        if (!(Boolean)this.options.getValueByName("Использование по бинду").get()) {
            return;
        }

        if (onlypvp.get() && !isPlayerNearby(12.0F)) {
            return;
        }

        if (e.getKey() == razbafKey.get()) {
            startRazbaf();
            return;
        }

        if (e.getKey() == disorientationKey.get()) disorientationThrow = true;
        if (e.getKey() == trapKey.get()) trapThrow = true;
        if (e.getKey() == flameKey.get()) flameThrow = true;
        if (e.getKey() == blatantKey.get()) blatantThrow = true;
        if (e.getKey() == otrigaKey.get() && !itemsMode.is("Новые")) otrigaThrow = true;
        if (e.getKey() == serkaKey.get() && !itemsMode.is("Новые")) serkaThrow = true;
        if (e.getKey() == bowKey.get()) bowThrow = true;
        if (e.getKey() == plastKey.get()) plastThrow = true;
        if (e.getKey() == auraKey.get()) auraThrow = true;
        if (e.getKey() == flashKey.get() && !itemsMode.is("Новые")) flashThrow = true;
        if (e.getKey() == snowballKey.get()) snowballThrow = true;
        if (e.getKey() == killerKey.get() && !itemsMode.is("Новые")) killerThrow = true;
        if (e.getKey() == medicKey.get() && !itemsMode.is("Новые")) medicThrow = true;
        if (e.getKey() == winnerKey.get() && !itemsMode.is("Новые")) winnerThrow = true;
        if (e.getKey() == agentKey.get() && !itemsMode.is("Новые")) agentThrow = true;
        if (e.getKey() == shulkerKey.get()) shulkerThrow = true;
        if (e.getKey() == xlopyshkaKey.get() && !itemsMode.is("Старые")) xlopyshkaThrow = true;
        if (e.getKey() == svatvodaKey.get() && !itemsMode.is("Старые")) svatvodaThrow = true;
        if (e.getKey() == gnevkaKey.get() && !itemsMode.is("Старые")) gnevkaThrow = true;
        if (e.getKey() == paladinKey.get() && !itemsMode.is("Старые")) paladinThrow = true;
        if (e.getKey() == assasinKey.get() && !itemsMode.is("Старые")) assasinThrow = true;
        if (e.getKey() == radiaciaKey.get() && !itemsMode.is("Старые")) radiaciaThrow = true;
        if (e.getKey() == snotvornoeKey.get() && !itemsMode.is("Старые")) snotvornoeThrow = true;
    }

    @Subscribe
    private void onUpdate(EventInput e) {
        if (stop != 0) {
            e.setForward(0.0F);
            e.setStrafe(0.0F);
            --stop;
        }
    }

    @Subscribe
    private void onMotion(EventMotion e) {
        if (!razbafActive || mc.player == null) {
            return;
        }

        e.setYaw(razbafYaw);
        e.setPitch(RAZBAF_PITCH);
        mc.player.rotationYawHead = razbafYaw;
        mc.player.renderYawOffset = razbafYaw;
        mc.player.rotationPitchHead = RAZBAF_PITCH;
    }

    @Subscribe
    private void onUpdate(EventUpdate e) {
        handleSmallZombies();

        if (!(Boolean)this.options.getValueByName("Использование по бинду").get()) {
            stopRazbaf();
            return;
        }

        if (handleRazbaf()) {
            this.handUtil.handleItemChange(System.currentTimeMillis() - this.delay > 200L);
            return;
        }

        if (checktouse.is("Фантайм")) {
            handleFunTimeMode();
        } else if (checktouse.is("Копии")) {
            handleCopiesMode();
        }

        this.handUtil.handleItemChange(System.currentTimeMillis() - this.delay > 200L);
    }

    private void startRazbaf() {
        if (mc.player == null || mc.world == null) {
            return;
        }

        razbafActive = true;
        razbafStage = 0;
        razbafNextAction = 0L;
        razbafYaw = mc.player.rotationYaw;
        applyRazbafRotation();
    }

    private void stopRazbaf() {
        razbafActive = false;
        razbafStage = 0;
        razbafNextAction = 0L;
    }

    private boolean handleRazbaf() {
        if (!razbafActive) {
            return false;
        }

        if (mc.player == null || mc.world == null) {
            stopRazbaf();
            return false;
        }

        long now = System.currentTimeMillis();
        this.handUtil.handleItemChange(now - this.delay > 200L);
        applyRazbafRotation();

        if (now < razbafNextAction) {
            return true;
        }

        if (razbafStage == 0) {
            useRazbafFirstPotion(now);
        } else if (razbafStage == 1) {
            useRazbafAura(now);
        } else if (razbafStage == 2) {
            useRazbafFinalPotion(now);
        } else {
            stopRazbaf();
        }

        return true;
    }

    private void applyRazbafRotation() {
        if (mc.player == null) {
            return;
        }

        razbafYaw = mc.player.rotationYaw;
        mc.player.rotationPitch = RAZBAF_PITCH;
        mc.player.rotationYawHead = razbafYaw;
        mc.player.renderYawOffset = razbafYaw;
        mc.player.rotationPitchHead = RAZBAF_PITCH;
    }

    private void useRazbafFirstPotion(long now) {
        int hbSlot;
        int invSlot;
        String notFoundText;
        String usedText;

        if (razbafFirstPotion.is("Слабость")) {
            hbSlot = getSplashPotionForEffectOrName(Effects.WEAKNESS, "слабост", true);
            invSlot = getSplashPotionForEffectOrName(Effects.WEAKNESS, "слабост", false);
            notFoundText = "Зелье слабости не найдено";
            usedText = "Зелье слабости успешно использовано";
        } else {
            hbSlot = getItemForName("снотворное", true);
            invSlot = getItemForName("снотворное", false);
            notFoundText = "Снотворное не найдено";
            usedText = "Снотворное успешно использовано";
        }

        useRazbafItem(hbSlot, invSlot, Items.SPLASH_POTION, notFoundText, usedText, NotificationManager.NotificationType.NOT_FOUND_SERKA, NotificationManager.NotificationType.USED_SERKA, now);
    }

    private void useRazbafAura(long now) {
        int hbSlot = getItemForNBTContains("godsaura", true);
        int invSlot = getItemForNBTContains("godsaura", false);

        if (hbSlot == -1 && invSlot == -1) {
            hbSlot = getItemForName("божья аура", true);
            invSlot = getItemForName("божья аура", false);
        }

        useRazbafItem(hbSlot, invSlot, Items.PHANTOM_MEMBRANE, "Божья аура не найдена", "Божья аура успешно использована", NotificationManager.NotificationType.NOT_FOUND_PHANTOM, NotificationManager.NotificationType.USED_PHANTOM, now);
    }

    private void useRazbafFinalPotion(long now) {
        String potionName = razbafFinalPotion.is("Ассасин") ? "зелье ассасина" : "зелье гнева";
        String displayName = razbafFinalPotion.is("Ассасин") ? "Зелье ассасина" : "Зелье гнева";
        int hbSlot = getItemForName(potionName, true);
        int invSlot = getItemForName(potionName, false);

        useRazbafItem(hbSlot, invSlot, Items.SPLASH_POTION, displayName + " не найдено", displayName + " успешно использовано", NotificationManager.NotificationType.NOT_FOUND_SERKA, NotificationManager.NotificationType.USED_SERKA, now);
    }

    private void useRazbafItem(int hbSlot, int invSlot, Item cooldownItem, String notFoundText, String usedText, NotificationManager.NotificationType notFoundType, NotificationManager.NotificationType usedType, long now) {
        if (hbSlot == -1 && invSlot == -1) {
            pushNotification(notFoundText, "", 3, notFoundType);
            stopRazbaf();
            return;
        }

        if (cooldownItem != null && mc.player.getCooldownTracker().hasCooldown(cooldownItem)) {
            return;
        }

        applyRazbafRotation();
        mc.player.connection.sendPacket(new CPlayerPacket.RotationPacket(razbafYaw, RAZBAF_PITCH, mc.player.isOnGround()));
        pushNotification(usedText, "", 3, usedType);
        int slot = findAndTrowItem(hbSlot, invSlot);
        if (slot > 8) {
            mc.playerController.pickItem(slot);
        }

        razbafStage++;
        razbafNextAction = now + RAZBAF_DELAY_MS;
    }

    private void handleSmallZombies() {
        if (mc.world == null) {
            changedZombieIds.clear();
            return;
        }

        if (!smallZombies.get()) {
            restoreChangedZombies();
            return;
        }

        for (Entity entity : mc.world.getAllEntities()) {
            if (!(entity instanceof ZombieEntity)) {
                continue;
            }

            ZombieEntity zombie = (ZombieEntity) entity;
            if (!zombie.isChild()) {
                changedZombieIds.add(zombie.getEntityId());
                zombie.setChild(true);
            }
        }
    }

    private void restoreChangedZombies() {
        if (mc.world == null) {
            changedZombieIds.clear();
            return;
        }

        Iterator<Integer> iterator = changedZombieIds.iterator();
        while (iterator.hasNext()) {
            Entity entity = mc.world.getEntityByID(iterator.next());
            if (entity instanceof ZombieEntity) {
                ZombieEntity zombie = (ZombieEntity) entity;
                if (zombie.isChild()) {
                    zombie.setChild(false);
                }
            }
            iterator.remove();
        }
    }

    @Subscribe
    public void onDisplay(EventDisplay e) {
        if (!trapTimer.get() || e.getType() != EventDisplay.Type.HIGH) {
            return;
        }

        consumables.removeIf(cons -> (cons.time - System.currentTimeMillis()) / 1000.0F <= 0.0F);
        List<TrapData> copy = new ArrayList<>(consumables);

        for (TrapData cons : copy) {
            if (mc.player != null) {
                double distance = mc.player.getPositionVec().distanceTo(cons.position);

                if (distance <= MAX_DISTANCE && cons.wasOutOfRange) {
                    cons.wasOutOfRange = false;
                    currentTrap = cons;
                } else if (distance > MAX_DISTANCE && !cons.wasOutOfRange) {
                    cons.wasOutOfRange = true;
                    if (cons == currentTrap) {
                        currentTrap = null;
                        mc.ingameGUI.setOverlayMessage(ITextComponent.getTextComponentOrEmpty(""), false);
                    }
                }
            }

            Vector2f vec2f = ProjectionUtil.project(cons.position.x, cons.position.y, cons.position.z);
            if (vec2f == null) {
                continue;
            }

            int screenWidth = mc.getMainWindow().getScaledWidth();
            int screenHeight = mc.getMainWindow().getScaledHeight();
            if (vec2f.x < 0 || vec2f.x > screenWidth || vec2f.y < 0 || vec2f.y > screenHeight) {
                continue;
            }

            double time = Math.round((cons.time - System.currentTimeMillis()) / 1000.0 * 10) / 10.0;
            String text = time + "\u0441";
            float size = 8.0F;
            float width = Fonts.sfMedium.getWidth(text, size);
            float height = Fonts.sfMedium.getHeight(size);
            float posX = vec2f.x - width / 2.0F;
            float posY = vec2f.y;

            float padding = 4.0F;
            float rectX = posX - padding;
            float rectY = posY - padding;
            float rectWidth = width + padding * 2;
            float rectHeight = height + padding * 2;

            int themeColor = Theme.MainColor(0);
            int bgColor = ColorUtils.interpolateColor(
                    ColorUtils.rgba(15, 15, 20, 180),
                    themeColor,
                    0.2f
            );

            RenderUtility.drawRoundedRect(rectX, rectY, rectWidth, rectHeight, new Vector4f(4, 4, 4, 4), bgColor);
            Fonts.sfMedium.drawText(e.getMatrixStack(), text, posX, posY, -1, size);

            double distance = mc.player != null ? mc.player.getPositionVec().distanceTo(cons.position) : 0;
            if (distance > MAX_DISTANCE) {
                String distText = "\u0412\u043d\u0435 \u0437\u043e\u043d\u044b";
                float distSize = 6.0F;
                float distWidth = Fonts.sfMedium.getWidth(distText, distSize);
                float distPosX = vec2f.x - distWidth / 2.0F;
                float distPosY = posY + height + padding;
                Fonts.sfMedium.drawText(e.getMatrixStack(), distText, distPosX, distPosY, ColorUtils.rgba(255, 100, 100, 200), distSize);
            } else {
                String bottomText = cons.type;
                float bottomSize = 6.0F;
                float bottomWidth = Fonts.sfMedium.getWidth(bottomText, bottomSize);
                float bottomPosX = vec2f.x - bottomWidth / 2.0F;
                float bottomPosY = posY + height + padding;
                Fonts.sfMedium.drawText(e.getMatrixStack(), bottomText, bottomPosX, bottomPosY, ColorUtils.rgba(100, 255, 100, 200), bottomSize);
            }
        }

        updateCounter++;
        if (updateCounter % 5 == 0 && currentTrap != null) {
            double timeLeft = (currentTrap.time - System.currentTimeMillis()) / 1000.0;
            if (timeLeft > 0) {
                updateTrapMessage(currentTrap.type, timeLeft);
            }
        }
    }

    private void handleFunTimeMode() {
        if (disorientationThrow) {
            this.handUtil.handleItemChange(System.currentTimeMillis() - this.delay > 200L);
            int hbSlot = getItemForNBTContains("desorientation", true);
            int invSlot = getItemForNBTContains("desorientation", false);

            if (invSlot == -1 && hbSlot == -1) {
                pushNotification("Дезориентация не найдена", "", 3, NotificationManager.NotificationType.NOT_FOUND_DISORIENTATION);
                disorientationThrow = false;
                return;
            }

            if (!mc.player.getCooldownTracker().hasCooldown(Items.ENDER_EYE)) {
                pushNotification("Дезориентация успешно использована", "", 3, NotificationManager.NotificationType.USED_DISORIENTATION);
                int slot = findAndTrowItem(hbSlot, invSlot);
                if (slot > 8) {
                    mc.playerController.pickItem(slot);
                }
            }
            disorientationThrow = false;
        }

        if (trapThrow) {
            int hbSlot = getItemForNBTContains("trap", true);
            int invSlot = getItemForNBTContains("trap", false);

            if (invSlot == -1 && hbSlot == -1) {
                pushNotification("Трапка не найдена", "", 3, NotificationManager.NotificationType.NOT_FOUND_TRAP);
                trapThrow = false;
                return;
            }

            if (!mc.player.getCooldownTracker().hasCooldown(Items.NETHERITE_SCRAP)) {
                pushNotification("Трапка успешно использована", "", 3, NotificationManager.NotificationType.USED_TRAP);
                int old = mc.player.inventory.currentItem;

                int slot = findAndTrowItem(hbSlot, invSlot);
                if (slot > 8) {
                    mc.playerController.pickItem(slot);
                }
                if (InventoryUtil.findEmptySlot(true) != -1 && mc.player.inventory.currentItem != old) {
                    mc.player.inventory.currentItem = old;
                }
            }
            trapThrow = false;
        }

        if (blatantThrow) {
            int hbSlot = getItemForNBTContains("sheerdust", true);
            int invSlot = getItemForNBTContains("sheerdust", false);

            if (invSlot == -1 && hbSlot == -1) {
                pushNotification("Явная пыль не найдена", "", 3, NotificationManager.NotificationType.NOT_FOUND_BLATANT);
                blatantThrow = false;
                return;
            }

            if (!mc.player.getCooldownTracker().hasCooldown(Items.SUGAR)) {
                pushNotification("Явная пыль успешно использована", "", 3, NotificationManager.NotificationType.USED_BLATANT);
                int old = mc.player.inventory.currentItem;

                int slot = findAndTrowItem(hbSlot, invSlot);
                if (slot > 8) {
                    mc.playerController.pickItem(slot);
                }
                if (InventoryUtil.findEmptySlot(true) != -1 && mc.player.inventory.currentItem != old) {
                    mc.player.inventory.currentItem = old;
                }
            }
            blatantThrow = false;
        }

        if (flameThrow) {
            int hbSlot = getItemForNBTContains("fierytornado", true);
            int invSlot = getItemForNBTContains("fierytornado", false);

            if (invSlot == -1 && hbSlot == -1) {
                pushNotification("Огненный смерч не найден", "", 3, NotificationManager.NotificationType.NOT_FOUND_TRAP);
                flameThrow = false;
                return;
            }

            if (!mc.player.getCooldownTracker().hasCooldown(Items.FIRE_CHARGE)) {
                pushNotification("Огненный смерч успешно использован", "", 3, NotificationManager.NotificationType.USED_TRAP);
                int old = mc.player.inventory.currentItem;

                int slot = findAndTrowItem(hbSlot, invSlot);
                if (slot > 8) {
                    mc.playerController.pickItem(slot);
                }
                if (InventoryUtil.findEmptySlot(true) != -1 && mc.player.inventory.currentItem != old) {
                    mc.player.inventory.currentItem = old;
                }
            }
            flameThrow = false;
        }

        if (plastThrow) {
            int hbSlot = getItemForNBTContains("stratum", true);
            int invSlot = getItemForNBTContains("stratum", false);

            if (invSlot == -1 && hbSlot == -1) {
                hbSlot = getItemForName("пласт", true);
                invSlot = getItemForName("пласт", false);
            }

            if (invSlot == -1 && hbSlot == -1) {
                pushNotification("Пласт не найден", "", 3, NotificationManager.NotificationType.NOT_FOUND_PLAST);
                plastThrow = false;
                return;
            }

            if (!mc.player.getCooldownTracker().hasCooldown(Items.DRIED_KELP)) {
                pushNotification("Пласт успешно использован", "", 3, NotificationManager.NotificationType.USED_PLAST);
                int old = mc.player.inventory.currentItem;

                int slot = findAndTrowItem(hbSlot, invSlot);
                if (slot > 8) {
                    mc.playerController.pickItem(slot);
                }
                if (InventoryUtil.findEmptySlot(true) != -1 && mc.player.inventory.currentItem != old) {
                    mc.player.inventory.currentItem = old;
                }
            }
            plastThrow = false;
        }

        if (otrigaThrow) {
            int hbSlot = getItemForNBTContains("burp", true);
            int invSlot = getItemForNBTContains("burp", false);

            if (invSlot == -1 && hbSlot == -1) {
                pushNotification("Отрыжка не найдена", "", 3, NotificationManager.NotificationType.NOT_FOUND_OTRIGA);
                otrigaThrow = false;
                return;
            }

            if (!mc.player.getCooldownTracker().hasCooldown(Items.SPLASH_POTION)) {
                pushNotification("Отрыжка успешно использована", "", 3, NotificationManager.NotificationType.USED_OTRIGA);
                int old = mc.player.inventory.currentItem;

                int slot = findAndTrowItem(hbSlot, invSlot);
                if (slot > 8) {
                    mc.playerController.pickItem(slot);
                }
                if (InventoryUtil.findEmptySlot(true) != -1 && mc.player.inventory.currentItem != old) {
                    mc.player.inventory.currentItem = old;
                }
            }
            otrigaThrow = false;
        }

        if (serkaThrow) {
            int hbSlot = getItemForNBTContains("acid", true);
            int invSlot = getItemForNBTContains("acid", false);

            if (invSlot == -1 && hbSlot == -1) {
                pushNotification("Серка не найдена", "", 3, NotificationManager.NotificationType.NOT_FOUND_SERKA);
                serkaThrow = false;
                return;
            }

            if (!mc.player.getCooldownTracker().hasCooldown(Items.SPLASH_POTION)) {
                pushNotification("Серка успешно использована", "", 3, NotificationManager.NotificationType.USED_SERKA);
                int old = mc.player.inventory.currentItem;

                int slot = findAndTrowItem(hbSlot, invSlot);
                if (slot > 8) {
                    mc.playerController.pickItem(slot);
                }
                if (InventoryUtil.findEmptySlot(true) != -1 && mc.player.inventory.currentItem != old) {
                    mc.player.inventory.currentItem = old;
                }
            }
            serkaThrow = false;
        }

        if (flashThrow) {
            int hbSlot = getItemForNBTContains("flash", true);
            int invSlot = getItemForNBTContains("flash", false);

            if (invSlot == -1 && hbSlot == -1) {
                pushNotification("Флеш не найден", "", 3, NotificationManager.NotificationType.NOT_FOUND_SERKA);
                flashThrow = false;
                return;
            }

            if (!mc.player.getCooldownTracker().hasCooldown(Items.SPLASH_POTION)) {
                pushNotification("Флеш успешно использован", "", 3, NotificationManager.NotificationType.USED_SERKA);
                int old = mc.player.inventory.currentItem;

                int slot = findAndTrowItem(hbSlot, invSlot);
                if (slot > 8) {
                    mc.playerController.pickItem(slot);
                }
                if (InventoryUtil.findEmptySlot(true) != -1 && mc.player.inventory.currentItem != old) {
                    mc.player.inventory.currentItem = old;
                }
            }
            flashThrow = false;
        }

        if (auraThrow) {
            this.handUtil.handleItemChange(System.currentTimeMillis() - this.delay > 200L);
            int hbSlot = getItemForNBTContains("godsaura", true);
            int invSlot = getItemForNBTContains("godsaura", false);

            if (invSlot == -1 && hbSlot == -1) {
                pushNotification("Божья аура не найдена", "", 3, NotificationManager.NotificationType.NOT_FOUND_PHANTOM);
                auraThrow = false;
                return;
            }

            if (!mc.player.getCooldownTracker().hasCooldown(Items.PHANTOM_MEMBRANE)) {
                pushNotification("Божья аура успешно использована", "", 3, NotificationManager.NotificationType.USED_PHANTOM);
                int slot = findAndTrowItem(hbSlot, invSlot);
                if (slot > 8) {
                    mc.playerController.pickItem(slot);
                }
            }
            auraThrow = false;
        }

        if (snowballThrow) {
            int hbSlot = getItem(Items.SNOWBALL, true);
            int invSlot = getItem(Items.SNOWBALL, false);

            if (invSlot == -1 && hbSlot == -1) {
                pushNotification("Снежок заморозка не найден", "", 3, NotificationManager.NotificationType.NOT_FOUND_SNOWBALL);
                snowballThrow = false;
                return;
            }

            if (!mc.player.getCooldownTracker().hasCooldown(Items.SNOWBALL)) {
                pushNotification("Снежок заморозка использован", "", 3, NotificationManager.NotificationType.USED_SNOWBALL);
                int old = mc.player.inventory.currentItem;

                int slot = findAndTrowItem(hbSlot, invSlot);
                if (slot > 8) {
                    mc.playerController.pickItem(slot);
                }
                if (InventoryUtil.findEmptySlot(true) != -1 && mc.player.inventory.currentItem != old) {
                    mc.player.inventory.currentItem = old;
                }
            }
            snowballThrow = false;
        }

        if (killerThrow) {
            int hbSlot = getItemForNBTContains("potion-killer", true);
            int invSlot = getItemForNBTContains("potion-killer", false);

            if (invSlot == -1 && hbSlot == -1) {
                pushNotification("Зелье киллера не найдено", "", 3, NotificationManager.NotificationType.NOT_FOUND_SERKA);
                killerThrow = false;
                return;
            }

            if (!mc.player.getCooldownTracker().hasCooldown(Items.SPLASH_POTION)) {
                pushNotification("Зелье киллера использовано", "", 3, NotificationManager.NotificationType.USED_SERKA);
                int old = mc.player.inventory.currentItem;

                int slot = findAndTrowItem(hbSlot, invSlot);
                if (slot > 8) {
                    mc.playerController.pickItem(slot);
                }
                if (InventoryUtil.findEmptySlot(true) != -1 && mc.player.inventory.currentItem != old) {
                    mc.player.inventory.currentItem = old;
                }
            }
            killerThrow = false;
        }

        if (medicThrow) {
            int hbSlot = getItemForNBTContains("potion-medic", true);
            int invSlot = getItemForNBTContains("potion-medic", false);

            if (invSlot == -1 && hbSlot == -1) {
                pushNotification("Зелье медика не найдено", "", 3, NotificationManager.NotificationType.NOT_FOUND_SERKA);
                medicThrow = false;
                return;
            }

            if (!mc.player.getCooldownTracker().hasCooldown(Items.SPLASH_POTION)) {
                pushNotification("Зелье медика использовано", "", 3, NotificationManager.NotificationType.USED_SERKA);
                int old = mc.player.inventory.currentItem;

                int slot = findAndTrowItem(hbSlot, invSlot);
                if (slot > 8) {
                    mc.playerController.pickItem(slot);
                }
                if (InventoryUtil.findEmptySlot(true) != -1 && mc.player.inventory.currentItem != old) {
                    mc.player.inventory.currentItem = old;
                }
            }
            medicThrow = false;
        }

        if (winnerThrow) {
            int hbSlot = getItemForNBTContains("potion-winner", true);
            int invSlot = getItemForNBTContains("potion-winner", false);

            if (invSlot == -1 && hbSlot == -1) {
                pushNotification("Зелье победителя не найдено", "", 3, NotificationManager.NotificationType.NOT_FOUND_SERKA);
                winnerThrow = false;
                return;
            }

            if (!mc.player.getCooldownTracker().hasCooldown(Items.SPLASH_POTION)) {
                pushNotification("Зелье победителя использовано", "", 3, NotificationManager.NotificationType.USED_SERKA);
                int old = mc.player.inventory.currentItem;

                int slot = findAndTrowItem(hbSlot, invSlot);
                if (slot > 8) {
                    mc.playerController.pickItem(slot);
                }
                if (InventoryUtil.findEmptySlot(true) != -1 && mc.player.inventory.currentItem != old) {
                    mc.player.inventory.currentItem = old;
                }
            }
            winnerThrow = false;
        }

        if (agentThrow) {
            int hbSlot = getItemForNBTContains("potion-agent", true);
            int invSlot = getItemForNBTContains("potion-agent", false);

            if (invSlot == -1 && hbSlot == -1) {
                pushNotification("Зелье агента не найдено", "", 3, NotificationManager.NotificationType.NOT_FOUND_SERKA);
                agentThrow = false;
                return;
            }

            if (!mc.player.getCooldownTracker().hasCooldown(Items.SPLASH_POTION)) {
                pushNotification("Зелье агента использовано", "", 3, NotificationManager.NotificationType.USED_SERKA);
                int old = mc.player.inventory.currentItem;

                int slot = findAndTrowItem(hbSlot, invSlot);
                if (slot > 8) {
                    mc.playerController.pickItem(slot);
                }
                if (InventoryUtil.findEmptySlot(true) != -1 && mc.player.inventory.currentItem != old) {
                    mc.player.inventory.currentItem = old;
                }
            }
            agentThrow = false;
        }

        if (bowThrow) {
            int hbSlot = getItem(Items.CROSSBOW, true);
            int invSlot = getItem(Items.CROSSBOW, false);

            if (invSlot == -1 && hbSlot == -1) {
                pushNotification("Арбалет не найден", "", 3, NotificationManager.NotificationType.NOT_FOUND_CROSSBOW);
                bowThrow = false;
                return;
            }

            if (!mc.player.getCooldownTracker().hasCooldown(Items.CROSSBOW)) {
                pushNotification("Арбалет успешно использован", "", 3, NotificationManager.NotificationType.USED_CROSSBOW);
                int old = mc.player.inventory.currentItem;

                int slot = findAndTrowItem(hbSlot, invSlot);
                if (slot > 8) {
                    mc.playerController.pickItem(slot);
                }
                if (InventoryUtil.findEmptySlot(true) != -1 && mc.player.inventory.currentItem != old) {
                    mc.player.inventory.currentItem = old;
                }
            }
            bowThrow = false;
        }

        if (shulkerThrow) {
            int hbSlot = getItemForName("ящик", true);
            int invSlot = getItemForName("ящик", false);

            if (invSlot == -1 && hbSlot == -1) {
                pushNotification("Шалкер не найден", "", 3, NotificationManager.NotificationType.NOT_FOUND_SERKA);
                shulkerThrow = false;
                return;
            }

            if (!mc.player.getCooldownTracker().hasCooldown(Items.SHULKER_BOX)) {
                pushNotification("Шалкер успешно использован", "", 3, NotificationManager.NotificationType.USED_SERKA);
                int old = mc.player.inventory.currentItem;

                int slot = findAndTrowItem(hbSlot, invSlot);
                if (slot > 8) {
                    mc.playerController.pickItem(slot);
                }
                if (InventoryUtil.findEmptySlot(true) != -1 && mc.player.inventory.currentItem != old) {
                    mc.player.inventory.currentItem = old;
                }
            }
            shulkerThrow = false;
        }

        if (xlopyshkaThrow) {
            this.handUtil.handleItemChange(System.currentTimeMillis() - this.delay > 200L);
            int hbSlot = getItemForName("хлопушка", true);
            int invSlot = getItemForName("хлопушка", false);

            if (invSlot == -1 && hbSlot == -1) {
                pushNotification("Хлопушка не найдена", "", 3, NotificationManager.NotificationType.NOT_FOUND_SERKA);
                xlopyshkaThrow = false;
                return;
            }

            if (!mc.player.getCooldownTracker().hasCooldown(Items.SPLASH_POTION)) {
                pushNotification("Хлопушка успешно использована", "", 3, NotificationManager.NotificationType.USED_SERKA);
                int slot = findAndTrowItem(hbSlot, invSlot);
                if (slot > 8) {
                    mc.playerController.pickItem(slot);
                }
            }
            xlopyshkaThrow = false;
        }

        if (svatvodaThrow) {
            this.handUtil.handleItemChange(System.currentTimeMillis() - this.delay > 200L);
            int hbSlot = getItemForName("святая вода", true);
            int invSlot = getItemForName("святая вода", false);

            if (invSlot == -1 && hbSlot == -1) {
                pushNotification("Святая вода не найдена", "", 3, NotificationManager.NotificationType.NOT_FOUND_SERKA);
                svatvodaThrow = false;
                return;
            }

            if (!mc.player.getCooldownTracker().hasCooldown(Items.SPLASH_POTION)) {
                pushNotification("Святая вода успешно использована", "", 3, NotificationManager.NotificationType.USED_SERKA);
                int slot = findAndTrowItem(hbSlot, invSlot);
                if (slot > 8) {
                    mc.playerController.pickItem(slot);
                }
            }
            svatvodaThrow = false;
        }

        if (gnevkaThrow) {
            this.handUtil.handleItemChange(System.currentTimeMillis() - this.delay > 200L);
            int hbSlot = getItemForName("зелье гнева", true);
            int invSlot = getItemForName("зелье гнева", false);

            if (invSlot == -1 && hbSlot == -1) {
                pushNotification("Зелье гнева не найдено", "", 3, NotificationManager.NotificationType.NOT_FOUND_SERKA);
                gnevkaThrow = false;
                return;
            }

            if (!mc.player.getCooldownTracker().hasCooldown(Items.SPLASH_POTION)) {
                pushNotification("Зелье гнева успешно использовано", "", 3, NotificationManager.NotificationType.USED_SERKA);
                int slot = findAndTrowItem(hbSlot, invSlot);
                if (slot > 8) {
                    mc.playerController.pickItem(slot);
                }
            }
            gnevkaThrow = false;
        }

        if (paladinThrow) {
            this.handUtil.handleItemChange(System.currentTimeMillis() - this.delay > 200L);
            int hbSlot = getItemForName("зелье палладина", true);
            int invSlot = getItemForName("зелье палладина", false);

            if (invSlot == -1 && hbSlot == -1) {
                pushNotification("Зелье паладина не найдено", "", 3, NotificationManager.NotificationType.NOT_FOUND_SERKA);
                paladinThrow = false;
                return;
            }

            if (!mc.player.getCooldownTracker().hasCooldown(Items.SPLASH_POTION)) {
                pushNotification("Зелье паладина успешно использовано", "", 3, NotificationManager.NotificationType.USED_SERKA);
                int slot = findAndTrowItem(hbSlot, invSlot);
                if (slot > 8) {
                    mc.playerController.pickItem(slot);
                }
            }
            paladinThrow = false;
        }

        if (assasinThrow) {
            this.handUtil.handleItemChange(System.currentTimeMillis() - this.delay > 200L);
            int hbSlot = getItemForName("зелье ассасина", true);
            int invSlot = getItemForName("зелье ассасина", false);

            if (invSlot == -1 && hbSlot == -1) {
                pushNotification("Зелье ассасина не найдено", "", 3, NotificationManager.NotificationType.NOT_FOUND_SERKA);
                assasinThrow = false;
                return;
            }

            if (!mc.player.getCooldownTracker().hasCooldown(Items.SPLASH_POTION)) {
                pushNotification("Зелье ассасина успешно использовано", "", 3, NotificationManager.NotificationType.USED_SERKA);
                int slot = findAndTrowItem(hbSlot, invSlot);
                if (slot > 8) {
                    mc.playerController.pickItem(slot);
                }
            }
            assasinThrow = false;
        }

        if (radiaciaThrow) {
            this.handUtil.handleItemChange(System.currentTimeMillis() - this.delay > 200L);
            int hbSlot = getItemForName("зелье радиации", true);
            int invSlot = getItemForName("зелье радиации", false);

            if (invSlot == -1 && hbSlot == -1) {
                pushNotification("Зелье радиации не найдено", "", 3, NotificationManager.NotificationType.NOT_FOUND_SERKA);
                radiaciaThrow = false;
                return;
            }

            if (!mc.player.getCooldownTracker().hasCooldown(Items.SPLASH_POTION)) {
                pushNotification("Зелье радиации успешно использовано", "", 3, NotificationManager.NotificationType.USED_SERKA);
                int slot = findAndTrowItem(hbSlot, invSlot);
                if (slot > 8) {
                    mc.playerController.pickItem(slot);
                }
            }
            radiaciaThrow = false;
        }

        if (snotvornoeThrow) {
            this.handUtil.handleItemChange(System.currentTimeMillis() - this.delay > 200L);
            int hbSlot = getItemForName("снотворное", true);
            int invSlot = getItemForName("снотворное", false);

            if (invSlot == -1 && hbSlot == -1) {
                pushNotification("Снотворное не найдено", "", 3, NotificationManager.NotificationType.NOT_FOUND_SERKA);
                snotvornoeThrow = false;
                return;
            }

            if (!mc.player.getCooldownTracker().hasCooldown(Items.SPLASH_POTION)) {
                pushNotification("Снотворное успешно использовано", "", 3, NotificationManager.NotificationType.USED_SERKA);
                int slot = findAndTrowItem(hbSlot, invSlot);
                if (slot > 8) {
                    mc.playerController.pickItem(slot);
                }
            }
            snotvornoeThrow = false;
        }
    }

    private void handleCopiesMode() {
        if (disorientationThrow) {
            this.handUtil.handleItemChange(System.currentTimeMillis() - this.delay > 200L);
            int hbSlot = getItemForName("дезориентация", true);
            int invSlot = getItemForName("дезориентация", false);

            if (invSlot == -1 && hbSlot == -1) {
                pushNotification("Дезориентация не найдена", "", 3, NotificationManager.NotificationType.NOT_FOUND_DISORIENTATION);
                disorientationThrow = false;
                return;
            }

            if (!mc.player.getCooldownTracker().hasCooldown(Items.ENDER_EYE)) {
                pushNotification("Дезориентация успешно использована", "", 3, NotificationManager.NotificationType.USED_DISORIENTATION);
                int slot = findAndTrowItem(hbSlot, invSlot);
                if (slot > 8) {
                    mc.playerController.pickItem(slot);
                }
            }
            disorientationThrow = false;
        }

        if (trapThrow) {
            int hbSlot = getTrapItem(true);
            int invSlot = getTrapItem(false);

            if (invSlot == -1 && hbSlot == -1) {
                pushNotification("Трапка не найдена", "", 3, NotificationManager.NotificationType.NOT_FOUND_TRAP);
                trapThrow = false;
                return;
            }

            if (!mc.player.getCooldownTracker().hasCooldown(Items.NETHERITE_SCRAP)) {
                pushNotification("Трапка успешно использована", "", 3, NotificationManager.NotificationType.USED_TRAP);
                int old = mc.player.inventory.currentItem;

                int slot = findAndTrowItem(hbSlot, invSlot);
                if (slot > 8) {
                    mc.playerController.pickItem(slot);
                }
                if (InventoryUtil.findEmptySlot(true) != -1 && mc.player.inventory.currentItem != old) {
                    mc.player.inventory.currentItem = old;
                }
            }
            trapThrow = false;
        }

        if (bowThrow) {
            int hbSlot = getItemForName("арбалет", true);
            int invSlot = getItemForName("арбалет", false);

            if (invSlot == -1 && hbSlot == -1) {
                hbSlot = getItemForName("заряженный", true);
                invSlot = getItemForName("заряженный", false);
            }

            if (invSlot == -1 && hbSlot == -1) {
                hbSlot = getItem(Items.CROSSBOW, true);
                invSlot = getItem(Items.CROSSBOW, false);
            }

            if (invSlot == -1 && hbSlot == -1) {
                pushNotification("Арбалет не найден", "", 3, NotificationManager.NotificationType.NOT_FOUND_CROSSBOW);
                bowThrow = false;
                return;
            }

            if (!mc.player.getCooldownTracker().hasCooldown(Items.CROSSBOW)) {
                pushNotification("Арбалет успешно использован", "", 3, NotificationManager.NotificationType.USED_CROSSBOW);
                int old = mc.player.inventory.currentItem;

                int slot = findAndTrowItem(hbSlot, invSlot);
                if (slot > 8) {
                    mc.playerController.pickItem(slot);
                }
                if (InventoryUtil.findEmptySlot(true) != -1 && mc.player.inventory.currentItem != old) {
                    mc.player.inventory.currentItem = old;
                }
            }
            bowThrow = false;
        }

        if (serkaThrow) {
            int hbSlot = getItemForName("серная", true);
            int invSlot = getItemForName("серная", false);

            if (invSlot == -1 && hbSlot == -1) {
                pushNotification("Серка не найдена", "", 3, NotificationManager.NotificationType.NOT_FOUND_SERKA);
                serkaThrow = false;
                return;
            }

            if (!mc.player.getCooldownTracker().hasCooldown(Items.SPLASH_POTION)) {
                pushNotification("Серка успешно использована", "", 3, NotificationManager.NotificationType.USED_SERKA);
                int old = mc.player.inventory.currentItem;

                int slot = findAndTrowItem(hbSlot, invSlot);
                if (slot > 8) {
                    mc.playerController.pickItem(slot);
                }
                if (InventoryUtil.findEmptySlot(true) != -1 && mc.player.inventory.currentItem != old) {
                    mc.player.inventory.currentItem = old;
                }
            }
            serkaThrow = false;
        }

        if (otrigaThrow) {
            int hbSlot = getItemForName("отрыжки", true);
            int invSlot = getItemForName("отрыжки", false);

            if (invSlot == -1 && hbSlot == -1) {
                pushNotification("Отрыжка не найдена", "", 3, NotificationManager.NotificationType.NOT_FOUND_OTRIGA);
                otrigaThrow = false;
                return;
            }

            if (!mc.player.getCooldownTracker().hasCooldown(Items.SPLASH_POTION)) {
                pushNotification("Отрыжка успешно использована", "", 3, NotificationManager.NotificationType.USED_OTRIGA);
                int old = mc.player.inventory.currentItem;

                int slot = findAndTrowItem(hbSlot, invSlot);
                if (slot > 8) {
                    mc.playerController.pickItem(slot);
                }
                if (InventoryUtil.findEmptySlot(true) != -1 && mc.player.inventory.currentItem != old) {
                    mc.player.inventory.currentItem = old;
                }
            }
            otrigaThrow = false;
        }

        if (plastThrow) {
            int hbSlot = getItemForName("пласт", true);
            int invSlot = getItemForName("пласт", false);

            if (invSlot == -1 && hbSlot == -1) {
                pushNotification("Пласт не найден", "", 3, NotificationManager.NotificationType.NOT_FOUND_PLAST);
                plastThrow = false;
                return;
            }

            if (!mc.player.getCooldownTracker().hasCooldown(Items.DRIED_KELP)) {
                pushNotification("Пласт успешно использован", "", 3, NotificationManager.NotificationType.USED_PLAST);
                int old = mc.player.inventory.currentItem;

                int slot = findAndTrowItem(hbSlot, invSlot);
                if (slot > 8) {
                    mc.playerController.pickItem(slot);
                }
                if (InventoryUtil.findEmptySlot(true) != -1 && mc.player.inventory.currentItem != old) {
                    mc.player.inventory.currentItem = old;
                }
            }
            plastThrow = false;
        }

        if (blatantThrow) {
            int hbSlot = getItemForName("явная", true);
            int invSlot = getItemForName("явная", false);

            if (invSlot == -1 && hbSlot == -1) {
                pushNotification("Явная пыль не найдена", "", 3, NotificationManager.NotificationType.NOT_FOUND_BLATANT);
                blatantThrow = false;
                return;
            }

            if (!mc.player.getCooldownTracker().hasCooldown(Items.SUGAR)) {
                pushNotification("Явная пыль успешно использована", "", 3, NotificationManager.NotificationType.USED_BLATANT);
                int old = mc.player.inventory.currentItem;

                int slot = findAndTrowItem(hbSlot, invSlot);
                if (slot > 8) {
                    mc.playerController.pickItem(slot);
                }
                if (InventoryUtil.findEmptySlot(true) != -1 && mc.player.inventory.currentItem != old) {
                    mc.player.inventory.currentItem = old;
                }
            }
            blatantThrow = false;
        }

        if (flameThrow) {
            int hbSlot = getItemForName("огненный", true);
            int invSlot = getItemForName("огненный", false);

            if (invSlot == -1 && hbSlot == -1) {
                pushNotification("Огненный смерч не найден", "", 3, NotificationManager.NotificationType.NOT_FOUND_FIRECHARGE);
                flameThrow = false;
                return;
            }

            if (!mc.player.getCooldownTracker().hasCooldown(Items.FIRE_CHARGE)) {
                pushNotification("Огненный смерч успешно использован", "", 3, NotificationManager.NotificationType.USED_FIRECHARGE);
                int old = mc.player.inventory.currentItem;

                int slot = findAndTrowItem(hbSlot, invSlot);
                if (slot > 8) {
                    mc.playerController.pickItem(slot);
                }
                if (InventoryUtil.findEmptySlot(true) != -1 && mc.player.inventory.currentItem != old) {
                    mc.player.inventory.currentItem = old;
                }
            }
            flameThrow = false;
        }

        if (flashThrow) {
            int hbSlot = getItemForName("флеш", true);
            int invSlot = getItemForName("флеш", false);

            if (invSlot == -1 && hbSlot == -1) {
                pushNotification("Флеш не найден", "", 3, NotificationManager.NotificationType.NOT_FOUND_SERKA);
                flashThrow = false;
                return;
            }

            if (!mc.player.getCooldownTracker().hasCooldown(Items.SPLASH_POTION)) {
                pushNotification("Флеш успешно использован", "", 3, NotificationManager.NotificationType.USED_SERKA);
                int old = mc.player.inventory.currentItem;

                int slot = findAndTrowItem(hbSlot, invSlot);
                if (slot > 8) {
                    mc.playerController.pickItem(slot);
                }
                if (InventoryUtil.findEmptySlot(true) != -1 && mc.player.inventory.currentItem != old) {
                    mc.player.inventory.currentItem = old;
                }
            }
            flashThrow = false;
        }

        if (auraThrow) {
            int hbSlot = getItemForName("аура", true);
            int invSlot = getItemForName("аура", false);

            if (invSlot == -1 && hbSlot == -1) {
                pushNotification("Божья аура не найдена", "", 3, NotificationManager.NotificationType.NOT_FOUND_PHANTOM);
                auraThrow = false;
                return;
            }

            if (!mc.player.getCooldownTracker().hasCooldown(Items.PHANTOM_MEMBRANE)) {
                pushNotification("Божья аура успешно использована", "", 3, NotificationManager.NotificationType.USED_PHANTOM);
                int slot = findAndTrowItem(hbSlot, invSlot);
                if (slot > 8) {
                    mc.playerController.pickItem(slot);
                }
            }
            auraThrow = false;
        }

        if (snowballThrow) {
            int hbSlot = getItemForName("снежок", true);
            int invSlot = getItemForName("снежок", false);

            if (invSlot == -1 && hbSlot == -1) {
                hbSlot = getItem(Items.SNOWBALL, true);
                invSlot = getItem(Items.SNOWBALL, false);
            }

            if (invSlot == -1 && hbSlot == -1) {
                pushNotification("Снежок заморозка не найден", "", 3, NotificationManager.NotificationType.NOT_FOUND_SNOWBALL);
                snowballThrow = false;
                return;
            }

            if (!mc.player.getCooldownTracker().hasCooldown(Items.SNOWBALL)) {
                pushNotification("Снежок заморозка использован", "", 3, NotificationManager.NotificationType.USED_SNOWBALL);
                int old = mc.player.inventory.currentItem;

                int slot = findAndTrowItem(hbSlot, invSlot);
                if (slot > 8) {
                    mc.playerController.pickItem(slot);
                }
                if (InventoryUtil.findEmptySlot(true) != -1 && mc.player.inventory.currentItem != old) {
                    mc.player.inventory.currentItem = old;
                }
            }
            snowballThrow = false;
        }

        if (killerThrow) {
            int hbSlot = getItemForName("киллера", true);
            int invSlot = getItemForName("киллера", false);

            if (invSlot == -1 && hbSlot == -1) {
                pushNotification("Зелье киллера не найдено", "", 3, NotificationManager.NotificationType.NOT_FOUND_SERKA);
                killerThrow = false;
                return;
            }

            if (!mc.player.getCooldownTracker().hasCooldown(Items.SPLASH_POTION)) {
                pushNotification("Зелье киллера использовано", "", 3, NotificationManager.NotificationType.USED_SERKA);
                int old = mc.player.inventory.currentItem;

                int slot = findAndTrowItem(hbSlot, invSlot);
                if (slot > 8) {
                    mc.playerController.pickItem(slot);
                }
                if (InventoryUtil.findEmptySlot(true) != -1 && mc.player.inventory.currentItem != old) {
                    mc.player.inventory.currentItem = old;
                }
            }
            killerThrow = false;
        }

        if (medicThrow) {
            int hbSlot = getItemForName("медика", true);
            int invSlot = getItemForName("медика", false);

            if (invSlot == -1 && hbSlot == -1) {
                pushNotification("Зелье медика не найдено", "", 3, NotificationManager.NotificationType.NOT_FOUND_SERKA);
                medicThrow = false;
                return;
            }

            if (!mc.player.getCooldownTracker().hasCooldown(Items.SPLASH_POTION)) {
                pushNotification("Зелье медика использовано", "", 3, NotificationManager.NotificationType.USED_SERKA);
                int old = mc.player.inventory.currentItem;

                int slot = findAndTrowItem(hbSlot, invSlot);
                if (slot > 8) {
                    mc.playerController.pickItem(slot);
                }
                if (InventoryUtil.findEmptySlot(true) != -1 && mc.player.inventory.currentItem != old) {
                    mc.player.inventory.currentItem = old;
                }
            }
            medicThrow = false;
        }

        if (winnerThrow) {
            int hbSlot = getItemForName("победителя", true);
            int invSlot = getItemForName("победителя", false);

            if (invSlot == -1 && hbSlot == -1) {
                pushNotification("Зелье победителя не найдено", "", 3, NotificationManager.NotificationType.NOT_FOUND_SERKA);
                winnerThrow = false;
                return;
            }

            if (!mc.player.getCooldownTracker().hasCooldown(Items.SPLASH_POTION)) {
                pushNotification("Зелье победителя использовано", "", 3, NotificationManager.NotificationType.USED_SERKA);
                int old = mc.player.inventory.currentItem;

                int slot = findAndTrowItem(hbSlot, invSlot);
                if (slot > 8) {
                    mc.playerController.pickItem(slot);
                }
                if (InventoryUtil.findEmptySlot(true) != -1 && mc.player.inventory.currentItem != old) {
                    mc.player.inventory.currentItem = old;
                }
            }
            winnerThrow = false;
        }

        if (agentThrow) {
            int hbSlot = getItemForName("агента", true);
            int invSlot = getItemForName("агента", false);

            if (invSlot == -1 && hbSlot == -1) {
                pushNotification("Зелье агента не найдено", "", 3, NotificationManager.NotificationType.NOT_FOUND_SERKA);
                agentThrow = false;
                return;
            }

            if (!mc.player.getCooldownTracker().hasCooldown(Items.SPLASH_POTION)) {
                pushNotification("Зелье агента использовано", "", 3, NotificationManager.NotificationType.USED_SERKA);
                int old = mc.player.inventory.currentItem;

                int slot = findAndTrowItem(hbSlot, invSlot);
                if (slot > 8) {
                    mc.playerController.pickItem(slot);
                }
                if (InventoryUtil.findEmptySlot(true) != -1 && mc.player.inventory.currentItem != old) {
                    mc.player.inventory.currentItem = old;
                }
            }
            agentThrow = false;
        }

        if (shulkerThrow) {
            int hbSlot = getItemForName("ящик", true);
            int invSlot = getItemForName("ящик", false);

            if (invSlot == -1 && hbSlot == -1) {
                pushNotification("Шалкер не найден", "", 3, NotificationManager.NotificationType.NOT_FOUND_SERKA);
                shulkerThrow = false;
                return;
            }

            if (!mc.player.getCooldownTracker().hasCooldown(Items.SHULKER_BOX)) {
                pushNotification("Шалкер успешно использован", "", 3, NotificationManager.NotificationType.USED_SERKA);
                int old = mc.player.inventory.currentItem;

                int slot = findAndTrowItem(hbSlot, invSlot);
                if (slot > 8) {
                    mc.playerController.pickItem(slot);
                }
                if (InventoryUtil.findEmptySlot(true) != -1 && mc.player.inventory.currentItem != old) {
                    mc.player.inventory.currentItem = old;
                }
            }
            shulkerThrow = false;
        }

        if (xlopyshkaThrow) {
            int hbSlot = getItemForName("хлопушка", true);
            int invSlot = getItemForName("хлопушка", false);

            if (invSlot == -1 && hbSlot == -1) {
                pushNotification("Хлопушка не найдена", "", 3, NotificationManager.NotificationType.NOT_FOUND_SERKA);
                xlopyshkaThrow = false;
                return;
            }

            if (!mc.player.getCooldownTracker().hasCooldown(Items.SPLASH_POTION)) {
                pushNotification("Хлопушка успешно использована", "", 3, NotificationManager.NotificationType.USED_SERKA);
                int old = mc.player.inventory.currentItem;

                int slot = findAndTrowItem(hbSlot, invSlot);
                if (slot > 8) {
                    mc.playerController.pickItem(slot);
                }
                if (InventoryUtil.findEmptySlot(true) != -1 && mc.player.inventory.currentItem != old) {
                    mc.player.inventory.currentItem = old;
                }
            }
            xlopyshkaThrow = false;
        }

        if (svatvodaThrow) {
            int hbSlot = getItemForName("святая вода", true);
            int invSlot = getItemForName("святая вода", false);

            if (invSlot == -1 && hbSlot == -1) {
                pushNotification("Святая вода не найдена", "", 3, NotificationManager.NotificationType.NOT_FOUND_SERKA);
                svatvodaThrow = false;
                return;
            }

            if (!mc.player.getCooldownTracker().hasCooldown(Items.SPLASH_POTION)) {
                pushNotification("Святая вода успешно использована", "", 3, NotificationManager.NotificationType.USED_SERKA);
                int old = mc.player.inventory.currentItem;

                int slot = findAndTrowItem(hbSlot, invSlot);
                if (slot > 8) {
                    mc.playerController.pickItem(slot);
                }
                if (InventoryUtil.findEmptySlot(true) != -1 && mc.player.inventory.currentItem != old) {
                    mc.player.inventory.currentItem = old;
                }
            }
            svatvodaThrow = false;
        }

        if (gnevkaThrow) {
            int hbSlot = getItemForName("зелье гнева", true);
            int invSlot = getItemForName("зелье гнева", false);

            if (invSlot == -1 && hbSlot == -1) {
                pushNotification("Зелье гнева не найдено", "", 3, NotificationManager.NotificationType.NOT_FOUND_SERKA);
                gnevkaThrow = false;
                return;
            }

            if (!mc.player.getCooldownTracker().hasCooldown(Items.SPLASH_POTION)) {
                pushNotification("Зелье гнева успешно использовано", "", 3, NotificationManager.NotificationType.USED_SERKA);
                int old = mc.player.inventory.currentItem;

                int slot = findAndTrowItem(hbSlot, invSlot);
                if (slot > 8) {
                    mc.playerController.pickItem(slot);
                }
                if (InventoryUtil.findEmptySlot(true) != -1 && mc.player.inventory.currentItem != old) {
                    mc.player.inventory.currentItem = old;
                }
            }
            gnevkaThrow = false;
        }

        if (paladinThrow) {
            int hbSlot = getItemForName("зелье палладина", true);
            int invSlot = getItemForName("зелье палладина", false);

            if (invSlot == -1 && hbSlot == -1) {
                pushNotification("Зелье паладина не найдено", "", 3, NotificationManager.NotificationType.NOT_FOUND_SERKA);
                paladinThrow = false;
                return;
            }

            if (!mc.player.getCooldownTracker().hasCooldown(Items.SPLASH_POTION)) {
                pushNotification("Зелье паладина успешно использовано", "", 3, NotificationManager.NotificationType.USED_SERKA);
                int old = mc.player.inventory.currentItem;

                int slot = findAndTrowItem(hbSlot, invSlot);
                if (slot > 8) {
                    mc.playerController.pickItem(slot);
                }
                if (InventoryUtil.findEmptySlot(true) != -1 && mc.player.inventory.currentItem != old) {
                    mc.player.inventory.currentItem = old;
                }
            }
            paladinThrow = false;
        }

        if (assasinThrow) {
            int hbSlot = getItemForName("зелье ассасина", true);
            int invSlot = getItemForName("зелье ассасина", false);

            if (invSlot == -1 && hbSlot == -1) {
                pushNotification("Зелье ассасина не найдено", "", 3, NotificationManager.NotificationType.NOT_FOUND_SERKA);
                assasinThrow = false;
                return;
            }

            if (!mc.player.getCooldownTracker().hasCooldown(Items.SPLASH_POTION)) {
                pushNotification("Зелье ассасина успешно использовано", "", 3, NotificationManager.NotificationType.USED_SERKA);
                int old = mc.player.inventory.currentItem;

                int slot = findAndTrowItem(hbSlot, invSlot);
                if (slot > 8) {
                    mc.playerController.pickItem(slot);
                }
                if (InventoryUtil.findEmptySlot(true) != -1 && mc.player.inventory.currentItem != old) {
                    mc.player.inventory.currentItem = old;
                }
            }
            assasinThrow = false;
        }

        if (radiaciaThrow) {
            int hbSlot = getItemForName("зелье радиации", true);
            int invSlot = getItemForName("зелье радиации", false);

            if (invSlot == -1 && hbSlot == -1) {
                pushNotification("Зелье радиации не найдено", "", 3, NotificationManager.NotificationType.NOT_FOUND_SERKA);
                radiaciaThrow = false;
                return;
            }

            if (!mc.player.getCooldownTracker().hasCooldown(Items.SPLASH_POTION)) {
                pushNotification("Зелье радиации успешно использовано", "", 3, NotificationManager.NotificationType.USED_SERKA);
                int old = mc.player.inventory.currentItem;

                int slot = findAndTrowItem(hbSlot, invSlot);
                if (slot > 8) {
                    mc.playerController.pickItem(slot);
                }
                if (InventoryUtil.findEmptySlot(true) != -1 && mc.player.inventory.currentItem != old) {
                    mc.player.inventory.currentItem = old;
                }
            }
            radiaciaThrow = false;
        }

        if (snotvornoeThrow) {
            int hbSlot = getItemForName("снотворное", true);
            int invSlot = getItemForName("снотворное", false);

            if (invSlot == -1 && hbSlot == -1) {
                pushNotification("Снотворное не найдено", "", 3, NotificationManager.NotificationType.NOT_FOUND_SERKA);
                snotvornoeThrow = false;
                return;
            }

            if (!mc.player.getCooldownTracker().hasCooldown(Items.SPLASH_POTION)) {
                pushNotification("Снотворное успешно использовано", "", 3, NotificationManager.NotificationType.USED_SERKA);
                int old = mc.player.inventory.currentItem;

                int slot = findAndTrowItem(hbSlot, invSlot);
                if (slot > 8) {
                    mc.playerController.pickItem(slot);
                }
                if (InventoryUtil.findEmptySlot(true) != -1 && mc.player.inventory.currentItem != old) {
                    mc.player.inventory.currentItem = old;
                }
            }
            snotvornoeThrow = false;
        }
    }

    @Subscribe
    private void onPacket(EventPacket e) {
        this.handUtil.onEventPacket(e);
        if (trapTimer.get() && e.isReceive()) {
            handleTrapTimerPacket(e.getPacket());
        }
        if (onlyDonateItems.get() && e.isReceive()) {
            handleDonatePickupPacket(e.getPacket());
        }

        if ((Boolean)this.options.getValueByName("Автоматически забирать ключ").get() && e.isReceive()) {
            IPacket packet = e.getPacket();
            if (packet instanceof SChatPacket) {
                SChatPacket chatPacket = (SChatPacket)packet;
                String message = chatPacket.getChatComponent().getString();
                if (message.contains("10,000 было начислено вам") || message.contains("Повторите текст еще раз")) {
                    mc.player.sendChatMessage("/piona");
                }
            }

            packet = e.getPacket();
            if (packet instanceof SOpenWindowPacket) {
                SOpenWindowPacket openPacket = (SOpenWindowPacket)packet;
                if (openPacket.getTitle().getString().contains("Вам подарок")) {
                    e.cancel();
                    mc.gameSettings.keyBindSneak.setPressed(true);
                }
            }

            packet = e.getPacket();
            if (packet instanceof SPlaySoundEffectPacket) {
                SPlaySoundEffectPacket soundPacket = (SPlaySoundEffectPacket)packet;
                if (soundPacket.getSound().getName().toString().contains("glass.place") ||
                        soundPacket.getSound().getName().toString().contains("note_block.xylophone")) {
                    e.cancel();
                }
            }

            packet = e.getPacket();
            if (packet instanceof SSetSlotPacket) {
                SSetSlotPacket slotPacket = (SSetSlotPacket)packet;
                ItemStack stack = slotPacket.getStack();
                if (stack.getDisplayName().getString().contains("Ключ")) {
                    mc.gameSettings.keyBindSneak.setPressed(false);
                    boolean click = true;

                    for(ITextComponent str : stack.getTooltip((PlayerEntity)null, ITooltipFlag.TooltipFlags.NORMAL)) {
                        if (str.getString().contains("Стоп")) {
                            mc.player.connection.sendPacket(new CCloseWindowPacket(slotPacket.getWindowId()));
                            click = false;
                            break;
                        }
                    }

                    if (click) {
                        mc.player.connection.sendPacket(new CClickWindowPacket(slotPacket.getWindowId(), 13, 0, ClickType.PICKUP,
                                mc.player.openContainer.getSlot(13).getStack(), mc.player.openContainer.getNextTransactionID(mc.player.inventory)));
                    }
                }
            }
        }
    }

    private void handleTrapTimerPacket(IPacket<?> packet) {
        if (!(packet instanceof SPlaySoundEffectPacket)) {
            return;
        }

        SPlaySoundEffectPacket wrapper = (SPlaySoundEffectPacket) packet;
        String soundPath = wrapper.getSound().getName().getPath();
        float volume = wrapper.getVolume();
        float pitch = wrapper.getPitch();

        if (soundPath.equals("block.piston.extend")) {
            Vector3d trapPos = Vector3d.copyCentered(new BlockPos(wrapper.getX(), wrapper.getY(), wrapper.getZ()));
            if (isInRange(trapPos)) {
                TrapData trap = new TrapData(System.currentTimeMillis() + 15000L, trapPos, "\u0422\u0440\u0430\u043f\u043a\u0430", 15000L);
                consumables.add(trap);
                currentTrap = trap;
            }
            return;
        }

        if (soundPath.equals("entity.evoker_fangs.attack")) {
            if ((volume == 0.5F || volume == 0.7F) && (pitch == 0.85F || pitch == 1.0F)) {
                Vector3d trapPos = new Vector3d(wrapper.getX() + 0.5, wrapper.getY() + 5.5, wrapper.getZ() + 0.5);
                if (isInRange(trapPos)) {
                    TrapData trap = new TrapData(System.currentTimeMillis() + 30000L, trapPos, "\u0414\u0440\u0430\u043a\u043e\u043d\u044c\u044f \u0442\u0440\u0430\u043f\u043a\u0430", 30000L);
                    consumables.add(trap);
                    currentTrap = trap;
                }
            }
            return;
        }

        if (soundPath.equals("block.anvil.place")) {
            BlockPos soundPos = new BlockPos(wrapper.getX(), wrapper.getY(), wrapper.getZ());
            long delay = 250L;

            scheduler.schedule(() -> getCube(soundPos, 4.0F, 4.0F).stream()
                    .filter(pos -> getDistance(soundPos, pos) > 2.0F && mc.world.getBlockState(pos).getBlock().equals(Blocks.COBBLESTONE))
                    .min(Comparator.comparing(pos -> getDistance(soundPos, pos)))
                    .ifPresent(pos -> {
                        long andesiteCount = getCube(pos, 1.0F, 1.0F).stream()
                                .filter(pos2 -> mc.world.getBlockState(pos2).getBlock().equals(Blocks.ANDESITE))
                                .count();

                        if (andesiteCount == 16L) {
                            Vector3d trapPos = Vector3d.copyCentered(pos).add(0.0, -0.5, 0.0);
                            if (isInRange(trapPos)) {
                                TrapData trap = new TrapData(System.currentTimeMillis() + 60000L - delay, trapPos, "\u041f\u043b\u0430\u0441\u0442", 60000L - delay);
                                consumables.add(trap);
                                currentTrap = trap;
                            }
                        } else if (andesiteCount == 9L || andesiteCount == 10L) {
                            Vector3d trapPos = Vector3d.copyCentered(pos);
                            if (isInRange(trapPos)) {
                                TrapData trap = new TrapData(System.currentTimeMillis() + 20000L - delay, trapPos, "\u0422\u0440\u0430\u043f\u043a\u0430", 20000L - delay);
                                consumables.add(trap);
                                currentTrap = trap;
                            }
                        }
                    }), delay, TimeUnit.MILLISECONDS);
            return;
        }

        if (soundPath.equals("entity.zombie_horse.death")) {
            BlockPos soundPos = new BlockPos(wrapper.getX(), wrapper.getY(), wrapper.getZ());
            long delay = 250L;

            scheduler.schedule(() -> getCube(soundPos, 4.0F, 4.0F).stream()
                    .filter(pos -> getDistance(soundPos, pos) > 2.0F && mc.world.getBlockState(pos).getBlock().equals(Blocks.COBBLESTONE))
                    .min(Comparator.comparing(pos -> getDistance(soundPos, pos)))
                    .ifPresent(pos -> {
                        long boneBlockCount = getCube(pos, 1.0F, 1.0F).stream()
                                .filter(pos2 -> mc.world.getBlockState(pos2).getBlock().equals(Blocks.BONE_BLOCK))
                                .count();

                        if (boneBlockCount == 16L) {
                            Vector3d trapPos = Vector3d.copyCentered(pos).add(0.0, -0.5, 0.0);
                            if (isInRange(trapPos)) {
                                TrapData trap = new TrapData(System.currentTimeMillis() + 60000L - delay, trapPos, "\u041a\u043e\u0441\u0442\u044f\u043d\u043e\u0439 \u043f\u043b\u0430\u0441\u0442", 60000L - delay);
                                consumables.add(trap);
                                currentTrap = trap;
                            }
                        } else if (boneBlockCount == 9L || boneBlockCount == 10L) {
                            Vector3d trapPos = Vector3d.copyCentered(pos);
                            if (isInRange(trapPos)) {
                                TrapData trap = new TrapData(System.currentTimeMillis() + 20000L - delay, trapPos, "\u041a\u043e\u0441\u0442\u044f\u043d\u0430\u044f \u0442\u0440\u0430\u043f\u043a\u0430", 20000L - delay);
                                consumables.add(trap);
                                currentTrap = trap;
                            }
                        }
                    }), delay, TimeUnit.MILLISECONDS);
        }
    }

    private boolean isFtHelperNotificationsEnabled() {
        try {
            Notifications miscNotification = Harmony.getInstance().getModuleManager().getNotifications();
            if (miscNotification == null) {
                return true;
            }
            if (!miscNotification.isState()) {
                return false;
            }
            return miscNotification.isFtHelperNotifyEnabled();
        } catch (Exception ignored) {
            return true;
        }
    }

    private void handleDonatePickupPacket(IPacket<?> packet) {
        if (mc.player == null || mc.world == null) {
            return;
        }
        if (!(packet instanceof SCollectItemPacket)) {
            return;
        }

        SCollectItemPacket collectPacket = (SCollectItemPacket) packet;
        if (collectPacket.getEntityID() != mc.player.getEntityId()) {
            return;
        }

        Entity entity = mc.world.getEntityByID(collectPacket.getCollectedItemEntityID());
        if (!(entity instanceof ItemEntity)) {
            return;
        }

        ItemStack stack = ((ItemEntity) entity).getItem().copy();
        if (stack.isEmpty() || !isDonateItem(stack)) {
            return;
        }

        int amount = Math.max(1, collectPacket.getAmount());
        ITextComponent message = new StringTextComponent(TextFormatting.WHITE + "Поднят донат предмет: ")
                .append(stack.getDisplayName().deepCopy())
                .append(new StringTextComponent(TextFormatting.GRAY + " x" + amount));
        print(message);
    }

    private boolean isDonateItem(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        if (!stack.hasDisplayName()) {
            return false;
        }
        return findTextColor(stack.getDisplayName()) != -1;
    }

    private int findTextColor(ITextComponent text) {
        if (text == null) {
            return -1;
        }

        Color color = text.getStyle().getColor();
        if (color != null) {
            return color.getColor();
        }

        for (ITextComponent sibling : text.getSiblings()) {
            int rgb = findTextColor(sibling);
            if (rgb != -1) {
                return rgb;
            }
        }
        return -1;
    }

    private void pushNotification(String text, String content, int time) {
        if (!isFtHelperNotificationsEnabled()) {
            return;
        }
        Notify.NOTIFICATION_MANAGER.add(text, content, time);
    }

    private void pushNotification(String text, String content, int time, NotificationManager.NotificationType type) {
        if (!isFtHelperNotificationsEnabled()) {
            return;
        }
        Notify.NOTIFICATION_MANAGER.add(text, content, time, type);
    }


    private int findAndTrowItem(int hbSlot, int invSlot) {
        int originalSlot = mc.player.inventory.currentItem;
        boolean isFreakTime = isConnectedToSkyTime();

        if (hbSlot != -1) {
            mc.player.connection.sendPacket(new CHeldItemChangePacket(hbSlot));
            mc.player.connection.sendPacket(new CPlayerTryUseItemPacket(Hand.MAIN_HAND));
            if (isFreakTime && (trapThrow || plastThrow)) {
                BlockRayTraceResult airplace = (BlockRayTraceResult) rayTrace(4.0F, mc.player.rotationYaw, mc.player.rotationPitch, mc.player);
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
            mc.player.connection.sendPacket(new CPlayerTryUseItemPacket(Hand.MAIN_HAND));
            if (isFreakTime && (trapThrow || plastThrow)) {
                BlockRayTraceResult airplace = (BlockRayTraceResult) rayTrace(4.0F, mc.player.rotationYaw, mc.player.rotationPitch, mc.player);
                mc.player.connection.sendPacket(new CPlayerTryUseItemOnBlockPacket(Hand.MAIN_HAND, airplace));
            }
            mc.player.swingArm(Hand.MAIN_HAND);
            this.delay = System.currentTimeMillis();
            return invSlot;
        }
    }

    @Override
    public boolean onDisable() {
        restoreChangedZombies();
        stopRazbaf();
        disorientationThrow = false;
        trapThrow = false;
        blatantThrow = false;
        plastThrow = false;
        otrigaThrow = false;
        serkaThrow = false;
        bowThrow = false;
        flameThrow = false;
        flashThrow = false;
        auraThrow = false;
        snowballThrow = false;
        killerThrow = false;
        medicThrow = false;
        winnerThrow = false;
        agentThrow = false;
        shulkerThrow = false;
        xlopyshkaThrow = false;
        svatvodaThrow = false;
        gnevkaThrow = false;
        paladinThrow = false;
        assasinThrow = false;
        radiaciaThrow = false;
        snotvornoeThrow = false;
        delay = 0;
        consumables.clear();
        currentTrap = null;
        updateCounter = 0;
        if (mc.ingameGUI != null) {
            mc.ingameGUI.setOverlayMessage(ITextComponent.getTextComponentOrEmpty(""), false);
        }
        super.onDisable();
        return false;
    }

    private boolean isInRange(Vector3d position) {
        if (mc.player == null) return false;
        double distance = mc.player.getPositionVec().distanceTo(position);
        return distance <= MAX_DISTANCE;
    }

    private void updateTrapMessage(String trapType, double seconds) {
        String messageText = TextFormatting.GREEN + trapType +
                TextFormatting.WHITE + " - " +
                TextFormatting.RED + String.format("%.1f", seconds) +
                TextFormatting.WHITE + " \u0441\u0435\u043a\u0443\u043d\u0434";
        mc.ingameGUI.setOverlayMessage(ITextComponent.getTextComponentOrEmpty(messageText), false);
    }

    private static List<BlockPos> getCube(BlockPos center, float radiusXZ, float radiusY) {
        List<BlockPos> positions = new ArrayList<>();
        int centerX = center.getX();
        int centerY = center.getY();
        int centerZ = center.getZ();

        for (int x = centerX - (int) radiusXZ; x <= centerX + radiusXZ; ++x) {
            for (int z = centerZ - (int) radiusXZ; z <= centerZ + radiusXZ; ++z) {
                for (int y = centerY - (int) radiusY; y < centerY + radiusY; ++y) {
                    positions.add(new BlockPos(x, y, z));
                }
            }
        }

        return positions;
    }

    private double getDistance(BlockPos pos1, BlockPos pos2) {
        double deltaX = pos1.getX() - pos2.getX();
        double deltaY = pos1.getY() - pos2.getY();
        double deltaZ = pos1.getZ() - pos2.getZ();
        return MathHelper.sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ);
    }

    private static class TrapData {
        public long time;
        public final Vector3d position;
        public final String type;
        public final long originalDuration;
        public boolean wasOutOfRange = false;
        public long pausedTime = 0;

        public TrapData(long time, Vector3d position, String type, long originalDuration) {
            this.time = time;
            this.position = position;
            this.type = type;
            this.originalDuration = originalDuration;
        }
    }

    private int getTrapItem(boolean inHotBar) {
        int firstSlot = inHotBar ? 0 : 9;
        int lastSlot = inHotBar ? 9 : 36;
        for (int i = firstSlot; i < lastSlot; i++) {
            ItemStack itemStack = mc.player.inventory.getStackInSlot(i);

            if (itemStack.getItem() instanceof AirItem) {
                continue;
            }

            String displayName = TextFormatting.getTextWithoutFormattingCodes(itemStack.getDisplayName().getString());
            if (displayName != null && displayName.toLowerCase().contains("трапка")) {
                return i;
            }

            List<ITextComponent> tooltip = itemStack.getTooltip(mc.player, mc.gameSettings.advancedItemTooltips ? ITooltipFlag.TooltipFlags.ADVANCED : ITooltipFlag.TooltipFlags.NORMAL);
            for (ITextComponent line : tooltip) {
                String tooltipText = TextFormatting.getTextWithoutFormattingCodes(line.getString()).toLowerCase();
                if (tooltipText.contains("создаёт клетку вокруг 3х3") || tooltipText.contains("нельзя сломать 15 секунд")) {
                    return i;
                }
            }
        }
        return -1;
    }

    private int getItemForName(String name, boolean inHotBar) {
        int firstSlot = inHotBar ? 0 : 9;
        int lastSlot = inHotBar ? 9 : 36;
        for (int i = firstSlot; i < lastSlot; i++) {
            ItemStack itemStack = mc.player.inventory.getStackInSlot(i);

            if (itemStack.getItem() instanceof AirItem) {
                continue;
            }

            String displayName = TextFormatting.getTextWithoutFormattingCodes(itemStack.getDisplayName().getString());
            if (displayName != null && displayName.toLowerCase().contains(name)) {
                return i;
            }
        }
        return -1;
    }

    private int getSplashPotionForEffectOrName(Effect effect, String name, boolean inHotBar) {
        int firstSlot = inHotBar ? 0 : 9;
        int lastSlot = inHotBar ? 9 : 36;
        for (int i = firstSlot; i < lastSlot; i++) {
            ItemStack itemStack = mc.player.inventory.getStackInSlot(i);

            if (!(itemStack.getItem() instanceof SplashPotionItem)) {
                continue;
            }

            for (EffectInstance effectInstance : PotionUtils.getEffectsFromStack(itemStack)) {
                if (effectInstance.getPotion() == effect) {
                    return i;
                }
            }

            if (itemTextContains(itemStack, name)) {
                return i;
            }
        }
        return -1;
    }

    private boolean itemTextContains(ItemStack itemStack, String text) {
        String needle = text.toLowerCase(Locale.ROOT);
        String displayName = TextFormatting.getTextWithoutFormattingCodes(itemStack.getDisplayName().getString());
        if (displayName != null && displayName.toLowerCase(Locale.ROOT).contains(needle)) {
            return true;
        }

        List<ITextComponent> tooltip = itemStack.getTooltip(mc.player, mc.gameSettings.advancedItemTooltips ? ITooltipFlag.TooltipFlags.ADVANCED : ITooltipFlag.TooltipFlags.NORMAL);
        for (ITextComponent line : tooltip) {
            String tooltipText = TextFormatting.getTextWithoutFormattingCodes(line.getString());
            if (tooltipText != null && tooltipText.toLowerCase(Locale.ROOT).contains(needle)) {
                return true;
            }
        }
        return false;
    }

    private int getItemForNBTContains(String nbt, boolean hotbar) {
        int start = hotbar ? 0 : 9;
        int end = hotbar ? 9 : 36;
        for (int i = start; i < end; i++) {
            ItemStack stack = mc.player.inventory.getStackInSlot(i);
            if (!(stack.getItem() instanceof AirItem) && stack.hasTag()) {
                String tag = stack.getTag().toString();
                if (tag != null && tag.toLowerCase().contains(nbt.toLowerCase())) {
                    return i;
                }
            }
        }
        return -1;
    }

    private int getItem(Item input, boolean inHotBar) {
        int firstSlot = inHotBar ? 0 : 9;
        int lastSlot = inHotBar ? 9 : 36;
        for (int i = firstSlot; i < lastSlot; i++) {
            ItemStack itemStack = mc.player.inventory.getStackInSlot(i);

            if (itemStack.getItem() instanceof AirItem) {
                continue;
            }
            if (itemStack.getItem() == input) {
                return i;
            }
        }
        return -1;
    }

    private boolean isPlayerNearby(float radius) {
        return mc.world.getPlayers().stream()
                .anyMatch(player -> player != mc.player &&
                        mc.player.getDistanceSq(player) <= radius * radius);
    }

    private boolean isConnectedToSkyTime() {
        return mc.getCurrentServerData() != null &&
                mc.getCurrentServerData().serverIP.toLowerCase().contains("skytime");
    }

    private Object rayTrace(float distance, float yaw, float pitch, Object player) {
        return null;
    }

    public static String convertTime(int seconds) {
        int hours = seconds / 3600;
        int minutes = seconds % 3600 / 60;
        int remainingSeconds = seconds % 60;
        String timeString = "";

        if (hours > 0) {
            int h = hours % 100;
            String hourSuffix;
            if (h >= 11 && h <= 14) {
                hourSuffix = "часов";
            } else {
                switch (hours % 10) {
                    case 1:
                        hourSuffix = "час";
                        break;
                    case 2:
                    case 3:
                    case 4:
                        hourSuffix = "часа";
                        break;
                    default:
                        hourSuffix = "часов";
                }
            }
            timeString = timeString + hours + " " + hourSuffix + " ";
        }

        if (minutes > 0) {
            int m = minutes % 100;
            String minuteSuffix;
            if (m >= 11 && m <= 14) {
                minuteSuffix = "минут";
            } else {
                switch (minutes % 10) {
                    case 1:
                        minuteSuffix = "минута";
                        break;
                    case 2:
                    case 3:
                    case 4:
                        minuteSuffix = "минуты";
                        break;
                    default:
                        minuteSuffix = "минут";
                }
            }
            timeString = timeString + minutes + " " + minuteSuffix + " ";
        }

        if (remainingSeconds > 0 || timeString.isEmpty()) {
            int s = remainingSeconds % 100;
            String secondSuffix;
            if (s >= 11 && s <= 14) {
                secondSuffix = "секунд";
            } else {
                switch (remainingSeconds % 10) {
                    case 1:
                        secondSuffix = "секунда";
                        break;
                    case 2:
                    case 3:
                    case 4:
                        secondSuffix = "секунды";
                        break;
                    default:
                        secondSuffix = "секунд";
                }
            }
            timeString = timeString + remainingSeconds + " " + secondSuffix;
        }

        return timeString.trim();
    }

    public ModeListSetting getOptions() {
        return this.options;
    }
}

