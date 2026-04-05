package dev.fusionize.workflow.component.local.beans;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ScriptComponentFactoryTest {

    @Test
    void shouldReturnCorrectName() {
        // setup
        var factory = new ScriptComponentFactory();

        // expectation
        var name = factory.getName();

        // validation
        assertThat(name).isEqualTo("script");
    }

    @Test
    void shouldCreateScriptComponent() {
        // setup
        var factory = new ScriptComponentFactory();

        // expectation
        var component = factory.create();

        // validation
        assertThat(component).isInstanceOf(ScriptComponent.class);
    }

    @Test
    void shouldDescribeComponent() {
        // setup
        var factory = new ScriptComponentFactory();

        // expectation
        var description = factory.describe();

        // validation
        assertThat(description).isNotNull();
        assertThat(description.getDomain()).isEqualTo("script");
        assertThat(description.getName()).isEqualTo("script");
        assertThat(description.getDescription()).contains("ScriptComponent");
    }
}
