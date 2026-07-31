package xd.harm.modules.impl.render;

import com.mojang.blaze3d.matrix.MatrixStack;
import xd.harm.Harmony;
import xd.harm.modules.api.Category;
import xd.harm.modules.api.Module;
import xd.harm.modules.api.ModuleRegister;
import xd.harm.modules.impl.combat.HitAura;
import xd.harm.modules.settings.impl.BooleanSetting;
import xd.harm.modules.settings.impl.ModeSetting;
import xd.harm.modules.settings.impl.SliderSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.item.SwordItem;
import net.minecraft.util.HandSide;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Quaternion;
import net.minecraft.util.math.vector.Vector3f;

@ModuleRegister(name = "SwingAnimation", category = Category.Render, desc = "Анимация на руки")
public class SwingAnimation extends Module {

    private static final float PI = (float) Math.PI;
    private static final float TWO_PI = PI * 2.0f;
    private static final Quaternion ROTATE_Y_90 = Vector3f.YP.rotationDegrees(90.0f);
    private static final Quaternion ROTATE_Y_45 = Vector3f.YP.rotationDegrees(45.0f);
    private static final Quaternion ROTATE_Y_20 = Vector3f.YP.rotationDegrees(20.0f);
    private static final Quaternion ROTATE_Y_NEG_45 = Vector3f.YP.rotationDegrees(-45.0f);
    private static final Quaternion ROTATE_Z_NEG_60 = Vector3f.ZP.rotationDegrees(-60.0f);
    private static final Quaternion ROTATE_Z_90 = Vector3f.ZP.rotationDegrees(90.0f);
    private static final Quaternion ROTATE_X_NEG_80 = Vector3f.XP.rotationDegrees(-80.0f);
    private static final Quaternion ROTATE_X_NEG_120 = Vector3f.XP.rotationDegrees(-120.0f);

    // --- Главная настройка: выбор режима анимации ---
    public ModeSetting animation = (ModeSetting) new ModeSetting("Анимация", "Свинг", "Свинг", "Блок", "Предмет").setSticky(true);

    // --- Отдельные типы анимации для каждого режима ---
    public ModeSetting swingType = new ModeSetting("Свинг тип", "Тип 1",
            "Тип 1", "Тип 2", "Тип 3", "Тип 4",
            "Тип 5", "Тип 6", "Тип 7", "Тип 8", "Тип 9",
            "Тип 10", "Тип 11", "Тип 12", "Тип 13",
            "Тип 14", "Тип 15"
    ).setVisible(() -> animation.is("Свинг"));

    public ModeSetting blockType = new ModeSetting("Блок тип", "Тип 1",
            "Тип 1", "Тип 2", "Тип 3", "Тип 4",
            "Тип 5", "Тип 6", "Тип 7", "Тип 8", "Тип 9",
            "Тип 10", "Тип 11", "Тип 12", "Тип 13",
            "Тип 14", "Тип 15"
    ).setVisible(() -> animation.is("Блок"));

    public ModeSetting itemType = new ModeSetting("Предмет тип", "Тип 1",
            "Тип 1", "Тип 2", "Тип 3", "Тип 4",
            "Тип 5", "Тип 6", "Тип 7", "Тип 8", "Тип 9",
            "Тип 10", "Тип 11", "Тип 12", "Тип 13",
            "Тип 14", "Тип 15"
    ).setVisible(() -> animation.is("Предмет"));

    // --- Кулдаун ---
    public BooleanSetting oldCooldownAnimation = new BooleanSetting("Старая анимация кулдауна", true);

    // --- Настройки для каждого режима (Свинг) ---
    public SliderSetting swingScale = new SliderSetting("Масштаб руки", 1.0f, 0.5f, 1.5f, 0.01f).setVisible(() -> animation.is("Свинг"));
    public SliderSetting swingPower = new SliderSetting("Сила взмаха", 70.0f, 10.0f, 200.0f, 5.0f).setVisible(() -> animation.is("Свинг"));
    public SliderSetting swingSpeed = new SliderSetting("Скорость взмаха", 6.0f, 1.0f, 30.0f, 1.0f).setVisible(() -> animation.is("Свинг"));
    public SliderSetting swingX = new SliderSetting("Сдвиг X", 0.0f, -2.0f, 2.0f, 0.001f).setVisible(() -> animation.is("Свинг"));
    public SliderSetting swingY = new SliderSetting("Сдвиг Y", 0.0f, -2.0f, 2.0f, 0.001f).setVisible(() -> animation.is("Свинг"));
    public SliderSetting swingZ = new SliderSetting("Сдвиг Z", 0.0f, -2.0f, 2.0f, 0.001f).setVisible(() -> animation.is("Свинг"));

