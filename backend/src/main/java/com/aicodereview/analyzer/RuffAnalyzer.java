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
public class RuffAnalyzer implements StaticAnalyzer {

    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public String getName() {
        return "RUFF";
    }

    @Override
    public boolean isAvailable() {
        try {
            Process process = new ProcessBuilder("ruff", "--version").start();
            return process.waitFor(5, TimeUnit.SECONDS) && process.exitValue() == 0;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public List<ReviewFinding> analyze(Path projectDir) {
        List<ReviewFinding> findings = new ArrayList<>();
        try {
            File reportFile = File.createTempFile("ruff-report", ".json");
            ProcessBuilder pb = new ProcessBuilder("ruff", "check", projectDir.toString(), "--output-format=json", "--output-file=" + reportFile.getAbsolutePath());
            pb.directory(projectDir.toFile());
            pb.redirectError(ProcessBuilder.Redirect.DISCARD);
            Process process = pb.start();
            
            process.waitFor(60, TimeUnit.SECONDS);

            if (reportFile.exists() && reportFile.length() > 0) {
                JsonNode results = mapper.readTree(reportFile);
                if (results.isArray()) {
                    for (JsonNode node : results) {
                        ReviewFinding finding = new ReviewFinding();
                        finding.setSource("RUFF");
                        finding.setRuleId(node.path("code").asText(""));
                        finding.setTitle(node.path("code").asText("") + " Code Quality Issue");
                        finding.setDescription(node.path("message").asText(""));
                        finding.setSeverity(FindingSeverity.LOW); // Ruff is mostly lint/style
                        finding.setCategory(FindingCategory.CODE_SMELL);
                        
                        String filePath = node.path("filename").asText("");
                        if (filePath.startsWith(projectDir.toString())) {
                            filePath = filePath.substring(projectDir.toString().length());
                            if (filePath.startsWith("/") || filePath.startsWith("\\")) {
                                filePath = filePath.substring(1);
                            }
                        }
                        filePath = filePath.replace("\\", "/");
                        
                        finding.setFile(filePath);
                        JsonNode loc = node.path("location");
                        if (!loc.isMissingNode()) {
                            finding.setLineNumber(loc.path("row").asInt(0));
                            finding.setColumnNumber(loc.path("column").asInt(0));
                        }
                        
                        findings.add(finding);
                    }
                }
            }
            reportFile.delete();
        } catch (Exception e) {
            System.err.println("Ruff execution failed: " + e.getMessage());
        }
        return findings;
    }
}
