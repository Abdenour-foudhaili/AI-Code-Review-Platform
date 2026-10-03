package com.aicodereview.analysis;

import com.aicodereview.analysis.TechnologyDetector.TechInfo;
import com.aicodereview.analysis.model.*;
import com.aicodereview.entity.FindingCategory;
import com.aicodereview.entity.FindingSeverity;
import com.aicodereview.entity.ReviewFinding;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Builds the architecture graph (nodes = types, edges = extends / implements / depends / calls / import / http)
 * from parsed files, classifies roles (Controller, Service, Repository, Entity, Angular component...) and detects
 * architectural violations (layer inversion, DTO leakage, cycles...). Purely static: no LLM involved.
 */
@Component
public class ArchitectureAnalyzer {

    public record Result(ArchitectureGraph graph, List<ReviewFinding> findings) {}

    private record Entry(GraphNode node, CodeElement element, ParsedFile file) {}

    private static final Pattern TOKEN = Pattern.compile("[A-Za-z_][A-Za-z0-9_]*");

    public Result analyze(List<ParsedFile> parsedFiles, TechInfo tech) {
        List<Entry> entries = new ArrayList<>();
        Set<String> usedIds = new HashSet<>();
        for (ParsedFile pf : parsedFiles) {
            if (isTestPath(pf.path())) continue;
            for (CodeElement e : pf.elements()) {
                String id = e.qualifiedName();
                int n = 2;
                while (!usedIds.add(id)) id = e.qualifiedName() + "~" + n++;
                int complexity = e.methods().stream().mapToInt(MethodInfo::complexity).sum();
                GraphNode node = new GraphNode(id, e.name(), classify(e, pf), e.kind(), e.file(), e.packageName(),
                        e.methods().size(), Math.max(1, e.endLine() - e.startLine() + 1), complexity, e.basePath(), e.endpoints());
                entries.add(new Entry(node, e, pf));
            }
        }

        Map<String, List<Entry>> bySimple = new HashMap<>();
        Map<String, Entry> byQualified = new HashMap<>();
        Map<String, List<Entry>> byFile = new HashMap<>();
        for (Entry en : entries) {
            bySimple.computeIfAbsent(en.node().label(), k -> new ArrayList<>()).add(en);
            byQualified.putIfAbsent(en.element().qualifiedName(), en);
            byFile.computeIfAbsent(en.node().file(), k -> new ArrayList<>()).add(en);
        }

        LinkedHashMap<String, GraphEdge> edges = new LinkedHashMap<>();
        List<ReviewFinding> findings = new ArrayList<>();

        for (Entry from : entries) {
            CodeElement e = from.element();
            for (String t : e.extendsTypes()) addEdge(edges, from, firstResolved(t, from, bySimple), "EXTENDS");
            for (String t : e.implementsTypes()) addEdge(edges, from, firstResolved(t, from, bySimple), "IMPLEMENTS");
            for (FieldInfo f : e.fields()) for (Entry to : resolveAll(f.type(), from, bySimple)) addEdge(edges, from, to, "DEPENDS");
            for (String t : e.constructorTypes()) for (Entry to : resolveAll(t, from, bySimple)) addEdge(edges, from, to, "DEPENDS");
            for (MethodInfo m : e.methods()) {
                for (String call : m.calls()) {
                    String receiver = call.substring(0, call.indexOf('.'));
                    for (FieldInfo f : e.fields()) {
                        if (f.name().equals(receiver)) {
                            for (Entry to : resolveAll(f.type(), from, bySimple)) addEdge(edges, from, to, "CALLS");
                        }
                    }
                }
            }
            // Java imports
            for (String imp : from.file().imports()) {
                Entry to = byQualified.get(imp);
                if (to != null) addEdge(edges, from, to, "IMPORT");
            }
        }
        addRelativeImportEdges(entries, byFile, edges);
        addHttpEdges(entries, edges);

        // keep IMPORT edges only when no stronger edge links the same pair
        Set<String> strong = new HashSet<>();
        for (GraphEdge ed : edges.values()) if (!ed.type().equals("IMPORT")) strong.add(ed.from() + ">" + ed.to());
        List<GraphEdge> edgeList = new ArrayList<>();
        for (GraphEdge ed : edges.values()) {
            if (ed.type().equals("IMPORT") && strong.contains(ed.from() + ">" + ed.to())) continue;
            edgeList.add(ed);
        }

        Map<String, Entry> byId = new HashMap<>();
        for (Entry en : entries) byId.put(en.node().id(), en);
        detectViolations(entries, byId, edgeList, bySimple, findings);

        Map<String, Integer> roleCounts = new LinkedHashMap<>();
        entries.stream().collect(java.util.stream.Collectors.groupingBy(en -> en.node().role(), java.util.stream.Collectors.counting()))
                .entrySet().stream().sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .forEach(en -> roleCounts.put(en.getKey(), en.getValue().intValue()));

        List<String> packages = entries.stream().map(en -> en.node().packageName()).filter(p -> !p.isEmpty())
                .distinct().sorted().limit(200).toList();

        List<String> styles = detectStyles(roleCounts, findings);
        ArchitectureGraph graph = new ArchitectureGraph(entries.stream().map(Entry::node).toList(), edgeList, roleCounts,
                packages, styles, tech == null ? List.of() : tech.technologies(), tech == null ? List.of() : tech.externalApis());
        return new Result(graph, findings);
    }

