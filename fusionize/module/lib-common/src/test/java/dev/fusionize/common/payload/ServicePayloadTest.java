package dev.fusionize.common.payload;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ServicePayloadTest {

    @Test
    void shouldCreateEmpty() {
        // setup
        // expectation
        var payload = new ServicePayload<String>();

        // validation
        assertThat(payload.getResponse()).isNull();
        assertThat(payload.getHeaders()).isNull();
        assertThat(payload.isTest()).isFalse();
    }

    @Test
    void shouldBuildWithResponse() {
        // setup
        var response = new ServiceResponse.Builder<String>()
                .status(200)
                .message("ok")
                .build();

        // expectation
        var payload = new ServicePayload.Builder<String>()
                .response(response)
                .build();

        // validation
        assertThat(payload.getResponse()).isEqualTo(response);
        assertThat(payload.getResponse().getStatus()).isEqualTo(200);
        assertThat(payload.getResponse().getMessage()).isEqualTo("ok");
    }

    @Test
    void shouldBuildWithHeaders() {
        // setup
        var headers = Map.<String, Object>of("Content-Type", "application/json", "X-Request-Id", "abc123");

        // expectation
        var payload = new ServicePayload.Builder<String>()
                .headers(headers)
                .build();

        // validation
        assertThat(payload.getHeaders()).containsEntry("Content-Type", "application/json");
        assertThat(payload.getHeaders()).containsEntry("X-Request-Id", "abc123");
        assertThat(payload.getHeaders()).hasSize(2);
    }

    @Test
    void shouldBuildWithTestFlag() {
        // setup
        // expectation
        var payload = new ServicePayload.Builder<String>()
                .test(true)
                .build();

        // validation
        assertThat(payload.isTest()).isTrue();
    }

    @Test
    void shouldSetAndGetAllFields() {
        // setup
        var payload = new ServicePayload<String>();
        var response = new ServiceResponse.Builder<String>()
                .status(201)
                .message("created")
                .build();
        var headers = Map.<String, Object>of("Authorization", "Bearer token");

        // expectation
        payload.setResponse(response);
        payload.setHeaders(headers);
        payload.setTest(true);

        // validation
        assertThat(payload.getResponse()).isEqualTo(response);
        assertThat(payload.getHeaders()).containsEntry("Authorization", "Bearer token");
        assertThat(payload.isTest()).isTrue();
    }
}
