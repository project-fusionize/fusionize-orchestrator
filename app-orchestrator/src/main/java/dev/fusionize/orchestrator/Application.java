package dev.fusionize.orchestrator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class Application {
    public static final String VERSION = "1.0";
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

}
