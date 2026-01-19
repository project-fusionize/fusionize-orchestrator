package dev.fusionize.orchestrator.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.fusionize.ai.exception.AgentConfigDomainAlreadyExistsException;
import dev.fusionize.ai.exception.AgentConfigNotFoundException;
import dev.fusionize.ai.exception.InvalidAgentConfigException;
import dev.fusionize.ai.model.AgentConfig;
import dev.fusionize.ai.service.AgentConfigManager;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
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

@WebMvcTest(AgentConfigController.class)
@AutoConfigureMockMvc(addFilters = false)
class AgentConfigControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AgentConfigManager agentConfigManager;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void getAll() throws Exception {
        AgentConfig config = new AgentConfig();
        config.setDomain("test.agent");
        when(agentConfigManager.getAll("")).thenReturn(List.of(config));

        mockMvc.perform(get("/api/1.0/agent-config"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.response.status").value(200))
                .andExpect(jsonPath("$.response.message[0].domain").value("test.agent"));
    }

    @Test
    void getByDomain_Success() throws Exception {
        String domain = "test.agent";
        AgentConfig config = new AgentConfig();
        config.setDomain(domain);
        when(agentConfigManager.getConfig(domain)).thenReturn(Optional.of(config));

        mockMvc.perform(get("/api/1.0/agent-config/{domain}", domain))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.response.status").value(200))
                .andExpect(jsonPath("$.response.message.domain").value(domain));
    }

    @Test
    void getByDomain_NotFound() throws Exception {
        String domain = "unknown.agent";
        when(agentConfigManager.getConfig(domain)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/1.0/agent-config/{domain}", domain))
                .andExpect(status().isNotFound()) // Assumes AgentConfigNotFoundException maps to 404 via Advisor
                .andExpect(jsonPath("$.httpStatus").value(404));
    }

    @Test
    void create_Success() throws Exception {
        AgentConfig config = new AgentConfig();
        config.setDomain("test.agent");
        when(agentConfigManager.createConfig(any(AgentConfig.class))).thenReturn(config);

        mockMvc.perform(post("/api/1.0/agent-config")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(config)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.response.status").value(200));
    }

    @Test
    void create_InvalidConfig() throws Exception {
        AgentConfig config = new AgentConfig();
        when(agentConfigManager.createConfig(any(AgentConfig.class)))
                .thenThrow(new InvalidAgentConfigException("Invalid config"));

        mockMvc.perform(post("/api/1.0/agent-config")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(config)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.httpStatus").value(400));
    }

    @Test
    void create_Conflict() throws Exception {
        AgentConfig config = new AgentConfig();
        when(agentConfigManager.createConfig(any(AgentConfig.class)))
                .thenThrow(new AgentConfigDomainAlreadyExistsException("exists"));

        mockMvc.perform(post("/api/1.0/agent-config")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(config)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.httpStatus").value(409));
    }

    @Test
    void update_Success() throws Exception {
        String domain = "test.agent";
        AgentConfig config = new AgentConfig();
        config.setDomain(domain);
        config.setId("id");

        when(agentConfigManager.getConfig(domain)).thenReturn(Optional.of(config));
        when(agentConfigManager.saveConfig(any(AgentConfig.class))).thenReturn(config);

        mockMvc.perform(put("/api/1.0/agent-config/{domain}", domain)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(config)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.response.status").value(200));
    }

    @Test
    void deleteTest() throws Exception {
        String domain = "test.agent";
        mockMvc.perform(delete("/api/1.0/agent-config/{domain}", domain))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.response.status").value(200));
    }
}
