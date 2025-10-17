package dev.fusionize.project;

import dev.fusionize.project.exception.ProjectNotExistException;
import dev.fusionize.project.exception.ProjectValidationException;
import dev.fusionize.user.activity.ActivityType;
import dev.fusionize.user.activity.UserActivity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public record ProjectService(ProjectRepository projectRepository) {
    private static final Logger log = LoggerFactory.getLogger(ProjectService.class);

    public static final String ERR_CODE_NO_PROJECT_NAME = "(p101) Project no name";
    public static final String ERR_CODE_NO_PROJECT = "(p105) Project missing";
    public static final String ERR_CODE_INVALID_NAME = "(p102) Project name invalid";
    public static final String ERR_CODE_INVALID_DOMAIN = "(p103) Project domain invalid";
    public static final String ERR_CODE_EXISTING_DOMAIN = "(p104) Project domain already exist";

    static class RegexTester {
        public static boolean isValidSubDns(String dns){
            if(dns==null) return false;
            return dns.matches("^([a-z0-9]+(-[a-z0-9]+)*)+$");
        }

        public static boolean isValidName(String name){
            if(name==null) return false;
            return name.matches("^([a-zA-Z0-9]+( [a-zA-Z0-9]+)*)+$");
        }
    }

    @Autowired
    public ProjectService {
    }

    public List<Project> findAll(String parentDomain) {
        return projectRepository.findAllByDomainStartsWith(parentDomain + ".");
    }

    public List<Project> findAllByIds(String parentDomain, List<String> ids) {
        return projectRepository.findAllByIdInAndDomainStartsWith(ids, parentDomain + ".");
    }

    public Project search(String param, String parentDomain) throws ProjectNotExistException {
        Project project;
        if (param.startsWith(parentDomain + "."))
            return getByDomain(param);
        try {
            project = getById(param);
        } catch (ProjectNotExistException e) {
            project = getByDomain(parentDomain + "." + param);
        }

        if (project.getDomain().startsWith(parentDomain))
            return project;
        else throw new ProjectNotExistException();

    }

    public Project getById(String id) throws ProjectNotExistException {
        Optional<Project> optionalProject = this.projectRepository.findById(id);
        if (optionalProject.isEmpty())
            throw new ProjectNotExistException();
        else return optionalProject.get();
    }

    public Project getByDomain(String domain) throws ProjectNotExistException {
        Optional<Project> optionalProject = this.projectRepository.findByDomain(domain);
        if (optionalProject.isEmpty())
            throw new ProjectNotExistException();
        else return optionalProject.get();
    }

    public Project create(Project project, String userId , String parentDomain) throws ProjectValidationException {
        project = validationAndSuggestion(project, parentDomain);
        return this.persist(project, UserActivity.builder(userId).type(ActivityType.CREATE).build());
    }

    public Project update(Project project, String userId ) throws ProjectValidationException {
        if (project == null) throw new ProjectValidationException(ERR_CODE_NO_PROJECT);
        if (project.getId() == null)
            throw new ProjectValidationException(ERR_CODE_NO_PROJECT_NAME);
        if (project.getName() == null)
            throw new ProjectValidationException(ERR_CODE_NO_PROJECT_NAME);
        if (!RegexTester.isValidName(project.getName()))
            throw new ProjectValidationException(ERR_CODE_INVALID_NAME);
        return this.persist(project, UserActivity.builder(userId).type(ActivityType.UPDATE).build());
    }

    public void delete(String projectId, String parentDomain) throws ProjectNotExistException {
        Project project = getById(projectId);
        if (!project.getDomain().startsWith(parentDomain + "."))
            throw new ProjectNotExistException();
        drop(project);
    }


    public Project validationAndSuggestion(Project project, String parentDomain) throws ProjectValidationException {
        if (project == null) throw new ProjectValidationException(ERR_CODE_NO_PROJECT);
        if (project.getName() == null)
            throw new ProjectValidationException(ERR_CODE_NO_PROJECT_NAME);
        if (!RegexTester.isValidName(project.getName()))
            throw new ProjectValidationException(ERR_CODE_INVALID_NAME);
        Project.Builder builder = Project.builder(parentDomain).withName(project.getName());
        if (project.getDomain() != null) {
            if (project.getDomain().startsWith(parentDomain + ".")) {
                project.setDomain(project.getDomain().replace(parentDomain + ".", ""));
            }
            if (!RegexTester.isValidSubDns(project.getDomain()))
                throw new ProjectValidationException(ERR_CODE_INVALID_DOMAIN);
            builder.withDomain(project.getDomain());
        }
        if (project.getCover() != null) {
            builder.withCover(project.getCover());
        }
        if (project.getDescription() != null) {
            builder.withDescription(project.getDescription());
        }
        Project newProject = builder.build();
        if (this.projectRepository.findByDomain(newProject.getDomain()).isPresent())
            throw new ProjectValidationException(ERR_CODE_EXISTING_DOMAIN);
        return newProject;
    }

    private void drop(Project project) {
        this.projectRepository.deleteById(project.getId());
    }

    private Project persist(Project project, UserActivity activity){
        if(activity!=null){
            project.pushActivity(activity);
        }
        return this.projectRepository.save(project);
    }
}
