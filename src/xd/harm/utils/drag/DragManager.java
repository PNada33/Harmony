package xd.harm.utils.drag;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.client.Minecraft;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.Map;

public class DragManager {
    public static LinkedHashMap<String, Dragging> draggables = new LinkedHashMap<>();
    private static final Map<String, SavedDrag> loadedDrags = new LinkedHashMap<>();

    private static final File DRAG_DATA = new File(Minecraft.getInstance().gameDir, "\\harmony\\files\\other\\drags.cfg");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().excludeFieldsWithoutExposeAnnotation().setPrettyPrinting().create();

    public static Dragging register(String name, Dragging dragging) {
        draggables.put(name, dragging);
        applyLoadedPosition(dragging);
        return dragging;
    }

    public static void save() {

        if (!DRAG_DATA.exists()) {
            DRAG_DATA.getParentFile().mkdirs();
        }
        try {
            Files.writeString(DRAG_DATA.toPath(), GSON.toJson(draggables.values()));
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public static void load() {
        if (!DRAG_DATA.exists()) {
            DRAG_DATA.getParentFile().mkdirs();
            return;
        }
        SavedDrag[] draggings;

        try {
            draggings = GSON.fromJson(Files.readString(DRAG_DATA.toPath()), SavedDrag[].class);

        } catch (IOException ex) {
            ex.printStackTrace();
            return;
        }

        loadedDrags.clear();

        if (draggings == null) {
            return;
        }

        for (SavedDrag dragging : draggings) {
            if (dragging == null || dragging.name == null) {
                continue;
            }

            loadedDrags.put(dragging.name, dragging);
            Dragging currentDrag = draggables.get(dragging.name);

            if (currentDrag != null) {
                applyLoadedPosition(currentDrag, dragging);
            }
        }
    }

    public static void applyLoadedPosition(Dragging dragging) {
        SavedDrag saved = loadedDrags.get(dragging.getName());

        if (saved != null) {
            applyLoadedPosition(dragging, saved);
        }
    }

    private static void applyLoadedPosition(Dragging dragging, SavedDrag saved) {
        dragging.setX(saved.x);
        dragging.setY(saved.y);
    }

    public static void resetAllPositions() {
        for (Dragging dragging : draggables.values()) {
            dragging.resetPosition();
        }
        save();
    }


}

class SavedDrag {
    @Expose
    @SerializedName("x")
    float x;

    @Expose
    @SerializedName("y")
    float y;

    @Expose
    @SerializedName("name")
    String name;
}
