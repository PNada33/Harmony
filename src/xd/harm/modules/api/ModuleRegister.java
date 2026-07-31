package xd.harm.modules.api;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(value = RetentionPolicy.RUNTIME)
public @interface ModuleRegister {
    String name();
    String desc() default "У этого модуля нет описания.";
    int key() default 0;
    Category category();
}
