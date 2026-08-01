package xd.harm.utils.figura;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;

import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Сканер библиотеки Figura-аватаров в &lt;gameDir&gt;/figura/avatars.
 *
 * v4:
 *  1. Битые / дублирующие паки из BLOCKED в список не попадают.
 *  2. Варианты одного персонажа (Shizuko / Shizuko Swimsuit,
 *     Winter Coat / Winter Coat Slim Body и т.д.) склеиваются в одну карточку:
 *     голова группы + список variants. Выбор живёт в FiguraPackSettings.
 *  3. Служебные модели (gun, shield, armor, bubble, ex_skill…) больше не
 *     навешиваются на аватар, даже если автор забыл visible=false.
 */
public final class FiguraAvatarLibrary {

    // ------------------------------------------------------------- Секции

    public enum Section {
        ALL("Все", 0),
        AVATARS("Аватары", 1),
        PETS("Петы", 2),
        ACCESSORIES("Аксессуары", 3),
        WEAPONS("Оружие", 4),
        WINGS("Крылья", 5),
        EMOTES("Забавы", 6),
        SCRIPTS("Скрипты", 7);

        public final String title;
        public final int order;

        Section(String title, int order) {
            this.title = title;
            this.order = order;
        }
    }

    /** Паки, которые не показываем вообще (битые или заменённые другими). */
    private static final Set<String> BLOCKED = new HashSet<String>(Arrays.asList(
            "shizuko"   // старый битый пак; его заменяет 01a_Shizuko из Blue Archive
    ));

    /** Модели-спутники из Figura-паков: рисуются только скриптами, нам не нужны. */
    private static final Set<String> EXTRA_MODELS = new HashSet<String>(Arrays.asList(
            "gun", "shield", "armor", "bubble", "bullet", "sword", "weapon",
            "death_animation", "action_wheel_gui", "action_wheel", "ex_skill", "ex_skill_1",
            "ex_skill_2", "ex_skill_3", "ex_skill_frame", "skill", "skill_frame",
            "ui", "hud", "gui", "icon", "portrait", "logo", "nameplate", "effects"
    ));

    private static final Pattern NUMBERED = Pattern.compile("^(\\d{1,4})[a-zA-Z]?[_\\-\\s]");

    /** Слова, которые обозначают вариант одного и того же персонажа. */
    private static final Set<String> VARIANT_WORDS = new HashSet<String>(Arrays.asList(
            "slim", "body", "swimsuit", "maid", "tracksuit", "idol", "riding", "battle",
            "christmas", "dress", "magical", "crimson", "summer", "winter", "alt",
            "alternative", "variant", "version", "pack", "full", "extra", "bends",
            "edition", "remake", "new", "old", "v1", "v2", "v3"
    ));

    // -------------------------------------------------------------- Запись

    public static final class Entry {

        /** Имя папки в figura/avatars, например 01a_Shizuko. */
        public final String folder;
        /** Красивое имя для GUI. */
        public final String name;
        public final String description;
        public final String author;
        /** Цвет из avatar.json (0xRRGGBB) или -1. */
        public final int color;
        public final Section section;
        /** Абсолютный путь к папке пака. */
        public final Path path;
        /** avatar.png или null. */
        public final Path preview;
        /** Все .bbmodel внутри пака, в порядке имён. */
        public final List<Path> models;
        /** Основная модель (main.bbmodel или самая крупная) или null. */
        public final Path primaryModel;
        /** Ключи customizations со значением visible=false. */
        public final Set<String> hiddenPaths;
        /** Полные имена моделей (без .bbmodel), у которых скрыт корень. */
        public final Set<String> hiddenModels;
        /** customizations moveTo: путь модели -> куда пересаживается. */
        public final Map<String, String> moveTo;
        /** Есть ли у пака Lua-скрипты (в Harmony они не исполняются). */
        public final boolean hasScripts;

