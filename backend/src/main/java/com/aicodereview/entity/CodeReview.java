package com.aicodereview.entity;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "code_reviews")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CodeReview {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String projectName;
    private String language;

    @Column(columnDefinition = "LONGTEXT")
    private String sourceCode;

    @Enumerated(EnumType.STRING)
    private ReviewType reviewType;

    @Enumerated(EnumType.STRING)
    private ReviewStatus status;

    private LocalDateTime createdAt;
    private LocalDateTime completedAt;

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "summary_id")
    private ReviewSummary summary;

    @OneToMany(mappedBy = "codeReview", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ReviewFinding> findings = new ArrayList<>();

    public void addFinding(ReviewFinding finding) {
        findings.add(finding);
        finding.setCodeReview(this);
    }
}
