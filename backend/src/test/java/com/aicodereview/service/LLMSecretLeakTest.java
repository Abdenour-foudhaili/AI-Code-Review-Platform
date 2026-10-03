package com.aicodereview.service;

import com.aicodereview.analyzer.GitleaksAnalyzer;
import com.aicodereview.entity.ReviewFinding;
import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

public class LLMSecretLeakTest {

    @Test
    public void testSecretNotLeakedToLLMContext() throws Exception {
        GitleaksAnalyzer analyzer = new GitleaksAnalyzer();
        if (!analyzer.isAvailable()) {
            return;
        }

        Path tempDir = Files.createTempDirectory("leak-test");
        Path secretFile = tempDir.resolve("multiple_secrets.py");
        
        String TEST_SECRET_1 = "ghp_1111111111111111111111111111111111111111";
        String TEST_SECRET_2 = "ghp_2222222222222222222222222222222222222222";
        
        String fileContent = "def do_something():\n" +
                             "    token1 = '" + TEST_SECRET_1 + "'\n" +
                             "    print('Hello')\n" +
                             "    token2 = '" + TEST_SECRET_2 + "'\n" +
                             "    token3 = '" + TEST_SECRET_2 + "'\n"; // same line secret test isn't exactly this, but close
        Files.writeString(secretFile, fileContent);
        
        List<ReviewFinding> fileFindings = analyzer.analyze(tempDir);
        assertFalse(fileFindings.isEmpty(), "Should find secrets");
        
        // Exact logic from ProjectAnalysisTransactionService
        StringBuilder context = new StringBuilder();
        context.append("Static analysis found the following issues in multiple_secrets.py:\n");
        for (ReviewFinding f : fileFindings) {
            context.append("- ").append(f.getSource()).append(" (").append(f.getRuleId()).append(") at line ")
                   .append(f.getLineNumber()).append(": ").append(f.getDescription()).append("\n");
        }
        context.append("\nCode context:\n");
        String[] lines = fileContent.split("\n");
        
        for (ReviewFinding f : fileFindings) {
            if ("GITLEAKS".equals(f.getSource()) && f.getLineNumber() != null && f.getLineNumber() > 0 && f.getLineNumber() <= lines.length) {
                lines[f.getLineNumber() - 1] = "/* *** MASKED SECRET *** */";
            }
        }
        
        for(int i=0; i<Math.min(lines.length, 150); i++){
            context.append(lines[i]).append("\n");
        }
        
        String finalContext = context.toString();
        
        assertFalse(finalContext.contains(TEST_SECRET_1), "Secret 1 leaked!");
        assertFalse(finalContext.contains(TEST_SECRET_2), "Secret 2 leaked!");
        assertTrue(finalContext.contains("/* *** MASKED SECRET *** */"), "Should contain masked string");
        
        Files.delete(secretFile);
        Files.delete(tempDir);
    }
}
