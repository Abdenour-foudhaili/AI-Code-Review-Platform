package com.aicodereview.analysis.parser;

import com.aicodereview.analysis.model.ParsedFile;

public interface CodeParser {
    boolean supports(String path);

    /** @throws IllegalArgumentException when the file cannot be parsed (the pipeline records it and continues). */
    ParsedFile parse(String path, String content);
}