    // --- Настройки для каждого режима (Блок) ---
    public SliderSetting blockScale = new SliderSetting("Масштаб руки", 1.0f, 0.5f, 1.5f, 0.01f).setVisible(() -> animation.is("Блок"));
    public SliderSetting blockPower = new SliderSetting("Сила взмаха", 70.0f, 10.0f, 200.0f, 5.0f).setVisible(() -> animation.is("Блок"));
    public SliderSetting blockSpeed = new SliderSetting("Скорость взмаха", 6.0f, 1.0f, 30.0f, 1.0f).setVisible(() -> animation.is("Блок"));
    public SliderSetting blockX = new SliderSetting("Сдвиг X", 0.0f, -2.0f, 2.0f, 0.001f).setVisible(() -> animation.is("Блок"));
    public SliderSetting blockY = new SliderSetting("Сдвиг Y", 0.0f, -2.0f, 2.0f, 0.001f).setVisible(() -> animation.is("Блок"));
    public SliderSetting blockZ = new SliderSetting("Сдвиг Z", 0.0f, -2.0f, 2.0f, 0.001f).setVisible(() -> animation.is("Блок"));

    // --- Настройки для каждого режима (Предмет) ---
    public SliderSetting itemScale = new SliderSetting("Масштаб руки", 1.0f, 0.5f, 1.5f, 0.01f).setVisible(() -> animation.is("Предмет"));
    public SliderSetting itemPower = new SliderSetting("Сила взмаха", 70.0f, 10.0f, 200.0f, 5.0f).setVisible(() -> animation.is("Предмет"));
    public SliderSetting itemSpeed = new SliderSetting("Скорость взмаха", 6.0f, 1.0f, 30.0f, 1.0f).setVisible(() -> animation.is("Предмет"));
    public SliderSetting itemX = new SliderSetting("Сдвиг X", 0.0f, -2.0f, 2.0f, 0.001f).setVisible(() -> animation.is("Предмет"));
    public SliderSetting itemY = new SliderSetting("Сдвиг Y", 0.0f, -2.0f, 2.0f, 0.001f).setVisible(() -> animation.is("Предмет"));
    public SliderSetting itemZ = new SliderSetting("Сдвиг Z", 0.0f, -2.0f, 2.0f, 0.001f).setVisible(() -> animation.is("Предмет"));

    // --- Вращение ---
    public BooleanSetting spin = new BooleanSetting("Вращение", false);
    public ModeSetting spinMode = new ModeSetting("Режим вращения", "Горизонтально", "Горизонтально", "Вертикально", "Приближение").setVisible(() -> spin.get());
    public ModeSetting spinHand = new ModeSetting("Рука вращения", "Все", "Все", "Левая", "Правая").setVisible(() -> spin.get());
    public SliderSetting spinSpeed = new SliderSetting("Скорость вращения", 8.0f, 1.0f, 15.0f, 1.0f).setVisible(() -> spin.get());
    public BooleanSetting stopOnHit = new BooleanSetting("Стоп при ударе", true).setVisible(() -> spin.get());

    // --- Блокирование щитом ---
    public final BooleanSetting blockAnimation = new BooleanSetting("Block Animation", false);
    public final BooleanSetting removeShield = new BooleanSetting("Remove Shield", false).setVisible(() -> blockAnimation.get());

    private HitAura cachedHitAura;

    public SwingAnimation() {
        swingType.setConfigKey("swing_anim_type");
        blockType.setConfigKey("block_anim_type");
        itemType.setConfigKey("item_anim_type");
        addSettings(
                animation, swingType, blockType, itemType, oldCooldownAnimation,
                // Свинг
                swingScale, swingPower, swingSpeed, swingX, swingY, swingZ,
                // Блок
                blockScale, blockPower, blockSpeed, blockX, blockY, blockZ,
                // Предмет
                itemScale, itemPower, itemSpeed, itemX, itemY, itemZ,
                // Вращение (общее)
                spin, spinMode, spinHand, spinSpeed, stopOnHit,
                // Блокирование щитом
                blockAnimation, removeShield
        );
    }

