package com.aicodereview.analysis;

import com.aicodereview.analysis.model.*;
import com.aicodereview.entity.FindingCategory;
import com.aicodereview.entity.FindingSeverity;
import com.aicodereview.entity.ReviewFinding;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Deterministic, rule-based static analysis (no LLM): complexity, method/class size, debug output, risky patterns,
 * hard-coded secrets (masked), and missing tests. Every finding has source = STATIC and a stable ruleId.
 */
@Component
public class StaticCodeAnalyzer {
    static final int LONG_METHOD_LINES = 60;
    static final int HIGH_COMPLEXITY = 15;
    static final int LARGE_CLASS_LINES = 500;
    private static final int MAX_PER_RULE = 25;

    private static final Pattern SYSOUT = Pattern.compile("System\\.(out|err)\\.print");
    private static final Pattern PRINT_STACK = Pattern.compile("\\.printStackTrace\\s*\\(");
    private static final Pattern CONSOLE_LOG = Pattern.compile("\\bconsole\\.(log|debug)\\s*\\(");
    private static final Pattern EVAL = Pattern.compile("\\beval\\s*\\(|new Function\\s*\\(|\\binnerHTML\\s*=|bypassSecurityTrust");
    private static final Pattern SQL_CONCAT = Pattern.compile("\"\\s*(?i:select|insert|update|delete)\\b[^\"]*\"\\s*\\+\\s*\\w");
    private static final Pattern CORS_ANY = Pattern.compile("@CrossOrigin\\s*\\(\\s*(?:origins\\s*=\\s*)?\"\\*\"|addAllowedOriginPattern\\(\"\\*\"\\)|setAllowedOrigins\\(.*\"\\*\"");
    private static final Pattern CSRF_OFF = Pattern.compile("csrf\\s*\\(\\s*\\w+\\s*->\\s*\\w+\\.disable\\(\\)\\)|csrf\\(\\)\\.disable\\(\\)");
    private static final Pattern WEAK_HASH = Pattern.compile("MessageDigest\\.getInstance\\(\"(MD5|SHA-?1)\"\\)|createHash\\(['\"](md5|sha1)['\"]\\)");
    private static final Pattern SECRET = Pattern.compile(
            "(?i)(password|passwd|secret|api[_-]?key|access[_-]?token|private[_-]?key)\\s*[:=]\\s*[\"']([^\"'\\s]{8,})[\"']");
    private static final Pattern AWS_KEY = Pattern.compile("AKIA[0-9A-Z]{16}");
    private static final Pattern GH_TOKEN = Pattern.compile("gh[pousr]_[A-Za-z0-9]{30,}");
    private static final Set<String> PLACEHOLDERS = Set.of("password", "changeme", "example", "your", "xxxx", "<", "${", "env", "process.env", "test", "dummy", "secret");

