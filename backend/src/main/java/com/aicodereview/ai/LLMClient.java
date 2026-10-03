package com.aicodereview.ai;

import com.aicodereview.entity.FindingCategory;
import com.aicodereview.entity.FindingSeverity;
import com.aicodereview.entity.ReviewFinding;
import com.aicodereview.entity.ReviewType;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * OpenAI-compatible chat/completions client (Gemini, OpenRouter, Mistral, Groq).
 * The API key is only sent as a bearer header and never logged.
 */
@Component
public class LLMClient {
    private static final Logger log = LoggerFactory.getLogger(LLMClient.class);

    /** Raised when the provider cannot answer (quota, rate limit, outage...). */
    public static class LLMException extends RuntimeException {
        private final int httpStatus;
        public LLMException(String message, int httpStatus, Throwable cause) { super(message, cause); this.httpStatus = httpStatus; }
        public int getHttpStatus() { return httpStatus; }
        public boolean isRateLimited() { return httpStatus == 429; }
    }

    private static final Set<String> BUILTIN_MODELS = Set.of(
            "openrouter/free",
            "cohere/north-mini-code:free",
            "google/gemma-4-31b-it:free",
            "google/gemma-4-26b-a4b-it:free",
            "mistralai/mistral-nemo:free",
            "meta-llama/llama-3.1-8b-instruct:free",
            "qwen/qwen-2.5-coder-32b-instruct:free",
            "gemini-2.5-flash",
            "gemini-2.5-flash-lite",
            "mistral-small-latest",
            "open-mistral-nemo",
            "llama-3.3-70b-versatile",
            "llama-3.1-8b-instant");

    private final LLMProperties properties;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private long lastCallAt = 0;

    public LLMClient(LLMProperties properties) {
        this.properties = properties;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10000);
        factory.setReadTimeout(properties.getTimeout() * 1000);
        this.restTemplate = new RestTemplate(factory);
        this.objectMapper = new ObjectMapper();
        this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    // ------------------------------------------------------------------ public API

    public List<ReviewFinding> analyzeCodeChunk(String chunk, ReviewType type, String filename, int lineOffset, String requestedModelId) {
        String model = resolveModel(requestedModelId);
        String body = callProvider(buildPrompt(chunk, type, filename), model);
        return parseResponse(body, filename, lineOffset);
    }

    /** Sends a free-form prompt and returns the assistant message content (expected to be JSON). */
    public String completeJson(String prompt, String requestedModelId) {
        String model = resolveModel(requestedModelId);
        String body = callProvider(prompt, model);
        return extractContent(body);
    }

    /** Parses a JSON findings payload (as returned by the model) into entities. */
    public List<ReviewFinding> parseFindings(String content, String filename, int lineOffset) {
        List<ReviewFinding> findings = new ArrayList<>();
        JsonNode outputRoot = readJsonLenient(content);
        if (outputRoot == null) return findings;
        JsonNode parsedList = outputRoot.has("findings") ? outputRoot.path("findings") : outputRoot;
        if (!parsedList.isArray()) return findings;
        for (JsonNode node : parsedList) {
            try {
                if (!node.hasNonNull("title") || !node.hasNonNull("category") || !node.hasNonNull("severity")) {
                    log.debug("Skipping malformed finding: missing required fields");
                    continue;
                }
                ReviewFinding f = new ReviewFinding();
                f.setFile(node.hasNonNull("file") && !node.path("file").asText().isBlank() ? node.path("file").asText() : filename);
                f.setTitle(truncate(node.path("title").asText(), 250));
                f.setDescription(node.path("description").asText("No description provided."));
                f.setRecommendation(node.path("recommendation").asText(""));
                if (node.hasNonNull("fixedCode")) f.setFixedCode(node.path("fixedCode").asText());
                if (node.hasNonNull("explanation")) f.setExplanation(node.path("explanation").asText());
                f.setCategory(parseCategory(node.path("category").asText()));
                f.setSeverity(parseSeverity(node.path("severity").asText()));
                Integer line = parseLine(node.path("lineNumber"));
                f.setLineNumber(line != null ? line + lineOffset : (lineOffset > 0 ? lineOffset : 1));
                f.setSource("LLM");
                findings.add(f);
            } catch (Exception e) {
                log.debug("Skipping malformed finding entry");
            }
        }
        return findings;
    }

