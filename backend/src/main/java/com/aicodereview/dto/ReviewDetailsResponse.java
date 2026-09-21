package com.aicodereview.dto;
import lombok.Data;
import java.util.List;

@Data
public class ReviewDetailsResponse extends ReviewResponse {
    private ReviewSummaryResponse summary;
    private List<ReviewFindingResponse> findings;
}
