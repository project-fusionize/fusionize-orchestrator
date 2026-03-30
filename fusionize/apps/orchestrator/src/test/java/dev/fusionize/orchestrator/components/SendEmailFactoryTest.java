package dev.fusionize.orchestrator.components;

import dev.fusionize.worker.component.annotations.RuntimeComponentDefinition;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SendEmailFactoryTest {

    @Test
    void shouldCreateSendEmailInstance() {
        // setup
        var factory = new SendEmailFactory();

        // expectation
        var instance = factory.create();

        // validation
        assertThat(instance).isNotNull().isInstanceOf(SendEmail.class);
    }

    @Test
    void shouldHaveCorrectAnnotationName() {
        // setup
        var annotation = SendEmailFactory.class.getAnnotation(RuntimeComponentDefinition.class);

        // expectation & validation
        assertThat(annotation).isNotNull();
        assertThat(annotation.name()).isEqualTo("test send email");
    }

    @Test
    void shouldHaveCorrectAnnotationDomain() {
        // setup
        var annotation = SendEmailFactory.class.getAnnotation(RuntimeComponentDefinition.class);

        // expectation & validation
        assertThat(annotation).isNotNull();
        assertThat(annotation.domain()).isEqualTo("test.sendEmail");
    }

    @Test
    void shouldHaveCorrectAnnotationDescription() {
        // setup
        var annotation = SendEmailFactory.class.getAnnotation(RuntimeComponentDefinition.class);

        // expectation & validation
        assertThat(annotation).isNotNull();
        assertThat(annotation.description()).isEqualTo("Send email component");
    }

    @Test
    void shouldHaveCorrectAnnotationType() {
        // setup
        var annotation = SendEmailFactory.class.getAnnotation(RuntimeComponentDefinition.class);

        // expectation & validation
        assertThat(annotation).isNotNull();
        assertThat(annotation.type()).isEqualTo(SendEmail.class);
    }
}
