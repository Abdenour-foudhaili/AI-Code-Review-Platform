package com.aicodereview.ai;
import com.aicodereview.entity.*;
import com.aicodereview.repository.CodeReviewRepository;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;

@Service
public class MockAIProvider implements AIReviewService {

    private final CodeReviewRepository repository;

    public MockAIProvider(CodeReviewRepository repository) {
        this.repository = repository;
    }

    @Override
    public void performReview(CodeReview review) {
        // Simulate AI processing delay
        try { Thread.sleep(2000); } catch (InterruptedException ignored) {}

        // Mock findings
        ReviewFinding finding1 = ReviewFinding.builder()
                .category(FindingCategory.SECURITY)
                .severity(FindingSeverity.HIGH)
                .title("SQL Injection")
                .description("Input is not sanitized before database query.")
                .lineNumber(42)
                .recommendation("Use parameterized queries.")
                .fixedCode("PreparedStatement ps = conn.prepareStatement(\"SELECT * FROM users WHERE id = ?\");")
                .build();

        ReviewFinding finding2 = ReviewFinding.builder()
                .category(FindingCategory.CODE_SMELL)
                .severity(FindingSeverity.MEDIUM)
                .title("Unused Variable")
                .description("The variable 'temp' is declared but never used.")
                .lineNumber(15)
                .recommendation("Remove the unused variable.")
                .build();

        review.addFinding(finding1);
        review.addFinding(finding2);

        // Mock summary
        ReviewSummary summary = ReviewSummary.builder()
                .totalIssues(2)
                .criticalIssues(0)
                .highIssues(1)
                .mediumIssues(1)
                .lowIssues(0)
                .qualityScore("B")
                .summary("Code has a few issues, including one security vulnerability.")
                .build();

        review.setSummary(summary);
        review.setStatus(ReviewStatus.COMPLETED);
        review.setCompletedAt(LocalDateTime.now());
        
        repository.save(review);
    }
}
