package org.spongepowered.asm.mixin.injection;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.CLASS)
@Target({})
public @interface At {
    String value();

    String target() default "";

    Shift shift() default Shift.NONE;

    int ordinal() default -1;

    enum Shift {
        BEFORE,
        AFTER,
        BY,
        NONE
    }
}
