package dev.fusionize.project;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ProjectTest {

    @Test
    void shouldBuildWithDescription() {
        // setup
        var parentDomain = "org";

        // expectation
        var project = Project.builder(parentDomain)
                .withDescription("A test project description")
                .withName("Test Project")
                .build();

        // validation
        assertThat(project.getDescription()).isEqualTo("A test project description");
    }

    @Test
    void shouldBuildWithNameAndDomain() {
        // setup
        var parentDomain = "org";

        // expectation
        var project = Project.builder(parentDomain)
                .withName("My Project")
                .withDescription("desc")
                .build();

        // validation
        assertThat(project.getName()).isEqualTo("My Project");
        assertThat(project.getDomain()).isNotNull();
        assertThat(project.getDomain()).startsWith("org.");
    }

    @Test
    void shouldBeEqual_whenSameFields() {
        // setup
        var project1 = Project.builder("org")
                .withName("Same")
                .withDomain("same-domain")
                .withDescription("desc")
                .withKey("key1")
                .build();

        var project2 = Project.builder("org")
                .withName("Same")
                .withDomain("same-domain")
                .withDescription("desc")
                .withKey("key1")
                .build();

        // expectation
        // validation
        assertThat(project1).isEqualTo(project2);
        assertThat(project1.hashCode()).isEqualTo(project2.hashCode());
    }

    @Test
    void shouldReturnToString() {
        // setup
        var project = Project.builder("org")
                .withName("ToString Project")
                .withDescription("toString desc")
                .build();

        // expectation
        var result = project.toString();

        // validation
        assertThat(result).contains("Project{");
        assertThat(result).contains("toString desc");
    }

    @Test
    void shouldSetAndGetFields() {
        // setup
        var project = Project.builder("org")
                .withName("Initial")
                .build();

        // expectation
        project.setId("proj-id-123");
        project.setDescription("updated description");

        // validation
        assertThat(project.getId()).isEqualTo("proj-id-123");
        assertThat(project.getDescription()).isEqualTo("updated description");
    }
}
