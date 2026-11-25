package com.renzochirinos.creditoia.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "llm")
public record LlmProperties(
        @NotBlank String provider,
        @NotBlank String model,
        @NotBlank String baseUrl,
        String apiKey,
        @NotNull Duration timeout) {
}
