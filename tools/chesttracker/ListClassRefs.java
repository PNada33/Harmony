import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Handle;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Set;
import java.util.TreeSet;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class ListClassRefs {
    public static void main(String[] args) throws Exception {
        Set<String> refs = new TreeSet<>();
        Set<String> own = new TreeSet<>();

        for (String arg : args) {
            try (JarFile jar = new JarFile(Path.of(arg).toFile())) {
                jar.stream()
                        .filter(entry -> entry.getName().endsWith(".class"))
                        .forEach(entry -> readClass(jar, entry, refs, own));
            }
        }

        refs.removeAll(own);
        refs.forEach(System.out::println);
    }

    private static void readClass(JarFile jar, JarEntry entry, Set<String> refs, Set<String> own) {
        try (InputStream input = jar.getInputStream(entry)) {
            ClassReader reader = new ClassReader(input);
            own.add(reader.getClassName());
            reader.accept(new RefVisitor(refs), ClassReader.SKIP_FRAMES);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static class RefVisitor extends ClassVisitor {
        private final Set<String> refs;

        private RefVisitor(Set<String> refs) {
            super(Opcodes.ASM9);
            this.refs = refs;
        }

        @Override
        public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
            addInternal(superName);
            if (interfaces != null) {
                for (String itf : interfaces) {
                    addInternal(itf);
                }
            }
        }

        @Override
        public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
            addDesc(descriptor);
            return null;
        }

        @Override
        public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
            addDesc(descriptor);
            return null;
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
            addMethodDesc(descriptor);
            if (exceptions != null) {
                for (String exception : exceptions) {
                    addInternal(exception);
                }
            }
            return new MethodVisitor(Opcodes.ASM9) {
                @Override
                public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
                    addDesc(descriptor);
                    return null;
                }

                @Override
                public void visitTypeInsn(int opcode, String type) {
                    addInternal(type);
                }

                @Override
                public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
                    addInternal(owner);
                    addDesc(descriptor);
                }

                @Override
                public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
                    addInternal(owner);
                    addMethodDesc(descriptor);
                }

                @Override
                public void visitInvokeDynamicInsn(String name, String descriptor, Handle bootstrapMethodHandle, Object... bootstrapMethodArguments) {
                    addMethodDesc(descriptor);
                    addInternal(bootstrapMethodHandle.getOwner());
                    addMethodDesc(bootstrapMethodHandle.getDesc());
                    for (Object arg : bootstrapMethodArguments) {
                        if (arg instanceof Type) {
                            addType((Type) arg);
                        } else if (arg instanceof Handle) {
                            Handle handle = (Handle) arg;
                            addInternal(handle.getOwner());
                            addMethodDesc(handle.getDesc());
                        }
                    }
                }

                @Override
                public void visitLdcInsn(Object value) {
                    if (value instanceof Type) {
                        addType((Type) value);
                    }
                }

                @Override
                public void visitMultiANewArrayInsn(String descriptor, int numDimensions) {
                    addDesc(descriptor);
                }
            };
        }

        private void addMethodDesc(String descriptor) {
            addType(Type.getReturnType(descriptor));
            for (Type type : Type.getArgumentTypes(descriptor)) {
                addType(type);
            }
        }

        private void addDesc(String descriptor) {
            addType(Type.getType(descriptor));
        }

        private void addType(Type type) {
            while (type.getSort() == Type.ARRAY) {
                type = type.getElementType();
            }
            if (type.getSort() == Type.OBJECT) {
                addInternal(type.getInternalName());
            }
        }

        private void addInternal(String internalName) {
            if (internalName != null && !internalName.startsWith("[")) {
                refs.add(internalName);
            }
        }
    }
}
