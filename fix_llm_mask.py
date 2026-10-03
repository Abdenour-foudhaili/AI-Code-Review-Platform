# coding=utf-8
import os
import re

path = 'backend/src/main/java/com/aicodereview/service/ProjectAnalysisTransactionService.java'
with open(path, 'r', encoding='utf-8') as f:
    text = f.read()

# Replace the context generation to mask lines that have GITLEAKS findings
old_context_builder = """                    context.append("\\nCode context:\\n");
                    // Add first 150 lines or so for context to avoid sending huge files
                    String[] lines = pf.getContent().split("\\n");
                    for(int i=0; i<Math.min(lines.length, 150); i++){
                        context.append(lines[i]).append("\\n");
                    }"""

new_context_builder = """                    context.append("\\nCode context:\\n");
                    // Add first 150 lines or so for context to avoid sending huge files
                    String[] lines = pf.getContent().split("\\n");
                    
                    // MASK secrets found by GITLEAKS
                    for (ReviewFinding f : fileFindings) {
                        if ("GITLEAKS".equals(f.getSource()) && f.getLineNumber() != null && f.getLineNumber() > 0 && f.getLineNumber() <= lines.length) {
                            lines[f.getLineNumber() - 1] = "/* *** MASKED SECRET *** */";
                        }
                    }
                    
                    for(int i=0; i<Math.min(lines.length, 150); i++){
                        context.append(lines[i]).append("\\n");
                    }"""

text = text.replace(old_context_builder, new_context_builder)

with open(path, 'w', encoding='utf-8') as f:
    f.write(text)
