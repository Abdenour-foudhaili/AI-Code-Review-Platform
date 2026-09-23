package com.aicodereview.dto;
import lombok.Data;

@Data
public class ReviewSummaryResponse {
    private Long id;
    private Integer totalIssues;
    private Integer criticalIssues;
    private Integer highIssues;
    private Integer mediumIssues;
    private Integer lowIssues;
    private String qualityScore;
    private String summary;
}
