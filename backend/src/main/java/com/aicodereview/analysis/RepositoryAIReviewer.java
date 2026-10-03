package com.aicodereview.analysis;

import com.aicodereview.ai.LLMClient;
import com.aicodereview.ai.LLMProperties;
import com.aicodereview.analysis.model.*;
import com.aicodereview.entity.FindingSeverity;
import com.aicodereview.entity.ReviewFinding;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * AI part of the analysis. Strategy (never "the whole repo in one prompt"):
 *  1. a compact repository overview built from the STATIC analysis (technologies, layers, endpoints, hot spots);
 *  2. one call for the global architecture review + summary;
 *  3. targeted calls on the riskiest files only (ranked by static findings, complexity, role), each with the overview,
 *     the file's direct dependencies, its static findings (to avoid duplicates) and the custom rules.
 * Secrets flagged by static analysis are masked before anything leaves the server. A failing call degrades gracefully.
 */
@Component
public class RepositoryAIReviewer {
    private static final Logger log = LoggerFactory.getLogger(RepositoryAIReviewer.class);

    public record Context(String repoName, TechnologyDetector.TechInfo tech, ArchitectureGraph graph, List<ScannedFile> files,
                          List<ReviewFinding> staticFindings, List<String> customRules, String modelId) {}

    public record Result(String summary, List<ReviewFinding> findings, List<String> warnings, boolean skipped) {}

    private final LLMClient llm;
    private final LLMProperties llmProps;
    private final AnalysisProperties props;

    public RepositoryAIReviewer(LLMClient llm, LLMProperties llmProps, AnalysisProperties props) {
        this.llm = llm;
        this.llmProps = llmProps;
        this.props = props;
    }

    public Result review(Context ctx) {
        List<String> warnings = new ArrayList<>();
        if (llmProps.isMock()) {
            return new Result("", List.of(), List.of("AI review skipped: LLM_PROVIDER is 'mock'. Set LLM_PROVIDER=gemini and LLM_API_KEY for the AI review."), true);
        }
        if (llmProps.getApiKey() == null || llmProps.getApiKey().isBlank()) {
            return new Result("", List.of(), List.of("AI review skipped: LLM_API_KEY is not configured."), true);
        }

        List<ReviewFinding> findings = new ArrayList<>();
        String summary = "";
        String overview = buildOverview(ctx);
        String rules = rulesBlock(ctx.customRules());

        // ---- 1. global architecture review
        try {
            String content = llm.completeJson(globalPrompt(overview, rules), ctx.modelId());
            JsonNode root = llm.readJsonLenient(content);
            if (root != null) {
                summary = root.path("summary").asText("");
                findings.addAll(llm.parseFindings(content, "(repository)", 0));
            } else {
                warnings.add("The AI returned an invalid JSON for the global review.");
            }
        } catch (LLMClient.LLMException e) {
            warnings.add("AI global review failed: " + e.getMessage());
            if (e.isRateLimited()) return new Result(summary, findings, add(warnings, "AI rate limit reached; remaining AI steps were skipped."), false);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return new Result("", List.of(), List.of("AI review skipped: " + e.getMessage()), true);
        }

        // ---- 2. targeted file reviews
        Map<String, List<ReviewFinding>> staticByFile = ctx.staticFindings().stream()
                .filter(f -> f.getFile() != null).collect(Collectors.groupingBy(ReviewFinding::getFile));
        Map<String, GraphNode> nodeByFile = new HashMap<>();
        for (GraphNode n : ctx.graph().nodes()) nodeByFile.putIfAbsent(n.file(), n);
        List<ScannedFile> selected = selectFiles(ctx.files(), staticByFile, nodeByFile);

        int done = 0;
        for (ScannedFile f : selected) {
            try {
                String prompt = filePrompt(f, overview, rules, staticByFile.getOrDefault(f.path(), List.of()), ctx.graph(), nodeByFile.get(f.path()));
                String content = llm.completeJson(prompt, ctx.modelId());
                List<ReviewFinding> parsed = llm.parseFindings(content, f.path(), 0);
                int maxLine = Math.max(1, f.lines());
                for (ReviewFinding pf : parsed) {
                    pf.setFile(f.path());
                    if (pf.getLineNumber() == null || pf.getLineNumber() < 1 || pf.getLineNumber() > maxLine) pf.setLineNumber(1);
                }
                findings.addAll(parsed);
                done++;
            } catch (LLMClient.LLMException e) {
                warnings.add("AI review failed for " + f.path() + ": " + e.getMessage());
                if (e.isRateLimited()) { warnings.add("AI rate limit reached; remaining files were skipped."); break; }
            } catch (RuntimeException e) {
                log.warn("AI review error for {}: {}", f.path(), e.getMessage());
                warnings.add("AI review error for " + f.path());
            }
        }
        log.info("AI review finished: {} file(s) reviewed, {} AI finding(s)", done, findings.size());
        return new Result(summary, dedupe(findings, ctx.staticFindings()), warnings, false);
    }

