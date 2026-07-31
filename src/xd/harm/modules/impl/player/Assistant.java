package xd.harm.modules.impl.player;

import com.google.common.eventbus.Subscribe;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.AirItem;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ArmorMaterial;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.play.server.SEntityStatusPacket;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.text.Color;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;
import org.lwjgl.opengl.GL11;
import xd.harm.Harmony;
import xd.harm.config.FriendStorage;
import xd.harm.events.network.EventPacket;
import xd.harm.events.render.EventDisplay;
import xd.harm.events.world.EventUpdate;
import xd.harm.modules.api.Category;
import xd.harm.modules.api.Module;
import xd.harm.modules.api.ModuleRegister;
import xd.harm.modules.settings.Setting;
import xd.harm.modules.settings.impl.BooleanSetting;
import xd.harm.modules.settings.impl.CategorySetting;
import xd.harm.modules.settings.impl.ModeListSetting;
import xd.harm.modules.settings.impl.ModeSetting;
import xd.harm.modules.settings.impl.SliderSetting;
import xd.harm.utils.SoundUtil;
import xd.harm.utils.animations.Animation;
import xd.harm.utils.animations.Direction;
import xd.harm.utils.animations.GifUtil;
import xd.harm.utils.animations.impl.EaseInOutQuad;
import xd.harm.utils.client.KeyStorage;
import xd.harm.utils.drag.Dragging;
import xd.harm.utils.render.color.ColorUtils;
import xd.harm.utils.render.font.Fonts;
import xd.harm.utils.render.rect.RenderUtility;
import xd.harm.utils.text.GradientUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

@ModuleRegister(name = "Assistant", category = Category.Player, desc = "ЮнЮн и подсказки расходников")
public class Assistant extends Module {
    private final SliderSetting yunyunGifSize = new SliderSetting("Размер ЮнЮн", 110, 20, 180, 1);
    private final SliderSetting yunyunSoundVolume = new SliderSetting("Громкость ЮнЮн", 80, 0, 100, 1);
    private final SliderSetting hintDuration = new SliderSetting("Время подсказки", 5.0f, 1.0f, 15.0f, 0.5f);
    private final CategorySetting assistantHintsCategory = new CategorySetting("Подсказки расходников");
    private final ModeSetting assistantSearchMode = new ModeSetting("Поиск подсказок", "Фантайм", "Фантайм", "Копии");
    private final ModeSetting assistantItemsMode = new ModeSetting("Предметы подсказок", "Новые", "Новые", "Старые");
    private final ModeListSetting assistantItems = new ModeListSetting("Предлагать предметы",
            new BooleanSetting("Дезориентация", false),
            new BooleanSetting("Трапка", false),
            new BooleanSetting("Явная пыль", false),
            new BooleanSetting("Огненный смерч", false),
            new BooleanSetting("Божья аура", false),
            new BooleanSetting("Снежок", false),
            new BooleanSetting("Пласт", false),
            (BooleanSetting) new BooleanSetting("Зелье отрыжки", false).setVisible(() -> assistantItemsMode.is("Старые")),
            (BooleanSetting) new BooleanSetting("Серная кислота", false).setVisible(() -> assistantItemsMode.is("Старые")),
            (BooleanSetting) new BooleanSetting("Зелье Киллера", false).setVisible(() -> assistantItemsMode.is("Старые")),
            (BooleanSetting) new BooleanSetting("Зелье Медика", false).setVisible(() -> assistantItemsMode.is("Старые")),
            (BooleanSetting) new BooleanSetting("Зелье Победителя", false).setVisible(() -> assistantItemsMode.is("Старые")),
            (BooleanSetting) new BooleanSetting("Зелье Агента", false).setVisible(() -> assistantItemsMode.is("Старые")),
            (BooleanSetting) new BooleanSetting("Сопли флеша", false).setVisible(() -> assistantItemsMode.is("Старые")),
            (BooleanSetting) new BooleanSetting("Хлопушка", false).setVisible(() -> assistantItemsMode.is("Новые")),
            (BooleanSetting) new BooleanSetting("Святая вода", false).setVisible(() -> assistantItemsMode.is("Новые")),
            (BooleanSetting) new BooleanSetting("Зелье гнева", false).setVisible(() -> assistantItemsMode.is("Новые")),
            (BooleanSetting) new BooleanSetting("Зелье паладина", false).setVisible(() -> assistantItemsMode.is("Новые")),
            (BooleanSetting) new BooleanSetting("Зелье ассасина", false).setVisible(() -> assistantItemsMode.is("Новые")),
            (BooleanSetting) new BooleanSetting("Зелье радиации", false).setVisible(() -> assistantItemsMode.is("Новые")),
            (BooleanSetting) new BooleanSetting("Снотворное", false).setVisible(() -> assistantItemsMode.is("Новые"))
    );
    private final Map<String, ModeListSetting> itemTriggers = new LinkedHashMap<>();
    private final Map<String, ModeListSetting> itemTargets = new LinkedHashMap<>();
    private final SliderSetting assistantMyLowHpThreshold = (SliderSetting) new SliderSetting("Порог ХП подсказки", 6.0f, 1.0f, 20.0f, 0.5f)
            .setVisible(() -> anyItemHasTrigger("Мало ХП у меня"));
    private final SliderSetting assistantEnemyLowHpThreshold = (SliderSetting) new SliderSetting("Порог ХП врага подсказки", 6.0f, 1.0f, 20.0f, 0.5f)
            .setVisible(() -> anyItemHasTrigger("Мало ХП у противника"));

    private final Dragging yunyunDrag = Harmony.getInstance().createDrag(this, "AssistantYunyun", 200, 50);
    private final Dragging hintDragging = Harmony.getInstance().createDrag(this, "AssistantHints", 100, 100);

