package dev.fusionize.orchestrator.components;

import dev.fusionize.workflow.component.runtime.ComponentRuntimeConfig;
import dev.fusionize.workflow.component.runtime.EndComponentRuntime;
import dev.fusionize.workflow.events.Event;
import dev.fusionize.workflow.events.EventPublisher;
import dev.fusionize.workflow.events.runtime.ComponentActivatedEvent;
import dev.fusionize.workflow.events.runtime.ComponentFinishedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;

public class MyCustomComponentEnd extends EndComponentRuntime {
    private static final Logger logger = LoggerFactory.getLogger(MyCustomComponentEnd.class);


    MyCustomComponentEnd(EventPublisher<Event> eventPublisher) {
        super(eventPublisher);
    }

    @Override
    public void configure(ComponentRuntimeConfig config) {}

    @Override
    public void canActivate(ComponentActivatedEvent onActivate) {
        logger.info("MockEndEmailComponent activated");
        onActivate.setException(null);
        publish(onActivate);
    }

    @Override
    public void finish(ComponentFinishedEvent onFinish) {
        CompletableFuture.runAsync(() -> {
            try {
                Thread.sleep(10000);
                logger.info("ComponentFinishedEvent finished");
                publish(onFinish);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).whenComplete((result, throwable) -> {
            if (throwable != null) {
                logger.error(throwable.getMessage(), throwable);
            }
        });
    }
}
