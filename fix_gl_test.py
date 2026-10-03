# coding=utf-8
import os

path = 'backend/src/test/java/com/aicodereview/analyzer/GitleaksAnalyzerTest.java'
with open(path, 'r', encoding='utf-8') as f:
    text = f.read()

text = text.replace('ghp_1234567890abcdefghijklmnopqrstuvwxyz', 'ghp_1234567890abcdefghijklmnopqrstuvwxyz0123')
with open(path, 'w', encoding='utf-8') as f:
    f.write(text)
