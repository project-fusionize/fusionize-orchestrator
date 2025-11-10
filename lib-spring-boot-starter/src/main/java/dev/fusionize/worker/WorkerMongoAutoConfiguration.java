package dev.fusionize.worker;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import org.bson.BsonReader;
import org.bson.BsonWriter;
import org.bson.UuidRepresentation;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.mongodb.config.AbstractMongoClientConfiguration;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Configuration
@Profile("!ut")
@EnableMongoRepositories(basePackages = {
        "dev.fusionize.workflow"
}, repositoryImplementationPostfix = "CustomImplementation", mongoTemplateRef = "workerMongoTemplate")
@ConditionalOnClass(Worker.class)
@EnableConfigurationProperties(WorkerProperties.class)
public class WorkerMongoAutoConfiguration extends AbstractMongoClientConfiguration {
    private final ConnectionString connectionString;

    public WorkerMongoAutoConfiguration(WorkerProperties workerProperties) {
        this.connectionString = new ConnectionString(workerProperties.getOrchestratorMongo());
    }

    @Bean
    @ConditionalOnMissingBean(name = "workerMongoTemplate")
    public MongoTemplate workerMongoTemplate() {
        return new MongoTemplate(mongoClient(), getDatabaseName());
    }

    @Override
    protected void configureClientSettings(MongoClientSettings.Builder builder) {
        // Register the ZonedDateTime codec at the driver level
        builder.applyConnectionString(connectionString)
                .uuidRepresentation(UuidRepresentation.STANDARD);
    }

    @Override
    protected String getDatabaseName() {
        return connectionString.getDatabase();
    }

    @Override
    protected boolean autoIndexCreation() {
        return true;
    }
}