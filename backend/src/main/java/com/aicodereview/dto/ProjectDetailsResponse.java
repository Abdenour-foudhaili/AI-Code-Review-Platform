package com.aicodereview.dto;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class ProjectDetailsResponse extends ProjectResponse {
    private List<ReviewFindingResponse> findings;
    private List<ProjectFileResponse> files;
}
