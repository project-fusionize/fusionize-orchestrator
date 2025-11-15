package dev.fusionize.worker.component.annotations;

import dev.fusionize.workflow.WorkflowNodeType;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RuntimeComponentDefinition {
    String value() default "";
    String name() default "";
    String description() default "";
    Class<?> type();
    WorkflowNodeType compatible() default WorkflowNodeType.TASK;
}
