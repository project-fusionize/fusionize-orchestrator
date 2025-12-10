package dev.fusionize.worker;

import dev.fusionize.workflow.events.Event;
import dev.fusionize.workflow.events.EventListener;
import dev.fusionize.workflow.events.EventPublisher;

import dev.fusionize.workflow.events.EventStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.util.ArrayList;
import java.util.List;

@Configuration
@Profile("!ut")
@ConditionalOnClass(Worker.class)
@EnableConfigurationProperties(WorkerProperties.class)
public class WorkerAmqpAutoConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(WorkerAmqpAutoConfiguration.class);

    public static final String FUSIONIZE_WORKFLOW_EVENTS_EXCHANGE = "fusionize.workflow.events";
    public static final String FUSIONIZE_WORKER_EVENTS_QUEUE = "fusionize.worker.events";

    @Bean
    public ConnectionFactory connectionFactory(WorkerProperties properties) {
        final CachingConnectionFactory connectionFactory = new CachingConnectionFactory();
        connectionFactory.setUri(properties.getOrchestratorAmqp());
        return connectionFactory;
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public TopicExchange fusionizeWorkflowEventsExchange() {
        return new TopicExchange(FUSIONIZE_WORKFLOW_EVENTS_EXCHANGE);
    }

    @Bean
    public Queue fusionizeWorkerEventsQueue() {
        return new Queue(FUSIONIZE_WORKER_EVENTS_QUEUE);
    }

    @Bean
    public Binding binding(Queue fusionizeWorkerEventsQueue, TopicExchange fusionizeWorkflowEventsExchange) {
        return BindingBuilder.bind(fusionizeWorkerEventsQueue).to(fusionizeWorkflowEventsExchange).with("#");
    }

    @Bean
    public EventPublisher<Event> eventPublisher(RabbitTemplate rabbitTemplate, EventStore<Event> eventStore) {
        return new EventPublisher<>(eventStore) {
            @Override
            public void publish(Event event) {
                super.publish(event);
                logger.debug("Publishing event: eventClass={}, eventId={}, causationId={}, correlationId={}",
                        event.getEventClass(), event.getEventId(), event.getCausationId(), event.getCorrelationId());
                rabbitTemplate.convertAndSend(FUSIONIZE_WORKFLOW_EVENTS_EXCHANGE, event.getClass().getName(), event);
            }
        };
    }

    @Bean
    public AmqpEventListener eventListener() {
        return new AmqpEventListener();
    }

    public static class AmqpEventListener implements EventListener<Event> {

        private static final Logger logger = LoggerFactory.getLogger(AmqpEventListener.class);

        private final List<EventCallback<Event>> callbacks = new ArrayList<>();

        @Override
        public void addListener(EventCallback<Event> callback) {
            callbacks.add(callback);
        }

        @RabbitListener(queues = FUSIONIZE_WORKER_EVENTS_QUEUE)
        public void onEvent(Event event) {
            logger.debug("Received event: eventClass={}, eventId={}, causationId={}, correlationId={}",
                    event.getEventClass(), event.getEventId(), event.getCausationId(), event.getCorrelationId());
            callbacks.forEach(c -> c.onEvent(event));
        }
    }
}
