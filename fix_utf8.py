import os

files = [
    'backend/src/main/java/com/aicodereview/analyzer/BanditAnalyzer.java',
    'backend/src/main/java/com/aicodereview/analyzer/GitleaksAnalyzer.java',
    'backend/src/main/java/com/aicodereview/analyzer/RuffAnalyzer.java',
    'backend/src/main/java/com/aicodereview/analyzer/StaticAnalyzer.java'
]

for file in files:
    with open(file, 'rb') as f:
        content = f.read()
    if content.startswith(b'\xef\xbb\xbf'):
        content = content[3:]
        with open(file, 'wb') as f:
            f.write(content)
            
path = 'backend/src/main/java/com/aicodereview/service/ProjectAnalysisTransactionService.java'
with open(path, 'r', encoding='utf-8') as f:
    text = f.read()
    
# Remove \n inside string literals from my string builder
text = text.replace('\\n', '\\n')
# Wait, it was `append("\\n")`. Java strings need `append("\n")`.
text = text.replace('.append("\\n")', '.append("\\n")')
text = text.replace('append(":\n");', 'append(":\\n");')
text = text.replace('append("\n");', 'append("\\n");')
text = text.replace('append("\nCode context:\n");', 'append("\\nCode context:\\n");')

with open(path, 'w', encoding='utf-8') as f:
    f.write(text)
