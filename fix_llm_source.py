import os

path = 'backend/src/main/java/com/aicodereview/ai/LLMClient.java'
with open(path, 'r', encoding='utf-8') as f:
    text = f.read()

# Add setSource("LLM") to parseResponse
text = text.replace('finding.setCategory(FindingCategory.valueOf(category));', 'finding.setCategory(FindingCategory.valueOf(category));\n                            finding.setSource("LLM");')

with open(path, 'w', encoding='utf-8') as f:
    f.write(text)
