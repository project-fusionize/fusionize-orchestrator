package dev.fusionize.common.parser;

import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.nodes.Tag;

import java.io.InputStream;

public class YamlParser<T> {
    private final Yaml yaml = new Yaml();
    public T fromYaml(String yml, Class<T> typeOfT){
        return yaml.loadAs(yml, typeOfT);
    }

    public T fromYaml(InputStream inputStream, Class<T> typeOfT){
        return yaml.loadAs(inputStream, typeOfT);
    }

    public String toYaml(T t){
        return yaml.dumpAs(t, Tag.MAP, null);
    }
}
