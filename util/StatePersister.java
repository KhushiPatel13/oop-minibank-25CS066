package util;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import model.Account;

public class StatePersister {
    private StatePersister() {}

    public static void save(Account[] accounts, Path file) throws IOException {
        if (accounts == null || file == null) {
            throw new IllegalArgumentException("Accounts and file path are required.");
        }
        Path parent = file.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        try (ObjectOutputStream output =
                new ObjectOutputStream(Files.newOutputStream(file))) {
            output.writeObject(accounts);
        }
    }

    public static Account[] load(Path file) throws IOException, ClassNotFoundException {
        if (file == null) {
            throw new IllegalArgumentException("File path is required.");
        }
        try (ObjectInputStream input =
                new ObjectInputStream(Files.newInputStream(file))) {
            Object state = input.readObject();
            if (!(state instanceof Account[] accounts)) {
                throw new IOException("Saved state does not contain an account array.");
            }
            return accounts;
        }
    }
}
