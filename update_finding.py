import os

path = 'backend/src/main/java/com/aicodereview/entity/ReviewFinding.java'
with open(path, 'r', encoding='utf-8') as f:
    text = f.read()

new_fields = """    // New integration fields
    private String source; // BANDIT, RUFF, GITLEAKS, LLM
    private String ruleId;
    @Column(name = "column_num")
    private Integer columnNumber;
    private String confidence;
    private String cwe;
    private String owasp;
    
    @Column(columnDefinition = "TEXT")
    private String evidence;
    
    @Column(columnDefinition = "TEXT")
    private String explanation;
"""

if "private String source;" not in text:
    # Insert before the last brace
    text = text[:text.rfind('}')] + new_fields + "}\n"
    with open(path, 'w', encoding='utf-8') as f:
        f.write(text)
