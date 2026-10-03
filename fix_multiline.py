# coding=utf-8
import os
import re

path = 'backend/src/main/java/com/aicodereview/analyzer/GitleaksAnalyzer.java'
with open(path, 'r', encoding='utf-8') as f:
    text = f.read()

target = """                        finding.setLineNumber(node.path("StartLine").asInt(0));
                        finding.setColumnNumber(node.path("StartColumn").asInt(0));
                        
                        // Mask evidence
                        finding.setEvidence("*** MASKED SECRET ***");
                        
                        findings.add(finding);"""

replacement = """                        int startLine = node.path("StartLine").asInt(0);
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
                        }"""

text = text.replace(target, replacement)

with open(path, 'w', encoding='utf-8') as f:
    f.write(text)
