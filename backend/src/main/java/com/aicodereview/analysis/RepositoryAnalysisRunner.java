package com.aicodereview.analysis;

import com.aicodereview.analysis.ArchitectureAnalyzer.Result;
import com.aicodereview.analysis.DiagramGenerator.Diagram;
import com.aicodereview.analysis.RepositoryScanner.ScanResult;
import com.aicodereview.analysis.TechnologyDetector.TechInfo;
import com.aicodereview.analysis.model.*;
import com.aicodereview.analysis.parser.CodeParser;
import com.aicodereview.entity.*;
import com.aicodereview.exception.ExternalServiceException;
import com.aicodereview.exception.ResourceNotFoundException;
import com.aicodereview.github.GithubApiClient;
import com.aicodereview.github.GithubConnectionService;
import com.aicodereview.repository.ProjectRepository;
import com.aicodereview.rules.RuleService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Asynchronous pipeline: fetch -> scan -> parse -> architecture graph -> diagrams -> static findings -> AI review -> report.
 * Each stage commits its own progress so the frontend can poll /status. A failure of one file never fails the whole run;
 * an AI failure keeps the static results (the review then completes with warnings).
 * Not @Transactional as a whole: it performs long network calls and must not hold a database transaction open.
 */
@Service
public class RepositoryAnalysisRunner {
    private static final Logger log = LoggerFactory.getLogger(RepositoryAnalysisRunner.class);
    private static final int MAX_PERSISTED_FILES = 300;

    private final ProjectRepository projects;
    private final GithubConnectionService githubConnection;
    private final GithubApiClient github;
    private final RepositoryScanner scanner;
    private final TechnologyDetector techDetector;
    private final List<CodeParser> parsers;
    private final ArchitectureAnalyzer architectureAnalyzer;
    private final DiagramGenerator diagramGenerator;
    private final StaticCodeAnalyzer staticAnalyzer;
    private final RepositoryAIReviewer aiReviewer;
    private final RuleService ruleService;
    private final AnalysisProperties props;
    private final TransactionTemplate tx;
    private final ObjectMapper mapper = new ObjectMapper();

    public RepositoryAnalysisRunner(ProjectRepository projects, GithubConnectionService githubConnection, GithubApiClient github,
                                    RepositoryScanner scanner, TechnologyDetector techDetector, List<CodeParser> parsers,
                                    ArchitectureAnalyzer architectureAnalyzer, DiagramGenerator diagramGenerator,
                                    StaticCodeAnalyzer staticAnalyzer, RepositoryAIReviewer aiReviewer, RuleService ruleService,
                                    AnalysisProperties props, PlatformTransactionManager txManager) {
        this.projects = projects;
        this.githubConnection = githubConnection;
        this.github = github;
        this.scanner = scanner;
        this.techDetector = techDetector;
        this.parsers = parsers;
        this.architectureAnalyzer = architectureAnalyzer;
        this.diagramGenerator = diagramGenerator;
        this.staticAnalyzer = staticAnalyzer;
        this.aiReviewer = aiReviewer;
        this.ruleService = ruleService;
        this.props = props;
        this.tx = new TransactionTemplate(txManager);
    }

    @Async
    public void runGithub(Long projectId, Long userId, String owner, String repo, String branch) {
        try {
            stage(projectId, AnalysisStage.CLONING, "Downloading " + owner + "/" + repo + "@" + branch);
            String token = githubConnection.requireToken(userId);
            String sha = github.resolveCommitSha(token, owner, repo, branch);
            byte[] zip = github.downloadZip(token, owner, repo, sha.isBlank() ? branch : sha, props.getMaxRepoBytes());
            tx.executeWithoutResult(s -> projects.findById(projectId).ifPresent(p -> { p.setCommitHash(sha); projects.save(p); }));
            stage(projectId, AnalysisStage.SCANNING, "Scanning files");
            ScanResult scan = scanner.scan(zip, true);
            if (scan.files().isEmpty()) throw new IllegalArgumentException("No analyzable source files were found in this repository");
            runPipeline(projectId, userId, scan.files(), scan.warnings());
        } catch (Exception e) {
            fail(projectId, e);
        }
    }

