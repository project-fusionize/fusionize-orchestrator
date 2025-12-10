package dev.fusionize.workflow.events;

import java.util.List;
import java.util.Optional;

public interface EventStore<T extends Event> {
    void save(T event);
    Optional<T> findByEventId(String eventId);
    List<T> findByCausationId(String causationId);
    List<T> findByCorrelationId(String correlationId);
}
