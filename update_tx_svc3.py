import os

path = 'backend/src/main/java/com/aicodereview/service/ProjectAnalysisTransactionService.java'
with open(path, 'r', encoding='utf-8') as f:
    text = f.read()

# Fix the counts issue
text = text.replace("countSeverity(f, new int[]{critical, high, medium, low});", """
                        if (f.getSeverity() != null) {
                            switch (f.getSeverity()) {
                                case CRITICAL -> critical++;
                                case HIGH -> high++;
                                case MEDIUM -> medium++;
                                case LOW, INFO -> low++;
                            }
                        }
""")
text = text.replace("countSeverity(f);", """
                        if (f.getSeverity() != null) {
                            switch (f.getSeverity()) {
                                case CRITICAL -> critical++;
                                case HIGH -> high++;
                                case MEDIUM -> medium++;
                                case LOW, INFO -> low++;
                            }
                        }
""")

# Also add deleteDirectory method if it's missing
delete_dir = """
    private void deleteDirectory(java.io.File directoryToBeDeleted) {
        java.io.File[] allContents = directoryToBeDeleted.listFiles();
        if (allContents != null) {
            for (java.io.File file : allContents) {
                deleteDirectory(file);
            }
        }
        directoryToBeDeleted.delete();
    }
"""
if "private void deleteDirectory" not in text:
    text = text.replace("public void markAsFailed(Long projectId) {", delete_dir + "\n    @Transactional\n    public void markAsFailed(Long projectId) {")
    
# Remove the old countSeverity definition
import re
text = re.sub(r'private void countSeverity\(ReviewFinding f, int\[\] counts\) \{.*?\n    \}', '', text, flags=re.DOTALL)

with open(path, 'w', encoding='utf-8') as f:
    f.write(text)