        /** Все виды этого пака, включая самого себя. Заполняется только у головы группы. */
        public final List<Entry> variants = new ArrayList<Entry>();
        /** Название вида: «Обычная», «Swimsuit», «Slim Body»… */
        public String variantLabel = "Обычная";
        /** Фолдер головы группы (у одиночных паков — он сам). */
        public String headFolder;
        /** true — это копия пета для раздела «Аксессуары»: носится на голове. */
        public boolean headMount = false;
        /** true — это карточка питомца из модуля Pet, а не Figura-пак. */
        public boolean moduleCard = false;
        /** Имя питомца в модуле Pet (для moduleCard). */
        public String petName = null;
        /** Имя модуля Harmony для карточек-функций (Katana, ChinaHat, Raincoat, PatPatPat). */
        public String moduleName = null;
        /** Подсказка для рисованной иконки: hat, coat, sword, paw, hand и т.д. */
        public String iconKind = null;

        Entry(String folder, String name, String description, String author, int color,
              Section section, Path path, Path preview, List<Path> models, Path primaryModel,
              Set<String> hiddenPaths, Set<String> hiddenModels, Map<String, String> moveTo,
              boolean hasScripts) {
            this.folder = folder;
            this.name = name;
            this.description = description;
            this.author = author;
            this.color = color;
            this.section = section;
            this.path = path;
            this.preview = preview;
            this.models = models;
            this.primaryModel = primaryModel;
            this.hiddenPaths = hiddenPaths;
            this.hiddenModels = hiddenModels;
            this.moveTo = moveTo;
            this.hasScripts = hasScripts;
            this.headFolder = folder;
        }

        public String key() {
            return folder.toLowerCase(Locale.ROOT);
        }

        public String subtitle() {
            if (description != null && !description.isEmpty()) {
                return description;
            }
            if (author != null && !author.isEmpty()) {
                return "Автор: " + author;
            }
            return section.title;
        }

        /** Есть ли у пака несколько видов. */
        public boolean hasVariants() {
            return variants.size() > 1;
        }

        /** Выбранный сейчас вид (или сам пак). */
        public Entry activeVariant() {
            if (variants.size() < 2) {
                return this;
            }
            String wanted = FiguraPackSettings.getVariant(folder);
            if (wanted != null) {
                for (int i = 0; i < variants.size(); i++) {
                    if (variants.get(i).folder.equalsIgnoreCase(wanted)) {
                        return variants.get(i);
                    }
                }
            }
            return this;
        }

        /** Нужно ли рисовать этот .bbmodel целиком. */
        public boolean isModelVisible(Path model) {
            if (model == null) {
                return false;
            }
            String id = modelId(model);
            if (hiddenModels.contains(id)) {
                return false;
            }
            if (EXTRA_MODELS.contains(id)) {
                return false;
            }
            // У аватаров с нормальной main-моделью рисуем только её:
            // всё остальное в паках Blue Archive — оружие, умения и прочая UI-мелочь.
            if (section == Section.AVATARS && primaryModel != null && isMainModel(primaryModel)
                    && !primaryModel.equals(model)) {
                return false;
            }
            return true;
        }

        /** Имя модели без расширения, в нижнем регистре. */
        public String modelId(Path model) {
            String file = model.getFileName().toString();
            int dot = file.lastIndexOf('.');
            String id = dot > 0 ? file.substring(0, dot) : file;
            return id.toLowerCase(Locale.ROOT);
        }

        /** Скрыта ли конкретная косточка внутри модели. */
        public boolean isBoneHidden(String modelId, String bonePath) {
            if (modelId == null || bonePath == null) {
                return false;
            }
            String full = ("models.models." + modelId + "." + bonePath).toLowerCase(Locale.ROOT);
            if (hiddenPaths.contains(full)) {
                return true;
            }
            for (String hidden : hiddenPaths) {
                if (full.startsWith(hidden + ".")) {
                    return true;
                }
            }
            return false;
        }
    }

    private static boolean isMainModel(Path model) {
        if (model == null) {
            return false;
        }
        String file = model.getFileName().toString().toLowerCase(Locale.ROOT);
        return file.equals("main.bbmodel") || file.equals("model.bbmodel")
                || file.equals("avatar.bbmodel") || file.equals("player.bbmodel");
    }

    // ------------------------------------------------------------ Состояние

    private static final Object LOCK = new Object();
    /** Головы групп — то, что видно в GUI. */
    private static final List<Entry> ENTRIES = new ArrayList<Entry>();
    /** Все паки без группировки — для поиска по имени папки. */
    private static final List<Entry> FLAT = new ArrayList<Entry>();
    private static final Set<String> FAVORITES = new LinkedHashSet<String>();

