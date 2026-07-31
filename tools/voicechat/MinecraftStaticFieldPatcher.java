import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;

public class MinecraftStaticFieldPatcher {
    private static final String MINECRAFT = "net/minecraft/client/Minecraft";
    private static final Set<String> STATIC_FIELDS = new HashSet<>();

    static {
        STATIC_FIELDS.add("player");
        STATIC_FIELDS.add("world");
        STATIC_FIELDS.add("gameSettings");
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            throw new IllegalArgumentException("Usage: MinecraftStaticFieldPatcher <jar>");
        }

        Path jar = Path.of(args[0]);
        Path patched = jar.resolveSibling(jar.getFileName() + ".patched");

        try (JarFile input = new JarFile(jar.toFile());
             JarOutputStream output = new JarOutputStream(Files.newOutputStream(patched))) {
            input.stream().forEach(entry -> {
                try {
                    JarEntry newEntry = new JarEntry(entry.getName());
                    newEntry.setTime(entry.getTime());
                    output.putNextEntry(newEntry);

                    if (!entry.isDirectory()) {
                        byte[] bytes = readAll(input.getInputStream(entry));

                        if (entry.getName().endsWith(".class")) {
                            bytes = patchClass(bytes);
                        }

                        output.write(bytes);
                    }

                    output.closeEntry();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        }

        Files.move(patched, jar, StandardCopyOption.REPLACE_EXISTING);
    }

    private static byte[] patchClass(byte[] bytes) {
        ClassReader reader = new ClassReader(bytes);
        ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_MAXS);
        FieldPatchClassVisitor visitor = new FieldPatchClassVisitor(writer);
        reader.accept(visitor, 0);
        return visitor.changed ? writer.toByteArray() : bytes;
    }

    private static byte[] readAll(InputStream input) throws IOException {
        try (input; ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            input.transferTo(output);
            return output.toByteArray();
        }
    }

    private static class FieldPatchClassVisitor extends ClassVisitor {
        private boolean changed;

        private FieldPatchClassVisitor(ClassVisitor classVisitor) {
            super(Opcodes.ASM9, classVisitor);
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
            MethodVisitor methodVisitor = super.visitMethod(access, name, descriptor, signature, exceptions);
            return new MethodVisitor(Opcodes.ASM9, methodVisitor) {
                @Override
                public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
                    if (MINECRAFT.equals(owner) && STATIC_FIELDS.contains(name)) {
                        if (opcode == Opcodes.GETFIELD) {
                            changed = true;
                            super.visitInsn(Opcodes.POP);
                            super.visitFieldInsn(Opcodes.GETSTATIC, owner, name, descriptor);
                            return;
                        }

                        if (opcode == Opcodes.PUTFIELD) {
                            changed = true;
                            super.visitInsn(Opcodes.SWAP);
                            super.visitInsn(Opcodes.POP);
                            super.visitFieldInsn(Opcodes.PUTSTATIC, owner, name, descriptor);
                            return;
                        }
                    }

                    super.visitFieldInsn(opcode, owner, name, descriptor);
                }
            };
        }
    }
}
