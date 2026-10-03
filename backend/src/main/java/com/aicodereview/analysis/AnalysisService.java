package com.aicodereview.analysis;

import com.aicodereview.analysis.AnalysisDtos.*;
import com.aicodereview.entity.*;
import com.aicodereview.exception.ResourceNotFoundException;
import com.aicodereview.github.GithubApiClient;
import com.aicodereview.github.GithubConnectionService;
import com.aicodereview.github.GithubDtos.GithubRepo;
import com.aicodereview.repository.ProjectFileRepository;
import com.aicodereview.repository.ProjectRepository;
import com.aicodereview.repository.ReviewFindingRepository;
import com.aicodereview.repository.UserRepository;
import com.aicodereview.service.OwnershipService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/** Synchronous (fast) part of the analysis API: start, status, history, findings, architecture, diagrams, code viewer. */
@Service
public class AnalysisService {
    private static final List<AnalysisStage> ORDER = List.of(AnalysisStage.CLONING, AnalysisStage.SCANNING, AnalysisStage.PARSING,
            AnalysisStage.ARCHITECTURE, AnalysisStage.GENERATING_DIAGRAM, AnalysisStage.ANALYZING, AnalysisStage.COMPLETED);
    private static final Map<AnalysisStage, String> LABELS = Map.of(
            AnalysisStage.CLONING, "Fetching repository", AnalysisStage.SCANNING, "Scanning files",
            AnalysisStage.PARSING, "Parsing code", AnalysisStage.ARCHITECTURE, "Architecture analysis",
            AnalysisStage.GENERATING_DIAGRAM, "Generating diagrams", AnalysisStage.ANALYZING, "AI code review",
            AnalysisStage.COMPLETED, "Final report");

    private final ProjectRepository projects;
    private final ProjectFileRepository files;
    private final ReviewFindingRepository findings;
    private final UserRepository users;
    private final OwnershipService ownership;
    private final GithubConnectionService githubConnection;
    private final GithubApiClient github;
    private final RepositoryAnalysisRunner runner;
    private final ObjectMapper mapper = new ObjectMapper();

    public AnalysisService(ProjectRepository projects, ProjectFileRepository files, ReviewFindingRepository findings,
                           UserRepository users, OwnershipService ownership, GithubConnectionService githubConnection,
                           GithubApiClient github, RepositoryAnalysisRunner runner) {
        this.projects = projects;
        this.files = files;
        this.findings = findings;
        this.users = users;
        this.ownership = ownership;
        this.githubConnection = githubConnection;
        this.github = github;
        this.runner = runner;
    }

    /** Creates the analysis row and returns immediately; the heavy work runs asynchronously. */
    public AnalysisSummary startGithubAnalysis(Long userId, StartAnalysisRequest req) {
        GithubApiClient.validateRepoName(req.getOwner(), req.getRepo());
        String token = githubConnection.requireToken(userId);
        GithubRepo repo = github.getRepository(token, req.getOwner(), req.getRepo());       // 404 / 401 / 403 mapped here
        String branch = req.getBranch() != null && !req.getBranch().isBlank() ? req.getBranch().trim() : repo.defaultBranch();

        Project project = createRow(userId, repo.fullName(), "GITHUB", repo.htmlUrl(), repo.fullName(), branch);
        runner.runGithub(project.getId(), userId, repo.owner(), repo.name(), branch);
        return toSummary(projects.findById(project.getId()).orElse(project), 0);
    }

    /** Deep analysis of an already imported ZIP project. */
    public AnalysisSummary startZipAnalysis(Long userId, Long projectId) {
        Project p = ownership.requireProject(projectId, userId);
        if (p.getStatus() == ProjectStatus.ANALYZING && p.getUpdatedAt() != null && p.getUpdatedAt().isAfter(LocalDateTime.now().minusMinutes(15))) {
            throw new IllegalArgumentException("An analysis is already running for this project");
        }
        markStarted(p.getId());
        runner.runStored(p.getId(), userId);
        return toSummary(projects.findById(p.getId()).orElse(p), 0);
    }

