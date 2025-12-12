package dev.fusionize.workflow.events;

import dev.fusionize.workflow.events.repo.EventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
public class EventRepoStore implements EventStore<Event> {
    public static Logger logger = LoggerFactory.getLogger(EventRepoStore.class);

    private final EventRepository eventRepository;

    public EventRepoStore(EventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    @Override
    public void save(Event event) {
        try {
            eventRepository.save(event);
        } catch (Exception e) {
            logger.error(e.getMessage(),e);
        }

    }

    @Override
    public Optional<Event> findByEventId(String eventId) {
        try {
            return eventRepository.findByEventId(eventId);

        } catch (Exception e) {
            logger.error(e.getMessage(),e);
            return Optional.empty();
        }
    }

    @Override
    public List<Event> findByCausationId(String causationId) {
        try {
            return eventRepository.findByCausationId(causationId);

        } catch (Exception e) {
            logger.error(e.getMessage(),e);
            return new ArrayList<>();
        }
    }

    @Override
    public List<Event> findByCorrelationId(String correlationId) {
        try {
            return eventRepository.findByCorrelationId(correlationId);

        } catch (Exception e) {
            logger.error(e.getMessage(),e);
            return new ArrayList<>();
        }
    }
}
