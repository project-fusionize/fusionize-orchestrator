package dev.fusionize.worker.workflow;
import dev.fusionize.workflow.Workflow;
import dev.fusionize.workflow.descriptor.WorkflowDescriptor;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;


public class WorkflowLoader {
    private static final Logger logger = LoggerFactory.getLogger(WorkflowLoader.class);

    public List<Workflow> loadWorkflows(String workflowDefinitionsDir) throws IOException {
        File workflowFolder = new File(workflowDefinitionsDir);
        if(!workflowFolder.exists() || !workflowFolder.isDirectory()){
            logger.error("Worker Workflow Definitions folder not found:  {}", workflowDefinitionsDir);
            return new ArrayList<>();
        }
        List<Workflow> workflows = new ArrayList<>();
        logger.info("Scanning for Workflow Definitions:  {}", Path.of(workflowFolder.getAbsolutePath()).normalize());
        FileUtils.listFiles(workflowFolder, new String[]{"workflow.yml","workflow.yaml"}, true)
                .stream().filter(f-> f!=null && f.exists() && f.isFile()).forEach(file -> {
                    try {
                        Workflow workflow = new WorkflowDescriptor().fromYamlDescription(Files.readString(file.toPath()));
                        workflows.add(workflow);
                    } catch (Exception e) {
                        logger.error("Failed to load workflow: {} -> {}", file.getAbsolutePath(), e.getMessage());
                    }
                });
        return workflows;
    }
}
