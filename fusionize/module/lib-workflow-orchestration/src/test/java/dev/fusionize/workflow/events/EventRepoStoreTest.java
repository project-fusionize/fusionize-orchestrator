package dev.fusionize.workflow.events;

import dev.fusionize.workflow.events.repo.EventRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventRepoStoreTest {

    @Mock
    private EventRepository eventRepository;

    @InjectMocks
    private EventRepoStore eventRepoStore;

    static class TestEvent extends Event {}

    @Test
    void shouldSaveEvent() {
        // setup
        var event = new TestEvent();

        // expectation
        eventRepoStore.save(event);

        // validation
        verify(eventRepository).save(event);
    }

    @Test
    void shouldNotThrow_whenSaveThrowsException() {
        // setup
        var event = new TestEvent();

        // expectation
        when(eventRepository.save(event)).thenThrow(new RuntimeException("save failed"));

        // validation
        eventRepoStore.save(event);
    }

    @Test
    void shouldFindByEventId() {
        // setup
        var event = new TestEvent();
        var eventId = "event-123";

        // expectation
        when(eventRepository.findByEventId(eventId)).thenReturn(Optional.of(event));

        // validation
        var result = eventRepoStore.findByEventId(eventId);
        assertThat(result).isPresent().contains(event);
    }

    @Test
    void shouldReturnEmpty_whenFindByEventIdThrowsException() {
        // setup
        var eventId = "event-123";

        // expectation
        when(eventRepository.findByEventId(eventId)).thenThrow(new RuntimeException("find failed"));

        // validation
        var result = eventRepoStore.findByEventId(eventId);
        assertThat(result).isEmpty();
    }

    @Test
    void shouldFindByCausationId() {
        // setup
        var event = new TestEvent();
        var causationId = "causation-123";

        // expectation
        when(eventRepository.findByCausationId(causationId)).thenReturn(List.of(event));

        // validation
        var result = eventRepoStore.findByCausationId(causationId);
        assertThat(result).hasSize(1).contains(event);
    }

    @Test
    void shouldReturnEmptyList_whenFindByCausationIdThrowsException() {
        // setup
        var causationId = "causation-123";

        // expectation
        when(eventRepository.findByCausationId(causationId)).thenThrow(new RuntimeException("find failed"));

        // validation
        var result = eventRepoStore.findByCausationId(causationId);
        assertThat(result).isEmpty();
    }

    @Test
    void shouldFindByCorrelationId() {
        // setup
        var event = new TestEvent();
        var correlationId = "correlation-123";

        // expectation
        when(eventRepository.findByCorrelationId(correlationId)).thenReturn(List.of(event));

        // validation
        var result = eventRepoStore.findByCorrelationId(correlationId);
        assertThat(result).hasSize(1).contains(event);
    }

    @Test
    void shouldReturnEmptyList_whenFindByCorrelationIdThrowsException() {
        // setup
        var correlationId = "correlation-123";

        // expectation
        when(eventRepository.findByCorrelationId(correlationId)).thenThrow(new RuntimeException("find failed"));

        // validation
        var result = eventRepoStore.findByCorrelationId(correlationId);
        assertThat(result).isEmpty();
    }
}
