# coding=utf-8
import os

path = 'backend/src/test/java/com/aicodereview/service/LLMSecretLeakTest.java'
with open(path, 'r', encoding='utf-8') as f:
    text = f.read()

text = text.replace('sk-test-1111111111111111', 'ghp_1111111111111111111111111111111111111111')
text = text.replace('sk-test-2222222222222222', 'ghp_2222222222222222222222222222222222222222')

with open(path, 'w', encoding='utf-8') as f:
    f.write(text)
