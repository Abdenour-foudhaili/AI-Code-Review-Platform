package com.aicodereview.dto;
import lombok.Data;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class ReviewDetailsResponse extends ReviewResponse {
    private String sourceCode;
    private ReviewSummaryResponse summary;
    private List<ReviewFindingResponse> findings;
}