    @Async
    public void runStored(Long projectId, Long userId) {
        try {
            stage(projectId, AnalysisStage.SCANNING, "Reading imported files");
            List<ScannedFile> files = tx.execute(s -> {
                Project p = projects.findById(projectId).orElseThrow(() -> new ResourceNotFoundException("Project not found"));
                List<ScannedFile> out = new ArrayList<>();
                for (ProjectFile pf : p.getFiles()) {
                    if (pf.getContent() == null) continue;
                    String ext = pf.getExtension() == null ? "" : pf.getExtension();
                    out.add(new ScannedFile(pf.getRelativePath(), pf.getContent(), RepositoryScanner.languageOf(pf.getRelativePath()), ext,
                            pf.getFileSize() == null ? pf.getContent().length() : pf.getFileSize(), RepositoryScanner.countLines(pf.getContent())));
                }
                return out;
            });
            if (files == null || files.isEmpty()) throw new IllegalArgumentException("This project has no analyzable files");
            runPipeline(projectId, userId, files, new ArrayList<>());
        } catch (Exception e) {
            fail(projectId, e);
        }
    }

    // ------------------------------------------------------------------ pipeline

    void runPipeline(Long projectId, Long userId, List<ScannedFile> files, List<String> warnings) throws Exception {
        String repoName = tx.execute(s -> projects.findById(projectId).map(Project::getName).orElse("repository"));

        stage(projectId, AnalysisStage.PARSING, "Parsing " + files.size() + " files");
        TechInfo tech = techDetector.detect(files);
        List<ParsedFile> parsed = new ArrayList<>();
        int failedParses = 0;
        for (ScannedFile f : files) {
            for (CodeParser parser : parsers) {
                if (!parser.supports(f.path())) continue;
                try {
                    parsed.add(parser.parse(f.path(), f.content()));
                } catch (Exception e) {            // one bad file never fails the analysis
                    failedParses++;
                    log.debug("Parser failure on {}: {}", f.path(), e.getMessage());
                }
                break;
            }
        }
        if (failedParses > 0) warnings.add(failedParses + " file(s) could not be parsed and were skipped by the structural analysis");

        stage(projectId, AnalysisStage.ARCHITECTURE, "Building architecture graph");
        Result arch = architectureAnalyzer.analyze(parsed, tech);
        List<ReviewFinding> staticFindings = new ArrayList<>(arch.findings());
        staticFindings.addAll(staticAnalyzer.analyze(files, parsed, arch.graph()));

        stage(projectId, AnalysisStage.GENERATING_DIAGRAM, "Generating diagrams");
        List<Diagram> diagrams = diagramGenerator.generate(arch.graph(), repoName);
        String archJson = mapper.writeValueAsString(arch.graph());
        String diagramsJson = mapper.writeValueAsString(Map.of("diagrams", diagrams));

        stage(projectId, AnalysisStage.ANALYZING, "AI review in progress");
        List<String> rules = ruleService.enabledRuleTexts(userId);
        String modelId = null;
        RepositoryAIReviewer.Result ai;
        try {
            ai = aiReviewer.review(new RepositoryAIReviewer.Context(repoName, tech, arch.graph(), files, staticFindings, rules, modelId));
        } catch (Exception e) {
            log.warn("AI review failed: {}", e.getMessage());
            ai = new RepositoryAIReviewer.Result("", List.of(), List.of("AI review failed: " + e.getMessage()), false);
        }
        warnings.addAll(ai.warnings());

        List<ReviewFinding> all = new ArrayList<>(staticFindings);
        all.addAll(ai.findings());
        persist(projectId, files, tech, arch.graph(), all, ai.summary(), archJson, diagramsJson, warnings);
    }

