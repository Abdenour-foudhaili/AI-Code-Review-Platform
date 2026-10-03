package com.aicodereview.analyzer;

import com.aicodereview.entity.ReviewFinding;
import java.nio.file.Path;
import java.util.List;

public interface StaticAnalyzer {
    String getName();
    boolean isAvailable();
    List<ReviewFinding> analyze(Path projectDir);
}
