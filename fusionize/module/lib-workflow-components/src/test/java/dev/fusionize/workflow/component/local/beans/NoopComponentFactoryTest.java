package dev.fusionize.workflow.component.local.beans;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NoopComponentFactoryTest {

    @Test
    void shouldReturnCorrectName() {
        // setup
        var factory = new NoopComponentFactory();

        // expectation
        var name = factory.getName();

        // validation
        assertThat(name).isEqualTo("noop");
    }

    @Test
    void shouldCreateNoopComponent() {
        // setup
        var factory = new NoopComponentFactory();

        // expectation
        var component = factory.create();

        // validation
        assertThat(component).isInstanceOf(NoopComponent.class);
    }

    @Test
    void shouldDescribeComponent() {
        // setup
        var factory = new NoopComponentFactory();

        // expectation
        var description = factory.describe();

        // validation
        assertThat(description).isNotNull();
        assertThat(description.getDomain()).isEqualTo("noop");
    }
}
