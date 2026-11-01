package util;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public class TransactionLog {
    private TransactionLog() {}

    public static synchronized void append(Path file, String line) throws IOException {
        if (file == null || line == null || line.isBlank()
                || line.indexOf('\n') >= 0 || line.indexOf('\r') >= 0) {
            throw new IllegalArgumentException("A log path and a single non-blank line are required.");
        }
        Path parent = file.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        byte[] entry = (line + System.lineSeparator()).getBytes(StandardCharsets.UTF_8);
        Files.write(file, entry, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }
}
