package dev.fusionize.common.exception;

import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

class ExceptionResponseTest {

    @Test
    void shouldBuildFromApplicationException() {
        // setup
        var appException = new ApplicationException("(err1) something went wrong");
        appException.put("key1", "value1");

        // expectation
        var response = ExceptionResponse.builder(appException).build();

        // validation
        assertThat(response.getCode()).isEqualTo("err1");
        assertThat(response.getMessage()).isEqualTo("something went wrong");
        assertThat(response.getData()).containsEntry("key1", "value1");
        assertThat(response.getHttpStatus()).isEqualTo(500);
        assertThat(response.getTime()).isNotNull();
    }

    @Test
    void shouldBuildFromRegularException() {
        // setup
        var regularException = new RuntimeException("plain error message");

        // expectation
        var response = ExceptionResponse.builder(regularException).build();

        // validation
        assertThat(response.getCode()).isEqualTo("u");
        assertThat(response.getMessage()).isEqualTo("plain error message");
        assertThat(response.getHttpStatus()).isEqualTo(500);
        assertThat(response.getTime()).isNotNull();
    }

    @Test
    void shouldSetHttpStatus() {
        // setup
        var exception = new RuntimeException("not found");

        // expectation
        var response = ExceptionResponse.builder(exception)
                .withHttpStatus(404)
                .build();

        // validation
        assertThat(response.getHttpStatus()).isEqualTo(404);
    }

    @Test
    void shouldSetTime() {
        // setup
        var exception = new RuntimeException("error");
        var before = new Date();

        // expectation
        var response = ExceptionResponse.builder(exception).build();

        // validation
        assertThat(response.getTime()).isNotNull();
        assertThat(response.getTime()).isAfterOrEqualTo(before);
    }

    @Test
    void shouldReturnMeaningfulToString() {
        // setup
        var exception = new RuntimeException("some error");

        // expectation
        var response = ExceptionResponse.builder(exception)
                .withHttpStatus(400)
                .build();
        var result = response.toString();

        // validation
        assertThat(result).contains("STATUS:400");
        assertThat(result).contains("RuntimeException");
        assertThat(result).contains("some error");
    }

    @Test
    void shouldHandleNullException() {
        // setup
        var response = new ExceptionResponse();

        // expectation
        var result = response.toString();

        // validation
        assertThat(result).contains("Unknown");
    }
}