    // ========================
    //  Логика выбора режима
    // ========================

    public String getActiveMode() {
        if (mc.player == null) return swingType.get();

        // Блокирование щитом
        if (blockAnimation.get() && mc.player.isHandActive() && mc.player.getActiveItemStack().getUseAction() == net.minecraft.item.UseAction.BLOCK) {
            return blockType.get();
        }

        // Если в руке меч
        if (mc.player.getHeldItemMainhand().getItem() instanceof SwordItem) {
            // HitAura активна с целью → Блок анимация
            HitAura hitAura = cachedHitAura;
            if (hitAura == null) {
                hitAura = Harmony.getInstance().getModuleManager().getHitAura();
                cachedHitAura = hitAura;
            }
            if (hitAura != null && hitAura.isState() && HitAura.getTarget() != null) {
                return blockType.get();
            }
            // Без цели → Свинг анимация
            return swingType.get();
        }

        // Не меч → Предмет анимация
        return itemType.get();
    }

    // ========================
    //  Выбор настроек по текущему активному режиму
    //  Возвращает настройку (Масштаб/Сила/Скорость/Сдвиг) для того режима,
    //  который сейчас реально применяется (Свинг / Блок / Предмет).
    // ========================

    /** Какая категория анимации сейчас активна: "Свинг", "Блок" или "Предмет". */
    public String getActiveCategory() {
        if (mc.player == null) return "Свинг";
        if (mc.player.getHeldItemMainhand().getItem() instanceof SwordItem) {
            HitAura hitAura = cachedHitAura;
            if (hitAura == null) {
                hitAura = Harmony.getInstance().getModuleManager().getHitAura();
                cachedHitAura = hitAura;
            }
            if (hitAura != null && hitAura.isState() && HitAura.getTarget() != null) {
                return "Блок";
            }
            return "Свинг";
        }
        return "Предмет";
    }

    public SliderSetting getScale() {
        return switch (getActiveCategory()) {
            case "Блок" -> blockScale;
            case "Предмет" -> itemScale;
            default -> swingScale;
        };
    }

    public SliderSetting getPower() {
        return switch (getActiveCategory()) {
            case "Блок" -> blockPower;
            case "Предмет" -> itemPower;
            default -> swingPower;
        };
    }

    public SliderSetting getSpeed() {
        return switch (getActiveCategory()) {
            case "Блок" -> blockSpeed;
            case "Предмет" -> itemSpeed;
            default -> swingSpeed;
        };
    }

    public SliderSetting getOffsetX() {
        return switch (getActiveCategory()) {
            case "Блок" -> blockX;
            case "Предмет" -> itemX;
            default -> swingX;
        };
    }

    public SliderSetting getOffsetY() {
        return switch (getActiveCategory()) {
            case "Блок" -> blockY;
            case "Предмет" -> itemY;
            default -> swingY;
        };
    }

    public SliderSetting getOffsetZ() {
        return switch (getActiveCategory()) {
            case "Блок" -> blockZ;
            case "Предмет" -> itemZ;
            default -> swingZ;
        };
    }

    public boolean shouldAnimate() {
        if (!isState()) return false;
        if (mc.player == null) return false;
        return true;
    }

    // ========================
    //  Главная анимация
    // ========================

