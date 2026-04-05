package dev.fusionize.project;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ProjectPermissionTest {

    @Test
    void shouldCheckPermission_ownerCanDoAll() {
        // setup
        var owner = ProjectPermission.Access.OWNER;

        // expectation
        var canOwner = owner.check(ProjectPermission.Access.OWNER);
        var canAdmin = owner.check(ProjectPermission.Access.ADMIN);
        var canDeveloper = owner.check(ProjectPermission.Access.DEVELOPER);
        var canReadonly = owner.check(ProjectPermission.Access.READONLY);

        // validation
        assertThat(canOwner).isTrue();
        assertThat(canAdmin).isTrue();
        assertThat(canDeveloper).isTrue();
        assertThat(canReadonly).isTrue();
    }

    @Test
    void shouldCheckPermission_readonlyCantAdmin() {
        // setup
        var readonly = ProjectPermission.Access.READONLY;

        // expectation
        var canOwner = readonly.check(ProjectPermission.Access.OWNER);
        var canAdmin = readonly.check(ProjectPermission.Access.ADMIN);
        var canDeveloper = readonly.check(ProjectPermission.Access.DEVELOPER);
        var canReadonly = readonly.check(ProjectPermission.Access.READONLY);

        // validation
        assertThat(canOwner).isFalse();
        assertThat(canAdmin).isFalse();
        assertThat(canDeveloper).isFalse();
        assertThat(canReadonly).isTrue();
    }

    @Test
    void shouldLookupByName() {
        // setup
        // expectation
        var owner = ProjectPermission.Access.get("OWNER");
        var admin = ProjectPermission.Access.get("ADMIN");
        var developer = ProjectPermission.Access.get("DEVELOPER");
        var readonly = ProjectPermission.Access.get("READONLY");

        // validation
        assertThat(owner).isEqualTo(ProjectPermission.Access.OWNER);
        assertThat(admin).isEqualTo(ProjectPermission.Access.ADMIN);
        assertThat(developer).isEqualTo(ProjectPermission.Access.DEVELOPER);
        assertThat(readonly).isEqualTo(ProjectPermission.Access.READONLY);
    }

    @Test
    void shouldReturnNullForUnknownName() {
        // setup
        // expectation
        var result = ProjectPermission.Access.get("NONEXISTENT");

        // validation
        assertThat(result).isNull();
    }

    @Test
    void shouldSetAndGetFields() {
        // setup
        var permission = new ProjectPermission();

        // expectation
        permission.setId("proj-123");
        permission.setAccess(ProjectPermission.Access.DEVELOPER);

        // validation
        assertThat(permission.getId()).isEqualTo("proj-123");
        assertThat(permission.getAccess()).isEqualTo(ProjectPermission.Access.DEVELOPER);
    }
}
