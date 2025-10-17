package dev.fusionize.common.test;


import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.data.mongo.AutoConfigureDataMongo;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.IndexOperations;
import org.springframework.data.mongodb.core.index.IndexResolver;
import org.springframework.data.mongodb.core.index.MongoPersistentEntityIndexResolver;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.MongoPersistentEntity;
import org.springframework.data.mongodb.core.mapping.MongoPersistentProperty;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.test.annotation.DirtiesContext;


@AutoConfigureDataMongo
@EnableAutoConfiguration
@DirtiesContext
@EnableMongoRepositories(basePackages = {
        "dev.fusionize"
}, repositoryImplementationPostfix = "CustomImplementation")
public record TestMongoConfig(MongoTemplate mongoTemplate) {
        @EventListener(ContextRefreshedEvent.class)
        public void initIndicesAfterStartup() {
                MappingContext<? extends MongoPersistentEntity<?>, MongoPersistentProperty> mappingContext = mongoTemplate
                        .getConverter().getMappingContext();
                IndexResolver resolver = new MongoPersistentEntityIndexResolver(mappingContext);
                mappingContext.getPersistentEntities()
                        .stream()
                        .filter(it -> it.isAnnotationPresent(Document.class))
                        .forEach(it -> {
                                IndexOperations indexOps = mongoTemplate.indexOps(it.getType());
                                resolver.resolveIndexFor(it.getType()).forEach(indexOps::createIndex);
                        });
        }
}
