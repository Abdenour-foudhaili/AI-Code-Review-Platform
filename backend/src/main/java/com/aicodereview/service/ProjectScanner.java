package com.aicodereview.service;
import com.aicodereview.entity.Project;
import com.aicodereview.entity.ProjectFile;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service
public class ProjectScanner {
    
    private static final List<String> IGNORED_DIRS = Arrays.asList(".git", "node_modules", "target", "dist", "build", ".idea", ".vscode");
    private static final List<String> SUPPORTED_EXTS = Arrays.asList(".java", ".py", ".js", ".ts", ".html", ".css");

    public void processProjectArchive(Project project, MultipartFile zipFile) throws IOException {
        Path tempDir = Files.createTempDirectory("project_" + project.getId());
        
        try (ZipInputStream zis = new ZipInputStream(zipFile.getInputStream())) {
            ZipEntry zipEntry = zis.getNextEntry();
            while (zipEntry != null) {
                if (!zipEntry.isDirectory()) {
                    String relativePath = zipEntry.getName().replace("\\", "/");
                    
                    // Prevent path traversal
                    if (relativePath.contains("..")) {
                        throw new IOException("Path traversal detected in ZIP");
                    }
                    
                    if (isRelevant(relativePath)) {
                        Path newFilePath = tempDir.resolve(relativePath);
                        Files.createDirectories(newFilePath.getParent());
                        Files.copy(zis, newFilePath, StandardCopyOption.REPLACE_EXISTING);
                        
                        String content = Files.readString(newFilePath);
                        String ext = getExtension(relativePath);
                        
                        ProjectFile pf = ProjectFile.builder()
                                .project(project)
                                .relativePath(relativePath)
                                .fileName(Paths.get(relativePath).getFileName().toString())
                                .language(getLanguage(ext))
                                .extension(ext)
                                .fileSize((long) content.getBytes().length)
                                .lineCount(content.split("\n").length)
                                .analysisStatus("PENDING")
                                .content(content)
                                .build();
                        
                        project.getFiles().add(pf);
                    }
                }
                zipEntry = zis.getNextEntry();
            }
            zis.closeEntry();
        } finally {
            deleteDirectory(tempDir.toFile());
        }
        
        // aggregate languages
        Set<String> langs = new HashSet<>();
        for (ProjectFile pf : project.getFiles()) {
            if (pf.getLanguage() != null && !pf.getLanguage().isEmpty()) {
                langs.add(pf.getLanguage());
            }
        }
        project.setDetectedLanguages(String.join(", ", langs));
    }
    
    private boolean isRelevant(String path) {
        for (String dir : IGNORED_DIRS) {
            if (path.contains("/" + dir + "/") || path.startsWith(dir + "/")) {
                return false;
            }
        }
        String ext = getExtension(path);
        return SUPPORTED_EXTS.contains(ext);
    }
    
    private String getExtension(String path) {
        int idx = path.lastIndexOf('.');
        if (idx > 0) return path.substring(idx);
        return "";
    }
    
    private String getLanguage(String ext) {
        switch (ext) {
            case ".java": return "Java";
            case ".py": return "Python";
            case ".js": return "JavaScript";
            case ".ts": return "TypeScript";
            case ".html": return "HTML";
            case ".css": return "CSS";
            default: return "Unknown";
        }
    }
    
    private void deleteDirectory(File dir) {
        if (dir.isDirectory()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File file : files) {
                    deleteDirectory(file);
                }
            }
        }
        dir.delete();
    }
}
