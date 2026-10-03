import os

path = 'backend/src/main/java/com/aicodereview/service/ProjectAnalysisTransactionService.java'
with open(path, 'r', encoding='utf-8') as f:
    text = f.read()

helper = """
    private void countSeverity(ReviewFinding f, int[] counts) {
        if (f.getSeverity() == null) return;
        switch (f.getSeverity()) {
            case CRITICAL -> counts[0]++;
            case HIGH -> counts[1]++;
            case MEDIUM -> counts[2]++;
            case LOW, INFO -> counts[3]++;
        }
    }
"""

if "private void countSeverity" not in text:
    text = text.replace("public void markAsFailed(Long projectId) {", helper + "\n    public void markAsFailed(Long projectId) {")
    
text = text.replace("countSeverity(f);", "countSeverity(f, new int[]{critical, high, medium, low});")
# Ah, Java arrays are pass by value for the reference, but modifying elements works. However, the primitive variables `critical`, `high`, etc., won't be updated!
# Let me just inline the switch statement instead of using a helper.

with open(path, 'w', encoding='utf-8') as f:
    f.write(text)
