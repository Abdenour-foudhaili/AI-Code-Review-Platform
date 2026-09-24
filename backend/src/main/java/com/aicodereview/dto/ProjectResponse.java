package com.aicodereview.dto;
import com.aicodereview.entity.ProjectStatus;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ProjectResponse {
    private Long id;
    private String name;
    private String description;
    private ProjectStatus status;
    private String detectedLanguages;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String qualityScore;
    private int filesCount;
}