    private void persist(Long projectId, List<ScannedFile> files, TechInfo tech, ArchitectureGraph graph, List<ReviewFinding> all,
                         String aiSummary, String archJson, String diagramsJson, List<String> warnings) throws Exception {
        String warningsJson = warnings.isEmpty() ? null : mapper.writeValueAsString(warnings);
        tx.executeWithoutResult(status -> {
            Project p = projects.findById(projectId).orElseThrow();
            p.getFindings().clear();
            // keep the content of files that have findings (code viewer), unless the project already stores its files (ZIP)
            Set<String> storedPaths = new HashSet<>();
            for (ProjectFile pf : p.getFiles()) storedPaths.add(pf.getRelativePath());
            Set<String> withFindings = new LinkedHashSet<>();
            for (ReviewFinding f : all) if (f.getFile() != null && f.getLineNumber() != null) withFindings.add(f.getFile());
            Map<String, ScannedFile> byPath = new HashMap<>();
            for (ScannedFile f : files) byPath.put(f.path(), f);
            int persisted = 0;
            for (String path : withFindings) {
                if (storedPaths.contains(path) || persisted >= MAX_PERSISTED_FILES || !byPath.containsKey(path)) continue;
                ScannedFile sf = byPath.get(path);
                p.getFiles().add(ProjectFile.builder().project(p).relativePath(sf.path()).fileName(sf.fileName()).language(sf.language())
                        .extension(sf.extension()).fileSize(sf.size()).lineCount(sf.lines()).analysisStatus("ANALYZED").content(sf.content()).build());
                persisted++;
            }
            int critical = 0, high = 0, medium = 0, low = 0;
            for (ReviewFinding f : all) {
                f.setProject(p);
                p.getFindings().add(f);
                switch (f.getSeverity()) {
                    case CRITICAL -> critical++;
                    case HIGH -> high++;
                    case MEDIUM -> medium++;
                    default -> low++;
                }
            }
            int score = 100 - Math.min(100, critical * 20 + high * 8 + medium * 3 + low);
            p.setQualityScore(score >= 90 ? "A" : score >= 80 ? "B" : score >= 70 ? "C" : score >= 60 ? "D" : "F");
            p.setDetectedLanguages(String.join(", ", tech.languageLines().keySet()));
            p.setTechnologies(String.join("|", tech.technologies()));
            p.setArchitectureJson(archJson);
            p.setDiagramsJson(diagramsJson);
            p.setSummaryText(buildSummary(p.getName(), tech, graph, all, critical, high, medium, low, aiSummary));
            p.setErrorMessage(warningsJson);
            p.setStatus(ProjectStatus.COMPLETED);
            p.setStage(AnalysisStage.COMPLETED);
            p.setStageMessage("Analysis completed");
            p.setCompletedAt(LocalDateTime.now());
            p.setUpdatedAt(LocalDateTime.now());
            projects.save(p);
        });
    }

    static String buildSummary(String name, TechInfo tech, ArchitectureGraph graph, List<ReviewFinding> all,
                               int critical, int high, int medium, int low, String aiSummary) {
        StringBuilder sb = new StringBuilder();
        sb.append(name).append(": ").append(graph.nodes().size()).append(" types analysed");
        if (!tech.technologies().isEmpty()) sb.append(" (").append(String.join(", ", tech.technologies().stream().limit(8).toList())).append(")");
        sb.append(". ").append(String.join(" ", graph.styles())).append(". ");
        sb.append(all.size()).append(" finding(s): ").append(critical).append(" critical, ").append(high).append(" high, ")
                .append(medium).append(" medium, ").append(low).append(" low/info.");
        if (aiSummary != null && !aiSummary.isBlank()) sb.append("\n\n").append(aiSummary.trim());
        return sb.toString();
    }

    private void stage(Long projectId, AnalysisStage stage, String message) {
        tx.executeWithoutResult(s -> projects.findById(projectId).ifPresent(p -> {
            p.setStage(stage);
            p.setStageMessage(message);
            p.setStatus(ProjectStatus.ANALYZING);
            p.setUpdatedAt(LocalDateTime.now());
            projects.save(p);
        }));
    }

    private void fail(Long projectId, Exception e) {
        String message = e instanceof IllegalArgumentException || e instanceof ExternalServiceException || e instanceof ResourceNotFoundException
                ? e.getMessage() : "Analysis failed unexpectedly";
        log.error("Analysis {} failed: {}", projectId, e.toString());
        try {
            tx.executeWithoutResult(s -> projects.findById(projectId).ifPresent(p -> {
                p.setStatus(ProjectStatus.FAILED);
                p.setStage(AnalysisStage.FAILED);
                p.setStageMessage("Analysis failed");
                p.setErrorMessage(message);
                p.setUpdatedAt(LocalDateTime.now());
                projects.save(p);
            }));
        } catch (Exception inner) {
            log.error("Could not record the failure of analysis {}", projectId);
        }
    }
}
