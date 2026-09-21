package com.aicodereview.dto;
import com.aicodereview.entity.ReviewType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateReviewRequest {
    @NotBlank(message = "Project name is required")
    private String projectName;

    @NotBlank(message = "Language is required")
    private String language;

    @NotBlank(message = "Source code is required")
    private String sourceCode;

    @NotNull(message = "Review type is required")
    private ReviewType reviewType;
}
