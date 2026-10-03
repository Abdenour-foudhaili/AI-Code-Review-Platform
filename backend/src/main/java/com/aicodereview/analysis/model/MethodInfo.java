package com.aicodereview.analysis.model;

import java.util.List;

/** calls: "field.method" pairs (receiver is a field / injected dependency), used to build CALLS edges. */
public record MethodInfo(String name, String returnType, int paramCount, int startLine, int endLine, int complexity,
                         List<String> calls) {
    public int length() { return Math.max(0, endLine - startLine + 1); }
}
