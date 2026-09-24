package com.aicodereview.entity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "project_files")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ProjectFile {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    private Project project;
    
    private String relativePath;
    private String fileName;
    private String language;
    private String extension;
    private Long fileSize;
    private Integer lineCount;
    private String analysisStatus;
    
    @Column(columnDefinition = "LONGTEXT")
    private String content;
}
