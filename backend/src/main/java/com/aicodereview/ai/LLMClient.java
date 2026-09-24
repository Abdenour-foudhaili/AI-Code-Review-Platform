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
            throw new IllegalStateException("AI API key is missing. Configure OPENROUTER_API_KEY environment variable.");
        }

        String prompt = buildPrompt(chunk, type, filename);
        String url = properties.getBaseUrl();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(properties.getApiKey());
        headers.set("HTTP-Referer", "https://github.com/Abdenour-foudhaili/AI-Code-Review-Platform");
        headers.set("X-Title", "AI Code Review Platform");

        String requestBody = String.format("""
            {
              "model": "%s",
              "messages": [
                {
                  "role": "user",
                  "content": %s
                }
              ],
              "temperature": %s,
              "response_format": { "type": "json_object" }
            }
            """, properties.getModel(), escapeJson(prompt), properties.getTemperature());

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
                "Do NOT invent issues. If no issues exist, return an empty array for 'findings'.\n" +
                "Distinguish between confirmed issues and suggestions.\n" +
                "Return the response STRICTLY as a JSON object containing a 'findings' array.\n" +
                "Each object in the array must have these exact keys:\n" +
                "- category (must be one of: BUG, SECURITY, CODE_SMELL, PERFORMANCE, STYLE, BEST_PRACTICE)\n" +
                "- severity (must be one of: CRITICAL, HIGH, MEDIUM, LOW, INFO)\n" +
                "- title (string)\n" +
                "- description (string)\n" +
                "- lineNumber (integer, line number in the snippet provided)\n" +
                "- recommendation (string)\n" +
                "- fixedCode (string, correct code block, optional)\n\n" +
                "Return JSON ONLY. No markdown, no explanations outside JSON.\n\n" +
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
            JsonNode choices = root.path("choices");
            if (choices.isArray() && choices.size() > 0) {
                JsonNode message = choices.get(0).path("message");
                if (message.has("content")) {
                    String text = message.path("content").asText().trim();
                    
                    // Strip markdown wrapping if model ignored instructions
                    if (text.startsWith("```json")) {
                        text = text.substring(7);
                    } else if (text.startsWith("```")) {
                        text = text.substring(3);
                    }
                    if (text.endsWith("```")) {
                        text = text.substring(0, text.length() - 3);
                    }
                    text = text.trim();
                    
                    JsonNode outputRoot = objectMapper.readTree(text);
                    JsonNode parsedList = outputRoot.has("findings") ? outputRoot.path("findings") : outputRoot;
                    
                    if (parsedList.isArray()) {
                        for (JsonNode node : parsedList) {
                            try {
                                if (!node.hasNonNull("title") || !node.hasNonNull("category") || !node.hasNonNull("severity")) {
                                    System.err.println("Skipping malformed finding: missing required fields.");
                                    continue;
                                }

                                ReviewFinding f = new ReviewFinding();
                                f.setFile(filename);
                                f.setTitle(node.path("title").asText());
                                f.setDescription(node.path("description").asText("No description provided."));
                                f.setRecommendation(node.path("recommendation").asText(""));
                                
                                if (node.hasNonNull("fixedCode")) {
                                    f.setFixedCode(node.path("fixedCode").asText());
                                }
                                
                                f.setCategory(FindingCategory.valueOf(node.path("category").asText()));
                                f.setSeverity(FindingSeverity.valueOf(node.path("severity").asText()));
                                
                                if (node.hasNonNull("lineNumber") && node.path("lineNumber").isInt()) {
                                    f.setLineNumber(node.path("lineNumber").asInt() + lineOffset);
                                } else {
                                    f.setLineNumber(lineOffset > 0 ? lineOffset : 1);
                                }
                                
                                findings.add(f);
                            } catch (IllegalArgumentException e) {
                                System.err.println("Skipping finding due to invalid enum value: " + e.getMessage());
                            } catch (Exception e) {
                                System.err.println("Skipping malformed finding entry.");
                            }
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
