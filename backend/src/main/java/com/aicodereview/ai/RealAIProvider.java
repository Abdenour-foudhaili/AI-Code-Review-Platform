package com.aicodereview.ai;

import com.aicodereview.entity.*;
import com.aicodereview.repository.CodeReviewRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@ConditionalOnProperty(name = "ai.llm.provider", havingValue = "openrouter")
public class RealAIProvider implements AIReviewService {

    private final CodeReviewRepository repository;
    private final LLMClient llmClient;

    public RealAIProvider(CodeReviewRepository repository, LLMClient llmClient) {
        this.repository = repository;
        this.llmClient = llmClient;
    }

    @Override
    public List<ReviewFinding> analyzeSourceCode(String sourceCode, ReviewType type, String filename) {
        if (sourceCode == null || sourceCode.trim().isEmpty()) {
            return new ArrayList<>();
        }
        
        // Chunking strategy for large files (e.g., > 12000 chars)
        int maxChunkSize = 12000;
        if (sourceCode.length() <= maxChunkSize) {
            List<ReviewFinding> findings = llmClient.analyzeCodeChunk(sourceCode, type, filename, 0);
            return findings != null ? findings : new ArrayList<>();
        }
        
        List<ReviewFinding> allFindings = new ArrayList<>();
        String[] lines = sourceCode.split("\n");
        StringBuilder currentChunk = new StringBuilder();
        int chunkStartLine = 1;
        
        for (int i = 0; i < lines.length; i++) {
            currentChunk.append(lines[i]).append("\n");
            // If chunk reaches max size or it's the last line
            if (currentChunk.length() >= maxChunkSize || i == lines.length - 1) {
                List<ReviewFinding> chunkFindings = llmClient.analyzeCodeChunk(
                        currentChunk.toString(), type, filename, chunkStartLine - 1);
                        
                if (chunkFindings != null) {
                    allFindings.addAll(chunkFindings);
                }
                currentChunk = new StringBuilder();
                chunkStartLine = i + 2; // Next line
            }
        }
        return allFindings;
    }

    @org.springframework.transaction.annotation.Transactional
    @Override
    public void performReview(CodeReview detachedReview) {
        CodeReview review = repository.findById(detachedReview.getId()).orElse(null);
        if (review == null) return;
        
        try {
            List<ReviewFinding> findings = analyzeSourceCode(review.getSourceCode(), review.getReviewType(), "SingleFile");

            int critical = 0, high = 0, medium = 0, low = 0;
            for (ReviewFinding f : findings) {
                review.addFinding(f);
                switch (f.getSeverity()) {
                    case CRITICAL -> critical++;
                    case HIGH -> high++;
                    case MEDIUM -> medium++;
                    case LOW, INFO -> low++;
                }
            }

            int total = critical + high + medium + low;
            String score = critical > 0 ? "D" : (high > 0 ? "C" : (medium > 0 ? "B" : "A"));
            String summaryText = String.format("AI analysis completed. Found %d issues. Code quality score is %s.", total, score);

            ReviewSummary summary = ReviewSummary.builder()
                    .totalIssues(total)
                    .criticalIssues(critical)
                    .highIssues(high)
                    .mediumIssues(medium)
                    .lowIssues(low)
                    .qualityScore(score)
                    .summary(summaryText)
                    .build();

            review.setSummary(summary);
            review.setStatus(ReviewStatus.COMPLETED);
            review.setCompletedAt(LocalDateTime.now());
        } catch (Exception e) {
            review.setStatus(ReviewStatus.FAILED);
            // Optionally store the error in summary
            ReviewSummary summary = ReviewSummary.builder()
                    .summary("Analysis failed due to: " + e.getMessage())
                    .build();
            review.setSummary(summary);
        }

        repository.save(review);
    }
}
