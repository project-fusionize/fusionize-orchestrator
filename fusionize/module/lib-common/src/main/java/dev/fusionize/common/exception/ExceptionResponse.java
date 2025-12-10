package dev.fusionize.common.exception;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class ExceptionResponse {
    private Exception exception;
    private String message;
    private String code;
    private Map<String,Object> data = new HashMap<>();
    private int httpStatus;
    private Date time;

    public static Builder builder(Exception exception){
        return new Builder(exception);
    }
    public static class Builder {
        private final Exception exception;
        private int httpStatus = 500;

        public Builder(Exception exception) {
            this.exception = exception;
        }

        public Builder withHttpStatus(int httpStatus){
            this.httpStatus = httpStatus;
            return this;
        }

        public ExceptionResponse build(){
            ExceptionResponse exceptionResponse = new ExceptionResponse();
            exceptionResponse.exception = this.exception;
            exceptionResponse.setHttpStatus(httpStatus);
            ApplicationException applicationException;
            if(this.exception instanceof ApplicationException){
                applicationException = (ApplicationException) this.exception;
            }else{
                applicationException = new ApplicationException(this.exception.getMessage(), this.exception);
            }
            exceptionResponse.setCode(applicationException.getCode());
            exceptionResponse.setData(applicationException.getData());
            exceptionResponse.setMessage(applicationException.getError());
            exceptionResponse.setTime(new Date());
            return exceptionResponse;
        }

    }

    @Override
    public String toString(){
        String exceptionString = "Unknown";
        if(this.exception != null)
            exceptionString = this.exception.getClass().getName();

        return "{STATUS:" +
                httpStatus +
                "} [" +
                exceptionString +
                "]\t(" +
                this.code +
                ")->" +
                this.message;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public Map<String, Object> getData() {
        return data;
    }

    public void setData(Map<String, Object> data) {
        this.data = data;
    }

    public int getHttpStatus() {
        return httpStatus;
    }

    public void setHttpStatus(int httpStatus) {
        this.httpStatus = httpStatus;
    }

    public Date getTime() {
        return time;
    }

    public void setTime(Date time) {
        this.time = time;
    }
}
