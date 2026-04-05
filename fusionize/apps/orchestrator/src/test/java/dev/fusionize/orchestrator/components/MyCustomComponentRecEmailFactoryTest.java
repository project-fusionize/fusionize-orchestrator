package dev.fusionize.orchestrator.components;

import dev.fusionize.orchestrator.EmailBoxService;
import dev.fusionize.worker.component.annotations.RuntimeComponentDefinition;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class MyCustomComponentRecEmailFactoryTest {

    @Mock
    EmailBoxService emailBoxService;

    @Test
    void shouldCreateMyCustomComponentRecEmailInstance() {
        // setup
        var factory = new MyCustomComponentRecEmailFactory(emailBoxService);

        // expectation
        var instance = factory.create();

        // validation
        assertThat(instance).isNotNull().isInstanceOf(MyCustomComponentRecEmail.class);
    }

    @Test
    void shouldHaveCorrectAnnotationName() {
        // setup
        var annotation = MyCustomComponentRecEmailFactory.class.getAnnotation(RuntimeComponentDefinition.class);

        // expectation & validation
        assertThat(annotation).isNotNull();
        assertThat(annotation.name()).isEqualTo("receivedIncomingEmail");
    }

    @Test
    void shouldHaveCorrectAnnotationDescription() {
        // setup
        var annotation = MyCustomComponentRecEmailFactory.class.getAnnotation(RuntimeComponentDefinition.class);

        // expectation & validation
        assertThat(annotation).isNotNull();
        assertThat(annotation.description()).isEqualTo("Received incoming email");
    }

    @Test
    void shouldHaveCorrectAnnotationType() {
        // setup
        var annotation = MyCustomComponentRecEmailFactory.class.getAnnotation(RuntimeComponentDefinition.class);

        // expectation & validation
        assertThat(annotation).isNotNull();
        assertThat(annotation.type()).isEqualTo(MyCustomComponentRecEmail.class);
    }
}
