package dev.fusionize.orchestrator.storage;

import dev.fusionize.common.exception.ExceptionResponse;
import dev.fusionize.storage.exception.StorageConnectionException;
import dev.fusionize.storage.exception.StorageDomainAlreadyExistsException;
import dev.fusionize.storage.exception.StorageException;
import dev.fusionize.storage.exception.StorageNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class StorageControllerAdvisor {

    @ExceptionHandler(StorageNotFoundException.class)
    public ResponseEntity<ExceptionResponse> handleNotFound(StorageNotFoundException ex) {
        return buildResponse(ex, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(StorageDomainAlreadyExistsException.class)
    public ResponseEntity<ExceptionResponse> handleDomainExists(StorageDomainAlreadyExistsException ex) {
        return buildResponse(ex, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ExceptionResponse> handleBadRequest(IllegalArgumentException ex) {
        return buildResponse(ex, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(StorageConnectionException.class)
    public ResponseEntity<ExceptionResponse> handleConnectionError(StorageConnectionException ex) {
        return buildResponse(ex, HttpStatus.BAD_GATEWAY);
    }

    @ExceptionHandler(StorageException.class)
    public ResponseEntity<ExceptionResponse> handleGeneric(StorageException ex) {
        return buildResponse(ex, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private ResponseEntity<ExceptionResponse> buildResponse(Exception ex, HttpStatus status) {
        ExceptionResponse response = ExceptionResponse.builder(ex)
                .withHttpStatus(status.value())
                .build();
        return new ResponseEntity<>(response, status);
    }
}
