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

    @Test
    public void testFirstRequestSucceeds() {
        String mockResponse = "{\"choices\": [{\"message\": {\"content\": \"{}\"}}]}";
        when(restTemplateMock.postForEntity(anyString(), any(), any())).thenReturn(new ResponseEntity<>(mockResponse, HttpStatus.OK));
        List<ReviewFinding> findings = llmClient.analyzeCodeChunk("test", ReviewType.BUG_DETECTION, "Test.java", 0, "openrouter/free");
        assertNotNull(findings);
    }

    @Test
    public void testFirstRequestFailsSecondSucceeds() {
        String mockResponse = "{\"choices\": [{\"message\": {\"content\": \"{}\"}}]}";
        when(restTemplateMock.postForEntity(anyString(), any(), any()))
            .thenThrow(new org.springframework.web.client.ResourceAccessException("Connection reset"))
            .thenReturn(new ResponseEntity<>(mockResponse, HttpStatus.OK));
        
        List<ReviewFinding> findings = llmClient.analyzeCodeChunk("test", ReviewType.BUG_DETECTION, "Test.java", 0, "openrouter/free");
        assertNotNull(findings);
        org.mockito.Mockito.verify(restTemplateMock, org.mockito.Mockito.times(2)).postForEntity(anyString(), any(), any());
    }

    @Test
    public void testTwoTransientFailuresThirdSucceeds() {
        String mockResponse = "{\"choices\": [{\"message\": {\"content\": \"{}\"}}]}";
        when(restTemplateMock.postForEntity(anyString(), any(), any()))
            .thenThrow(new org.springframework.web.client.ResourceAccessException("Connection reset 1"))
            .thenThrow(new org.springframework.web.client.ResourceAccessException("Connection reset 2"))
            .thenReturn(new ResponseEntity<>(mockResponse, HttpStatus.OK));
        
        List<ReviewFinding> findings = llmClient.analyzeCodeChunk("test", ReviewType.BUG_DETECTION, "Test.java", 0, "openrouter/free");
        assertNotNull(findings);
        org.mockito.Mockito.verify(restTemplateMock, org.mockito.Mockito.times(3)).postForEntity(anyString(), any(), any());
    }

    @Test
    public void testThreeTransientFailuresThrows() {
        when(restTemplateMock.postForEntity(anyString(), any(), any()))
            .thenThrow(new org.springframework.web.client.ResourceAccessException("Connection reset"));
        
        assertThrows(RuntimeException.class, () -> {
            llmClient.analyzeCodeChunk("test", ReviewType.BUG_DETECTION, "Test.java", 0, "openrouter/free");
        });
        org.mockito.Mockito.verify(restTemplateMock, org.mockito.Mockito.times(3)).postForEntity(anyString(), any(), any());
    }

    @Test
    public void testHttp400NoRetry() {
        when(restTemplateMock.postForEntity(anyString(), any(), any()))
            .thenThrow(new org.springframework.web.client.HttpClientErrorException(HttpStatus.BAD_REQUEST));
        
        assertThrows(RuntimeException.class, () -> {
            llmClient.analyzeCodeChunk("test", ReviewType.BUG_DETECTION, "Test.java", 0, "openrouter/free");
        });
        org.mockito.Mockito.verify(restTemplateMock, org.mockito.Mockito.times(1)).postForEntity(anyString(), any(), any());
    }

    @Test
    public void testHttp401NoRetry() {
        when(restTemplateMock.postForEntity(anyString(), any(), any()))
            .thenThrow(new org.springframework.web.client.HttpClientErrorException(HttpStatus.UNAUTHORIZED));
        
        assertThrows(RuntimeException.class, () -> {
            llmClient.analyzeCodeChunk("test", ReviewType.BUG_DETECTION, "Test.java", 0, "openrouter/free");
        });
        org.mockito.Mockito.verify(restTemplateMock, org.mockito.Mockito.times(1)).postForEntity(anyString(), any(), any());
    }

    @Test
    public void testHttp402NoRetry() {
        when(restTemplateMock.postForEntity(anyString(), any(), any()))
            .thenThrow(new org.springframework.web.client.HttpClientErrorException(HttpStatus.PAYMENT_REQUIRED));
        
        assertThrows(RuntimeException.class, () -> {
            llmClient.analyzeCodeChunk("test", ReviewType.BUG_DETECTION, "Test.java", 0, "openrouter/free");
        });
        org.mockito.Mockito.verify(restTemplateMock, org.mockito.Mockito.times(1)).postForEntity(anyString(), any(), any());
    }

    @Test
    public void testApprovedFreeModelAccepted() {
        String mockResponse = "{\"choices\": [{\"message\": {\"content\": \"{}\"}}]}";
        when(restTemplateMock.postForEntity(anyString(), any(), any())).thenReturn(new ResponseEntity<>(mockResponse, HttpStatus.OK));
        
        // Should not throw IllegalArgumentException
        llmClient.analyzeCodeChunk("test", ReviewType.BUG_DETECTION, "Test.java", 0, "google/gemma-4-31b-it:free");
    }


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
        String mockResponse = "{\"choices\": [{\"message\": {\"content\": \"{\\\"findings\\\": [{\\\"category\\\": \\\"BUG\\\", \\\"severity\\\": \\\"HIGH\\\", \\\"title\\\": \\\"Null Check\\\", \\\"description\\\": \\\"Desc\\\", \\\"lineNumber\\\": 10}]}\"}}]}";
        
        when(restTemplateMock.postForEntity(anyString(), any(), any())).thenReturn(new ResponseEntity<>(mockResponse, HttpStatus.OK));

        List<ReviewFinding> findings = llmClient.analyzeCodeChunk("String x = null; x.length();", ReviewType.BUG_DETECTION, "Test.java", 0, "openrouter/free");
        
        assertEquals(1, findings.size());
        assertEquals("Null Check", findings.get(0).getTitle());
        assertEquals(FindingCategory.BUG, findings.get(0).getCategory());
        assertEquals(FindingSeverity.HIGH, findings.get(0).getSeverity());
        assertEquals(10, findings.get(0).getLineNumber());
    }

    @Test
    public void testMissingApiKeyThrowsException() {
        LLMProperties props = new LLMProperties();
        LLMClient clientNoKey = new LLMClient(props);
        
        assertThrows(IllegalStateException.class, () -> {
            clientNoKey.analyzeCodeChunk("code", ReviewType.FULL_REVIEW, "test.java", 0, "openrouter/free");
        });
    }

    @Test
    public void testMalformedJsonResponseReturnsEmptyList() {
        String mockResponse = "{\"choices\": [{\"message\": {\"content\": \"INVALID JSON CONTENT\"}}]}";
        when(restTemplateMock.postForEntity(anyString(), any(), any())).thenReturn(new ResponseEntity<>(mockResponse, HttpStatus.OK));

        List<ReviewFinding> findings = llmClient.analyzeCodeChunk("code", ReviewType.FULL_REVIEW, "test.java", 0, "openrouter/free");
        
        assertTrue(findings.isEmpty());
    }

    @Test
    public void testUnauthorizedPaidModelRejected() {
        assertThrows(IllegalArgumentException.class, () -> {
            llmClient.analyzeCodeChunk("code", ReviewType.FULL_REVIEW, "Test.java", 0, "openai/gpt-3.5-turbo");
        });
    }
}
