package dev.fusionize.workflow.events;


public interface EventListener<E extends Event>  {
    @FunctionalInterface
    interface EventCallback<E extends Event> {
        void onEvent(E event);
    }
    void addListener(EventCallback<E> callback);
}
