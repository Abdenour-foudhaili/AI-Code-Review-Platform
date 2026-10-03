# coding=utf-8
import os
import re

path = 'backend/src/main/java/com/aicodereview/service/ProjectAnalysisTransactionService.java'
with open(path, 'r', encoding='utf-8') as f:
    text = f.read()

# Wrap LLM call in try-catch
target1 = """                    List<ReviewFinding> enriched = aiReviewService.analyzeSourceCode(context.toString(), ReviewType.FULL_REVIEW, pf.getRelativePath(), modelId);
                    
                    // If LLM succeeds"""

replacement1 = """                    List<ReviewFinding> enriched = null;
                    try {
                        enriched = aiReviewService.analyzeSourceCode(context.toString(), ReviewType.FULL_REVIEW, pf.getRelativePath(), modelId);
                    } catch (Exception e) {
                        System.err.println("LLM Enrichment failed for " + pf.getRelativePath() + ": " + e.getMessage());
                    }
                    
                    // If LLM succeeds"""

text = text.replace(target1, replacement1)

target2 = """                    List<ReviewFinding> aiFindings = aiReviewService.analyzeSourceCode(pf.getContent(), ReviewType.FULL_REVIEW, pf.getRelativePath(), modelId);
                    if (aiFindings != null) {"""

replacement2 = """                    List<ReviewFinding> aiFindings = null;
                    try {
                        aiFindings = aiReviewService.analyzeSourceCode(pf.getContent(), ReviewType.FULL_REVIEW, pf.getRelativePath(), modelId);
                    } catch (Exception e) {
                        System.err.println("Fallback LLM failed for " + pf.getRelativePath() + ": " + e.getMessage());
                    }
                    if (aiFindings != null) {"""

text = text.replace(target2, replacement2)

with open(path, 'w', encoding='utf-8') as f:
    f.write(text)
