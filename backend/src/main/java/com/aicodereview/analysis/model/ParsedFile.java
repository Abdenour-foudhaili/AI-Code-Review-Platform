package com.aicodereview.analysis.model;

import java.util.List;

public record ParsedFile(String path, String language, String packageName, List<String> imports,
                         List<CodeElement> elements, List<String> apiPaths, int loc) {}
