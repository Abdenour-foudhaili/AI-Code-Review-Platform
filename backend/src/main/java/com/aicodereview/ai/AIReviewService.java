package com.aicodereview.ai;
import com.aicodereview.entity.CodeReview;
import com.aicodereview.entity.ReviewFinding;
import com.aicodereview.entity.ReviewType;
import java.util.List;

public interface AIReviewService {
    void performReview(CodeReview codeReview);
    List<ReviewFinding> analyzeSourceCode(String sourceCode, ReviewType type, String filename);
}
