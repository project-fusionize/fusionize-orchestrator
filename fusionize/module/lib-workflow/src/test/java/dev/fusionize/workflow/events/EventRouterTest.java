package dev.fusionize.workflow.events;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventRouterTest {

    static class TestEvent extends Event {
        // minimal concrete event for testing
    }

    @Mock
    private EventHandler<TestEvent> handler;

    @Mock
    private EventPublisher<Event> eventPublisher;

    @Mock
    private EventStore<Event> eventStore;

    @Mock
    private EventListener<Event> eventListener;

    private EventRouter eventRouter;

    @BeforeEach
    void setUp() {
        // setup
        lenient().when(handler.getEventType()).thenReturn(TestEvent.class);

        // expectation
        eventRouter = new EventRouter(List.of(handler), eventPublisher, eventStore, eventListener);
    }

    @Test
    void shouldRegisterHandlersGroupedByEventType() throws Exception {
        // setup
        @SuppressWarnings("unchecked")
        EventHandler<TestEvent> handler1 = mock(EventHandler.class);
        @SuppressWarnings("unchecked")
        EventHandler<TestEvent> handler2 = mock(EventHandler.class);
        when(handler1.getEventType()).thenReturn(TestEvent.class);
        when(handler2.getEventType()).thenReturn(TestEvent.class);

        // expectation
        var router = new EventRouter(List.of(handler1, handler2), eventPublisher, eventStore, eventListener);

        // validation
        // Verify both handlers are invoked when an event of their type is routed
        var event = new TestEvent();
        when(handler1.shouldHandle(event)).thenReturn(true);
        when(handler2.shouldHandle(event)).thenReturn(true);
        when(handler1.handle(event)).thenReturn(null);
        when(handler2.handle(event)).thenReturn(null);

        router.handleEvent(event);

        verify(handler1).handle(event);
        verify(handler2).handle(event);
    }

    @Test
    void shouldRegisterListenerOnEventListener() {
        // setup — handled in @BeforeEach

        // expectation — constructor should call addListener

        // validation
        verify(eventListener).addListener(any());
    }

    @Test
    void shouldRouteEventToMatchingHandler() throws Exception {
        // setup
        var event = new TestEvent();
        when(handler.shouldHandle(event)).thenReturn(true);
        when(handler.handle(event)).thenReturn(null);

        // expectation
        eventRouter.handleEvent(event);

        // validation
        verify(handler).handle(event);
        verify(eventStore).save(event);
    }

    @Test
    void shouldPublishOutgoingEvent_whenHandlerReturnsEvent() throws Exception {
        // setup
        var event = new TestEvent();
        var outgoingEvent = new TestEvent();
        when(handler.shouldHandle(event)).thenReturn(true);
        when(handler.handle(event)).thenReturn(outgoingEvent);

        // expectation
        eventRouter.handleEvent(event);

        // validation
        verify(eventPublisher).publish(outgoingEvent);
    }

    @Test
    void shouldNotPublishOutgoingEvent_whenHandlerReturnsNull() throws Exception {
        // setup
        var event = new TestEvent();
        when(handler.shouldHandle(event)).thenReturn(true);
        when(handler.handle(event)).thenReturn(null);

        // expectation
        eventRouter.handleEvent(event);

        // validation
        verify(eventPublisher, never()).publish(any());
    }

    @Test
    void shouldSetProcessedDateOnEvent_afterHandling() throws Exception {
        // setup
        var event = new TestEvent();
        assertNull(event.getProcessedDate());
        when(handler.shouldHandle(event)).thenReturn(true);
        when(handler.handle(event)).thenReturn(null);

        // expectation
        eventRouter.handleEvent(event);

        // validation
        assertNotNull(event.getProcessedDate());
    }

    @Test
    void shouldSkipHandler_whenShouldHandleReturnsFalse() throws Exception {
        // setup
        var event = new TestEvent();
        when(handler.shouldHandle(event)).thenReturn(false);

        // expectation
        eventRouter.handleEvent(event);

        // validation
        verify(handler, never()).handle(any());
    }

    @Test
    void shouldHandleExceptionFromHandler_withoutThrowing() throws Exception {
        // setup
        var event = new TestEvent();
        when(handler.shouldHandle(event)).thenReturn(true);
        when(handler.handle(event)).thenThrow(new RuntimeException("handler failure"));

        // expectation & validation
        assertDoesNotThrow(() -> eventRouter.handleEvent(event));
    }

    @Test
    void shouldCallMultipleHandlers_forSameEventType() throws Exception {
        // setup
        @SuppressWarnings("unchecked")
        EventHandler<TestEvent> handler1 = mock(EventHandler.class);
        @SuppressWarnings("unchecked")
        EventHandler<TestEvent> handler2 = mock(EventHandler.class);
        when(handler1.getEventType()).thenReturn(TestEvent.class);
        when(handler2.getEventType()).thenReturn(TestEvent.class);

        var router = new EventRouter(List.of(handler1, handler2), eventPublisher, eventStore, eventListener);

        var event = new TestEvent();
        when(handler1.shouldHandle(event)).thenReturn(true);
        when(handler2.shouldHandle(event)).thenReturn(true);
        when(handler1.handle(event)).thenReturn(null);
        when(handler2.handle(event)).thenReturn(null);

        // expectation
        router.handleEvent(event);

        // validation
        verify(handler1).handle(event);
        verify(handler2).handle(event);
        verify(eventStore, times(2)).save(event);
    }
}
