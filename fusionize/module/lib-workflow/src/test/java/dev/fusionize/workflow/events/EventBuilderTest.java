package dev.fusionize.workflow.events;

import dev.fusionize.workflow.events.orchestration.ActivationRequestEvent;
import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

class EventBuilderTest {

    @Test
    void shouldSetEventId() {
        // setup
        var eventId = "custom-event-id";

        // expectation
        var event = ActivationRequestEvent.builder(this)
                .eventId(eventId)
                .build();

        // validation
        assertThat(event.getEventId()).isEqualTo(eventId);
    }

    @Test
    void shouldSetCorrelationId() {
        // setup
        var correlationId = "corr-123";

        // expectation
        var event = ActivationRequestEvent.builder(this)
                .correlationId(correlationId)
                .build();

        // validation
        assertThat(event.getCorrelationId()).isEqualTo(correlationId);
    }

    @Test
    void shouldSetCausationId() {
        // setup
        var causationId = "cause-456";

        // expectation
        var event = ActivationRequestEvent.builder(this)
                .causationId(causationId)
                .build();

        // validation
        assertThat(event.getCausationId()).isEqualTo(causationId);
    }

    @Test
    void shouldGenerateDefaultEventId() {
        // setup
        // no eventId set on builder

        // expectation
        var event = ActivationRequestEvent.builder(this).build();

        // validation
        assertThat(event.getEventId()).isNotNull();
        assertThat(event.getEventId()).startsWith("EVNT");
    }

    @Test
    void shouldGenerateDefaultCorrelationId() {
        // setup
        // no correlationId set on builder

        // expectation
        var event = ActivationRequestEvent.builder(this).build();

        // validation
        assertThat(event.getCorrelationId()).isNotNull();
        assertThat(event.getCorrelationId()).isNotEmpty();
    }

    @Test
    void shouldSetEventClass() {
        // setup
        // eventClass is set automatically from the class passed to builder

        // expectation
        var event = ActivationRequestEvent.builder(this).build();

        // validation
        assertThat(event.getEventClass()).isEqualTo(ActivationRequestEvent.class.getCanonicalName());
    }

    @Test
    void shouldSetSource() {
        // setup
        var source = this;

        // expectation
        var event = ActivationRequestEvent.builder(source).build();

        // validation
        assertThat(event.getSource()).isEqualTo(this.getClass().getCanonicalName());
    }
}
