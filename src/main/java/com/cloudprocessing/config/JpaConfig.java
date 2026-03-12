package com.cloudprocessing.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Enables @CreatedDate and @LastModifiedDate population on JPA entities.
 */
@Configuration
@EnableJpaAuditing
public class JpaConfig {
}
