package xd.harm.utils.figura;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.vector.Vector3d;
import xd.harm.Harmony;
import xd.harm.events.render.EventRender3D;
import xd.harm.modules.models.angrybirds.AngryBirdsBrain;
import xd.harm.modules.models.angrybirds.AngryBirdsModel;
import xd.harm.modules.models.cow.CowBrain;
import xd.harm.modules.models.cow.CowModel;
import xd.harm.modules.models.dog.DogBrain;
import xd.harm.modules.models.dog.DogModel;
import xd.harm.modules.models.dragon.DragonBrain;
import xd.harm.modules.models.dragon.DragonModel;
import xd.harm.modules.models.freddy.FreddyBrain;
import xd.harm.modules.models.freddy.FreddyModel;
import xd.harm.modules.models.minun.MinunBrain;
import xd.harm.modules.models.minun.MinunModel;
import xd.harm.modules.models.penguin.PenguinBrain;
import xd.harm.modules.models.penguin.PenguinModel;
import xd.harm.modules.models.spike.SpikeBrain;
import xd.harm.modules.models.spike.SpikeModel;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/**
 * Питомцы Harmony целиком внутри Figura Cosmetic (вкладка «Петы»).
 *
 * Раньше этим занимался модуль Pet из ClickGUI: он держал «мозги», модели
 * и настройку «Выбор Питомца». Модуль удалён — всё состояние теперь здесь,
 * без ModeSetting, поэтому выбор питомца всегда применяется мгновенно.
 *
 * Рендер вызывается из FiguraWear.onRender (общий EventRender3D),
 * «мозги» регистрируются на шине событий только пока питомец вызван.
 */
public final class FiguraPetController {

    /** Порядок совпадает с индексами старой настройки «Выбор Питомца». */
    private static final String[] NAMES = new String[]{
            "Пёс",
            "Дракон",
            "Коровка",
            "Спайк",
            "Минун",
            "Фредди",
            "Пингвин",
            "Энгри бёрдс",
            "Аксолотль",
            "Черепашка"
    };

    private static final FiguraPetController INSTANCE = new FiguraPetController();

    private final DogModel dogModel = new DogModel(RenderType::getEntityCutoutNoCull);
    private final DragonModel dragonModel = new DragonModel(RenderType::getEntityCutoutNoCull);
    private final CowModel cowModel = new CowModel();
    private final SpikeModel spikeModel = new SpikeModel();
    private final MinunModel minunModel = new MinunModel();
    private final FreddyModel freddyModel = new FreddyModel();
    private final PenguinModel penguinModel = new PenguinModel();
    private final AngryBirdsModel angryBirdsModel = new AngryBirdsModel();

    /** Питомцы из Figura-паков. */
    private final FiguraPetModel axolotlPet = new FiguraPetModel("Axolotl!", 0.7F);
    private final FiguraPetModel turtlePet = new FiguraPetModel("Turtle Pet", 0.75F);

    private final DogBrain dogBrain = new DogBrain();
    private final CowBrain cowBrain = new CowBrain();
    private final DragonBrain dragonBrain = new DragonBrain();
    private final SpikeBrain spikeBrain = new SpikeBrain();
    private final MinunBrain minunBrain = new MinunBrain();
    private final FreddyBrain freddyBrain = new FreddyBrain();
    private final PenguinBrain penguinBrain = new PenguinBrain();
    private final AngryBirdsBrain angryBirdsBrain = new AngryBirdsBrain();

    private volatile int index = 0;
    private volatile boolean active = false;
    private volatile boolean brainsRegistered = false;
    private boolean loaded = false;

    private FiguraPetController() {
    }

    public static FiguraPetController get() {
        INSTANCE.load();
        return INSTANCE;
    }

    // ------------------------------------------------------ Состояние

    public String[] names() {
        String[] copy = new String[NAMES.length];
        System.arraycopy(NAMES, 0, copy, 0, NAMES.length);
        return copy;
    }

    public String current() {
        int i = index;
        return i >= 0 && i < NAMES.length ? NAMES[i] : NAMES[0];
    }