    /** Tolerant JSON reader: strips markdown fences and surrounding prose. Returns null when nothing parsable is found. */
    public JsonNode readJsonLenient(String content) {
        if (content == null) return null;
        String text = content.trim();
        if (text.startsWith("```json")) text = text.substring(7);
        else if (text.startsWith("```")) text = text.substring(3);
        if (text.endsWith("```")) text = text.substring(0, text.length() - 3);
        text = text.trim();
        try {
            return objectMapper.readTree(text);
        } catch (Exception first) {
            int start = text.indexOf('{');
            int end = text.lastIndexOf('}');
            if (start >= 0 && end > start) {
                try { return objectMapper.readTree(text.substring(start, end + 1)); } catch (Exception ignored) { /* fall through */ }
            }
            log.warn("LLM response is not valid JSON ({} chars)", text.length());
            return null;
        }
    }

    // ------------------------------------------------------------------ internals

    String resolveModel(String requestedModelId) {
        if (properties.getApiKey() == null || properties.getApiKey().isEmpty()) {
            throw new IllegalStateException("AI API key is missing. Configure the LLM_API_KEY environment variable.");
        }
        boolean requested = requestedModelId != null && !requestedModelId.trim().isEmpty();
        String configured = properties.resolvedModel();
        String resolved = requested ? requestedModelId.trim() : configured;

        Set<String> allowed = new HashSet<>(BUILTIN_MODELS);
        allowed.add(configured);                               // the model configured by the operator is always trusted
        allowed.addAll(properties.extraAllowedModels());

        if (!allowed.contains(resolved)) {
            if (!requested) return "openrouter/free";
            throw new IllegalArgumentException("Model not allowed. Only whitelisted FREE models are permitted.");
        }
        return resolved;
    }

