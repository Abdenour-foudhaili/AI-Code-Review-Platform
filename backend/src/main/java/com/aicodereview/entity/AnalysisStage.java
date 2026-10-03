package com.aicodereview.entity;

/** Fine-grained progress of a repository analysis (Project.status stays coarse: READY/ANALYZING/COMPLETED/FAILED). */
public enum AnalysisStage {
    PENDING, CLONING, SCANNING, PARSING, ARCHITECTURE, GENERATING_DIAGRAM, ANALYZING, COMPLETED, FAILED
}
