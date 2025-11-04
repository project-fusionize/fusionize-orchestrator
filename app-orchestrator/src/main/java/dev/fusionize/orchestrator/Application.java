package dev.fusionize.orchestrator;

import dev.fusionize.worker.component.annotations.EnableRuntimeComponents;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableRuntimeComponents(basePackages = "dev.fusionize.orchestrator.components")
public class Application {
    public static final String VERSION = "1.0";
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

}