    @Transactional
    Project createRow(Long userId, String name, String sourceType, String url, String fullName, String branch) {
        Project p = Project.builder()
                .name(name).description("Repository analysis").status(ProjectStatus.ANALYZING)
                .owner(users.getReferenceById(userId)).sourceType(sourceType).repositoryUrl(url)
                .repositoryFullName(fullName).branch(branch).stage(AnalysisStage.PENDING).stageMessage("Waiting to start")
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();
        return projects.save(p);
    }

    @Transactional
    void markStarted(Long id) {
        Project p = projects.findById(id).orElseThrow();
        p.setStatus(ProjectStatus.ANALYZING);
        p.setStage(AnalysisStage.PENDING);
        p.setStageMessage("Waiting to start");
        p.setErrorMessage(null);
        p.setUpdatedAt(LocalDateTime.now());
        projects.save(p);
    }

    // ------------------------------------------------------------------ reads

    @Transactional(readOnly = true)
    public List<AnalysisSummary> history(Long userId) {
        return projects.findByOwnerIdOrderByCreatedAtDesc(userId).stream()
                .filter(p -> p.getStage() != null || "GITHUB".equals(p.getSourceType()))
                .map(p -> toSummary(p, projects.countFindings(p.getId()))).toList();
    }

    @Transactional(readOnly = true)
    public AnalysisDetails details(Long userId, Long id) {
        Project p = ownership.requireProject(id, userId);
        List<ReviewFinding> list = findings.findByProjectIdOrderBySeverityAscFileAscLineNumberAsc(id);
        Map<String, Long> bySeverity = count(list, f -> f.getSeverity() == null ? "INFO" : f.getSeverity().name());
        Map<String, Long> byCategory = count(list, f -> f.getCategory() == null ? "BEST_PRACTICE" : f.getCategory().name());
        Map<String, Long> bySource = count(list, f -> f.getSource() == null ? "LLM" : f.getSource());
        List<String> tech = p.getTechnologies() == null || p.getTechnologies().isBlank() ? List.of()
                : Arrays.stream(p.getTechnologies().split("\\|")).filter(s -> !s.isBlank()).toList();
        List<String> warnings = readList(p.getErrorMessage() != null && p.getStatus() != ProjectStatus.FAILED ? p.getErrorMessage() : null);
        return new AnalysisDetails(toSummary(p, list.size()), p.getSummaryText(), tech, warnings, bySeverity, byCategory, bySource);
    }

    @Transactional(readOnly = true)
    public AnalysisStatus status(Long userId, Long id) {
        Project p = ownership.requireProject(id, userId);
        AnalysisStage stage = p.getStage() == null ? (p.getStatus() == ProjectStatus.COMPLETED ? AnalysisStage.COMPLETED : AnalysisStage.PENDING) : p.getStage();
        boolean failed = stage == AnalysisStage.FAILED;
        int idx = ORDER.indexOf(stage);
        List<StageInfo> stages = new ArrayList<>();
        for (int i = 0; i < ORDER.size(); i++) {
            AnalysisStage s = ORDER.get(i);
            String state;
            if (failed) state = "pending";
            else if (stage == AnalysisStage.COMPLETED) state = "done";
            else if (idx < 0) state = "pending";
            else state = i < idx ? "done" : i == idx ? "active" : "pending";
            stages.add(new StageInfo(s.name(), LABELS.get(s), state));
        }
        int progress = failed ? 0 : stage == AnalysisStage.COMPLETED ? 100 : idx < 0 ? 0 : (int) Math.round(idx * 100.0 / (ORDER.size() - 1));
        String error = failed ? p.getErrorMessage() : null;
        return new AnalysisStatus(p.getId(), p.getStatus().name(), stage.name(), p.getStageMessage(), progress, stages, error);
    }

