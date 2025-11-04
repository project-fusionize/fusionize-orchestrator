package dev.fusionize.worker.oidc;
import dev.fusionize.common.exception.ApplicationException;

public class OidcTokenClientException extends ApplicationException {
    public OidcTokenClientException() {
        super();
    }

    public OidcTokenClientException(String message) {
        super(message);
    }

    public OidcTokenClientException(String message, Throwable cause) {
        super(message, cause);
    }

    public OidcTokenClientException(Throwable cause) {
        super(cause);
    }
}
