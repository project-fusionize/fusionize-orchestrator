package dev.fusionize.workflow.component.local.beans;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DelayComponentFactoryTest {

    private final DelayComponentFactory factory = new DelayComponentFactory();

    @Test
    void shouldReturnCorrectName() {
        // setup
        var factory = new DelayComponentFactory();

        // expectation
        var name = factory.getName();

        // validation
        assertThat(name).isEqualTo("delay");
    }

    @Test
    void shouldCreateDelayComponent() {
        // setup
        var factory = new DelayComponentFactory();

        // expectation
        var component = factory.create();

        // validation
        assertThat(component).isInstanceOf(DelayComponent.class);
    }

    @Test
    void shouldDescribeComponent() {
        // setup
        var factory = new DelayComponentFactory();

        // expectation
        var description = factory.describe();

        // validation
        assertThat(description).isNotNull();
        assertThat(description.getDomain()).isEqualTo("delay");
    }
}
