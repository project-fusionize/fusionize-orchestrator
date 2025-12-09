package dev.fusionize.storage.exception;

public class StorageNotFoundException extends StorageException {
    public StorageNotFoundException(String domain) {
        super("Storage config not found for domain: " + domain);
    }
}
