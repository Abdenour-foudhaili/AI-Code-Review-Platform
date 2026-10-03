import os

path = 'backend/src/test/java/com/aicodereview/service/ProjectServiceTest.java'
with open(path, 'r', encoding='utf-8') as f:
    text = f.read()

# Replace constructor calls with an empty list
text = text.replace('new ProjectAnalysisTransactionService(repository, aiReviewService)', 'new ProjectAnalysisTransactionService(repository, aiReviewService, java.util.Collections.emptyList())')

with open(path, 'w', encoding='utf-8') as f:
    f.write(text)
