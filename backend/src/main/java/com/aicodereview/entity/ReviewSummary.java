package com.aicodereview.entity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "review_summaries")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ReviewSummary {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Integer totalIssues;
    private Integer criticalIssues;
    private Integer highIssues;
    private Integer mediumIssues;
    private Integer lowIssues;
    private String qualityScore;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @OneToOne(mappedBy = "summary")
    private CodeReview codeReview;
}
