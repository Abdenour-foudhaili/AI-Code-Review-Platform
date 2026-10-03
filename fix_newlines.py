import os
import re

path = 'backend/src/main/java/com/aicodereview/service/ProjectAnalysisTransactionService.java'
with open(path, 'r', encoding='utf-8') as f:
    text = f.read()

# Fix literal newlines in Java strings
text = re.sub(r'append\(":\n"\)', 'append(":\\n")', text)
text = re.sub(r'append\("\n"\)', 'append("\\n")', text)
text = re.sub(r'append\("\nCode context:\n"\)', 'append("\\nCode context:\\n")', text)

with open(path, 'w', encoding='utf-8') as f:
    f.write(text)
