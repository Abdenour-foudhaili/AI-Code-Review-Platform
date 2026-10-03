package com.aicodereview.analysis;

import com.aicodereview.analysis.AnalysisDtos.*;
import com.aicodereview.security.AuthenticatedUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** Repository analyses (GitHub repositories and deep analysis of imported ZIP projects). All endpoints require a JWT. */
@RestController
@RequestMapping("/api/analyses")
public class AnalysisController {
    private final AnalysisService service;

    public AnalysisController(AnalysisService service) { this.service = service; }

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Map<String, Object> start(@Valid @RequestBody StartAnalysisRequest request, @AuthenticationPrincipal AuthenticatedUser user) {
        AnalysisSummary s = service.startGithubAnalysis(user.id(), request);
        return Map.of("reviewId", s.id(), "analysisId", s.id(), "status", "PENDING");
    }

    @PostMapping("/zip/{projectId}")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Map<String, Object> startZip(@PathVariable Long projectId, @AuthenticationPrincipal AuthenticatedUser user) {
        AnalysisSummary s = service.startZipAnalysis(user.id(), projectId);
        return Map.of("reviewId", s.id(), "analysisId", s.id(), "status", "PENDING");
    }

    @GetMapping
    public List<AnalysisSummary> history(@AuthenticationPrincipal AuthenticatedUser user) { return service.history(user.id()); }

    @GetMapping("/{id}")
    public AnalysisDetails details(@PathVariable Long id, @AuthenticationPrincipal AuthenticatedUser user) { return service.details(user.id(), id); }

    @GetMapping("/{id}/status")
    public AnalysisStatus status(@PathVariable Long id, @AuthenticationPrincipal AuthenticatedUser user) { return service.status(user.id(), id); }

    @GetMapping("/{id}/findings")
    public List<FindingDto> findings(@PathVariable Long id, @RequestParam(required = false) String category,
                                     @RequestParam(required = false) String severity, @RequestParam(required = false) String file,
                                     @RequestParam(required = false) String source, @AuthenticationPrincipal AuthenticatedUser user) {
        return service.findings(user.id(), id, category, severity, file, source);
    }

    @GetMapping("/{id}/architecture")
    public ResponseEntity<String> architecture(@PathVariable Long id, @AuthenticationPrincipal AuthenticatedUser user) {
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(service.architecture(user.id(), id));
    }

    @GetMapping("/{id}/diagrams")
    public ResponseEntity<String> diagrams(@PathVariable Long id, @AuthenticationPrincipal AuthenticatedUser user) {
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(service.diagrams(user.id(), id));
    }

    @GetMapping("/{id}/code")
    public CodeContext code(@PathVariable Long id, @RequestParam String path, @RequestParam(defaultValue = "1") int line,
                            @RequestParam(defaultValue = "12") int radius, @AuthenticationPrincipal AuthenticatedUser user) {
        return service.codeContext(user.id(), id, path, line, radius);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, @AuthenticationPrincipal AuthenticatedUser user) { service.delete(user.id(), id); }
}