    public void generateSwing(MatrixStack stack, int side, float swingProgress, float equipProgress) {
        if (mc.player == null) return;

        // Опускание предмета по кулдауну (ванильное поведение)
        stack.translate(0.0f, equipProgress * -0.6f, 0.0f);

        // powerMul: Сила типа / 70 = 1.0 при значении по умолчанию
        float powerMul = getPower().getFloat() / 70.0f;
        float convertedProgress = MathHelper.sin(MathHelper.sqrt(swingProgress) * PI);
        String mode = getActiveMode();



        if (mode.equals("Тип 1")) {
            float anim = (float) Math.sin((double) swingProgress * Math.PI / 2.0 * 2.0);
            stack.translate((double) (side * 0.56f) + getOffsetX().getFloat(), -0.52f + getOffsetY().getFloat(), -0.72f + getOffsetZ().getFloat());
            stack.rotate(ROTATE_Y_90);
            stack.rotate(ROTATE_Z_NEG_60);
            stack.rotate(Vector3f.XP.rotationDegrees(-80.0f - 40.0f * anim * powerMul));
            float sc = getScale().getFloat();
            stack.scale(sc, sc, sc);
        } else if (mode.equals("Тип 2")) {
            float anim = (float) Math.sin((double) swingProgress * Math.PI / 2.0 * 2.0);
            stack.translate((double) (side * 0.56f) + getOffsetX().getFloat(), -0.52f + getOffsetY().getFloat(), -0.72f + getOffsetZ().getFloat());
            stack.translate(0.2, 0.2, (double) (-0.15f * anim - 0.15f));
            stack.rotate(ROTATE_X_NEG_120);
            stack.rotate(Vector3f.YP.rotationDegrees(-anim * 90.0f * powerMul + 30.0f));
            stack.rotate(ROTATE_Z_90);
            float sc = getScale().getFloat();
            stack.scale(sc, sc, sc);
        } else if (mode.equals("Тип 3")) {
            float f3 = MathHelper.sqrt(swingProgress);
            float sinF3 = MathHelper.sin(f3 * PI);
            float sideValue = (float) side;
            stack.translate((double) (side * 0.56f) + getOffsetX().getFloat(), -0.52f + getOffsetY().getFloat(), -0.72f + getOffsetZ().getFloat());
            stack.rotate(Vector3f.YP.rotationDegrees(sideValue * (45.0f + MathHelper.sin(swingProgress * swingProgress * PI) / 4.0f * -120.0f)));
            stack.rotate(Vector3f.ZP.rotationDegrees(sideValue * sinF3 * -20.0f));
            stack.rotate(Vector3f.XP.rotationDegrees(sinF3 * -80.0f));
            stack.rotate(side == 1 ? ROTATE_Y_NEG_45 : side == -1 ? ROTATE_Y_45 : Vector3f.YP.rotationDegrees(sideValue * -45.0f));
            float sc = getScale().getFloat();
            stack.scale(sc, sc, sc);

            // ========================
            //  Тип 4 = 1.7
            // ========================
        } else if (mode.equals("Тип 4")) {
            transformBlockPosition(stack, equipProgress);
            applySwingTransformation(stack, swingProgress, convertedProgress, powerMul);
            applyBlockTransformation(stack);
            applyBlockScale(stack);

            // ========================
            //  Тип 5 = 1.8
            // ========================
        } else if (mode.equals("Тип 5")) {
            transformBlockPosition(stack, equipProgress);
            applyBlockTransformation(stack);
            applyBlockScale(stack);

            // ========================
            //  Тип 6 = Rub
            // ========================
        } else if (mode.equals("Тип 6")) {
            transformBlockPosition(stack, equipProgress);
            applyBlockTransformation(stack);
            stack.rotate(Vector3f.YP.rotationDegrees(convertedProgress * -30.0f * powerMul));
            stack.rotate(Vector3f.ZP.rotationDegrees(convertedProgress * -30.0f * powerMul));
            applyBlockScale(stack);

            // ========================
            //  Тип 7 = Bounce
            // ========================
        } else if (mode.equals("Тип 7")) {
            transformBlockPosition(stack, equipProgress);
            applyBlockTransformation(stack);
            stack.rotate(Vector3f.YP.rotationDegrees(convertedProgress * 42.0f * powerMul));
            stack.rotate(Vector3f.ZP.rotationDegrees(-convertedProgress * 22.0f * powerMul));
            applyBlockScale(stack);

            // ========================
            //  Тип 8 = Diagonal
            // ========================
        } else if (mode.equals("Тип 8")) {
            transformBlockPosition(stack, equipProgress);
            applyBlockTransformation(stack);
            stack.rotate(Vector3f.XP.rotationDegrees(5.0f - (convertedProgress * 32.0f * powerMul)));
            applyBlockScale(stack);

            // ========================
            //  Тип 9 = Gothaj
            // ========================
        } else if (mode.equals("Тип 9")) {
            transformFirstPersonItem(stack, equipProgress / 2.0f, 0.0f, powerMul);
            stack.rotate(new Quaternion(new Vector3f(1.0f, 0.0f, 2.0f), -convertedProgress * 30.0f * powerMul, true));
            stack.rotate(new Quaternion(new Vector3f(1.5f, convertedProgress / 1.2f, 0.0f), -convertedProgress * 44.0f * powerMul, true));
            applyCustomTransform(stack);
            applyBlockScale(stack);

            // ========================
            //  Тип 10 = Swong
            // ========================
        } else if (mode.equals("Тип 10")) {
            transformFirstPersonItem(stack, equipProgress / 2.0f, swingProgress, powerMul);
            stack.rotate(new Quaternion(new Vector3f(-convertedProgress, 0.0f, 9.0f), convertedProgress * 15.0f * powerMul, true));
            stack.rotate(new Quaternion(new Vector3f(1.0f, -convertedProgress / 2.0f, 0.0f), convertedProgress * 40.0f * powerMul, true));
            applyCustomTransform(stack);
            applyBlockScale(stack);

            // ========================
            //  Тип 11 = Exhibition
            // ========================
        } else if (mode.equals("Тип 11")) {
            transformFirstPersonItem(stack, equipProgress / 2.0f, 0.0f, powerMul);
            stack.rotate(new Quaternion(new Vector3f(1.0f, 0.0f, 2.0f), -convertedProgress * 31.0f * powerMul, true));
            stack.rotate(new Quaternion(new Vector3f(1.5f, convertedProgress / 1.1f, 0.0f), -convertedProgress * 33.0f * powerMul, true));
            applyCustomTransform(stack);
            applyBlockScale(stack);

            // ========================
            //  Тип 12 = NeverHook
            // ========================
        } else if (mode.equals("Тип 12")) {
            transformFirstPersonItem(stack, equipProgress / 3.0f, swingProgress, powerMul);
            applyCustomTransform(stack);
            applyBlockScale(stack);

            // ========================
            //  Тип 13 = Mod 1
            // ========================
        } else if (mode.equals("Тип 13")) {
            float anim = (float) Math.sin(swingProgress * (Math.PI / 2) * 2);
            float sc = getScale().getFloat();
            stack.translate(0.56f + getOffsetX().getFloat(), -0.52f + getOffsetY().getFloat(), -0.72f + getOffsetZ().getFloat());
            stack.rotate(Vector3f.YP.rotationDegrees(90.0f));
            stack.rotate(Vector3f.ZP.rotationDegrees(-60.0f));
            stack.rotate(Vector3f.XP.rotationDegrees(-90.0f - 50.0f * powerMul * anim));
            stack.scale(sc, sc, sc);

            // ========================
            //  Тип 14 = Mod 2
            // ========================
        } else if (mode.equals("Тип 14")) {
            float anim = (float) Math.sin(swingProgress * (Math.PI / 2) * 2);
            float sc = getScale().getFloat();
            stack.translate(0.56f + getOffsetX().getFloat(), -0.52f + getOffsetY().getFloat(), -0.72f + getOffsetZ().getFloat());
            stack.rotate(Vector3f.YP.rotationDegrees(15.0f * anim * powerMul));
            stack.rotate(Vector3f.ZP.rotationDegrees(-60.0f * anim * powerMul));
            stack.rotate(Vector3f.XP.rotationDegrees((-45.0f - 50.0f * powerMul) * anim));
            stack.scale(sc, sc, sc);

            // ========================
            //  Тип 15 = Mod 6 (чистая ваниль)
            //  Полностью повторяет поведение Mod 6 из AndroQuantum:
            //  анимация НЕ перехватывает рендер, FirstPersonRenderer идёт по
            //  ванильному пути (см. isVanillaType15() + FirstPersonRenderer).
            //  Здесь -- пустой заглушка: generateSwing не должен вызываться.
            // ========================
        } else if (mode.equals("Тип 15")) {
            // intentionally empty: vanilla path handles rendering.
            // Блок оставлен, чтобы getActiveMode() корректно распознавал Тип 15
            // и чтобы не сломать цепочку else-if для Тип 16 (ниже при наличии).
        }
    }

