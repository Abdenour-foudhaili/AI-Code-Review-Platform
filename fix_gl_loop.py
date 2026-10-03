# coding=utf-8
import os

path = 'backend/src/main/java/com/aicodereview/analyzer/GitleaksAnalyzer.java'
with open(path, 'r', encoding='utf-8') as f:
    text = f.read()

target = """                        String rawPathStr = node.path("File").asText("");"""
replacement = """                        try {
                        String rawPathStr = node.path("File").asText("");"""

text = text.replace(target, replacement)

target2 = """                            findings.add(multiLineFinding);
                        }
                    }"""
replacement2 = """                            findings.add(multiLineFinding);
                        }
                        } catch (Exception pathEx) {
                            System.err.println("Error processing path: " + pathEx.getMessage());
                        }
                    }"""

text = text.replace(target2, replacement2)

with open(path, 'w', encoding='utf-8') as f:
    f.write(text)
