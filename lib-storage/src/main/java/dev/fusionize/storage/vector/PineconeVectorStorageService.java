package dev.fusionize.storage.vector;

import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.pinecone.PineconeVectorStore;

import java.util.List;

public class PineconeVectorStorageService implements VectorStorageService {

    private final PineconeVectorStore vectorStore;

    public PineconeVectorStorageService(String apiKey, String indexName,
            String namespace, EmbeddingModel embeddingModel) {
        this.vectorStore = PineconeVectorStore.builder(embeddingModel)
                .apiKey(apiKey)
                .indexName(indexName)
                .namespace(namespace)
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
        // No resources to close
    }
}