    public List<ReviewFinding> analyze(List<ScannedFile> files, List<ParsedFile> parsed, ArchitectureGraph graph) {
        Map<String, Integer> perRule = new HashMap<>();
        List<ReviewFinding> out = new ArrayList<>();

        for (ParsedFile pf : parsed) {
            if (ArchitectureAnalyzer.isTestPath(pf.path())) continue;
            for (CodeElement e : pf.elements()) {
                int classLines = e.endLine() - e.startLine() + 1;
                if (classLines > LARGE_CLASS_LINES) {
                    add(out, perRule, FindingFactory.staticFinding("SMELL-001", FindingCategory.CODE_SMELL, FindingSeverity.MEDIUM, e.file(), e.startLine(),
                            "Large class: " + e.name() + " (" + classLines + " lines)",
                            "Classes this large usually mix several responsibilities and are hard to understand and test.",
                            "Split " + e.name() + " into smaller classes with a single responsibility (Extract Class).", e.name() + ": " + classLines + " lines"));
                }
                for (MethodInfo m : e.methods()) {
                    if (m.length() > LONG_METHOD_LINES) {
                        add(out, perRule, FindingFactory.staticFinding("SMELL-002", FindingCategory.CODE_SMELL, FindingSeverity.MEDIUM, e.file(), m.startLine(),
                                "Long method: " + e.name() + "." + m.name() + " (" + m.length() + " lines)",
                                "Long methods are hard to read, test and reuse.",
                                "Extract cohesive blocks of " + m.name() + " into well-named private methods (Extract Method).", m.name() + ": " + m.length() + " lines"));
                    }
                    if (m.complexity() > HIGH_COMPLEXITY) {
                        FindingSeverity sev = m.complexity() > 30 ? FindingSeverity.HIGH : FindingSeverity.MEDIUM;
                        add(out, perRule, FindingFactory.staticFinding("SMELL-003", FindingCategory.CODE_SMELL, sev, e.file(), m.startLine(),
                                "High cyclomatic complexity in " + e.name() + "." + m.name() + " (" + m.complexity() + ")",
                                "Many branches make the method difficult to test exhaustively and error-prone.",
                                "Simplify conditions, use guard clauses, or split the method (Decompose Conditional / Extract Method).", "complexity=" + m.complexity()));
                    }
                    if (m.paramCount() > 6) {
                        add(out, perRule, FindingFactory.staticFinding("SMELL-004", FindingCategory.REFACTORING, FindingSeverity.LOW, e.file(), m.startLine(),
                                "Too many parameters in " + e.name() + "." + m.name() + " (" + m.paramCount() + ")",
                                "Long parameter lists are hard to call correctly.",
                                "Group related parameters in a parameter object / DTO (Introduce Parameter Object).", m.paramCount() + " parameters"));
                    }
                }
            }
        }

        for (ScannedFile f : files) {
            String path = f.path();
            boolean test = ArchitectureAnalyzer.isTestPath(path);
            String lang = f.language();
            boolean code = lang.equals("Java") || lang.equals("TypeScript") || lang.equals("JavaScript") || lang.equals("Python") || lang.equals("Kotlin");
            if (!code && !lang.equals("YAML") && !lang.equals("Properties") && !lang.equals("JSON")) continue;
            String[] lines = f.content().split("\n", -1);
            for (int i = 0; i < lines.length; i++) {
                String line = lines[i];
                if (line.length() > 2000) continue;
                int n = i + 1;
                if (!test && code) {
                    if (lang.equals("Java") || lang.equals("Kotlin")) {
                        if (SYSOUT.matcher(line).find() && !isComment(line)) {
                            add(out, perRule, FindingFactory.staticFinding("SMELL-010", FindingCategory.BEST_PRACTICE, FindingSeverity.LOW, path, n,
                                    "System.out/err used instead of a logger", "Console output is not configurable, leveled or structured.",
                                    "Use SLF4J (private static final Logger log = LoggerFactory.getLogger(...)).", line.trim()));
                        }
                        if (PRINT_STACK.matcher(line).find()) {
                            add(out, perRule, FindingFactory.staticFinding("SMELL-011", FindingCategory.BEST_PRACTICE, FindingSeverity.LOW, path, n,
                                    "printStackTrace() used", "Stack traces printed to stderr bypass the logging configuration.",
                                    "Log the exception with the logger: log.error(\"message\", ex).", line.trim()));
                        }
                        if (SQL_CONCAT.matcher(line).find()) {
                            add(out, perRule, FindingFactory.staticFinding("SEC-001", FindingCategory.SECURITY, FindingSeverity.HIGH, path, n,
                                    "Possible SQL injection (string concatenation in a query)",
                                    "Concatenating values into SQL text allows an attacker to alter the query.",
                                    "Use parameterised queries (PreparedStatement, JPA named parameters, Spring Data derived queries).", line.trim()));
                        }
                        if (CORS_ANY.matcher(line).find()) {
                            add(out, perRule, FindingFactory.staticFinding("SEC-002", FindingCategory.SECURITY, FindingSeverity.MEDIUM, path, n,
                                    "CORS allows any origin", "A wildcard origin lets any website call the API from a browser.",
                                    "Restrict the allowed origins to the known frontend URL(s).", line.trim()));
                        }
                        if (CSRF_OFF.matcher(line).find()) {
                            add(out, perRule, FindingFactory.staticFinding("SEC-003", FindingCategory.SECURITY, FindingSeverity.INFO, path, n,
                                    "CSRF protection disabled", "Acceptable only for stateless APIs that never authenticate with cookies.",
                                    "Verify that authentication is token-based (Authorization header) and no cookie authenticates API calls.", line.trim()));
                        }
                    }
                    if (lang.equals("TypeScript") || lang.equals("JavaScript")) {
                        if (CONSOLE_LOG.matcher(line).find() && !isComment(line)) {
                            add(out, perRule, FindingFactory.staticFinding("SMELL-012", FindingCategory.BEST_PRACTICE, FindingSeverity.INFO, path, n,
                                    "console.log left in code", "Debug output should not ship to production.",
                                    "Remove it or route it through a logging service.", line.trim()));
                        }
                        if (EVAL.matcher(line).find()) {
                            add(out, perRule, FindingFactory.staticFinding("SEC-004", FindingCategory.SECURITY, FindingSeverity.HIGH, path, n,
                                    "Dynamic code / unsafe HTML injection", "eval, new Function, innerHTML assignment and bypassSecurityTrust* can lead to XSS.",
                                    "Avoid them; use data binding and framework sanitisation.", line.trim()));
                        }
                    }
                    if (WEAK_HASH.matcher(line).find()) {
                        add(out, perRule, FindingFactory.staticFinding("SEC-005", FindingCategory.SECURITY, FindingSeverity.MEDIUM, path, n,
                                "Weak hash algorithm (MD5 / SHA-1)", "These algorithms are broken for security purposes.",
                                "Use SHA-256+ for integrity and BCrypt/Argon2 for passwords.", line.trim()));
                    }
                }
                if (!test) detectSecret(out, perRule, path, n, line);
            }
        }
        addMissingTestFindings(out, files, graph);
        out.sort(Comparator.comparing((ReviewFinding f) -> f.getSeverity().ordinal()).thenComparing(ReviewFinding::getFile).thenComparing(f -> f.getLineNumber() == null ? 0 : f.getLineNumber()));
        return out;
    }

