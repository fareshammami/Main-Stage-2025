package cqrses.config;

import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KeycloakAdminConfig {


    @Bean
    public Keycloak keycloakAdminClient() {
        return KeycloakBuilder.builder()
                .serverUrl("http://localhost:8081") // or "http://keycloak:8080" in Docker
                .realm("MainStageRealm") // 👈 your actual realm (not "master")
                .clientId("springboot-backendC")
                .clientSecret("L7YdxkgryRCUSv8cXMmM94FHZ5AKz8YE")
                .grantType(OAuth2Constants.CLIENT_CREDENTIALS)
                .build();
    }
}