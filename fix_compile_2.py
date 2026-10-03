# coding=utf-8
import os
import re

path = 'backend/src/main/java/com/aicodereview/service/ProjectAnalysisTransactionService.java'
with open(path, 'r', encoding='utf-8') as f:
    text = f.read()

# Fix qualityScore
text = text.replace('project.setQualityScore(Math.max(0, score));', 
'''String grade = score >= 90 ? "A" : score >= 80 ? "B" : score >= 70 ? "C" : score >= 60 ? "D" : "F";
            project.setQualityScore(grade);''')

# Add markAsFailed
text = text.replace('public void analyzeProjectInTransaction(Long projectId, String modelId) {',
'''@Transactional
    public void markAsFailed(Long projectId) {
        projectRepository.findById(projectId).ifPresent(p -> {
            p.setStatus(ProjectStatus.FAILED);
            projectRepository.save(p);
        });
    }
    
    @Transactional
    public void analyzeProjectInTransaction(Long projectId, String modelId) {''')

with open(path, 'w', encoding='utf-8') as f:
    f.write(text)
