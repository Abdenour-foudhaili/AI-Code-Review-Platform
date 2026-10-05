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

    // FAKE CREDENTIALS ADDED FOR VULNERABILITY TESTING
    private static final String AWS_ACCESS_KEY_ID = "AKIAIOSFODNN7EXAMPLE";
    private static final String AWS_SECRET_ACCESS_KEY = "wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY";
    private static final String GITHUB_TOKEN = "ghp_1234567890abcdefghijklmnopqrstuvwxyzABCD";

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
                .status(ProjectStatus.QUEUED)
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
    
    @Transactional
    public synchronized void analyzeProject(Long id, String modelId) {
        Project p = getProject(id);
        
        if (p.getStatus() == ProjectStatus.ANALYZING) {
            // Recover stale analysis if stuck for more than 10 minutes
            if (p.getUpdatedAt() != null && p.getUpdatedAt().isBefore(LocalDateTime.now().minusMinutes(10))) {
                p.setStatus(ProjectStatus.FAILED);
                repository.save(p); // explicitly save the fallback state if needed
            } else {
                throw new RuntimeException("Already analyzing");
            }
        }
        
        // Immediately set state to ANALYZING before async execution to prevent duplicate requests
        p.setStatus(ProjectStatus.ANALYZING);
        p.setUpdatedAt(LocalDateTime.now());
        repository.save(p);
        
        orchestrator.runProjectAnalysis(id, modelId);
    }
}
