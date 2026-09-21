package com.aicodereview.dto;
import com.aicodereview.entity.ReviewStatus;
import com.aicodereview.entity.ReviewType;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ReviewResponse {
    private Long id;
    private String projectName;
    private String language;
    private ReviewType reviewType;
    private ReviewStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
}