    // ------------------------------------------------------------------ classification

    static String classify(CodeElement e, ParsedFile pf) {
        Set<String> ann = new HashSet<>(e.annotations());
        String n = e.name();
        String pkg = e.packageName() == null ? "" : e.packageName().toLowerCase(Locale.ROOT);
        String lang = pf.language();

        if (lang.equals("Java")) {
            if (ann.contains("RestController") || ann.contains("Controller")) return "CONTROLLER";
            if (ann.contains("RestControllerAdvice") || ann.contains("ControllerAdvice")) return "EXCEPTION_HANDLER";
            if (ann.contains("Service")) return "SERVICE";
            boolean repoType = e.extendsTypes().stream().anyMatch(t -> t.endsWith("Repository") || t.equals("JpaRepository")
                    || t.equals("CrudRepository") || t.equals("PagingAndSortingRepository") || t.equals("MongoRepository"));
            if (ann.contains("Repository") || repoType) return "REPOSITORY";
            if (ann.contains("Entity") || ann.contains("Embeddable") || ann.contains("MappedSuperclass") || ann.contains("Document")) return "ENTITY";
            if (ann.contains("Configuration") || ann.contains("EnableWebSecurity") || ann.contains("ConfigurationProperties")) return "CONFIG";
            if (ann.contains("SpringBootApplication")) return "APPLICATION";
            if (ann.contains("Component") && n.endsWith("Mapper")) return "MAPPER";
            if (n.endsWith("Mapper")) return "MAPPER";
            if (n.endsWith("Filter")) return "FILTER";
            if (n.endsWith("Dto") || n.endsWith("DTO") || n.endsWith("Request") || n.endsWith("Response") || n.endsWith("Payload")) return "DTO";
            if (n.endsWith("Exception") || n.endsWith("Error")) return "EXCEPTION";
            if (n.endsWith("Controller")) return "CONTROLLER";
            if (n.endsWith("Repository")) return "REPOSITORY";
            if (n.endsWith("Service") || n.endsWith("ServiceImpl")) return "SERVICE";
            if (n.endsWith("Config") || n.endsWith("Configuration") || n.endsWith("Properties")) return "CONFIG";
            if (ann.contains("Component")) return "COMPONENT";
            if (pkg.contains(".dto")) return "DTO";
            if (pkg.contains(".entity") || pkg.contains(".domain")) return "ENTITY";
            if (pkg.contains(".repository")) return "REPOSITORY";
            if (pkg.contains(".service")) return "SERVICE";
            if (pkg.contains(".controller")) return "CONTROLLER";
            if (pkg.contains(".config")) return "CONFIG";
            if (e.kind().equals("ENUM")) return "ENUM";
            if (e.kind().equals("INTERFACE")) return "INTERFACE";
            return "CLASS";
        }
        if (lang.equals("TypeScript") || lang.equals("JavaScript")) {
            if (ann.contains("Component")) return "ANGULAR_COMPONENT";
            if (ann.contains("Injectable")) return n.endsWith("Guard") ? "GUARD" : n.endsWith("Interceptor") ? "INTERCEPTOR" : "ANGULAR_SERVICE";
            if (ann.contains("NgModule")) return "ANGULAR_MODULE";
            if (ann.contains("Directive") || ann.contains("Pipe")) return "ANGULAR_DIRECTIVE";
            if (e.kind().equals("INTERFACE") || e.kind().equals("ENUM")) return "MODEL";
            if (n.endsWith("Guard")) return "GUARD";
            if (n.endsWith("Interceptor")) return "INTERCEPTOR";
            if (n.endsWith("Controller")) return "CONTROLLER";
            if (n.endsWith("Service")) return "ANGULAR_SERVICE";
            return "CLASS";
        }
        if (lang.equals("Python")) {
            String p = pf.path().toLowerCase(Locale.ROOT);
            if (p.contains("views") || p.contains("controllers") || p.contains("routes") || p.contains("api/")) return "CONTROLLER";
            if (p.contains("models")) return "ENTITY";
            if (p.contains("services")) return "SERVICE";
            if (p.contains("repositories")) return "REPOSITORY";
            return e.kind().equals("MODULE") ? "MODULE" : "CLASS";
        }
        return "CLASS";
    }

