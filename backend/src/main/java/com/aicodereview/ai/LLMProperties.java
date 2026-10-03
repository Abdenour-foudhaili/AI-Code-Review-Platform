package com.aicodereview.ai;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * LLM configuration. All supported providers expose an OpenAI-compatible chat/completions endpoint.
 * Blank baseUrl/model fall back to the provider defaults (see resolvedBaseUrl / resolvedModel).
 * Recommended free option: provider=gemini (Gemini 2.5 Flash: 1M-token context window, generous free quota).
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "ai.llm")
public class LLMProperties {
    private String provider = "mock"; // mock | gemini | openrouter | mistral | groq
    private String baseUrl = "";
    private String apiKey;
    private String model = "";
    private int timeout = 60;
    private double temperature = 0.1;
    /** Minimum delay between two LLM calls (free tiers are limited in requests per minute). */
    private long minIntervalMs = 0;
    /** Extra model ids (comma separated) that users may request in addition to the built-in free models. */
    private String allowedModels = "";

    public String resolvedBaseUrl() {
        if (baseUrl != null && !baseUrl.isBlank()) return baseUrl.trim();
        return switch (normalizedProvider()) {
            case "gemini" -> "https://generativelanguage.googleapis.com/v1beta/openai/chat/completions";
            case "mistral" -> "https://api.mistral.ai/v1/chat/completions";
            case "groq" -> "https://api.groq.com/openai/v1/chat/completions";
            default -> "https://openrouter.ai/api/v1/chat/completions";
        };
    }

    public String resolvedModel() {
        if (model != null && !model.isBlank()) return model.trim();
        return switch (normalizedProvider()) {
            case "gemini" -> "gemini-FLASH-latest";
            case "mistral" -> "mistral-small-latest";
            case "groq" -> "llama-3.3-70b-versatile";
            default -> "openrouter/free";
        };
    }

    public boolean supportsJsonMode() { return !"gemini".equals(normalizedProvider()); }

    public boolean isMock() { return "mock".equals(normalizedProvider()); }

    public String normalizedProvider() { return provider == null ? "mock" : provider.trim().toLowerCase(Locale.ROOT); }

    public List<String> extraAllowedModels() {
        if (allowedModels == null || allowedModels.isBlank()) return List.of();
        return Arrays.stream(allowedModels.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
    }
}
