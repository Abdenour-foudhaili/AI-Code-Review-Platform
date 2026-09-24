package com.aicodereview.service;
import com.aicodereview.entity.Project;
import com.aicodereview.entity.ProjectStatus;
import com.aicodereview.repository.ProjectRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class ProjectService {
    private final ProjectRepository repository;
    private final ProjectScanner scanner;
    private final ProjectAnalysisOrchestrator orchestrator;
    
    public ProjectService(ProjectRepository repository, ProjectScanner scanner, ProjectAnalysisOrchestrator orchestrator) {
        this.repository = repository;
        this.scanner = scanner;
        this.orchestrator = orchestrator;
    }
    
    @Transactional
    public Project createProject(String name, String description, MultipartFile file) throws Exception {
        if (file == null || file.isEmpty()) throw new IllegalArgumentException("ZIP file is empty");
        
        Project project = Project.builder()
                .name(name)
                .description(description)
                .status(ProjectStatus.READY)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
                
        // Save first to get ID
        project = repository.save(project);
        
        scanner.processProjectArchive(project, file);
        return repository.save(project);
    }
    
    public List<Project> getAllProjects() {
        return repository.findAll();
    }
    
    public Project getProject(Long id) {
        return repository.findById(id).orElseThrow(() -> new RuntimeException("Not found"));
    }
    
    public void deleteProject(Long id) {
        repository.deleteById(id);
    }
    
    public void analyzeProject(Long id) {
        Project p = getProject(id);
        if (p.getStatus() == ProjectStatus.ANALYZING) throw new RuntimeException("Already analyzing");
        orchestrator.runProjectAnalysis(id);
    }
}