    private final GifUtil yunyunGif = new GifUtil("harmony/images/gifs/yunyun.gif");
    private final GifUtil yunyunLowHpGif = new GifUtil("harmony/images/gifs/yunyun_lowhp.gif");
    private final GifUtil yunyunEnemyGif = new GifUtil("harmony/images/gifs/yunyun_enemy.gif");
    private final GifUtil yunyunTotemGif = new GifUtil("harmony/images/gifs/yunyun_totem.gif");
    private final GifUtil yunyunSuggestGif = new GifUtil("harmony/images/gifs/yunyun_suggest.gif");
    private GifUtil currentGif = yunyunGif;

    private final List<ItemHint> activeHints = new ArrayList<>();
    private final Map<String, Long> hintCooldowns = new HashMap<>();
    private final Map<ResourceLocation, Long> yunyunSoundCooldowns = new HashMap<>();
    private final List<AssistantItem> assistantHintItems = new ArrayList<>();
    private final Map<String, PlayerTotemData> assistantEnemyTotems = new HashMap<>();

    private float animatedX = -1;
    private float animatedY = -1;
    private float animatedScale = 1.0f;
    private float animatedStartX = -1;
    private float animatedTotalWidth = 140;
    private float suggestScale = 1.0f;

    private long yunyunSelfTotemUntilMs = 0L;
    private long yunyunEnemyActionUntilMs = 0L;
    private static final long YUNYUN_SELF_TOTEM_DURATION_MS = 1500L;
    private static final long YUNYUN_ENEMY_ACTION_DURATION_MS = 800L;
    private static final long YUNYUN_SOUND_COOLDOWN_MS = 2000L;
    private static final float YUNYUN_LOW_HP_RATIO = 0.35F;
    private static final float ASSISTANT_RANGE = 10.0F;
    private long assistantLastMyLowHpCheck = 0L;
    private long assistantLastEnemyLowHpCheck = 0L;
    private long assistantSelfTotemHintUntilMs = 0L;
    private boolean cachedMyLowHp = false;
    private boolean cachedEnemyLowHp = false;

    public Assistant() {
        yunyunDrag.setAllowOffscreen(true);
        initAssistantHintItems();
        initItemTriggers();
        addSettings(buildSettings());
    }

    private Setting<?>[] buildSettings() {
        List<Setting<?>> settings = new ArrayList<>();
        settings.add(yunyunGifSize);
        settings.add(yunyunSoundVolume);
        settings.add(hintDuration);
        settings.add(assistantHintsCategory);
        settings.add(assistantSearchMode);
        settings.add(assistantItemsMode);
        settings.add(assistantItems);
        for (String name : itemTriggers.keySet()) {
            settings.add(itemTriggers.get(name));
            if (itemTargets.containsKey(name)) {
                settings.add(itemTargets.get(name));
            }
        }
        settings.add(assistantMyLowHpThreshold);
        settings.add(assistantEnemyLowHpThreshold);
        return settings.toArray(new Setting<?>[0]);
    }

    private void initAssistantHintItems() {
        assistantHintItems.add(new AssistantItem("Дезориентация", ItemGroup.COMMON, Items.ENDER_EYE, "desorientation", "дезориентация"));
        assistantHintItems.add(new AssistantItem("Трапка", ItemGroup.COMMON, Items.NETHERITE_SCRAP, "trap", "трапка"));
        assistantHintItems.add(new AssistantItem("Явная пыль", ItemGroup.COMMON, Items.SUGAR, "sheerdust", "явная"));
        assistantHintItems.add(new AssistantItem("Огненный смерч", ItemGroup.COMMON, Items.FIRE_CHARGE, "fierytornado", "огненный"));
        assistantHintItems.add(new AssistantItem("Божья аура", ItemGroup.COMMON, Items.PHANTOM_MEMBRANE, "godsaura", "аура").withoutTargetTrigger());
        assistantHintItems.add(new AssistantItem("Снежок", ItemGroup.COMMON, Items.SNOWBALL, null, "снежок").fallbackItem(Items.SNOWBALL));
        assistantHintItems.add(new AssistantItem("Пласт", ItemGroup.COMMON, Items.DRIED_KELP, "stratum", "пласт"));
        assistantHintItems.add(new AssistantItem("Зелье отрыжки", ItemGroup.OLD, Items.SPLASH_POTION, "burp", "отрыж"));
        assistantHintItems.add(new AssistantItem("Серная кислота", ItemGroup.OLD, Items.SPLASH_POTION, "sulfuric-acid", "серн"));
        assistantHintItems.add(new AssistantItem("Зелье Киллера", ItemGroup.OLD, Items.SPLASH_POTION, "potion-killer", "киллера"));
        assistantHintItems.add(new AssistantItem("Зелье Медика", ItemGroup.OLD, Items.SPLASH_POTION, "potion-medic", "медика"));
        assistantHintItems.add(new AssistantItem("Зелье Победителя", ItemGroup.OLD, Items.SPLASH_POTION, "potion-winner", "победителя"));
        assistantHintItems.add(new AssistantItem("Зелье Агента", ItemGroup.OLD, Items.SPLASH_POTION, "potion-agent", "агента"));
        assistantHintItems.add(new AssistantItem("Сопли флеша", ItemGroup.OLD, Items.SPLASH_POTION, "flash", "флеш", "сопли"));
        assistantHintItems.add(new AssistantItem("Хлопушка", ItemGroup.NEW, Items.SPLASH_POTION, null, "хлопушка"));
        assistantHintItems.add(new AssistantItem("Святая вода", ItemGroup.NEW, Items.SPLASH_POTION, null, "святая вода"));
        assistantHintItems.add(new AssistantItem("Зелье гнева", ItemGroup.NEW, Items.SPLASH_POTION, null, "зелье гнева"));
        assistantHintItems.add(new AssistantItem("Зелье паладина", ItemGroup.NEW, Items.SPLASH_POTION, null, "паладина", "палладина"));
        assistantHintItems.add(new AssistantItem("Зелье ассасина", ItemGroup.NEW, Items.SPLASH_POTION, null, "ассасина"));
        assistantHintItems.add(new AssistantItem("Зелье радиации", ItemGroup.NEW, Items.SPLASH_POTION, null, "радиации"));
        assistantHintItems.add(new AssistantItem("Снотворное", ItemGroup.NEW, Items.SPLASH_POTION, null, "снотворное"));
    }