    private static volatile boolean loaded;
    private static volatile boolean favoritesLoaded;

    private FiguraAvatarLibrary() {
    }

    // ------------------------------------------------------------------ API

    /** Список для GUI: по одной записи на персонажа. */
    public static List<Entry> all() {
        ensureLoaded();
        synchronized (LOCK) {
            return new ArrayList<Entry>(ENTRIES);
        }
    }

    /** Все паки, включая все виды. */
    public static List<Entry> allFlat() {
        ensureLoaded();
        synchronized (LOCK) {
            return new ArrayList<Entry>(FLAT);
        }
    }

    public static Entry byFolder(String folder) {
        if (folder == null) {
            return null;
        }
        ensureLoaded();
        synchronized (LOCK) {
            for (Entry entry : FLAT) {
                if (entry.folder.equalsIgnoreCase(folder)) {
                    return entry;
                }
            }
        }
        return null;
    }

    /** Голова группы для любого вида. */
    public static Entry head(String folder) {
        Entry entry = byFolder(folder);
        if (entry == null) {
            return null;
        }
        Entry byHead = byFolder(entry.headFolder);
        return byHead == null ? entry : byHead;
    }

    /** Имя папки головы группы (ключ для настроек). */
    public static String headFolder(String folder) {
        Entry entry = byFolder(folder);
        return entry == null ? folder : entry.headFolder;
    }

    /** Фильтр для GUI: секция + строка поиска + только избранное. */
    public static List<Entry> filter(Section section, String search, boolean favoritesOnly) {
        ensureLoaded();
        String query = search == null ? "" : search.trim().toLowerCase(Locale.ROOT);

        List<Entry> result = new ArrayList<Entry>();
        synchronized (LOCK) {
            for (Entry entry : ENTRIES) {
                if (section != null && section != Section.ALL && entry.section != section) {
                    continue;
                }
                if (favoritesOnly && !isFavorite(entry.folder)) {
                    continue;
                }
                if (!query.isEmpty()) {
                    StringBuilder hay = new StringBuilder();
                    for (int i = 0; i < entry.variants.size(); i++) {
                        Entry v = entry.variants.get(i);
                        hay.append(v.name).append(' ').append(v.folder).append(' ');
                    }
                    hay.append(entry.name).append(' ').append(entry.folder).append(' ')
                            .append(entry.description == null ? "" : entry.description).append(' ')
                            .append(entry.author == null ? "" : entry.author);
                    if (hay.toString().toLowerCase(Locale.ROOT).indexOf(query) < 0) {
                        continue;
                    }
                }
                result.add(entry);
            }
        }
        return result;
    }

    public static int count(Section section) {
        return filter(section, "", false).size();
    }

    public static void invalidate() {
        synchronized (LOCK) {
            ENTRIES.clear();
            FLAT.clear();
            loaded = false;
        }
    }

    public static void reload() {
        invalidate();
        ensureLoaded();
    }

    public static boolean isLoaded() {
        return loaded;
    }

    // --------------------------------------------------------- Избранное

    public static boolean isFavorite(String folder) {
        if (folder == null) {
            return false;
        }
        ensureFavorites();
        synchronized (LOCK) {
            return FAVORITES.contains(folder.toLowerCase(Locale.ROOT));
        }
    }

    public static void toggleFavorite(String folder) {
        if (folder == null) {
            return;
        }
        ensureFavorites();
        String key = folder.toLowerCase(Locale.ROOT);
        synchronized (LOCK) {
            if (!FAVORITES.remove(key)) {
                FAVORITES.add(key);
            }
        }
        saveFavorites();
    }

    private static Path favoritesFile() {
        return gameDir().toPath().resolve("figura").resolve("harmony_favorites.txt");
    }

    private static void ensureFavorites() {
        if (favoritesLoaded) {
            return;
        }
        favoritesLoaded = true;
        Path file = favoritesFile();
        if (!Files.isRegularFile(file)) {
            return;
        }
        try {
            List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
            synchronized (LOCK) {
                for (String line : lines) {
                    String trimmed = line.trim().toLowerCase(Locale.ROOT);
                    if (!trimmed.isEmpty()) {
                        FAVORITES.add(trimmed);
                    }
                }
            }
        } catch (Exception ignored) {
        }
    }