    public int getPetTypeIndex() {
        return index;
    }

    public boolean isActive() {
        return active;
    }

    /** Совместимость со старым API модуля. */
    public boolean isState() {
        return active;
    }

    public void select(String name) {
        if (name == null) {
            return;
        }
        for (int i = 0; i < NAMES.length; i++) {
            if (NAMES[i].equalsIgnoreCase(name.trim())) {
                if (index != i) {
                    index = i;
                    save();
                }
                return;
            }
        }
    }

    public void setActive(boolean state) {
        if (active == state) {
            if (state) {
                registerBrains();
            }
            return;
        }
        active = state;
        if (state) {
            registerBrains();
        } else {
            unregisterBrains();
        }
        save();
    }

    public void toggle() {
        setActive(!active);
    }

    /** Вызвать конкретного питомца одним действием. */
    public void summon(String name) {
        select(name);
        setActive(true);
    }

    public boolean isSummoned(String name) {
        return active && name != null && current().equalsIgnoreCase(name.trim());
    }

    // ------------------------------------------------------ Шина событий

    private void registerBrains() {
        if (brainsRegistered) {
            return;
        }
        try {
            Object bus = Harmony.getInstance().getEventBus();
            if (bus == null) {
                return;
            }
            Harmony.getInstance().getEventBus().register(dogBrain);
            Harmony.getInstance().getEventBus().register(cowBrain);
            Harmony.getInstance().getEventBus().register(dragonBrain);
            Harmony.getInstance().getEventBus().register(spikeBrain);
            Harmony.getInstance().getEventBus().register(minunBrain);
            Harmony.getInstance().getEventBus().register(freddyBrain);
            Harmony.getInstance().getEventBus().register(penguinBrain);
            Harmony.getInstance().getEventBus().register(angryBirdsBrain);
            brainsRegistered = true;
        } catch (Throwable ignored) {
        }
    }

    private void unregisterBrains() {
        if (!brainsRegistered) {
            return;
        }
        try {
            Harmony.getInstance().getEventBus().unregister(dogBrain);
            Harmony.getInstance().getEventBus().unregister(cowBrain);
            Harmony.getInstance().getEventBus().unregister(dragonBrain);
            Harmony.getInstance().getEventBus().unregister(spikeBrain);
            Harmony.getInstance().getEventBus().unregister(minunBrain);
            Harmony.getInstance().getEventBus().unregister(freddyBrain);
            Harmony.getInstance().getEventBus().unregister(penguinBrain);
            Harmony.getInstance().getEventBus().unregister(angryBirdsBrain);
        } catch (Throwable ignored) {
        }
        brainsRegistered = false;
    }

    // ------------------------------------------------------ Рендер

    /** Вызывается из FiguraWear.onRender. */
    public void render(EventRender3D event) {
        if (!active || event == null) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.player == null || mc.world == null) {
            return;
        }
        registerBrains();

        MatrixStack stack = event.getStack();
        double renderPosX = mc.getRenderManager().renderPosX();
        double renderPosY = mc.getRenderManager().renderPosY();
        double renderPosZ = mc.getRenderManager().renderPosZ();
        float patScale = patScale();
        int type = index;

