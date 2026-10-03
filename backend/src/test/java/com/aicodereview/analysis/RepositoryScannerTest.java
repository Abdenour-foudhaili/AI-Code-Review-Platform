package com.aicodereview.analysis;

import com.aicodereview.analysis.RepositoryScanner.ScanResult;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.*;

class RepositoryScannerTest {
    private final AnalysisProperties props = new AnalysisProperties();
    private final RepositoryScanner scanner = new RepositoryScanner(props);

    private byte[] zip(String... nameAndContent) throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(bos)) {
            for (int i = 0; i < nameAndContent.length; i += 2) {
                zos.putNextEntry(new ZipEntry(nameAndContent[i]));
                zos.write(nameAndContent[i + 1].getBytes(StandardCharsets.UTF_8));
                zos.closeEntry();
            }
        }
        return bos.toByteArray();
    }

    @Test
    void stripsGithubRootFolderAndKeepsSources() throws Exception {
        ScanResult r = scanner.scan(zip("owner-repo-abc123/src/Main.java", "class Main {}\n", "owner-repo-abc123/README.md", "# hi"), true);
        assertEquals(1, r.files().size());
        assertEquals("src/Main.java", r.files().get(0).path());
        assertEquals("Java", r.files().get(0).language());
    }

    @Test
    void ignoresDependenciesBuildOutputAndLockFiles() throws Exception {
        ScanResult r = scanner.scan(zip(
                "r/node_modules/lib/index.js", "x=1",
                "r/target/classes/A.java", "class A{}",
                "r/dist/app.js", "x=1",
                "r/.git/config", "x",
                "r/package-lock.json", "{}",
                "r/app.min.js", "x",
                "r/src/app.ts", "export const a = 1;"), true);
        assertEquals(List.of("src/app.ts"), r.files().stream().map(f -> f.path()).toList());
    }

    @Test
    void neverReadsSecretFilesOrBinaryContent() throws Exception {
        ScanResult r = scanner.scan(zip(
                "r/.env", "API_KEY=abcdef123456",
                "r/.env.production", "X=1",
                "r/server.key", "-----BEGIN",
                "r/Fake.java", "class A {\u0000\u0000}",
                "r/Ok.java", "class Ok {}"), true);
        assertEquals(List.of("Ok.java"), r.files().stream().map(f -> f.path()).toList());
    }

    @Test
    void skipsPathTraversalEntries() throws Exception {
        ScanResult r = scanner.scan(zip("r/../../etc/Evil.java", "class Evil {}", "r/Good.java", "class Good {}"), true);
        assertEquals(List.of("Good.java"), r.files().stream().map(f -> f.path()).toList());
    }

    @Test
    void skipsOversizedFilesWithAWarning() throws Exception {
        AnalysisProperties small = new AnalysisProperties();
        small.setMaxFileBytes(50);
        ScanResult r = new RepositoryScanner(small).scan(zip("r/Big.java", "x".repeat(500), "r/Small.java", "class S {}"), true);
        assertEquals(1, r.files().size());
        assertTrue(r.warnings().stream().anyMatch(w -> w.contains("Big.java")));
    }

    @Test
    void enforcesFileCountLimit() throws Exception {
        AnalysisProperties small = new AnalysisProperties();
        small.setMaxFiles(2);
        ScanResult r = new RepositoryScanner(small).scan(zip("r/A.java", "class A{}", "r/B.java", "class B{}", "r/C.java", "class C{}"), true);
        assertEquals(2, r.files().size());
        assertFalse(r.warnings().isEmpty());
    }

    @Test
    void corruptedArchiveIsReportedClearly() {
        // not a zip at all: no entries -> empty result (ZipInputStream does not throw on garbage), never an exception leak
        ScanResult r = scanner.scan("this is not a zip".getBytes(StandardCharsets.UTF_8), true);
        assertTrue(r.files().isEmpty());
    }
}
