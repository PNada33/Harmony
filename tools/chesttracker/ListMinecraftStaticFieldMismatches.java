import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

import java.nio.file.Files;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class ListMinecraftStaticFieldMismatches {
    private static final String MINECRAFT = "net/minecraft/client/Minecraft";
    private static Map<String, String> staticMinecraftFields;

    public static void main(String[] args) throws Exception {
        staticMinecraftFields = readStaticMinecraftFields(Path.of("out/production/client/net/minecraft/client/Minecraft.class"));
        boolean found = false;
        for (String arg : args) {
            try (JarFile jar = new JarFile(Path.of(arg).toFile())) {
                Enumeration<JarEntry> entries = jar.entries();
                while (entries.hasMoreElements()) {
                    JarEntry entry = entries.nextElement();
                    if (!entry.getName().endsWith(".class")) {
                        continue;
                    }

                    try (InputStream stream = jar.getInputStream(entry)) {
                        ClassNode node = new ClassNode();
                        new ClassReader(stream).accept(node, 0);
                        for (MethodNode method : node.methods) {
                            for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; insn = insn.getNext()) {
                                if (insn instanceof FieldInsnNode field
                                        && field.getOpcode() == Opcodes.GETFIELD
                                        && MINECRAFT.equals(field.owner)
                                        && field.desc.equals(staticMinecraftFields.get(field.name))) {
                                    found = true;
                                    System.out.println(arg + " " + node.name + "." + method.name + method.desc + " GETFIELD static " + field.name);
                                }
                            }
                        }
                    }
                }
            }
        }

        if (!found) {
            System.out.println("No GETFIELD static Minecraft field mismatches found");
        }
    }

    private static Map<String, String> readStaticMinecraftFields(Path classFile) throws Exception {
        ClassNode node = new ClassNode();
        new ClassReader(Files.readAllBytes(classFile)).accept(node, ClassReader.SKIP_CODE | ClassReader.SKIP_FRAMES);

        Map<String, String> fields = new HashMap<>();
        for (FieldNode field : node.fields) {
            if ((field.access & Opcodes.ACC_STATIC) != 0) {
                fields.put(field.name, field.desc);
            }
        }
        return fields;
    }
}
