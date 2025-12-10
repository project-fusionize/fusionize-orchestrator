package dev.fusionize.common.payload;

import java.util.Map;

public class ServicePayload<T> {
    private ServiceResponse<T> response;
    private Map<String, Object> headers;
    private boolean test;

    public ServicePayload() {}

    private ServicePayload(Builder<T> builder) {
        this.response = builder.response;
        this.headers = builder.headers;
        this.test = builder.test;
    }

    public ServiceResponse<T> getResponse() {
        return response;
    }

    public Map<String, Object> getHeaders() {
        return headers;
    }

    public boolean isTest() {
        return test;
    }

    public void setResponse(ServiceResponse<T> response) {
        this.response = response;
    }

    public void setHeaders(Map<String, Object> headers) {
        this.headers = headers;
    }

    public void setTest(boolean test) {
        this.test = test;
    }

    public static class Builder<T> {
        private ServiceResponse<T> response;
        private Map<String, Object> headers;
        private boolean test;

        public Builder<T> response(ServiceResponse<T> response) {
            this.response = response;
            return this;
        }

        public Builder<T> headers(Map<String, Object> headers) {
            this.headers = headers;
            return this;
        }

        public Builder<T> test(boolean test) {
            this.test = test;
            return this;
        }

        public ServicePayload<T> build() {
            return new ServicePayload<>(this);
        }
    }
}
