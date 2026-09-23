package com.aicodereview.entity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "review_findings")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ReviewFinding {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private FindingCategory category;

    @Enumerated(EnumType.STRING)
    private FindingSeverity severity;

    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    private Integer lineNumber;

    @Column(columnDefinition = "TEXT")
    private String recommendation;

    @Column(columnDefinition = "TEXT")
    private String fixedCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "code_review_id")
    private CodeReview codeReview;
}
