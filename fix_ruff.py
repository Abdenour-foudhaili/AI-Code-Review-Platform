import os

path = 'backend/src/main/java/com/aicodereview/analyzer/RuffAnalyzer.java'
with open(path, 'r', encoding='utf-8') as f:
    text = f.read()

text = text.replace('FindingCategory.CODE_QUALITY', 'FindingCategory.CODE_SMELL')

with open(path, 'w', encoding='utf-8') as f:
    f.write(text)
