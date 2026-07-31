package xd.harm.config;

import xd.harm.utils.SoundUtil;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;

import java.io.File;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.Set;

import xd.harm.utils.client.IMinecraft;

import static java.io.File.separator;

@UtilityClass
public class StaffStorage implements IMinecraft {

    @Getter
    private final Set<String> staffs = new HashSet<>();
    private final File file = new File(mc.gameDir + separator + "harmony" + separator + "files" + separator + "other" + separator + "staffs.cfg");

    @SneakyThrows
    public void load() {
        if (file.exists()) {
            staffs.addAll(Files.readAllLines(file.toPath()));
        } else {
            file.createNewFile();
        }
    }

    public void add(String name) {
        staffs.add(name);
        SoundUtil.playSound("addfriend.wav");
        save();
    }

    public void remove(String name) {
        staffs.remove(name);
        SoundUtil.playSound("removefriend.wav");
        save();
    }

    public void clear() {
        staffs.clear();
        SoundUtil.playSound("removefriend.wav");
        save();
    }

    public boolean isStaff(String name) {
        return staffs.contains(name);
    }

    @SneakyThrows
    private void save() {
        Files.write(file.toPath(), staffs);
    }
}
