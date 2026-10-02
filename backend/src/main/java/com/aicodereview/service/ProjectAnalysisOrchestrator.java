package com.aicodereview.service;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class ProjectAnalysisOrchestrator {
    
    private final ProjectAnalysisTransactionService transactionService;

    public ProjectAnalysisOrchestrator(ProjectAnalysisTransactionService transactionService) {
        this.transactionService = transactionService;
    }
    
    @Async
    public void runProjectAnalysis(Long projectId, String modelId) {
        try {
            transactionService.analyzeProjectInTransaction(projectId, modelId);
        } catch (Exception e) {
            System.err.println("Analysis failed for project " + projectId + ": " + e.getMessage());
            e.printStackTrace();
            transactionService.markAsFailed(projectId);
        }
    }
}
