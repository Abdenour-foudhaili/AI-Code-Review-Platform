package com.aicodereview.controller;
import com.aicodereview.dto.*;
import com.aicodereview.entity.Project;
import com.aicodereview.mapper.ProjectMapper;
import com.aicodereview.security.AuthenticatedUser;
import com.aicodereview.service.OwnershipService;
import com.aicodereview.service.ProjectService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;

/** ZIP-imported projects (legacy flow), now authenticated and isolated per user. */
@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    private final ProjectService projectService;
    private final ProjectMapper projectMapper;
    private final OwnershipService ownership;

    public ProjectController(ProjectService projectService, ProjectMapper projectMapper, OwnershipService ownership) {
        this.projectService = projectService;
        this.projectMapper = projectMapper;
        this.ownership = ownership;
    }

    @PostMapping
    public ResponseEntity<?> createProject(@RequestParam("name") String name,
                                           @RequestParam("description") String description,
                                           @RequestParam("file") MultipartFile file,
                                           @AuthenticationPrincipal AuthenticatedUser user) {
        try {
            Project p = projectService.createProject(name, description, file);
            ownership.assignProjectOwner(p.getId(), user.id());
            return ResponseEntity.ok(projectMapper.toResponse(p));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping
    @Transactional(readOnly = true)
    public ResponseEntity<List<ProjectResponse>> getAllProjects(@AuthenticationPrincipal AuthenticatedUser user) {
        return ResponseEntity.ok(ownership.listProjects(user.id()).stream()
                .map(projectMapper::toResponse)
                .collect(Collectors.toList()));
    }

    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public ResponseEntity<ProjectDetailsResponse> getProject(@PathVariable Long id, @AuthenticationPrincipal AuthenticatedUser user) {
        return ResponseEntity.ok(projectMapper.toDetailsResponse(ownership.requireProject(id, user.id())));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProject(@PathVariable Long id, @AuthenticationPrincipal AuthenticatedUser user) {
        ownership.requireProjectAccess(id, user.id());
        projectService.deleteProject(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/analyze")
    public ResponseEntity<?> analyzeProject(@PathVariable Long id,
                                            @RequestParam(value = "model", required = false) String modelId,
                                            @AuthenticationPrincipal AuthenticatedUser user) {
        ownership.requireProjectAccess(id, user.id());
        try {
            projectService.analyzeProject(id, modelId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
