package dev.fusionize.process.runtime;

import dev.fusionize.process.*;
import dev.fusionize.process.repo.ProcessExecutionRepository;
import dev.fusionize.workflow.context.Context;
import dev.fusionize.workflow.component.runtime.ComponentRuntimeConfig;
import dev.fusionize.workflow.component.runtime.ComponentRuntimeEngine;
import dev.fusionize.workflow.events.Event;
import dev.fusionize.workflow.events.EventPublisher;
import org.flowable.bpmn.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.CompletableFuture;

@Service
public class ProcessRuntimeEngine {
    private static final Logger log = LoggerFactory.getLogger(ProcessRuntimeEngine.class);

    private final ProcessExecutionRepository processExecutionRepository;
    private final ComponentRuntimeEngine componentRuntimeEngine;
    private final EventPublisher<Event> eventPublisher;

    public ProcessRuntimeEngine(ProcessExecutionRepository processExecutionRepository,
                                ComponentRuntimeEngine componentRuntimeEngine,
                                EventPublisher<Event> eventPublisher) {
        this.processExecutionRepository = processExecutionRepository;
        this.componentRuntimeEngine = componentRuntimeEngine;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Start a new process execution
     */
    public ProcessExecution startProcess(dev.fusionize.process.Process process, Context initialContext) {
        if (process == null || process.getBpmnModel() == null) {
            throw new IllegalArgumentException("Process and BpmnModel cannot be null");
        }

        ProcessExecution execution = ProcessExecution.of(process, initialContext);
        execution.setStatus(ProcessExecutionStatus.IN_PROGRESS);
        processExecutionRepository.save(execution);

        // Find and activate start events
        List<StartEvent> startEvents = findStartEvents(process.getBpmnModel());
        if (startEvents.isEmpty()) {
            log.warn("No start events found in process {}", process.getProcessId());
            execution.setStatus(ProcessExecutionStatus.ERROR);
            processExecutionRepository.save(execution);
            return execution;
        }

        // Activate all start events (BPMN allows multiple start events)
        for (StartEvent startEvent : startEvents) {
            ProcessElementExecution elementExecution = ProcessElementExecution.of(startEvent, execution.getContext());
            elementExecution.setState(ProcessElementExecutionState.ACTIVE);
            execution.getActiveElements().add(elementExecution);
            processExecutionRepository.save(execution);

            // Complete start event and continue flow
            completeElement(execution, elementExecution);
        }

        return execution;
    }

    /**
     * Continue process execution after an element is completed
     */
    public void continueExecution(ProcessExecution execution, ProcessElementExecution completedElement) {
        if (execution == null || completedElement == null) {
            log.error("Cannot continue execution: execution or element is null");
            return;
        }

        // Mark element as completed
        completedElement.setState(ProcessElementExecutionState.COMPLETED);
        execution.moveToCompleted(completedElement);
        processExecutionRepository.save(execution);

        // Find outgoing sequence flows
        List<SequenceFlow> outgoingFlows = getOutgoingSequenceFlows(completedElement.getFlowElement(), execution.getProcess().getBpmnModel());
        
        if (outgoingFlows.isEmpty()) {
            // Check if this is an end event
            if (completedElement.getFlowElement() instanceof EndEvent) {
                handleEndEvent(execution, (EndEvent) completedElement.getFlowElement());
            } else {
                log.warn("Element {} has no outgoing flows and is not an end event", completedElement.getElementId());
            }
            return;
        }

        // Process outgoing flows
        for (SequenceFlow flow : outgoingFlows) {
            FlowElement targetElement = getTargetElement(flow, execution.getProcess().getBpmnModel());
            if (targetElement != null) {
                activateElement(execution, targetElement, completedElement.getContext());
            }
        }
    }

    /**
     * Activate a BPMN element
     */
    private void activateElement(ProcessExecution execution, FlowElement element, Context context) {
        ProcessElementExecution elementExecution = ProcessElementExecution.of(element, context);
        elementExecution.setState(ProcessElementExecutionState.ACTIVE);
        execution.getActiveElements().add(elementExecution);
        processExecutionRepository.save(execution);

        log.debug("Activated element {} of type {}", element.getId(), element.getClass().getSimpleName());

        // Handle different element types
        if (element instanceof Task) {
            handleTask(execution, elementExecution, (Task) element);
        } else if (element instanceof ExclusiveGateway) {
            handleExclusiveGateway(execution, elementExecution, (ExclusiveGateway) element);
        } else if (element instanceof ParallelGateway) {
            handleParallelGateway(execution, elementExecution, (ParallelGateway) element);
        } else if (element instanceof InclusiveGateway) {
            handleInclusiveGateway(execution, elementExecution, (InclusiveGateway) element);
        } else if (element instanceof EndEvent) {
            handleEndEvent(execution, (EndEvent) element);
        } else if (element instanceof IntermediateCatchEvent) {
            handleIntermediateCatchEvent(execution, elementExecution, (IntermediateCatchEvent) element);
        }
//        else if (element instanceof IntermediateThrowEvent) {
//            handleIntermediateThrowEvent(execution, elementExecution, (IntermediateThrowEvent) element);
//        }
        else {
            log.warn("Unsupported element type: {}", element.getClass().getSimpleName());
            // For unsupported elements, just continue to next elements
            completeElement(execution, elementExecution);
        }
    }

    /**
     * Handle task execution
     */
    private void handleTask(ProcessExecution execution, ProcessElementExecution elementExecution, Task task) {
        elementExecution.setState(ProcessElementExecutionState.WAITING);
        processExecutionRepository.save(execution);

        // Extract component information from task
        String componentName = extractComponentName(task);
        if (componentName == null || componentName.isEmpty()) {
            log.warn("Task {} has no component name, skipping execution", task.getId());
            elementExecution.setState(ProcessElementExecutionState.COMPLETED);
            continueExecution(execution, elementExecution);
            return;
        }

        // Create component config from task properties
        ComponentRuntimeConfig config = createComponentConfig(task);
        
        // Use ComponentRuntimeEngine to execute the task
        // Note: This is a simplified integration - in a real implementation,
        // you would need to adapt the ComponentRuntimeEngine to work with BPMN tasks
        CompletableFuture.runAsync(() -> {
            try {
                // For now, we'll simulate task execution
                // In a full implementation, this would integrate with ComponentRuntimeEngine
                log.info("Executing task {} with component {}", task.getId(), componentName);
                
                // Simulate task execution
                Thread.sleep(100); // Simulate work
                
                // Update context (in real implementation, this would come from component)
                Context updatedContext = elementExecution.getContext();
                updatedContext.getData().put("lastTask", task.getId());
                updatedContext.getData().put("lastTaskName", task.getName());
                
                elementExecution.setContext(updatedContext);
                elementExecution.setState(ProcessElementExecutionState.COMPLETED);
                continueExecution(execution, elementExecution);
            } catch (Exception e) {
                log.error("Error executing task {}", task.getId(), e);
                elementExecution.setState(ProcessElementExecutionState.FAILED);
                execution.setStatus(ProcessExecutionStatus.ERROR);
                processExecutionRepository.save(execution);
            }
        });
    }

    /**
     * Handle exclusive gateway (XOR gateway)
     */
    private void handleExclusiveGateway(ProcessExecution execution, ProcessElementExecution elementExecution, ExclusiveGateway gateway) {
        List<SequenceFlow> outgoingFlows = gateway.getOutgoingFlows();
        
        if (outgoingFlows.isEmpty()) {
            log.warn("Exclusive gateway {} has no outgoing flows", gateway.getId());
            completeElement(execution, elementExecution);
            return;
        }

        // Evaluate conditions and select the first matching flow
        SequenceFlow selectedFlow = null;
        for (SequenceFlow flow : outgoingFlows) {
            if (evaluateFlowCondition(flow, elementExecution.getContext())) {
                selectedFlow = flow;
                break;
            }
        }

        // If no condition matches and there's a default flow, use it
        if (selectedFlow == null) {
            selectedFlow = outgoingFlows.stream()
                    .filter(flow -> flow.getId() != null && flow.getId().contains("default"))
                    .findFirst()
                    .orElse(outgoingFlows.get(0)); // Fallback to first flow
        }

        elementExecution.setState(ProcessElementExecutionState.COMPLETED);
        continueExecution(execution, elementExecution);

        // Activate target of selected flow
        if (selectedFlow != null) {
            FlowElement targetElement = getTargetElement(selectedFlow, execution.getProcess().getBpmnModel());
            if (targetElement != null) {
                activateElement(execution, targetElement, elementExecution.getContext());
            }
        }
    }

    /**
     * Handle parallel gateway (AND gateway)
     */
    private void handleParallelGateway(ProcessExecution execution, ProcessElementExecution elementExecution, ParallelGateway gateway) {
        List<SequenceFlow> outgoingFlows = gateway.getOutgoingFlows();
        
        if (outgoingFlows.isEmpty()) {
            log.warn("Parallel gateway {} has no outgoing flows", gateway.getId());
            completeElement(execution, elementExecution);
            return;
        }

        // For parallel split: activate all outgoing flows
        // For parallel merge: wait for all incoming flows (handled separately)
        if (isParallelSplit(gateway, execution.getProcess().getBpmnModel())) {
            elementExecution.setState(ProcessElementExecutionState.COMPLETED);
            continueExecution(execution, elementExecution);

            // Activate all target elements in parallel
            for (SequenceFlow flow : outgoingFlows) {
                FlowElement targetElement = getTargetElement(flow, execution.getProcess().getBpmnModel());
                if (targetElement != null) {
                    activateElement(execution, targetElement, elementExecution.getContext());
                }
            }
        } else {
            // Parallel merge: check if all incoming flows are completed
            // This is a simplified implementation - full implementation would track incoming tokens
            elementExecution.setState(ProcessElementExecutionState.COMPLETED);
            continueExecution(execution, elementExecution);
        }
    }

    /**
     * Handle inclusive gateway (OR gateway)
     */
    private void handleInclusiveGateway(ProcessExecution execution, ProcessElementExecution elementExecution, InclusiveGateway gateway) {
        List<SequenceFlow> outgoingFlows = gateway.getOutgoingFlows();
        
        if (outgoingFlows.isEmpty()) {
            log.warn("Inclusive gateway {} has no outgoing flows", gateway.getId());
            completeElement(execution, elementExecution);
            return;
        }

        // Evaluate all conditions and activate matching flows
        elementExecution.setState(ProcessElementExecutionState.COMPLETED);
        continueExecution(execution, elementExecution);

        for (SequenceFlow flow : outgoingFlows) {
            if (evaluateFlowCondition(flow, elementExecution.getContext())) {
                FlowElement targetElement = getTargetElement(flow, execution.getProcess().getBpmnModel());
                if (targetElement != null) {
                    activateElement(execution, targetElement, elementExecution.getContext());
                }
            }
        }
    }

    /**
     * Handle end event
     */
    private void handleEndEvent(ProcessExecution execution, EndEvent endEvent) {
        log.info("Process execution {} reached end event {}", execution.getProcessExecutionId(), endEvent.getId());
        execution.setStatus(ProcessExecutionStatus.SUCCESS);
        processExecutionRepository.save(execution);
    }

    /**
     * Handle intermediate catch event
     */
    private void handleIntermediateCatchEvent(ProcessExecution execution, ProcessElementExecution elementExecution, IntermediateCatchEvent event) {
        // For timer events, wait for timer
        // For message events, wait for message
        // For now, just continue
        log.info("Handling intermediate catch event {}", event.getId());
        completeElement(execution, elementExecution);
    }

    /**
     * Handle intermediate throw event
     */
//    private void handleIntermediateThrowEvent(ProcessExecution execution, ProcessElementExecution elementExecution, IntermediateThrowEvent event) {
//        log.info("Handling intermediate throw event {}", event.getId());
//        completeElement(execution, elementExecution);
//    }

    /**
     * Complete an element and continue execution
     */
    private void completeElement(ProcessExecution execution, ProcessElementExecution elementExecution) {
        continueExecution(execution, elementExecution);
    }

    /**
     * Find start events in the BPMN model
     */
    private List<StartEvent> findStartEvents(BpmnModel bpmnModel) {
        List<StartEvent> startEvents = new ArrayList<>();
        org.flowable.bpmn.model.Process process = bpmnModel.getMainProcess();
        if (process != null) {
            for (FlowElement element : process.getFlowElements()) {
                if (element instanceof StartEvent) {
                    startEvents.add((StartEvent) element);
                }
            }
        }
        return startEvents;
    }

    /**
     * Get outgoing sequence flows for an element
     */
    private List<SequenceFlow> getOutgoingSequenceFlows(FlowElement element, BpmnModel bpmnModel) {
        if (element instanceof FlowNode) {
            return ((FlowNode) element).getOutgoingFlows();
        }
        return Collections.emptyList();
    }

    /**
     * Get target element from a sequence flow
     */
    private FlowElement getTargetElement(SequenceFlow flow, BpmnModel bpmnModel) {
        org.flowable.bpmn.model.Process process = bpmnModel.getMainProcess();
        if (process != null && flow.getTargetRef() != null) {
            return process.getFlowElement(flow.getTargetRef());
        }
        return null;
    }

    /**
     * Evaluate condition on a sequence flow
     */
    private boolean evaluateFlowCondition(SequenceFlow flow, Context context) {
        if (flow.getConditionExpression() == null) {
            return true; // No condition means always true
        }

        // Simplified condition evaluation
        // In a full implementation, this would parse and evaluate the condition expression
        String condition = flow.getConditionExpression();
        if (condition == null || condition.trim().isEmpty()) {
            return true;
        }

        // For now, return true for default flows
        return true;
    }

    /**
     * Check if a parallel gateway is a split (multiple outgoing) or merge (multiple incoming)
     */
    private boolean isParallelSplit(ParallelGateway gateway, BpmnModel bpmnModel) {
        return gateway.getOutgoingFlows().size() > 1;
    }

    /**
     * Extract component name from task
     */
    private String extractComponentName(Task task) {
        // Try to get from task name or ID
        if (task.getName() != null && !task.getName().isEmpty()) {
            return task.getName();
        }
        return task.getId();
    }

    /**
     * Create component config from task properties
     */
    private ComponentRuntimeConfig createComponentConfig(Task task) {
        ComponentRuntimeConfig.Builder builder = ComponentRuntimeConfig.builder();
        builder.put("taskId", task.getId());
        builder.put("taskName", task.getName());
        builder.put("taskType", task.getClass().getSimpleName());
        return builder.build();
    }

    /**
     * Get process execution by ID
     */
    public Optional<ProcessExecution> getProcessExecution(String processExecutionId) {
        return processExecutionRepository.findByProcessExecutionId(processExecutionId);
    }

    /**
     * Suspend process execution
     */
    public void suspendProcessExecution(String processExecutionId) {
        Optional<ProcessExecution> executionOpt = getProcessExecution(processExecutionId);
        if (executionOpt.isPresent()) {
            ProcessExecution execution = executionOpt.get();
            execution.setStatus(ProcessExecutionStatus.SUSPENDED);
            processExecutionRepository.save(execution);
        }
    }

    /**
     * Resume suspended process execution
     */
    public void resumeProcessExecution(String processExecutionId) {
        Optional<ProcessExecution> executionOpt = getProcessExecution(processExecutionId);
        if (executionOpt.isPresent()) {
            ProcessExecution execution = executionOpt.get();
            if (execution.getStatus() == ProcessExecutionStatus.SUSPENDED) {
                execution.setStatus(ProcessExecutionStatus.IN_PROGRESS);
                processExecutionRepository.save(execution);
                
                // Resume active elements
                for (ProcessElementExecution element : execution.getActiveElements()) {
                    if (element.getState() == ProcessElementExecutionState.WAITING) {
                        FlowElement flowElement = element.getFlowElement();
                        if (flowElement instanceof Task) {
                            handleTask(execution, element, (Task) flowElement);
                        }
                    }
                }
            }
        }
    }

    /**
     * Terminate process execution
     */
    public void terminateProcessExecution(String processExecutionId) {
        Optional<ProcessExecution> executionOpt = getProcessExecution(processExecutionId);
        if (executionOpt.isPresent()) {
            ProcessExecution execution = executionOpt.get();
            execution.setStatus(ProcessExecutionStatus.TERMINATED);
            processExecutionRepository.save(execution);
        }
    }
}

