package dev.fusionize.worker.ai;

import dev.fusionize.ai.model.ChatModelConfig;
import dev.fusionize.ai.model.descriptor.ChatModelConfigDescriptor;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class ChatModelConfigLoader {
    private static final Logger logger = LoggerFactory.getLogger(ChatModelConfigLoader.class);

    public List<ChatModelConfig> loadChatModelConfigs(String chatModelConfigDefinitionsDir) throws IOException {
        File chatModelConfigFolder = new File(chatModelConfigDefinitionsDir);
        if(!chatModelConfigFolder.exists() || !chatModelConfigFolder.isDirectory()){
            logger.error("Worker Chat Model Config Definitions folder not found:  {}", chatModelConfigDefinitionsDir);
            return new ArrayList<>();
        }
        List<ChatModelConfig> chatModelConfigs = new ArrayList<>();
        logger.info("Scanning for Chat Model Config Definitions:  {}", Path.of(chatModelConfigFolder.getAbsolutePath()).normalize());
        FileUtils.listFiles(chatModelConfigFolder, new String[]{"yml","yaml"}, true)
                .stream().filter(f-> f!=null && f.exists() && f.isFile() && (f.getName().equals("chat-model.yml") || f.getName().equals("chat-model.yaml"))).forEach(file -> {
                    try {
                        ChatModelConfig chatModelConfig = new ChatModelConfigDescriptor().fromYamlDescription(Files.readString(file.toPath()));
                        chatModelConfigs.add(chatModelConfig);
                    } catch (Exception e) {
                        logger.error("Failed to load chat model config: {} -> {}", file.getAbsolutePath(), e.getMessage());
                    }
                });
        return chatModelConfigs;
    }
}
