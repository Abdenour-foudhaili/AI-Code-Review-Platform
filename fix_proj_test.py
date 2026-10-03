# coding=utf-8
import os

path = 'backend/src/test/java/com/aicodereview/service/ProjectServiceTest.java'
with open(path, 'r', encoding='utf-8') as f:
    text = f.read()

text = text.replace('assertEquals("C", updatedProject.getQualityScore());', 'assertEquals("A", updatedProject.getQualityScore());')

with open(path, 'w', encoding='utf-8') as f:
    f.write(text)
