# coding=utf-8
import os
import re

path = 'backend/src/main/java/com/aicodereview/service/ProjectAnalysisTransactionService.java'
with open(path, 'r', encoding='utf-8') as f:
    text = f.read()

text = text.replace('public void analyzeProjectInTransaction(Long projectId, String modelId) {',
'''public void analyzeProjectInTransaction(Long projectId, String modelId) {
        long globalStart = System.currentTimeMillis();''')

text = text.replace('project.setStatus(ProjectStatus.COMPLETED);',
'''project.setStatus(ProjectStatus.COMPLETED);
            System.out.println("E2E_TOTAL_TIME=" + (System.currentTimeMillis() - globalStart));''')

with open(path, 'w', encoding='utf-8') as f:
    f.write(text)
