package com.aicodereview.mapper;
import com.aicodereview.dto.*;
import com.aicodereview.entity.Project;
import com.aicodereview.entity.ProjectFile;
import org.springframework.stereotype.Component;
import java.util.stream.Collectors;

@Component
public class ProjectMapper {
    private final ReviewMapper reviewMapper;
    
    public ProjectMapper(ReviewMapper reviewMapper) {
        this.reviewMapper = reviewMapper;
    }

    public ProjectResponse toResponse(Project entity) {
        ProjectResponse dto = new ProjectResponse();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setDescription(entity.getDescription());
        dto.setStatus(entity.getStatus());
        dto.setDetectedLanguages(entity.getDetectedLanguages());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        dto.setQualityScore(entity.getQualityScore());
        dto.setFilesCount(entity.getFiles() != null ? entity.getFiles().size() : 0);
        return dto;
    }
    
    public ProjectDetailsResponse toDetailsResponse(Project entity) {
        ProjectDetailsResponse dto = new ProjectDetailsResponse();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setDescription(entity.getDescription());
        dto.setStatus(entity.getStatus());
        dto.setDetectedLanguages(entity.getDetectedLanguages());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        dto.setQualityScore(entity.getQualityScore());
        dto.setFilesCount(entity.getFiles() != null ? entity.getFiles().size() : 0);
        
        if (entity.getFindings() != null) {
            dto.setFindings(entity.getFindings().stream()
                .map(reviewMapper::toFindingResponse)
                .collect(Collectors.toList()));
        }
        
        if (entity.getFiles() != null) {
            dto.setFiles(entity.getFiles().stream().map(f -> {
                ProjectFileResponse fResp = new ProjectFileResponse();
                fResp.setId(f.getId());
                fResp.setRelativePath(f.getRelativePath());
                fResp.setLanguage(f.getLanguage());
                fResp.setAnalysisStatus(f.getAnalysisStatus());
                return fResp;
            }).collect(Collectors.toList()));
        }
        return dto;
    }
}
