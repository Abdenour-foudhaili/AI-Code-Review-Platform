import os
import re

path = 'backend/src/main/java/com/aicodereview/service/ProjectAnalysisTransactionService.java'
with open(path, 'r', encoding='utf-8') as f:
    text = f.read()

# Fix the context generation
old_loop = """                    // Add first 100 lines or so for context to avoid sending huge files
                    String[] lines = pf.getContent().split("\\n");
                    for(int i=0; i<Math.min(lines.length, 150); i++){
                        context.append(i+1).append(": ").append(lines[i]).append("\\n");
                    }"""

new_loop = """                    // Add first 150 lines or so for context to avoid sending huge files
                    String[] lines = pf.getContent().split("\\n");
                    for(int i=0; i<Math.min(lines.length, 150); i++){
                        context.append(lines[i]).append("\\n");
                    }"""

text = text.replace(old_loop, new_loop)

with open(path, 'w', encoding='utf-8') as f:
    f.write(text)
