package dev.fusionize.workflow.events;


public abstract class EventPublisher<E extends Event> {
    private final EventStore<E> eventStore;

    protected EventPublisher(EventStore<E> eventStore) {
        this.eventStore = eventStore;
    }

    public void publish(E event) {
        eventStore.save(event);
    }

}