    private static void saveFavorites() {
        try {
            Path file = favoritesFile();
            Path parent = file.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            StringBuilder sb = new StringBuilder();
            synchronized (LOCK) {
                for (String key : FAVORITES) {
                    sb.append(key).append('\n');
                }
            }
            Files.write(file, sb.toString().getBytes(StandardCharsets.UTF_8));
        } catch (Exception ignored) {
        }
    }

    // ------------------------------------------------------------ Сканер

    private static File gameDir() {
        Minecraft mc = Minecraft.getInstance();
        return mc != null && mc.gameDir != null ? mc.gameDir : new File(".");
    }

    private static void ensureLoaded() {
        if (loaded) {
            return;
        }
        synchronized (LOCK) {
            if (loaded) {
                return;
            }
            ENTRIES.clear();
            FLAT.clear();
            FLAT.addAll(scan());
            ENTRIES.addAll(group(FLAT));
            addHeadMountCopies();
            applyPetModule();
            applyModuleCards();
            loaded = true;
        }
    }

    /** Паки петов, у которых есть вторая карточка в «Аксессуарах» (носится на голове). */
    private static final Set<String> HEAD_MOUNT = new HashSet<String>(Arrays.asList(
            "axolotl!"
    ));

    /** Делает копию пета в разделе «Аксессуары» — она сидит на голове. */
    private static void addHeadMountCopies() {
        List<Entry> copies = new ArrayList<Entry>();
        for (int i = 0; i < ENTRIES.size(); i++) {
            Entry entry = ENTRIES.get(i);
            if (entry.section != Section.PETS || entry.headMount) {
                continue;
            }
            if (!HEAD_MOUNT.contains(entry.folder.toLowerCase(Locale.ROOT))) {
                continue;
            }
            Entry copy = new Entry(entry.folder + "#head", entry.name, entry.description, entry.author,
                    entry.color, Section.ACCESSORIES, entry.path, entry.preview, entry.models,
                    entry.primaryModel, entry.hiddenPaths, entry.hiddenModels, entry.moveTo,
                    entry.hasScripts);
            copy.headMount = true;
            copy.variantLabel = "На голове";
            copies.add(copy);
        }
        ENTRIES.addAll(copies);
        FLAT.addAll(copies);
    }

    /** Паки, которые переехали внутрь функции Pet и своих карточек больше не имеют. */
    private static final Set<String> PET_MODULE = new HashSet<String>(Arrays.asList(
            "axolotl!",
            "turtle pet"
    ));

    /**
     * Убирает Axolotl! и Turtle Pet из вкладки «Петы» (они теперь внутри функции Pet)
     * и ставит туда карточку самой функции Pet. Из FLAT паки не удаляются —
     * по ним работают функция Pet и копия аксолотля на голове.
     */
    /** Имена питомцев на случай, если модуль ещё не поднялся. */
    private static final String[] FALLBACK_PETS = new String[]{
            "Пёс", "Дракон", "Коровка", "Спайк", "Минун",
            "Фредди", "Пингвин", "Энгри бёрдс", "Аксолотль", "Черепашка"
    };

    /**
     * Карточки визуальных функций Harmony, которые переехали из ClickGUI в Figura Cosmetic:
     * {имя модуля, подпись, раздел, вид иконки}.
     */
    private static final String[][] MODULE_CARDS = new String[][]{
            {"Katana", "Катана", "3D-катана на спине", "WEAPONS", "sword"},
            {"ChinaHat", "Китайская шляпа", "Шляпа-конус над головой", "ACCESSORIES", "hat"},
            {"Raincoat", "Плащ", "Кастомный плащ за спиной", "ACCESSORIES", "coat"},
            {"PatPatPat", "Погладить", "Гладить питомцев и мобов правой кнопкой", "EMOTES", "hand"}
    };

    /** Ставит карточки функций в нужные разделы библиотеки. */
    private static void applyModuleCards() {
        for (int i = 0; i < MODULE_CARDS.length; i++) {
            String[] row = MODULE_CARDS[i];
            Section target;
            try {
                target = Section.valueOf(row[3]);
            } catch (Exception ignored) {
                continue;
            }
            Entry card = new Entry("#module:" + row[0], row[1], row[2], "Harmony", -1,
                    target, null, null, new ArrayList<Path>(), null,
                    new HashSet<String>(), new HashSet<String>(), new LinkedHashMap<String, String>(), false);
            card.moduleCard = true;
            card.moduleName = row[0];
            card.iconKind = row[4];
            ENTRIES.add(card);
            FLAT.add(card);
        }
        sortEntries();
    }

