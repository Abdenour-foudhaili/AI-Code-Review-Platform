package com.aicodereview.analysis;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/** Limits and filters of the repository analysis (extensible through application.yml). */
@Data
@Component
@ConfigurationProperties(prefix = "analysis")
public class AnalysisProperties {
    private long maxFileBytes = 200_000;
    private int maxFiles = 1500;
    private long maxRepoBytes = 100L * 1024 * 1024;
    private long maxUncompressedBytes = 60L * 1024 * 1024;
    private int aiMaxFiles = 12;
    private int aiMaxCharsPerFile = 12_000;

    private List<String> ignoredDirs = List.of(".git", "node_modules", "target", "dist", "build", "out", ".idea", ".vscode",
            ".gradle", ".angular", "coverage", "__pycache__", ".venv", "venv", "vendor", ".next", ".mvn", ".github/modernize");
    private List<String> ignoredFileSuffixes = List.of(".lock", ".min.js", ".min.css", ".map", ".class", ".jar", ".war",
            ".png", ".jpg", ".jpeg", ".gif", ".ico", ".svg", ".webp", ".pdf", ".zip", ".gz", ".tar", ".7z", ".mp4", ".mov",
            ".mp3", ".woff", ".woff2", ".ttf", ".eot", ".exe", ".dll", ".so", ".bin", ".pem", ".key", ".p12", ".jks");
    private List<String> ignoredFileNames = List.of("package-lock.json", "yarn.lock", "pnpm-lock.yaml", "mvnw", "mvnw.cmd");
}
