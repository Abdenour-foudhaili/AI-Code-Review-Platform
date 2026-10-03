import os
import re

path = 'backend/src/main/java/com/aicodereview/service/ProjectAnalysisTransactionService.java'
with open(path, 'r', encoding='utf-8') as f:
    text = f.read()

# Add imports
imports = """import com.aicodereview.analyzer.StaticAnalyzer;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Map;
import java.util.stream.Collectors;
"""

if "import com.aicodereview.analyzer.StaticAnalyzer;" not in text:
    text = text.replace("import java.util.List;", "import java.util.List;\n" + imports)

# Inject analyzers into constructor
if "private final List<StaticAnalyzer> analyzers;" not in text:
    text = text.replace("private final AIReviewService aiReviewService;", 
                        "private final AIReviewService aiReviewService;\n    private final List<StaticAnalyzer> analyzers;")
    
    text = text.replace("public ProjectAnalysisTransactionService(ProjectRepository projectRepository, AIReviewService aiReviewService) {",
                        "public ProjectAnalysisTransactionService(ProjectRepository projectRepository, AIReviewService aiReviewService, List<StaticAnalyzer> analyzers) {")
    
    text = text.replace("this.aiReviewService = aiReviewService;",
                        "this.aiReviewService = aiReviewService;\n        this.analyzers = analyzers;")


# Replace the loop with the new logic
old_loop = """        for (ProjectFile pf : project.getFiles()) {
            List<ReviewFinding> newFindings = aiReviewService.analyzeSourceCode(pf.getContent(), ReviewType.FULL_REVIEW, pf.getRelativePath(), modelId);
            
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
        }"""

new_loop = """        Path tempDir = null;
        try {
            tempDir = Files.createTempDirectory("project_analyze_" + projectId);
            for (ProjectFile pf : project.getFiles()) {
                Path filePath = tempDir.resolve(pf.getRelativePath());
                Files.createDirectories(filePath.getParent());
                Files.writeString(filePath, pf.getContent(), StandardOpenOption.CREATE);
            }
            
            List<ReviewFinding> staticFindings = new ArrayList<>();
            for (StaticAnalyzer analyzer : analyzers) {
                if (analyzer.isAvailable()) {
                    System.out.println("Running " + analyzer.getName());
                    List<ReviewFinding> toolFindings = analyzer.analyze(tempDir);
                    staticFindings.addAll(toolFindings);
                } else {
                    System.out.println(analyzer.getName() + " is SKIPPED (not available)");
                }
            }
            
            // Deduplication (basic)
            List<ReviewFinding> deduplicated = new ArrayList<>();
            for (ReviewFinding f : staticFindings) {
                boolean duplicate = deduplicated.stream().anyMatch(existing -> 
                    existing.getFile().equals(f.getFile()) &&
                    existing.getLineNumber() != null &&
                    existing.getLineNumber().equals(f.getLineNumber()) &&
                    existing.getCategory() == f.getCategory() &&
                    existing.getSource().equals(f.getSource())
                );
                if (!duplicate) {
                    deduplicated.add(f);
                }
            }
            
            // Group by file
            Map<String, List<ReviewFinding>> findingsByFile = deduplicated.stream()
                .collect(Collectors.groupingBy(ReviewFinding::getFile));
                
            for (ProjectFile pf : project.getFiles()) {
                List<ReviewFinding> fileFindings = findingsByFile.get(pf.getRelativePath());
                if (fileFindings != null && !fileFindings.isEmpty()) {
                    // Enrich with LLM
                    StringBuilder context = new StringBuilder();
                    context.append("Static analysis found the following issues in ").append(pf.getRelativePath()).append(":\n");
                    for (ReviewFinding f : fileFindings) {
                        context.append("- ").append(f.getSource()).append(" (").append(f.getRuleId()).append(") at line ")
                               .append(f.getLineNumber()).append(": ").append(f.getDescription()).append("\n");
                    }
                    context.append("\nCode context:\n");
                    // Add first 100 lines or so for context to avoid sending huge files
                    String[] lines = pf.getContent().split("\\n");
                    for(int i=0; i<Math.min(lines.length, 150); i++){
                        context.append(i+1).append(": ").append(lines[i]).append("\\n");
                    }
                    
                    // Call LLM for targeted enrichment
                    List<ReviewFinding> enriched = aiReviewService.analyzeSourceCode(context.toString(), ReviewType.FULL_REVIEW, pf.getRelativePath(), modelId);
                    
                    // If LLM succeeds, we could merge. But prompt says "Conserve séparément la détection originale et l'enrichissement IA"
                    // To keep it simple, we just save the static findings AND the LLM explanations if they exist.
                    // Or we merge the LLM's explanation back into the static finding.
                    // Let's enrich the static findings with LLM text if they match.
                    // For now, save static findings, and if LLM provided findings, save them too as "LLM" source.
                    for (ReviewFinding f : fileFindings) {
                        f.setProject(project);
                        project.getFindings().add(f);
                        totalIssues++;
                        countSeverity(f);
                    }
                    if (enriched != null) {
                        for (ReviewFinding f : enriched) {
                            f.setProject(project);
                            f.setSource("LLM");
                            project.getFindings().add(f);
                            totalIssues++;
                            countSeverity(f);
                        }
                    }
                } else {
                    // Configurable: should we send files with NO static findings? 
                    // Let's send a small chunk to LLM to check for architectural issues if desired, 
                    // or skip to save time. The prompt says: "Ne pas effectuer un appel LLM pour chaque finding par défaut. Ne pas envoyer un projet entier lorsqu'un petit extrait suffit."
                    // Let's skip files with no static findings to save time, achieving the performance goal.
                }
                pf.setAnalysisStatus("COMPLETED");
            }

        } catch (Exception e) {
            System.err.println("Error during analysis: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (tempDir != null) {
                try {
                    deleteDirectory(tempDir.toFile());
                } catch (Exception ignore) {}
            }
        }"""

if "Path tempDir = null;" not in text:
    text = text.replace(old_loop, new_loop)
    
    # Add countSeverity helper
    helper = """
    private void countSeverity(ReviewFinding f) {
        // We will do it inline or replace logic. Let's just inline it instead of a helper to be safe.
    }
    """
    # Wait, instead of helper, I'll inline the severity count in Python. Let me rewrite the replace string properly.

with open(path, 'w', encoding='utf-8') as f:
    f.write(text)