    /** Сортировка каталога: сначала раздел, потом имя. */
    private static void sortEntries() {
        Collections.sort(ENTRIES, new Comparator<Entry>() {
            public int compare(Entry a, Entry b) {
                if (a.section.order != b.section.order) {
                    return a.section.order - b.section.order;
                }
                return a.name.compareToIgnoreCase(b.name);
            }
        });
    }

    private static void applyPetModule() {
        Iterator<Entry> it = ENTRIES.iterator();
        while (it.hasNext()) {
            Entry entry = it.next();
            if (entry.headMount || entry.moduleCard) {
                continue;
            }
            if (PET_MODULE.contains(entry.folder.toLowerCase(Locale.ROOT))) {
                it.remove();
            }
        }

        String[] pets = PetModuleBridge.petNames();
        if (pets == null || pets.length == 0) {
            pets = FALLBACK_PETS;
        }
        for (int i = 0; i < pets.length; i++) {
            String pet = pets[i];
            if (pet == null || pet.trim().isEmpty()) {
                continue;
            }
            Entry card = new Entry("#pet:" + pet, pet,
                    "Питомец Harmony", "Harmony", -1,
                    Section.PETS, null, null, new ArrayList<Path>(), null,
                    new HashSet<String>(), new HashSet<String>(), new LinkedHashMap<String, String>(), false);
            card.moduleCard = true;
            card.petName = pet;
            card.iconKind = "paw";
            ENTRIES.add(card);
            FLAT.add(card);
        }
        Collections.sort(ENTRIES, new Comparator<Entry>() {
            public int compare(Entry a, Entry b) {
                if (a.section.order != b.section.order) {
                    return a.section.order - b.section.order;
                }
                return a.name.compareToIgnoreCase(b.name);
            }
        });
    }

    private static List<Entry> scan() {
        List<Entry> result = new ArrayList<Entry>();
        Path root = FiguraAvatarInstaller.avatarsDir();
        if (!Files.isDirectory(root)) {
            return result;
        }
        List<Path> children = new ArrayList<Path>();
        Stream<Path> stream = null;
        try {
            stream = Files.list(root);
            Iterator<Path> it = stream.iterator();
            while (it.hasNext()) {
                children.add(it.next());
            }
        } catch (Exception ignored) {
        } finally {
            if (stream != null) {
                try {
                    stream.close();
                } catch (Exception ignored) {
                }
            }
        }

        for (Path child : children) {
            if (!Files.isDirectory(child)) {
                continue;
            }
            String folderName = child.getFileName().toString().toLowerCase(Locale.ROOT);
            if (BLOCKED.contains(folderName)) {
                continue;
            }
            Entry entry = read(child);
            if (entry != null) {
                result.add(entry);
            }
        }

        Collections.sort(result, new Comparator<Entry>() {
            public int compare(Entry a, Entry b) {
                if (a.section.order != b.section.order) {
                    return a.section.order - b.section.order;
                }
                return a.folder.compareToIgnoreCase(b.folder);
            }
        });
        return result;
    }

    // ---------------------------------------------------------- Группы видов

