package com.aicodereview.analysis;

import com.aicodereview.analysis.model.ScannedFile;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Turns a repository archive (ZIP, e.g. the GitHub zipball) into a filtered list of text source files.
 * Nothing is written to disk. Filters: ignored folders, lock/binary/media/archive files, secrets files (.env, keys),
 * oversized files, binary content (NUL bytes), file-count and total-size caps.
 */
@Component
public class RepositoryScanner {

    public record ScanResult(List<ScannedFile> files, int totalEntries, int skipped, List<String> warnings) {}

    private static final Set<String> CODE_EXTENSIONS = Set.of(".java", ".kt", ".ts", ".tsx", ".js", ".jsx", ".mjs", ".py",
            ".go", ".rs", ".cs", ".php", ".rb", ".html", ".css", ".scss", ".sql", ".xml", ".yml", ".yaml", ".properties",
            ".gradle", ".kts", ".sh", ".toml");
    private static final Set<String> SPECIAL_NAMES = Set.of("Dockerfile", "docker-compose.yml", "package.json", "pom.xml",
            "requirements.txt", "Pipfile", "go.mod", "Cargo.toml", "angular.json", "tsconfig.json", "Makefile");

    private final AnalysisProperties props;

    public RepositoryScanner(AnalysisProperties props) { this.props = props; }

    public ScanResult scan(byte[] zipBytes, boolean stripRootFolder) {
        List<ScannedFile> files = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        int total = 0;
        int skipped = 0;
        long keptBytes = 0;
        int maxEntries = Math.max(props.getMaxFiles() * 30, 10_000);

        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipBytes), StandardCharsets.UTF_8)) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (++total > maxEntries) {
                    warnings.add("Archive has too many entries; remaining entries were ignored");
                    break;
                }
                if (entry.isDirectory()) continue;
                String path = normalize(entry.getName(), stripRootFolder);
                if (path == null || !isAnalyzable(path)) { skipped++; continue; }
                if (files.size() >= props.getMaxFiles()) {
                    warnings.add("File limit (" + props.getMaxFiles() + ") reached; the rest of the repository was ignored");
                    break;
                }
                byte[] data = readCapped(zis, props.getMaxFileBytes());
                if (data == null) { skipped++; warnings.add("Skipped large file: " + path); continue; }
                if (data.length == 0 || looksBinary(data)) { skipped++; continue; }
                keptBytes += data.length;
                if (keptBytes > props.getMaxUncompressedBytes()) {
                    warnings.add("Total content limit reached; the rest of the repository was ignored");
                    break;
                }
                String content = new String(data, StandardCharsets.UTF_8);
                String ext = extensionOf(path);
                files.add(new ScannedFile(path, content, languageOf(path), ext, data.length, countLines(content)));
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("Invalid or corrupted archive: " + e.getMessage(), e);
        }
        files.sort(Comparator.comparing(ScannedFile::path));
        return new ScanResult(files, total, skipped, warnings);
    }

    /** Strips the GitHub root folder, rejects traversal / absolute names. Returns null when the entry must be skipped. */
    static String normalize(String rawName, boolean stripRoot) {
        String name = rawName.replace('\\', '/');
        if (name.startsWith("/") || name.contains("../") || name.equals("..") || name.contains("\u0000")) return null;
        if (stripRoot) {
            int i = name.indexOf('/');
            if (i < 0) return null;
            name = name.substring(i + 1);
        }
        return name.isEmpty() ? null : name;
    }

    boolean isAnalyzable(String path) {
        String lower = path.toLowerCase(Locale.ROOT);
        String fileName = path.substring(path.lastIndexOf('/') + 1);
        String lowerName = fileName.toLowerCase(Locale.ROOT);
        for (String dir : props.getIgnoredDirs()) {
            if (path.startsWith(dir + "/") || path.contains("/" + dir + "/")) return false;
        }
        if (props.getIgnoredFileNames().contains(fileName)) return false;
        for (String suffix : props.getIgnoredFileSuffixes()) if (lower.endsWith(suffix)) return false;
        // secrets are never read nor sent anywhere
        if (lowerName.startsWith(".env") || lowerName.equals("id_rsa") || lowerName.equals("credentials.json")
                || lowerName.equals("client_secret.json") || lowerName.endsWith(".keystore")) return false;
        if (SPECIAL_NAMES.contains(fileName) || lowerName.startsWith("docker-compose") || lowerName.startsWith("dockerfile")) return true;
        return CODE_EXTENSIONS.contains(extensionOf(path));
    }

    private static byte[] readCapped(InputStream in, long max) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        long total = 0;
        int n;
        boolean tooBig = false;
        while ((n = in.read(buf)) != -1) {
            total += n;
            if (total > max) { tooBig = true; continue; }   // keep draining the entry so the stream stays consistent
            out.write(buf, 0, n);
        }
        return tooBig ? null : out.toByteArray();
    }

    static boolean looksBinary(byte[] data) {
        int limit = Math.min(data.length, 8000);
        for (int i = 0; i < limit; i++) if (data[i] == 0) return true;
        return false;
    }

    static String extensionOf(String path) {
        String name = path.substring(path.lastIndexOf('/') + 1);
        int i = name.lastIndexOf('.');
        return i > 0 ? name.substring(i).toLowerCase(Locale.ROOT) : "";
    }

    static int countLines(String content) {
        if (content.isEmpty()) return 0;
        int n = 1;
        for (int i = 0; i < content.length(); i++) if (content.charAt(i) == '\n') n++;
        return n;
    }

    static String languageOf(String path) {
        String name = path.substring(path.lastIndexOf('/') + 1);
        if (name.equals("Dockerfile") || name.toLowerCase(Locale.ROOT).startsWith("dockerfile")) return "Docker";
        return switch (extensionOf(path)) {
            case ".java" -> "Java";
            case ".kt", ".kts" -> "Kotlin";
            case ".ts", ".tsx" -> "TypeScript";
            case ".js", ".jsx", ".mjs" -> "JavaScript";
            case ".py" -> "Python";
            case ".go" -> "Go";
            case ".rs" -> "Rust";
            case ".cs" -> "C#";
            case ".php" -> "PHP";
            case ".rb" -> "Ruby";
            case ".html" -> "HTML";
            case ".css", ".scss" -> "CSS";
            case ".sql" -> "SQL";
            case ".xml" -> "XML";
            case ".yml", ".yaml" -> "YAML";
            case ".json" -> "JSON";
            case ".properties" -> "Properties";
            case ".gradle" -> "Gradle";
            case ".sh" -> "Shell";
            case ".toml" -> "TOML";
            default -> "Other";
        };
    }
}
