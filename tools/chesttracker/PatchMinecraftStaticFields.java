import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;

public class PatchMinecraftStaticFields {
    private static final String MINECRAFT = "net/minecraft/client/Minecraft";
    private static final String MC_DESC = "Lnet/minecraft/client/Minecraft;";
    private static Map<String, String> staticMinecraftFields;

    public static void main(String[] args) throws Exception {
        staticMinecraftFields = readStaticMinecraftFields(Path.of("out/production/client/net/minecraft/client/Minecraft.class"));
        for (String arg : args) {
            Path jar = Path.of(arg);
            int patched = patchJar(jar);
            if (patched > 0) {
                System.out.println(jar + ": patched " + patched + " field reads");
            } else {
                System.out.println(jar + ": no changes");
            }
        }
    }

    private static int patchJar(Path jar) throws IOException {
        Path temp = Files.createTempFile(jar.getParent(), jar.getFileName().toString(), ".tmp");
        int patched = 0;

        try (JarFile input = new JarFile(jar.toFile());
             JarOutputStream output = new JarOutputStream(Files.newOutputStream(temp))) {
            Set<String> written = new HashSet<>();
            Enumeration<JarEntry> entries = input.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (!written.add(entry.getName())) {
                    continue;
                }

                JarEntry outEntry = new JarEntry(entry.getName());
                outEntry.setTime(entry.getTime());
                output.putNextEntry(outEntry);

                byte[] bytes;
                try (InputStream stream = input.getInputStream(entry)) {
                    bytes = readAll(stream);
                }

                if (!entry.isDirectory() && entry.getName().endsWith(".class")) {
                    PatchResult result = patchClass(bytes);
                    bytes = result.bytes;
                    patched += result.count;
                }

                output.write(bytes);
                output.closeEntry();
            }
        }

        if (patched > 0) {
            Files.move(temp, jar, StandardCopyOption.REPLACE_EXISTING);
        } else {
            Files.deleteIfExists(temp);
        }

        return patched;
    }

    private static PatchResult patchClass(byte[] bytes) {
        ClassNode node = new ClassNode();
        new ClassReader(bytes).accept(node, 0);

        int patched = 0;
        for (MethodNode method : node.methods) {
            for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; insn = insn.getNext()) {
                if (!(insn instanceof FieldInsnNode field)) {
                    continue;
                }

                if (field.getOpcode() != Opcodes.GETFIELD || !MINECRAFT.equals(field.owner) || !isTargetField(field)) {
                    continue;
                }

                AbstractInsnNode previous = previousRealInsn(field);
                AbstractInsnNode previousPrevious = previous == null ? null : previousRealInsn(previous);
                int removeCount = safeMinecraftInstanceLoadInstructionCount(previous, previousPrevious);
                if (removeCount == 0) {
                    continue;
                }

                if (removeCount == 2) {
                    method.instructions.remove(previousPrevious);
                }
                method.instructions.remove(previous);
                field.setOpcode(Opcodes.GETSTATIC);
                patched++;
            }
        }

        if (patched == 0) {
            return new PatchResult(bytes, 0);
        }

        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        node.accept(writer);
        return new PatchResult(writer.toByteArray(), patched);
    }

    private static boolean isTargetField(FieldInsnNode field) {
        return field.desc.equals(staticMinecraftFields.get(field.name));
    }

    private static Map<String, String> readStaticMinecraftFields(Path classFile) throws IOException {
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

    private static int safeMinecraftInstanceLoadInstructionCount(AbstractInsnNode insn, AbstractInsnNode previous) {
        if (insn instanceof MethodInsnNode method) {
            return method.getOpcode() == Opcodes.INVOKESTATIC
                    && MINECRAFT.equals(method.owner)
                    && "getInstance".equals(method.name)
                    && ("()" + MC_DESC).equals(method.desc) ? 1 : 0;
        }

        if (insn instanceof VarInsnNode var && var.getOpcode() == Opcodes.ALOAD) {
            return 1;
        }

        if (insn instanceof FieldInsnNode field
                && field.getOpcode() == Opcodes.GETFIELD
                && MC_DESC.equals(field.desc)
                && previous instanceof VarInsnNode var
                && var.getOpcode() == Opcodes.ALOAD) {
            return 2;
        }

        return 0;
    }

    private static AbstractInsnNode previousRealInsn(AbstractInsnNode insn) {
        AbstractInsnNode previous = insn.getPrevious();
        while (previous != null && previous.getOpcode() < 0) {
            previous = previous.getPrevious();
        }
        return previous;
    }

    private static byte[] readAll(InputStream stream) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int read;
        while ((read = stream.read(buffer)) >= 0) {
            output.write(buffer, 0, read);
        }
        return output.toByteArray();
    }

    private record PatchResult(byte[] bytes, int count) {
    }
}
