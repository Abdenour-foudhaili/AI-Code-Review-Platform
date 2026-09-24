package com.aicodereview.ai;

import com.aicodereview.entity.FindingCategory;
import com.aicodereview.entity.FindingSeverity;
import com.aicodereview.entity.ReviewFinding;
import com.aicodereview.entity.ReviewType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class LLMClientTest {

    private LLMClient llmClient;
    private RestTemplate restTemplateMock;

    @BeforeEach
    public void setup() {
        LLMProperties props = new LLMProperties();
        props.setApiKey("test-key");
        llmClient = new LLMClient(props);
        
        restTemplateMock = mock(RestTemplate.class);
        ReflectionTestUtils.setField(llmClient, "restTemplate", restTemplateMock);
    }

    @Test
    public void testValidJsonResponse() {
        String mockResponse = "{\"candidates\": [{\"content\": {\"parts\": [{\"text\": \"[{\\\"category\\\": \\\"BUG\\\", \\\"severity\\\": \\\"HIGH\\\", \\\"title\\\": \\\"Null Check\\\", \\\"lineNumber\\\": 10}]\"}]}}]}";
        when(restTemplateMock.postForEntity(anyString(), any(), any())).thenReturn(new ResponseEntity<>(mockResponse, HttpStatus.OK));

        List<ReviewFinding> findings = llmClient.analyzeCodeChunk("String x = null; x.length();", ReviewType.BUG_DETECTION, "Test.java", 0);
        
        assertEquals(1, findings.size());
        assertEquals("Null Check", findings.get(0).getTitle());
        assertEquals(FindingCategory.BUG, findings.get(0).getCategory());
        assertEquals(FindingSeverity.HIGH, findings.get(0).getSeverity());
        assertEquals(10, findings.get(0).getLineNumber());
    }

    @Test
    public void testMissingApiKeyThrowsException() {
        LLMProperties props = new LLMProperties(); // No API key
        LLMClient clientNoKey = new LLMClient(props);
        
        assertThrows(IllegalStateException.class, () -> {
            clientNoKey.analyzeCodeChunk("code", ReviewType.FULL_REVIEW, "test.java", 0);
        });
    }

    @Test
    public void testMalformedJsonResponseReturnsEmptyList() {
        String mockResponse = "{\"candidates\": [{\"content\": {\"parts\": [{\"text\": \"INVALID JSON CONTENT\"}]}}]}";
        when(restTemplateMock.postForEntity(anyString(), any(), any())).thenReturn(new ResponseEntity<>(mockResponse, HttpStatus.OK));

        List<ReviewFinding> findings = llmClient.analyzeCodeChunk("code", ReviewType.FULL_REVIEW, "test.java", 0);
        
        assertTrue(findings.isEmpty());
    }
}