    private void initItemTriggers() {
        for (AssistantItem item : assistantHintItems) {
            String name = item.displayName;
            ModeListSetting triggers = new ModeListSetting("Триггеры: " + name,
                    new BooleanSetting("Цель в радиусе", false),
                    new BooleanSetting("Мало ХП у меня", false),
                    new BooleanSetting("Мало ХП у противника", false),
                    new BooleanSetting("Снесли Тотем мне", false),
                    new BooleanSetting("Снёс Тотем я", false)
            );
            triggers.setVisible(() -> assistantItems.getValueByName(name).get());
            itemTriggers.put(name, triggers);

            ModeListSetting targets = new ModeListSetting("Цели: " + name,
                    new BooleanSetting("Незеритовая броня", false),
                    new BooleanSetting("Алмазная броня", false),
                    new BooleanSetting("Элитры", false)
            );
            targets.setVisible(() -> assistantItems.getValueByName(name).get()
                    && itemTriggers.containsKey(name)
                    && itemTriggers.get(name).getValueByName("Цель в радиусе").get());
            itemTargets.put(name, targets);
        }
    }

    private boolean anyItemHasTrigger(String triggerName) {
        for (AssistantItem item : assistantHintItems) {
            if (!isAssistantItemEnabled(item)) continue;
            ModeListSetting triggers = itemTriggers.get(item.displayName);
            if (triggers != null && triggers.getValueByName(triggerName).get()) return true;
        }
        return false;
    }

    @Override
    public boolean onDisable() {
        activeHints.clear();
        hintCooldowns.clear();
        yunyunSoundCooldowns.clear();
        assistantEnemyTotems.clear();
        assistantSelfTotemHintUntilMs = 0L;
        return super.onDisable();
    }

    @Subscribe
    private void onUpdate(EventUpdate e) {
        if (mc.gameSettings.showDebugInfo) {
            return;
        }

        updateAssistantHints();
        updateHintTimers();
        updateYunyunSelection();
        if (currentGif != null) {
            currentGif.update();
        }
        if (currentGif != yunyunSuggestGif) {
            yunyunSuggestGif.update();
        }
    }

    @Subscribe
    private void onDisplay(EventDisplay e) {
        if (mc.gameSettings.showDebugInfo) {
            return;
        }

        if (e.getType() == EventDisplay.Type.HIGH) {
            renderYunyun(e);
        }
    }

    @Subscribe
    private void onPacket(EventPacket e) {
        handleYunyunStatePacket(e);
    }

    public void addHint(String itemName, String reason, int slot, ItemStack itemStack, int keyBind) {
        float maxTime = hintDuration.get().floatValue();
        long currentTime = System.currentTimeMillis();

        for (ItemHint hint : activeHints) {
            if (hint.itemName.equals(itemName)) {
                hint.displayTime = maxTime;
                if (hint.removing) {
                    hint.removing = false;
                    hint.animation.setDirection(Direction.FORWARDS);
                }
                return;
            }
        }

        if (hintCooldowns.containsKey(itemName)) {
            long lastTime = hintCooldowns.get(itemName);
            if (currentTime - lastTime < (long) (maxTime * 1000) + 1000) {
                return;
            }
        }

        hintCooldowns.put(itemName, currentTime);
        activeHints.add(new ItemHint(itemName, reason, slot, maxTime, itemStack, keyBind));
    }

    private void updateHintTimers() {
        float deltaTime = 1.0f / 20.0f;
        Iterator<ItemHint> iterator = activeHints.iterator();
        while (iterator.hasNext()) {
            ItemHint hint = iterator.next();
            hint.displayTime -= deltaTime;
            if (hint.displayTime <= 0 && !hint.removing) {
                hint.removing = true;
                hint.animation.setDirection(Direction.BACKWARDS);
            }
            if (hint.animation.finished(Direction.BACKWARDS)) {
                iterator.remove();
            }
        }
    }

