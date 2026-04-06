import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class FileService {
    private final Path filePath;

    public FileService(String fileName) {
        this.filePath = Path.of(fileName);
    }

    public void write(String content) {
        try {
            Files.writeString(filePath, content, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Could not write file: " + filePath, e);
        }
    }

    public String read() {
        try {
            if (!Files.exists(filePath)) {
                return "";
            }
            return Files.readString(filePath, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Could not read file: " + filePath, e);
        }
    }

    public boolean exists() {
        return Files.exists(filePath);
    }

    public String path() {
        return filePath.toAbsolutePath().toString();
    }
}
