package com.aicodereview.ai;

import com.aicodereview.entity.FindingCategory;
import com.aicodereview.entity.FindingSeverity;
import com.aicodereview.entity.ReviewFinding;
import com.aicodereview.entity.ReviewType;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

@Component
public class LLMClient {

    private final LLMProperties properties;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public LLMClient(LLMProperties properties) {
        this.properties = properties;
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
        this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public List<ReviewFinding> analyzeCodeChunk(String chunk, ReviewType type, String filename, int lineOffset) {
        if (properties.getApiKey() == null || properties.getApiKey().isEmpty()) {
            throw new IllegalStateException("AI API key is missing. Configure ai.llm.api-key in application.yml.");
        }

        String prompt = buildPrompt(chunk, type, filename);
        
        String url = properties.getApiUrl() + properties.getModel() + ":generateContent?key=" + properties.getApiKey();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        String requestBody = String.format("""
            {
              "contents": [{
                "parts": [{"text": %s}]
              }],
              "generationConfig": {
                "temperature": %s,
                "response_mime_type": "application/json"
              }
            }
            """, escapeJson(prompt), properties.getTemperature());

        HttpEntity<String> request = new HttpEntity<>(requestBody, headers);
        
        try {
            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
            return parseResponse(response.getBody(), filename, lineOffset);
        } catch (Exception e) {
            System.err.println("Error calling LLM API: " + e.getMessage());
            throw new RuntimeException("LLM Provider error: " + e.getMessage(), e);
        }
    }

    private String buildPrompt(String code, ReviewType type, String filename) {
        return "You are an expert software engineer and security reviewer.\n" +
                "Review the following code snippet from file: " + filename + "\n" +
                "Focus on: " + type.name() + " (Bugs, Security, Code Smells, Maintainability, Refactoring, Performance).\n" +
                "Do NOT invent issues. If no issues exist, return an empty JSON array [].\n" +
                "Distinguish between confirmed issues and suggestions.\n" +
                "Return the response STRICTLY as a JSON array of objects with these exact keys:\n" +
                "- category (must be one of: BUG, SECURITY, CODE_SMELL, PERFORMANCE, STYLE, BEST_PRACTICE)\n" +
                "- severity (must be one of: CRITICAL, HIGH, MEDIUM, LOW, INFO)\n" +
                "- title (string)\n" +
                "- description (string)\n" +
                "- lineNumber (integer, line number in the snippet provided)\n" +
                "- recommendation (string)\n" +
                "- fixedCode (string, correct code block, optional)\n\n" +
                "Code:\n" + code;
    }

    private String escapeJson(String text) {
        try {
            return objectMapper.writeValueAsString(text);
        } catch (JsonProcessingException e) {
            return "\"\"";
        }
    }

    private List<ReviewFinding> parseResponse(String jsonBody, String filename, int lineOffset) {
        List<ReviewFinding> findings = new ArrayList<>();
        if (jsonBody == null || jsonBody.isEmpty()) return findings;

        try {
            JsonNode root = objectMapper.readTree(jsonBody);
            JsonNode candidates = root.path("candidates");
            if (candidates.isArray() && candidates.size() > 0) {
                JsonNode parts = candidates.get(0).path("content").path("parts");
                if (parts.isArray() && parts.size() > 0) {
                    String text = parts.get(0).path("text").asText();
                    
                    List<JsonNode> parsedList = objectMapper.readValue(text, new TypeReference<List<JsonNode>>() {});
                    for (JsonNode node : parsedList) {
                        try {
                            ReviewFinding f = new ReviewFinding();
                            f.setFile(filename);
                            f.setTitle(node.path("title").asText("Issue"));
                            f.setDescription(node.path("description").asText(""));
                            f.setRecommendation(node.path("recommendation").asText(""));
                            if (node.has("fixedCode")) {
                                f.setFixedCode(node.path("fixedCode").asText(""));
                            }
                            
                            // Safe enum parsing
                            try {
                                f.setCategory(FindingCategory.valueOf(node.path("category").asText("CODE_SMELL")));
                            } catch (Exception e) {
                                f.setCategory(FindingCategory.CODE_SMELL);
                            }
                            
                            try {
                                f.setSeverity(FindingSeverity.valueOf(node.path("severity").asText("INFO")));
                            } catch (Exception e) {
                                f.setSeverity(FindingSeverity.INFO);
                            }
                            
                            if (node.has("lineNumber") && node.path("lineNumber").isInt()) {
                                f.setLineNumber(node.path("lineNumber").asInt() + lineOffset);
                            }
                            findings.add(f);
                        } catch (Exception e) {
                            // Skip malformed entries
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to parse structured LLM response: " + e.getMessage());
        }
        return findings;
    }
}