    private void detectSecret(List<ReviewFinding> out, Map<String, Integer> perRule, String path, int n, String line) {
        String lower = path.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".md") || lower.contains("example") || lower.contains("sample")) return;
        String masked = null;
        Matcher m = SECRET.matcher(line);
        if (m.find()) {
            String value = m.group(2).toLowerCase(Locale.ROOT);
            boolean placeholder = PLACEHOLDERS.stream().anyMatch(value::contains) || line.contains("${") || line.contains("process.env")
                    || line.contains("getenv") || m.group(2).chars().distinct().count() < 4;
            if (!placeholder) masked = line.replace(m.group(2), "********").trim();
        } else if (AWS_KEY.matcher(line).find()) {
            masked = AWS_KEY.matcher(line).replaceAll("AKIA****************").trim();
        } else if (GH_TOKEN.matcher(line).find()) {
            masked = GH_TOKEN.matcher(line).replaceAll("gh*_****").trim();
        }
        if (masked != null) {
            add(out, perRule, FindingFactory.staticFinding("SEC-010", FindingCategory.SECURITY, FindingSeverity.HIGH, path, n,
                    "Hard-coded secret", "A credential appears to be committed in the source code (value masked here).",
                    "Move the secret to an environment variable or secret manager and rotate the exposed value.", masked));
        }
    }

    private void addMissingTestFindings(List<ReviewFinding> out, List<ScannedFile> files, ArchitectureGraph graph) {
        Set<String> testNames = new HashSet<>();
        boolean anyTests = false;
        for (ScannedFile f : files) {
            if (ArchitectureAnalyzer.isTestPath(f.path())) {
                anyTests = true;
                String name = f.fileName().toLowerCase(Locale.ROOT).replaceAll("\\.(java|ts|js|py)$", "")
                        .replace(".spec", "").replace(".test", "").replaceFirst("^test_", "");
                testNames.add(name.replaceAll("(tests?)$", ""));
            }
        }
        if (!anyTests && !files.isEmpty()) {
            out.add(FindingFactory.staticFinding("TEST-001", FindingCategory.TEST, FindingSeverity.MEDIUM, "(repository)", null,
                    "No automated tests found", "The repository contains no test files.",
                    "Add unit tests, starting with the services and security-critical code.", "0 test files"));
            return;
        }
        int reported = 0;
        for (GraphNode n : graph.nodes()) {
            if (reported >= 15) break;
            boolean critical = n.role().equals("SERVICE") || n.role().equals("ANGULAR_SERVICE") || n.role().equals("GUARD")
                    || n.role().equals("INTERCEPTOR") || n.role().equals("FILTER");
            if (!critical || n.methods() < 2 || n.kind().equals("INTERFACE")) continue;
            String key = n.label().toLowerCase(Locale.ROOT).replace("impl", "");
            boolean covered = testNames.stream().anyMatch(t -> t.contains(key) || key.contains(t) && t.length() > 3);
            if (!covered) {
                out.add(FindingFactory.staticFinding("TEST-002", FindingCategory.TEST, FindingSeverity.LOW, n.file(), null,
                        "No test found for " + n.label(), "No test file seems to cover this " + n.role().toLowerCase(Locale.ROOT).replace('_', ' ') + ".",
                        "Create unit tests for " + n.label() + " covering the nominal paths and error cases.", n.label()));
                reported++;
            }
        }
    }

    private void add(List<ReviewFinding> out, Map<String, Integer> perRule, ReviewFinding f) {
        int count = perRule.merge(f.getRuleId(), 1, Integer::sum);
        if (count <= MAX_PER_RULE) out.add(f);
    }

    private static boolean isComment(String line) {
        String t = line.trim();
        return t.startsWith("//") || t.startsWith("*") || t.startsWith("/*") || t.startsWith("#");
    }
}
