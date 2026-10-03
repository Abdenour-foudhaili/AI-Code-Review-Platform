# coding=utf-8
import os

path = 'backend/src/main/java/com/aicodereview/analyzer/GitleaksAnalyzer.java'
with open(path, 'r', encoding='utf-8') as f:
    text = f.read()

target = """                        java.nio.file.Path projPath = projectDir.toAbsolutePath().normalize();"""
replacement = """                        java.nio.file.Path projPath = projectDir.toRealPath();
                        java.nio.file.Path gitleaksRealPath = gitleaksPath.toRealPath();"""

text = text.replace(target, replacement)
text = text.replace('gitleaksPath.isAbsolute() && gitleaksPath.startsWith(projPath)', 'gitleaksRealPath.isAbsolute() && gitleaksRealPath.startsWith(projPath)')
text = text.replace('projPath.relativize(gitleaksPath).toString()', 'projPath.relativize(gitleaksRealPath).toString()')

with open(path, 'w', encoding='utf-8') as f:
    f.write(text)