    static boolean isTestPath(String path) {
        String p = path.toLowerCase(Locale.ROOT);
        return p.contains("/src/test/") || p.startsWith("test/") || p.startsWith("tests/") || p.contains("/tests/")
                || p.endsWith(".spec.ts") || p.endsWith(".test.ts") || p.endsWith(".test.js") || p.endsWith("test.java")
                || p.endsWith("tests.java") || p.contains("/__tests__/") || p.matches(".*(^|/)test_[^/]*\\.py");
    }

    // ------------------------------------------------------------------ edges

    private void addEdge(Map<String, GraphEdge> edges, Entry from, Entry to, String type) {
        if (to == null || from.node().id().equals(to.node().id())) return;
        edges.putIfAbsent(from.node().id() + ">" + to.node().id() + ":" + type, new GraphEdge(from.node().id(), to.node().id(), type));
    }

    private Entry firstResolved(String type, Entry from, Map<String, List<Entry>> bySimple) {
        List<Entry> r = resolveAll(type, from, bySimple);
        return r.isEmpty() ? null : r.get(0);
    }

    private List<Entry> resolveAll(String typeString, Entry from, Map<String, List<Entry>> bySimple) {
        List<Entry> result = new ArrayList<>();
        if (typeString == null) return result;
        Matcher m = TOKEN.matcher(typeString);
        Set<String> seen = new HashSet<>();
        while (m.find()) {
            String token = m.group();
            if (!seen.add(token) || token.equals(from.node().label())) continue;
            List<Entry> candidates = bySimple.get(token);
            if (candidates == null || candidates.isEmpty()) continue;
            result.add(pick(candidates, from));
        }
        return result;
    }

    private Entry pick(List<Entry> candidates, Entry from) {
        if (candidates.size() == 1) return candidates.get(0);
        for (Entry c : candidates) if (c.node().packageName().equals(from.node().packageName())) return c;
        List<String> imports = from.file().imports();
        for (Entry c : candidates) {
            String q = c.element().qualifiedName();
            if (imports.contains(q) || imports.contains(c.node().packageName() + ".*")) return c;
        }
        return candidates.get(0);
    }

    /** TypeScript relative imports: link elements of the importing file to the (small) imported module. */
    private void addRelativeImportEdges(List<Entry> entries, Map<String, List<Entry>> byFile, Map<String, GraphEdge> edges) {
        for (Entry from : entries) {
            if (!(from.file().language().equals("TypeScript") || from.file().language().equals("JavaScript"))) continue;
            for (String spec : from.file().imports()) {
                if (!spec.startsWith(".")) continue;
                String base = resolveRelative(TypeScriptFolder.of(from.node().file()), spec);
                for (String candidate : List.of(base + ".ts", base + ".tsx", base + ".js", base + "/index.ts")) {
                    List<Entry> targets = byFile.get(candidate);
                    if (targets != null && targets.size() <= 3) {
                        for (Entry to : targets) addEdge(edges, from, to, "IMPORT");
                        break;
                    }
                }
            }
        }
    }

    private static final class TypeScriptFolder {
        static String of(String path) { int i = path.lastIndexOf('/'); return i >= 0 ? path.substring(0, i) : ""; }
    }

    static String resolveRelative(String folder, String spec) {
        Deque<String> parts = new ArrayDeque<>();
        if (!folder.isEmpty()) parts.addAll(Arrays.asList(folder.split("/")));
        for (String seg : spec.split("/")) {
            if (seg.equals(".") || seg.isEmpty()) continue;
            if (seg.equals("..")) { if (!parts.isEmpty()) parts.removeLast(); }
            else parts.addLast(seg);
        }
        return String.join("/", parts);
    }

