package dev.fusionize.workflow.events;

public interface EventPublisher<E extends Event> {
    void publish(E event);
}