    private String callProvider(String prompt, String model) {
        String url = properties.resolvedBaseUrl();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(properties.getApiKey());
        headers.set("HTTP-Referer", "https://github.com/Abdenour-foudhaili/AI-Code-Review-Platform");
        headers.set("X-Title", "AI Code Review Platform");

        ObjectNode root = objectMapper.createObjectNode();
        root.put("model", model);
        root.putArray("messages").addObject().put("role", "user").put("content", prompt);
        root.put("temperature", properties.getTemperature());
        if (properties.supportsJsonMode()) root.putObject("response_format").put("type", "json_object");

        HttpEntity<String> request;
        try {
            request = new HttpEntity<>(objectMapper.writeValueAsString(root), headers);
        } catch (Exception e) {
            throw new IllegalStateException("Cannot serialise LLM request", e);
        }

        int maxAttempts = 3;
        long backoff = 1000;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                throttle();
                ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
                return response.getBody();
            } catch (ResourceAccessException e) {
                if (attempt == maxAttempts) {
                    log.error("LLM provider unreachable after {} attempts: {}", maxAttempts, e.getMessage());
                    throw new LLMException("LLM Provider error: " + e.getMessage(), 0, e);
                }
                log.warn("LLM request failed (transient network error). Retrying {}/{}", attempt + 1, maxAttempts);
                sleep(backoff);
                backoff *= 2;
            } catch (HttpStatusCodeException e) {
                int status = e.getStatusCode().value();
                if (status == HttpStatus.TOO_MANY_REQUESTS.value() && attempt < maxAttempts) {
                    long wait = retryAfterMillis(e, backoff * 2);
                    log.warn("LLM rate limited (HTTP 429). Waiting {} ms before retry {}/{}", wait, attempt + 1, maxAttempts);
                    sleep(wait);
                    backoff *= 2;
                    continue;
                }
                log.error("Error calling LLM API (HTTP {})", status);
                throw new LLMException("LLM Provider error: HTTP " + status, status, e);
            } catch (Exception e) {
                log.error("Error calling LLM API: {}", e.getMessage());
                throw new LLMException("LLM Provider error: " + e.getMessage(), 0, e);
            }
        }
        throw new LLMException("LLM Provider error: no response", 0, null);
    }

    private long retryAfterMillis(HttpStatusCodeException e, long fallback) {
        try {
            String h = e.getResponseHeaders() == null ? null : e.getResponseHeaders().getFirst("Retry-After");
            if (h != null) return Math.min(Math.max(Long.parseLong(h.trim()), 1) * 1000, 60_000);
        } catch (NumberFormatException ignored) { /* use fallback */ }
        return Math.min(fallback, 60_000);
    }

    private synchronized void throttle() {
        long min = properties.getMinIntervalMs();
        if (min > 0) {
            long wait = lastCallAt + min - System.currentTimeMillis();
            if (wait > 0) sleep(wait);
        }
        lastCallAt = System.currentTimeMillis();
    }

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted during LLM wait", ie);
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

    private String extractContent(String jsonBody) {
        if (jsonBody == null || jsonBody.isEmpty()) return "";
        try {
            JsonNode root = objectMapper.readTree(jsonBody);
            JsonNode choices = root.path("choices");
            if (choices.isArray() && choices.size() > 0) {
                JsonNode message = choices.get(0).path("message");
                if (message.has("content")) return message.path("content").asText("").trim();
            }
        } catch (Exception e) {
            log.warn("Failed to read LLM response envelope: {}", e.getMessage());
        }
        return "";
    }

    private List<ReviewFinding> parseResponse(String jsonBody, String filename, int lineOffset) {
        String content = extractContent(jsonBody);
        if (content.isEmpty()) return new ArrayList<>();
        return parseFindings(content, filename, lineOffset);
    }

    private static FindingCategory parseCategory(String raw) {
        String v = raw == null ? "" : raw.trim().toUpperCase(Locale.ROOT).replace(' ', '_').replace('-', '_');
        try {
            return FindingCategory.valueOf(v);
        } catch (IllegalArgumentException e) {
            return switch (v) {
                case "MAINTAINABILITY", "READABILITY", "DESIGN" -> FindingCategory.CODE_SMELL;
                case "TESTS", "TESTING" -> FindingCategory.TEST;
                case "VULNERABILITY" -> FindingCategory.SECURITY;
                default -> FindingCategory.BEST_PRACTICE;
            };
        }
    }

    private static FindingSeverity parseSeverity(String raw) {
        String v = raw == null ? "" : raw.trim().toUpperCase(Locale.ROOT);
        try {
            return FindingSeverity.valueOf(v);
        } catch (IllegalArgumentException e) {
            return switch (v) {
                case "BLOCKER" -> FindingSeverity.CRITICAL;
                case "MAJOR" -> FindingSeverity.HIGH;
                case "MINOR" -> FindingSeverity.LOW;
                default -> FindingSeverity.MEDIUM;
            };
        }
    }

    private static Integer parseLine(JsonNode n) {
        if (n == null || n.isNull() || n.isMissingNode()) return null;
        if (n.isInt() || n.isLong()) return n.asInt();
        if (n.isTextual()) {
            String digits = n.asText().replaceAll("[^0-9].*$", "");
            if (!digits.isEmpty()) {
                try { return Integer.parseInt(digits); } catch (NumberFormatException ignored) { /* none */ }
            }
        }
        return null;
    }

    private static String truncate(String s, int max) { return s.length() <= max ? s : s.substring(0, max); }
}
