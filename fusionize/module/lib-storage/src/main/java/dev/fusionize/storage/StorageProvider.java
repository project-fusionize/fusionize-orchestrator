package dev.fusionize.storage;

public enum StorageProvider {
    // Vector Storage Providers
    PINECONE(StorageType.VECTOR_STORAGE),
    MONGO_DB(StorageType.VECTOR_STORAGE),
    CHROMA_DB(StorageType.VECTOR_STORAGE),

    // File Storage Providers
    AWS_S3(StorageType.FILE_STORAGE),
    AZURE_BLOB(StorageType.FILE_STORAGE);

    private final StorageType type;

    StorageProvider(StorageType type) {
        this.type = type;
    }

    public StorageType getType() {
        return type;
    }
}
