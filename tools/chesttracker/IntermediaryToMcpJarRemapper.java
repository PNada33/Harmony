import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.Remapper;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
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
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;

public class IntermediaryToMcpJarRemapper {
    public static void main(String[] args) throws Exception {
        if (args.length < 6 || args.length > 7) {
            throw new IllegalArgumentException("Usage: <intermediary-v2.jar> <mcp.tsrg> <methods.csv> <fields.csv> <input.jar> <output.jar> [--skip-nested-jars]");
        }

        boolean skipNestedJars = args.length == 7 && "--skip-nested-jars".equals(args[6]);
        MappingData mappings = buildMappings(Path.of(args[0]), Path.of(args[1]), Path.of(args[2]), Path.of(args[3]));
        remapJar(Path.of(args[4]), Path.of(args[5]), mappings, skipNestedJars);
    }

    private static MappingData buildMappings(Path intermediaryJar, Path mcpTsrg, Path methodsCsv, Path fieldsCsv) throws IOException {
        TinyData tiny = readIntermediaryMappings(intermediaryJar);
        McpData mcp = readMcpMappings(mcpTsrg);
        Map<String, String> mcpMethods = readCsv(methodsCsv);
        Map<String, String> mcpFields = readCsv(fieldsCsv);

        MappingData out = new MappingData();

        for (Map.Entry<String, String> entry : tiny.interToOfficialClass.entrySet()) {
            String mcpClass = mcp.officialToMcpClass.get(entry.getValue());
            if (mcpClass != null) {
                out.classes.put(entry.getKey(), mcpClass);
            }
        }

        for (TinyMember method : tiny.methods) {
            String srgName = mcp.officialMethodToSrg.get(memberKey(method.officialOwner, method.officialName, method.officialDesc));
            if (srgName != null) {
                String mcpName = mcpMethods.getOrDefault(srgName, srgName);
                out.methods.put(memberKey(method.interOwner, method.interName, method.interDesc), mcpName);
                out.methodNames.put(method.interName, mcpName);
            }
        }

        for (TinyMember field : tiny.fields) {
            String srgName = mcp.officialFieldToSrg.get(field.officialOwner + "\t" + field.officialName);
            if (srgName != null) {
                String mcpName = mcpFields.getOrDefault(srgName, srgName);
                out.fields.put(field.interOwner + "\t" + field.interName, mcpName);
                out.fieldNames.put(field.interName, mcpName);
            }
        }

        return out;
    }

