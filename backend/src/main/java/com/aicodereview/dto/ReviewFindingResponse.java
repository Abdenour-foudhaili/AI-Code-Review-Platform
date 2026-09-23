package com.aicodereview.dto;
import com.aicodereview.entity.FindingCategory;
import com.aicodereview.entity.FindingSeverity;
import lombok.Data;

@Data
public class ReviewFindingResponse {
    private Long id;
    private FindingCategory category;
    private FindingSeverity severity;
    private String title;
    private String description;
    private Integer lineNumber;
    private String recommendation;
    private String fixedCode;
}
