package dev.fusionize.orchestrator.storage;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.fusionize.ai.DocumentExtractor;
import dev.fusionize.orchestrator.EmailBoxService;
import dev.fusionize.orchestrator.WebhookService;
import dev.fusionize.storage.StorageConfig;
import org.springframework.ai.chat.client.ChatClient;
import dev.fusionize.storage.StorageConfigManager;
import dev.fusionize.storage.exception.StorageConnectionException;
import dev.fusionize.storage.exception.StorageNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(StorageConfigController.class)
@AutoConfigureMockMvc(addFilters = false)
class StorageConfigControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StorageConfigManager storageConfigManager;

    @MockitoBean
    private EmailBoxService emailBoxService;

    @MockitoBean
    private WebhookService webhookService;

    @MockitoBean
    private DocumentExtractor documentExtractor;

    @MockitoBean
    private ChatClient.Builder chatClientBuilder;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void getAll() throws Exception {
        StorageConfig config = new StorageConfig();
        config.setProvider(dev.fusionize.storage.StorageProvider.AWS_S3);
        when(storageConfigManager.getAll("")).thenReturn(List.of(config));

        mockMvc.perform(get("/api/1.0/storage-config"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.response.status").value(200))
                .andExpect(jsonPath("$.response.message[0].provider").value("AWS_S3"));
    }

    @Test
    void getByDomain() throws Exception {
        String domain = "test.domain";
        StorageConfig config = new StorageConfig();
        config.setProvider(dev.fusionize.storage.StorageProvider.AWS_S3);
        when(storageConfigManager.getConfig(domain)).thenReturn(Optional.of(config));

        mockMvc.perform(get("/api/1.0/storage-config/{domain}", domain))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.response.status").value(200))
                .andExpect(jsonPath("$.response.message.provider").value("AWS_S3"));
    }

    @Test
    void create() throws Exception {
        StorageConfig config = new StorageConfig();
        config.setProvider(dev.fusionize.storage.StorageProvider.AWS_S3);
        when(storageConfigManager.saveConfig(any(StorageConfig.class))).thenReturn(config);

        mockMvc.perform(post("/api/1.0/storage-config")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(config)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.response.status").value(200))
                .andExpect(jsonPath("$.response.message.provider").value("AWS_S3"));
    }

    @Test
    void create_InvalidConfig() throws Exception {
        StorageConfig config = new StorageConfig();
        when(storageConfigManager.saveConfig(any(StorageConfig.class)))
                .thenThrow(new IllegalArgumentException("Invalid config"));

        mockMvc.perform(post("/api/1.0/storage-config")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(config)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.httpStatus").value(400));
    }

    @Test
    void testConnection_Success() throws Exception {
        StorageConfig config = new StorageConfig();
        config.setProvider(dev.fusionize.storage.StorageProvider.AWS_S3);

        mockMvc.perform(post("/api/1.0/storage-config/test-connection")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(config)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.response.status").value(200));
    }

    @Test
    void testConnection_Failure() throws Exception {
        StorageConfig config = new StorageConfig();
        doThrow(new StorageConnectionException("Connection failed", null))
                .when(storageConfigManager).testConnection(any(StorageConfig.class));

        mockMvc.perform(post("/api/1.0/storage-config/test-connection")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(config)))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.httpStatus").value(502));
    }

    @Test
    void update() throws Exception {
        String domain = "test.domain";
        StorageConfig config = new StorageConfig();
        config.setProvider(dev.fusionize.storage.StorageProvider.AWS_S3);
        config.setId("existing-id");

        when(storageConfigManager.getConfig(domain)).thenReturn(Optional.of(config));
        when(storageConfigManager.saveConfig(any(StorageConfig.class))).thenReturn(config);

        mockMvc.perform(put("/api/1.0/storage-config/{domain}", domain)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(config)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.response.status").value(200))
                .andExpect(jsonPath("$.response.message.provider").value("AWS_S3"));
    }

    @Test
    void deleteTest() throws Exception {
        String domain = "test.domain";

        mockMvc.perform(delete("/api/1.0/storage-config/{domain}", domain))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.response.status").value(200));
    }
}