    @Transactional(readOnly = true)
    public List<FindingDto> findings(Long userId, Long id, String category, String severity, String file, String source) {
        ownership.requireProjectAccess(id, userId);
        return findings.findByProjectIdOrderBySeverityAscFileAscLineNumberAsc(id).stream()
                .filter(f -> category == null || category.isBlank() || (f.getCategory() != null && f.getCategory().name().equalsIgnoreCase(category)))
                .filter(f -> severity == null || severity.isBlank() || (f.getSeverity() != null && f.getSeverity().name().equalsIgnoreCase(severity)))
                .filter(f -> file == null || file.isBlank() || (f.getFile() != null && f.getFile().toLowerCase().contains(file.toLowerCase())))
                .filter(f -> source == null || source.isBlank() || source.equalsIgnoreCase(f.getSource()))
                .map(AnalysisService::toDto).toList();
    }

    @Transactional(readOnly = true)
    public String architecture(Long userId, Long id) {
        Project p = ownership.requireProject(id, userId);
        if (p.getArchitectureJson() == null) throw new ResourceNotFoundException("Architecture is not available yet");
        return p.getArchitectureJson();
    }

    @Transactional(readOnly = true)
    public String diagrams(Long userId, Long id) {
        Project p = ownership.requireProject(id, userId);
        if (p.getDiagramsJson() == null) throw new ResourceNotFoundException("Diagrams are not available yet");
        return p.getDiagramsJson();
    }

    /** Code viewer: lines around a finding. Only files persisted with the analysis (files that have findings) are available. */
    @Transactional(readOnly = true)
    public CodeContext codeContext(Long userId, Long id, String path, int line, int radius) {
        ownership.requireProjectAccess(id, userId);
        ProjectFile pf = files.findByProjectId(id).stream().filter(f -> f.getRelativePath().equals(path)).findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Source file not available: " + path));
        String[] all = (pf.getContent() == null ? "" : pf.getContent()).split("\n", -1);
        int r = Math.min(Math.max(radius, 3), 60);
        int target = Math.min(Math.max(line, 1), Math.max(all.length, 1));
        int start = Math.max(1, target - r);
        int end = Math.min(all.length, target + r);
        List<String> out = new ArrayList<>();
        for (int i = start; i <= end; i++) out.add(all[i - 1]);
        return new CodeContext(path, target, start, pf.getLanguage(), out);
    }

    @Transactional
    public void delete(Long userId, Long id) {
        Project p = ownership.requireProject(id, userId);
        projects.delete(p);
    }

    // ------------------------------------------------------------------ mapping

    static AnalysisSummary toSummary(Project p, long findingsCount) {
        return new AnalysisSummary(p.getId(), p.getName(), p.getSourceType() == null ? "ZIP" : p.getSourceType(), p.getRepositoryUrl(),
                p.getBranch(), p.getCommitHash(), p.getStatus() == null ? null : p.getStatus().name(),
                p.getStage() == null ? null : p.getStage().name(), p.getQualityScore(), p.getCreatedAt(), p.getCompletedAt(), findingsCount);
    }

    static FindingDto toDto(ReviewFinding f) {
        return new FindingDto(f.getId(), f.getCategory() == null ? null : f.getCategory().name(),
                f.getSeverity() == null ? null : f.getSeverity().name(), f.getTitle(), f.getDescription(), f.getFile(),
                f.getLineNumber(), f.getRecommendation(), f.getFixedCode(), f.getSource(), f.getRuleId(), f.getEvidence());
    }

    private static Map<String, Long> count(List<ReviewFinding> list, java.util.function.Function<ReviewFinding, String> key) {
        return list.stream().collect(Collectors.groupingBy(key, TreeMap::new, Collectors.counting()));
    }

    private List<String> readList(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            return mapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return List.of();
        }
    }
}
