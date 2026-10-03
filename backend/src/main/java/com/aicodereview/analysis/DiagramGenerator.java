package com.aicodereview.analysis;

import com.aicodereview.analysis.model.*;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/** Generates Mermaid source from the architecture graph. The frontend renders it (zoom / pan / SVG-PNG export). */
@Component
public class DiagramGenerator {

    public record Diagram(String type, String title, String mermaid) {}

    private static final int MAX_NODES = 60;
    private static final int MAX_CLASS_NODES = 40;

    public List<Diagram> generate(ArchitectureGraph g, String repoName) {
        List<Diagram> out = new ArrayList<>();
        out.add(new Diagram("SYSTEM", "Global architecture", system(g, repoName)));
        out.add(new Diagram("COMPONENT", "Component diagram", component(g)));
        out.add(new Diagram("CLASS", "Class diagram", classDiagram(g)));
        out.add(new Diagram("DEPENDENCY", "Package / module dependencies", dependencies(g)));
        return out;
    }

    // ------------------------------------------------------------------ system view

    String system(ArchitectureGraph g, String repoName) {
        Map<String, Integer> roles = g.roleCounts();
        boolean hasFront = roles.keySet().stream().anyMatch(r -> r.startsWith("ANGULAR"));
        boolean hasBack = roles.containsKey("CONTROLLER") || roles.containsKey("SERVICE");
        List<String> tech = g.technologies();
        StringBuilder sb = new StringBuilder("graph LR\n");
        sb.append("  user([User])\n");
        if (hasFront) {
            String fw = tech.contains("Angular") ? "Angular" : tech.contains("React") ? "React" : "Frontend";
            sb.append("  front[\"").append(fw).append(" frontend\"]\n  user --> front\n");
        }
        if (hasBack) {
            String fw = tech.contains("Spring Boot") ? "Spring Boot" : tech.contains("Express") ? "Express" : tech.contains("Django") ? "Django" : "Backend";
            sb.append("  back[\"").append(fw).append(" backend\"]\n");
            sb.append(hasFront ? "  front -->|REST| back\n" : "  user --> back\n");
        }
        if (!hasFront && !hasBack) sb.append("  app[\"").append(esc(repoName)).append("\"]\n  user --> app\n");
        String core = hasBack ? "back" : (hasFront ? "front" : "app");
        String db = tech.contains("MySQL") ? "MySQL" : tech.contains("PostgreSQL") ? "PostgreSQL" : tech.contains("MongoDB") ? "MongoDB" : null;
        if (db == null && tech.contains("JPA / Hibernate")) db = "Database";
        if (db != null) sb.append("  db[(").append(db).append(")]\n  ").append(core).append(" --> db\n");
        int i = 0;
        for (String host : g.externalApis()) {
            if (i >= 6) break;
            String id = "ext" + i++;
            sb.append("  ").append(id).append("{{\"").append(esc(host)).append("\"}}\n  ").append(core).append(" -.-> ").append(id).append("\n");
        }
        return sb.toString();
    }

    // ------------------------------------------------------------------ component diagram (layers)

