package dev.fusionize.project;
import dev.fusionize.common.parser.YamlParser;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class ProjectDescriptor {
    private static YamlParser<ProjectDescriptor> yamlParser = new YamlParser<>();
    private String id;
    private String description;
    private String name;
    private String cover;
    private String domain;

    public static ProjectDescriptor fromFile(File yml) throws IOException {
        String ymlString = Files.readString(yml.toPath());
        if (ymlString == null || ymlString.isEmpty()) {
            throw new IOException("Descriptor yaml not readable");
        }
        return yamlParser.fromYaml(ymlString, ProjectDescriptor.class);
    }

    public String toYaml(){
        return yamlParser.toYaml(this);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCover() {
        return cover;
    }

    public void setCover(String cover) {
        this.cover = cover;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }
}