    // ------------------------------------------------------------------ context building

    String buildOverview(Context ctx) {
        ArchitectureGraph g = ctx.graph();
        StringBuilder sb = new StringBuilder();
        sb.append("Repository: ").append(ctx.repoName()).append('\n');
        sb.append("Technologies: ").append(String.join(", ", ctx.tech().technologies())).append('\n');
        sb.append("Source lines by language: ").append(ctx.tech().languageLines()).append('\n');
        sb.append("Architecture style: ").append(String.join("; ", g.styles())).append('\n');
        sb.append("Components by role: ").append(g.roleCounts()).append('\n');
        List<String> endpoints = new ArrayList<>();
        for (GraphNode n : g.nodes()) if (n.endpoints() != null) endpoints.addAll(n.endpoints());
        if (!endpoints.isEmpty()) sb.append("REST endpoints (").append(endpoints.size()).append("): ")
                .append(endpoints.stream().limit(40).collect(Collectors.joining(", "))).append('\n');
        Map<String, GraphNode> byId = g.nodes().stream().collect(Collectors.toMap(GraphNode::id, n -> n, (a, b) -> a));
        String deps = g.edges().stream().filter(e -> e.type().equals("DEPENDS") || e.type().equals("CALLS") || e.type().equals("HTTP"))
                .limit(80).map(e -> label(byId, e.from()) + "->" + label(byId, e.to())).distinct().collect(Collectors.joining(", "));
        if (!deps.isEmpty()) sb.append("Main dependencies: ").append(deps).append('\n');
        Map<String, Long> bySeverity = ctx.staticFindings().stream()
                .collect(Collectors.groupingBy(f -> f.getSeverity().name(), TreeMap::new, Collectors.counting()));
        sb.append("Static analysis findings: ").append(bySeverity).append('\n');
        String hot = g.nodes().stream().sorted((a, b) -> b.complexity() - a.complexity()).limit(8)
                .map(n -> n.label() + "(complexity " + n.complexity() + ")").collect(Collectors.joining(", "));
        if (!hot.isEmpty()) sb.append("Most complex types: ").append(hot).append('\n');
        return sb.toString();
    }

    private static String label(Map<String, GraphNode> byId, String id) {
        GraphNode n = byId.get(id);
        return n == null ? id : n.label();
    }

    List<ScannedFile> selectFiles(List<ScannedFile> files, Map<String, List<ReviewFinding>> staticByFile, Map<String, GraphNode> nodeByFile) {
        record Scored(ScannedFile file, int score) {}
        List<Scored> scored = new ArrayList<>();
        for (ScannedFile f : files) {
            boolean code = f.language().equals("Java") || f.language().equals("TypeScript") || f.language().equals("JavaScript")
                    || f.language().equals("Python") || f.language().equals("Kotlin");
            if (!code || ArchitectureAnalyzer.isTestPath(f.path()) || f.lines() < 15) continue;
            int score = 0;
            List<ReviewFinding> sf = staticByFile.getOrDefault(f.path(), List.of());
            for (ReviewFinding x : sf) score += x.getSeverity() == FindingSeverity.CRITICAL ? 8 : x.getSeverity() == FindingSeverity.HIGH ? 5 : 2;
            GraphNode n = nodeByFile.get(f.path());
            if (n != null) {
                score += Math.min(n.complexity() / 4, 15);
                score += switch (n.role()) {
                    case "CONTROLLER", "FILTER", "GUARD", "INTERCEPTOR" -> 5;
                    case "SERVICE", "ANGULAR_SERVICE" -> 4;
                    case "CONFIG" -> 3;
                    case "REPOSITORY" -> 2;
                    default -> 0;
                };
            }
            String lower = f.path().toLowerCase(Locale.ROOT);
            if (lower.contains("auth") || lower.contains("security") || lower.contains("jwt") || lower.contains("login")
                    || lower.contains("token") || lower.contains("password") || lower.contains("upload")) score += 6;
            scored.add(new Scored(f, score));
        }
        scored.sort((a, b) -> b.score() - a.score());
        return scored.stream().limit(Math.max(0, props.getAiMaxFiles())).map(Scored::file).toList();
    }

    // ------------------------------------------------------------------ prompts

