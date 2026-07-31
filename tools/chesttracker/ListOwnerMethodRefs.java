import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.Set;
import java.util.TreeSet;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class ListOwnerMethodRefs {
    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            throw new IllegalArgumentException("Usage: ListOwnerMethodRefs <owner-internal-name> <jar>...");
        }

        String owner = args[0];
        Set<String> refs = new TreeSet<>();

        for (int i = 1; i < args.length; i++) {
            try (JarFile jar = new JarFile(Path.of(args[i]).toFile())) {
                var entries = jar.entries();
                while (entries.hasMoreElements()) {
                    JarEntry entry = entries.nextElement();
                    if (!entry.getName().endsWith(".class")) {
                        continue;
                    }

                    try (InputStream stream = jar.getInputStream(entry)) {
                        ClassNode node = new ClassNode();
                        new ClassReader(stream).accept(node, ClassReader.SKIP_FRAMES);
                        for (MethodNode method : node.methods) {
                            for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; insn = insn.getNext()) {
                                if (insn.getOpcode() == Opcodes.INVOKEVIRTUAL
                                        || insn.getOpcode() == Opcodes.INVOKESTATIC
                                        || insn.getOpcode() == Opcodes.INVOKESPECIAL
                                        || insn.getOpcode() == Opcodes.INVOKEINTERFACE) {
                                    MethodInsnNode methodInsn = (MethodInsnNode) insn;
                                    if (owner.equals(methodInsn.owner)) {
                                        refs.add(methodInsn.name + methodInsn.desc);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        refs.forEach(System.out::println);
    }
}
