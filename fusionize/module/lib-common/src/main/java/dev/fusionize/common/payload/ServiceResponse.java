package dev.fusionize.common.payload;

import java.util.Date;

public class ServiceResponse<T> {
    private Date time;
    private int status;
    private T message;

    public ServiceResponse() {
        this.time = new Date();
    }

    private ServiceResponse(Builder<T> builder) {
        this.time = builder.time != null ? builder.time : new Date();
        this.status = builder.status;
        this.message = builder.message;
    }

    public Date getTime() {
        return time;
    }

    public int getStatus() {
        return status;
    }

    public T getMessage() {
        return message;
    }

    public void setTime(Date time) {
        this.time = time;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public void setMessage(T message) {
        this.message = message;
    }

    public static class Builder<T> {
        private Date time;
        private int status;
        private T message;

        public Builder<T> time(Date time) {
            this.time = time;
            return this;
        }

        public Builder<T> status(int status) {
            this.status = status;
            return this;
        }

        public Builder<T> message(T message) {
            this.message = message;
            return this;
        }

        public ServiceResponse<T> build() {
            return new ServiceResponse<>(this);
        }
    }
}
