package com.aicodereview.service;

import com.aicodereview.ai.AIReviewService;
import com.aicodereview.analyzer.StaticAnalyzer;
import com.aicodereview.entity.Project;
import com.aicodereview.entity.ProjectFile;
import com.aicodereview.entity.ProjectStatus;
import com.aicodereview.entity.ReviewFinding;
import com.aicodereview.entity.ReviewType;
import com.aicodereview.repository.ProjectRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ProjectAnalysisTransactionService {

    private final ProjectRepository projectRepository;
    private final AIReviewService aiReviewService;
    private final List<StaticAnalyzer> analyzers;

    public ProjectAnalysisTransactionService(ProjectRepository projectRepository, AIReviewService aiReviewService, List<StaticAnalyzer> analyzers) {
        this.projectRepository = projectRepository;
        this.aiReviewService = aiReviewService;
        this.analyzers = analyzers;
    }

    @Transactional
    public void markAsFailed(Long projectId) {
        projectRepository.findById(projectId).ifPresent(p -> {
            p.setStatus(ProjectStatus.FAILED);
            projectRepository.save(p);
        });
    }
    
    @Transactional
    public void analyzeProjectInTransaction(Long projectId, String modelId) {
        long globalStart = System.currentTimeMillis();
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found: " + projectId));

        try {
            project.getFindings().clear();

            if (project.getFiles() == null || project.getFiles().isEmpty()) {
                project.setStatus(ProjectStatus.COMPLETED);
                projectRepository.save(project);
                return;
            }

            Path tempDir = Files.createTempDirectory("project_analyze_");
            for (ProjectFile pf : project.getFiles()) {
                Path filePath = tempDir.resolve(pf.getRelativePath());
                Files.createDirectories(filePath.getParent());
                Files.writeString(filePath, pf.getContent());
            }

            List<ReviewFinding> allStaticFindings = new ArrayList<>();
            for (StaticAnalyzer analyzer : analyzers) {
                if (analyzer.isAvailable()) {
                    System.out.println("Running " + analyzer.getName());
                    List<ReviewFinding> toolFindings = analyzer.analyze(tempDir);
                    if (toolFindings != null) {
                        allStaticFindings.addAll(toolFindings);
                    }
                } else {
                    System.out.println(analyzer.getName() + " is SKIPPED (not available)");
                }
            }

            // Simple deduplication
            List<ReviewFinding> deduped = new ArrayList<>();
            for (ReviewFinding f : allStaticFindings) {
                boolean duplicate = false;
                for (ReviewFinding d : deduped) {
                    if (f.getFile().equals(d.getFile()) && f.getLineNumber() != null && f.getLineNumber().equals(d.getLineNumber()) && f.getRuleId().equals(d.getRuleId())) {
                        duplicate = true;
                        break;
                    }
                }
                if (!duplicate) {
                    deduped.add(f);
                }
            }

            Map<String, List<ReviewFinding>> findingsByFile = deduped.stream()
                .collect(Collectors.groupingBy(ReviewFinding::getFile));

            int totalIssues = 0;
            int critical = 0, high = 0, medium = 0, low = 0;

            for (ProjectFile pf : project.getFiles()) {
                List<ReviewFinding> fileFindings = findingsByFile.get(pf.getRelativePath());
                if (fileFindings != null && !fileFindings.isEmpty()) {
                    StringBuilder context = new StringBuilder();
                    context.append("Static analysis found the following issues in ").append(pf.getRelativePath()).append(":\n");
                    for (ReviewFinding f : fileFindings) {
                        context.append("- ").append(f.getSource()).append(" (").append(f.getRuleId()).append(") at line ")
                               .append(f.getLineNumber()).append(": ").append(f.getDescription()).append("\n");
                    }
                    context.append("\nCode context:\n");
                    String[] lines = pf.getContent().split("\n");
                    
                    for (ReviewFinding f : fileFindings) {
                        if ("GITLEAKS".equals(f.getSource()) && f.getLineNumber() != null && f.getLineNumber() > 0 && f.getLineNumber() <= lines.length) {
                            lines[f.getLineNumber() - 1] = "/* *** MASKED SECRET *** */";
                        }
                    }
                    
                    for(int i=0; i<Math.min(lines.length, 150); i++){
                        context.append(lines[i]).append("\n");
                    }

                    List<ReviewFinding> enriched = null;
                    try {
                        enriched = aiReviewService.analyzeSourceCode(context.toString(), ReviewType.FULL_REVIEW, pf.getRelativePath(), modelId);
                    } catch (Exception e) {
                        System.err.println("LLM Enrichment failed for " + pf.getRelativePath() + ": " + e.getMessage());
                    }

                    for (ReviewFinding f : fileFindings) {
                        f.setProject(project);
                        project.getFindings().add(f);
                        totalIssues++;
                        if (f.getSeverity() != null) {
                            switch (f.getSeverity()) {
                                case CRITICAL -> critical++;
                                case HIGH -> high++;
                                case MEDIUM -> medium++;
                                case LOW, INFO -> low++;
                            }
                        }
                    }
                    if (enriched != null) {
                        for (ReviewFinding f : enriched) {
                            f.setProject(project);
                            f.setSource("LLM");
                            project.getFindings().add(f);
                            totalIssues++;
                            if (f.getSeverity() != null) {
                                switch (f.getSeverity()) {
                                    case CRITICAL -> critical++;
                                    case HIGH -> high++;
                                    case MEDIUM -> medium++;
                                    case LOW, INFO -> low++;
                                }
                            }
                        }
                    }
                } else {
                    List<ReviewFinding> aiFindings = null;
                    try {
                        aiFindings = aiReviewService.analyzeSourceCode(pf.getContent(), ReviewType.FULL_REVIEW, pf.getRelativePath(), modelId);
                    } catch (Exception e) {
                        System.err.println("Fallback LLM failed for " + pf.getRelativePath() + ": " + e.getMessage());
                    }
                    if (aiFindings != null) {
                        for (ReviewFinding f : aiFindings) {
                            f.setProject(project);
                            f.setSource("LLM");
                            project.getFindings().add(f);
                            totalIssues++;
                            if (f.getSeverity() != null) {
                                switch (f.getSeverity()) {
                                    case CRITICAL -> critical++;
                                    case HIGH -> high++;
                                    case MEDIUM -> medium++;
                                    case LOW, INFO -> low++;
                                }
                            }
                        }
                    }
                }
            }

            int score = 100 - (critical * 20 + high * 10 + medium * 5 + low);
            String grade = score >= 90 ? "A" : score >= 80 ? "B" : score >= 70 ? "C" : score >= 60 ? "D" : "F";
            project.setQualityScore(grade);
            project.setStatus(ProjectStatus.COMPLETED);
            projectRepository.save(project);
            System.out.println("E2E_TOTAL_TIME=" + (System.currentTimeMillis() - globalStart));

        } catch (Exception e) {
            System.err.println("Error during analysis: " + e.getMessage());
            e.printStackTrace();
            project.setStatus(ProjectStatus.FAILED);
            projectRepository.save(project);
        }
    }
}
