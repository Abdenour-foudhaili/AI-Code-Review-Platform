package com.aicodereview.analysis.model;

public record ScannedFile(String path, String content, String language, String extension, long size, int lines) {
    public String fileName() {
        int i = path.lastIndexOf('/');
        return i >= 0 ? path.substring(i + 1) : path;
    }
}
