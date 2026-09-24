package com.aicodereview.ai;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "ai.llm")
public class LLMProperties {
    private String provider = "mock"; // "mock" or "real"
    private String apiUrl = "https://generativelanguage.googleapis.com/v1beta/models/";
    private String apiKey;
    private String model = "gemini-2.5-flash";
    private int timeoutSeconds = 60;
    private double temperature = 0.2;
}
