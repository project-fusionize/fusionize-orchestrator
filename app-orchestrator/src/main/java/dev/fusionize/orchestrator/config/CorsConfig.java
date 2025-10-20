package dev.fusionize.orchestrator.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Collections;
import java.util.List;

@Configuration
public class CorsConfig {
    @Bean("allowedOriginPattern")
    @Profile("local")
    List<String> localAllowedOriginPattern() {
        return List.of("http://localhost:[*]");
    }

    @Bean("allowedOriginPattern")
    @Profile("!local")
    List<String> allowedOriginPattern() {
        return List.of("https://fusionize.dev", "https://*.fusionize.dev");
    }

    @Bean
    @Profile("local")
    @Primary
    CorsConfigurationSource corsLocalConfigurationSource(
            @Qualifier("allowedOriginPattern") List<String> origins) {
        return getCorsConfiguration(origins);
    }

    @Bean
    @Profile("!local")
    @Primary
    CorsConfigurationSource corsConfigurationSource(
            @Qualifier("allowedOriginPattern") List<String> origins) {
        return getCorsConfiguration(origins);
    }

    private CorsConfigurationSource getCorsConfiguration(List<String> allowedOrigin){
        CorsConfiguration configuration = new CorsConfiguration().applyPermitDefaultValues();
        configuration.setAllowCredentials(true);
        configuration.setAllowedOriginPatterns(allowedOrigin);
        configuration.setAllowedMethods(Collections.singletonList("*"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
