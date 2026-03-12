package com.cloudprocessing;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

/**
 * Smoke test: verifies the Spring application context loads successfully.
 * Uses Testcontainers for a real Postgres instance and mocks AWS S3 beans
 * so the test runs without any cloud credentials.
 */
@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class CloudProcessingApplicationTests {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
        .withDatabaseName("cloudprocessing_test")
        .withUsername("test")
        .withPassword("test");

    @org.springframework.test.context.DynamicPropertySource
    static void overrideDataSource(org.springframework.test.context.DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    // AWS beans mocked so the context boots without real credentials
    @MockBean S3Client s3Client;
    @MockBean S3Presigner s3Presigner;

    @Test
    void contextLoads() {
        // If the context starts, the test passes
    }
}
