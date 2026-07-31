package xd.harm.config;

import xd.harm.command.api.PrefixImpl;
import xd.harm.utils.client.IMinecraft;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.Collections;

import static java.io.File.separator;

@UtilityClass
public class PrefixStorage implements IMinecraft {

    private final File file = new File(mc.gameDir, separator + "harmony" + separator + "files" + separator + "other" + separator + "prefix.cfg");
    private PrefixImpl prefixImpl = new PrefixImpl();
    public String prefix = "";

    @SneakyThrows
    public void load() {
        if (file.exists()) {
            prefixImpl.set(Files.readString(file.toPath()));
        } else {
            file.createNewFile();
            Files.write(file.toPath(), Collections.singleton("."), StandardOpenOption.WRITE);
            prefix = ".";
        }
    }

    @SneakyThrows
    public void updatePrefix(String newPrefix) {
        Files.write(file.toPath(), Collections.singleton(newPrefix), StandardOpenOption.WRITE);
    }
}
