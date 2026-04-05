package dev.fusionize.workflow.component.local.beans;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ForkComponentFactoryTest {

    @Test
    void shouldReturnCorrectName() {
        // setup
        var factory = new ForkComponentFactory();

        // expectation
        var name = factory.getName();

        // validation
        assertThat(name).isEqualTo("fork");
    }

    @Test
    void shouldCreateForkComponent() {
        // setup
        var factory = new ForkComponentFactory();

        // expectation
        var component = factory.create();

        // validation
        assertThat(component).isInstanceOf(ForkComponent.class);
    }

    @Test
    void shouldDescribeComponent() {
        // setup
        var factory = new ForkComponentFactory();

        // expectation
        var description = factory.describe();

        // validation
        assertThat(description).isNotNull();
        assertThat(description.getDomain()).isEqualTo("fork");
        assertThat(description.getName()).isEqualTo("fork");
        assertThat(description.getDescription()).contains("ForkComponent");
    }
}