    private static TinyData readIntermediaryMappings(Path intermediaryJar) throws IOException {
        TinyData data = new TinyData();

        try (JarFile jar = new JarFile(intermediaryJar.toFile())) {
            JarEntry entry = jar.getJarEntry("mappings/mappings.tiny");
            if (entry == null) {
                throw new IOException("Missing mappings/mappings.tiny in " + intermediaryJar);
            }

            try (BufferedReader reader = new BufferedReader(new java.io.InputStreamReader(jar.getInputStream(entry), StandardCharsets.UTF_8))) {
                String header = reader.readLine();
                if (header == null || !header.startsWith("tiny\t2\t0\tofficial\tintermediary")) {
                    throw new IOException("Unsupported intermediary tiny header: " + header);
                }

                String officialOwner = null;
                String interOwner = null;
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.startsWith("c\t")) {
                        String[] parts = line.split("\t");
                        if (parts.length >= 3) {
                            officialOwner = parts[1];
                            interOwner = parts[2];
                            data.officialToInterClass.put(officialOwner, interOwner);
                            data.interToOfficialClass.put(interOwner, officialOwner);
                        }
                    } else if (line.startsWith("\tm\t") && officialOwner != null && interOwner != null) {
                        String[] parts = line.split("\t");
                        if (parts.length >= 5) {
                            data.methods.add(new TinyMember(officialOwner, interOwner, parts[3], parts[4], parts[2]));
                        }
                    } else if (line.startsWith("\tf\t") && officialOwner != null && interOwner != null) {
                        String[] parts = line.split("\t");
                        if (parts.length >= 5) {
                            data.fields.add(new TinyMember(officialOwner, interOwner, parts[3], parts[4], parts[2]));
                        }
                    }
                }
            }
        }

        for (TinyMember method : data.methods) {
            method.interDesc = mapDescriptor(method.officialDesc, data.officialToInterClass);
        }
        for (TinyMember field : data.fields) {
            methodLikeFieldDescriptor(field, data.officialToInterClass);
        }

        return data;
    }

    private static void methodLikeFieldDescriptor(TinyMember field, Map<String, String> classMap) {
        field.interDesc = mapDescriptor(field.officialDesc, classMap);
    }

    private static McpData readMcpMappings(Path tsrg) throws IOException {
        McpData data = new McpData();

        try (BufferedReader reader = Files.newBufferedReader(tsrg, StandardCharsets.UTF_8)) {
            String officialOwner = null;
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isEmpty()) {
                    continue;
                }

                if (!Character.isWhitespace(line.charAt(0))) {
                    String[] parts = line.split(" ");
                    if (parts.length >= 2) {
                        officialOwner = parts[0];
                        data.officialToMcpClass.put(parts[0], parts[1]);
                    }
                } else if (officialOwner != null) {
                    String trimmed = line.trim();
                    String[] parts = trimmed.split(" ");
                    if (parts.length == 2) {
                        data.officialFieldToSrg.put(officialOwner + "\t" + parts[0], parts[1]);
                    } else if (parts.length >= 3) {
                        data.officialMethodToSrg.put(memberKey(officialOwner, parts[0], parts[1]), parts[2]);
                    }
                }
            }
        }

        return data;
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

    private static void remapJar(Path input, Path output, MappingData mappings, boolean skipNestedJars) throws IOException {
        Files.createDirectories(output.toAbsolutePath().getParent());

        McpRemapper remapper = new McpRemapper(mappings);
        Set<String> written = new HashSet<>();

        try (JarFile jarInput = new JarFile(input.toFile());
             JarOutputStream jarOutput = new JarOutputStream(Files.newOutputStream(output))) {
            jarInput.stream().forEach(entry -> {
                try {
                    if (entry.isDirectory()) {
                        return;
                    }

                    String name = entry.getName();
                    if (skipNestedJars && name.startsWith("META-INF/jars/") && name.endsWith(".jar")) {
                        return;
                    }

                    String outName = name.endsWith(".class") ? remapper.map(name.substring(0, name.length() - 6)) + ".class" : name;
                    if (!written.add(outName)) {
                        return;
                    }

                    JarEntry outEntry = new JarEntry(outName);
                    outEntry.setTime(entry.getTime());
                    jarOutput.putNextEntry(outEntry);

                    try (InputStream inputStream = jarInput.getInputStream(entry)) {
                        if (name.endsWith(".class")) {
                            jarOutput.write(remapClass(inputStream, remapper));
                        } else {
                            copy(inputStream, jarOutput);
                        }
                    }

                    jarOutput.closeEntry();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        } catch (RuntimeException e) {
            if (e.getCause() instanceof IOException) {
                throw (IOException) e.getCause();
            }
            throw e;
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

    private static String mapDescriptor(String descriptor, Map<String, String> classMap) {
        StringBuilder out = new StringBuilder(descriptor.length());
        int index = 0;
        while (index < descriptor.length()) {
            char c = descriptor.charAt(index);
            if (c == 'L') {
                int end = descriptor.indexOf(';', index);
                String name = descriptor.substring(index + 1, end);
                out.append('L').append(classMap.getOrDefault(name, name)).append(';');
                index = end + 1;
            } else {
                out.append(c);
                index++;
            }
        }
        return out.toString();
    }

    private static String memberKey(String owner, String name, String descriptor) {
        return owner + "\t" + name + "\t" + descriptor;
    }

    private static class McpRemapper extends Remapper {
        private final MappingData mappings;

        private McpRemapper(MappingData mappings) {
            this.mappings = mappings;
        }

        @Override
        public String map(String internalName) {
            return mappings.classes.getOrDefault(internalName, internalName);
        }

        @Override
        public String mapMethodName(String owner, String name, String descriptor) {
            String mapped = mappings.methods.get(memberKey(owner, name, descriptor));
            return mapped != null ? mapped : mappings.methodNames.getOrDefault(name, name);
        }

        @Override
        public String mapFieldName(String owner, String name, String descriptor) {
            String mapped = mappings.fields.get(owner + "\t" + name);
            return mapped != null ? mapped : mappings.fieldNames.getOrDefault(name, name);
        }
    }

    private static class MappingData {
        private final Map<String, String> classes = new HashMap<>();
        private final Map<String, String> methods = new HashMap<>();
        private final Map<String, String> fields = new HashMap<>();
        private final Map<String, String> methodNames = new HashMap<>();
        private final Map<String, String> fieldNames = new HashMap<>();
    }

    private static class TinyData {
        private final Map<String, String> officialToInterClass = new HashMap<>();
        private final Map<String, String> interToOfficialClass = new HashMap<>();
        private final java.util.List<TinyMember> methods = new java.util.ArrayList<>();
        private final java.util.List<TinyMember> fields = new java.util.ArrayList<>();
    }

    private static class TinyMember {
        private final String officialOwner;
        private final String interOwner;
        private final String officialName;
        private final String interName;
        private final String officialDesc;
        private String interDesc;

        private TinyMember(String officialOwner, String interOwner, String officialName, String interName, String officialDesc) {
            this.officialOwner = officialOwner;
            this.interOwner = interOwner;
            this.officialName = officialName;
            this.interName = interName;
            this.officialDesc = officialDesc;
        }
    }

    private static class McpData {
        private final Map<String, String> officialToMcpClass = new HashMap<>();
        private final Map<String, String> officialMethodToSrg = new HashMap<>();
        private final Map<String, String> officialFieldToSrg = new HashMap<>();
    }
}
