package dev.fusionize.worker;

import dev.fusionize.workflow.events.Event;
import dev.fusionize.workflow.events.EventStore;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

public class WorkerAmqpAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(WorkerAmqpAutoConfiguration.class, RabbitAutoConfiguration.class));

    @Test
    public void testConnectionFactoryConfiguration() {
        this.contextRunner
                .withBean(EventStore.class, () -> new EventStore<Event>() {
                    @Override
                    public void save(Event event) {
                    }

                    @Override
                    public java.util.Optional<Event> findByEventId(String eventId) {
                        return java.util.Optional.empty();
                    }

                    @Override
                    public java.util.List<Event> findByCausationId(String causationId) {
                        return java.util.Collections.emptyList();
                    }

                    @Override
                    public java.util.List<Event> findByCorrelationId(String correlationId) {
                        return java.util.Collections.emptyList();
                    }
                })
                .withPropertyValues(
                        "fusionize.worker.orchestrator-amqp=amqp://guest:guest@localhost:5672",
                        "spring.rabbitmq.listener.simple.auto-startup=false")
                .run((context) -> {
                    assertThat(context).hasSingleBean(ConnectionFactory.class);
                    ConnectionFactory connectionFactory = context.getBean(ConnectionFactory.class);
                    assertThat(connectionFactory.getHost()).isEqualTo("localhost");
                    assertThat(connectionFactory.getPort()).isEqualTo(5672);
                    assertThat(connectionFactory.getUsername()).isEqualTo("guest");

                    assertThat(context).hasSingleBean(MessageConverter.class);
                    assertThat(context).hasBean("fusionizeWorkflowEventsExchange");
                    assertThat(context).hasBean("fusionizeWorkerEventsQueue");
                    assertThat(context).hasBean("eventPublisher");
                    assertThat(context).hasBean("eventListener");
                });
    }
}
