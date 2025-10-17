package dev.fusionize.common.template;

public class TemplateParserException extends Exception{
    public TemplateParserException() {
        super();
    }

    public TemplateParserException(String message) {
        super(message);
    }

    public TemplateParserException(String message, Throwable cause) {
        super(message, cause);
    }

    public TemplateParserException(Throwable cause) {
        super(cause);
    }
}
