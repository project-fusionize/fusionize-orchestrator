package dev.fusionize.worker.component.annotations;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RuntimeComponentDefinition {
    String value() default "";
    String description() default "";
}