    String component(ArchitectureGraph g) {
        Map<String, String> label = new LinkedHashMap<>();
        label.put("ANGULAR_COMPONENT", "Angular components");
        label.put("GUARD", "Guards");
        label.put("INTERCEPTOR", "Interceptors");
        label.put("ANGULAR_SERVICE", "Angular services");
        label.put("CONTROLLER", "Controllers");
        label.put("FILTER", "Filters");
        label.put("SERVICE", "Services");
        label.put("MAPPER", "Mappers");
        label.put("REPOSITORY", "Repositories");
        label.put("ENTITY", "Entities");
        label.put("DTO", "DTOs");
        label.put("CONFIG", "Configuration");
        label.put("COMPONENT", "Components");
        label.put("MODULE", "Modules");

        Map<String, Integer> counts = new HashMap<>();
        Map<String, String> roleOf = new HashMap<>();
        for (GraphNode n : g.nodes()) {
            if (label.containsKey(n.role())) {
                counts.merge(n.role(), 1, Integer::sum);
                roleOf.put(n.id(), n.role());
            }
        }
        StringBuilder sb = new StringBuilder("graph TD\n");
        if (counts.isEmpty()) {
            sb.append("  empty[\"No layered components detected\"]\n");
            return sb.toString();
        }
        for (Map.Entry<String, String> e : label.entrySet()) {
            if (counts.containsKey(e.getKey())) {
                sb.append("  ").append(e.getKey()).append("[\"").append(e.getValue()).append(" (").append(counts.get(e.getKey())).append(")\"]\n");
            }
        }
        Map<String, Integer> weights = new LinkedHashMap<>();
        for (GraphEdge ed : g.edges()) {
            String a = roleOf.get(ed.from());
            String b = roleOf.get(ed.to());
            if (a == null || b == null || a.equals(b)) continue;
            if (ed.type().equals("EXTENDS") || ed.type().equals("IMPLEMENTS")) continue;
            weights.merge(a + ">" + b + (ed.type().equals("HTTP") ? ":HTTP" : ""), 1, Integer::sum);
        }
        weights.forEach((k, v) -> {
            boolean http = k.endsWith(":HTTP");
            String pair = http ? k.substring(0, k.length() - 5) : k;
            String[] p = pair.split(">");
            sb.append("  ").append(p[0]).append(http ? " -->|HTTP " + v + "| " : " -->|" + v + "| ").append(p[1]).append("\n");
        });
        sb.append(styleBlock(label.keySet(), counts.keySet()));
        return sb.toString();
    }

    private String styleBlock(Collection<String> order, Set<String> present) {
        Map<String, String> colors = Map.ofEntries(
                Map.entry("CONTROLLER", "#dbeafe"), Map.entry("ANGULAR_COMPONENT", "#dbeafe"), Map.entry("SERVICE", "#dcfce7"),
                Map.entry("ANGULAR_SERVICE", "#dcfce7"), Map.entry("REPOSITORY", "#fef9c3"), Map.entry("ENTITY", "#fce7f3"),
                Map.entry("DTO", "#ede9fe"), Map.entry("CONFIG", "#e5e7eb"), Map.entry("GUARD", "#ffedd5"),
                Map.entry("INTERCEPTOR", "#ffedd5"), Map.entry("FILTER", "#ffedd5"), Map.entry("MAPPER", "#ccfbf1"),
                Map.entry("COMPONENT", "#e5e7eb"), Map.entry("MODULE", "#e5e7eb"));
        StringBuilder sb = new StringBuilder();
        for (String role : order) {
            if (present.contains(role) && colors.containsKey(role)) {
                sb.append("  style ").append(role).append(" fill:").append(colors.get(role)).append(",stroke:#64748b,color:#0f172a\n");
            }
        }
        return sb.toString();
    }

    // ------------------------------------------------------------------ class diagram

    String classDiagram(ArchitectureGraph g) {
        List<GraphNode> nodes = g.nodes().stream()
                .filter(n -> !n.role().equals("EXCEPTION") && !n.role().equals("APPLICATION"))
                .sorted(Comparator.comparingInt((GraphNode n) -> importance(n.role())).reversed().thenComparing(GraphNode::label))
                .limit(MAX_CLASS_NODES).toList();
        Map<String, String> alias = new HashMap<>();
        StringBuilder sb = new StringBuilder("classDiagram\n");
        int i = 0;
        for (GraphNode n : nodes) {
            String a = "C" + i++;
            alias.put(n.id(), a);
            sb.append("  class ").append(a).append("[\"").append(esc(n.label())).append("\"] {\n");
            if (n.kind().equals("INTERFACE")) sb.append("    <<interface>>\n");
            else if (n.kind().equals("ENUM")) sb.append("    <<enumeration>>\n");
            else if (!n.role().equals("CLASS") && !n.role().equals("INTERFACE") && !n.role().equals("MODEL"))
                sb.append("    <<").append(n.role().toLowerCase(Locale.ROOT).replace('_', ' ').replace(" ", "")).append(">>\n");
            sb.append("    ").append(n.methods()).append(" methods, ").append(n.loc()).append(" lines\n");
            sb.append("  }\n");
        }
        Set<String> seen = new HashSet<>();
        for (GraphEdge ed : g.edges()) {
            String a = alias.get(ed.from());
            String b = alias.get(ed.to());
            if (a == null || b == null || !seen.add(a + b + ed.type())) continue;
            switch (ed.type()) {
                case "EXTENDS" -> sb.append("  ").append(b).append(" <|-- ").append(a).append("\n");
                case "IMPLEMENTS" -> sb.append("  ").append(b).append(" <|.. ").append(a).append("\n");
                case "DEPENDS" -> sb.append("  ").append(a).append(" --> ").append(b).append("\n");
                default -> { }
            }
        }
        if (nodes.size() < g.nodes().size()) {
            sb.append("  note \"Showing the ").append(nodes.size()).append(" most relevant of ").append(g.nodes().size()).append(" types\"\n");
        }
        return sb.toString();
    }

