package dev.fusionize.worker;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;
import org.springframework.data.mongodb.core.convert.NoOpDbRefResolver;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;

@Configuration
public class WorkerMongoTemplateConfiguration {
    private final MongoDatabaseFactory mongoDatabaseFactory;
    private final MongoMappingContext mongoMappingContext;
    private final MongoCustomConversions mongoCustomConversions;

    public WorkerMongoTemplateConfiguration(MongoDatabaseFactory mongoDatabaseFactory,
                                            MongoMappingContext mongoMappingContext,
                                            MongoCustomConversions mongoCustomConversions) {
        this.mongoDatabaseFactory = mongoDatabaseFactory;
        this.mongoMappingContext = mongoMappingContext;
        this.mongoCustomConversions = mongoCustomConversions;
    }

    @Bean
    public MongoTemplate workerMongoTemplate() {
        MappingMongoConverter converter = new MappingMongoConverter(
                NoOpDbRefResolver.INSTANCE, mongoMappingContext);
        converter.setCustomConversions(mongoCustomConversions);
        converter.afterPropertiesSet();
        return new MongoTemplate(mongoDatabaseFactory, converter);
    }

}
