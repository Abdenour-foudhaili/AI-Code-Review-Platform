# coding=utf-8
import os
import re

path = 'backend/src/main/java/com/aicodereview/service/ProjectAnalysisTransactionService.java'
with open(path, 'r', encoding='utf-8') as f:
    text = f.read()

text = text.replace('@Transactional\n    @Transactional', '@Transactional')

with open(path, 'w', encoding='utf-8') as f:
    f.write(text)
