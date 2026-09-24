package com.aicodereview.dto;
import lombok.Data;

@Data
public class ProjectFileResponse {
    private Long id;
    private String relativePath;
    private String language;
    private String analysisStatus;
}
