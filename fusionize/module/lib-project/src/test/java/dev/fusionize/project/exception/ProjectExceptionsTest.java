package dev.fusionize.project.exception;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ProjectExceptionsTest {

    private static final String MESSAGE = "(p101) Test error message";
    private static final Throwable CAUSE = new RuntimeException("root cause");

    // ---- ProjectAccessException ----

    @Test
    void projectAccessException_noArg() {
        // setup
        var exception = new ProjectAccessException();

        // expectation
        var message = exception.getMessage();

        // validation
        assertThat(message).isNull();
        assertThat(exception.getCause()).isNull();
        assertThat(exception.getCode()).isEqualTo("u");
    }

    @Test
    void projectAccessException_message() {
        // setup
        var exception = new ProjectAccessException(MESSAGE);

        // expectation
        var message = exception.getMessage();

        // validation
        assertThat(message).isEqualTo(MESSAGE);
        assertThat(exception.getCode()).isEqualTo("p101");
        assertThat(exception.getError()).isEqualTo("Test error message");
    }

    @Test
    void projectAccessException_messageAndCause() {
        // setup
        var exception = new ProjectAccessException(MESSAGE, CAUSE);

        // expectation
        var cause = exception.getCause();

        // validation
        assertThat(exception.getMessage()).isEqualTo(MESSAGE);
        assertThat(cause).isEqualTo(CAUSE);
        assertThat(exception.getCode()).isEqualTo("p101");
    }

    @Test
    void projectAccessException_cause() {
        // setup
        var exception = new ProjectAccessException(CAUSE);

        // expectation
        var cause = exception.getCause();

        // validation
        assertThat(cause).isEqualTo(CAUSE);
        assertThat(exception.getCode()).isEqualTo("u");
    }

    // ---- ProjectNotExistException ----

    @Test
    void projectNotExistException_noArg() {
        // setup
        var exception = new ProjectNotExistException();

        // expectation
        var message = exception.getMessage();

        // validation
        assertThat(message).isNull();
        assertThat(exception.getCause()).isNull();
        assertThat(exception.getCode()).isEqualTo("u");
    }

    @Test
    void projectNotExistException_message() {
        // setup
        var exception = new ProjectNotExistException(MESSAGE);

        // expectation
        var message = exception.getMessage();

        // validation
        assertThat(message).isEqualTo(MESSAGE);
        assertThat(exception.getCode()).isEqualTo("p101");
        assertThat(exception.getError()).isEqualTo("Test error message");
    }

    @Test
    void projectNotExistException_messageAndCause() {
        // setup
        var exception = new ProjectNotExistException(MESSAGE, CAUSE);

        // expectation
        var cause = exception.getCause();

        // validation
        assertThat(exception.getMessage()).isEqualTo(MESSAGE);
        assertThat(cause).isEqualTo(CAUSE);
        assertThat(exception.getCode()).isEqualTo("p101");
    }

    @Test
    void projectNotExistException_cause() {
        // setup
        var exception = new ProjectNotExistException(CAUSE);

        // expectation
        var cause = exception.getCause();

        // validation
        assertThat(cause).isEqualTo(CAUSE);
        assertThat(exception.getCode()).isEqualTo("u");
    }

    // ---- ProjectOperationException ----

    @Test
    void projectOperationException_noArg() {
        // setup
        var exception = new ProjectOperationException();

        // expectation
        var message = exception.getMessage();

        // validation
        assertThat(message).isNull();
        assertThat(exception.getCause()).isNull();
        assertThat(exception.getCode()).isEqualTo("u");
    }

    @Test
    void projectOperationException_message() {
        // setup
        var exception = new ProjectOperationException(MESSAGE);

        // expectation
        var message = exception.getMessage();

        // validation
        assertThat(message).isEqualTo(MESSAGE);
        assertThat(exception.getCode()).isEqualTo("p101");
        assertThat(exception.getError()).isEqualTo("Test error message");
    }

    @Test
    void projectOperationException_messageAndCause() {
        // setup
        var exception = new ProjectOperationException(MESSAGE, CAUSE);

        // expectation
        var cause = exception.getCause();

        // validation
        assertThat(exception.getMessage()).isEqualTo(MESSAGE);
        assertThat(cause).isEqualTo(CAUSE);
        assertThat(exception.getCode()).isEqualTo("p101");
    }

    @Test
    void projectOperationException_cause() {
        // setup
        var exception = new ProjectOperationException(CAUSE);

        // expectation
        var cause = exception.getCause();

        // validation
        assertThat(cause).isEqualTo(CAUSE);
        assertThat(exception.getCode()).isEqualTo("u");
    }

    // ---- ProjectValidationException ----

    @Test
    void projectValidationException_noArg() {
        // setup
        var exception = new ProjectValidationException();

        // expectation
        var message = exception.getMessage();

        // validation
        assertThat(message).isNull();
        assertThat(exception.getCause()).isNull();
        assertThat(exception.getCode()).isEqualTo("u");
    }

    @Test
    void projectValidationException_message() {
        // setup
        var exception = new ProjectValidationException(MESSAGE);

        // expectation
        var message = exception.getMessage();

        // validation
        assertThat(message).isEqualTo(MESSAGE);
        assertThat(exception.getCode()).isEqualTo("p101");
        assertThat(exception.getError()).isEqualTo("Test error message");
    }

    @Test
    void projectValidationException_messageAndCause() {
        // setup
        var exception = new ProjectValidationException(MESSAGE, CAUSE);

        // expectation
        var cause = exception.getCause();

        // validation
        assertThat(exception.getMessage()).isEqualTo(MESSAGE);
        assertThat(cause).isEqualTo(CAUSE);
        assertThat(exception.getCode()).isEqualTo("p101");
    }

    @Test
    void projectValidationException_cause() {
        // setup
        var exception = new ProjectValidationException(CAUSE);

        // expectation
        var cause = exception.getCause();

        // validation
        assertThat(cause).isEqualTo(CAUSE);
        assertThat(exception.getCode()).isEqualTo("u");
    }
}
