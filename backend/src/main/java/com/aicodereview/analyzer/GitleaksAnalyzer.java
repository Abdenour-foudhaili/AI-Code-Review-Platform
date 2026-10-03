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
public class GitleaksAnalyzer implements StaticAnalyzer {

    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public String getName() {
        return "GITLEAKS";
    }

    @Override
    public boolean isAvailable() {
        try {
            Process process = new ProcessBuilder("gitleaks", "version").start();
            return process.waitFor(5, TimeUnit.SECONDS) && process.exitValue() == 0;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public List<ReviewFinding> analyze(Path projectDir) {
        List<ReviewFinding> findings = new ArrayList<>();
        try {
            File reportFile = File.createTempFile("gitleaks-report", ".json");
            ProcessBuilder pb = new ProcessBuilder("gitleaks", "detect", "--source", projectDir.toString(), "--no-git", "--report-format", "json", "--report-path", reportFile.getAbsolutePath());
            pb.directory(projectDir.toFile());
            pb.redirectOutput(ProcessBuilder.Redirect.DISCARD);
            pb.redirectError(ProcessBuilder.Redirect.DISCARD);
            Process process = pb.start();
            
            process.waitFor(60, TimeUnit.SECONDS);

            if (reportFile.exists() && reportFile.length() > 0) {
                JsonNode results = mapper.readTree(reportFile);
                if (results.isArray()) {
                    for (JsonNode node : results) {
                        ReviewFinding finding = new ReviewFinding();
                        finding.setSource("GITLEAKS");
                        finding.setRuleId(node.path("RuleID").asText(""));
                        finding.setTitle("Potential Secret Detected");
                        // DO NOT store the actual secret in the DB or send to LLM
                        String description = node.path("Description").asText("");
                        finding.setDescription("A hardcoded secret was detected: " + description);
                        finding.setSeverity(FindingSeverity.CRITICAL);
                        finding.setCategory(FindingCategory.SECURITY);
                        
                        try {
                        String rawPathStr = node.path("File").asText("");
                        System.out.println("GITLEAKS RAW PATH: " + rawPathStr);
                        System.out.println("PROJECT DIR REAL PATH: " + projectDir.toRealPath());
                        java.nio.file.Path gitleaksPath = java.nio.file.Path.of(rawPathStr).normalize();
                        java.nio.file.Path projPath = projectDir.toRealPath();
                        java.nio.file.Path gitleaksRealPath = gitleaksPath.toRealPath();
                        
                        String relativePathStr;
                        if (gitleaksRealPath.isAbsolute() && gitleaksRealPath.startsWith(projPath)) {
                            relativePathStr = projPath.relativize(gitleaksRealPath).toString();
                        } else {
                            relativePathStr = gitleaksPath.toString();
                        }
                        
                        relativePathStr = relativePathStr.replace("\\", "/");
                        finding.setFile(relativePathStr);
                        int startLine = node.path("StartLine").asInt(0);
                        int endLine = node.path("EndLine").asInt(startLine);
                        
                        // If the secret spans multiple lines, we create a finding for each line
                        // so that ALL lines containing the secret are masked by the LLM context builder.
                        for (int line = startLine; line <= endLine; line++) {
                            if (line == 0) continue;
                            ReviewFinding multiLineFinding = new ReviewFinding();
                            multiLineFinding.setSource(finding.getSource());
                            multiLineFinding.setRuleId(finding.getRuleId());
                            multiLineFinding.setTitle(finding.getTitle());
                            multiLineFinding.setDescription(finding.getDescription());
                            multiLineFinding.setSeverity(finding.getSeverity());
                            multiLineFinding.setCategory(finding.getCategory());
                            multiLineFinding.setFile(finding.getFile());
                            multiLineFinding.setLineNumber(line);
                            multiLineFinding.setColumnNumber(node.path("StartColumn").asInt(0));
                            multiLineFinding.setEvidence("*** MASKED SECRET ***");
                            findings.add(multiLineFinding);
                        }
                        } catch (Exception pathEx) {
                            System.err.println("Error processing path: " + pathEx.getMessage());
                            pathEx.printStackTrace();
                        }
                    }
                }
            }
            reportFile.delete();
        } catch (Exception e) {
            System.err.println("Gitleaks execution failed: " + e.getMessage());
        }
        return findings;
    }
}