    private String globalPrompt(String overview, String rules) {
        return "You are a senior software architect and application security reviewer reviewing a WHOLE repository.\n"
                + "You are given a structural overview produced by a static analyzer (not the source code).\n"
                + "Do not invent facts that are not supported by the overview. Prefer fewer, high-confidence findings.\n\n"
                + "=== OVERVIEW ===\n" + overview + "\n" + rules
                + "Return STRICT JSON only, with this shape:\n"
                + "{\"summary\": \"4-8 sentence assessment of architecture, quality, security posture and main risks\",\n"
                + " \"findings\": [{\"category\": \"ARCHITECTURE|SECURITY|PERFORMANCE|REFACTORING|TEST|BEST_PRACTICE\","
                + " \"severity\": \"CRITICAL|HIGH|MEDIUM|LOW|INFO\", \"title\": \"...\", \"description\": \"...\","
                + " \"recommendation\": \"...\", \"file\": \"path or (repository)\", \"lineNumber\": 1}]}\n"
                + "At most 8 findings. Include suggested missing tests as category TEST. No markdown.";
    }

    private String filePrompt(ScannedFile f, String overview, String rules, List<ReviewFinding> staticFindings,
                              ArchitectureGraph graph, GraphNode node) {
        Set<Integer> maskedLines = new HashSet<>();
        StringBuilder known = new StringBuilder();
        for (ReviewFinding sf : staticFindings) {
            if ("SEC-010".equals(sf.getRuleId()) && sf.getLineNumber() != null) maskedLines.add(sf.getLineNumber());
            known.append("- ").append(sf.getRuleId()).append(" line ").append(sf.getLineNumber()).append(": ").append(sf.getTitle()).append('\n');
        }
        String neighbours = "";
        if (node != null) {
            Map<String, GraphNode> byId = graph.nodes().stream().collect(Collectors.toMap(GraphNode::id, n -> n, (a, b) -> a));
            neighbours = graph.edges().stream().filter(e -> e.from().equals(node.id()) || e.to().equals(node.id()))
                    .limit(25).map(e -> label(byId, e.from()) + " -" + e.type() + "-> " + label(byId, e.to()))
                    .collect(Collectors.joining("\n"));
        }
        StringBuilder code = new StringBuilder();
        String[] lines = f.content().split("\n", -1);
        for (int i = 0; i < lines.length && code.length() < props.getAiMaxCharsPerFile(); i++) {
            String line = maskedLines.contains(i + 1) ? "/* *** MASKED SECRET *** */" : lines[i];
            code.append(i + 1).append("| ").append(line.length() > 400 ? line.substring(0, 400) : line).append('\n');
        }
        return "You are an expert code reviewer. Review ONE file in the context of the whole repository.\n"
                + "Find real bugs, security vulnerabilities, performance problems, refactoring opportunities and missing tests.\n"
                + "Do NOT invent issues; if there are none return an empty findings array. Do not repeat the already-known static findings.\n\n"
                + "=== REPOSITORY OVERVIEW ===\n" + overview + "\n"
                + (node != null ? "=== THIS FILE: " + f.path() + " (role " + node.role() + ") ===\n" : "=== THIS FILE: " + f.path() + " ===\n")
                + (neighbours.isEmpty() ? "" : "Relations:\n" + neighbours + "\n")
                + (known.length() == 0 ? "" : "Already known static findings (do not repeat):\n" + known)
                + rules
                + "Code with line numbers:\n" + code + "\n"
                + "Return STRICT JSON only: {\"findings\": [{\"category\": \"BUG|SECURITY|CODE_SMELL|PERFORMANCE|REFACTORING|TEST|ARCHITECTURE|BEST_PRACTICE\","
                + " \"severity\": \"CRITICAL|HIGH|MEDIUM|LOW|INFO\", \"title\": \"...\", \"description\": \"...\","
                + " \"lineNumber\": <absolute line number from the listing>, \"recommendation\": \"...\", \"fixedCode\": \"optional corrected snippet\"}]}\n"
                + "At most 6 findings. No markdown, no text outside the JSON.";
    }

    private String rulesBlock(List<String> rules) {
        if (rules == null || rules.isEmpty()) return "";
        StringBuilder sb = new StringBuilder("=== CUSTOM RULES (report a finding of category BEST_PRACTICE or ARCHITECTURE, severity MEDIUM, when violated) ===\n");
        int i = 1;
        for (String r : rules) sb.append(i++).append(". ").append(r.length() > 500 ? r.substring(0, 500) : r).append('\n');
        return sb.append('\n').toString();
    }

    private List<ReviewFinding> dedupe(List<ReviewFinding> ai, List<ReviewFinding> staticFindings) {
        Set<String> seen = new HashSet<>();
        for (ReviewFinding s : staticFindings) seen.add(key(s));
        List<ReviewFinding> out = new ArrayList<>();
        for (ReviewFinding f : ai) if (seen.add(key(f))) out.add(f);
        return out;
    }

    private static String key(ReviewFinding f) {
        return (f.getFile() + "|" + f.getLineNumber() + "|" + (f.getTitle() == null ? "" : f.getTitle().toLowerCase(Locale.ROOT))).trim();
    }

    private static List<String> add(List<String> l, String s) { l.add(s); return l; }
}
