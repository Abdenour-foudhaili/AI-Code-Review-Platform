# coding=utf-8
import os
import re

path = 'backend/src/test/java/com/aicodereview/analyzer/GitleaksAnalyzerTest.java'
with open(path, 'r', encoding='utf-8') as f:
    text = f.read()

test = """    @Test
    public void testPathResolutionCrossPlatform() throws Exception {
        GitleaksAnalyzer analyzer = new GitleaksAnalyzer();
        if (!analyzer.isAvailable()) return;
        
        Path projectDir = Files.createTempDirectory("gitleaks_test_path");
        Path subDir = projectDir.resolve("src/main");
        Files.createDirectories(subDir);
        Path secretFile = subDir.resolve("config.py");
        Files.writeString(secretFile, "API_KEY = \\"sk-test-1234567890abcdef\\"");
        
        List<ReviewFinding> findings = analyzer.analyze(projectDir);
        
        assertFalse(findings.isEmpty(), "Should find the secret");
        ReviewFinding f = findings.get(0);
        
        // Assert path is purely relative and uses forward slashes
        assertEquals("src/main/config.py", f.getFile(), "Path should be relative and use forward slashes");
    }
}"""

text = text.replace("}", test)
with open(path, 'w', encoding='utf-8') as f:
    f.write(text)
