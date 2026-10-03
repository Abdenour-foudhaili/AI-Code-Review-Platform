package com.aicodereview.analysis.model;

/** type: DEPENDS, CALLS, EXTENDS, IMPLEMENTS, IMPORT, HTTP. */
public record GraphEdge(String from, String to, String type) {}
