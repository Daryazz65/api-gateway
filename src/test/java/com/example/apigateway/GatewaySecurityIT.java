package com.example.apigateway;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.client.WebClient;

@SpringBootTest(webEnvironment = RANDOM_PORT)
@AutoConfigureWebTestClient
class GatewaySecurityIT extends AbstractIT {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void healthIsAvailableWithoutAuthentication() {
        webTestClient.get()
                .uri("/actuator/health")
                .exchange()
                .expectStatus()
                .isOk();
    }

    @Test
    void protectedEndpointRejectsAnonymousRequest() {
        webTestClient.get()
                .uri("/api/v1/not-configured")
                .exchange()
                .expectStatus()
                .isUnauthorized();
    }

    @Test
    void keycloakOpenIdConfigurationIsAvailable() {
        OpenIdConfiguration configuration = WebClient.create(keycloakBaseUrl())
                .get()
                .uri("/realms/file-conversion/.well-known/openid-configuration")
                .retrieve()
                .bodyToMono(OpenIdConfiguration.class)
                .block();

        assertThat(configuration).isNotNull();
        assertThat(configuration.issuer())
                .isEqualTo(keycloakBaseUrl() + "/realms/file-conversion");
    }

    private record OpenIdConfiguration(String issuer) {
    }
}