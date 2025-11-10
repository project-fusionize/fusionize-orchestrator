package dev.fusionize.workflow.events;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class EventRouter {
    public static Logger logger = LoggerFactory.getLogger(EventRouter.class);
    private final Map<Class<? extends Event>, List<EventHandler<? extends Event>>> handlersByType = new HashMap<>();
    private final EventPublisher<Event> eventPublisher;
    private final EventStore<Event> eventStore;

    @Autowired
    public EventRouter(List<EventHandler<?>> handlers,
                       EventPublisher<Event> eventPublisher,
                       EventStore<Event> eventStore, EventListener<Event> eventListener) {
        this.eventPublisher = eventPublisher;
        this.eventStore = eventStore;
        handlersByType.putAll(
                handlers.stream()
                        .collect(Collectors.groupingBy(EventHandler::getEventType))
        );
        eventListener.addListener(this::handleEvent);
    }

    @SuppressWarnings("unchecked")
    public <E extends Event> void handleEvent(E event) {
        List<EventHandler<? extends Event>> handlers = handlersByType.get(event.getClass());
        if (handlers == null || handlers.isEmpty()) {
            logger.error("Event Handler Not Found Exception", new EventHandlerNotFoundException());
        }

        for (EventHandler<? extends Event> handler : handlers) {
            EventHandler<E> typedHandler = (EventHandler<E>) handler;
            if (typedHandler.shouldHandle(event)) {
                Event outgoing = typedHandler.handle(event);
                event.setProcessedDate(new Date());
                eventStore.save(event);
                if(outgoing != null) {
                    eventPublisher.publish(outgoing);
                }
            }
        }
    }

}
