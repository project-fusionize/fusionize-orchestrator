package dev.fusionize.common.parser.descriptor;

public interface JsonDescriptor <T>{
    T fromJsonDescription(String json);
    String toJsonDescription(T model);
}
