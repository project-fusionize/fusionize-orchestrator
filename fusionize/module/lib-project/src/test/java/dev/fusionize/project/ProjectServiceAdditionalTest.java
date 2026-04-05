package dev.fusionize.project;

import dev.fusionize.project.exception.ProjectNotExistException;
import dev.fusionize.project.exception.ProjectValidationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProjectServiceAdditionalTest {

    private static final String PARENT_DOMAIN = "parent";
    private static final String USER_ID = "user-1";

    @Mock
    ProjectRepository projectRepository;

    @InjectMocks
    ProjectService projectService;

    private Project buildProject(String name) {
        return Project.builder(PARENT_DOMAIN).withName(name).build();
    }

    private Project buildProjectWithId(String id, String name) {
        var project = buildProject(name);
        project.setId(id);
        return project;
    }

    // ---- findAll ----

    @Test
    void shouldFindAll() {
        // setup
        var expected = List.of(buildProject("Alpha"), buildProject("Beta"));
        when(projectRepository.findAllByDomainStartsWith(PARENT_DOMAIN + ".")).thenReturn(expected);

        // expectation
        var result = projectService.findAll(PARENT_DOMAIN);

        // validation
        assertThat(result).isEqualTo(expected);
        verify(projectRepository).findAllByDomainStartsWith(PARENT_DOMAIN + ".");
    }

    // ---- findAllByIds ----

    @Test
    void shouldFindAllByIds() {
        // setup
        var ids = List.of("id-1", "id-2");
        var expected = List.of(buildProject("Alpha"));
        when(projectRepository.findAllByIdInAndDomainStartsWith(ids, PARENT_DOMAIN + ".")).thenReturn(expected);

        // expectation
        var result = projectService.findAllByIds(PARENT_DOMAIN, ids);

        // validation
        assertThat(result).isEqualTo(expected);
        verify(projectRepository).findAllByIdInAndDomainStartsWith(ids, PARENT_DOMAIN + ".");
    }

    // ---- search ----

    @Test
    void shouldSearchByDomain_whenParamStartsWithParentDomain() throws ProjectNotExistException {
        // setup
        var project = buildProject("Alpha");
        var fullDomain = project.getDomain();
        when(projectRepository.findByDomain(fullDomain)).thenReturn(Optional.of(project));

        // expectation
        var result = projectService.search(fullDomain, PARENT_DOMAIN);

        // validation
        assertThat(result).isEqualTo(project);
    }

    @Test
    void shouldSearchById_whenParamIsId() throws ProjectNotExistException {
        // setup
        var project = buildProjectWithId("some-id", "Alpha");
        when(projectRepository.findById("some-id")).thenReturn(Optional.of(project));

        // expectation
        var result = projectService.search("some-id", PARENT_DOMAIN);

        // validation
        assertThat(result).isEqualTo(project);
    }

    @Test
    void shouldSearchByDomain_whenIdNotFound() throws ProjectNotExistException {
        // setup
        var project = buildProject("Alpha");
        when(projectRepository.findById("unknown-id")).thenReturn(Optional.empty());
        when(projectRepository.findByDomain(PARENT_DOMAIN + ".unknown-id")).thenReturn(Optional.of(project));

        // expectation
        var result = projectService.search("unknown-id", PARENT_DOMAIN);

        // validation
        assertThat(result).isEqualTo(project);
    }

    @Test
    void shouldThrowProjectNotExist_whenSearchDomainDoesNotStartWithParent() {
        // setup
        var project = buildProjectWithId("some-id", "Alpha");
        project.setDomain("other-domain.alpha");
        when(projectRepository.findById("some-id")).thenReturn(Optional.of(project));

        // expectation + validation
        assertThatThrownBy(() -> projectService.search("some-id", PARENT_DOMAIN))
                .isInstanceOf(ProjectNotExistException.class);
    }

    // ---- getById ----

    @Test
    void shouldGetById() throws ProjectNotExistException {
        // setup
        var project = buildProjectWithId("id-1", "Alpha");
        when(projectRepository.findById("id-1")).thenReturn(Optional.of(project));

        // expectation
        var result = projectService.getById("id-1");

        // validation
        assertThat(result).isEqualTo(project);
    }

    @Test
    void shouldThrowProjectNotExist_whenIdNotFound() {
        // setup
        when(projectRepository.findById("missing")).thenReturn(Optional.empty());

        // expectation + validation
        assertThatThrownBy(() -> projectService.getById("missing"))
                .isInstanceOf(ProjectNotExistException.class);
    }

    // ---- getByDomain ----

    @Test
    void shouldGetByDomain() throws ProjectNotExistException {
        // setup
        var project = buildProject("Alpha");
        var domain = project.getDomain();
        when(projectRepository.findByDomain(domain)).thenReturn(Optional.of(project));

        // expectation
        var result = projectService.getByDomain(domain);

        // validation
        assertThat(result).isEqualTo(project);
    }

    @Test
    void shouldThrowProjectNotExist_whenDomainNotFound() {
        // setup
        when(projectRepository.findByDomain("parent.missing")).thenReturn(Optional.empty());

        // expectation + validation
        assertThatThrownBy(() -> projectService.getByDomain("parent.missing"))
                .isInstanceOf(ProjectNotExistException.class);
    }

    // ---- update ----

    @Test
    void shouldUpdate_successfully() throws ProjectValidationException {
        // setup
        var project = buildProjectWithId("id-1", "Valid Name");
        when(projectRepository.save(any(Project.class))).thenReturn(project);

        // expectation
        var result = projectService.update(project, USER_ID);

        // validation
        assertThat(result).isEqualTo(project);
        verify(projectRepository).save(any(Project.class));
    }

    @Test
    void shouldThrowValidation_whenUpdateNullProject() {
        // setup
        Project project = null;

        // expectation + validation
        assertThatThrownBy(() -> projectService.update(project, USER_ID))
                .isInstanceOf(ProjectValidationException.class)
                .hasMessage(ProjectService.ERR_CODE_NO_PROJECT);
    }

    @Test
    void shouldThrowValidation_whenUpdateNullId() {
        // setup
        var project = buildProject("Valid Name");
        project.setId(null);

        // expectation + validation
        assertThatThrownBy(() -> projectService.update(project, USER_ID))
                .isInstanceOf(ProjectValidationException.class)
                .hasMessage(ProjectService.ERR_CODE_NO_PROJECT_NAME);
    }

    @Test
    void shouldThrowValidation_whenUpdateNullName() {
        // setup
        var project = buildProjectWithId("id-1", "Temp");
        project.setName(null);

        // expectation + validation
        assertThatThrownBy(() -> projectService.update(project, USER_ID))
                .isInstanceOf(ProjectValidationException.class)
                .hasMessage(ProjectService.ERR_CODE_NO_PROJECT_NAME);
    }

    @Test
    void shouldThrowValidation_whenUpdateInvalidName() {
        // setup
        var project = buildProjectWithId("id-1", "Temp");
        project.setName("invalid$name!");

        // expectation + validation
        assertThatThrownBy(() -> projectService.update(project, USER_ID))
                .isInstanceOf(ProjectValidationException.class)
                .hasMessage(ProjectService.ERR_CODE_INVALID_NAME);
    }

    // ---- delete ----

    @Test
    void shouldDelete_successfully() throws ProjectNotExistException {
        // setup
        var project = buildProjectWithId("id-1", "Alpha");
        when(projectRepository.findById("id-1")).thenReturn(Optional.of(project));

        // expectation
        projectService.delete("id-1", PARENT_DOMAIN);

        // validation
        verify(projectRepository).deleteById("id-1");
    }

    @Test
    void shouldThrowNotExist_whenDeleteFromWrongDomain() {
        // setup
        var project = buildProjectWithId("id-1", "Alpha");
        project.setDomain("other-domain.alpha");
        when(projectRepository.findById("id-1")).thenReturn(Optional.of(project));

        // expectation + validation
        assertThatThrownBy(() -> projectService.delete("id-1", PARENT_DOMAIN))
                .isInstanceOf(ProjectNotExistException.class);
    }

    // ---- validationAndSuggestion ----

    @Test
    void shouldValidate_throwsWhenNullProject() {
        // setup
        Project project = null;

        // expectation + validation
        assertThatThrownBy(() -> projectService.validationAndSuggestion(project, PARENT_DOMAIN))
                .isInstanceOf(ProjectValidationException.class)
                .hasMessage(ProjectService.ERR_CODE_NO_PROJECT);
    }

    @Test
    void shouldValidate_throwsWhenNullName() {
        // setup
        var project = buildProject("Temp");
        project.setName(null);

        // expectation + validation
        assertThatThrownBy(() -> projectService.validationAndSuggestion(project, PARENT_DOMAIN))
                .isInstanceOf(ProjectValidationException.class)
                .hasMessage(ProjectService.ERR_CODE_NO_PROJECT_NAME);
    }

    @Test
    void shouldValidate_throwsWhenInvalidName() {
        // setup
        var project = buildProject("Temp");
        project.setName("bad$name!");

        // expectation + validation
        assertThatThrownBy(() -> projectService.validationAndSuggestion(project, PARENT_DOMAIN))
                .isInstanceOf(ProjectValidationException.class)
                .hasMessage(ProjectService.ERR_CODE_INVALID_NAME);
    }

    @Test
    void shouldValidate_throwsWhenInvalidDomain() {
        // setup
        var project = buildProject("Valid Name");
        project.setDomain("INVALID DOMAIN!");

        // expectation + validation
        assertThatThrownBy(() -> projectService.validationAndSuggestion(project, PARENT_DOMAIN))
                .isInstanceOf(ProjectValidationException.class)
                .hasMessage(ProjectService.ERR_CODE_INVALID_DOMAIN);
    }

    @Test
    void shouldValidate_throwsWhenDomainExists() {
        // setup
        var project = buildProject("Valid Name");
        when(projectRepository.findByDomain(project.getDomain())).thenReturn(Optional.of(project));

        // expectation + validation
        assertThatThrownBy(() -> projectService.validationAndSuggestion(project, PARENT_DOMAIN))
                .isInstanceOf(ProjectValidationException.class)
                .hasMessage(ProjectService.ERR_CODE_EXISTING_DOMAIN);
    }
}
