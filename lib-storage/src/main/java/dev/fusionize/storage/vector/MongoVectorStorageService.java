package dev.fusionize.storage.vector;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import dev.fusionize.storage.StorageConfig;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.mongodb.atlas.MongoDBAtlasVectorStore;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory;

import java.util.List;

public class MongoVectorStorageService implements VectorStorageService {

    private final MongoDBAtlasVectorStore vectorStore;
    private final MongoClient mongoClient;

    public MongoVectorStorageService(String uri, String databaseName, String collectionName, String vectorIndexName,
            String pathName, List<String> metadataFields, EmbeddingModel embeddingModel) {

        this.mongoClient = MongoClients.create(uri);
        var mongoTemplate = new MongoTemplate(new SimpleMongoClientDatabaseFactory(this.mongoClient, databaseName));

        this.vectorStore = MongoDBAtlasVectorStore.builder(mongoTemplate, embeddingModel)
                .collectionName(collectionName)
                .vectorIndexName(vectorIndexName)
                .pathName(pathName)
                .metadataFieldsToFilter(metadataFields)
                .initializeSchema(true)
                .build();
    }

    @Override
    public void add(List<Document> documents) {
        vectorStore.add(documents);
    }

    @Override
    public List<Document> search(String query) {
        return vectorStore.similaritySearch(query);
    }

    @Override
    public void delete(List<String> ids) {
        vectorStore.delete(ids);
    }

    @Override
    public VectorStore getVectorStore() {
        return vectorStore;
    }

    @Override
    public void destroy() throws Exception {
        if (mongoClient != null) {
            mongoClient.close();
        }
    }

    @SuppressWarnings("unchecked")
    public static MongoVectorStorageService instantiate(StorageConfig config,
                                                        EmbeddingModel embeddingModel) {
        String uri = (String) config.getSecrets().get("uri");
        String databaseName = (String) config.getProperties().get("databaseName");
        String collectionName = (String) config.getProperties().get("collectionName");
        String vectorIndexName = (String) config.getProperties().get("vectorIndexName");
        String pathName = (String) config.getProperties().get("pathName");
        List<String> metadataFields = (List<String>) config.getProperties().get("metadataFields");

        return new MongoVectorStorageService(uri, databaseName, collectionName, vectorIndexName, pathName,
                metadataFields, embeddingModel);
    }
}
