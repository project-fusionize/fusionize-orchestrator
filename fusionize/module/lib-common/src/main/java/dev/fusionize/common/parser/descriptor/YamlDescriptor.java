package dev.fusionize.common.parser.descriptor;

public interface YamlDescriptor <T>{
    T fromYamlDescription(String yaml);
    String toYamlDescription(T model);
}
