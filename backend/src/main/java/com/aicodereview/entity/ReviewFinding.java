package com.aicodereview.entity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "review_findings")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ReviewFinding {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "code_review_id")
    private CodeReview codeReview;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    private Project project;
    
    private String file; // used for project analysis to identify the file

    @Enumerated(EnumType.STRING)
    @Column(length = 30, columnDefinition = "varchar(30)")
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
    // New integration fields
    private String source; // BANDIT, RUFF, GITLEAKS, LLM
    private String ruleId;
    @Column(name = "column_num")
    private Integer columnNumber;
    private String confidence;
    private String cwe;
    private String owasp;
    
    @Column(columnDefinition = "TEXT")
    private String evidence;
    
    @Column(columnDefinition = "TEXT")
    private String explanation;
}
