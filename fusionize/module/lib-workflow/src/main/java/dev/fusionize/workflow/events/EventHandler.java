package dev.fusionize.workflow.events;

public interface EventHandler<E extends Event> {
    boolean shouldHandle(E event);
    Event handle(E event) throws Exception;
    Class<E> getEventType();
}
