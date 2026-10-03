package com.aicodereview.entity;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "projects")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Project {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private String description;
    @Enumerated(EnumType.STRING)
    private ProjectStatus status;
    private String detectedLanguages;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String qualityScore;

    // --- ownership + repository analysis metadata (all nullable: legacy rows stay valid) ---
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    private User owner;

    /** ZIP or GITHUB. */
    private String sourceType;
    private String repositoryFullName;
    private String repositoryUrl;
    private String branch;
    private String commitHash;

    @Enumerated(EnumType.STRING)
    private AnalysisStage stage;
    private String stageMessage;
    private LocalDateTime completedAt;

    @Column(columnDefinition = "TEXT")
    private String technologies;
    @Column(columnDefinition = "TEXT")
    private String summaryText;
    @Column(columnDefinition = "TEXT")
    private String errorMessage;
    @Column(columnDefinition = "LONGTEXT")
    private String architectureJson;
    @Column(columnDefinition = "LONGTEXT")
    private String diagramsJson;

    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ProjectFile> files = new ArrayList<>();

    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ReviewFinding> findings = new ArrayList<>();
}
