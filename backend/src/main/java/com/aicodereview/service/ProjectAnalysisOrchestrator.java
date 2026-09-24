package com.aicodereview.service;
import com.aicodereview.ai.AIReviewService;
import com.aicodereview.entity.*;
import com.aicodereview.repository.ProjectRepository;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ProjectAnalysisOrchestrator {
    
    private final ProjectRepository projectRepository;
    private final AIReviewService aiReviewService;

    public ProjectAnalysisOrchestrator(ProjectRepository projectRepository, AIReviewService aiReviewService) {
        this.projectRepository = projectRepository;
        this.aiReviewService = aiReviewService;
    }
    
    @Async
    public void runProjectAnalysis(Long projectId) {
        try {
            analyzeInTransaction(projectId);
        } catch (Exception e) {
            markAsFailed(projectId);
        }
    }
    
    @Transactional
    public void analyzeInTransaction(Long projectId) {
        Project project = projectRepository.findById(projectId).orElse(null);
        if (project == null) return;
        
        project.setStatus(ProjectStatus.ANALYZING);
        projectRepository.save(project);
        
        // Clear previous findings
        project.getFindings().clear();
        
        int totalIssues = 0;
        int critical = 0, high = 0, medium = 0, low = 0;
        
        for (ProjectFile pf : project.getFiles()) {
            List<ReviewFinding> newFindings = aiReviewService.analyzeSourceCode(pf.getContent(), ReviewType.FULL_REVIEW, pf.getRelativePath());
            
            for (ReviewFinding f : newFindings) {
                f.setProject(project);
                project.getFindings().add(f);
                
                totalIssues++;
                switch (f.getSeverity()) {
                    case CRITICAL -> critical++;
                    case HIGH -> high++;
                    case MEDIUM -> medium++;
                    case LOW, INFO -> low++;
                }
            }
            pf.setAnalysisStatus("COMPLETED");
        }
        
        String score = critical > 0 ? "D" : (high > 0 ? "C" : (medium > 0 ? "B" : "A"));
        
        project.setStatus(ProjectStatus.COMPLETED);
        project.setUpdatedAt(LocalDateTime.now());
        project.setQualityScore(score);
        
        projectRepository.save(project);
    }
    
    @Transactional
    public void markAsFailed(Long projectId) {
        Project project = projectRepository.findById(projectId).orElse(null);
        if (project != null) {
            project.setStatus(ProjectStatus.FAILED);
            projectRepository.save(project);
        }
    }
}