    /** Frontend -> backend: client code calling an /api path owned by a controller. */
    private void addHttpEdges(List<Entry> entries, Map<String, GraphEdge> edges) {
        List<Entry> controllers = entries.stream().filter(en -> en.node().role().equals("CONTROLLER")
                && en.element().basePath() != null && !en.element().basePath().isEmpty()).toList();
        if (controllers.isEmpty()) return;
        for (Entry from : entries) {
            if (from.file().apiPaths().isEmpty() || from.file().language().equals("Java")) continue;
            if (!(from.node().role().equals("ANGULAR_SERVICE") || from.node().role().equals("ANGULAR_COMPONENT") || from.node().role().equals("CLASS"))) continue;
            for (String api : from.file().apiPaths()) {
                for (Entry c : controllers) {
                    String base = c.element().basePath();
                    if (api.length() >= 5 && (api.equals(base) || api.startsWith(base + "/") || base.startsWith(api + "/"))) {
                        addEdge(edges, from, c, "HTTP");
                    }
                }
            }
        }
    }

    // ------------------------------------------------------------------ violations

    private void detectViolations(List<Entry> entries, Map<String, Entry> byId, List<GraphEdge> edges,
                                  Map<String, List<Entry>> bySimple, List<ReviewFinding> findings) {
        Set<String> reported = new HashSet<>();
        for (GraphEdge ed : edges) {
            if (!(ed.type().equals("DEPENDS") || ed.type().equals("CALLS"))) continue;
            Entry from = byId.get(ed.from());
            Entry to = byId.get(ed.to());
            if (from == null || to == null) continue;
            String rf = from.node().role();
            String rt = to.node().role();
            String key = ed.from() + ">" + ed.to();
            if (rf.equals("CONTROLLER") && rt.equals("REPOSITORY") && reported.add("1" + key)) {
                findings.add(FindingFactory.staticFinding("ARCH-001", FindingCategory.ARCHITECTURE, FindingSeverity.MEDIUM,
                        from.node().file(), from.element().startLine(),
                        "Controller " + from.node().label() + " accesses repository " + to.node().label() + " directly",
                        "The presentation layer bypasses the service layer, which spreads business and transaction logic into controllers.",
                        "Introduce (or use) a service that wraps " + to.node().label() + " and call that service from the controller.",
                        from.node().label() + " -> " + to.node().label()));
            } else if (rf.equals("REPOSITORY") && (rt.equals("SERVICE") || rt.equals("CONTROLLER")) && reported.add("2" + key)) {
                findings.add(FindingFactory.staticFinding("ARCH-002", FindingCategory.ARCHITECTURE, FindingSeverity.MEDIUM,
                        from.node().file(), from.element().startLine(),
                        "Repository " + from.node().label() + " depends on higher layer " + to.node().label(),
                        "Dependencies should point downwards (Controller -> Service -> Repository -> Entity).",
                        "Move the logic that needs " + to.node().label() + " into the service layer.",
                        from.node().label() + " -> " + to.node().label()));
            } else if (rf.equals("SERVICE") && rt.equals("CONTROLLER") && reported.add("3" + key)) {
                findings.add(FindingFactory.staticFinding("ARCH-003", FindingCategory.ARCHITECTURE, FindingSeverity.MEDIUM,
                        from.node().file(), from.element().startLine(),
                        "Service " + from.node().label() + " depends on controller " + to.node().label(),
                        "A service must not know about the web layer.",
                        "Extract the shared logic into a service or helper used by both classes.",
                        from.node().label() + " -> " + to.node().label()));
            } else if (rf.equals("ENTITY") && (rt.equals("SERVICE") || rt.equals("REPOSITORY") || rt.equals("CONTROLLER")) && reported.add("4" + key)) {
                findings.add(FindingFactory.staticFinding("ARCH-004", FindingCategory.ARCHITECTURE, FindingSeverity.MEDIUM,
                        from.node().file(), from.element().startLine(),
                        "Entity " + from.node().label() + " depends on application layer class " + to.node().label(),
                        "Domain entities should stay independent from services, repositories and controllers.",
                        "Remove the dependency from the entity and move the behaviour to a service.",
                        from.node().label() + " -> " + to.node().label()));
            }
        }

        for (Entry en : entries) {
            // Angular component calling HTTP directly
            if (en.node().role().equals("ANGULAR_COMPONENT") && en.element().constructorTypes().contains("HttpClient")) {
                findings.add(FindingFactory.staticFinding("ARCH-005", FindingCategory.ARCHITECTURE, FindingSeverity.LOW,
                        en.node().file(), en.element().startLine(),
                        "Component " + en.node().label() + " uses HttpClient directly",
                        "HTTP access in components makes them harder to test and reuse.",
                        "Move HTTP calls to an injectable service and inject that service in the component.", "HttpClient injected in component"));
            }
            // Controller exposes entities
            if (en.node().role().equals("CONTROLLER")) {
                Set<String> exposed = new LinkedHashSet<>();
                for (MethodInfo m : en.element().methods()) {
                    for (Entry to : resolveAll(m.returnType(), en, bySimple)) {
                        if (to.node().role().equals("ENTITY")) exposed.add(to.node().label());
                    }
                }
                if (!exposed.isEmpty()) {
                    findings.add(FindingFactory.staticFinding("ARCH-006", FindingCategory.ARCHITECTURE, FindingSeverity.MEDIUM,
                            en.node().file(), en.element().startLine(),
                            "Controller " + en.node().label() + " returns entities directly: " + String.join(", ", exposed),
                            "Returning JPA entities couples the API contract to the database model and may leak fields or trigger lazy loading issues.",
                            "Return dedicated response DTOs and map entities in the service layer.", String.join(", ", exposed)));
                }
            }
        }
        detectCycles(entries, byId, edges, findings);
    }