    /** Склеивает варианты одного персонажа в одну карточку. */
    private static List<Entry> group(List<Entry> flat) {
        Map<String, List<Entry>> groups = new LinkedHashMap<String, List<Entry>>();
        for (int i = 0; i < flat.size(); i++) {
            Entry entry = flat.get(i);
            String key = groupKey(entry);
            List<Entry> bucket = groups.get(key);
            if (bucket == null) {
                bucket = new ArrayList<Entry>();
                groups.put(key, bucket);
            }
            bucket.add(entry);
        }

        List<Entry> heads = new ArrayList<Entry>();
        for (Map.Entry<String, List<Entry>> e : groups.entrySet()) {
            List<Entry> bucket = e.getValue();
            Entry head = bucket.get(0);
            for (int i = 1; i < bucket.size(); i++) {
                Entry candidate = bucket.get(i);
                if (candidate.name.length() < head.name.length()) {
                    head = candidate;
                }
            }
            head.variants.clear();
            head.variants.add(head);
            for (int i = 0; i < bucket.size(); i++) {
                Entry other = bucket.get(i);
                if (other != head) {
                    head.variants.add(other);
                }
            }
            for (int i = 0; i < head.variants.size(); i++) {
                Entry variant = head.variants.get(i);
                variant.headFolder = head.folder;
                variant.variantLabel = variantLabel(head, variant, i);
            }
            heads.add(head);
        }

        Collections.sort(heads, new Comparator<Entry>() {
            public int compare(Entry a, Entry b) {
                if (a.section.order != b.section.order) {
                    return a.section.order - b.section.order;
                }
                return a.folder.compareToIgnoreCase(b.folder);
            }
        });
        return heads;
    }

    private static String groupKey(Entry entry) {
        Matcher matcher = NUMBERED.matcher(entry.folder);
        if (matcher.find()) {
            // 01a_Shizuko и 01b_Shizuko_Swimsuit — один и тот же персонаж.
            return entry.section.name() + "|#" + matcher.group(1);
        }
        return entry.section.name() + "|" + baseName(entry.name);
    }

    /** «Winter Coat Slim Body» -> «winter coat». */
    private static String baseName(String name) {
        if (name == null) {
            return "";
        }
        String text = name.toLowerCase(Locale.ROOT);
        text = text.replaceAll("\\([^)]*\\)", " ");
        text = text.replaceAll("\\[[^\\]]*\\]", " ");
        text = text.replace('-', ' ').replace('_', ' ');
        text = text.replaceAll("[^a-zа-яё0-9 ]", " ");
        text = text.replaceAll("\\s+", " ").trim();

        List<String> words = new ArrayList<String>(Arrays.asList(text.split(" ")));
        while (words.size() > 1) {
            String last = words.get(words.size() - 1);
            if (VARIANT_WORDS.contains(last) || last.matches("\\d+(\\.\\d+)?[a-z]?")) {
                words.remove(words.size() - 1);
            } else {
                break;
            }
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < words.size(); i++) {
            if (words.get(i).isEmpty()) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(words.get(i));
        }
        return sb.length() == 0 ? text : sb.toString();
    }

    private static String variantLabel(Entry head, Entry variant, int index) {
        if (variant == head) {
            return "Обычная";
        }
        String full = variant.name == null ? "" : variant.name.trim();
        String base = head.name == null ? "" : head.name.trim();
        String label = full;
        if (base.length() > 0 && full.toLowerCase(Locale.ROOT).startsWith(base.toLowerCase(Locale.ROOT))) {
            label = full.substring(base.length());
        }
        label = label.replaceAll("^[\\s\\-_()\\[\\]]+", "");
        label = label.replaceAll("[\\s\\-_()\\[\\]]+$", "");
        label = label.replaceAll("\\s+", " ").trim();
        if (label.isEmpty()) {
            label = full.isEmpty() ? ("Вид " + (index + 1)) : full;
        }
        return normalizeWords(label);
    }

    // ------------------------------------------------------------- Чтение

    private static Entry read(Path dir) {
        String folder = dir.getFileName().toString();

        List<Path> models = collectModels(dir);
        Path preview = dir.resolve("avatar.png");
        if (!Files.isRegularFile(preview)) {
            preview = null;
        }
        if (models.isEmpty() && preview == null) {
            return null;
        }

        String name = null;
        String description = "";
        String author = "";
        int color = -1;
        Set<String> hiddenPaths = new LinkedHashSet<String>();
        Set<String> hiddenModels = new LinkedHashSet<String>();
        Map<String, String> moveTo = new LinkedHashMap<String, String>();

        Path json = dir.resolve("avatar.json");
        if (Files.isRegularFile(json)) {
            InputStreamReader reader = null;
            try {
                reader = new InputStreamReader(Files.newInputStream(json), StandardCharsets.UTF_8);
                JsonElement parsed = new JsonParser().parse(reader);
                if (parsed != null && parsed.isJsonObject()) {
                    JsonObject obj = parsed.getAsJsonObject();
                    if (obj.has("name") && obj.get("name").isJsonPrimitive()) {
                        name = obj.get("name").getAsString();
                    }
                    if (obj.has("description") && obj.get("description").isJsonPrimitive()) {
                        description = obj.get("description").getAsString();
                    }
                    if (obj.has("authors") && obj.get("authors").isJsonPrimitive()) {
                        author = obj.get("authors").getAsString();
                    } else if (obj.has("author") && obj.get("author").isJsonPrimitive()) {
                        author = obj.get("author").getAsString();
                    }
                    if (obj.has("color") && obj.get("color").isJsonPrimitive()) {
                        color = parseColor(obj.get("color").getAsString());
                    }
                    if (obj.has("customizations") && obj.get("customizations").isJsonObject()) {
                        readCustomizations(obj.getAsJsonObject("customizations"), hiddenPaths, hiddenModels, moveTo);
                    }
                }
            } catch (Exception ignored) {
            } finally {
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (Exception ignored) {
                    }
                }
            }
        }

