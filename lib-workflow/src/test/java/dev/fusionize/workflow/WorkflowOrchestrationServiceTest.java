package dev.fusionize.workflow;

import dev.fusionize.workflow.component.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.function.Predicate;

class WorkflowOrchestrationServiceTest {
    WorkflowOrchestrationService service;
    WorkflowComponentRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new WorkflowComponentRegistry();
        service = new WorkflowOrchestrationService(registry);
    }

    @Test
    void orchestrate() {
        registry.register(WorkflowComponent.builder("test")
                .withDomain("receivedEmail")
                .withCompatible(WorkflowNodeType.TASK)
                .build(), new WorkflowComponentConfig(),
                new MockRecEmailComponent());
        Workflow workflow = Workflow.builder("test")
                .addNode(WorkflowNode.builder()
                        .workflowNodeId("node-1")
                        .component("task:test.receivedEmail")
                        .componentConfig(WorkflowComponentConfig.builder().put("address", "a@b.com").build())
                        .type(WorkflowNodeType.TASK)
                ).build();
        service.orchestrate(workflow);
        service.orchestrate(workflow);

    }

    static final class MockRecEmailComponent implements WorkflowComponentRuntimeTask {
        String address;
        @Override
        public void run(WorkflowContext context, Predicate<WorkflowContext> onFinish) {
            System.out.println("incoming email: " + address);
        }

        @Override
        public void configure(WorkflowNode runningNode) {
            this.address = runningNode.getComponentConfig().getConfig().get("address").toString();
        }

        @Override
        public boolean canActivate(WorkflowContext context) {
            return true;
        }

        @Override
        public WorkflowComponentRuntime clone() throws CloneNotSupportedException {
            return (WorkflowComponentRuntime) super.clone();
        }
    }
}