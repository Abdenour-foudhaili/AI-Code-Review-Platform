package com.aicodereview.analysis;

import com.aicodereview.analysis.ArchitectureAnalyzer.Result;
import com.aicodereview.analysis.TechnologyDetector.TechInfo;
import com.aicodereview.analysis.model.*;
import com.aicodereview.analysis.parser.JavaCodeParser;
import com.aicodereview.analysis.parser.TypeScriptCodeParser;
import com.aicodereview.entity.ReviewFinding;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ParserAndArchitectureTest {
    static final String CONTROLLER = """
            package com.demo.web;

            import com.demo.repo.UserRepository;
            import com.demo.service.UserService;
            import org.springframework.web.bind.annotation.*;

            @RestController
            @RequestMapping("/api/users")
            public class UserController {
                private final UserService service;
                private final UserRepository repository;
                public UserController(UserService service, UserRepository repository) { this.service = service; this.repository = repository; }

                @GetMapping("/{id}")
                public com.demo.model.User get(Long id) {
                    if (id == null) { return null; }
                    return service.find(id);
                }

                @PostMapping
                public void create() { repository.save(null); }
            }
            """;
    static final String SERVICE = """
            package com.demo.service;
            import com.demo.repo.UserRepository;
            @org.springframework.stereotype.Service
            public class UserService {
                private final UserRepository repo;
                public UserService(UserRepository repo) { this.repo = repo; }
                public Object find(Long id) { return repo.findById(id); }
                public void debug() { System.out.println("x"); }
            }
            """;
    static final String REPO = """
            package com.demo.repo;
            public interface UserRepository extends JpaRepository<User, Long> { }
            """;
    static final String ENTITY = """
            package com.demo.model;
            @jakarta.persistence.Entity
            public class User { private Long id; private String name; }
            """;

    private final JavaCodeParser parser = new JavaCodeParser();

    private ParsedFile parse(String path, String src) { return parser.parse(path, src); }

    @Test
    void javaParserExtractsStructureEndpointsAndComplexity() {
        ParsedFile pf = parse("src/UserController.java", CONTROLLER);
        assertEquals("com.demo.web", pf.packageName());
        CodeElement c = pf.elements().get(0);
        assertEquals("UserController", c.name());
        assertTrue(c.annotations().contains("RestController"));
        assertEquals("/api/users", c.basePath());
        assertTrue(c.endpoints().contains("GET /api/users/{id}"), c.endpoints().toString());
        assertTrue(c.endpoints().contains("POST /api/users"), c.endpoints().toString());
        assertEquals(2, c.methods().size());
        MethodInfo get = c.methods().stream().filter(m -> m.name().equals("get")).findFirst().orElseThrow();
        assertEquals(2, get.complexity());      // base 1 + one if
        assertTrue(get.calls().contains("service.find"));
    }

    @Test
    void javaParserRejectsUnparsableSource() {
        assertThrows(IllegalArgumentException.class, () -> parse("Broken.java", "public class { void ((( "));
    }

    @Test
    void typeScriptParserDetectsAngularServiceAndComponent() {
        String svc = "import { Injectable } from '@angular/core';\n@Injectable({providedIn:'root'})\nexport class ProjectService {\n  private apiUrl = `${environment.apiUrl}/projects`;\n  constructor(private http: HttpClient) {}\n  list() {\n    if (true) { return this.http.get('/api/projects'); }\n    return null;\n  }\n}\n";
        ParsedFile pf = new TypeScriptCodeParser().parse("src/project.service.ts", svc);
        assertEquals(1, pf.elements().size());
        CodeElement e = pf.elements().get(0);
        assertEquals("ProjectService", e.name());
        assertTrue(e.annotations().contains("Injectable"));
        assertTrue(e.constructorTypes().contains("HttpClient"));
        assertFalse(e.methods().isEmpty());
        assertTrue(pf.apiPaths().contains("/api/projects"), pf.apiPaths().toString());
    }

    @Test
    void architectureAnalyzerBuildsLayersAndFlagsControllerToRepository() {
        List<ParsedFile> files = List.of(parse("web/UserController.java", CONTROLLER), parse("service/UserService.java", SERVICE),
                parse("repo/UserRepository.java", REPO), parse("model/User.java", ENTITY));
        Result result = new ArchitectureAnalyzer().analyze(files, new TechInfo(List.of("Java", "Spring Boot"), List.of(), Map.of()));

        Map<String, Integer> roles = result.graph().roleCounts();
        assertEquals(1, roles.get("CONTROLLER"));
        assertEquals(1, roles.get("SERVICE"));
        assertEquals(1, roles.get("REPOSITORY"));
        assertEquals(1, roles.get("ENTITY"));
        assertTrue(result.graph().edges().stream().anyMatch(e -> e.from().endsWith("UserController") && e.to().endsWith("UserService")));
        assertTrue(result.graph().edges().stream().anyMatch(e -> e.from().endsWith("UserService") && e.to().endsWith("UserRepository")));
        assertTrue(result.findings().stream().anyMatch(f -> "ARCH-001".equals(f.getRuleId())), "controller -> repository must be flagged");
        assertTrue(result.findings().stream().anyMatch(f -> "ARCH-006".equals(f.getRuleId())), "controller returning an entity must be flagged");
    }

    @Test
    void cleanLayersProduceNoLayeringViolation() {
        String cleanController = CONTROLLER.replace("private final UserRepository repository;", "")
                .replace(", UserRepository repository)", ")").replace(" this.repository = repository;", "")
                .replace("repository.save(null);", "").replace("public com.demo.model.User get", "public Object get");
        List<ParsedFile> files = List.of(parse("web/UserController.java", cleanController), parse("service/UserService.java", SERVICE),
                parse("repo/UserRepository.java", REPO), parse("model/User.java", ENTITY));
        Result result = new ArchitectureAnalyzer().analyze(files, new TechInfo(List.of(), List.of(), Map.of()));
        assertTrue(result.findings().stream().noneMatch(f -> "ARCH-001".equals(f.getRuleId())));
        assertTrue(result.graph().styles().get(0).contains("layers respected"), result.graph().styles().toString());
    }

    @Test
    void diagramsAreValidMermaidHeaders() {
        List<ParsedFile> files = List.of(parse("web/UserController.java", CONTROLLER), parse("service/UserService.java", SERVICE),
                parse("repo/UserRepository.java", REPO), parse("model/User.java", ENTITY));
        ArchitectureGraph g = new ArchitectureAnalyzer().analyze(files, new TechInfo(List.of("Spring Boot", "MySQL"), List.of("api.example.org"), Map.of())).graph();
        List<DiagramGenerator.Diagram> diagrams = new DiagramGenerator().generate(g, "demo");
        assertEquals(4, diagrams.size());
        assertTrue(diagrams.get(0).mermaid().startsWith("graph LR"));
        assertTrue(diagrams.get(1).mermaid().startsWith("graph TD"));
        assertTrue(diagrams.get(1).mermaid().contains("CONTROLLER"));
        assertTrue(diagrams.get(2).mermaid().startsWith("classDiagram"));
        assertTrue(diagrams.get(3).mermaid().startsWith("graph LR"));
    }

    @Test
    void staticAnalyzerFindsSmellsAndMasksSecrets() {
        String secretLine = "    String apiKey = \"sk_live_9f8e7d6c5b4a3210\";\n";
        String src = "package a;\npublic class Demo {\n" + secretLine + "    void run() { System.out.println(\"hi\"); }\n}\n";
        ScannedFile file = new ScannedFile("src/Demo.java", src, "Java", ".java", src.length(), 5);
        ParsedFile pf = parse("src/Demo.java", src);
        ArchitectureGraph g = new ArchitectureAnalyzer().analyze(List.of(pf), new TechInfo(List.of(), List.of(), Map.of())).graph();
        List<ReviewFinding> findings = new StaticCodeAnalyzer().analyze(List.of(file), List.of(pf), g);

        assertTrue(findings.stream().anyMatch(f -> "SMELL-010".equals(f.getRuleId())));
        ReviewFinding secret = findings.stream().filter(f -> "SEC-010".equals(f.getRuleId())).findFirst().orElseThrow();
        assertFalse(secret.getEvidence().contains("sk_live_9f8e7d6c5b4a3210"), "the secret value must never be stored");
        assertTrue(findings.stream().anyMatch(f -> "TEST-001".equals(f.getRuleId())), "no tests in the repo");
        assertTrue(findings.stream().allMatch(f -> "STATIC".equals(f.getSource())));
    }

    @Test
    void placeholdersAreNotReportedAsSecrets() {
        String src = "class A {\n  String password = \"changeme-placeholder\";\n  String k = System.getenv(\"API_KEY\");\n}\n";
        ScannedFile file = new ScannedFile("src/A.java", src, "Java", ".java", src.length(), 4);
        List<ReviewFinding> findings = new StaticCodeAnalyzer().analyze(List.of(file), List.of(), new ArchitectureGraph(List.of(), List.of(), Map.of(), List.of(), List.of(), List.of(), List.of()));
        assertTrue(findings.stream().noneMatch(f -> "SEC-010".equals(f.getRuleId())));
    }
}