    private void updateAssistantHints() {
        if (mc.player == null || mc.world == null) {
            return;
        }

        long now = System.currentTimeMillis();

        boolean myLowHp = cachedMyLowHp;
        if (anyItemHasTrigger("Мало ХП у меня") && now - assistantLastMyLowHpCheck >= 1000L) {
            assistantLastMyLowHpCheck = now;
            float currentHealth = mc.player.getHealth() + mc.player.getAbsorptionAmount();
            myLowHp = currentHealth <= assistantMyLowHpThreshold.get();
            cachedMyLowHp = myLowHp;
        }

        boolean enemyLowHp = cachedEnemyLowHp;
        if (anyItemHasTrigger("Мало ХП у противника") && now - assistantLastEnemyLowHpCheck >= 500L) {
            assistantLastEnemyLowHpCheck = now;
            PlayerEntity lowHpTarget = findLowHpAssistantTarget();
            enemyLowHp = lowHpTarget != null;
            cachedEnemyLowHp = enemyLowHp;
        }

        boolean selfTotemPopped = false;
        if (assistantSelfTotemHintUntilMs > 0L && now >= assistantSelfTotemHintUntilMs) {
            assistantSelfTotemHintUntilMs = 0L;
            selfTotemPopped = true;
        }

        boolean enemyTotemPopped = !assistantEnemyTotems.isEmpty();
        if (enemyTotemPopped) {
            assistantEnemyTotems.clear();
        }

        for (AssistantItem item : assistantHintItems) {
            if (!isAssistantItemEnabled(item)) {
                continue;
            }

            ModeListSetting triggers = itemTriggers.get(item.displayName);
            if (triggers == null) {
                continue;
            }

            String reason = null;
            if (triggers.getValueByName("Цель в радиусе").get()) {
                ModeListSetting targets = itemTargets.get(item.displayName);
                if (targets != null) {
                    PlayerEntity target = findAssistantTarget(targets);
                    if (target != null) {
                        reason = "Цель в радиусе";
                    }
                }
            }
            if (reason == null && myLowHp && triggers.getValueByName("Мало ХП у меня").get()) {
                reason = "Мало ХП у тебя";
            }
            if (reason == null && enemyLowHp && triggers.getValueByName("Мало ХП у противника").get()) {
                reason = "Мало ХП у врага";
            }
            if (reason == null && selfTotemPopped && triggers.getValueByName("Снесли Тотем мне").get()) {
                reason = "Снесли тотем тебе";
            }
            if (reason == null && enemyTotemPopped && triggers.getValueByName("Снёс Тотем я").get()) {
                reason = "Снёс тотем врагу";
            }

            if (reason != null) {
                int slot = findAssistantItemSlot(item);
                if (slot != -1 && !mc.player.getCooldownTracker().hasCooldown(item.cooldownItem)) {
                    addHint(item.displayName, reason, slot, mc.player.inventory.getStackInSlot(slot), getKeyBindForItem(item.displayName));
                }
            }
        }

        for (ItemHint hint : activeHints) {
            if (hint.removing) continue;

            AssistantItem assistantItem = null;
            for (AssistantItem item : assistantHintItems) {
                if (item.displayName.equals(hint.itemName)) {
                    assistantItem = item;
                    break;
                }
            }
            if (assistantItem != null && findAssistantItemSlot(assistantItem) == -1) {
                hint.removing = true;
                hint.animation.setDirection(Direction.BACKWARDS);
                continue;
            }

            boolean triggerActive;
            switch (hint.reason) {
                case "Цель в радиусе":
                    ModeListSetting targets = assistantItem != null ? itemTargets.get(assistantItem.displayName) : null;
                    triggerActive = targets != null && findAssistantTarget(targets) != null;
                    break;
                case "Мало ХП у тебя": triggerActive = myLowHp; break;
                case "Мало ХП у врага": triggerActive = enemyLowHp; break;
                default: continue;
            }
            if (!triggerActive) {
                hint.removing = true;
                hint.animation.setDirection(Direction.BACKWARDS);
            }
        }
    }

    private boolean isAssistantItemEnabled(AssistantItem item) {
        if (!assistantItems.getValueByName(item.displayName).get()) {
            return false;
        }
        if (item.group == ItemGroup.OLD && !assistantItemsMode.is("Старые")) {
            return false;
        }
        if (item.group == ItemGroup.NEW && !assistantItemsMode.is("Новые")) {
            return false;
        }
        return true;
    }

    private int findAssistantItemSlot(AssistantItem item) {
        int hotbarSlot = findAssistantItemSlot(item, true);
        if (hotbarSlot != -1) {
            return hotbarSlot;
        }
        return findAssistantItemSlot(item, false);
    }

    private int findAssistantItemSlot(AssistantItem item, boolean hotbar) {
        if (item.displayName.equals("Трапка") && assistantSearchMode.is("Копии")) {
            return getTrapItem(hotbar);
        }
        if (assistantSearchMode.is("Фантайм") && item.nbtToken != null) {
            int nbtSlot = getItemForNBTContains(item.nbtToken, hotbar);
            if (nbtSlot != -1) {
                return nbtSlot;
            }
        }
        for (String nameToken : item.nameTokens) {
            int nameSlot = getItemForName(nameToken, hotbar);
            if (nameSlot != -1) {
                return nameSlot;
            }
        }
        if (item.fallbackItem != null) {
            return getItem(item.fallbackItem, hotbar);
        }
        return -1;
    }

    private void markAssistantEnemyTotem(Entity entity) {
        if (!(entity instanceof PlayerEntity)) {
            return;
        }
        PlayerEntity player = (PlayerEntity) entity;
        if (FriendStorage.isFriend(player.getName().getString())) {
            return;
        }
        if (mc.player == null || mc.player.getDistanceSq(player) > ASSISTANT_RANGE * ASSISTANT_RANGE) {
            return;
        }
        assistantEnemyTotems.put(player.getName().getString(), new PlayerTotemData(System.currentTimeMillis()));
    }

    private PlayerEntity findAssistantTarget(ModeListSetting targets) {
        for (PlayerEntity player : mc.world.getPlayers()) {
            if (!isValidAssistantTarget(player)) {
                continue;
            }
            if (matchesAssistantTargetArmor(player, targets)) {
                return player;
            }
        }
        return null;
    }

    private PlayerEntity findLowHpAssistantTarget() {
        for (PlayerEntity player : mc.world.getPlayers()) {
            if (!isValidAssistantTarget(player)) {
                continue;
            }
            float enemyHealth = player.getHealth() + player.getAbsorptionAmount();
            if (enemyHealth <= assistantEnemyLowHpThreshold.get()) {
                return player;
            }
        }
        return null;
    }

    private boolean isValidAssistantTarget(PlayerEntity player) {
        if (player == null || player == mc.player) {
            return false;
        }
        if (FriendStorage.isFriend(player.getName().getString())) {
            return false;
        }
        return mc.player.getDistanceSq(player) <= ASSISTANT_RANGE * ASSISTANT_RANGE;
    }

    private boolean matchesAssistantTargetArmor(PlayerEntity player, ModeListSetting targets) {
        boolean hasElytra = hasElytra(player);
        ArmorType armorType = getArmorType(player);

        if (targets.getValueByName("Элитры").get() && hasElytra) {
            return true;
        }
        if (targets.getValueByName("Незеритовая броня").get() && armorType == ArmorType.NETHERITE && !hasElytra) {
            return true;
        }
        return targets.getValueByName("Алмазная броня").get() && armorType == ArmorType.DIAMOND && !hasElytra;
    }

