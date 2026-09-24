package com.aicodereview.service;
import com.aicodereview.entity.*;
import com.aicodereview.repository.ProjectRepository;
import com.aicodereview.ai.AIReviewService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import java.io.ByteArrayOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import java.util.Optional;
import java.util.List;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ProjectServiceTest {

    @Mock
    private ProjectRepository repository;
    
    @Mock
    private AIReviewService aiReviewService;
    
    private ProjectScanner scanner = new ProjectScanner();
    
    private ProjectAnalysisOrchestrator orchestrator;
    
    private ProjectService service;

    @BeforeEach
    void setUp() {
        orchestrator = new ProjectAnalysisOrchestrator(repository, aiReviewService);
        service = new ProjectService(repository, scanner, orchestrator);
    }

    private MockMultipartFile createZip(String name, String content) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            ZipEntry entry = new ZipEntry(name);
            zos.putNextEntry(entry);
            zos.write(content.getBytes());
            zos.closeEntry();
        }
        return new MockMultipartFile("file", "test.zip", "application/zip", baos.toByteArray());
    }

    @Test
    void createProject_success() throws Exception {
        MockMultipartFile file = createZip("Test.java", "public class Test {}");
        
        Project p = new Project();
        p.setId(1L);
        
        when(repository.save(any(Project.class))).thenReturn(p);
        
        Project res = service.createProject("Test Project", "Desc", file);
        assertNotNull(res);
        assertTrue(p.getFiles().size() > 0);
        assertEquals("Test.java", p.getFiles().get(0).getFileName());
        assertEquals("Java", p.getFiles().get(0).getLanguage());
    }

    @Test
    void pathTraversal_prevented() throws Exception {
        MockMultipartFile file = createZip("../secret.txt", "secret");
        
        Project p = new Project();
        p.setId(1L);
        when(repository.save(any(Project.class))).thenReturn(p);
        
        assertThrows(Exception.class, () -> {
            service.createProject("Hack", "Desc", file);
        });
    }

    @Test
    void ignoreRules_work() throws Exception {
        MockMultipartFile file = createZip("node_modules/test.js", "console.log('hi');");
        
        Project p = new Project();
        p.setId(1L);
        when(repository.save(any(Project.class))).thenReturn(p);
        
        service.createProject("Test", "Desc", file);
        assertEquals(0, p.getFiles().size(), "Should ignore node_modules");
    }

    @Test
    void analyzeProject_orchestratesCorrectly() {
        Project p = new Project();
        p.setId(1L);
        p.setStatus(ProjectStatus.READY);
        
        ProjectFile pf = new ProjectFile();
        pf.setContent("System.out.println(1);");
        pf.setRelativePath("Main.java");
        p.getFiles().add(pf);
        
        when(repository.findById(1L)).thenReturn(Optional.of(p));
        
        List<ReviewFinding> mockFindings = new ArrayList<>();
        ReviewFinding f = new ReviewFinding();
        f.setSeverity(FindingSeverity.HIGH);
        mockFindings.add(f);
        
        when(aiReviewService.analyzeSourceCode(any(), any(), any())).thenReturn(mockFindings);
        
        orchestrator.analyzeInTransaction(1L);
        
        assertEquals(ProjectStatus.COMPLETED, p.getStatus());
        assertEquals("C", p.getQualityScore());
        assertEquals(1, p.getFindings().size());
        assertEquals("COMPLETED", pf.getAnalysisStatus());
    }
}
