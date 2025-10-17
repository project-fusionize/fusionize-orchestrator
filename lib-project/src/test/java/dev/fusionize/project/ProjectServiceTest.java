package dev.fusionize.project;

import dev.fusionize.common.test.TestMongoConfig;
import dev.fusionize.common.test.TestMongoConversionConfig;
import dev.fusionize.common.utility.KeyUtil;
import dev.fusionize.project.exception.ProjectNotExistException;
import dev.fusionize.project.exception.ProjectValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;


@DataMongoTest()
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = { TestMongoConfig.class, TestMongoConversionConfig.class})
@ActiveProfiles("ut")
class ProjectServiceTest {
    private static final String A_PARENT_DOMAIN = "test-parent-a";
    private static final String B_PARENT_DOMAIN = "test-parent-b";


    @Autowired
    ProjectRepository projectRepository;

    ProjectService projectService;

    String userId = "123";

    @BeforeEach
    void setUp() {
        projectRepository.deleteAll();
        projectService = new ProjectService(projectRepository);
    }


    private Project getRandomProject(String parentDomain, String namePrefix, String description) {
        return Project.builder(parentDomain)
                .withName(namePrefix + " " + KeyUtil.getRandomAlphabeticalKey(4))
                .withDescription(description)
                .build();
    }

    @Test
    public void findAll() throws ProjectValidationException {
        Project project = getRandomProject(B_PARENT_DOMAIN, "Book Store", null);
        project = projectService.create(project, userId, B_PARENT_DOMAIN);

        project = getRandomProject(A_PARENT_DOMAIN, "Bike Store", "com.bs");
        project = projectService.create(project, userId, A_PARENT_DOMAIN);

        project = getRandomProject(A_PARENT_DOMAIN, "Servicing", null);
        project = projectService.create(project, userId, A_PARENT_DOMAIN);

        String servicingDomain = project.getDomain();


        project = getRandomProject(servicingDomain, "Car Shop", null);
        project = projectService.create(project, userId, servicingDomain);

        project = getRandomProject(servicingDomain, "Insurance", "com.b.insurance");
        project = projectService.create(project, userId, servicingDomain);

        List<Project> aProjects = projectService.findAll(A_PARENT_DOMAIN);
        System.out.println(aProjects);
        List<Project> bProjects = projectService.findAll(B_PARENT_DOMAIN);
        System.out.println(bProjects);

        assertEquals(4, aProjects.size());
        assertEquals(1, bProjects.size());
    }

    @Test
    public void getByDomain() throws ProjectValidationException, ProjectNotExistException {
        Project Expectedproject = getRandomProject(B_PARENT_DOMAIN, "Book Store", "Test");
        Expectedproject = projectService.create(Expectedproject, userId, B_PARENT_DOMAIN);
        Project project = projectService.getByDomain(Expectedproject.getDomain());
        assertEquals(project.getId(), Expectedproject.getId());
        assertEquals(project.getName(), Expectedproject.getName());
        assertEquals(project.getDomain(), Expectedproject.getDomain());
    }

    @Test
    public void create() {
        Project validProject = getRandomProject(A_PARENT_DOMAIN, "Book Store", null);
        try {
            Project createdProject = projectService.create(validProject, userId, A_PARENT_DOMAIN);
            assertNotNull(createdProject.getId());
            assertTrue(createdProject.getDomain().startsWith(A_PARENT_DOMAIN + '.'));
            assertEquals(validProject.getName(), createdProject.getName());
            assertEquals(validProject.getDescription(), createdProject.getDescription());
            assertNotNull(createdProject.getKey());
            assertEquals(32 , createdProject.getKey().length());
            System.out.println(createdProject.getKey());

        } catch (ProjectValidationException e) {
            fail(e.getMessage());
        }

        try {
            Project createdProject = projectService.create(validProject, userId, A_PARENT_DOMAIN);
            fail();
        } catch (ProjectValidationException e) {
            assertEquals(ProjectService.ERR_CODE_EXISTING_DOMAIN, e.getMessage());
        }

        validProject = getRandomProject(B_PARENT_DOMAIN, "Insurance", "com insurance");
        try {
            Project createdProject = projectService.create(validProject, userId, B_PARENT_DOMAIN);
            assertNotNull(createdProject.getId());
            assertTrue(createdProject.getDomain().startsWith(B_PARENT_DOMAIN + '.'));
            assertEquals(validProject.getName(), createdProject.getName());
            assertEquals("com insurance", createdProject.getDescription());
        } catch (ProjectValidationException e) {
            fail(e.getMessage());

        }

        Project invalidProject = getRandomProject(A_PARENT_DOMAIN, "Bo00k$Store", null);
        try {
            Project createdProject = projectService.create(invalidProject, userId, A_PARENT_DOMAIN);
            fail();
        } catch (ProjectValidationException e) {
            assertEquals(ProjectService.ERR_CODE_INVALID_NAME, e.getMessage());

        }

        invalidProject = getRandomProject(A_PARENT_DOMAIN, "Flight Booker", null);
        invalidProject.setDomain("invalid Domain");
        try {
            Project createdProject = projectService.create(invalidProject, userId, A_PARENT_DOMAIN);
            fail();
        } catch (ProjectValidationException e) {
            assertEquals(ProjectService.ERR_CODE_INVALID_DOMAIN, e.getMessage());
        }

    }

    @Test
    public void search() throws ProjectValidationException, ProjectNotExistException {
        Project Expectedproject = getRandomProject(B_PARENT_DOMAIN, "Book Store", "Test");
        Expectedproject = projectService.create(Expectedproject, userId, B_PARENT_DOMAIN);
        Project project = projectService.search(Expectedproject.getId(), B_PARENT_DOMAIN);
        assertEquals(project.getId(), Expectedproject.getId());
        assertEquals(project.getName(), Expectedproject.getName());
        assertEquals(project.getDomain(), Expectedproject.getDomain());
    }
}