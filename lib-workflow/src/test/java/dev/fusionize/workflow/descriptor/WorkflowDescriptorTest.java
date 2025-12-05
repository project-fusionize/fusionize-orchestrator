package dev.fusionize.workflow.descriptor;

import dev.fusionize.workflow.Workflow;
import dev.fusionize.workflow.WorkflowNode;
import dev.fusionize.workflow.WorkflowNodeType;
import dev.fusionize.workflow.component.ComponentConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class WorkflowDescriptorTest {

    private WorkflowDescriptor descriptor;
    private URL workflowTestJsonUrl;
    private URL workflowTestYamlUrl;
    private URL workflowSimpleJsonUrl;
    private URL workflowSimpleYamlUrl;

    @BeforeEach
    void setUp() {
        descriptor = new WorkflowDescriptor();
        workflowTestJsonUrl = this.getClass().getResource("/workflow-test.json");
        workflowTestYamlUrl = this.getClass().getResource("/workflow-test.yml");
        workflowSimpleJsonUrl = this.getClass().getResource("/workflow-simple.json");
        workflowSimpleYamlUrl = this.getClass().getResource("/workflow-simple.yml");

        assertNotNull(workflowTestJsonUrl, "workflow-test.json resource not found");
        assertNotNull(workflowTestYamlUrl, "workflow-test.yml resource not found");
        assertNotNull(workflowSimpleJsonUrl, "workflow-simple.json resource not found");
        assertNotNull(workflowSimpleYamlUrl, "workflow-simple.yml resource not found");
    }

    @Test
    void fromJsonDescription_WithValidJson_ShouldReturnWorkflow() throws IOException {
        // Given
        String json = Files.readString(new File(workflowTestJsonUrl.getFile()).toPath());

        // When
        Workflow workflow = descriptor.fromJsonDescription(json);

        // Then
        assertNotNull(workflow);
        assertEquals("Test Email Workflow", workflow.getName());
        assertEquals("test.email-workflow", workflow.getDomain());
        assertEquals("test-email-workflow-key", workflow.getKey());
        assertEquals("A test workflow for email processing", workflow.getDescription());
        assertEquals(1, workflow.getVersion());
        assertTrue(workflow.isActive());
        assertNotNull(workflow.getNodes());
        assertFalse(workflow.getNodes().isEmpty());
    }

    @Test
    void fromJsonDescription_WithValidJson_ShouldBuildCorrectNodeHierarchy() throws IOException {
        // Given
        String json = Files.readString(new File(workflowTestJsonUrl.getFile()).toPath());

        // When
        Workflow workflow = descriptor.fromJsonDescription(json);

        // Then
        assertNotNull(workflow.getNodes());
        assertEquals(1, workflow.getNodes().size()); // Only root node (start1)

        WorkflowNode root = workflow.getNodes().get(0);
        assertEquals("start1", root.getWorkflowNodeKey());
        assertEquals(WorkflowNodeType.START, root.getType());
        assertEquals(1, root.getChildren().size());

        WorkflowNode decision = root.getChildren().get(0);
        assertEquals("decision1", decision.getWorkflowNodeKey());
        assertEquals(WorkflowNodeType.DECISION, decision.getType());
        assertEquals(2, decision.getChildren().size());

        // Verify both task nodes are children of decision
        List<String> taskKeys = decision.getChildren().stream()
                .map(WorkflowNode::getWorkflowNodeKey)
                .toList();
        assertTrue(taskKeys.contains("task1"));
        assertTrue(taskKeys.contains("task2"));
    }

    @Test
    void fromJsonDescription_WithValidJson_ShouldPreserveComponentConfig() throws IOException {
        // Given
        String json = Files.readString(new File(workflowTestJsonUrl.getFile()).toPath());

        // When
        Workflow workflow = descriptor.fromJsonDescription(json);

        // Then
        WorkflowNode startNode = workflow.getNodes().get(0);
        assertNotNull(startNode.getComponentConfig());
        assertEquals("incoming@email.com", startNode.getComponentConfig().getConfig().get("address"));

        WorkflowNode decision = startNode.getChildren().get(0);
        assertNotNull(decision.getComponentConfig());
        assertTrue(decision.getComponentConfig().getConfig().containsKey("routeMap"));
        @SuppressWarnings("unchecked")
        Map<String, String> routeMap = (Map<String, String>) decision.getComponentConfig().getConfig().get("routeMap");
        assertEquals("task1", routeMap.get("route 1"));
        assertEquals("task2", routeMap.get("route 2"));
    }

    @Test
    void fromYamlDescription_WithValidYaml_ShouldReturnWorkflow() throws IOException {
        // Given
        String yaml = Files.readString(new File(workflowTestYamlUrl.getFile()).toPath());

        // When
        Workflow workflow = descriptor.fromYamlDescription(yaml);

        // Then
        assertNotNull(workflow);
        assertEquals("Test Email Workflow", workflow.getName());
        assertEquals("test.email-workflow", workflow.getDomain());
        assertEquals("test-email-workflow-key", workflow.getKey());
        assertEquals("A test workflow for email processing", workflow.getDescription());
        assertEquals(1, workflow.getVersion());
        assertTrue(workflow.isActive());
    }

    @Test
    void fromYamlDescription_WithValidYaml_ShouldBuildCorrectNodeHierarchy() throws IOException {
        // Given
        String yaml = Files.readString(new File(workflowTestYamlUrl.getFile()).toPath());

        // When
        Workflow workflow = descriptor.fromYamlDescription(yaml);

        // Then
        assertNotNull(workflow.getNodes());
        assertEquals(1, workflow.getNodes().size());

        WorkflowNode root = workflow.getNodes().get(0);
        assertEquals("start1", root.getWorkflowNodeKey());
        assertEquals(1, root.getChildren().size());

        WorkflowNode decision = root.getChildren().get(0);
        assertEquals("decision1", decision.getWorkflowNodeKey());
        assertEquals(2, decision.getChildren().size());
    }

    @Test
    void fromYamlDescription_WithSimpleWorkflow_ShouldParseCorrectly() throws IOException {
        // Given
        String yaml = Files.readString(new File(workflowSimpleYamlUrl.getFile()).toPath());

        // When
        Workflow workflow = descriptor.fromYamlDescription(yaml);

        // Then
        assertNotNull(workflow);
        assertEquals("Simple Workflow", workflow.getName());
        assertEquals(1, workflow.getNodes().size());

        WorkflowNode start = workflow.getNodes().get(0);
        assertEquals("start", start.getWorkflowNodeKey());
        assertEquals(1, start.getChildren().size());

        WorkflowNode task = start.getChildren().get(0);
        assertEquals("task", task.getWorkflowNodeKey());
        assertEquals(1, task.getChildren().size());

        WorkflowNode end = task.getChildren().get(0);
        assertEquals("end", end.getWorkflowNodeKey());
        assertTrue(end.getChildren().isEmpty());
    }

    @Test
    void fromJsonDescription_WithSimpleWorkflow_ShouldParseCorrectly() throws IOException {
        // Given
        String json = Files.readString(new File(workflowSimpleJsonUrl.getFile()).toPath());

        // When
        Workflow workflow = descriptor.fromJsonDescription(json);

        // Then
        assertNotNull(workflow);
        assertEquals("Simple Workflow", workflow.getName());
        assertEquals(1, workflow.getNodes().size());

        WorkflowNode start = workflow.getNodes().get(0);
        assertEquals("start", start.getWorkflowNodeKey());
        assertEquals(1, start.getChildren().size());

        WorkflowNode task = start.getChildren().get(0);
        assertEquals("task", task.getWorkflowNodeKey());
        assertNotNull(task.getComponentConfig());
        assertEquals(5000.0, task.getComponentConfig().getConfig().get("timeout"));
    }

    @Test
    void toJsonDescription_WithValidWorkflow_ShouldGenerateValidJson() {
        // Given
        Workflow workflow = createTestWorkflow();

        // When
        String json = descriptor.toJsonDescription(workflow);

        // Then
        assertNotNull(json);
        assertFalse(json.isEmpty());
        assertTrue(json.contains("\"name\""));
        assertTrue(json.contains("\"nodes\""));
        
        // Parse back to verify it's valid JSON
        Workflow parsedWorkflow = descriptor.fromJsonDescription(json);
        assertNotNull(parsedWorkflow);
        assertEquals(workflow.getName(), parsedWorkflow.getName());
    }

    @Test
    void toJsonDescription_WithValidWorkflow_ShouldPreserveWorkflowData() {
        // Given
        Workflow workflow = createTestWorkflow();

        // When
        String json = descriptor.toJsonDescription(workflow);
        Workflow parsedWorkflow = descriptor.fromJsonDescription(json);

        // Then
        assertNotNull(parsedWorkflow);
        assertEquals(workflow.getName(), parsedWorkflow.getName());
        assertEquals(workflow.getDomain(), parsedWorkflow.getDomain());
        assertEquals(workflow.getDescription(), parsedWorkflow.getDescription());
        assertEquals(workflow.getVersion(), parsedWorkflow.getVersion());
        assertEquals(workflow.isActive(), parsedWorkflow.isActive());
    }

    @Test
    void toYamlDescription_WithValidWorkflow_ShouldGenerateValidYaml() {
        // Given
        Workflow workflow = createTestWorkflow();

        // When
        String yaml = descriptor.toYamlDescription(workflow);

        // Then
        assertNotNull(yaml);
        assertFalse(yaml.isEmpty());
        assertTrue(yaml.contains("name:"));
        assertTrue(yaml.contains("nodes:"));
        
        // Parse back to verify it's valid YAML
        Workflow parsedWorkflow = descriptor.fromYamlDescription(yaml);
        assertNotNull(parsedWorkflow);
        assertEquals(workflow.getName(), parsedWorkflow.getName());
    }

    @Test
    void toYamlDescription_WithValidWorkflow_ShouldPreserveWorkflowData() {
        // Given
        Workflow workflow = createTestWorkflow();

        // When
        String yaml = descriptor.toYamlDescription(workflow);
        Workflow parsedWorkflow = descriptor.fromYamlDescription(yaml);

        // Then
        assertNotNull(parsedWorkflow);
        assertEquals(workflow.getName(), parsedWorkflow.getName());
        assertEquals(workflow.getDomain(), parsedWorkflow.getDomain());
        assertEquals(workflow.getDescription(), parsedWorkflow.getDescription());
        assertEquals(workflow.getVersion(), parsedWorkflow.getVersion());
        assertEquals(workflow.isActive(), parsedWorkflow.isActive());
    }

    @Test
    void roundTrip_JsonToWorkflowToJson_ShouldPreserveData() throws IOException {
        // Given
        String originalJson = Files.readString(new File(workflowTestJsonUrl.getFile()).toPath());

        // When
        Workflow workflow = descriptor.fromJsonDescription(originalJson);
        String generatedJson = descriptor.toJsonDescription(workflow);
        Workflow roundTripWorkflow = descriptor.fromJsonDescription(generatedJson);

        // Then
        assertNotNull(roundTripWorkflow);
        assertEquals("Test Email Workflow", roundTripWorkflow.getName());
        assertEquals("test.email-workflow", roundTripWorkflow.getDomain());
        assertNotNull(roundTripWorkflow.getNodes());
        assertEquals(1, roundTripWorkflow.getNodes().size());
    }

    @Test
    void roundTrip_YamlToWorkflowToYaml_ShouldPreserveData() throws IOException {
        // Given
        String originalYaml = Files.readString(new File(workflowTestYamlUrl.getFile()).toPath());

        // When
        Workflow workflow = descriptor.fromYamlDescription(originalYaml);
        String generatedYaml = descriptor.toYamlDescription(workflow);
        Workflow roundTripWorkflow = descriptor.fromYamlDescription(generatedYaml);

        // Then
        assertNotNull(roundTripWorkflow);
        assertEquals("Test Email Workflow", roundTripWorkflow.getName());
        assertEquals("test.email-workflow", roundTripWorkflow.getDomain());
        assertNotNull(roundTripWorkflow.getNodes());
        assertEquals(1, roundTripWorkflow.getNodes().size());
    }

    @Test
    void crossFormat_JsonToYaml_ShouldWork() throws IOException {
        // Given
        String json = Files.readString(new File(workflowTestJsonUrl.getFile()).toPath());

        // When
        Workflow workflow = descriptor.fromJsonDescription(json);
        String yaml = descriptor.toYamlDescription(workflow);
        Workflow parsedFromYaml = descriptor.fromYamlDescription(yaml);

        // Then
        assertNotNull(parsedFromYaml);
        assertEquals(workflow.getName(), parsedFromYaml.getName());
        assertEquals(workflow.getDomain(), parsedFromYaml.getDomain());
    }

    @Test
    void crossFormat_YamlToJson_ShouldWork() throws IOException {
        // Given
        String yaml = Files.readString(new File(workflowTestYamlUrl.getFile()).toPath());

        // When
        Workflow workflow = descriptor.fromYamlDescription(yaml);
        String json = descriptor.toJsonDescription(workflow);
        Workflow parsedFromJson = descriptor.fromJsonDescription(json);

        // Then
        assertNotNull(parsedFromJson);
        assertEquals(workflow.getName(), parsedFromJson.getName());
        assertEquals(workflow.getDomain(), parsedFromJson.getDomain());
    }

    @Test
    void toJsonDescription_WithWorkflowWithComponentConfig_ShouldIncludeConfig() {
        // Given
        WorkflowNode node = WorkflowNode.builder()
                .type(WorkflowNodeType.TASK)
                .component("task:test.task")
                .workflowNodeKey("task1")
                .componentConfig(ComponentConfig.builder()
                        .put("address", "test@example.com")
                        .put("retryCount", 3)
                        .build())
                .build();

        Workflow workflow = Workflow.builder("test")
                .withName("Test Workflow")
                .withDomain("test.workflow")
                .withKey("test-key")
                .withDescription("Test")
                .withVersion(1)
                .withActive(true)
                .addNode(node)
                .build();

        // When
        String json = descriptor.toJsonDescription(workflow);
        Workflow parsed = descriptor.fromJsonDescription(json);

        // Then
        assertNotNull(parsed);
        WorkflowNode parsedNode = parsed.getNodes().get(0);
        assertNotNull(parsedNode.getComponentConfig());
        assertEquals("test@example.com", parsedNode.getComponentConfig().getConfig().get("address"));
        assertEquals(3.0, parsedNode.getComponentConfig().getConfig().get("retryCount"));
    }

    @Test
    void toYamlDescription_WithEmptyNodes_ShouldHandleGracefully() {
        // Given
        Workflow workflow = Workflow.builder("test")
                .withName("Empty Workflow")
                .withDomain("test.empty")
                .withKey("empty-key")
                .withDescription("Empty workflow")
                .withVersion(1)
                .withActive(true)
                .withNodes(List.of())
                .build();

        // When
        String yaml = descriptor.toYamlDescription(workflow);
        Workflow parsed = descriptor.fromYamlDescription(yaml);

        // Then
        assertNotNull(parsed);
        assertNotNull(parsed.getNodes());
        assertTrue(parsed.getNodes().isEmpty());
    }

    @Test
    void fromJsonDescription_WithEmptyNodes_ShouldHandleGracefully() {
        // Given
        String json = """
            {
              "kind": "Workflow",
              "apiVersion": "v1",
              "name": "Empty Workflow",
              "domain": "test.empty",
              "key": "empty-key",
              "description": "Empty workflow",
              "version": 1,
              "active": true,
              "nodes": {}
            }
            """;

        // When
        Workflow workflow = descriptor.fromJsonDescription(json);

        // Then
        assertNotNull(workflow);
        assertEquals("Empty Workflow", workflow.getName());
        assertNotNull(workflow.getNodes());
        assertTrue(workflow.getNodes().isEmpty());
    }

    @Test
    void fromYamlDescription_WithEmptyNodes_ShouldHandleGracefully() {
        // Given
        String yaml = """
            kind: Workflow
            apiVersion: v1
            name: Empty Workflow
            domain: test.empty
            key: empty-key
            description: Empty workflow
            version: 1
            active: true
            nodes: {}
            """;

        // When
        Workflow workflow = descriptor.fromYamlDescription(yaml);

        // Then
        assertNotNull(workflow);
        assertEquals("Empty Workflow", workflow.getName());
        assertNotNull(workflow.getNodes());
        assertTrue(workflow.getNodes().isEmpty());
    }

    // Helper method to create a test workflow
    private Workflow createTestWorkflow() {
        WorkflowNode endNode = WorkflowNode.builder()
                .type(WorkflowNodeType.END)
                .component("end:test.end")
                .workflowNodeKey("end1")
                .build();

        WorkflowNode taskNode = WorkflowNode.builder()
                .type(WorkflowNodeType.TASK)
                .component("task:test.task")
                .workflowNodeKey("task1")
                .componentConfig(ComponentConfig.builder()
                        .put("address", "test@example.com")
                        .build())
                .addChild(endNode)
                .build();

        WorkflowNode startNode = WorkflowNode.builder()
                .type(WorkflowNodeType.START)
                .component("start:test.start")
                .workflowNodeKey("start1")
                .addChild(taskNode)
                .build();

        return Workflow.builder("test")
                .withName("Test Workflow")
                .withDomain("test.workflow")
                .withKey("test-key")
                .withDescription("A test workflow")
                .withVersion(1)
                .withActive(true)
                .addNode(startNode)
                .build();
    }
}

