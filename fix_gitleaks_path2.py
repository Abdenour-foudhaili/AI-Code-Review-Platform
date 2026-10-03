# coding=utf-8
import os
import re

path = 'backend/src/main/java/com/aicodereview/analyzer/GitleaksAnalyzer.java'
with open(path, 'r', encoding='utf-8') as f:
    text = f.read()

target = r"""                        String filePath = node.path("File").asText("");
                        if (filePath.startsWith(projectDir.toString())) {
                            filePath = filePath.substring(projectDir.toString().length());
                            if (filePath.startsWith("/") || filePath.startsWith("\\")) {
                                filePath = filePath.substring(1);
                            }
                        }
                        filePath = filePath.replace("\\", "/");
                        
                        finding.setFile(filePath);"""

replacement = """                        String rawPathStr = node.path("File").asText("");
                        java.nio.file.Path gitleaksPath = java.nio.file.Path.of(rawPathStr).normalize();
                        java.nio.file.Path projPath = projectDir.toAbsolutePath().normalize();
                        
                        String relativePathStr;
                        if (gitleaksPath.isAbsolute() && gitleaksPath.startsWith(projPath)) {
                            relativePathStr = projPath.relativize(gitleaksPath).toString();
                        } else {
                            relativePathStr = gitleaksPath.toString();
                        }
                        
                        relativePathStr = relativePathStr.replace("\\\\", "/");
                        finding.setFile(relativePathStr);"""

text = text.replace(target, replacement)
if target in text:
    print("WARNING: STILL FOUND")
else:
    print("Replaced!")

with open(path, 'w', encoding='utf-8') as f:
    f.write(text)
