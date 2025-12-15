package dev.fusionize.orchestrator.workflow;

import dev.fusionize.workflow.Workflow;
import dev.fusionize.workflow.WorkflowExecution;
import dev.fusionize.workflow.WorkflowLogger;
import dev.fusionize.workflow.registry.WorkflowExecutionRepoRegistry;
import dev.fusionize.workflow.registry.WorkflowRepoRegistry;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(WorkflowController.class)
@AutoConfigureMockMvc(addFilters = false)
class WorkflowControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private WorkflowRepoRegistry workflowRepoRegistry;

    @MockitoBean
    private WorkflowLogger workflowLogger;

    @MockitoBean
    private WorkflowExecutionRepoRegistry workflowExecutionRepoRegistry;

    @InjectMocks
    private WorkflowController workflowController;

    @Test
    void getAll() throws Exception {
        Workflow workflow = new Workflow();
        workflow.setDomain("test-workflow");
        when(workflowRepoRegistry.getAll()).thenReturn(List.of(workflow));

        mockMvc.perform(get("/api/1.0/workflow"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.response.status").value(200))
                .andExpect(jsonPath("$.response.message[0].domain").value("test-workflow"));
    }

    @Test
    void getWorkflowExecutions() throws Exception {
        String workflowId = "test-workflow";
        WorkflowExecution execution = new WorkflowExecution();
        execution.setWorkflowId(workflowId);
        execution.setWorkflowExecutionId("exec-1");
        
        when(workflowExecutionRepoRegistry.getWorkflowExecutions(workflowId))
                .thenReturn(List.of(execution));

        mockMvc.perform(get("/api/1.0/workflow/{workflowId}/executions", workflowId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.response.status").value(200))
                .andExpect(jsonPath("$.response.message[0].workflowId").value(workflowId))
                .andExpect(jsonPath("$.response.message[0].workflowExecutionId").value("exec-1"));
    }
}
