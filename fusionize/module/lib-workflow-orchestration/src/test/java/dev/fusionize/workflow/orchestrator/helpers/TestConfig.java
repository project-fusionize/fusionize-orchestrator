package dev.fusionize.workflow.orchestrator.helpers;

import dev.fusionize.common.test.TestMongoConfig;
import dev.fusionize.common.test.TestMongoConversionConfig;
import dev.fusionize.workflow.events.Event;
import dev.fusionize.workflow.events.EventListener;
import dev.fusionize.workflow.events.EventPublisher;
import dev.fusionize.workflow.events.EventStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;
import org.springframework.data.mongodb.core.convert.NoOpDbRefResolver;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;

import java.util.ArrayList;
import java.util.List;

/**
 * -----------------------------
 * SPRING TEST CONFIGURATION
 * -----------------------------
 * This configuration defines a simple in-memory EventPublisher
 * for testing workflow orchestration without needing messaging infrastructure.
 */
@Configuration
@ComponentScan(basePackages = { "dev.fusionize.workflow", "dev.fusionize.process" })
@Import({
        TestMongoConfig.class,
        TestMongoConversionConfig.class
})
public class TestConfig {

    private final MongoDatabaseFactory mongoDatabaseFactory;
    private final MongoMappingContext mongoMappingContext;
    private final MongoCustomConversions mongoCustomConversions;

    public static Logger logger = LoggerFactory.getLogger(TestConfig.class);

    public TestConfig(MongoDatabaseFactory mongoDatabaseFactory,
                      MongoMappingContext mongoMappingContext,
                      MongoCustomConversions mongoCustomConversions) {
        this.mongoDatabaseFactory = mongoDatabaseFactory;
        this.mongoMappingContext = mongoMappingContext;
        this.mongoCustomConversions = mongoCustomConversions;
    }

    static final class WorkflowApplicationEvent extends ApplicationEvent {
        public final Event event;

        public WorkflowApplicationEvent(Object source, Event event) {
            super(source);
            this.event = event;
        }
    }

    /**
     * Simple adapter to use Spring's ApplicationEventPublisher
     * as the workflow's EventPublisher abstraction.
     */
    @Bean
    public EventPublisher<Event> eventPublisher(ApplicationEventPublisher eventPublisher,
                                                EventStore<Event> eventStore) {
        return new EventPublisher<>(eventStore) {
            @Override
            public void publish(Event event) {
                super.publish(event);
                eventPublisher.publishEvent(new WorkflowApplicationEvent(event.getSource(), event));
            }
        };
    }

    @Bean
    public EventListener<Event> eventListener() {
        return new EventListener<Event>() {
            final List<EventCallback<Event>> callbacks = new ArrayList<>();

            @Override
            public void addListener(EventCallback<Event> callback) {
                callbacks.add(callback);
            }

            @org.springframework.context.event.EventListener()
            public void onEvent(WorkflowApplicationEvent workflowApplicationEvent) {
                callbacks.forEach(c -> c.onEvent(workflowApplicationEvent.event));
            }
        };
    }

    @Bean
    public MongoTemplate workerMongoTemplate() {
        MappingMongoConverter converter = new MappingMongoConverter(
                NoOpDbRefResolver.INSTANCE, mongoMappingContext);
        converter.setCustomConversions(mongoCustomConversions);
        converter.afterPropertiesSet();
        return new MongoTemplate(mongoDatabaseFactory, converter);
    }

    @Bean
    public MongoTemplate mongoTemplate() {
        MappingMongoConverter converter = new MappingMongoConverter(
                NoOpDbRefResolver.INSTANCE, mongoMappingContext);
        converter.setCustomConversions(mongoCustomConversions);
        converter.afterPropertiesSet();
        return new MongoTemplate(mongoDatabaseFactory, converter);
    }
}
