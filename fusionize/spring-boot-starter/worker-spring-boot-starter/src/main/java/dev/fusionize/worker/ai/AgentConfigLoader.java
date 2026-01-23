package dev.fusionize.worker.ai;

import dev.fusionize.ai.model.AgentConfig;
import dev.fusionize.ai.model.descriptor.AgentConfigDescriptor;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class AgentConfigLoader {
    private static final Logger logger = LoggerFactory.getLogger(AgentConfigLoader.class);

    public List<AgentConfig> loadAgentConfigs(String agentConfigDefinitionsDir) throws IOException {
        File agentConfigFolder = new File(agentConfigDefinitionsDir);
        if(!agentConfigFolder.exists() || !agentConfigFolder.isDirectory()){
            logger.error("Worker Agent Config Definitions folder not found:  {}", agentConfigDefinitionsDir);
            return new ArrayList<>();
        }
        List<AgentConfig> agentConfigs = new ArrayList<>();
        logger.info("Scanning for Agent Config Definitions:  {}", Path.of(agentConfigFolder.getAbsolutePath()).normalize());
        FileUtils.listFiles(agentConfigFolder, new String[]{"yml","yaml"}, true)
                .stream().filter(f-> f!=null && f.exists() && f.isFile() && (f.getName().equals("agent.yml") || f.getName().equals("agent.yaml"))).forEach(file -> {
                    try {
                        AgentConfig agentConfig = new AgentConfigDescriptor().fromYamlDescription(Files.readString(file.toPath()));
                        agentConfigs.add(agentConfig);
                    } catch (Exception e) {
                        logger.error("Failed to load agent config: {} -> {}", file.getAbsolutePath(), e.getMessage());
                    }
                });
        return agentConfigs;
    }
}
