package dev.fusionize.storage.vector;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
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
}