    private void detectCycles(List<Entry> entries, Map<String, Entry> byId, List<GraphEdge> edges, List<ReviewFinding> findings) {
        Map<String, List<String>> adj = new HashMap<>();
        for (GraphEdge ed : edges) {
            if (ed.type().equals("DEPENDS") || ed.type().equals("CALLS")) adj.computeIfAbsent(ed.from(), k -> new ArrayList<>()).add(ed.to());
        }
        Map<String, Integer> index = new HashMap<>();
        Map<String, Integer> low = new HashMap<>();
        Deque<String> stack = new ArrayDeque<>();
        Set<String> onStack = new HashSet<>();
        int[] counter = {0};
        List<List<String>> sccs = new ArrayList<>();
        for (Entry en : entries) {
            if (!index.containsKey(en.node().id())) strongConnect(en.node().id(), adj, index, low, stack, onStack, counter, sccs);
        }
        int reported = 0;
        for (List<String> scc : sccs) {
            if (scc.size() < 2 || scc.size() > 8 || reported >= 10) continue;
            boolean allEntities = scc.stream().allMatch(id -> byId.get(id) != null && byId.get(id).node().role().equals("ENTITY"));
            if (allEntities) continue;   // bidirectional JPA associations are legitimate
            List<String> names = scc.stream().map(id -> byId.get(id).node().label()).sorted().toList();
            Entry first = byId.get(scc.get(0));
            findings.add(FindingFactory.staticFinding("ARCH-007", FindingCategory.ARCHITECTURE, FindingSeverity.MEDIUM,
                    first.node().file(), first.element().startLine(),
                    "Circular dependency between " + String.join(", ", names),
                    "These types depend on each other, which makes them hard to test, reuse and evolve.",
                    "Break the cycle by extracting an interface or moving the shared behaviour into a third component.",
                    String.join(" <-> ", names)));
            reported++;
        }
    }

    private void strongConnect(String v, Map<String, List<String>> adj, Map<String, Integer> index, Map<String, Integer> low,
                               Deque<String> stack, Set<String> onStack, int[] counter, List<List<String>> sccs) {
        index.put(v, counter[0]);
        low.put(v, counter[0]);
        counter[0]++;
        stack.push(v);
        onStack.add(v);
        for (String w : adj.getOrDefault(v, List.of())) {
            if (!index.containsKey(w)) {
                strongConnect(w, adj, index, low, stack, onStack, counter, sccs);
                low.put(v, Math.min(low.get(v), low.get(w)));
            } else if (onStack.contains(w)) {
                low.put(v, Math.min(low.get(v), index.get(w)));
            }
        }
        if (low.get(v).equals(index.get(v))) {
            List<String> scc = new ArrayList<>();
            String w;
            do {
                w = stack.pop();
                onStack.remove(w);
                scc.add(w);
            } while (!w.equals(v));
            sccs.add(scc);
        }
    }

    private List<String> detectStyles(Map<String, Integer> roles, List<ReviewFinding> findings) {
        List<String> styles = new ArrayList<>();
        if (roles.containsKey("CONTROLLER") && roles.containsKey("SERVICE") && roles.containsKey("REPOSITORY")) {
            long layerIssues = findings.stream().filter(f -> f.getRuleId() != null && f.getRuleId().matches("ARCH-00[1-4]")).count();
            styles.add("Layered architecture (Controller -> Service -> Repository -> Entity)"
                    + (layerIssues == 0 ? " - layers respected" : " - " + layerIssues + " layering violation(s)"));
        }
        if (roles.containsKey("ANGULAR_COMPONENT") && roles.containsKey("ANGULAR_SERVICE")) styles.add("Angular component / service architecture");
        if (styles.isEmpty()) styles.add("No well-known architectural style detected");
        return styles;
    }
}
