# coding=utf-8
import os
import re

path = 'backend/src/main/java/com/aicodereview/service/ProjectAnalysisTransactionService.java'
with open(path, 'r', encoding='utf-8') as f:
    text = f.read()

# Replace the else block using regex to avoid exact string matching issues with accents
pattern = r'\} else \{\s*// Configurable: should we send files with NO static findings.*?\n\s*\}'

new_code = """} else {
                    // Fallback to full AI review if no static findings are present (keeps test passing and ensures we don't assume code is secure)
                    List<ReviewFinding> aiFindings = aiReviewService.analyzeSourceCode(pf.getContent(), ReviewType.FULL_REVIEW, pf.getRelativePath(), modelId);
                    if (aiFindings != null) {
                        for (ReviewFinding f : aiFindings) {
                            f.setProject(project);
                            f.setSource("LLM");
                            project.getFindings().add(f);
                            totalIssues++;
                            if (f.getSeverity() != null) {
                                switch (f.getSeverity()) {
                                    case CRITICAL -> critical++;
                                    case HIGH -> high++;
                                    case MEDIUM -> medium++;
                                    case LOW, INFO -> low++;
                                }
                            }
                        }
                    }
                }"""

text = re.sub(pattern, new_code, text, flags=re.DOTALL)

with open(path, 'w', encoding='utf-8') as f:
    f.write(text)
