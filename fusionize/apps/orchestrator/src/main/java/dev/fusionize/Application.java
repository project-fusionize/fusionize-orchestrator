package dev.fusionize;

import dev.fusionize.worker.component.annotations.EnableRuntimeComponents;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableRuntimeComponents(basePackages = {
        "dev.fusionize.orchestrator.components",
        "dev.fusionize.ai",
        "dev.fusionize.web"
})
public class Application {
    public static final String VERSION = "1.0";
    public static final String API_PREFIX = "/api/"+VERSION;

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

}
