package com.aicodereview.analysis;

import com.aicodereview.analysis.DiagramGenerator.Diagram;
import com.aicodereview.analysis.model.ArchitectureGraph;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

public final class AnalysisDtos {
    private AnalysisDtos() {}

    @Data
    public static class StartAnalysisRequest {
        @NotBlank(message = "Owner is required")
        @Pattern(regexp = "^[A-Za-z0-9_.-]{1,100}$", message = "Invalid owner")
        private String owner;

        @NotBlank(message = "Repository is required")
        @Pattern(regexp = "^[A-Za-z0-9_.-]{1,100}$", message = "Invalid repository name")
        private String repo;

        @jakarta.validation.constraints.Size(max = 200, message = "Branch name too long")
        private String branch;
    }

    public record AnalysisSummary(Long id, String name, String sourceType, String repositoryUrl, String branch, String commitHash,
                                  String status, String stage, String qualityScore, LocalDateTime createdAt, LocalDateTime completedAt,
                                  long findingsCount) {}

    public record StageInfo(String key, String label, String state) {}

    public record AnalysisStatus(Long id, String status, String stage, String message, int progress, List<StageInfo> stages, String error) {}

    public record FindingDto(Long id, String category, String severity, String title, String description, String file,
                             Integer lineNumber, String recommendation, String fixedCode, String source, String ruleId, String evidence) {}

    public record AnalysisDetails(AnalysisSummary summary, String summaryText, List<String> technologies, List<String> warnings,
                                  java.util.Map<String, Long> bySeverity, java.util.Map<String, Long> byCategory,
                                  java.util.Map<String, Long> bySource) {}

    public record ArchitectureResponse(ArchitectureGraph graph) {}

    public record DiagramsResponse(List<Diagram> diagrams) {}

    public record CodeContext(String file, int line, int startLine, String language, List<String> lines) {}
}