    // ========================
    //  Mod 6 (Тип 15) -- ванильный путь
    //  AndroQuantum: при "Mod 6" миксин ItemInHandRendererMixin НЕ перехватывает
    //  рендер руки, и срабатывает чистая ванильная анимация Minecraft.
    //  Чтобы повторить это 1:1 в Harmony, FirstPersonRenderer должен уходить
    //  на свой else-блок (transformSideFirstPerson + transformFirstPerson),
    //  а не вызывать generateSwing().
    // ========================
    public boolean isVanillaType15() {
        return "Тип 15".equals(getActiveMode());
    }

    // ========================
    //  Вспомогательные методы
    // ========================

    private void transformBlockPosition(MatrixStack stack, float equipProgress) {
        stack.translate(getOffsetX().getFloat(), getOffsetY().getFloat(), 0.0f);
        stack.translate(0.56f, -0.52f + equipProgress * -0.6f, -0.72f);
    }

    private void applyBlockTransformation(MatrixStack stack) {
        stack.rotate(Vector3f.YP.rotationDegrees(-18.0f));
        stack.rotate(Vector3f.ZP.rotationDegrees(82.0f));
        stack.rotate(Vector3f.YP.rotationDegrees(112.0f));
    }

    private void applySwingTransformation(MatrixStack stack, float swingProgress, float convertedProgress, float powerMul) {
        float f = MathHelper.sin(swingProgress * swingProgress * PI);
        stack.rotate(Vector3f.YP.rotationDegrees(45.0f + f * -20.0f * powerMul));
        stack.rotate(Vector3f.ZP.rotationDegrees(convertedProgress * -20.0f * powerMul));
        stack.rotate(Vector3f.XP.rotationDegrees(convertedProgress * -80.0f * powerMul));
        stack.rotate(Vector3f.YP.rotationDegrees(-45.0f));
    }

