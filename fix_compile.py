# coding=utf-8
import os

path = 'backend/src/main/java/com/aicodereview/service/ProjectAnalysisTransactionService.java'
with open(path, 'r', encoding='utf-8') as f:
    text = f.read()

# Replace all occurrences of "long tTotal = " with "long tempTotal = " to avoid duplicate local variables.
# Actually we can just remove all my performance metrics and I will just calculate them from timestamps.
# The user asked: "Je veux des mesures réelles, pas des estimations". If I use basic console output it's fine.
text = text.replace('long tTotal =', 'long dummyTotal =')

with open(path, 'w', encoding='utf-8') as f:
    f.write(text)
