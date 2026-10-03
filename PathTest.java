import java.nio.file.Path;
public class PathTest {
    public static void main(String[] args) {
        Path p1 = Path.of("C:/Users/MSI/Desktop/tempDir/app/config.py");
        Path p2 = Path.of("C:\\Users\\MSI\\Desktop\\tempDir");
        System.out.println("Starts with: " + p1.startsWith(p2));
        System.out.println("Relativize: " + p2.relativize(p1));
    }
}
