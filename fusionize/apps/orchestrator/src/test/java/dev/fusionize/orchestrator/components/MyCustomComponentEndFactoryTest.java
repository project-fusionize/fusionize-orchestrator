package dev.fusionize.orchestrator.components;

import dev.fusionize.worker.component.annotations.RuntimeComponentDefinition;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MyCustomComponentEndFactoryTest {

    @Test
    void shouldCreateMyCustomComponentEndInstance() {
        // setup
        var factory = new MyCustomComponentEndFactory();

        // expectation
        var instance = factory.create();

        // validation
        assertThat(instance).isNotNull().isInstanceOf(MyCustomComponentEnd.class);
    }

    @Test
    void shouldHaveCorrectAnnotationName() {
        // setup
        var annotation = MyCustomComponentEndFactory.class.getAnnotation(RuntimeComponentDefinition.class);

        // expectation & validation
        assertThat(annotation).isNotNull();
        assertThat(annotation.name()).isEqualTo("end");
    }

    @Test
    void shouldHaveCorrectAnnotationDescription() {
        // setup
        var annotation = MyCustomComponentEndFactory.class.getAnnotation(RuntimeComponentDefinition.class);

        // expectation & validation
        assertThat(annotation).isNotNull();
        assertThat(annotation.description()).isEqualTo("End component");
    }

    @Test
    void shouldHaveCorrectAnnotationType() {
        // setup
        var annotation = MyCustomComponentEndFactory.class.getAnnotation(RuntimeComponentDefinition.class);

        // expectation & validation
        assertThat(annotation).isNotNull();
        assertThat(annotation.type()).isEqualTo(MyCustomComponentEnd.class);
    }
}
