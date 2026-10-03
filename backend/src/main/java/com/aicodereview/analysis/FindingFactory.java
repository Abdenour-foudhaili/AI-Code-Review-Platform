package com.aicodereview.analysis;

import com.aicodereview.entity.FindingCategory;
import com.aicodereview.entity.FindingSeverity;
import com.aicodereview.entity.ReviewFinding;

/** Builds findings produced by the (deterministic) static analysis. Source is always "STATIC". */
public final class FindingFactory {
    private FindingFactory() {}

    public static ReviewFinding staticFinding(String ruleId, FindingCategory category, FindingSeverity severity, String file,
                                              Integer line, String title, String description, String recommendation, String evidence) {
        ReviewFinding f = new ReviewFinding();
        f.setSource("STATIC");
        f.setRuleId(ruleId);
        f.setCategory(category);
        f.setSeverity(severity);
        f.setFile(file);
        f.setLineNumber(line);
        f.setTitle(title.length() > 250 ? title.substring(0, 250) : title);
        f.setDescription(description);
        f.setRecommendation(recommendation);
        f.setEvidence(evidence == null ? null : (evidence.length() > 300 ? evidence.substring(0, 300) : evidence));
        f.setConfidence("HIGH");
        return f;
    }
}