        if (name == null || name.trim().isEmpty()) {
            name = cleanName(folder);
        } else {
            name = cleanName(name);
        }

        Path primary = pickPrimary(models);
        boolean hasScripts = Files.isDirectory(dir.resolve("scripts"));
        Section section = detectSection(folder, name);

        return new Entry(folder, name, description, author, color, section, dir, preview,
                models, primary, hiddenPaths, hiddenModels, moveTo, hasScripts);
    }

    /**
     * customizations выглядят так:
     * "models.models.gun.Gun": { "visible": false, "moveTo": "...", "parentType": "None" }
     */
    private static void readCustomizations(JsonObject customizations, Set<String> hiddenPaths,
                                           Set<String> hiddenModels, Map<String, String> moveTo) {
        for (Map.Entry<String, JsonElement> e : customizations.entrySet()) {
            String path = e.getKey();
            if (path == null || path.isEmpty() || !e.getValue().isJsonObject()) {
                continue;
            }
            JsonObject value = e.getValue().getAsJsonObject();
            String lower = path.toLowerCase(Locale.ROOT);

            if (value.has("visible") && value.get("visible").isJsonPrimitive()) {
                boolean visible = true;
                try {
                    visible = value.get("visible").getAsBoolean();
                } catch (Exception ignored) {
                }
                if (!visible) {
                    hiddenPaths.add(lower);
                    String prefix = "models.models.";
                    if (lower.startsWith(prefix)) {
                        String rest = lower.substring(prefix.length());
                        if (rest.indexOf('.') < 0 && !rest.isEmpty()) {
                            hiddenModels.add(rest);
                        }
                    }
                }
            }
            if (value.has("moveTo") && value.get("moveTo").isJsonPrimitive()) {
                try {
                    moveTo.put(lower, value.get("moveTo").getAsString());
                } catch (Exception ignored) {
                }
            }
        }
    }

    private static List<Path> collectModels(Path dir) {
        final List<Path> models = new ArrayList<Path>();
        Stream<Path> stream = null;
        try {
            stream = Files.walk(dir, 4);
            Iterator<Path> it = stream.iterator();
            while (it.hasNext()) {
                Path p = it.next();
                if (!Files.isRegularFile(p)) {
                    continue;
                }
                String fileName = p.getFileName().toString().toLowerCase(Locale.ROOT);
                if (fileName.endsWith(".bbmodel")) {
                    models.add(p);
                }
            }
        } catch (Exception ignored) {
        } finally {
            if (stream != null) {
                try {
                    stream.close();
                } catch (Exception ignored) {
                }
            }
        }
        Collections.sort(models, new Comparator<Path>() {
            public int compare(Path a, Path b) {
                return a.getFileName().toString().compareToIgnoreCase(b.getFileName().toString());
            }
        });
        return models;
    }

    /** main.bbmodel / model.bbmodel / avatar.bbmodel, иначе — самый крупный файл. */
    private static Path pickPrimary(List<Path> models) {
        if (models.isEmpty()) {
            return null;
        }
        String[] preferred = {"main.bbmodel", "model.bbmodel", "avatar.bbmodel", "player.bbmodel"};
        for (String want : preferred) {
            for (Path p : models) {
                if (p.getFileName().toString().equalsIgnoreCase(want)) {
                    return p;
                }
            }
        }
        Path best = models.get(0);
        long bestSize = -1L;
        for (Path p : models) {
            long size;
            try {
                size = Files.size(p);
            } catch (Exception e) {
                size = 0L;
            }
            if (size > bestSize) {
                bestSize = size;
                best = p;
            }
        }
        return best;
    }

    private static int parseColor(String raw) {
        if (raw == null) {
            return -1;
        }
        String hex = raw.trim();
        if (hex.startsWith("#")) {
            hex = hex.substring(1);
        }
        if (hex.length() != 6) {
            return -1;
        }
        try {
            return Integer.parseInt(hex, 16);
        } catch (Exception e) {
            return -1;
        }
    }

    // ------------------------------------------------------ Имена и секции

    /** 01a_Shizuko_Swimsuit -> Shizuko Swimsuit. */
    public static String cleanName(String folder) {
        if (folder == null || folder.isEmpty()) {
            return "";
        }
        String text = splitCamel(folder);
        text = text.replace('_', ' ').replace('-', ' ');
        text = text.replaceAll("^\\s*\\d{1,4}[a-zA-Z]?\\s*", "");
        text = text.replaceAll("\\s+\\d+$", "");
        text = text.replaceAll("\\s+", " ").trim();
        if (text.isEmpty()) {
            text = folder;
        }
        return normalizeWords(text);
    }

    private static String splitCamel(String text) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (i > 0 && Character.isUpperCase(c)
                    && Character.isLowerCase(text.charAt(i - 1))) {
                sb.append(' ');
            }
            sb.append(c);
        }
        return sb.toString();
    }

    private static String normalizeWords(String text) {
        String[] words = text.split(" ");
        StringBuilder sb = new StringBuilder();
        for (String word : words) {
            if (word.isEmpty()) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append(' ');
            }
            if (word.length() <= 3 && word.equals(word.toUpperCase(Locale.ROOT))) {
                sb.append(word);
            } else {
                sb.append(Character.toUpperCase(word.charAt(0)));
                sb.append(word.substring(1).toLowerCase(Locale.ROOT));
            }
        }
        return sb.toString();
    }

    /** Классификация по имени — порт detectSection из Rich-клиента. */
    public static Section detectSection(String folder, String name) {
        String haystack = ((folder == null ? "" : folder) + " " + (name == null ? "" : name))
                .toLowerCase(Locale.ROOT);

        // Паки Blue Archive лежат как 18b_Michiru_Dress / 20c_Hina_Dress — это всегда полные аватары.
        if (folder != null && folder.matches("^\\d\\d[a-zA-Z]_.*")) {
            return Section.AVATARS;
        }

        if (containsAny(haystack, new String[]{"wing", "wings", "elytra", "cape", "cloak"})) {
            return Section.WINGS;
        }
        if (containsAny(haystack, new String[]{"script", "scripts", "lua", "macro", "walkguide",
                "bends", "pbr", "shader", "speed"})) {
            return Section.SCRIPTS;
        }
        if (containsAny(haystack, new String[]{"sword", "blade", "katana", "катана", "scythe", "bow",
                "weapon", "wrench", "dagger", "spear", "trident", "hammer", "halberd", "rapier", "sabre",
                "gun", "rifle", "pistol", "staff", "wand", "lance", "knife"})) {
            return Section.WEAPONS;
        }
        if (containsAny(haystack, new String[]{"pet", "pets", "companion", "axolotl", "turtle",
                "cat", "dog", "fox", "wolf", "bee", "fish"})) {
            return Section.PETS;
        }
        if (containsAny(haystack, new String[]{"accessory", "accessories", "asset", "hat", "cap",
                "crown", "helmet", "head", "headwear", "hood", "mask", "beret", "leaf", "mushroom",
                "flower", "glasses", "backpack", "tail", "ears", "horn", "horns", "halo", "stand",
                "coat", "jacket", "hoodie", "raincoat", "scarf", "umbrella", "necklace", "ribbon",
                "collar", "goggles", "axe"})) {
            return Section.ACCESSORIES;
        }
        return Section.AVATARS;
    }

    private static boolean containsAny(String haystack, String[] needles) {
        for (String needle : needles) {
            if (haystack.indexOf(needle) >= 0) {
                return true;
            }
        }
        return false;
    }
}
