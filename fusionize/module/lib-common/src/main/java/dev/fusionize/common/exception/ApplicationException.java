package dev.fusionize.common.exception;

import java.util.HashMap;
import java.util.Map;

public class ApplicationException extends Exception {
    private static final String EXCEPTION_REGEX = "\\([a-z]+[\\d.]*\\) .*";
    private static final String UNKNOWN_CODE = "u";

    private static String CODE_EXTRACTOR(String message) {
        if (message != null && message.matches(EXCEPTION_REGEX)) {
            String[] splitMessage = message.split("\\) ");
            if (splitMessage.length == 2) {
                return splitMessage[0].replaceFirst("\\(", "");
            }
        }
        return UNKNOWN_CODE;
    }

    private static String ERROR_EXTRACTOR(String message) {
        if (message != null && message.matches(EXCEPTION_REGEX)) {
            String[] splitMessage = message.split("\\) ");
            if (splitMessage.length == 2) {
                return splitMessage[1];
            }
        }
        return message;
    }

    private final String code;
    private final Map<String, Object> data = new HashMap<>();
    private final String error;

    public ApplicationException() {
        super();
        this.code = UNKNOWN_CODE;
        this.error = null;
    }

    public ApplicationException(String message) {
        super(message);
        this.error = ERROR_EXTRACTOR(message);
        this.code = CODE_EXTRACTOR(message);
    }

    public ApplicationException(String message, Throwable cause) {
        super(message,  cause);
        this.error = ERROR_EXTRACTOR(message);
        this.code = CODE_EXTRACTOR(message);
    }

    public ApplicationException(Throwable cause) {
        super(cause);
        if(cause.getMessage()!=null){
            this.error = ERROR_EXTRACTOR(cause.getMessage());
            this.code = CODE_EXTRACTOR(cause.getMessage());
        }else {
            this.code = UNKNOWN_CODE;
            this.error = null;
        }
    }

    public void put(String key, Object val){
        this.data.put(key,val);
    }

    public String getCode() {
        return code;
    }

    public Map<String, Object> getData() {
        return data;
    }

    public String getError() {
        return error;
    }
}