        try {
            if (type == 1) {
                dragonBrain.setEntity(mc.player);
                stack.push();
                stack.translate(dragonBrain.getPos().x - renderPosX, dragonBrain.getPos().y - renderPosY, dragonBrain.getPos().z - renderPosZ);
                if (patScale != 1.0F) {
                    stack.scale(1.0F, patScale, 1.0F);
                }
                dragonModel.setRotationAngles(mc.player.ticksExisted + event.getPartialTicks(), dragonBrain);
                dragonModel.render(stack, event, dragonBrain);
                stack.pop();
                return;
            }

            if (type == 2) {
                cowBrain.setEntity(mc.player);
                stack.push();
                stack.translate(cowBrain.getPos().x - renderPosX, cowBrain.getPos().y - renderPosY, cowBrain.getPos().z - renderPosZ);
                if (patScale != 1.0F) {
                    stack.scale(1.0F, patScale, 1.0F);
                }
                cowModel.render(stack, event, cowBrain);
                stack.pop();
                return;
            }

            if (type == 3) {
                spikeBrain.setEntity(mc.player);
                stack.push();
                stack.translate(spikeBrain.getPos().x - renderPosX, spikeBrain.getPos().y - renderPosY, spikeBrain.getPos().z - renderPosZ);
                if (patScale != 1.0F) {
                    stack.scale(1.0F, patScale, 1.0F);
                }
                spikeModel.render(stack, event, spikeBrain);
                stack.pop();
                return;
            }

            if (type == 4) {
                minunBrain.setEntity(mc.player);
                stack.push();
                stack.translate(minunBrain.getPos().x - renderPosX, minunBrain.getPos().y - renderPosY, minunBrain.getPos().z - renderPosZ);
                if (patScale != 1.0F) {
                    stack.scale(1.0F, patScale, 1.0F);
                }
                minunModel.render(stack, event, minunBrain);
                stack.pop();
                return;
            }

            if (type == 5) {
                freddyBrain.setEntity(mc.player);
                stack.push();
                stack.translate(freddyBrain.getPos().x - renderPosX, freddyBrain.getPos().y - renderPosY, freddyBrain.getPos().z - renderPosZ);
                if (patScale != 1.0F) {
                    stack.scale(1.0F, patScale, 1.0F);
                }
                freddyModel.render(stack, event, freddyBrain);
                stack.pop();
                return;
            }

            if (type == 6) {
                penguinBrain.setEntity(mc.player);
                stack.push();
                stack.translate(penguinBrain.getPos().x - renderPosX, penguinBrain.getPos().y - renderPosY, penguinBrain.getPos().z - renderPosZ);
                if (patScale != 1.0F) {
                    stack.scale(1.0F, patScale, 1.0F);
                }
                penguinModel.render(stack, event, penguinBrain);
                stack.pop();
                return;
            }

            if (type == 7) {
                angryBirdsBrain.setEntity(mc.player);
                stack.push();
                stack.translate(angryBirdsBrain.getPos().x - renderPosX, angryBirdsBrain.getPos().y - renderPosY, angryBirdsBrain.getPos().z - renderPosZ);
                if (patScale != 1.0F) {
                    stack.scale(1.0F, patScale, 1.0F);
                }
                angryBirdsModel.render(stack, event, angryBirdsBrain);
                stack.pop();
                return;
            }

            if (type == 8) {
                // Аксолотль летает рядом с игроком — берём летающий мозг.
                dragonBrain.setEntity(mc.player);
                stack.push();
                stack.translate(dragonBrain.getPos().x - renderPosX, dragonBrain.getPos().y - renderPosY, dragonBrain.getPos().z - renderPosZ);
                if (patScale != 1.0F) {
                    stack.scale(1.0F, patScale, 1.0F);
                }
                axolotlPet.render(stack, dragonBrain.getBody(), 1.0F, 0.0F, 0.0F);
                stack.pop();
                return;
            }

            if (type == 9) {
                // Черепашка ходит по земле — мозг как у пса.
                dogBrain.setEntity(mc.player);
                stack.push();
                stack.translate(dogBrain.getPos().x - renderPosX, dogBrain.getPos().y - renderPosY, dogBrain.getPos().z - renderPosZ);
                if (patScale != 1.0F) {
                    stack.scale(1.0F, patScale, 1.0F);
                }
                turtlePet.render(stack, dogBrain.getBody(), 1.0F, dogBrain.limbSwing, dogBrain.limbSwingAmount);
                stack.pop();
                return;
            }

            dogBrain.setEntity(mc.player);
            stack.push();
            stack.translate(dogBrain.getPos().x - renderPosX, dogBrain.getPos().y - renderPosY, dogBrain.getPos().z - renderPosZ);
            if (patScale != 1.0F) {
                stack.scale(1.0F, patScale, 1.0F);
            }
            dogModel.setRotationAngles(mc.player.ticksExisted, dogBrain);
            dogModel.render(stack, event, dogBrain);
            stack.pop();
        } catch (Throwable ignored) {
        }
    }

    private float patScale() {
        try {
            return CosmeticPat.get().getPetScale(index, getPatHeight());
        } catch (Throwable ignored) {
        }
        return 1.0F;
    }

    // ------------------------------------------------------ Гладим питомца

    public Vector3d getPatPosition() {
        Minecraft mc = Minecraft.getInstance();
        if (!active || mc == null || mc.player == null) {
            return null;
        }
        try {
            switch (index) {
                case 1:
                case 8:
                    dragonBrain.setEntity(mc.player);
                    return dragonBrain.getPos();
                case 2:
                    cowBrain.setEntity(mc.player);
                    return cowBrain.getPos();
                case 3:
                    spikeBrain.setEntity(mc.player);
                    return spikeBrain.getPos();
                case 4:
                    minunBrain.setEntity(mc.player);
                    return minunBrain.getPos();
                case 5:
                    freddyBrain.setEntity(mc.player);
                    return freddyBrain.getPos();
                case 6:
                    penguinBrain.setEntity(mc.player);
                    return penguinBrain.getPos();
                case 7:
                    angryBirdsBrain.setEntity(mc.player);
                    return angryBirdsBrain.getPos();
                default:
                    dogBrain.setEntity(mc.player);
                    return dogBrain.getPos();
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    public float getPatHeight() {
        switch (index) {
            case 1:
                return 1.35F;
            case 2:
                return 1.85F;
            case 3:
                return 1.45F;
            case 4:
                return 1.45F;
            case 5:
                return 1.55F;
            case 6:
                return 1.2F;
            case 7:
                return 1.15F;
            case 8:
                return 0.7F;
            case 9:
                return 0.75F;
            default:
                return 0.85F;
        }
    }

    public boolean canPat(Vector3d start, Vector3d end) {
        Vector3d pos = getPatPosition();
        if (pos == null) {
            return false;
        }
        float radius;
        switch (index) {
            case 1:
                radius = 1.2F;
                break;
            case 2:
                radius = 1.0F;
                break;
            case 3:
                radius = 1.15F;
                break;
            case 4:
                radius = 0.85F;
                break;
            case 5:
            case 7:
                radius = 0.75F;
                break;
            case 6:
            case 8:
            case 9:
                radius = 0.7F;
                break;
            default:
                radius = 0.8F;
                break;
        }
        float height = getPatHeight();
        AxisAlignedBB box = new AxisAlignedBB(pos.x - radius, pos.y, pos.z - radius, pos.x + radius, pos.y + height, pos.z + radius);
        return box.rayTrace(start, end).isPresent();
    }

    // ------------------------------------------------------ Сохранение

    private static File gameDir() {
        Minecraft mc = Minecraft.getInstance();
        return mc != null && mc.gameDir != null ? mc.gameDir : new File(".");
    }

    private static File saveFile() {
        return new File(new File(gameDir(), "figura"), "harmony_pet.txt");
    }

    private synchronized void load() {
        if (loaded) {
            return;
        }
        loaded = true;
        try {
            File file = saveFile();
            if (!file.isFile()) {
                return;
            }
            String raw = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8).trim();
            if (raw.isEmpty()) {
                return;
            }
            String[] parts = raw.split("\\|", 2);
            boolean state = "1".equals(parts[0].trim());
            if (parts.length > 1) {
                String name = parts[1].trim();
                for (int i = 0; i < NAMES.length; i++) {
                    if (NAMES[i].equalsIgnoreCase(name)) {
                        index = i;
                        break;
                    }
                }
            }
            if (state) {
                active = true;
                registerBrains();
            }
        } catch (Throwable ignored) {
        }
    }

    private void save() {
        try {
            File file = saveFile();
            File parent = file.getParentFile();
            if (parent != null) {
                parent.mkdirs();
            }
            String data = (active ? "1" : "0") + "|" + current();
            Files.write(file.toPath(), data.getBytes(StandardCharsets.UTF_8));
        } catch (Throwable ignored) {
        }
    }
}
