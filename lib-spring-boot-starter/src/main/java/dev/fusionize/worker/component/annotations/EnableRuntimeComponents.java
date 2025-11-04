package dev.fusionize.worker.component.annotations;

import dev.fusionize.worker.component.RuntimeComponentRegistrar;
import org.springframework.context.annotation.Import;
import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(RuntimeComponentRegistrar.class)
public @interface EnableRuntimeComponents {
    /**
     * Base packages to scan for @RuntimeComponent annotations.
     * If empty, will scan the package of the class that declares this annotation.
     */
    String[] basePackages() default {};

    /**
     * Base package classes to scan for @RuntimeComponent annotations.
     */
    Class<?>[] basePackageClasses() default {};
}