    private ArmorType getArmorType(PlayerEntity player) {
        int diamondPieces = 0;
        int netheritePieces = 0;
        int totalPieces = 0;

        for (EquipmentSlotType slot : new EquipmentSlotType[]{EquipmentSlotType.HEAD, EquipmentSlotType.CHEST, EquipmentSlotType.LEGS, EquipmentSlotType.FEET}) {
            ItemStack stack = player.getItemStackFromSlot(slot);
            if (slot == EquipmentSlotType.CHEST && stack.getItem() == Items.ELYTRA) {
                continue;
            }
            if (!stack.isEmpty() && stack.getItem() instanceof ArmorItem) {
                totalPieces++;
                ArmorMaterial material = (ArmorMaterial) ((ArmorItem) stack.getItem()).getArmorMaterial();
                if (material == ArmorMaterial.DIAMOND) {
                    diamondPieces++;
                } else if (material == ArmorMaterial.NETHERITE) {
                    netheritePieces++;
                }
            }
        }

        if (totalPieces == 0) {
            return ArmorType.NONE;
        }
        if (netheritePieces >= 2) {
            return ArmorType.NETHERITE;
        }
        if (diamondPieces >= 2) {
            return ArmorType.DIAMOND;
        }
        return ArmorType.OTHER;
    }

    private boolean hasElytra(PlayerEntity player) {
        return player.getItemStackFromSlot(EquipmentSlotType.CHEST).getItem() == Items.ELYTRA;
    }

