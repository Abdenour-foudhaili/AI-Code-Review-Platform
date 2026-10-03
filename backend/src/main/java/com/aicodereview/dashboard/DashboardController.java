package com.aicodereview.dashboard;

import com.aicodereview.entity.Project;
import com.aicodereview.entity.ProjectStatus;
import com.aicodereview.github.GithubConnectionService;
import com.aicodereview.github.GithubDtos.GithubStatus;
import com.aicodereview.repository.ProjectRepository;
import com.aicodereview.security.AuthenticatedUser;
import com.aicodereview.service.OwnershipService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.*;

/** Real, per-user dashboard data: counts come from the database, never from static values. */
@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    public record RecentAnalysis(Long id, String name, String sourceType, String status, String stage, String qualityScore, LocalDateTime createdAt) {}

    public record LastAnalysis(Long id, String name, String summary, String qualityScore, LocalDateTime completedAt) {}

    public record DashboardResponse(long repositories, long analyses, long completedAnalyses, long singleFileReviews, long criticalFindings,
                                    long securityFindings, List<RecentAnalysis> recentAnalyses, LastAnalysis lastAnalysis,
                                    Map<String, Integer> architectureRoles, List<String> architectureStyles, List<String> technologies,
                                    GithubStatus github) {}

    private final ProjectRepository projects;
    private final OwnershipService ownership;
    private final GithubConnectionService github;
    private final ObjectMapper mapper = new ObjectMapper();

    public DashboardController(ProjectRepository projects, OwnershipService ownership, GithubConnectionService github) {
        this.projects = projects;
        this.ownership = ownership;
        this.github = github;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public DashboardResponse get(@AuthenticationPrincipal AuthenticatedUser user) {
        List<Project> mine = ownership.listProjects(user.id());
        long repos = mine.stream().filter(p -> "GITHUB".equals(p.getSourceType())).map(Project::getRepositoryFullName).distinct().count();
        long completed = mine.stream().filter(p -> p.getStatus() == ProjectStatus.COMPLETED).count();
        List<RecentAnalysis> recent = mine.stream().limit(6)
                .map(p -> new RecentAnalysis(p.getId(), p.getName(), p.getSourceType() == null ? "ZIP" : p.getSourceType(),
                        p.getStatus() == null ? null : p.getStatus().name(), p.getStage() == null ? null : p.getStage().name(),
                        p.getQualityScore(), p.getCreatedAt())).toList();
        Optional<Project> last = mine.stream().filter(p -> p.getStatus() == ProjectStatus.COMPLETED && p.getStage() != null)
                .max(Comparator.comparing(p -> p.getCompletedAt() == null ? LocalDateTime.MIN : p.getCompletedAt()));
        Map<String, Integer> roles = new LinkedHashMap<>();
        List<String> styles = new ArrayList<>();
        List<String> tech = new ArrayList<>();
        LastAnalysis lastDto = null;
        if (last.isPresent()) {
            Project p = last.get();
            String summary = p.getSummaryText() == null ? "" : p.getSummaryText();
            lastDto = new LastAnalysis(p.getId(), p.getName(), summary.length() > 400 ? summary.substring(0, 400) + "…" : summary,
                    p.getQualityScore(), p.getCompletedAt());
            try {
                if (p.getArchitectureJson() != null) {
                    JsonNode g = mapper.readTree(p.getArchitectureJson());
                    g.path("roleCounts").fields().forEachRemaining(e -> roles.put(e.getKey(), e.getValue().asInt()));
                    g.path("styles").forEach(n -> styles.add(n.asText()));
                }
            } catch (Exception ignored) { /* dashboard still works without the overview */ }
            if (p.getTechnologies() != null) for (String t : p.getTechnologies().split("\\|")) if (!t.isBlank()) tech.add(t);
        }
        return new DashboardResponse(repos, mine.size(), completed, ownership.listReviews(user.id()).size(),
                projects.countCriticalFindings(user.id()), projects.countSecurityFindings(user.id()), recent, lastDto,
                roles, styles, tech, github.status(user.id()));
    }
}
