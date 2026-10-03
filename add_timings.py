# coding=utf-8
import os
import re

path = 'backend/src/main/java/com/aicodereview/service/ProjectAnalysisTransactionService.java'
with open(path, 'r', encoding='utf-8') as f:
    text = f.read()

# Add timings.
# We will inject variables and System.currentTimeMillis() around blocks.

text = text.replace('public void analyzeProjectInTransaction(Long projectId, String modelId) {', 
'''public void analyzeProjectInTransaction(Long projectId, String modelId) {
        long tTotalStart = System.currentTimeMillis();
        long tExtract = 0;
        long tBandit = 0;
        long tRuff = 0;
        long tGitleaks = 0;
        long tDedup = 0;
        long tContext = 0;
        long tLlm = 0;
        long tPersist = 0;
''')

# Extraction
text = text.replace('Path tempDir = Files.createTempDirectory("project_analyze_");',
'''long tExtStart = System.currentTimeMillis();
            Path tempDir = Files.createTempDirectory("project_analyze_");''')
text = text.replace('// Run static analyzers',
'''tExtract = System.currentTimeMillis() - tExtStart;
            // Run static analyzers''')

# Analyzers loop
# We can't easily measure each analyzer from the loop without changing it, but we can do it inside the loop.
loop_replace = '''long tAnalyzerStart = System.currentTimeMillis();
                    List<ReviewFinding> toolFindings = analyzer.analyze(tempDir);
                    long tAnalyzerDuration = System.currentTimeMillis() - tAnalyzerStart;
                    if (analyzer.getName().equals("BANDIT")) tBandit = tAnalyzerDuration;
                    if (analyzer.getName().equals("RUFF")) tRuff = tAnalyzerDuration;
                    if (analyzer.getName().equals("GITLEAKS")) tGitleaks = tAnalyzerDuration;'''
text = text.replace('List<ReviewFinding> toolFindings = analyzer.analyze(tempDir);', loop_replace)

# Deduplication
text = text.replace('// Deduplicate findings based on file, line, category, and source',
'''long tDedupStart = System.currentTimeMillis();
            // Deduplicate findings based on file, line, category, and source''')
text = text.replace('// Prepare context for LLM grouped by file',
'''tDedup = System.currentTimeMillis() - tDedupStart;
            // Prepare context for LLM grouped by file''')

# Context & LLM
text = text.replace('for (ProjectFile pf : project.getFiles()) {',
'''long tLLMLoopStart = System.currentTimeMillis();
            for (ProjectFile pf : project.getFiles()) {''')

text = text.replace('StringBuilder context = new StringBuilder();',
'''long tCtxStart = System.currentTimeMillis();
                    StringBuilder context = new StringBuilder();''')

text = text.replace('List<ReviewFinding> enriched = aiReviewService.analyzeSourceCode(context.toString(), ReviewType.FULL_REVIEW, pf.getRelativePath(), modelId);',
'''tContext += (System.currentTimeMillis() - tCtxStart);
                    long tLlmStart = System.currentTimeMillis();
                    List<ReviewFinding> enriched = aiReviewService.analyzeSourceCode(context.toString(), ReviewType.FULL_REVIEW, pf.getRelativePath(), modelId);
                    tLlm += (System.currentTimeMillis() - tLlmStart);''')

# Fallback LLM
text = text.replace('List<ReviewFinding> aiFindings = aiReviewService.analyzeSourceCode(pf.getContent(), ReviewType.FULL_REVIEW, pf.getRelativePath(), modelId);',
'''long tLlmStart2 = System.currentTimeMillis();
                    List<ReviewFinding> aiFindings = aiReviewService.analyzeSourceCode(pf.getContent(), ReviewType.FULL_REVIEW, pf.getRelativePath(), modelId);
                    tLlm += (System.currentTimeMillis() - tLlmStart2);''')

# Persist and Finish
text = text.replace('projectRepository.save(project);',
'''long tPersistStart = System.currentTimeMillis();
            projectRepository.save(project);
            tPersist = System.currentTimeMillis() - tPersistStart;
            
            long tTotal = System.currentTimeMillis() - tTotalStart;
            System.out.println("=== PERFORMANCE METRICS ===");
            System.out.println("Extraction: " + tExtract + " ms");
            System.out.println("Bandit: " + tBandit + " ms");
            System.out.println("Ruff: " + tRuff + " ms");
            System.out.println("Gitleaks: " + tGitleaks + " ms");
            System.out.println("Deduplication: " + tDedup + " ms");
            System.out.println("Context generation: " + tContext + " ms");
            System.out.println("LLM Calls: " + tLlm + " ms");
            System.out.println("Persistence: " + tPersist + " ms");
            System.out.println("Total Time: " + tTotal + " ms");
            System.out.println("===========================");''')

with open(path, 'w', encoding='utf-8') as f:
    f.write(text)
