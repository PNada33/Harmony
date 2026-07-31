import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.Remapper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;

public class SrgToMcpJarRemapper {
    public static void main(String[] args) throws Exception {
        if (args.length != 4) {
            throw new IllegalArgumentException("Usage: <methods.csv> <fields.csv> <input.jar> <output.jar>");
        }

        Map<String, String> methods = readCsv(Path.of(args[0]));
        Map<String, String> fields = readCsv(Path.of(args[1]));
        remapJar(Path.of(args[2]), Path.of(args[3]), methods, fields);
    }

    private static Map<String, String> readCsv(Path path) throws IOException {
        Map<String, String> mappings = new HashMap<>();
        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            String line = reader.readLine();
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",", 4);
                if (parts.length >= 2 && !parts[0].isBlank() && !parts[1].isBlank()) {
                    mappings.put(parts[0], parts[1]);
                }
            }
        }
        return mappings;
    }

    private static void remapJar(Path input, Path output, Map<String, String> methods, Map<String, String> fields) throws IOException {
        Files.createDirectories(output.toAbsolutePath().getParent());

        McpRemapper remapper = new McpRemapper(methods, fields);
        Set<String> written = new HashSet<>();

        try (JarInputStream jarInput = new JarInputStream(Files.newInputStream(input));
             JarOutputStream jarOutput = new JarOutputStream(Files.newOutputStream(output))) {
            JarEntry entry;
            while ((entry = jarInput.getNextJarEntry()) != null) {
                if (entry.isDirectory()) {
                    continue;
                }

                String name = entry.getName();
                if (!written.add(name)) {
                    continue;
                }

                JarEntry outEntry = new JarEntry(name);
                jarOutput.putNextEntry(outEntry);

                if (name.endsWith(".class")) {
                    byte[] remapped = remapClass(jarInput, remapper);
                    jarOutput.write(remapped);
                } else {
                    copy(jarInput, jarOutput);
                }

                jarOutput.closeEntry();
            }
        }
    }

    private static byte[] remapClass(InputStream input, Remapper remapper) throws IOException {
        ClassReader reader = new ClassReader(input);
        ClassWriter writer = new ClassWriter(0);
        ClassRemapper classRemapper = new ClassRemapper(writer, remapper);
        reader.accept(classRemapper, 0);
        return writer.toByteArray();
    }

    private static void copy(InputStream input, OutputStream output) throws IOException {
        byte[] buffer = new byte[16 * 1024];
        int read;
        while ((read = input.read(buffer)) >= 0) {
            output.write(buffer, 0, read);
        }
    }

    private static class McpRemapper extends Remapper {
        private final Map<String, String> methods;
        private final Map<String, String> fields;

        private McpRemapper(Map<String, String> methods, Map<String, String> fields) {
            this.methods = methods;
            this.fields = fields;
        }

        @Override
        public String mapMethodName(String owner, String name, String descriptor) {
            return methods.getOrDefault(name, name);
        }

        @Override
        public String mapInvokeDynamicMethodName(String name, String descriptor) {
            return methods.getOrDefault(name, name);
        }

        @Override
        public String mapFieldName(String owner, String name, String descriptor) {
            return fields.getOrDefault(name, name);
        }
    }
}
