package dev.fusionize.storage.exception;

public class StorageDomainAlreadyExistsException extends StorageException {
    public StorageDomainAlreadyExistsException(String domain) {
        super("Storage config with domain " + domain + " already exists");
    }
}
