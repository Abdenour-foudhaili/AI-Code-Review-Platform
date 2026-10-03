package com.aicodereview.analysis.model;

import java.util.List;
import java.util.Map;

public record ArchitectureGraph(List<GraphNode> nodes, List<GraphEdge> edges, Map<String, Integer> roleCounts,
                                List<String> packages, List<String> styles, List<String> technologies,
                                List<String> externalApis) {}
