package com.aicodereview.service;

import com.aicodereview.entity.Project;
import com.aicodereview.entity.ProjectStatus;
import com.aicodereview.repository.ProjectRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
public class ProjectAnalysisIntegrationTest {

    @Autowired
    private ProjectAnalysisTransactionService txService;

    @Autowired
    private ProjectRepository repository;

    @Test
    public void testAnalyzeInTransactionAvoidsLazyInitException() {
        // Setup: Save a project to the actual embedded DB
        Project project = Project.builder()
                .name("Integration Test Project")
                .status(ProjectStatus.READY)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        project = repository.save(project);

        // Act: This will throw LazyInitializationException if @Transactional is missing or bypassed
        // Because findById loads it, session closes, and then it touches project.getFindings().
        txService.analyzeProjectInTransaction(project.getId(), "openrouter/free");

        // Assert
        Project updatedProject = repository.findById(project.getId()).orElse(null);
        assertNotNull(updatedProject);
        // If it reaches here without exception, and the status became COMPLETED, it works.
        // It should become COMPLETED because there are 0 files, so the loop finishes instantly.
        assertEquals(ProjectStatus.COMPLETED, updatedProject.getStatus());
    }
}