    private int getTrapItem(boolean hotbar) {
        int start = hotbar ? 0 : 9;
        int end = hotbar ? 9 : 36;
        for (int i = start; i < end; i++) {
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

    private int getItemForName(String name, boolean hotbar) {
        int start = hotbar ? 0 : 9;
        int end = hotbar ? 9 : 36;
        for (int i = start; i < end; i++) {
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

    private int getItem(Item item, boolean hotbar) {
        int start = hotbar ? 0 : 9;
        int end = hotbar ? 9 : 36;
        for (int i = start; i < end; i++) {
            ItemStack itemStack = mc.player.inventory.getStackInSlot(i);
            if (!(itemStack.getItem() instanceof AirItem) && itemStack.getItem() == item) {
                return i;
            }
        }
        return -1;
    }

    private int getKeyBindForItem(String itemName) {
        FTHelper ftHelper = Harmony.getInstance().getModuleManager().getFTHelper();
        if (ftHelper == null) {
            return -1;
        }

        switch (itemName) {
            case "Дезориентация":
                return ftHelper.disorientationKey.get();
            case "Трапка":
                return ftHelper.trapKey.get();
            case "Явная пыль":
                return ftHelper.blatantKey.get();
            case "Огненный смерч":
                return ftHelper.flameKey.get();
            case "Божья аура":
                return ftHelper.auraKey.get();
            case "Снежок":
                return ftHelper.snowballKey.get();
            case "Пласт":
                return ftHelper.plastKey.get();
            case "Зелье отрыжки":
                return ftHelper.otrigaKey.get();
            case "Серная кислота":
                return ftHelper.serkaKey.get();
            case "Зелье Киллера":
                return ftHelper.killerKey.get();
            case "Зелье Медика":
                return ftHelper.medicKey.get();
            case "Зелье Победителя":
                return ftHelper.winnerKey.get();
            case "Зелье Агента":
                return ftHelper.agentKey.get();
            case "Сопли флеша":
                return ftHelper.flashKey.get();
            case "Хлопушка":
                return ftHelper.xlopyshkaKey.get();
            case "Святая вода":
                return ftHelper.svatvodaKey.get();
            case "Зелье гнева":
                return ftHelper.gnevkaKey.get();
            case "Зелье паладина":
                return ftHelper.paladinKey.get();
            case "Зелье ассасина":
                return ftHelper.assasinKey.get();
            case "Зелье радиации":
                return ftHelper.radiaciaKey.get();
            case "Снотворное":
                return ftHelper.snotvornoeKey.get();
            default:
                return -1;
        }
    }

    private void handleYunyunStatePacket(EventPacket e) {
        if (mc.world == null || mc.player == null || !(e.getPacket() instanceof SEntityStatusPacket)) {
            return;
        }

        SEntityStatusPacket packet = (SEntityStatusPacket) e.getPacket();
        Entity packetEntity = packet.getEntity(mc.world);
        if (!(packetEntity instanceof LivingEntity)) {
            return;
        }

        long now = System.currentTimeMillis();
        if (packet.getOpCode() == 35) {
            if (packetEntity == mc.player) {
                yunyunSelfTotemUntilMs = now + YUNYUN_SELF_TOTEM_DURATION_MS;
                assistantSelfTotemHintUntilMs = now + 100L;
                playYunyunSound(SoundUtil.YUNYUN_TOTEM_SOUND);
            } else {
                markAssistantEnemyTotem(packetEntity);
                if (isCurrentCombatTarget(packetEntity)) {
                    yunyunEnemyActionUntilMs = now + YUNYUN_ENEMY_ACTION_DURATION_MS;
                    playYunyunSound(SoundUtil.YUNYUN_ENEMY_SOUND);
                }
            }
        } else if (packet.getOpCode() == 3 && packetEntity != mc.player && isCurrentCombatTarget(packetEntity)) {
            yunyunEnemyActionUntilMs = now + YUNYUN_ENEMY_ACTION_DURATION_MS;
            playYunyunSound(SoundUtil.YUNYUN_ENEMY_SOUND);
        }
    }

    private boolean isCurrentCombatTarget(Entity packetEntity) {
        if (packetEntity == null || Harmony.getInstance().getModuleManager().getHitAura() == null) {
            return false;
        }

        LivingEntity auraTarget = Harmony.getInstance().getModuleManager().getHitAura().getTarget();
        return auraTarget != null && auraTarget.getEntityId() == packetEntity.getEntityId();
    }

    private GifUtil getYunyunStateGif() {
        long now = System.currentTimeMillis();
        if (now < yunyunSelfTotemUntilMs) {
            return yunyunTotemGif;
        }
        if (now < yunyunEnemyActionUntilMs) {
            return yunyunEnemyGif;
        }
        if (isLocalPlayerLowHp()) {
            return yunyunLowHpGif;
        }
        if (!activeHints.isEmpty()) {
            return yunyunSuggestGif;
        }
        return yunyunGif;
    }

    private boolean isLocalPlayerLowHp() {
        if (mc.player == null) {
            return false;
        }
        float maxHealth = Math.max(1.0f, mc.player.getMaxHealth());
        return mc.player.getHealth() / maxHealth <= YUNYUN_LOW_HP_RATIO;
    }

    private void playYunyunSound(ResourceLocation soundLocation) {
        long now = System.currentTimeMillis();
        Long lastPlayTime = yunyunSoundCooldowns.get(soundLocation);
        if (lastPlayTime != null && now - lastPlayTime < YUNYUN_SOUND_COOLDOWN_MS) {
            return;
        }

        yunyunSoundCooldowns.put(soundLocation, now);
        SoundUtil.playYunyunSound(soundLocation, yunyunSoundVolume.get().floatValue() / 100.0F);
    }

    private void updateYunyunSelection() {
        GifUtil selectedGif = getYunyunStateGif();
        if (currentGif == selectedGif) {
            return;
        }

        currentGif = selectedGif;
        if (currentGif != null) {
            currentGif.reset();
        }
        if (selectedGif == yunyunLowHpGif) {
            playYunyunSound(SoundUtil.YUNYUN_LOW_HP_SOUND);
        } else if (selectedGif == yunyunSuggestGif) {
            playYunyunSound(SoundUtil.YUNYUN_SUGGEST_SOUND);
        }
    }

    private void renderYunyun(EventDisplay e) {
        if (currentGif == null || !currentGif.isLoaded()) {
            return;
        }

        boolean hasHints = !activeHints.isEmpty();
        boolean isSuggest = currentGif == yunyunSuggestGif && hasHints;
        float targetSuggestScale = isSuggest ? 1.4f : 1.0f;
        suggestScale += (targetSuggestScale - suggestScale) * 0.12f;

        float baseX = yunyunDrag.getX() + yunyunDrag.getWobbleX();
        float baseY = yunyunDrag.getY() + yunyunDrag.getWobbleY();
        float baseHeight = yunyunGifSize.get().floatValue();
        float aspectRatio = (currentGif.getGifWidth() > 0 && currentGif.getGifHeight() > 0)
                ? (float) currentGif.getGifWidth() / currentGif.getGifHeight()
                : (700f / 353f);
        float baseWidth = baseHeight * aspectRatio;

        float height = baseHeight * suggestScale;
        float width = baseWidth * suggestScale;
        float x = baseX - (width - baseWidth) / 2f;
        float y = baseY - (height - baseHeight);

        yunyunDrag.setWidth(baseWidth);
        yunyunDrag.setHeight(baseHeight);

        ResourceLocation currentFrame = currentGif.getCurrentFrame();
        if (currentFrame == null) {
            return;
        }

        float grabScale = yunyunDrag.getGrabScale();
        float grabRot = yunyunDrag.getWobbleAngle();
        boolean grab = Math.abs(grabScale - 1.0f) > 0.001f || Math.abs(grabRot) > 0.001f;
        if (grab) {
            GL11.glPushMatrix();
            if (Math.abs(grabScale - 1.0f) > 0.001f) {
                RenderUtility.customScaledObject2D(x, y, width, height, grabScale);
            }
            if (Math.abs(grabRot) > 0.001f) {
                RenderUtility.customRotatedObject2D(x, y, width, height, grabRot);
            }
        }

        RenderSystem.pushMatrix();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.color4f(1.0f, 1.0f, 1.0f, 1.0f);
        mc.getTextureManager().bindTexture(currentFrame);

        boolean flipped = x + width / 2f > mc.getMainWindow().getScaledWidth() / 2f;
        drawGifFrame(e, x, y, width, height, flipped);

        RenderSystem.disableBlend();
        RenderSystem.popMatrix();

        if (hasHints) {
            renderSuggestItems(e.getMatrixStack(), x, y, width, height);
        }

        if (grab) {
            GL11.glPopMatrix();
        }
    }

    private void renderSuggestItems(MatrixStack ms, float yunyunX, float yunyunY, float yunyunW, float yunyunH) {
        int count = Math.min(activeHints.size(), 5);
        float itemSize = yunyunH * 0.15f;
        float fontSize = 5.0f;
        float bindFontSize = 5.5f;
        float spacing = 2f;
        float columnWidth = Math.max(itemSize, 38);
        float columnGap = 4f;

        float totalWidth = count * columnWidth + (count - 1) * columnGap;
        float centerX = yunyunX + yunyunW / 2f - 4;
        float startX = centerX - totalWidth / 2f;

        float totalHeight = itemSize + spacing + fontSize + spacing + bindFontSize;
        float headTopY = yunyunY + yunyunH * 0.08f;
        float topY = headTopY - totalHeight - 3f;

        for (int i = 0; i < count; i++) {
            ItemHint hint = activeHints.get(i);
            if (hint.itemStack == null || hint.itemStack.isEmpty()) continue;
            float alpha = (float) hint.animation.getOutput();
            float colCenterX = startX + i * (columnWidth + columnGap) + columnWidth / 2f;

            float itemScale = (itemSize / 16.0f) * alpha;
            float offsetX = (itemSize - itemSize * alpha) / 2;
            float offsetY = (itemSize - itemSize * alpha) / 2;
            float itemX = colCenterX - itemSize / 2f;

            RenderSystem.pushMatrix();
            RenderSystem.translatef(itemX + offsetX, topY + offsetY, 200);
            RenderSystem.scalef(itemScale, itemScale, 1.0f);
            mc.getItemRenderer().renderItemAndEffectIntoGUI(hint.itemStack, 0, 0);
            RenderSystem.popMatrix();

            float textY = topY + itemSize + spacing;
            ITextComponent reasonGradient = GradientUtil.gradient(hint.reason);
            float reasonWidth = getGradientWidth(reasonGradient, fontSize);
            drawGradientParts(ms, reasonGradient, colCenterX - reasonWidth / 2f, textY, fontSize, alpha, true);

            textY += fontSize + spacing;
            String bindText = "[" + getKeyName(hint.keyBind) + "]";
            ITextComponent bindGradient = GradientUtil.gradient(bindText);
            float bindWidth = getGradientWidth(bindGradient, bindFontSize);
            drawGradientParts(ms, bindGradient, colCenterX - bindWidth / 2f, textY, bindFontSize, alpha, true);
        }
    }

    private void drawGifFrame(EventDisplay e, float x, float y, float width, float height, boolean flipped) {
        Matrix4f matrix = e.getMatrixStack().getLast().getMatrix();
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();

        float minU = flipped ? 1.0f : 0.0f;
        float maxU = flipped ? 0.0f : 1.0f;

        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
        buffer.pos(matrix, x, y + height, 0.0f).tex(minU, 1.0f).endVertex();
        buffer.pos(matrix, x + width, y + height, 0.0f).tex(maxU, 1.0f).endVertex();
        buffer.pos(matrix, x + width, y, 0.0f).tex(maxU, 0.0f).endVertex();
        buffer.pos(matrix, x, y, 0.0f).tex(minU, 0.0f).endVertex();
        tessellator.draw();
    }

    private void renderHints(EventDisplay e) {
        if (activeHints.isEmpty() || mc.player == null || mc.world == null) {
            return;
        }

        MatrixStack ms = e.getMatrixStack();

        if (animatedX == -1) {
            animatedX = hintDragging.getX();
            animatedY = hintDragging.getY();
        }

        float targetX = hintDragging.getX();
        float targetY = hintDragging.getY();
        animatedX += (targetX - animatedX) * 0.15f;
        animatedY += (targetY - animatedY) * 0.15f;

        int hintCount = Math.min(activeHints.size(), 5);
        float targetScale = hintCount > 1 ? 0 : 1;
        animatedScale += (targetScale - animatedScale) * 0.08f;

        float baseIconSize = 32;
        float baseItemWidth = 140;
        float baseFontSize = 7;
        float baseBindFontSize = 8;
        float baseLineHeight = 11;
        float smallIconSize = 24;
        float smallItemWidth = 55;
        float smallFontSize = 5.5f;
        float smallBindFontSize = 6.5f;
        float smallLineHeight = 8;
        float spacing = 5;

        float iconSize = smallIconSize + (baseIconSize - smallIconSize) * animatedScale;
        float itemWidth = smallItemWidth + (baseItemWidth - smallItemWidth) * animatedScale;
        float fontSize = smallFontSize + (baseFontSize - smallFontSize) * animatedScale;
        float bindFontSize = smallBindFontSize + (baseBindFontSize - smallBindFontSize) * animatedScale;
        float lineHeight = smallLineHeight + (baseLineHeight - smallLineHeight) * animatedScale;

        float targetTotalWidth = hintCount * itemWidth + (hintCount - 1) * spacing;
        animatedTotalWidth += (targetTotalWidth - animatedTotalWidth) * 0.08f;

        float centerX = animatedX + 70;
        float targetStartX = centerX - animatedTotalWidth / 2;
        if (animatedStartX == -1) {
            animatedStartX = centerX - itemWidth / 2;
        }
        animatedStartX += (targetStartX - animatedStartX) * 0.08f;

        for (int index = 0; index < hintCount; index++) {
            ItemHint hint = activeHints.get(index);
            float hintX = animatedStartX + index * (itemWidth + spacing);
            float textY = drawHintItem(ms, hint, hintX, animatedY, itemWidth, iconSize, fontSize, lineHeight, bindFontSize);
        }

        float totalHeight = iconSize + 4 + lineHeight * 3;
        hintDragging.setWidth(140);
        hintDragging.setHeight(totalHeight);
    }

    private float drawHintItem(MatrixStack ms, ItemHint hint, float hintX, float posY, float itemWidth,
                               float iconSize, float fontSize, float lineHeight, float bindFontSize) {
        float alpha = (float) hint.animation.getOutput();
        float iconX = hintX + (itemWidth - iconSize) / 2;
        float iconY = posY;

        if (hint.itemStack != null && !hint.itemStack.isEmpty()) {
            float itemScale = (iconSize / 16.0f) * alpha;
            float offsetX = (iconSize - iconSize * alpha) / 2;
            float offsetY = (iconSize - iconSize * alpha) / 2;

            RenderSystem.pushMatrix();
            RenderSystem.translatef(iconX + offsetX, iconY + offsetY, 0);
            RenderSystem.scalef(itemScale, itemScale, 1.0f);
            mc.getItemRenderer().renderItemAndEffectIntoGUI(hint.itemStack, 0, 0);
            RenderSystem.popMatrix();
        }

        float textY = iconY + iconSize + 4;
        drawCenteredReason(ms, hint.reason, hintX, itemWidth, textY, fontSize, alpha);
        textY += lineHeight;
        drawCenteredItemName(ms, hint.itemName, hintX, itemWidth, textY, fontSize, alpha, animatedScale > 0.5f);
        textY += lineHeight;
        drawCenteredGradient(ms, "[" + getKeyName(hint.keyBind) + "]", hintX, itemWidth, textY, bindFontSize, alpha);
        return textY;
    }

    private void drawCenteredReason(MatrixStack ms, String reasonText, float hintX, float itemWidth, float y,
                                    float fontSize, float alpha) {
        if (reasonText.contains("Мало ХП")) {
            drawCenteredGradient(ms, reasonText, hintX, itemWidth, y, fontSize, alpha, true);
            return;
        }

        float reasonWidth = Fonts.sfui.getWidth(reasonText, fontSize);
        float reasonX = hintX + (itemWidth - reasonWidth) / 2;
        int reasonColor = ColorUtils.rgba(255, 255, 255, (int) (255 * alpha));
        Fonts.sfui.drawText(ms, reasonText, reasonX, y, reasonColor, fontSize, 0.05f);
    }

    private void drawCenteredItemName(MatrixStack ms, String itemName, float hintX, float itemWidth, float y,
                                      float fontSize, float alpha, boolean showPrefix) {
        if (!showPrefix) {
            drawCenteredGradient(ms, itemName, hintX, itemWidth, y, fontSize, alpha);
            return;
        }

        String prefix = "Заюзай: ";
        float prefixWidth = Fonts.sfui.getWidth(prefix, fontSize);
        ITextComponent gradientText = GradientUtil.gradient(itemName);
        float totalTextWidth = prefixWidth + getGradientWidth(gradientText, fontSize);
        float x = hintX + (itemWidth - totalTextWidth) / 2;

        Fonts.sfui.drawText(ms, prefix, x, y, ColorUtils.rgba(255, 255, 255, (int) (255 * alpha)), fontSize, 0.05f);
        x += prefixWidth;
        drawGradientParts(ms, gradientText, x, y, fontSize, alpha, true);
    }

    private void drawCenteredGradient(MatrixStack ms, String text, float hintX, float itemWidth, float y,
                                      float fontSize, float alpha) {
        drawCenteredGradient(ms, text, hintX, itemWidth, y, fontSize, alpha, false);
    }

    private void drawCenteredGradient(MatrixStack ms, String text, float hintX, float itemWidth, float y,
                                      float fontSize, float alpha, boolean red) {
        ITextComponent gradientText = red ? GradientUtil.redGradient(text) : GradientUtil.gradient(text);
        float width = getGradientWidth(gradientText, fontSize);
        float x = hintX + (itemWidth - width) / 2;
        drawGradientParts(ms, gradientText, x, y, fontSize, alpha, !red);
    }

    private float getGradientWidth(ITextComponent gradientText, float fontSize) {
        float width = 0;
        for (ITextComponent sibling : gradientText.getSiblings()) {
            width += Fonts.sfui.getWidth(sibling.getUnformattedComponentText(), fontSize);
        }
        return width;
    }

    private void drawGradientParts(MatrixStack ms, ITextComponent gradientText, float x, float y,
                                   float fontSize, float alpha, boolean soften) {
        float offset = x;
        for (ITextComponent sibling : gradientText.getSiblings()) {
            String charText = sibling.getUnformattedComponentText();
            Color color = sibling.getStyle().getColor();
            int colorInt = color != null ? color.getColor() : ColorUtils.rgb(255, 255, 255);
            if (soften) {
                colorInt = ColorUtils.interpolateColor(colorInt, ColorUtils.rgb(255, 255, 255), 0.2f);
            }
            colorInt = ColorUtils.reAlphaInt(colorInt, (int) (255 * alpha));
            Fonts.sfui.drawText(ms, charText, offset, y, colorInt, fontSize, 0.05f);
            offset += Fonts.sfui.getWidth(charText, fontSize);
        }
    }

    private String getKeyName(int keyCode) {
        if (keyCode == 0 || keyCode == -1) {
            return "Нет бинда";
        }
        if (keyCode < 0) {
            int mouseButton = keyCode + 100;
            return mouseButton >= 0 ? "MOUSE" + (mouseButton + 1) : "Нет бинда";
        }
        String name = org.lwjgl.glfw.GLFW.glfwGetKeyName(keyCode, 0);
        if (name != null && !name.isEmpty()) {
            return name.toUpperCase();
        }
        String storageName = KeyStorage.getKey(keyCode);
        if (storageName != null && !storageName.isEmpty()) {
            return storageName;
        }
        return "Key " + keyCode;
    }

    private enum ItemGroup {
        COMMON,
        OLD,
        NEW
    }

    private enum ArmorType {
        NONE,
        OTHER,
        DIAMOND,
        NETHERITE
    }

    private static class PlayerTotemData {
        final long triggerTime;

        PlayerTotemData(long triggerTime) {
            this.triggerTime = triggerTime;
        }
    }

    private static class AssistantItem {
        final String displayName;
        final ItemGroup group;
        final Item cooldownItem;
        final String nbtToken;
        final String[] nameTokens;
        Item fallbackItem;
        boolean targetTriggerAllowed = true;

        AssistantItem(String displayName, ItemGroup group, Item cooldownItem, String nbtToken, String... nameTokens) {
            this.displayName = displayName;
            this.group = group;
            this.cooldownItem = cooldownItem;
            this.nbtToken = nbtToken;
            this.nameTokens = nameTokens;
        }

        AssistantItem fallbackItem(Item fallbackItem) {
            this.fallbackItem = fallbackItem;
            return this;
        }

        AssistantItem withoutTargetTrigger() {
            this.targetTriggerAllowed = false;
            return this;
        }
    }

    private static class ItemHint {
        final String itemName;
        final String reason;
        final int slot;
        float displayTime;
        final float maxTime;
        final ItemStack itemStack;
        final int keyBind;
        final Animation animation;
        boolean removing = false;

        ItemHint(String itemName, String reason, int slot, float maxTime, ItemStack itemStack, int keyBind) {
            this.itemName = itemName;
            this.reason = reason;
            this.slot = slot;
            this.displayTime = maxTime;
            this.maxTime = maxTime;
            this.itemStack = itemStack;
            this.keyBind = keyBind;
            this.animation = new EaseInOutQuad(500, 1.0, Direction.FORWARDS);
        }
    }
}
