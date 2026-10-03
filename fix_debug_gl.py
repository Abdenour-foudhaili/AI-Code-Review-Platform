# coding=utf-8
import os

path = 'backend/src/main/java/com/aicodereview/analyzer/GitleaksAnalyzer.java'
with open(path, 'r', encoding='utf-8') as f:
    text = f.read()

target = """                        try {
                        String rawPathStr = node.path("File").asText("");"""
replacement = """                        try {
                        String rawPathStr = node.path("File").asText("");
                        System.out.println("GITLEAKS RAW PATH: " + rawPathStr);
                        System.out.println("PROJECT DIR REAL PATH: " + projectDir.toRealPath());"""

text = text.replace(target, replacement)

target2 = """                        } catch (Exception pathEx) {
                            System.err.println("Error processing path: " + pathEx.getMessage());
                        }"""
replacement2 = """                        } catch (Exception pathEx) {
                            System.err.println("Error processing path: " + pathEx.getMessage());
                            pathEx.printStackTrace();
                        }"""

text = text.replace(target2, replacement2)

with open(path, 'w', encoding='utf-8') as f:
    f.write(text)
