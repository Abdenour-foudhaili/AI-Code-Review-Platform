package com.aicodereview.ai;

import com.aicodereview.entity.*;
import com.aicodereview.repository.CodeReviewRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Service
public class MockAIProvider implements AIReviewService {

    private final CodeReviewRepository repository;
    private final Random random = new Random();

    public MockAIProvider(CodeReviewRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<ReviewFinding> analyzeSourceCode(String sourceCode, ReviewType type, String filename) {
        List<ReviewFinding> findings = new ArrayList<>();
        if (type == ReviewType.FULL_REVIEW || type == ReviewType.SECURITY) {
            findings.addAll(generateSecurityFindings(sourceCode));
        }
        if (type == ReviewType.FULL_REVIEW || type == ReviewType.BUG_DETECTION) {
            findings.addAll(generateBugFindings(sourceCode));
        }
        if (type == ReviewType.FULL_REVIEW || type == ReviewType.CODE_QUALITY) {
            findings.addAll(generateCodeSmellFindings(sourceCode));
        }
        if (findings.isEmpty()) {
            findings.addAll(generateCodeSmellFindings(sourceCode));
        }
        for (ReviewFinding f : findings) {
            f.setFile(filename);
        }
        return findings;
    }

    @org.springframework.transaction.annotation.Transactional
    @Override
    public void performReview(CodeReview detachedReview) {
        try { Thread.sleep(2000); } catch (InterruptedException ignored) {}

        CodeReview review = repository.findById(detachedReview.getId()).orElse(null);
        if (review == null) return;

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
        
        repository.save(review);
    }

    private List<ReviewFinding> generateSecurityFindings(String code) {
        List<ReviewFinding> list = new ArrayList<>();
        list.add(ReviewFinding.builder()
                .category(FindingCategory.SECURITY)
                .severity(FindingSeverity.CRITICAL)
                .title("SQL Injection Vulnerability")
                .description("Input parameters are concatenated directly into SQL queries.")
                .lineNumber(findLineNumber(code, "SELECT", 42))
                .recommendation("Use parameterized statements (PreparedStatement) to prevent SQL injection.")
                .fixedCode("PreparedStatement ps = conn.prepareStatement(\"SELECT * FROM users WHERE username = ?\");\nps.setString(1, username);")
                .build());
        return list;
    }

    private List<ReviewFinding> generateBugFindings(String code) {
        List<ReviewFinding> list = new ArrayList<>();
        list.add(ReviewFinding.builder()
                .category(FindingCategory.BUG)
                .severity(FindingSeverity.HIGH)
                .title("Potential NullPointerException")
                .description("Object reference is used before checking for null.")
                .lineNumber(findLineNumber(code, ".", 15))
                .recommendation("Add a null check before calling methods on the object.")
                .fixedCode("if (user != null && user.getName() != null) {\n    System.out.println(user.getName());\n}")
                .build());
        return list;
    }

    private List<ReviewFinding> generateCodeSmellFindings(String code) {
        List<ReviewFinding> list = new ArrayList<>();
        list.add(ReviewFinding.builder()
                .category(FindingCategory.CODE_SMELL)
                .severity(FindingSeverity.MEDIUM)
                .title("Complex Method")
                .description("This method has high cyclomatic complexity and handles too many responsibilities.")
                .lineNumber(findLineNumber(code, "public", 10))
                .recommendation("Extract logic into smaller, well-named helper methods (Extract Method refactoring).")
                .build());
        return list;
    }

    private int findLineNumber(String code, String keyword, int defaultLine) {
        if (code == null || code.isEmpty()) return defaultLine;
        String[] lines = code.split("\n");
        for (int i = 0; i < lines.length; i++) {
            if (lines[i].contains(keyword)) {
                return i + 1;
            }
        }
        return defaultLine;
    }
}
