package com.aicodereview.mapper;
import com.aicodereview.dto.*;
import com.aicodereview.entity.CodeReview;
import com.aicodereview.entity.ReviewFinding;
import com.aicodereview.entity.ReviewSummary;
import org.springframework.stereotype.Component;
import java.util.stream.Collectors;

@Component
public class ReviewMapper {

    public ReviewResponse toResponse(CodeReview entity) {
        ReviewResponse dto = new ReviewResponse();
        dto.setId(entity.getId());
        dto.setProjectName(entity.getProjectName());
        dto.setLanguage(entity.getLanguage());
        dto.setReviewType(entity.getReviewType());
        dto.setStatus(entity.getStatus());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setCompletedAt(entity.getCompletedAt());
        return dto;
    }

    public ReviewDetailsResponse toDetailsResponse(CodeReview entity) {
        ReviewDetailsResponse dto = new ReviewDetailsResponse();
        dto.setId(entity.getId());
        dto.setProjectName(entity.getProjectName());
        dto.setLanguage(entity.getLanguage());
        dto.setReviewType(entity.getReviewType());
        dto.setStatus(entity.getStatus());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setCompletedAt(entity.getCompletedAt());
        
        if (entity.getSummary() != null) {
            dto.setSummary(toSummaryResponse(entity.getSummary()));
        }
        
        if (entity.getFindings() != null) {
            dto.setFindings(entity.getFindings().stream()
                    .map(this::toFindingResponse)
                    .collect(Collectors.toList()));
        }
        return dto;
    }

    public ReviewFindingResponse toFindingResponse(ReviewFinding entity) {
        ReviewFindingResponse dto = new ReviewFindingResponse();
        dto.setId(entity.getId());
        dto.setCategory(entity.getCategory());
        dto.setSeverity(entity.getSeverity());
        dto.setTitle(entity.getTitle());
        dto.setDescription(entity.getDescription());
        dto.setLineNumber(entity.getLineNumber());
        dto.setRecommendation(entity.getRecommendation());
        dto.setFixedCode(entity.getFixedCode());
        return dto;
    }

    public ReviewSummaryResponse toSummaryResponse(ReviewSummary entity) {
        ReviewSummaryResponse dto = new ReviewSummaryResponse();
        dto.setId(entity.getId());
        dto.setTotalIssues(entity.getTotalIssues());
        dto.setCriticalIssues(entity.getCriticalIssues());
        dto.setHighIssues(entity.getHighIssues());
        dto.setMediumIssues(entity.getMediumIssues());
        dto.setLowIssues(entity.getLowIssues());
        dto.setQualityScore(entity.getQualityScore());
        dto.setSummary(entity.getSummary());
        return dto;
    }
}