    private static int importance(String role) {
        return switch (role) {
            case "CONTROLLER", "ANGULAR_COMPONENT" -> 9;
            case "SERVICE", "ANGULAR_SERVICE" -> 8;
            case "REPOSITORY" -> 7;
            case "ENTITY" -> 6;
            case "INTERFACE", "GUARD", "INTERCEPTOR", "FILTER" -> 5;
            case "MAPPER", "CONFIG" -> 4;
            case "DTO" -> 3;
            default -> 2;
        };
    }

    // ------------------------------------------------------------------ package dependency diagram

    String dependencies(ArchitectureGraph g) {
        Map<String, GraphNode> byId = g.nodes().stream().collect(Collectors.toMap(GraphNode::id, n -> n, (a, b) -> a));
        Map<String, Integer> weights = new LinkedHashMap<>();
        Map<String, Integer> sizes = new HashMap<>();
        for (GraphNode n : g.nodes()) sizes.merge(group(n), 1, Integer::sum);
        for (GraphEdge ed : g.edges()) {
            GraphNode a = byId.get(ed.from());
            GraphNode b = byId.get(ed.to());
            if (a == null || b == null) continue;
            String ga = group(a);
            String gb = group(b);
            if (!ga.equals(gb)) weights.merge(ga + "\u0001" + gb, 1, Integer::sum);
        }
        List<String> groups = sizes.entrySet().stream().sorted((x, y) -> y.getValue() - x.getValue())
                .limit(MAX_NODES).map(Map.Entry::getKey).toList();
        Map<String, String> alias = new HashMap<>();
        StringBuilder sb = new StringBuilder("graph LR\n");
        int i = 0;
        for (String grp : groups) {
            String a = "P" + i++;
            alias.put(grp, a);
            sb.append("  ").append(a).append("[\"").append(esc(grp)).append(" (").append(sizes.get(grp)).append(")\"]\n");
        }
        weights.forEach((k, v) -> {
            String[] p = k.split("\u0001");
            if (alias.containsKey(p[0]) && alias.containsKey(p[1])) {
                sb.append("  ").append(alias.get(p[0])).append(" -->|").append(v).append("| ").append(alias.get(p[1])).append("\n");
            }
        });
        if (groups.isEmpty()) sb.append("  empty[\"No modules detected\"]\n");
        return sb.toString();
    }

    private static String group(GraphNode n) {
        String pkg = n.packageName();
        if (pkg == null || pkg.isEmpty()) return "(root)";
        String[] parts = pkg.split("[./]");
        if (pkg.contains(".")) {      // Java: last two segments, e.g. aicodereview.service
            int from = Math.max(0, parts.length - 2);
            return String.join(".", Arrays.copyOfRange(parts, from, parts.length));
        }
        int from = Math.max(0, parts.length - 3);        // TS folders: last three segments
        return String.join("/", Arrays.copyOfRange(parts, from, parts.length));
    }

    private static String esc(String s) {
        return s == null ? "" : s.replace("\"", "'").replace("\n", " ").replace("[", "(").replace("]", ")").replace("<", "‹").replace(">", "›");
    }
}
