package com.aicodereview.analyzer;

import com.aicodereview.entity.FindingCategory;
import com.aicodereview.entity.FindingSeverity;
import com.aicodereview.entity.ReviewFinding;
import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

public class GitleaksAnalyzerTest {

    @Test
    public void testParseGitleaksJson() throws Exception {
        GitleaksAnalyzer analyzer = new GitleaksAnalyzer();
        
        // Ensure Gitleaks is in PATH for test
        ProcessBuilder pb = new ProcessBuilder("gitleaks", "version");
        try { pb.start().waitFor(); } catch(Exception e) { return; }
        
        Path projectDir = Files.createTempDirectory("gitleaks_test");
        Path secretFile = projectDir.resolve("test_secret.py");
        Files.writeString(secretFile, "GITHUB_TOKEN = \"sk-test-1234567890abcdef\"");
        
        List<ReviewFinding> findings = analyzer.analyze(projectDir);
        
        assertFalse(findings.isEmpty(), "Should find the secret");
        ReviewFinding f = findings.get(0);
        assertEquals("GITLEAKS", f.getSource());
        assertNotNull(f.getRuleId());
        assertEquals(FindingSeverity.CRITICAL, f.getSeverity());
        assertEquals("test_secret.py", f.getFile());
    }

    @Test
    public void testPathResolutionCrossPlatform() throws Exception {
        GitleaksAnalyzer analyzer = new GitleaksAnalyzer();
        if (!analyzer.isAvailable()) return;
        
        Path projectDir = Files.createTempDirectory("gitleaks_test_path");
        Path subDir = projectDir.resolve("src").resolve("main");
        Files.createDirectories(subDir);
        Path secretFile = subDir.resolve("config.py");
        Files.writeString(secretFile, "API_KEY = \"sk-test-1234567890abcdef\"");
        
        List<ReviewFinding> findings = analyzer.analyze(projectDir);
        
        assertFalse(findings.isEmpty(), "Should find the secret in nested folder");
        ReviewFinding f = findings.get(0);
        
        assertEquals("src/main/config.py", f.getFile(), "Path should be relative and use forward slashes");
    }
}
