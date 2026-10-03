# coding=utf-8
import os

path = 'backend/src/test/java/com/aicodereview/service/LLMSecretLeakTest.java'
with open(path, 'r', encoding='utf-8') as f:
    text = f.read()

text = text.replace('List<ReviewFinding> fileFindings = analyzer.analyze(tempDir);', 
'''List<ReviewFinding> fileFindings = analyzer.analyze(tempDir);
        assertFalse(fileFindings.isEmpty(), "Should find secrets");''')

with open(path, 'w', encoding='utf-8') as f:
    f.write(text)
