package dev.fusionize.storage.vector;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;

import java.util.List;

import org.springframework.beans.factory.DisposableBean;

public interface VectorStorageService extends DisposableBean {
    void add(List<Document> documents);

    List<Document> search(String query);

    void delete(List<String> ids);

    VectorStore getVectorStore();
}
