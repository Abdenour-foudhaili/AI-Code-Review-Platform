package com.aicodereview.analysis.model;

import java.util.List;

public record GraphNode(String id, String label, String role, String kind, String file, String packageName,
                        int methods, int loc, int complexity, String basePath, List<String> endpoints) {}
