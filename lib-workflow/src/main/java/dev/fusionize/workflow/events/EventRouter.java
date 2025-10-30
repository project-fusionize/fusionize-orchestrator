package dev.fusionize.workflow.events;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class EventRouter {
    private final Map<Class<? extends Event>, List<EventHandler<? extends Event>>> handlersByType = new HashMap<>();
    private final EventPublisher<Event> eventPublisher;
    private final EventStore<Event> eventStore;

    @Autowired
    public EventRouter(List<EventHandler<?>> handlers,
                       EventPublisher<Event> eventPublisher,
                       EventStore<Event> eventStore) {
        this.eventPublisher = eventPublisher;
        this.eventStore = eventStore;
        handlersByType.putAll(
                handlers.stream()
                        .collect(Collectors.groupingBy(EventHandler::getEventType))
        );
    }

    @SuppressWarnings("unchecked")
    @EventListener
    public <E extends Event> void handleEvent(E event) throws EventHandlerNotFoundException {
        eventStore.save(event);
        List<EventHandler<? extends Event>> handlers = handlersByType.get(event.getClass());
        if (handlers == null || handlers.isEmpty()) {
            throw new EventHandlerNotFoundException();
        }

        for (EventHandler<? extends Event> handler : handlers) {
            EventHandler<E> typedHandler = (EventHandler<E>) handler;
            if (typedHandler.shouldHandle(event)) {
                Event outgoing = typedHandler.handle(event);
                event.setProcessedDate(ZonedDateTime.now());
                eventStore.save(event);
                if(outgoing != null) {
                    eventPublisher.publish(outgoing);
                }
            }
        }
    }

    public <E extends Event> void publishEvent(E event) {
        eventStore.save(event);
        eventPublisher.publish(event);
    }
}
