package com.aicodereview.ai;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "ai.llm")
public class LLMProperties {
    private String provider = "mock"; // "mock" or "openrouter"
    private String baseUrl = "https://openrouter.ai/api/v1/chat/completions";
    private String apiKey;
    private String model = "openai/gpt-3.5-turbo";
    private int timeout = 60;
    private double temperature = 0.1;
}