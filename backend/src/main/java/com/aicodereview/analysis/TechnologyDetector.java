package com.aicodereview.analysis;

import com.aicodereview.analysis.model.ScannedFile;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Detects languages, frameworks, build tools and external API hosts from file names and manifests. */
@Component
public class TechnologyDetector {

    public record TechInfo(List<String> technologies, List<String> externalApis, Map<String, Integer> languageLines) {}

    private static final Pattern URL_HOST = Pattern.compile("https?://([a-zA-Z0-9.-]+\\.[a-zA-Z]{2,})");
    private static final Set<String> NOISE_HOSTS = Set.of("www.w3.org", "w3.org", "schemas.xmlsoap.org", "maven.apache.org",
            "xml.apache.org", "www.springframework.org", "json-schema.org", "example.com", "www.example.com", "github.com",
            "www.apache.org", "fonts.googleapis.com", "fonts.gstatic.com", "angular.dev", "localhost", "xmlns.jcp.org",
            "java.sun.com", "www.eclipse.org", "docs.spring.io", "spring.io", "opensource.org", "creativecommons.org");

    public TechInfo detect(List<ScannedFile> files) {
        Set<String> tech = new LinkedHashSet<>();
        Map<String, Integer> languageLines = new TreeMap<>();
        Map<String, Integer> hosts = new HashMap<>();
        StringBuilder manifests = new StringBuilder();

        for (ScannedFile f : files) {
            if (isSource(f.language())) languageLines.merge(f.language(), f.lines(), Integer::sum);
            String name = f.fileName();
            String lower = name.toLowerCase(Locale.ROOT);
            if (name.equals("pom.xml") || name.equals("package.json") || lower.endsWith(".gradle") || lower.endsWith(".kts")
                    || name.equals("requirements.txt") || name.equals("Pipfile") || name.equals("go.mod") || name.equals("Cargo.toml")
                    || lower.startsWith("application") && (lower.endsWith(".yml") || lower.endsWith(".yaml") || lower.endsWith(".properties"))
                    || lower.startsWith("docker-compose")) {
                manifests.append('\n').append(f.content().toLowerCase(Locale.ROOT));
            }
            switch (name) {
                case "pom.xml" -> tech.add("Maven");
                case "package.json" -> tech.add("Node.js");
                case "Dockerfile" -> tech.add("Docker");
                case "angular.json" -> tech.add("Angular");
                case "go.mod" -> tech.add("Go");
                case "Cargo.toml" -> tech.add("Rust");
                default -> { }
            }
            if (lower.endsWith(".gradle") || lower.endsWith(".kts") && lower.startsWith("build")) tech.add("Gradle");
            if (lower.startsWith("docker-compose") || lower.startsWith("dockerfile")) tech.add("Docker");
            if (isSource(f.language()) && !lower.contains("test") && !lower.endsWith(".spec.ts")) {
                Matcher m = URL_HOST.matcher(f.content());
                int guard = 0;
                while (m.find() && guard++ < 50) {
                    String host = m.group(1).toLowerCase(Locale.ROOT);
                    if (!NOISE_HOSTS.contains(host) && !host.endsWith(".local")) hosts.merge(host, 1, Integer::sum);
                    if (host.equals("api.github.com") || host.equals("gitlab.com")) hosts.merge(host, 1, Integer::sum);
                }
            }
        }

        // languages by presence
        for (String lang : languageLines.keySet()) {
            switch (lang) {
                case "Java", "Kotlin", "TypeScript", "JavaScript", "Python", "Go", "Rust", "C#", "PHP", "Ruby", "SQL" -> tech.add(lang);
                default -> { }
            }
        }
        String m = manifests.toString();
        if (m.contains("spring-boot")) tech.add("Spring Boot");
        if (m.contains("spring-boot-starter-security") || m.contains("spring-security")) tech.add("Spring Security");
        if (m.contains("spring-boot-starter-data-jpa") || m.contains("hibernate")) tech.add("JPA / Hibernate");
        if (m.contains("lombok")) tech.add("Lombok");
        if (m.contains("jjwt") || m.contains("oauth2-jose") || m.contains("jsonwebtoken")) tech.add("JWT");
        if (m.contains("mysql")) tech.add("MySQL");
        if (m.contains("postgres")) tech.add("PostgreSQL");
        if (m.contains("mongodb") || m.contains("mongoose")) tech.add("MongoDB");
        if (m.contains("\"@angular/core\"")) tech.add("Angular");
        if (m.contains("\"react\"")) tech.add("React");
        if (m.contains("\"vue\"")) tech.add("Vue.js");
        if (m.contains("\"express\"")) tech.add("Express");
        if (m.contains("\"@nestjs/core\"")) tech.add("NestJS");
        if (m.contains("django")) tech.add("Django");
        if (m.contains("flask")) tech.add("Flask");
        if (m.contains("fastapi")) tech.add("FastAPI");
        if (m.contains("\"typescript\"")) tech.add("TypeScript");

        List<String> apis = hosts.entrySet().stream()
                .sorted((a, b) -> b.getValue() - a.getValue())
                .limit(8).map(Map.Entry::getKey).toList();
        return new TechInfo(new ArrayList<>(tech), apis, languageLines);
    }

    private static boolean isSource(String language) {
        return switch (language) {
            case "Java", "Kotlin", "TypeScript", "JavaScript", "Python", "Go", "Rust", "C#", "PHP", "Ruby", "SQL", "HTML", "CSS" -> true;
            default -> false;
        };
    }
}
