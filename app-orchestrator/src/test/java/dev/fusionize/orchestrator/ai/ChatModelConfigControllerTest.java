package dev.fusionize.orchestrator.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.fusionize.ai.model.ChatModelConfig;
import dev.fusionize.ai.service.ChatModelManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ChatModelConfigController.class)
@AutoConfigureMockMvc(addFilters = false)
class ChatModelConfigControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ChatModelManager chatModelManager;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void getAll() throws Exception {
        ChatModelConfig config = new ChatModelConfig();
        config.setProvider("openai");
        when(chatModelManager.getAll("")).thenReturn(List.of(config));

        mockMvc.perform(get("/api/ai/config"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.response.status").value(200))
                .andExpect(jsonPath("$.response.message[0].provider").value("openai"));
    }

    @Test
    void getByDomain() throws Exception {
        String domain = "test.domain";
        ChatModelConfig config = new ChatModelConfig();
        config.setProvider("openai");
        when(chatModelManager.getModel(domain)).thenReturn(Optional.of(config));

        mockMvc.perform(get("/api/ai/config/{domain}", domain))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.response.status").value(200))
                .andExpect(jsonPath("$.response.message.provider").value("openai"));
    }

    @Test
    void create() throws Exception {
        ChatModelConfig config = new ChatModelConfig();
        config.setProvider("openai");
        when(chatModelManager.saveModel(any(ChatModelConfig.class))).thenReturn(config);

        mockMvc.perform(post("/api/ai/config")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(config)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.response.status").value(200))
                .andExpect(jsonPath("$.response.message.provider").value("openai"));
    }

    @Test
    void update() throws Exception {
        String domain = "test.domain";
        ChatModelConfig config = new ChatModelConfig();
        config.setProvider("openai");
        config.setId("existing-id");

        when(chatModelManager.getModel(domain)).thenReturn(Optional.of(config));
        when(chatModelManager.saveModel(any(ChatModelConfig.class))).thenReturn(config);

        mockMvc.perform(put("/api/ai/config/{domain}", domain)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(config)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.response.status").value(200))
                .andExpect(jsonPath("$.response.message.provider").value("openai"));
    }

    @Test
    void deleteTest() throws Exception {
        String domain = "test.domain";

        mockMvc.perform(delete("/api/ai/config/{domain}", domain))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.response.status").value(200));
    }
}
