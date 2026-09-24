package com.aicodereview.controller;
import com.aicodereview.dto.*;
import com.aicodereview.entity.Project;
import com.aicodereview.service.ProjectService;
import com.aicodereview.mapper.ProjectMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/projects")
@CrossOrigin("*")
public class ProjectController {
    
    private final ProjectService projectService;
    private final ProjectMapper projectMapper;
    
    public ProjectController(ProjectService projectService, ProjectMapper projectMapper) {
        this.projectService = projectService;
        this.projectMapper = projectMapper;
    }
    
    @PostMapping
    public ResponseEntity<?> createProject(@RequestParam("name") String name, 
                                           @RequestParam("description") String description, 
                                           @RequestParam("file") MultipartFile file) {
        try {
            Project p = projectService.createProject(name, description, file);
            return ResponseEntity.ok(projectMapper.toResponse(p));
        } catch(Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    
    @GetMapping
    public ResponseEntity<List<ProjectResponse>> getAllProjects() {
        return ResponseEntity.ok(projectService.getAllProjects().stream()
                .map(projectMapper::toResponse)
                .collect(Collectors.toList()));
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<ProjectDetailsResponse> getProject(@PathVariable Long id) {
        return ResponseEntity.ok(projectMapper.toDetailsResponse(projectService.getProject(id)));
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProject(@PathVariable Long id) {
        projectService.deleteProject(id);
        return ResponseEntity.ok().build();
    }
    
    @PostMapping("/{id}/analyze")
    public ResponseEntity<?> analyzeProject(@PathVariable Long id) {
        try {
            projectService.analyzeProject(id);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
