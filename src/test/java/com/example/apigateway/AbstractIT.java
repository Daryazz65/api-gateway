package com.example.apigateway;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;
import org.testcontainers.containers.wait.strategy.Wait;

@Testcontainers
public abstract class AbstractIT {

    private static final int KEYCLOAK_PORT = 8080;

    @Container
    static final GenericContainer<?> KEYCLOAK =
            new GenericContainer<>(
                    DockerImageName.parse(
                            "quay.io/keycloak/keycloak:26.8.1"
                    )
            )
                    .withExposedPorts(KEYCLOAK_PORT)
                    .withEnv("KC_BOOTSTRAP_ADMIN_USERNAME", "admin")
                    .withEnv("KC_BOOTSTRAP_ADMIN_PASSWORD", "admin")
                    .withCopyFileToContainer(
                            MountableFile.forClasspathResource(
                                    "keycloak/file-conversion-realm.json"
                            ),
                            "/opt/keycloak/data/import/file-conversion-realm.json"
                    )
                    .withCommand("start-dev", "--import-realm")
                    .waitingFor(
                            Wait.forHttp(
                                            "/realms/file-conversion"
                                                    + "/.well-known"
                                                    + "/openid-configuration"
                                    )
                                    .forPort(KEYCLOAK_PORT)
                                    .forStatusCode(200)
                    );

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        String issuerUri = keycloakBaseUrl()
                + "/realms/file-conversion";

        registry.add(
                "spring.security.oauth2.client.provider.keycloak.issuer-uri",
                () -> issuerUri
        );
        registry.add(
                "spring.security.oauth2.resourceserver.jwt.issuer-uri",
                () -> issuerUri
        );
        registry.add(
                "eureka.client.enabled",
                () -> false
        );
        registry.add(
                "spring.cloud.discovery.enabled",
                () -> false
        );
    }

    protected static String keycloakBaseUrl() {
        return "http://"
                + KEYCLOAK.getHost()
                + ":"
                + KEYCLOAK.getMappedPort(KEYCLOAK_PORT);
    }
}