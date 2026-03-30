package dev.fusionize.workflow.component;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class WorkflowComponentTest {

    @Test
    void shouldBuildWithGeneratedComponentId() {
        // setup
        var builder = WorkflowComponent.builder("test-domain");

        // expectation
        var component = builder.build();

        // validation
        assertThat(component.getComponentId()).startsWith("COMP");
    }

    @Test
    void shouldBuildWithExplicitComponentId() {
        // setup
        var builder = WorkflowComponent.builder("test-domain")
                .withComponentId("my-id");

        // expectation
        var component = builder.build();

        // validation
        assertThat(component.getComponentId()).isEqualTo("my-id");
    }

    @Test
    void shouldBuildWithDescription() {
        // setup
        var builder = WorkflowComponent.builder("test-domain")
                .withDescription("desc");

        // expectation
        var component = builder.build();

        // validation
        assertThat(component.getDescription()).isEqualTo("desc");
    }

    @Test
    void shouldBuildWithSingleActor() {
        // setup
        var builder = WorkflowComponent.builder("test-domain")
                .withActor(Actor.SYSTEM);

        // expectation
        var component = builder.build();

        // validation
        assertThat(component.getActors()).containsExactly(Actor.SYSTEM);
    }

    @Test
    void shouldBuildWithMultipleActors() {
        // setup
        var builder = WorkflowComponent.builder("test-domain")
                .withActors(Set.of(Actor.SYSTEM, Actor.AI));

        // expectation
        var component = builder.build();

        // validation
        assertThat(component.getActors()).containsExactlyInAnyOrder(Actor.SYSTEM, Actor.AI);
    }

    @Test
    void shouldBuildWithNameAndDomain() {
        // setup
        var builder = WorkflowComponent.builder("parent")
                .withName("test")
                .withDomain("child");

        // expectation
        var component = builder.build();

        // validation
        assertThat(component.getName()).isEqualTo("test");
        assertThat(component.getDomain()).isEqualTo("parent.child");
    }

    @Test
    void shouldMergeFrom_updatesName() {
        // setup
        var target = WorkflowComponent.builder("domain")
                .withName("original")
                .build();
        var source = WorkflowComponent.builder("domain")
                .withName("updated")
                .build();

        // expectation
        target.mergeFrom(source);

        // validation
        assertThat(target.getName()).isEqualTo("updated");
    }

    @Test
    void shouldMergeFrom_updatesDescription() {
        // setup
        var target = WorkflowComponent.builder("domain")
                .withDescription("original")
                .build();
        var source = WorkflowComponent.builder("domain")
                .withDescription("updated")
                .build();

        // expectation
        target.mergeFrom(source);

        // validation
        assertThat(target.getDescription()).isEqualTo("updated");
    }

    @Test
    void shouldMergeFrom_updatesActors() {
        // setup
        var target = WorkflowComponent.builder("domain")
                .withActor(Actor.SYSTEM)
                .build();
        var source = WorkflowComponent.builder("domain")
                .withActors(Set.of(Actor.AI, Actor.HUMAN))
                .build();

        // expectation
        target.mergeFrom(source);

        // validation
        assertThat(target.getActors()).containsExactlyInAnyOrder(Actor.AI, Actor.HUMAN);
    }

    @Test
    void shouldMergeFrom_doesNotOverwriteWithNull() {
        // setup
        var target = WorkflowComponent.builder("domain")
                .withName("original")
                .build();
        var source = WorkflowComponent.builder("domain")
                .build();

        // expectation
        target.mergeFrom(source);

        // validation
        assertThat(target.getName()).isEqualTo("original");
    }

    @Test
    void shouldMergeFrom_doesNotOverwriteWithEmptyActors() {
        // setup
        var target = WorkflowComponent.builder("domain")
                .withActor(Actor.SYSTEM)
                .build();
        var source = WorkflowComponent.builder("domain")
                .build();

        // expectation
        target.mergeFrom(source);

        // validation
        assertThat(target.getActors()).containsExactly(Actor.SYSTEM);
    }

    @Test
    void shouldMergeFrom_ignoresNullSource() {
        // setup
        var target = WorkflowComponent.builder("domain")
                .withName("original")
                .build();

        // expectation
        target.mergeFrom(null);

        // validation
        assertThat(target.getName()).isEqualTo("original");
    }
}