    private void transformFirstPersonItem(MatrixStack stack, float equipProgress, float swingProgress, float powerMul) {
        stack.translate(getOffsetX().getFloat(), getOffsetY().getFloat(), 0.0f);
        stack.translate(0.56f, -0.52f, -0.72f);
        stack.translate(0.0f, equipProgress * -0.6f, 0.0f);
        stack.rotate(Vector3f.YP.rotationDegrees(45.0f));
        float f2 = MathHelper.sin(MathHelper.sqrt(swingProgress) * PI);
        stack.rotate(Vector3f.ZP.rotationDegrees(f2 * -20.0f * powerMul));
        if (f2 != 0) {
            stack.rotate(new Quaternion(new Vector3f(0.01f, 0.0f, 0.0f), f2 * -80.0f * powerMul, true));
        }
        stack.translate(0.4f, 0.2f, 0.2f);
    }

    private void applyCustomTransform(MatrixStack stack) {
        stack.rotate(new Quaternion(new Vector3f(0.0f, 1.0f, 0.0f), 20.0f, true));
        stack.rotate(new Quaternion(new Vector3f(1.0f, 0.0f, 0.0f), -80.0f, true));
        stack.rotate(new Quaternion(new Vector3f(0.0f, 1.0f, 0.0f), 20.0f, true));
    }

    private void applyBlockScale(MatrixStack stack) {
        float scaleVal = getScale().getFloat();
        stack.scale(scaleVal, scaleVal, scaleVal);
    }

    // ========================
    //  Вращение
    // ========================

    public void processSpinRotation(MatrixStack stack, HandSide hand) {
        if (!isState() || !spin.getBool()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && mc.player.isSwingInProgress && stopOnHit.getBool()) return;
        if (spinHand.is("Левая") && hand != HandSide.LEFT) return;
        if (spinHand.is("Правая") && hand != HandSide.RIGHT) return;

        int speed = Math.max(1, (int) spinSpeed.getFloat());
        long currentTime = System.currentTimeMillis();
        float angle = (float) (currentTime / speed % 360);

        if (spinMode.is("Горизонтально")) {
            stack.rotate(Vector3f.YP.rotationDegrees(angle));
        } else if (spinMode.is("Вертикально")) {
            stack.rotate(Vector3f.XN.rotationDegrees(angle));
        } else if (spinMode.is("Приближение")) {
            float zoom = 1.0f + MathHelper.sin((currentTime % 1000L) / 1000.0f * TWO_PI) * 0.25f;
            stack.scale(zoom, zoom, zoom);
        }
    }
}
