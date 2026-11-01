package util;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class ReportGenerator {
    private ReportGenerator() {}

    public static String generate(Path logsFolder, Path reportFile) throws IOException {
        if (logsFolder == null || reportFile == null) {
            throw new IllegalArgumentException("Log and report paths are required.");
        }

        long totalDeposits = 0;
        long totalWithdrawals = 0;
        List<Path> logFiles = new ArrayList<>();
        try (DirectoryStream<Path> entries = Files.newDirectoryStream(logsFolder, "*.log")) {
            for (Path entry : entries) {
                if (Files.isRegularFile(entry)) {
                    logFiles.add(entry);
                }
            }
        }
        logFiles.sort(Comparator.comparing(path -> path.getFileName().toString()));

        StringBuilder report = new StringBuilder("End-of-day transaction report\n");
        for (Path logFile : logFiles) {
            try (BufferedReader reader = Files.newBufferedReader(logFile, StandardCharsets.UTF_8)) {
                String line;
                long lineNumber = 0;
                while ((line = reader.readLine()) != null) {
                    lineNumber++;
                    String[] parts = line.trim().split("\\s+");
                    if (parts.length != 2) {
                        throw new IOException("Invalid transaction at " + logFile + ":"
                                + lineNumber);
                    }

                    long amount;
                    try {
                        amount = Long.parseLong(parts[1]);
                    } catch (NumberFormatException exception) {
                        throw new IOException("Invalid amount at " + logFile + ":"
                                + lineNumber, exception);
                    }
                    if (amount < 0) {
                        throw new IOException("Negative amount at " + logFile + ":"
                                + lineNumber);
                    }

                    switch (parts[0].toUpperCase(Locale.ROOT)) {
                        case "DEPOSIT" -> totalDeposits = Math.addExact(totalDeposits, amount);
                        case "WITHDRAW" -> totalWithdrawals =
                                Math.addExact(totalWithdrawals, amount);
                        default -> throw new IOException("Unknown transaction type at "
                                + logFile + ":" + lineNumber);
                    }
                }
            }

            BasicFileAttributes attributes =
                    Files.readAttributes(logFile, BasicFileAttributes.class);
            report.append("File: ").append(logFile.getFileName())
                    .append(", size: ").append(attributes.size())
                    .append(" bytes, last modified: ").append(attributes.lastModifiedTime())
                    .append('\n');
        }

        report.append("Total deposits: Rs. ").append(totalDeposits).append('\n')
                .append("Total withdrawals: Rs. ").append(totalWithdrawals).append('\n');

        Path parent = reportFile.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.write(reportFile, report.toString().getBytes(StandardCharsets.UTF_8),
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE);
        return report.toString();
    }
}
