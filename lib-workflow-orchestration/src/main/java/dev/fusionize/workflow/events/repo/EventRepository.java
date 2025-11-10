package dev.fusionize.workflow.events.repo;

import dev.fusionize.workflow.events.Event;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EventRepository extends MongoRepository<Event, String>{
    Event save(Event event);
    Optional<Event> findByEventId(String id);
    List<Event> findByCorrelationId(String id);
    List<Event> findByCausationId(String id);
}

