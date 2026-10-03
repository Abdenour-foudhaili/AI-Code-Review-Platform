package com.aicodereview.analyzer;

import com.aicodereview.entity.FindingCategory;
import com.aicodereview.entity.FindingSeverity;
import com.aicodereview.entity.ReviewFinding;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
public class BanditAnalyzer implements StaticAnalyzer {

    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public String getName() {
        return "BANDIT";
    }

    @Override
    public boolean isAvailable() {
        try {
            Process process = new ProcessBuilder("bandit", "--version").start();
            return process.waitFor(5, TimeUnit.SECONDS) && process.exitValue() == 0;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public List<ReviewFinding> analyze(Path projectDir) {
        List<ReviewFinding> findings = new ArrayList<>();
        try {
            File reportFile = File.createTempFile("bandit-report", ".json");
            ProcessBuilder pb = new ProcessBuilder("bandit", "-r", projectDir.toString(), "-f", "json", "-o", reportFile.getAbsolutePath());
            pb.directory(projectDir.toFile());
            Process process = pb.start();
            
            // Bandit returns 1 if issues are found, so we don't strictly check exitValue == 0
            process.waitFor(60, TimeUnit.SECONDS);

            if (reportFile.exists() && reportFile.length() > 0) {
                JsonNode root = mapper.readTree(reportFile);
                JsonNode results = root.path("results");
                if (results.isArray()) {
                    for (JsonNode node : results) {
                        ReviewFinding finding = new ReviewFinding();
                        finding.setSource("BANDIT");
                        finding.setRuleId(node.path("test_id").asText(""));
                        finding.setTitle(node.path("test_name").asText("Security Issue"));
                        finding.setDescription(node.path("issue_text").asText(""));
                        finding.setSeverity(mapSeverity(node.path("issue_severity").asText("")));
                        finding.setCategory(FindingCategory.SECURITY); // Bandit is purely security
                        finding.setConfidence(node.path("issue_confidence").asText(""));
                        
                        String filePath = node.path("filename").asText("");
                        // Remove projectDir prefix if absolute
                        if (filePath.startsWith(projectDir.toString())) {
                            filePath = filePath.substring(projectDir.toString().length());
                            if (filePath.startsWith("/") || filePath.startsWith("\\")) {
                                filePath = filePath.substring(1);
                            }
                        }
                        // Normalize backslashes
                        filePath = filePath.replace("\\", "/");
                        
                        finding.setFile(filePath);
                        finding.setLineNumber(node.path("line_number").asInt(0));
                        finding.setEvidence(node.path("code").asText(""));
                        
                        findings.add(finding);
                    }
                }
            }
            reportFile.delete();
        } catch (Exception e) {
            System.err.println("Bandit execution failed: " + e.getMessage());
        }
        return findings;
    }

    private FindingSeverity mapSeverity(String severity) {
        if (severity == null) return FindingSeverity.INFO;
        return switch (severity.toUpperCase()) {
            case "HIGH" -> FindingSeverity.CRITICAL; // or HIGH based on your model
            case "MEDIUM" -> FindingSeverity.MEDIUM;
            case "LOW" -> FindingSeverity.LOW;
            default -> FindingSeverity.INFO;
        };
    }
}
