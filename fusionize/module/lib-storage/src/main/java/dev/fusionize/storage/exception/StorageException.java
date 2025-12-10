package dev.fusionize.storage.exception;

import dev.fusionize.common.exception.ApplicationException;

public class StorageException extends ApplicationException {
    public StorageException(String message) {
        super(new Exception(message));
    }

    public StorageException(String message, Throwable cause) {
        super(new Exception(message, cause));
    }
}
