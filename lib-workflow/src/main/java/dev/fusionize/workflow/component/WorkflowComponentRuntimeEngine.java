package dev.fusionize.workflow.component;

import dev.fusionize.workflow.component.event.RuntimeEvent;
import dev.fusionize.workflow.component.event.RuntimeEventData;
import dev.fusionize.workflow.component.event.data.*;
import dev.fusionize.workflow.component.exceptions.ComponentNotFoundException;
import dev.fusionize.workflow.component.runtime.*;
import dev.fusionize.workflow.component.runtime.event.ComponentActivateEventData;
import dev.fusionize.workflow.component.runtime.event.ComponentFinishedEventData;
import dev.fusionize.workflow.component.runtime.event.ComponentTriggeredEventData;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class WorkflowComponentRuntimeEngine {
    public static final String ERR_CODE_COMP_NOT_FOUND = "(wcre101) Runtime component not found";
    private final ApplicationEventPublisher eventPublisher;
    private final WorkflowComponentRegistry workflowComponentRegistry;

    //todo make actual repository
    private final List<RuntimeEvent<?>> eventRepository = new ArrayList<>();

    public WorkflowComponentRuntimeEngine(ApplicationEventPublisher eventPublisher,
                                          WorkflowComponentRegistry workflowComponentRegistry) {
        this.eventPublisher = eventPublisher;
        this.workflowComponentRegistry = workflowComponentRegistry;
    }


//    private void executeStart(ComponentRuntime runtime, WorkflowContext context){
//        if(runtime instanceof ComponentRuntimeStart startRuntime){
//            startRuntime.start(context, (WorkflowContext start)->{
//                System.out.println(start);
//                return true;
//            });
//        }
//    }
//
//    private void executeDecision(ComponentRuntime runtime, WorkflowContext context){
//        if(runtime instanceof ComponentRuntimeDecision decisionRuntime){
//            decisionRuntime.decide(context, (List<WorkflowNode> candidates)->{
//                System.out.println(candidates);
//                return true;
//            });
//        }
//    }
//
//    private void executeTask(ComponentRuntime runtime, WorkflowContext context){
//        if(runtime instanceof ComponentRuntimeTask taskRuntime){
//            taskRuntime.run(context, (WorkflowContext finish)->{
//                System.out.println(finish);
//                return true;
//            });
//        }
//    }
//
//    private void executeWait(ComponentRuntime runtime, WorkflowContext context){
//        if(runtime instanceof ComponentRuntimeWait waitRuntime){
//            waitRuntime.wait(context, (WorkflowContext resumed)->{
//                System.out.println(resumed);
//                return true;
//            });
//        }
//    }
//
//    private void executeEnd(ComponentRuntime runtime, WorkflowContext context){
//        if(runtime instanceof ComponentRuntimeEnd endRuntime){
//            endRuntime.finish(context, (WorkflowExecutionStatus status)->{
//                System.out.println(status);
//                return true;
//            });
//        }
//    }

    @EventListener
    public void handleComponentActivateEvent(ComponentEvent<ComponentActivateEventData> event) {
        if(!shouldHandle(event, new ComponentActivateEventData().getEventTypeName())){
            return;
        }
        String causationId =  event.getCausationId();
        RuntimeEvent<?> runtimeActivationEvent = eventRepository.stream().filter(
                e-> causationId.equals(e.getEventId())).findFirst().orElse(null);
        RuntimeEvent<ActivateEventData> activateResponseEvent =
                new RuntimeEvent<ActivateEventData>(this)
                        .fromSource(this, runtimeActivationEvent, RuntimeEvent.Variant.RESPONSE, ActivateEventData.builder()
                                .activate(event.getData().getActivated())
                                .context(event.getData().getContext())
                                .origin(RuntimeEventData.Origin.RUNTIME_ENGINE)
                                .build());
        activateResponseEvent.getData().setException(event.getData().getException());
        eventPublisher.publishEvent(activateResponseEvent);
    }

    @EventListener
    public void handleComponentFinishedEvent(ComponentEvent<ComponentFinishedEventData> event) {

    }

    @EventListener
    public void handleComponentTriggeredEvent(ComponentEvent<ComponentTriggeredEventData> event) {
        if(!shouldHandle(event, new ComponentTriggeredEventData().getEventTypeName())){
            return;
        }
        String causationId =  event.getCausationId();
        RuntimeEvent<?> runtimeEvent = eventRepository.stream().filter(
                e-> causationId.equals(e.getEventId())).findFirst().orElse(null);
        //todo fix: assumption of StartEventData is always true
        RuntimeEvent<StartEventData> startEvent =
                new RuntimeEvent<StartEventData>(this)
                        .fromSource(this, runtimeEvent, RuntimeEvent.Variant.RESPONSE, StartEventData.builder()
                                .context(event.getData().getContext())
                                .origin(RuntimeEventData.Origin.RUNTIME_ENGINE)
                                .build());
        startEvent.getData().setException(event.getData().getException());
        eventPublisher.publishEvent(startEvent);
    }

    @EventListener
    public void handleActivateEvent(RuntimeEvent<ActivateEventData> event) {
        eventRepository.add(event);
        if(!shouldHandle(event, new ActivateEventData().getEventTypeName())){
            return;
        }
        if(RuntimeEvent.Variant.REQUEST.equals(event.getVariant())){
            Optional<ComponentRuntime> optionalWorkflowComponentRuntime =  workflowComponentRegistry.get(
                    event.getComponent(),
                    event.ne.getWorkflowNode().getComponentConfig());
            if(optionalWorkflowComponentRuntime.isEmpty()){
                RuntimeEvent<ActivateEventData> activateResponseEvent =
                        new RuntimeEvent<ActivateEventData>(this)
                                .fromSource(this, event, RuntimeEvent.Variant.RESPONSE, ActivateEventData.builder()
                                        .context(event.getData().getContext())
                                        .origin(RuntimeEventData.Origin.RUNTIME_ENGINE)
                                        .build());
                activateResponseEvent.getData().setException(new ComponentNotFoundException(ERR_CODE_COMP_NOT_FOUND));
                eventPublisher.publishEvent(activateResponseEvent);
                return;
            }

            ComponentRuntime runtime = optionalWorkflowComponentRuntime.get();
            runtime.canActivate(
                    new ComponentEvent.Builder<ComponentActivateEventData>()
                    .source(runtime).correlationId(event.getCorrelationId())
                    .causationId(event.getEventId())
                    .component(event.getComponent())
                    .processed(false)
                    .data(ComponentActivateEventData.builder()
                            .context(event.getData().getContext()).build())
                            .build());
        }

    }

    @EventListener
    public void handleStartEvent(RuntimeEvent<StartEventData> event) {
        eventRepository.add(event);
        if(!shouldHandle(event, new StartEventData().getEventTypeName())){
            return;
        }
        if(RuntimeEvent.Variant.REQUEST.equals(event.getVariant())){
            Optional<ComponentRuntime> optionalWorkflowComponentRuntime =  workflowComponentRegistry.get(
                    event.getComponent(),
                    event.ne.getWorkflowNode().getComponentConfig());
            if(optionalWorkflowComponentRuntime.isEmpty()){
                RuntimeEvent<StartEventData> activateResponseEvent =
                        new RuntimeEvent<StartEventData>(this)
                                .fromSource(this, event, RuntimeEvent.Variant.RESPONSE, StartEventData.builder()
                                        .context(event.getData().getContext())
                                        .origin(RuntimeEventData.Origin.RUNTIME_ENGINE)
                                        .build());
                activateResponseEvent.getData().setException(new ComponentNotFoundException(ERR_CODE_COMP_NOT_FOUND));
                eventPublisher.publishEvent(activateResponseEvent);
                return;
            }

            ComponentRuntime runtime = optionalWorkflowComponentRuntime.get();
            if(runtime instanceof ComponentRuntimeStart){
                ((ComponentRuntimeStart) runtime).start(
                        new ComponentEvent.Builder<ComponentTriggeredEventData>()
                                .source(runtime).correlationId(event.getCorrelationId())
                                .causationId(event.getEventId())
                                .component(event.getComponent())
                                .processed(false)
                                .data(ComponentTriggeredEventData.builder().context(event.getData().getContext()).build()).build());
            }
        }
    }

    @EventListener
    public void handleEndEvent(RuntimeEvent<EndEventData> event) {
        if(!shouldHandle(event, new EndEventData().getEventTypeName())){
            return;
        }
    }


    private boolean shouldHandle(RuntimeEvent re, String e){
        return re!=null && !re.isProcessed() && !this.equals(re.getSource())
                && re.getData().getEventTypeName().equals(e)
                && RuntimeEventData.Origin.ORCHESTRATOR.equals(re.getData().getOrigin());
    }

    private boolean shouldHandle(ComponentEvent ce, String e){
        return ce!=null && !ce.isProcessed() && !this.equals(ce.getSource())
                && ce.getData().getEventTypeName().equals(e)
                && RuntimeEventData.Origin.COMPONENT.equals(ce.getData().getOrigin());
    }
}
