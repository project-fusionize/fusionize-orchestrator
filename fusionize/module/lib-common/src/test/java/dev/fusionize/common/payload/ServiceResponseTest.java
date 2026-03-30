package dev.fusionize.common.payload;

import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

class ServiceResponseTest {

    @Test
    void shouldCreateWithDefaultTime() {
        // setup
        var before = new Date();

        // expectation
        var response = new ServiceResponse<String>();

        // validation
        assertThat(response.getTime()).isNotNull();
        assertThat(response.getTime()).isAfterOrEqualTo(before);
        assertThat(response.getStatus()).isZero();
        assertThat(response.getMessage()).isNull();
    }

    @Test
    void shouldBuildWithAllFields() {
        // setup
        var time = new Date(1000L);
        var status = 200;
        var message = "success";

        // expectation
        var response = new ServiceResponse.Builder<String>()
                .time(time)
                .status(status)
                .message(message)
                .build();

        // validation
        assertThat(response.getTime()).isEqualTo(time);
        assertThat(response.getStatus()).isEqualTo(status);
        assertThat(response.getMessage()).isEqualTo(message);
    }

    @Test
    void shouldBuildWithNullTime_defaultsToNow() {
        // setup
        var before = new Date();

        // expectation
        var response = new ServiceResponse.Builder<String>()
                .time(null)
                .status(404)
                .message("not found")
                .build();

        // validation
        assertThat(response.getTime()).isNotNull();
        assertThat(response.getTime()).isAfterOrEqualTo(before);
        assertThat(response.getStatus()).isEqualTo(404);
        assertThat(response.getMessage()).isEqualTo("not found");
    }

    @Test
    void shouldSetAndGetAllFields() {
        // setup
        var response = new ServiceResponse<Integer>();
        var time = new Date(5000L);

        // expectation
        response.setTime(time);
        response.setStatus(500);
        response.setMessage(42);

        // validation
        assertThat(response.getTime()).isEqualTo(time);
        assertThat(response.getStatus()).isEqualTo(500);
        assertThat(response.getMessage()).isEqualTo(42);
    }
}
